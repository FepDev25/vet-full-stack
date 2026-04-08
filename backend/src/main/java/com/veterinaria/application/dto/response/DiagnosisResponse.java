package com.veterinaria.application.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.veterinaria.domain.enums.DiagnosisSeverity;

public record DiagnosisResponse(
        UUID id,
        UUID consultationId,
        String cieCode,
        String description,
        DiagnosisSeverity severity,
        boolean isPrimary,
        OffsetDateTime createdAt
) {}
