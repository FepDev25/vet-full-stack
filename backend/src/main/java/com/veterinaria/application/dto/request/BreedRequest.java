package com.veterinaria.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BreedRequest(
        @NotBlank @Size(max = 100) String name
) {}
