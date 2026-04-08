package com.veterinaria.application.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.veterinaria.domain.enums.PatientSex;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatientUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull UUID speciesId,
        UUID breedId,
        LocalDate birthDate,
        @NotNull PatientSex sex,
        @DecimalMin(value = "0", inclusive = false) BigDecimal weightKg,
        @Size(max = 100) String coatColor,
        @Size(max = 50) String microchipNumber,
        Boolean isSterilized
) {}
