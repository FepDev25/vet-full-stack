package com.veterinaria.ai.features.soap;

import java.time.LocalDate;
import java.util.List;

public record PatientHistorySnippet(
        LocalDate date,
        String anamnesisSnippet,
        List<String> diagnoses,
        List<String> prescriptions
) {}
