package com.veterinaria.ai.provider;

public record AiOptions(
        String model,
        Double temperature,
        Integer maxTokens
) {}
