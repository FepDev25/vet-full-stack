package com.veterinaria.application.dto.request;

import com.veterinaria.domain.enums.DiagnosisSeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiagnosisRequest(
        @Size(max = 20) String cieCode,
        @NotBlank @Size(max = 500) String description,
        @NotNull DiagnosisSeverity severity,
        @NotNull Boolean isPrimary
) {}
