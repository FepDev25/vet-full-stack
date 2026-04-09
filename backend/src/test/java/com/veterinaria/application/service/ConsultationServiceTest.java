package com.veterinaria.application.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.application.dto.request.ConsultationPatchRequest;
import com.veterinaria.application.dto.request.ConsultationRequest;
import com.veterinaria.application.dto.request.DiagnosisRequest;
import com.veterinaria.application.dto.response.ConsultationResponse;
import com.veterinaria.application.dto.response.DiagnosisResponse;
import com.veterinaria.domain.entity.Appointment;
import com.veterinaria.domain.entity.Consultation;
import com.veterinaria.domain.entity.Diagnosis;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Species;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.enums.AppointmentStatus;
import com.veterinaria.domain.enums.DiagnosisSeverity;
import com.veterinaria.domain.enums.StaffRole;
import com.veterinaria.domain.repository.AppointmentRepository;
import com.veterinaria.domain.repository.ConsultationRepository;
import com.veterinaria.domain.repository.DiagnosisRepository;
import com.veterinaria.domain.repository.InvoiceRepository;
import com.veterinaria.domain.repository.PrescriptionRepository;
import com.veterinaria.domain.repository.ProductRepository;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ConflictException;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock ConsultationRepository consultationRepo;
    @Mock DiagnosisRepository    diagnosisRepo;
    @Mock PrescriptionRepository prescriptionRepo;
    @Mock AppointmentRepository  appointmentRepo;
    @Mock StaffRepository        staffRepo;
    @Mock ProductRepository      productRepo;
    @Mock InvoiceRepository      invoiceRepo;

    @InjectMocks ConsultationService service;

    private Staff       vet;
    private Staff       assistant;
    private Patient     patient;
    private Appointment completedAppt;
    private Appointment pendingAppt;
    private Consultation consultation;

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

        completedAppt = new Appointment();
        completedAppt.setId(UUID.randomUUID());
        completedAppt.setPatient(patient);
        completedAppt.setStaff(vet);
        completedAppt.setStatus(AppointmentStatus.COMPLETED);
        completedAppt.setScheduledAt(OffsetDateTime.now().minusHours(2));
        completedAppt.setReason("Revisión");

        pendingAppt = new Appointment();
        pendingAppt.setId(UUID.randomUUID());
        pendingAppt.setPatient(patient);
        pendingAppt.setStaff(vet);
        pendingAppt.setStatus(AppointmentStatus.PENDING);
        pendingAppt.setScheduledAt(OffsetDateTime.now().plusDays(1));
        pendingAppt.setReason("Control");

        consultation = new Consultation();
        consultation.setId(UUID.randomUUID());
        consultation.setAppointment(completedAppt);
        consultation.setStaff(vet);
        consultation.setWeightKg(BigDecimal.valueOf(32.0));
        consultation.setTemperatureC(BigDecimal.valueOf(38.5));
    }

    // ── createConsultation ────────────────────────────────────────────────────

    @Test
    void createConsultation_success() {
        when(appointmentRepo.findById(completedAppt.getId())).thenReturn(Optional.of(completedAppt));
        when(consultationRepo.existsByAppointmentId(completedAppt.getId())).thenReturn(false);
        when(staffRepo.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(consultationRepo.save(any())).thenReturn(consultation);
        when(diagnosisRepo.findByConsultationId(consultation.getId())).thenReturn(List.of());
        when(prescriptionRepo.findByConsultationId(consultation.getId())).thenReturn(List.of());

        ConsultationRequest req = new ConsultationRequest(
                completedAppt.getId(), vet.getId(),
                "Anamnesis", "Examen", "Plan",
                BigDecimal.valueOf(32.0), BigDecimal.valueOf(38.5));

        ConsultationResponse resp = service.createConsultation(req);

        assertThat(resp).isNotNull();
        verify(consultationRepo).save(any());
    }

    @Test
    void createConsultation_appointmentNotCompleted_BR06_throws() {
        when(appointmentRepo.findById(pendingAppt.getId())).thenReturn(Optional.of(pendingAppt));

        ConsultationRequest req = new ConsultationRequest(
                pendingAppt.getId(), vet.getId(), null, null, null, null, null);

        assertThatThrownBy(() -> service.createConsultation(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-06");
    }

    @Test
    void createConsultation_alreadyExists_BR10_throws() {
        when(appointmentRepo.findById(completedAppt.getId())).thenReturn(Optional.of(completedAppt));
        when(consultationRepo.existsByAppointmentId(completedAppt.getId())).thenReturn(true);

        ConsultationRequest req = new ConsultationRequest(
                completedAppt.getId(), vet.getId(), null, null, null, null, null);

        assertThatThrownBy(() -> service.createConsultation(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-10");
    }

    @Test
    void createConsultation_staffNotVet_BR08_throws() {
        when(appointmentRepo.findById(completedAppt.getId())).thenReturn(Optional.of(completedAppt));
        when(consultationRepo.existsByAppointmentId(completedAppt.getId())).thenReturn(false);
        when(staffRepo.findById(assistant.getId())).thenReturn(Optional.of(assistant));

        ConsultationRequest req = new ConsultationRequest(
                completedAppt.getId(), assistant.getId(), null, null, null, null, null);

        assertThatThrownBy(() -> service.createConsultation(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-08");
    }

    // ── addDiagnosis ──────────────────────────────────────────────────────────

    @Test
    void addDiagnosis_success() {
        when(consultationRepo.findById(consultation.getId())).thenReturn(Optional.of(consultation));
        when(diagnosisRepo.existsByConsultationIdAndIsPrimaryTrue(consultation.getId())).thenReturn(false);

        Diagnosis diag = buildDiagnosis(consultation, true);
        when(diagnosisRepo.save(any())).thenReturn(diag);

        DiagnosisRequest req = new DiagnosisRequest("H60.3", "Otitis externa", DiagnosisSeverity.MODERATE, true);
        DiagnosisResponse resp = service.addDiagnosis(consultation.getId(), req);

        assertThat(resp.isPrimary()).isTrue();
    }

    @Test
    void addDiagnosis_duplicatePrimary_BR13_throws() {
        when(consultationRepo.findById(consultation.getId())).thenReturn(Optional.of(consultation));
        when(diagnosisRepo.existsByConsultationIdAndIsPrimaryTrue(consultation.getId())).thenReturn(true);

        DiagnosisRequest req = new DiagnosisRequest(null, "Segundo diagnóstico primario", DiagnosisSeverity.MILD, true);

        assertThatThrownBy(() -> service.addDiagnosis(consultation.getId(), req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-13");
    }

    @Test
    void addDiagnosis_nonPrimary_withExistingPrimary_success() {
        when(consultationRepo.findById(consultation.getId())).thenReturn(Optional.of(consultation));
        // ya existe un primario, pero este no es primario → no debe verificar
        Diagnosis diag = buildDiagnosis(consultation, false);
        when(diagnosisRepo.save(any())).thenReturn(diag);

        DiagnosisRequest req = new DiagnosisRequest("L08.9", "Pioderma secundaria", DiagnosisSeverity.MILD, false);
        DiagnosisResponse resp = service.addDiagnosis(consultation.getId(), req);

        assertThat(resp.isPrimary()).isFalse();
        verify(diagnosisRepo, never()).existsByConsultationIdAndIsPrimaryTrue(any());
    }

    // ── updateConsultation ────────────────────────────────────────────────────

    @Test
    void updateConsultation_lockedByPaidInvoice_BR11_throws() {
        when(consultationRepo.findById(consultation.getId())).thenReturn(Optional.of(consultation));
        when(invoiceRepo.existsPaidInvoiceForConsultation(consultation.getId())).thenReturn(true);

        ConsultationPatchRequest req = new ConsultationPatchRequest("nuevo anamnesis", null, null, null, null);

        assertThatThrownBy(() -> service.updateConsultation(consultation.getId(), req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-11");
    }

    @Test
    void updateConsultation_notLocked_success() {
        when(consultationRepo.findById(consultation.getId())).thenReturn(Optional.of(consultation));
        when(invoiceRepo.existsPaidInvoiceForConsultation(consultation.getId())).thenReturn(false);
        when(consultationRepo.save(any())).thenReturn(consultation);
        when(diagnosisRepo.findByConsultationId(consultation.getId())).thenReturn(List.of());
        when(prescriptionRepo.findByConsultationId(consultation.getId())).thenReturn(List.of());

        ConsultationPatchRequest req = new ConsultationPatchRequest("nuevo anamnesis", null, null, null, null);
        ConsultationResponse resp = service.updateConsultation(consultation.getId(), req);

        assertThat(resp).isNotNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Diagnosis buildDiagnosis(Consultation c, boolean isPrimary) {
        Diagnosis d = new Diagnosis();
        d.setId(UUID.randomUUID());
        d.setConsultation(c);
        d.setCieCode("H60.3");
        d.setDescription("Diagnóstico de prueba");
        d.setSeverity(DiagnosisSeverity.MODERATE);
        d.setPrimary(isPrimary);
        d.setCreatedAt(OffsetDateTime.now());
        return d;
    }
}
