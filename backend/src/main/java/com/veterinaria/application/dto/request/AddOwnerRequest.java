package com.veterinaria.application.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddOwnerRequest(
        @NotNull UUID clientId,
        Boolean isPrimaryOwner
) {}
