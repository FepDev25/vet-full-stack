package com.veterinaria.application.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.veterinaria.domain.enums.ProductType;

public record ProductResponse(
        UUID id,
        String name,
        ProductType type,
        String description,
        String sku,
        Integer stockQuantity,
        BigDecimal unitPrice,
        BigDecimal costPrice,
        Integer minStockAlert,
        boolean requiresPrescription,
        boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
