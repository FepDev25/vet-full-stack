package com.veterinaria.application.dto.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Size;

public record AppointmentPatchRequest(
        OffsetDateTime scheduledAt,
        @Size(max = 255) String reason,
        String notes
) {}
