package com.veterinaria.ai.features.soap;

import java.util.List;

public record SoapSuggestion(
        String subjective,
        String objective,
        String assessment,
        String plan,
        List<SuggestedDiagnosis> suggestedDiagnoses,
        List<SuggestedPrescription> suggestedPrescriptions,
        List<String> warnings,
        String followUp,
        List<String> disclaimers
) {}
