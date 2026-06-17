package com.veterinaria.ai.provider;

import java.math.BigDecimal;

public class NoOpProvider implements AiProvider {

    private final String fixedText;
    private final long simulatedLatencyMs;

    public NoOpProvider() {
        this("{}", 0L);
    }

    public NoOpProvider(String fixedText, long simulatedLatencyMs) {
        this.fixedText = fixedText;
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    @Override
    public AiResponse complete(AiRequest request) {
        return AiResponse.success(fixedText, 100, 50, new BigDecimal("0.000150"), simulatedLatencyMs);
    }
}
