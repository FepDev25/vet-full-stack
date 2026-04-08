package com.veterinaria.application.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VaccinationResponse(
        UUID id,
        UUID patientId,
        UUID productId,
        String productName,
        UUID staffId,
        String staffFullName,
        LocalDate administeredAt,
        LocalDate nextDueDate,
        String batchNumber,
        OffsetDateTime createdAt
) {}
