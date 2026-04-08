package com.veterinaria.application.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VaccinationRequest(
        @NotNull UUID patientId,
        @NotNull UUID productId,
        @NotNull UUID staffId,
        @NotNull LocalDate administeredAt,
        LocalDate nextDueDate,
        @NotBlank @Size(max = 100) String batchNumber
) {}
