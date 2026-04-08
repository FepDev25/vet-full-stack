package com.veterinaria.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record VaccinationDueResponse(
        UUID vaccinationId,
        UUID patientId,
        String patientName,
        UUID productId,
        String productName,
        LocalDate nextDueDate,
        long daysRemaining
) {}
