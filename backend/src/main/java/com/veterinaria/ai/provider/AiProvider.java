package com.veterinaria.ai.provider;

public interface AiProvider {
    AiResponse complete(AiRequest request);
}
