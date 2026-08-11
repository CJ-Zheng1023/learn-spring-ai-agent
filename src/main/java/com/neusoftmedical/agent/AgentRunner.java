package com.neusoftmedical.agent;

import com.neusoftmedical.skill.QueryParserSkill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.neusoftmedical.skill.MedicalDictSkill;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;

@Component
public class AgentRunner implements CommandLineRunner {

    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider mcpTools;
    private final MedicalDictSkill medicalDictSkill;
    private final QueryParserSkill queryParserSkill;

    public AgentRunner(ChatClient chatClient, SyncMcpToolCallbackProvider mcpTools, MedicalDictSkill medicalDictSkill, QueryParserSkill queryParserSkill) {
        this.chatClient = chatClient;
        this.mcpTools = mcpTools;
        this.medicalDictSkill = medicalDictSkill;
        this.queryParserSkill = queryParserSkill;
    }

    @Override
    public void run(String... args) {
        String arg= args[0];
        String result;
        if (arg.equals("t1")) {
            // 做好约束
            String systemPrompt = """
                请充分利用好工具，在查询和统计之前需要把检查类型名称和字段名称转换为所对应的索引编码。
                要求限定在database_1库中进行查询和统计。
                假如查询的是患者数据，返回内容限定在患者编号(patientNo)、患者姓名、性别和年龄。
                假如是统计类的查询，则只返回统计结果。
                严禁修改es数据，只可查询，只读不可写！
                """;
            // 真实场景中用户端输入的内容
            String userPrompt = """
                查询球蛋白数值在40到50区间，并且血小板计数在40到60之间的患者数据。
                """;
            /*String userPrompt = """
                查询患者r500的红细胞分布宽度的平均值、中位数、最大值和最小值。
                """;*/
            result = chatClient.prompt().tools(mcpTools, medicalDictSkill).system(systemPrompt).user(userPrompt).call().content();
        } else {
            // 做好约束
            String systemPrompt = """
                仅返回json结构数据。
                需要将json中的fieldName属性需要通过工具转成所对应的字段索引编码(fieldId)。
                """;
            // 真实场景中用户端输入的内容
            String userPrompt = """
                查询球蛋白数值在40到50区间，并且血小板计数在40到60之间，检查时间为近三个月的患者数据。
                """;
            result = chatClient.prompt().tools(queryParserSkill, medicalDictSkill).system(systemPrompt).user(userPrompt).call().content();
        }
        System.out.println("=========================");
        System.out.println(result);
        System.out.println("=========================");

        System.exit(0);
    }
}
