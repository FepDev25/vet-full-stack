package com.veterinaria.application.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.request.AppointmentRequest;
import com.veterinaria.application.dto.response.AppointmentResponse;
import com.veterinaria.domain.enums.AppointmentStatus;
import com.veterinaria.exception.BusinessRuleException;

/**
 * Tests de integración para AppointmentService.
 * Usa los seeds del perfil "test" (V4__test_seeds.sql).
 * @Transactional garantiza rollback automático tras cada test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentServiceIT {

    @Autowired AppointmentService service;

    // Seeds: Dr. Mendoza (VETERINARIAN)
    static final UUID VET_ID     = UUID.fromString("00000000-0003-0003-0003-000000000001");
    // Seeds: Max (paciente activo)
    static final UUID PATIENT_MAX = UUID.fromString("00000000-0005-0005-0005-000000000001");
    // Seeds: cita Rocky en estado CONFIRMED (futuro)
    static final UUID APPT_ROCKY_CONFIRMED = UUID.fromString("00000000-0008-0008-0008-000000000004");

    @Test
    void createAppointment_success_withSeedData() {
        AppointmentRequest req = new AppointmentRequest(
                PATIENT_MAX, VET_ID,
                OffsetDateTime.now().plusDays(30),
                "Control anual", null);

        AppointmentResponse resp = service.createAppointment(req);

        assertThat(resp.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(resp.patientId()).isEqualTo(PATIENT_MAX);
        assertThat(resp.staffId()).isEqualTo(VET_ID);
    }

    @Test
    void createAppointment_nonVetStaff_BR08_throws() {
        UUID assistantId = UUID.fromString("00000000-0003-0003-0003-000000000003");

        AppointmentRequest req = new AppointmentRequest(
                PATIENT_MAX, assistantId,
                OffsetDateTime.now().plusDays(5),
                "Revisión", null);

        assertThatThrownBy(() -> service.createAppointment(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("VETERINARIAN");
    }

    @Test
    void fullStateFlow_pending_confirmed_inProgress_completed() {
        // Crear cita nueva
        AppointmentRequest req = new AppointmentRequest(
                PATIENT_MAX, VET_ID,
                OffsetDateTime.now().plusDays(60),
                "Flujo completo", null);
        AppointmentResponse created = service.createAppointment(req);
        UUID apptId = created.id();

        // PENDING → CONFIRMED
        AppointmentResponse confirmed = service.confirmAppointment(apptId);
        assertThat(confirmed.status()).isEqualTo(AppointmentStatus.CONFIRMED);

        // CONFIRMED → IN_PROGRESS
        AppointmentResponse started = service.startAppointment(apptId);
        assertThat(started.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);

        // IN_PROGRESS → COMPLETED
        AppointmentResponse completed = service.completeAppointment(apptId);
        assertThat(completed.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void createAppointment_conflictWithExistingConfirmed_BR07_throws() {
        // Rocky ya tiene cita CONFIRMED a las 2026-03-28 09:00 UTC
        // Intentar otra cita al mismo tiempo para el mismo vet
        UUID patientLuna = UUID.fromString("00000000-0005-0005-0005-000000000002");
        OffsetDateTime conflicting = OffsetDateTime.parse("2026-03-28T09:00:00+00:00");

        AppointmentRequest req = new AppointmentRequest(
                patientLuna, VET_ID, conflicting, "Conflicto", null);

        assertThatThrownBy(() -> service.createAppointment(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-07");
    }
}
