package com.veterinaria.ai.features.soap.dto;

import java.util.UUID;

import com.veterinaria.ai.features.soap.SoapSuggestion;

public record SoapSuggestResponse(
        UUID interactionId,
        SoapSuggestion suggestion
) {}
