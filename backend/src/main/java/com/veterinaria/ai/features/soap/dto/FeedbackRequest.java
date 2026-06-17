package com.veterinaria.ai.features.soap.dto;

import java.util.UUID;

import com.veterinaria.ai.audit.AiFeedback;

import jakarta.validation.constraints.NotNull;

public record FeedbackRequest(
        @NotNull UUID interactionId,
        @NotNull AiFeedback rating
) {}
