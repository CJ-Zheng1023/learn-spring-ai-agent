package com.neusoftmedical.skill;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 查询条件解析Skill（基于中文字段名的结构化输出）
 * 将自然语言描述的患者查询条件转换为结构化 JSON 数组，
 * 与 {@link QueryConditionParserSkill} 的区别：fieldName 直接使用字段中文名，不做短编码映射；
 * operation 取值为五算子字典：range / neq / eq / contains / excludes。
 */
@Component
public class QueryParserSkill {

    private final ChatModel chatModel;

    public QueryParserSkill(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Tool(description = "将自然语言描述的患者查询条件转换为结构化JSON数组。每个数组元素形如 {\"fieldName\":\"字段中文名\",\"operation\":\"算子\",\"value\":\"条件值\"}。operation取值：range(区间/大于/小于/近N天/近N月)、neq(不等于)、eq(等于/是)、contains(包含)、excludes(不包含)。range的value约定：大于20→\"20,\"；小于20→\",20\"；X到Y之间→\"X, Y\"；近N天/近N月→毫秒时间戳区间\"ts1,ts2\"。fieldName直接用字段中文名，不做编码映射。")
    public String parseQuery(
            @ToolParam(description = "自然语言查询条件，例如：我想查询年龄在35到45岁区间，白细胞数量大于20的患者")
            String naturalQuery
    ) {
        String systemPrompt = buildSystemPrompt();
        Prompt prompt = new Prompt(
                new SystemMessage(systemPrompt),
                new UserMessage(naturalQuery)
        );
        ChatResponse response = chatModel.call(prompt);
        if (response.getResult() == null) {
            return "[]";
        }
        String text = response.getResult().getOutput().getText();
        System.out.println(text);
        return sanitizeJson(text);
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个查询条件解析器，负责将自然语言描述的患者查询条件转换为结构化 JSON 数组。").append("\n\n");

        // 当前日期与毫秒时间戳：每次调用现场计算
        sb.append("今天是 ").append(LocalDate.now()).append("，当前毫秒时间戳 now=").append(System.currentTimeMillis()).append("\n\n");

        sb.append("【输出格式】\n");
        sb.append("- 输出必须是 JSON 数组，每个元素形如 {\"fieldName\":\"<字段中文名>\",\"operation\":\"<算子>\",\"value\":\"<条件值>\"}。\n");
        sb.append("- value 统一为字符串；区间类条件 value 形如 \"lo, hi\"（小数允许保留两位小数）。\n");
        sb.append("- 不要输出 Markdown 代码块、解释文字或换行缩进以外内容，只输出纯 JSON 数组。如果解析不到任何条件，输出 []。\n\n");

        sb.append("【五算子 operation 字典】（自然语言关键词 → operation 值）\n");
        sb.append("- range    : 区间 / 大于 / 小于 / 近N天 / 近N月。关键词：大于、高于、超过、以上、小于、低于、以下、区间、…到…、…之间、…~…、近N天、近N月\n");
        sb.append("- neq      : 不等于。关键词：不等于、不是、非、除…外（排除单值）\n");
        sb.append("- eq       : 等于 / 是。关键词：等于、是、为、就是（含性别等枚举：男 / 女）\n");
        sb.append("- contains : 包含。关键词：包含、含有、出现、提到\n");
        sb.append("- excludes : 不包含。关键词：不包含、不含有、排除、剔除\n\n");

        sb.append("【range 算子的 value 约定】\n");
        sb.append("- 大于 X            → value = \"X,\"          （例：大于 20 → \"20,\"）\n");
        sb.append("- 小于 X            → value = \",X\"          （例：小于 20 → \",20\"）\n");
        sb.append("- X 到 Y 之间 / 区间  → value = \"X, Y\"        （例：35 到 45 → \"35, 45\"）\n");
        sb.append("- 近 N 天            → value = \"ts1,ts2\"     （ts1 = now − N×86400000，ts2 = now）\n");
        sb.append("- 近 N 月            → value = \"ts1,ts2\"     （ts1 = LocalDate.now().minusMonths(N) 当日 00:00:00 对应毫秒，ts2 = now）\n");
        sb.append("- 所有毫秒时间戳必须输出为 13 位数字字符串。\n\n");

        sb.append("【fieldName 规则】\n");
        sb.append("- fieldName 直接使用查询条件中出现的字段中文名（如 \"年龄\"、\"白细胞\"），不做任何短编码或别名映射。\n");
        sb.append("- value 保留原始数值 / 文本，但需去掉单位（如 \"35 到 45 岁\" → \"35, 45\"，不带 \"岁\"）。\n");
        sb.append("- 性别等枚举保持原文：男 / 女。\n\n");

        sb.append("【示例】\n");
        sb.append("输入：我想查询年龄在35到45岁区间，白细胞数量大于20的患者\n");
        sb.append("输出：[{\"fieldName\":\"年龄\",\"operation\":\"range\",\"value\":\"35, 45\"},{\"fieldName\":\"白细胞\",\"operation\":\"range\",\"value\":\"20,\"}]\n");
        return sb.toString();
    }

    private String sanitizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "[]";
        }
        String text = raw.trim();
        // 去除可能的 ```json ... ``` 包裹
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
            text = text.trim();
        }
        // 仅保留首个 '[' 到末个 ']' 的部分，避免解释性文字污染
        int l = text.indexOf('[');
        int r = text.lastIndexOf(']');
        if (l >= 0 && r > l) {
            text = text.substring(l, r + 1);
        }
        // 标准化中文逗号
        return text.replace("，", ",").trim();
    }
}
