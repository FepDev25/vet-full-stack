package com.veterinaria.ai.features.soap;

import java.util.UUID;

import com.veterinaria.domain.enums.ProductType;

public record ProductCatalogSnippet(
        UUID id,
        String name,
        ProductType type
) {}
