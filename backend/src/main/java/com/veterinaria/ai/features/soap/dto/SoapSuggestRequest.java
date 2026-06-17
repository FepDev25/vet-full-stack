package com.veterinaria.ai.features.soap.dto;

import jakarta.validation.constraints.NotNull;

public record SoapSuggestRequest(
        @NotNull String freeText,
        Boolean includePatientHistory
) {}
