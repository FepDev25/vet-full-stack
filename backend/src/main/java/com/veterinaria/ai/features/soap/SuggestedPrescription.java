package com.veterinaria.ai.features.soap;

import java.util.UUID;

public record SuggestedPrescription(
        UUID productId,
        String productNameHint,
        String dosage,
        String frequency,
        Integer durationDays,
        String instructions
) {}
