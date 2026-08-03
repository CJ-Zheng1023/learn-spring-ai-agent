package com.neusoftmedical.config;

import com.neusoftmedical.skill.MedicalDictSkill;
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
            SyncMcpToolCallbackProvider mcpTools, MedicalDictSkill medicalDictSkill) {

        return ChatClient.builder(chatModel)
                .defaultTools(mcpTools, medicalDictSkill)
                .build();
    }
}
