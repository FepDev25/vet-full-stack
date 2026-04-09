package com.veterinaria.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.request.VaccinationRequest;
import com.veterinaria.application.dto.response.VaccinationDueResponse;
import com.veterinaria.application.dto.response.VaccinationResponse;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Product;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.entity.Vaccination;
import com.veterinaria.domain.enums.ProductType;
import com.veterinaria.domain.repository.PatientRepository;
import com.veterinaria.domain.repository.ProductRepository;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.domain.repository.VaccinationRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class VaccinationService {

    private final VaccinationRepository vaccinationRepo;
    private final PatientRepository     patientRepo;
    private final ProductRepository     productRepo;
    private final StaffRepository       staffRepo;

    public VaccinationService(VaccinationRepository vaccinationRepo,
                              PatientRepository patientRepo,
                              ProductRepository productRepo,
                              StaffRepository staffRepo) {
        this.vaccinationRepo = vaccinationRepo;
        this.patientRepo     = patientRepo;
        this.productRepo     = productRepo;
        this.staffRepo       = staffRepo;
    }

    // obtener un registro de vacunación por ID
    public VaccinationResponse getVaccination(UUID id) {
        return toResponse(findOrThrow(id));
    }

    // listar vacunaciones de un paciente
    public List<VaccinationResponse> listByPatient(UUID patientId) {
        patientRepo.findByIdAndDeletedAtIsNull(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("PATIENT_NOT_FOUND",
                        "Paciente no encontrado: " + patientId));
        return vaccinationRepo.findByPatientId(patientId).stream()
                .map(this::toResponse).toList();
    }

    // listar próximas vacunaciones por vencer en los próximos N días
    public List<VaccinationDueResponse> listDueVaccinations(int daysAhead) {
        LocalDate cutoff = LocalDate.now().plusDays(daysAhead);
        return vaccinationRepo.findDueVaccinations(cutoff).stream()
                .map(this::toDueResponse).toList();
    }

    // registrar una nueva vacunación
    @Transactional
    public VaccinationResponse createVaccination(VaccinationRequest req) {
        Patient patient = patientRepo.findByIdAndDeletedAtIsNull(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("PATIENT_NOT_FOUND",
                        "Paciente no encontrado: " + req.patientId()));

        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND",
                        "Producto no encontrado: " + req.productId()));

        // BR-18: el producto debe ser de tipo VACCINE
        if (product.getType() != ProductType.VACCINE) {
            throw new BusinessRuleException("NOT_VACCINE_PRODUCT",
                    "El producto debe ser de tipo VACCINE para registrar una vacunación (BR-18)", 422);
        }

        Staff staff = staffRepo.findById(req.staffId())
                .orElseThrow(() -> new ResourceNotFoundException("STAFF_NOT_FOUND",
                        "Personal no encontrado: " + req.staffId()));

        // BR-20: nextDueDate debe ser posterior a administeredAt
        if (req.nextDueDate() != null && !req.nextDueDate().isAfter(req.administeredAt())) {
            throw new BusinessRuleException("INVALID_NEXT_DUE_DATE",
                    "next_due_date debe ser posterior a administered_at (BR-20)", 422);
        }

        Vaccination vaccination = new Vaccination();
        vaccination.setPatient(patient);
        vaccination.setProduct(product);
        vaccination.setStaff(staff);
        vaccination.setAdministeredAt(req.administeredAt());
        vaccination.setNextDueDate(req.nextDueDate());
        vaccination.setBatchNumber(req.batchNumber());

        return toResponse(vaccinationRepo.save(vaccination));
    }

    // eliminar un registro de vacunación
    @Transactional
    public void deleteVaccination(UUID id) {
        Vaccination v = findOrThrow(id);
        vaccinationRepo.delete(v);
    }

    // HELPERS

    private Vaccination findOrThrow(UUID id) {
        return vaccinationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VACCINATION_NOT_FOUND",
                        "Registro de vacunación no encontrado: " + id));
    }

    private VaccinationResponse toResponse(Vaccination v) {
        return new VaccinationResponse(
                v.getId(),
                v.getPatient().getId(),
                v.getProduct().getId(),
                v.getProduct().getName(),
                v.getStaff().getId(),
                v.getStaff().getFirstName() + " " + v.getStaff().getLastName(),
                v.getAdministeredAt(),
                v.getNextDueDate(),
                v.getBatchNumber(),
                v.getCreatedAt());
    }

    private VaccinationDueResponse toDueResponse(Vaccination v) {
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), v.getNextDueDate());
        return new VaccinationDueResponse(
                v.getId(),
                v.getPatient().getId(),
                v.getPatient().getName(),
                v.getProduct().getId(),
                v.getProduct().getName(),
                v.getNextDueDate(),
                daysRemaining);
    }
}
