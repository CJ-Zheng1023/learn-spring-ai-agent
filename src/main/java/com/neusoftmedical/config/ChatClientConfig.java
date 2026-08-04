package com.neusoftmedical.config;

import com.neusoftmedical.skill.MedicalDictSkill;
import com.neusoftmedical.skill.QueryConditionParserSkill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(
            ChatModel chatModel,
            SyncMcpToolCallbackProvider mcpTools,
            MedicalDictSkill medicalDictSkill,
            QueryConditionParserSkill queryConditionParserSkill) {

        return ChatClient.builder(chatModel)
                .defaultTools(mcpTools, medicalDictSkill, queryConditionParserSkill)
                .build();
    }
}
