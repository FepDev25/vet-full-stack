package com.veterinaria.application.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OwnerResponse(
        UUID clientId,
        UUID patientId,
        boolean isPrimaryOwner,
        OffsetDateTime createdAt,
        String clientFirstName,
        String clientLastName,
        String clientEmail
) {}
