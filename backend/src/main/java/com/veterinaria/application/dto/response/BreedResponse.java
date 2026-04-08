package com.veterinaria.application.dto.response;

import java.util.UUID;

public record BreedResponse(UUID id, UUID speciesId, String name) {}
