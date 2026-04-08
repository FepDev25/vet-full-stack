package com.veterinaria.application.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.veterinaria.domain.enums.StaffRole;

public record StaffResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String licenseNumber,
        StaffRole role,
        boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
