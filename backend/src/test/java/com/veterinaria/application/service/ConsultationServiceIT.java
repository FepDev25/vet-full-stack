package com.veterinaria.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.request.ConsultationPatchRequest;
import com.veterinaria.application.dto.request.ConsultationRequest;
import com.veterinaria.application.dto.request.DiagnosisRequest;
import com.veterinaria.application.dto.response.ConsultationResponse;
import com.veterinaria.application.dto.response.DiagnosisResponse;
import com.veterinaria.domain.enums.DiagnosisSeverity;
import com.veterinaria.exception.ConflictException;

/**
 * Tests de integración para ConsultationService.
 * Utiliza datos del V4__test_seeds.sql.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsultationServiceIT {

    @Autowired ConsultationService service;
    @Autowired AppointmentService  appointmentService;

    // Seeds
    static final UUID VET_ID            = UUID.fromString("00000000-0003-0003-0003-000000000001");
    static final UUID APPT_LUNA         = UUID.fromString("00000000-0008-0008-0008-000000000002"); // COMPLETED, sin consulta
    static final UUID CONSULTATION_MAX  = UUID.fromString("00000000-0009-0009-0009-000000000001"); // tiene invoice PAID
    static final UUID CONSULTATION_LUNA = UUID.fromString("00000000-0009-0009-0009-000000000002"); // sin factura

    @Test
    void createConsultation_BR06_appointmentMustBeCompleted() {
        // Cita Rocky está en CONFIRMED → no permite crear consulta
        UUID confirmedAppt = UUID.fromString("00000000-0008-0008-0008-000000000004");

        ConsultationRequest req = new ConsultationRequest(
                confirmedAppt, VET_ID, "Anamnesis", null, null, null, null);

        assertThatThrownBy(() -> service.createConsultation(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-06");
    }

    @Test
    void createConsultation_BR10_duplicateForSameAppointment() {
        // Consulta de Max ya existe en seeds
        UUID apptMax = UUID.fromString("00000000-0008-0008-0008-000000000001");

        ConsultationRequest req = new ConsultationRequest(
                apptMax, VET_ID, "Segunda consulta", null, null, null, null);

        assertThatThrownBy(() -> service.createConsultation(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-10");
    }

    @Test
    void updateConsultation_BR11_lockedByPaidInvoice() {
        // MAX tiene invoice PAID → consulta bloqueada
        ConsultationPatchRequest req = new ConsultationPatchRequest(
                "nuevo anamnesis", null, null, null, null);

        assertThatThrownBy(() -> service.updateConsultation(CONSULTATION_MAX, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-11");
    }

    @Test
    void updateConsultation_notLocked_success() {
        // Luna no tiene factura → editable
        ConsultationPatchRequest req = new ConsultationPatchRequest(
                "Anamnesis actualizada", "Examen actualizado", null,
                BigDecimal.valueOf(4.30), null);

        ConsultationResponse resp = service.updateConsultation(CONSULTATION_LUNA, req);

        assertThat(resp.anamnesis()).isEqualTo("Anamnesis actualizada");
    }

    @Test
    void addDiagnosis_BR13_duplicatePrimaryThrows() {
        // Luna ya tiene un diagnóstico primario
        DiagnosisRequest req = new DiagnosisRequest(
                null, "Segundo diagnóstico primario", DiagnosisSeverity.MILD, true);

        assertThatThrownBy(() -> service.addDiagnosis(CONSULTATION_LUNA, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-13");
    }

    @Test
    void addDiagnosis_nonPrimary_success() {
        DiagnosisRequest req = new DiagnosisRequest(
                "L08.9", "Diagnóstico secundario", DiagnosisSeverity.MILD, false);

        DiagnosisResponse resp = service.addDiagnosis(CONSULTATION_LUNA, req);

        assertThat(resp.isPrimary()).isFalse();
        assertThat(resp.description()).isEqualTo("Diagnóstico secundario");
    }
}
