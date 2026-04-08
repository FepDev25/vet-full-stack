package com.veterinaria.application.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.veterinaria.domain.enums.InvoiceStatus;
import com.veterinaria.domain.enums.PaymentMethod;

public record InvoiceResponse(
        UUID id,
        UUID clientId,
        String clientFullName,
        UUID consultationId,
        InvoiceStatus status,
        BigDecimal subtotal,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal total,
        PaymentMethod paymentMethod,
        String notes,
        OffsetDateTime issuedAt,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
