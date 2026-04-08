package com.veterinaria.application.dto.response;

import java.util.UUID;

import com.veterinaria.domain.enums.ProductType;

public record LowStockAlertResponse(
        UUID productId,
        String name,
        String sku,
        ProductType type,
        int stockQuantity,
        int minStockAlert
) {}
