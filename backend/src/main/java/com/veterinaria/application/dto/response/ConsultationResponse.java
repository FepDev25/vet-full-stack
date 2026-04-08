package com.veterinaria.application.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ConsultationResponse(
        UUID id,
        UUID appointmentId,
        UUID patientId,
        String patientName,
        UUID staffId,
        String staffFullName,
        String anamnesis,
        String physicalExam,
        String treatmentPlan,
        BigDecimal weightKg,
        BigDecimal temperatureC,
        List<DiagnosisResponse> diagnoses,
        List<PrescriptionResponse> prescriptions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
