package com.veterinaria.application.dto.request;

import java.math.BigDecimal;

import com.veterinaria.domain.enums.ProductType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull ProductType type,
        String description,
        @NotBlank @Size(max = 100) String sku,
        @Min(0) Integer stockQuantity,
        @NotNull @DecimalMin("0") BigDecimal unitPrice,
        @DecimalMin("0") BigDecimal costPrice,
        @Min(0) Integer minStockAlert,
        Boolean requiresPrescription
) {}
