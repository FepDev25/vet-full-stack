package com.veterinaria.ai.features.soap;

import com.veterinaria.domain.enums.DiagnosisSeverity;

public record SuggestedDiagnosis(
        String description,
        String cieCode,
        DiagnosisSeverity severity,
        boolean isPrimary,
        double confidence,
        String rationale
) {}
