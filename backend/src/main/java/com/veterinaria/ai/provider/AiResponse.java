package com.veterinaria.ai.provider;

import java.math.BigDecimal;

public record AiResponse(
        String text,
        Integer promptTokens,
        Integer completionTokens,
        BigDecimal costUsd,
        long latencyMs,
        String error,
        boolean success
) {
    public static AiResponse success(String text,
                                     Integer promptTokens,
                                     Integer completionTokens,
                                     BigDecimal costUsd,
                                     long latencyMs) {
        return new AiResponse(text, promptTokens, completionTokens, costUsd, latencyMs, null, true);
    }

    public static AiResponse failure(String error, long latencyMs) {
        return new AiResponse(null, null, null, null, latencyMs, error, false);
    }
}
