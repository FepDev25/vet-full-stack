package com.veterinaria.application.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ProductPatchRequest(
        @Size(max = 255) String name,
        String description,
        @Min(0) Integer stockQuantity,
        @DecimalMin("0") BigDecimal unitPrice,
        @DecimalMin("0") BigDecimal costPrice,
        @Min(0) Integer minStockAlert,
        Boolean requiresPrescription,
        Boolean isActive
) {}
