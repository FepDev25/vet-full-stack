package com.veterinaria.ai.provider;

public record AiRequest(
        String systemPrompt,
        String userPrompt,
        AiOptions options
) {}
