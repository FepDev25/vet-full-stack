package com.veterinaria.ai.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import com.veterinaria.ai.config.AiProperties;

public class AnthropicProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);
    private static final BigDecimal TOKENS_PER_MILLION = BigDecimal.valueOf(1_000_000);

    private final ChatModel chatModel;
    private final AiProperties.Pricing pricing;

    public AnthropicProvider(ChatModel chatModel, AiProperties.Pricing pricing) {
        this.chatModel = chatModel;
        this.pricing = pricing;
    }

    @Override
    public AiResponse complete(AiRequest request) {
        long start = System.currentTimeMillis();
        try {
            AnthropicChatOptions options = AnthropicChatOptions.builder()
                    .model(request.options().model())
                    .temperature(request.options().temperature())
                    .maxTokens(request.options().maxTokens())
                    .build();

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(request.systemPrompt()),
                    new UserMessage(request.userPrompt())
            ), options);

            ChatResponse response = chatModel.call(prompt);
            long latency = System.currentTimeMillis() - start;

            String text = response.getResult() != null && response.getResult().getOutput() != null
                    ? response.getResult().getOutput().getText()
                    : null;
            Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
            Integer promptTokens = usage != null ? usage.getPromptTokens() : null;
            Integer completionTokens = usage != null ? usage.getCompletionTokens() : null;
            BigDecimal cost = calculateCost(promptTokens, completionTokens);

            return AiResponse.success(text, promptTokens, completionTokens, cost, latency);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("Anthropic call failed after {}ms: {}", latency, e.getMessage());
            return AiResponse.failure(e.getMessage(), latency);
        }
    }

    private BigDecimal calculateCost(Integer promptTokens, Integer completionTokens) {
        if (promptTokens == null || completionTokens == null || pricing == null) {
            return null;
        }
        BigDecimal inCost = pricing.anthropicInputUsdPerMtok()
                .multiply(BigDecimal.valueOf(promptTokens))
                .divide(TOKENS_PER_MILLION, 6, RoundingMode.HALF_UP);
        BigDecimal outCost = pricing.anthropicOutputUsdPerMtok()
                .multiply(BigDecimal.valueOf(completionTokens))
                .divide(TOKENS_PER_MILLION, 6, RoundingMode.HALF_UP);
        return inCost.add(outCost);
    }
}
