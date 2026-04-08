package com.veterinaria.application.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.page.AppointmentPage;
import com.veterinaria.application.dto.page.PageMeta;
import com.veterinaria.application.dto.request.AppointmentPatchRequest;
import com.veterinaria.application.dto.request.AppointmentRequest;
import com.veterinaria.application.dto.request.CancelRequest;
import com.veterinaria.application.dto.response.AppointmentResponse;
import com.veterinaria.domain.entity.Appointment;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.enums.AppointmentStatus;
import com.veterinaria.domain.enums.StaffRole;
import com.veterinaria.domain.repository.AppointmentRepository;
import com.veterinaria.domain.repository.PatientRepository;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class AppointmentService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);

    private static final List<AppointmentStatus> MUTABLE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepo;
    private final PatientRepository     patientRepo;
    private final StaffRepository       staffRepo;

    public AppointmentService(AppointmentRepository appointmentRepo,
                              PatientRepository patientRepo,
                              StaffRepository staffRepo) {
        this.appointmentRepo = appointmentRepo;
        this.patientRepo     = patientRepo;
        this.staffRepo       = staffRepo;
    }

    // LIST

    // listar citas con filtros
    @Transactional(readOnly = true)
    public AppointmentPage listAppointments(UUID staffId, UUID patientId,
                                            AppointmentStatus status,
                                            OffsetDateTime date, Pageable pageable) {
        OffsetDateTime datePlusOne = (date != null) ? date.plusDays(1) : null;
        Page<Appointment> page = appointmentRepo.findByFilters(
                staffId, patientId, status, date, datePlusOne, pageable);
        return new AppointmentPage(
                page.getContent().stream().map(this::toResponse).toList(),
                toPageMeta(page));
    }

    // listar citas por múltiples pacientes
    @Transactional(readOnly = true)
    public AppointmentPage listAppointmentsByPatientIds(List<UUID> patientIds,
                                                         AppointmentStatus status,
                                                         Pageable pageable) {
        Page<Appointment> page = appointmentRepo.findByPatientIdsAndStatus(patientIds, status, pageable);
        return new AppointmentPage(
                page.getContent().stream().map(this::toResponse).toList(),
                toPageMeta(page));
    }

    // listar citas de un paciente específico
    @Transactional(readOnly = true)
    public AppointmentPage listPatientAppointments(UUID patientId, Pageable pageable) {
        requirePatientExists(patientId);
        Page<Appointment> page =
                appointmentRepo.findByPatientIdOrderByScheduledAtDesc(patientId, pageable);
        return new AppointmentPage(
                page.getContent().stream().map(this::toResponse).toList(),
                toPageMeta(page));
    }

    // GET

    // obtener detalles de una cita
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(UUID id) {
        return toResponse(findOrThrow(id));
    }

    // CREATE

    // crear nueva cita
    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest req) {
        Patient patient = patientRepo.findByIdAndDeletedAtIsNull(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("PATIENT_NOT_FOUND",
                        "Paciente no encontrado: " + req.patientId()));

        Staff staff = staffRepo.findById(req.staffId())
                .orElseThrow(() -> new ResourceNotFoundException("STAFF_NOT_FOUND",
                        "Personal no encontrado: " + req.staffId()));

        // BR-08: solo VETERINARIAN activo puede ser asignado a una cita
        validateVeterinarianActive(staff);

        // BR-09: scheduledAt debe ser futuro
        if (!req.scheduledAt().isAfter(OffsetDateTime.now())) {
            throw new BusinessRuleException("PAST_SCHEDULED_AT",
                    "La fecha de la cita debe ser futura (BR-09)", 422);
        }

        // BR-07: sin solapamiento en ventana de +-30 min
        checkNoConflict(staff.getId(), req.scheduledAt(), null);

        Appointment appt = new Appointment();
        appt.setPatient(patient);
        appt.setStaff(staff);
        appt.setScheduledAt(req.scheduledAt());
        appt.setReason(req.reason());
        appt.setNotes(req.notes());
        return toResponse(appointmentRepo.save(appt));
    }

    // UPDATE (PATCH)

    // actualizar fecha, motivo o notas de una cita si está en estado PENDING o CONFIRMED
    @Transactional
    public AppointmentResponse updateAppointment(UUID id, AppointmentPatchRequest req) {
        Appointment appt = findOrThrow(id);

        if (!MUTABLE_STATUSES.contains(appt.getStatus())) {
            throw new BusinessRuleException("APPOINTMENT_NOT_MUTABLE",
                    "Solo se pueden modificar citas en estado PENDING o CONFIRMED", 422);
        }

        if (req.scheduledAt() != null) {
            if (!req.scheduledAt().isAfter(OffsetDateTime.now())) {
                throw new BusinessRuleException("PAST_SCHEDULED_AT",
                        "La fecha de la cita debe ser futura (BR-09)", 422);
            }
            checkNoConflict(appt.getStaff().getId(), req.scheduledAt(), id);
            appt.setScheduledAt(req.scheduledAt());
        }

        if (req.reason() != null) appt.setReason(req.reason());
        if (req.notes()  != null) appt.setNotes(req.notes());

        return toResponse(appointmentRepo.save(appt));
    }

    // STATE TRANSITIONS

    @Transactional
    public AppointmentResponse confirmAppointment(UUID id) {
        Appointment appt = findOrThrow(id);
        requireStatus(appt, AppointmentStatus.PENDING, "confirmar");
        appt.setStatus(AppointmentStatus.CONFIRMED);
        return toResponse(appointmentRepo.save(appt));
    }

    @Transactional
    public AppointmentResponse startAppointment(UUID id) {
        Appointment appt = findOrThrow(id);
        requireStatus(appt, AppointmentStatus.CONFIRMED, "iniciar");
        appt.setStatus(AppointmentStatus.IN_PROGRESS);
        return toResponse(appointmentRepo.save(appt));
    }

    @Transactional
    public AppointmentResponse completeAppointment(UUID id) {
        Appointment appt = findOrThrow(id);
        requireStatus(appt, AppointmentStatus.IN_PROGRESS, "completar");
        appt.setStatus(AppointmentStatus.COMPLETED);
        return toResponse(appointmentRepo.save(appt));
    }

    @Transactional
    public AppointmentResponse cancelAppointment(UUID id, CancelRequest req) {
        Appointment appt = findOrThrow(id);
        if (!MUTABLE_STATUSES.contains(appt.getStatus())) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Solo se pueden cancelar citas en estado PENDING o CONFIRMED", 422);
        }
        appt.setStatus(AppointmentStatus.CANCELLED);
        if (req != null && req.notes() != null) {
            String existing = appt.getNotes();
            appt.setNotes(existing != null
                    ? existing + " | " + req.notes()
                    : req.notes());
        }
        return toResponse(appointmentRepo.save(appt));
    }

    @Transactional
    public AppointmentResponse markNoShow(UUID id) {
        Appointment appt = findOrThrow(id);
        if (!MUTABLE_STATUSES.contains(appt.getStatus())) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Solo se pueden marcar como NO_SHOW citas en estado PENDING o CONFIRMED", 422);
        }
        appt.setStatus(AppointmentStatus.NO_SHOW);
        return toResponse(appointmentRepo.save(appt));
    }

    // HELPERS

    Appointment findOrThrow(UUID id) {
        return appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("APPOINTMENT_NOT_FOUND",
                        "Cita no encontrada: " + id));
    }

    private void requirePatientExists(UUID patientId) {
        patientRepo.findByIdAndDeletedAtIsNull(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("PATIENT_NOT_FOUND",
                        "Paciente no encontrado: " + patientId));
    }

    private void validateVeterinarianActive(Staff staff) {
        if (staff.getRole() != StaffRole.VETERINARIAN) {
            throw new BusinessRuleException("NOT_VETERINARIAN",
                    "Solo un VETERINARIAN puede ser asignado a una cita (BR-08)", 422);
        }
        if (!staff.isActive()) {
            throw new BusinessRuleException("STAFF_INACTIVE",
                    "El veterinario no está activo (BR-08)", 422);
        }
    }

    private void checkNoConflict(UUID staffId, OffsetDateTime scheduledAt, UUID excludeId) {
        OffsetDateTime from = scheduledAt.minusMinutes(30);
        OffsetDateTime to   = scheduledAt.plusMinutes(30);
        long conflicts = appointmentRepo.countConflictingAppointments(
                staffId, ACTIVE_STATUSES, from, to, excludeId);
        if (conflicts > 0) {
            throw new BusinessRuleException("APPOINTMENT_CONFLICT",
                    "El veterinario ya tiene una cita activa en ese horario (BR-07)", 409);
        }
    }

    private void requireStatus(Appointment appt, AppointmentStatus expected, String action) {
        if (appt.getStatus() != expected) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Para " + action + " la cita debe estar en estado " + expected
                    + " (actual: " + appt.getStatus() + ")", 422);
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        String patientName  = a.getPatient().getName();
        String staffName    = a.getStaff().getFirstName() + " " + a.getStaff().getLastName();
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getId(),
                patientName,
                a.getStaff().getId(),
                staffName,
                a.getScheduledAt(),
                a.getStatus(),
                a.getReason(),
                a.getNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    private PageMeta toPageMeta(Page<?> page) {
        return new PageMeta(page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
