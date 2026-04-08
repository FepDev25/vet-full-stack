package com.veterinaria.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        UUID invoiceId,
        UUID productId,
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
