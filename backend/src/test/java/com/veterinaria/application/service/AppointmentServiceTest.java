package com.veterinaria.application.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.application.dto.request.AppointmentRequest;
import com.veterinaria.application.dto.request.CancelRequest;
import com.veterinaria.application.dto.response.AppointmentResponse;
import com.veterinaria.domain.entity.Appointment;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Species;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.enums.AppointmentStatus;
import com.veterinaria.domain.enums.StaffRole;
import com.veterinaria.domain.repository.AppointmentRepository;
import com.veterinaria.domain.repository.PatientRepository;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepo;
    @Mock PatientRepository     patientRepo;
    @Mock StaffRepository       staffRepo;

    @InjectMocks AppointmentService service;

    private Patient patient;
    private Staff   vet;
    private Staff   assistant;

    @BeforeEach
    void setUp() {
        Species species = new Species();
        species.setId(UUID.randomUUID());
        species.setName("Perro");

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setName("Max");
        patient.setSpecies(species);

        vet = new Staff();
        vet.setId(UUID.randomUUID());
        vet.setFirstName("Carlos");
        vet.setLastName("Mendoza");
        vet.setRole(StaffRole.VETERINARIAN);
        vet.setActive(true);

        assistant = new Staff();
        assistant.setId(UUID.randomUUID());
        assistant.setFirstName("Pedro");
        assistant.setLastName("Ramírez");
        assistant.setRole(StaffRole.ASSISTANT);
        assistant.setActive(true);
    }

    // ── createAppointment ─────────────────────────────────────────────────────

    @Test
    void createAppointment_success() {
        AppointmentRequest req = new AppointmentRequest(
                patient.getId(), vet.getId(),
                OffsetDateTime.now().plusDays(1),
                "Revisión general", null);

        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId()))
                .thenReturn(Optional.of(patient));
        when(staffRepo.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(appointmentRepo.countConflictingAppointments(any(), any(), any(), any(), any()))
                .thenReturn(0L);

        Appointment saved = buildAppointment(AppointmentStatus.PENDING, req.scheduledAt());
        when(appointmentRepo.save(any())).thenReturn(saved);

        AppointmentResponse resp = service.createAppointment(req);

        assertThat(resp).isNotNull();
        assertThat(resp.status()).isEqualTo(AppointmentStatus.PENDING);
        verify(appointmentRepo).save(any());
    }

    @Test
    void createAppointment_patientNotFound_throwsResourceNotFoundException() {
        AppointmentRequest req = new AppointmentRequest(
                UUID.randomUUID(), vet.getId(),
                OffsetDateTime.now().plusDays(1), "Revisión", null);

        when(patientRepo.findByIdAndDeletedAtIsNull(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAppointment(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Paciente no encontrado");
    }

    @Test
    void createAppointment_staffNotVeterinarian_BR08_throws() {
        AppointmentRequest req = new AppointmentRequest(
                patient.getId(), assistant.getId(),
                OffsetDateTime.now().plusDays(1), "Revisión", null);

        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId()))
                .thenReturn(Optional.of(patient));
        when(staffRepo.findById(assistant.getId())).thenReturn(Optional.of(assistant));

        assertThatThrownBy(() -> service.createAppointment(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("VETERINARIAN");
    }

    @Test
    void createAppointment_staffInactive_BR08_throws() {
        vet.setActive(false);
        AppointmentRequest req = new AppointmentRequest(
                patient.getId(), vet.getId(),
                OffsetDateTime.now().plusDays(1), "Revisión", null);

        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId()))
                .thenReturn(Optional.of(patient));
        when(staffRepo.findById(vet.getId())).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> service.createAppointment(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-08");
    }

    @Test
    void createAppointment_pastDate_BR09_throws() {
        AppointmentRequest req = new AppointmentRequest(
                patient.getId(), vet.getId(),
                OffsetDateTime.now().minusHours(1), "Revisión", null);

        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId()))
                .thenReturn(Optional.of(patient));
        when(staffRepo.findById(vet.getId())).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> service.createAppointment(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-09");
    }

    @Test
    void createAppointment_conflictingSchedule_BR07_throws() {
        AppointmentRequest req = new AppointmentRequest(
                patient.getId(), vet.getId(),
                OffsetDateTime.now().plusDays(1), "Revisión", null);

        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId()))
                .thenReturn(Optional.of(patient));
        when(staffRepo.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(appointmentRepo.countConflictingAppointments(any(), any(), any(), any(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.createAppointment(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-07");
    }

    // ── State machine ─────────────────────────────────────────────────────────

    @Test
    void confirmAppointment_pending_becomesConfirmed() {
        Appointment appt = buildAppointment(AppointmentStatus.PENDING, OffsetDateTime.now().plusDays(1));
        when(appointmentRepo.findById(appt.getId())).thenReturn(Optional.of(appt));
        when(appointmentRepo.save(appt)).thenReturn(appt);

        AppointmentResponse resp = service.confirmAppointment(appt.getId());

        assertThat(resp.status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void confirmAppointment_alreadyConfirmed_throws() {
        Appointment appt = buildAppointment(AppointmentStatus.CONFIRMED, OffsetDateTime.now().plusDays(1));
        when(appointmentRepo.findById(appt.getId())).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> service.confirmAppointment(appt.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void startAppointment_confirmed_becomesInProgress() {
        Appointment appt = buildAppointment(AppointmentStatus.CONFIRMED, OffsetDateTime.now().plusDays(1));
        when(appointmentRepo.findById(appt.getId())).thenReturn(Optional.of(appt));
        when(appointmentRepo.save(appt)).thenReturn(appt);

        AppointmentResponse resp = service.startAppointment(appt.getId());

        assertThat(resp.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    void completeAppointment_inProgress_becomesCompleted() {
        Appointment appt = buildAppointment(AppointmentStatus.IN_PROGRESS, OffsetDateTime.now().plusDays(1));
        when(appointmentRepo.findById(appt.getId())).thenReturn(Optional.of(appt));
        when(appointmentRepo.save(appt)).thenReturn(appt);

        AppointmentResponse resp = service.completeAppointment(appt.getId());

        assertThat(resp.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void cancelAppointment_pending_becomesCancelled() {
        Appointment appt = buildAppointment(AppointmentStatus.PENDING, OffsetDateTime.now().plusDays(1));
        when(appointmentRepo.findById(appt.getId())).thenReturn(Optional.of(appt));
        when(appointmentRepo.save(appt)).thenReturn(appt);

        AppointmentResponse resp = service.cancelAppointment(appt.getId(), new CancelRequest("motivo"));

        assertThat(resp.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancelAppointment_completed_throws() {
        Appointment appt = buildAppointment(AppointmentStatus.COMPLETED, OffsetDateTime.now().minusDays(1));
        when(appointmentRepo.findById(appt.getId())).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> service.cancelAppointment(appt.getId(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PENDING o CONFIRMED");
    }

    @Test
    void markNoShow_confirmed_becomesNoShow() {
        Appointment appt = buildAppointment(AppointmentStatus.CONFIRMED, OffsetDateTime.now().plusDays(1));
        when(appointmentRepo.findById(appt.getId())).thenReturn(Optional.of(appt));
        when(appointmentRepo.save(appt)).thenReturn(appt);

        AppointmentResponse resp = service.markNoShow(appt.getId());

        assertThat(resp.status()).isEqualTo(AppointmentStatus.NO_SHOW);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Appointment buildAppointment(AppointmentStatus status, OffsetDateTime scheduledAt) {
        Appointment a = new Appointment();
        a.setId(UUID.randomUUID());
        a.setPatient(patient);
        a.setStaff(vet);
        a.setScheduledAt(scheduledAt);
        a.setStatus(status);
        a.setReason("Test reason");
        return a;
    }
}
