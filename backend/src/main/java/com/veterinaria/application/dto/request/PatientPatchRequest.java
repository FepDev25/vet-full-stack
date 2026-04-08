package com.veterinaria.application.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.veterinaria.domain.enums.PatientSex;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record PatientPatchRequest(
        @Size(max = 100) String name,
        UUID breedId,
        LocalDate birthDate,
        PatientSex sex,
        @DecimalMin(value = "0", inclusive = false) BigDecimal weightKg,
        @Size(max = 100) String coatColor,
        @Size(max = 50) String microchipNumber,
        Boolean isSterilized
) {}
