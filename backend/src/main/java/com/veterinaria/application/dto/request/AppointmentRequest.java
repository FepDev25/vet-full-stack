package com.veterinaria.application.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppointmentRequest(
        @NotNull UUID patientId,
        @NotNull UUID staffId,
        @NotNull OffsetDateTime scheduledAt,
        @NotBlank @Size(max = 255) String reason,
        String notes
) {}
