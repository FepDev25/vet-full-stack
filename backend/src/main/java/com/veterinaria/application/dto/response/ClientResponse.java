package com.veterinaria.application.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
