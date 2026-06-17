package com.veterinaria.ai.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.veterinaria.ai.provider.AiProvider;
import com.veterinaria.ai.provider.AnthropicProvider;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    @ConditionalOnMissingBean(AiProvider.class)
    public AiProvider anthropicProvider(ChatModel chatModel, AiProperties properties) {
        return new AnthropicProvider(chatModel, properties.pricing());
    }
}
