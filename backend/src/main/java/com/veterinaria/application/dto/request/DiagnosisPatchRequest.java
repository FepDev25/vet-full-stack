package com.veterinaria.application.dto.request;

import com.veterinaria.domain.enums.DiagnosisSeverity;

import jakarta.validation.constraints.Size;

public record DiagnosisPatchRequest(
        @Size(max = 20) String cieCode,
        @Size(max = 500) String description,
        DiagnosisSeverity severity,
        Boolean isPrimary
) {}
