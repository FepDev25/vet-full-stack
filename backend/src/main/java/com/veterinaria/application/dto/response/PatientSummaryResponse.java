package com.veterinaria.application.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.veterinaria.domain.enums.PatientSex;

public record PatientSummaryResponse(
        UUID id,
        String name,
        UUID speciesId,
        String speciesName,
        UUID breedId,
        String breedName,
        PatientSex sex,
        LocalDate birthDate,
        OffsetDateTime createdAt
) {}
