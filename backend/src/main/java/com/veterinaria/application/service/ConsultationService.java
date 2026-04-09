package com.veterinaria.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.page.ConsultationPage;
import com.veterinaria.application.dto.page.PageMeta;
import com.veterinaria.application.dto.request.ConsultationPatchRequest;
import com.veterinaria.application.dto.request.ConsultationRequest;
import com.veterinaria.application.dto.request.DiagnosisPatchRequest;
import com.veterinaria.application.dto.request.DiagnosisRequest;
import com.veterinaria.application.dto.request.PrescriptionRequest;
import com.veterinaria.application.dto.response.ConsultationResponse;
import com.veterinaria.application.dto.response.DiagnosisResponse;
import com.veterinaria.application.dto.response.PrescriptionResponse;
import com.veterinaria.domain.entity.Appointment;
import com.veterinaria.domain.entity.Consultation;
import com.veterinaria.domain.entity.Diagnosis;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Prescription;
import com.veterinaria.domain.entity.Product;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.enums.AppointmentStatus;
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
import com.veterinaria.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ConsultationService {

    private final ConsultationRepository consultationRepo;
    private final DiagnosisRepository    diagnosisRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final AppointmentRepository  appointmentRepo;
    private final StaffRepository        staffRepo;
    private final ProductRepository      productRepo;
    private final InvoiceRepository      invoiceRepo;

    public ConsultationService(ConsultationRepository consultationRepo,
                               DiagnosisRepository diagnosisRepo,
                               PrescriptionRepository prescriptionRepo,
                               AppointmentRepository appointmentRepo,
                               StaffRepository staffRepo,
                               ProductRepository productRepo,
                               InvoiceRepository invoiceRepo) {
        this.consultationRepo  = consultationRepo;
        this.diagnosisRepo     = diagnosisRepo;
        this.prescriptionRepo  = prescriptionRepo;
        this.appointmentRepo   = appointmentRepo;
        this.staffRepo         = staffRepo;
        this.productRepo       = productRepo;
        this.invoiceRepo       = invoiceRepo;
    }

    // CONSULTATION

    // listar consultas de un paciente (paginado)
    public ConsultationPage listByPatient(UUID patientId, Pageable pageable) {
        Page<Consultation> page = consultationRepo.findByPatientId(patientId, pageable);
        List<ConsultationResponse> content = page.getContent().stream()
                .map(this::toResponse).toList();
        return new ConsultationPage(content, toPageMeta(page));
    }

    // obtener consulta con detalles
    public ConsultationResponse getConsultation(UUID id) {
        Consultation c = consultationRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("CONSULTATION_NOT_FOUND",
                        "Consulta no encontrada: " + id));
        return toResponse(c);
    }

    // crear consulta
    @Transactional
    public ConsultationResponse createConsultation(ConsultationRequest req) {
        Appointment appt = appointmentRepo.findById(req.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("APPOINTMENT_NOT_FOUND",
                        "Cita no encontrada: " + req.appointmentId()));

        // BR-06: la cita debe estar en estado COMPLETED
        if (appt.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ConflictException("APPOINTMENT_NOT_COMPLETED",
                    "Solo se puede crear una consulta para una cita en estado COMPLETED (BR-06)");
        }

        // BR-10: una cita no puede tener más de una consulta
        if (consultationRepo.existsByAppointmentId(req.appointmentId())) {
            throw new ConflictException("CONSULTATION_ALREADY_EXISTS",
                    "Ya existe una consulta para esta cita (BR-10)");
        }

        Staff staff = staffRepo.findById(req.staffId())
                .orElseThrow(() -> new ResourceNotFoundException("STAFF_NOT_FOUND",
                        "Personal no encontrado: " + req.staffId()));

        // BR-08: solo VETERINARIAN activo puede realizar consultas
        validateVeterinarianActive(staff);

        Consultation consultation = new Consultation();
        consultation.setAppointment(appt);
        consultation.setStaff(staff);
        consultation.setAnamnesis(req.anamnesis());
        consultation.setPhysicalExam(req.physicalExam());
        consultation.setTreatmentPlan(req.treatmentPlan());
        consultation.setWeightKg(req.weightKg());
        consultation.setTemperatureC(req.temperatureC());

        return toResponse(consultationRepo.save(consultation));
    }

    // actualizar consulta
    @Transactional
    public ConsultationResponse updateConsultation(UUID id, ConsultationPatchRequest req) {
        Consultation c = findOrThrow(id);

        // BR-11: bloqueado si hay una factura PAID para esta consulta
        if (invoiceRepo.existsPaidInvoiceForConsultation(id)) {
            throw new com.veterinaria.exception.ConflictException("CONSULTATION_LOCKED",
                    "La consulta no puede modificarse: existe una factura en estado PAID (BR-11)");
        }

        if (req.anamnesis()     != null) c.setAnamnesis(req.anamnesis());
        if (req.physicalExam()  != null) c.setPhysicalExam(req.physicalExam());
        if (req.treatmentPlan() != null) c.setTreatmentPlan(req.treatmentPlan());
        if (req.weightKg()      != null) c.setWeightKg(req.weightKg());
        if (req.temperatureC()  != null) c.setTemperatureC(req.temperatureC());

        return toResponse(consultationRepo.save(c));
    }

    // DIAGNOSES

    // listar diagnósticos de una consulta
    public List<DiagnosisResponse> listDiagnoses(UUID consultationId) {
        findOrThrow(consultationId);
        return diagnosisRepo.findByConsultationId(consultationId).stream()
                .map(this::toDiagnosisResponse).toList();
    }

    // agregar diagnóstico a consulta
    @Transactional
    public DiagnosisResponse addDiagnosis(UUID consultationId, DiagnosisRequest req) {
        Consultation c = findOrThrow(consultationId);

        // BR-13: solo un diagnóstico primario por consulta
        if (req.isPrimary() && diagnosisRepo.existsByConsultationIdAndIsPrimaryTrue(consultationId)) {
            throw new ConflictException("DUPLICATE_PRIMARY_DIAGNOSIS",
                    "Ya existe un diagnóstico primario para esta consulta (BR-13)");
        }

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setConsultation(c);
        diagnosis.setCieCode(req.cieCode());
        diagnosis.setDescription(req.description());
        diagnosis.setSeverity(req.severity());
        diagnosis.setPrimary(req.isPrimary());

        return toDiagnosisResponse(diagnosisRepo.save(diagnosis));
    }

    // actualizar diagnóstico
    @Transactional
    public DiagnosisResponse updateDiagnosis(UUID consultationId, UUID diagnosisId,
                                              DiagnosisPatchRequest req) {
        findOrThrow(consultationId);
        Diagnosis d = diagnosisRepo.findById(diagnosisId)
                .filter(x -> x.getConsultation().getId().equals(consultationId))
                .orElseThrow(() -> new ResourceNotFoundException("DIAGNOSIS_NOT_FOUND",
                        "Diagnóstico no encontrado: " + diagnosisId));

        if (req.isPrimary() != null && req.isPrimary()) {
            // BR-13: si se marca como primary, limpiar el anterior y asignar nuevo
            diagnosisRepo.clearPrimary(consultationId);
            d.setPrimary(true);
        } else if (req.isPrimary() != null) {
            d.setPrimary(req.isPrimary());
        }

        if (req.cieCode()     != null) d.setCieCode(req.cieCode());
        if (req.description() != null) d.setDescription(req.description());
        if (req.severity()    != null) d.setSeverity(req.severity());

        return toDiagnosisResponse(diagnosisRepo.save(d));
    }

    // eliminar diagnóstico
    @Transactional
    public void deleteDiagnosis(UUID consultationId, UUID diagnosisId) {
        findOrThrow(consultationId);
        Diagnosis d = diagnosisRepo.findById(diagnosisId)
                .filter(x -> x.getConsultation().getId().equals(consultationId))
                .orElseThrow(() -> new ResourceNotFoundException("DIAGNOSIS_NOT_FOUND",
                        "Diagnóstico no encontrado: " + diagnosisId));
        diagnosisRepo.delete(d);
    }

    // PRESCRIPTIONS

    // listar prescripciones de una consulta
    public List<PrescriptionResponse> listPrescriptions(UUID consultationId) {
        findOrThrow(consultationId);
        return prescriptionRepo.findByConsultationId(consultationId).stream()
                .map(this::toPrescriptionResponse).toList();
    }

    // agregar prescripción a consulta
    @Transactional
    public PrescriptionResponse addPrescription(UUID consultationId, PrescriptionRequest req) {
        Consultation c = findOrThrow(consultationId);

        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND",
                        "Producto no encontrado: " + req.productId()));

        Prescription prescription = new Prescription();
        prescription.setConsultation(c);
        prescription.setProduct(product);
        prescription.setDosage(req.dosage());
        prescription.setFrequency(req.frequency());
        prescription.setDurationDays(req.durationDays());
        prescription.setInstructions(req.instructions());

        return toPrescriptionResponse(prescriptionRepo.save(prescription));
    }

    // eliminar prescripción
    @Transactional
    public void deletePrescription(UUID consultationId, UUID prescriptionId) {
        findOrThrow(consultationId);
        Prescription p = prescriptionRepo.findById(prescriptionId)
                .filter(x -> x.getConsultation().getId().equals(consultationId))
                .orElseThrow(() -> new ResourceNotFoundException("PRESCRIPTION_NOT_FOUND",
                        "Prescripción no encontrada: " + prescriptionId));
        prescriptionRepo.delete(p);
    }

    // HELPERS

    Consultation findOrThrow(UUID id) {
        return consultationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CONSULTATION_NOT_FOUND",
                        "Consulta no encontrada: " + id));
    }

    private void validateVeterinarianActive(Staff staff) {
        if (staff.getRole() != StaffRole.VETERINARIAN) {
            throw new BusinessRuleException("NOT_VETERINARIAN",
                    "Solo un VETERINARIAN puede realizar una consulta (BR-08)", 422);
        }
        if (!staff.isActive()) {
            throw new BusinessRuleException("STAFF_INACTIVE",
                    "El veterinario no está activo (BR-08)", 422);
        }
    }

    private ConsultationResponse toResponse(Consultation c) {
        Patient patient = c.getAppointment().getPatient();
        Staff   staff   = c.getStaff();
        List<DiagnosisResponse>    diagnoses     = diagnosisRepo.findByConsultationId(c.getId())
                .stream().map(this::toDiagnosisResponse).toList();
        List<PrescriptionResponse> prescriptions = prescriptionRepo.findByConsultationId(c.getId())
                .stream().map(this::toPrescriptionResponse).toList();

        return new ConsultationResponse(
                c.getId(),
                c.getAppointment().getId(),
                patient.getId(),
                patient.getName(),
                staff.getId(),
                staff.getFirstName() + " " + staff.getLastName(),
                c.getAnamnesis(),
                c.getPhysicalExam(),
                c.getTreatmentPlan(),
                c.getWeightKg(),
                c.getTemperatureC(),
                diagnoses,
                prescriptions,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }

    private DiagnosisResponse toDiagnosisResponse(Diagnosis d) {
        return new DiagnosisResponse(d.getId(), d.getConsultation().getId(),
                d.getCieCode(), d.getDescription(), d.getSeverity(), d.isPrimary(),
                d.getCreatedAt());
    }

    private PrescriptionResponse toPrescriptionResponse(Prescription p) {
        return new PrescriptionResponse(p.getId(), p.getConsultation().getId(),
                p.getProduct().getId(), p.getProduct().getName(),
                p.getDosage(), p.getFrequency(), p.getDurationDays(),
                p.getInstructions(), p.getCreatedAt());
    }

    private PageMeta toPageMeta(Page<?> page) {
        return new PageMeta(page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
