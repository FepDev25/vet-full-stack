package com.veterinaria.application.dto.request;

import com.veterinaria.domain.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record PayInvoiceRequest(@NotNull PaymentMethod paymentMethod) {}
