package com.neusoftmedical.skill;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 查询条件解析Skill
 * 将自然语言描述的患者查询条件转换为结构化 JSON 数组，
 * 例如 "我想查询年龄在35到45岁区间，白细胞数量大于20的男性患者"
 * 转换为 [{"age":"35, 45","op":"range"},{"bxb":"20","op":"gt"},{"gender":"男","op":"equal"}]
 * <p>
 * 转换规则（字段中文名→短编码、运算符语义、输出结构）与业务强绑定，
 * 字段词典维护在 {@link #FIELD_DICT}，运算符在 {@link #OP_DICT}。
 */
@Component
public class QueryConditionParserSkill {

    /** 业务字段词典：中文名 / 常见同义词 → 后端存储字段短编码 */
    private static final Map<String, String> FIELD_DICT = buildFieldDict();

    /** 运算符词典：自然语言关键词 → 输出 op 值 */
    private static final Map<String, String> OP_DICT = buildOpDict();

    private final ChatModel chatModel;

    public QueryConditionParserSkill(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Skill：把自然语言查询条件解析成结构化 JSON 数组字符串。
     * 例如：我想查询年龄在35到45岁区间，白细胞数量大于20的男性患者
     * 返回：[{"age":"35, 45","op":"range"},{"bxb":"20","op":"gt"},{"gender":"男","op":"equal"}]
     *
     * @param naturalQuery 自然语言查询条件描述
     * @return 结构化 JSON 数组字符串
     */
    @Tool(description = "将自然语言描述的患者查询条件转换为结构化JSON数组。"
            + "每个数组元素形如 {\"fieldCode\":\"value\", \"op\":\"operationId\"}。"
            + "支持语义区间(greater than等中文表述)，会按内置业务词典将中文字段名映射为字段短编码。"
            + "输入为一句自然语言查询条件。")
    public String parseQueryCondition(
            @ToolParam(description = "自然语言查询条件，例如：我想查询年龄在35到45岁区间，白细胞数量大于20的男性患者")
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
        return sanitizeJson(text);
    }

    /**
     * 组装系统提示词：包含输出格式规范、运算符语义、业务字段词典。
     */
    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个查询条件解析器，负责将自然语言描述的患者查询条件转换为结构化 JSON 数组。").append("\n\n");

        sb.append("【输出格式】\n");
        sb.append("- 输出必须是 JSON 数组，每个元素形如 {\"<fieldCode>\":\"<value>\",\"op\":\"<opId>\"}。\n");
        sb.append("- value 是字符串；区间类条件 value 形如 \"lo, hi\"（小数允许保留两位小数）。\n");
        sb.append("- 不要输出 Markdown 代码块、解释文字或换行缩进以外内容，只输出纯 JSON 数组。如果解析不到条件，输出 []。\n\n");

        sb.append("【运算符 op 字典】（自然语言关键词 → op 值）\n");
        sb.append("- range  : 区间。例 35~45/35到45/35-45 → \"35, 45\"\n");
        sb.append("- gt     : 大于。关键词：大于、高于、超过\n");
        sb.append("- gte    : 大于等于。关键词：大于等于、不低于、至少\n");
        sb.append("- lt     : 小于。关键词：小于、低于\n");
        sb.append("- lte    : 小于等于。关键词：小于等于、不超过、至多\n");
        sb.append("- equal  : 等于。关键词：是、为、等于（含性别等枚举条件）\n");
        sb.append("- between: 两数之间（同 range，二选一即可，默认用 range）\n");
        sb.append("- contains: 文本包含。用于病程记录、影像报告等长文本条件\n\n");

        sb.append("【业务字段词典】（中文 / 常见同义词 → 字段短编码 fieldCode）\n");
        FIELD_DICT.forEach((k, v) -> sb.append("- ").append(k).append(" → ").append(v).append('\n'));
        sb.append('\n');
        sb.append("说明：自然语言出现的字段词需按上面的词典映射为 fieldCode；未命中词典的字段保留中文原文作为 fieldCode，并在 value 中给出原始数值/文本。性别男性输出 '男'、女性输出 '女'。年龄直接用 age 作为 fieldCode。\n");
        sb.append("【示例】\n");
        sb.append("输入：我想查询年龄在35到45岁区间，白细胞数量大于20的男性患者\n");
        sb.append("输出：[{\"age\":\"35, 45\",\"op\":\"range\"},{\"bxb\":\"20\",\"op\":\"gt\"},{\"gender\":\"男\",\"op\":\"equal\"}]");
        return sb.toString();
    }

    /**
     * 清洗模型输出：去除 Markdown 代码块包裹；剥离前后空白与非 JSON 段落。
     */
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

    /** 构建业务字段词典。可按业务需要持续扩展。 */
    private static Map<String, String> buildFieldDict() {
        Map<String, String> m = new LinkedHashMap<>();
        // 患者基础信息
        m.put("年龄", "age");
        m.put("性别", "gender");
        m.put("男性", "gender");
        m.put("女性", "gender");
        // 血细胞分析（急）
        m.put("白细胞", "bxb");
        m.put("白细胞计数", "bxb");
        m.put("红细胞", "hxb");
        m.put("红细胞计数", "hxb");
        m.put("血红蛋白", "hxdb");
        m.put("血红蛋白量", "hxdb");
        m.put("血小板", "xxb");
        m.put("血小板计数", "xxb");
        m.put("中性粒细胞", "zxgxb");
        m.put("中性粒细胞计数", "zxgxb");
        m.put("淋巴细胞", "lxb");
        m.put("淋巴细胞计数", "lxb");
        m.put("单核细胞", "dhxb");
        m.put("嗜酸性细胞", "ssxxb");
        m.put("嗜碱性细胞", "sjxxb");
        // 生化全套
        m.put("肌酐", "jga");
        m.put("尿素", "ns");
        m.put("尿酸", "nss");
        m.put("白蛋白", "bdb");
        m.put("总蛋白", "zdb");
        m.put("球蛋白", "qdb");
        m.put("总胆红素", "zrhs");
        m.put("直接胆红素", "zjrhs");
        m.put("间接胆红素", "jjrhs");
        m.put("钾", "jia");
        m.put("钠", "na");
        m.put("氯", "lv");
        m.put("钙", "gai");
        m.put("丙氨酸氨基转移酶", "bxa");
        m.put("谷丙转氨酶", "bxa");
        m.put("天冬氨酸氨基转移酶", "gsa");
        m.put("谷草转氨酶", "gsa");
        m.put("碱性磷酸酶", "jxlsme");
        m.put("谷氨酰转肽酶", "gxzzme");
        // 凝血
        m.put("纤维蛋白原", "xdb");
        m.put("D-二聚体", "DDE");
        m.put("凝血酶原时间", "nxyzsj");
        m.put("凝血酶时间", "nxsj");
        m.put("活化部分凝血活酶时间", "hdbfnhmsj");
        // 病程 / 影像
        m.put("检查所见", "jcsj");
        m.put("检查结论", "jcjl");
        m.put("检查项目", "jcxm");
        m.put("现病史", "xbs");
        m.put("既往病史", "jwbs");
        m.put("入院诊断", "rzdj");
        m.put("诊疗经过", "zljg");
        m.put("专科检查", "zkjc");
        return m;
    }

    /** 构建运算符词典（保留供内部一致性检查使用）。 */
    private static Map<String, String> buildOpDict() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("range", "区间 (lo, hi)");
        m.put("gt", "大于");
        m.put("gte", "大于等于");
        m.put("lt", "小于");
        m.put("lte", "小于等于");
        m.put("equal", "等于");
        m.put("between", "介于（与 range 等价）");
        m.put("contains", "包含");
        return m;
    }
}