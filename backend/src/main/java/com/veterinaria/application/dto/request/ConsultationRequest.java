package com.veterinaria.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsultationRequest(
        @NotNull UUID appointmentId,
        @NotNull UUID staffId,
        String anamnesis,
        String physicalExam,
        String treatmentPlan,
        @Positive BigDecimal weightKg,
        @DecimalMin("30.0") @DecimalMax("45.0") BigDecimal temperatureC
) {}
