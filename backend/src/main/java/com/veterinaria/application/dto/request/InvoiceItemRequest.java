package com.veterinaria.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvoiceItemRequest(
        UUID productId,
        @NotBlank @Size(max = 500) String description,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin("0") BigDecimal unitPrice
) {}
