package com.veterinaria.application.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        UUID consultationId,
        UUID productId,
        String productName,
        String dosage,
        String frequency,
        Integer durationDays,
        String instructions,
        OffsetDateTime createdAt
) {}
