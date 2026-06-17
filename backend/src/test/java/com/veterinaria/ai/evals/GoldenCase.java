package com.veterinaria.ai.evals;

public record GoldenCase(
        String caseName,
        Input input,
        Expected expected
) {
    public record Input(
            String freeText,
            Boolean includePatientHistory
    ) {}

    public record Expected(
            Integer subjectiveMinLength,
            Integer objectiveMinLength,
            Integer assessmentMinLength,
            Integer planMinLength,
            Integer diagnosisMinCount,
            Integer diagnosisMaxCount,
            Boolean requirePrimary,
            Integer warningsMinCount,
            Boolean requireStandardDisclaimers
    ) {}
}
