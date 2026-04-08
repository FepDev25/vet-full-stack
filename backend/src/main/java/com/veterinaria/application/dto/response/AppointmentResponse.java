package com.veterinaria.application.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.veterinaria.domain.enums.AppointmentStatus;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        String patientName,
        UUID staffId,
        String staffFullName,
        OffsetDateTime scheduledAt,
        AppointmentStatus status,
        String reason,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
