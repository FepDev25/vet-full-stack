package com.veterinaria.ai.evals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.yaml.snakeyaml.Yaml;

import com.veterinaria.ai.features.soap.SoapAssistantService;
import com.veterinaria.ai.features.soap.SoapSuggestion;
import com.veterinaria.ai.features.soap.SuggestedDiagnosis;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "evals", matches = "true")
class SoapAssistantEvalIT {

    @Autowired
    private SoapAssistantService service;

    private static final UUID CONSULTATION_ID = UUID.fromString("00000000-0009-0009-0009-000000000001");
    private static final UUID VET_ID = UUID.fromString("00000000-0003-0003-0003-000000000001");

    private static final String[] CASE_FILES = {
            "case_01_gastroenteritis.yaml",
            "case_02_fractura.yaml",
            "case_03_control_sano.yaml",
            "case_04_dermatitis.yaml",
            "case_05_vacunacion.yaml"
    };

    static Stream<Arguments> goldenCases() {
        return Stream.of(CASE_FILES).map(SoapAssistantEvalIT::loadGoldenCase).map(Arguments::of);
    }

    @SuppressWarnings("unchecked")
    private static GoldenCase loadGoldenCase(String filename) {
        Yaml yaml = new Yaml();
        try (InputStream is = SoapAssistantEvalIT.class.getClassLoader()
                .getResourceAsStream("evals/soap/" + filename)) {
            if (is == null) throw new IllegalStateException("YAML not found: " + filename);
            Map<String, Object> raw = yaml.load(is);
            String caseName = (String) raw.get("caseName");
            Map<String, Object> inputRaw = (Map<String, Object>) raw.get("input");
            GoldenCase.Input input = new GoldenCase.Input(
                    (String) inputRaw.get("freeText"),
                    (Boolean) inputRaw.get("includePatientHistory")
            );
            Map<String, Object> expRaw = (Map<String, Object>) raw.get("expected");
            GoldenCase.Expected expected = new GoldenCase.Expected(
                    asInteger(expRaw.get("subjectiveMinLength")),
                    asInteger(expRaw.get("objectiveMinLength")),
                    asInteger(expRaw.get("assessmentMinLength")),
                    asInteger(expRaw.get("planMinLength")),
                    asInteger(expRaw.get("diagnosisMinCount")),
                    asInteger(expRaw.get("diagnosisMaxCount")),
                    (Boolean) expRaw.get("requirePrimary"),
                    asInteger(expRaw.get("warningsMinCount")),
                    (Boolean) expRaw.get("requireStandardDisclaimers")
            );
            return new GoldenCase(caseName, input, expected);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + filename, e);
        }
    }

    private static Integer asInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Integer i) return i;
        if (o instanceof Long l) return l.intValue();
        return Integer.valueOf(o.toString());
    }

    @ParameterizedTest
    @MethodSource("goldenCases")
    void eval_goldenCase_meetsExpectations(GoldenCase golden) {
        SoapAssistantService.SoapSuggestionResult result = service.suggest(
                CONSULTATION_ID,
                golden.input().freeText(),
                Boolean.TRUE.equals(golden.input().includePatientHistory()),
                VET_ID
        );

        assertNotNull(result, "Result null for: " + golden.caseName());
        assertNotNull(result.suggestion(), "Suggestion null for: " + golden.caseName());

        SoapSuggestion s = result.suggestion();
        GoldenCase.Expected exp = golden.expected();
        String label = golden.caseName();

        assertMinLength(s.subjective(), exp.subjectiveMinLength(), "subjective", label);
        assertMinLength(s.objective(), exp.objectiveMinLength(), "objective", label);
        assertMinLength(s.assessment(), exp.assessmentMinLength(), "assessment", label);
        assertMinLength(s.plan(), exp.planMinLength(), "plan", label);

        int dxCount = s.suggestedDiagnoses() != null ? s.suggestedDiagnoses().size() : 0;
        if (exp.diagnosisMinCount() != null) {
            assertTrue(dxCount >= exp.diagnosisMinCount(),
                    label + ": diagnoses=" + dxCount + " < min=" + exp.diagnosisMinCount());
        }
        if (exp.diagnosisMaxCount() != null) {
            assertTrue(dxCount <= exp.diagnosisMaxCount(),
                    label + ": diagnoses=" + dxCount + " > max=" + exp.diagnosisMaxCount());
        }

        if (Boolean.TRUE.equals(exp.requirePrimary())) {
            long primaryCount = s.suggestedDiagnoses() != null
                    ? s.suggestedDiagnoses().stream().filter(SuggestedDiagnosis::isPrimary).count()
                    : 0;
            assertEquals(1, primaryCount, label + ": must have exactly 1 primary diagnosis, got " + primaryCount);
        }

        if (exp.warningsMinCount() != null) {
            int warnCount = s.warnings() != null ? s.warnings().size() : 0;
            assertTrue(warnCount >= exp.warningsMinCount(),
                    label + ": warnings=" + warnCount + " < min=" + exp.warningsMinCount());
        }

        if (Boolean.TRUE.equals(exp.requireStandardDisclaimers())) {
            assertNotNull(s.disclaimers(), label + ": disclaimers null");
            boolean hasValidationDisclaimer = s.disclaimers().stream()
                    .anyMatch(d -> d.contains("validada por un veterinario"));
            if (!hasValidationDisclaimer) {
                fail(label + ": missing standard disclaimer about validation");
            }
        }
    }

    private static void assertMinLength(String value, Integer minLength, String field, String label) {
        if (minLength == null) return;
        assertNotNull(value, label + ": " + field + " is null");
        assertTrue(value.length() >= minLength,
                label + ": " + field + " length=" + value.length() + " < min=" + minLength);
    }
}
