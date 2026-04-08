package com.veterinaria.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.veterinaria.domain.enums.PatientSex;

public record PatientResponse(
        UUID id,
        String name,
        UUID speciesId,
        String speciesName,
        UUID breedId,
        String breedName,
        PatientSex sex,
        LocalDate birthDate,
        BigDecimal weightKg,
        String coatColor,
        String microchipNumber,
        boolean isSterilized,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
