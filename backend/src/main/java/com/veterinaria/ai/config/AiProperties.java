package com.veterinaria.ai.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public record AiProperties(Pricing pricing) {

    public record Pricing(
            BigDecimal anthropicInputUsdPerMtok,
            BigDecimal anthropicOutputUsdPerMtok
    ) {}
}
