package com.veterinaria.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record InvoiceCreateRequest(
        @NotNull UUID clientId,
        UUID consultationId,
        @NotNull @DecimalMin("0") BigDecimal taxRate,
        String notes
) {}
