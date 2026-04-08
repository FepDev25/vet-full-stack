package com.veterinaria.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;

public record InvoicePatchRequest(
        UUID clientId,
        @DecimalMin("0") BigDecimal taxRate,
        String notes
) {}
