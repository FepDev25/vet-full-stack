package com.veterinaria.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.page.PageMeta;
import com.veterinaria.application.dto.page.StaffPage;
import com.veterinaria.application.dto.request.StaffPatchRequest;
import com.veterinaria.application.dto.request.StaffRequest;
import com.veterinaria.application.dto.response.StaffResponse;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.enums.StaffRole;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ConflictException;
import com.veterinaria.exception.ResourceNotFoundException;

// servicio de staff
@Service
@Transactional(readOnly = true)
public class StaffService {

    private final StaffRepository staffRepo;

    public StaffService(StaffRepository staffRepo) {
        this.staffRepo = staffRepo;
    }

    // listar al personal con filtros
    @Transactional(readOnly = true)
    public StaffPage listStaff(StaffRole role, Boolean isActive, Pageable pageable) {
        Page<Staff> page = staffRepo.findByFilters(role, isActive, pageable);
        List<StaffResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new StaffPage(content, toPageMeta(page));
    }

    // obtener un miembro del personal por ID
    @Transactional(readOnly = true)
    public StaffResponse getStaff(UUID id) {
        return toResponse(findOrThrow(id));
    }

    // crear un nuevo miembro del personal
    @Transactional
    public StaffResponse createStaff(StaffRequest req) {
        // BR-26: license_number obligatorio para VETERINARIAN
        if (req.role() == StaffRole.VETERINARIAN
                && (req.licenseNumber() == null || req.licenseNumber().isBlank())) {
            throw new BusinessRuleException("LICENSE_REQUIRED",
                    "El número de licencia es obligatorio para VETERINARIAN (BR-26)", 422);
        }

        if (staffRepo.existsByEmail(req.email())) {
            throw new ConflictException("DUPLICATE_EMAIL",
                    "Ya existe un miembro del personal con el email '" + req.email() + "'");
        }

        Staff staff = new Staff();
        applyFields(staff, req);
        return toResponse(staffRepo.save(staff));
    }

    // reemplazar completamente un miembro del personal
    @Transactional
    public StaffResponse replaceStaff(UUID id, StaffRequest req) {
        Staff staff = findOrThrow(id);

        if (req.role() == StaffRole.VETERINARIAN
                && (req.licenseNumber() == null || req.licenseNumber().isBlank())) {
            throw new BusinessRuleException("LICENSE_REQUIRED",
                    "El número de licencia es obligatorio para VETERINARIAN (BR-26)", 422);
        }

        staffRepo.findByEmail(req.email())
                .filter(s -> !s.getId().equals(id))
                .ifPresent(s -> { throw new ConflictException("DUPLICATE_EMAIL",
                        "Ya existe un miembro del personal con el email '" + req.email() + "'"); });

        applyFields(staff, req);
        return toResponse(staffRepo.save(staff));
    }

    // actualizar parcialmente un miembro del personal
    @Transactional
    public StaffResponse updateStaff(UUID id, StaffPatchRequest req) {
        Staff staff = findOrThrow(id);

        if (req.email() != null) {
            staffRepo.findByEmail(req.email())
                    .filter(s -> !s.getId().equals(id))
                    .ifPresent(s -> { throw new ConflictException("DUPLICATE_EMAIL",
                            "Ya existe un miembro del personal con el email '" + req.email() + "'"); });
            staff.setEmail(req.email());
        }
        if (req.firstName()     != null) staff.setFirstName(req.firstName());
        if (req.lastName()      != null) staff.setLastName(req.lastName());
        if (req.phone()         != null) staff.setPhone(req.phone());
        if (req.licenseNumber() != null) staff.setLicenseNumber(req.licenseNumber());
        if (req.isActive()      != null) staff.setActive(req.isActive());
        return toResponse(staffRepo.save(staff));
    }

    // BR-27: desactivación lógica — nunca eliminación física
    @Transactional
    public void deactivateStaff(UUID id) {
        Staff staff = findOrThrow(id);
        staff.setActive(false);
        staffRepo.save(staff);
    }

    // HELPERS

    public Staff findOrThrow(UUID id) {
        return staffRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("STAFF_NOT_FOUND",
                        "Personal no encontrado: " + id));
    }

    private void applyFields(Staff staff, StaffRequest req) {
        staff.setFirstName(req.firstName());
        staff.setLastName(req.lastName());
        staff.setEmail(req.email());
        staff.setPhone(req.phone());
        staff.setLicenseNumber(req.licenseNumber());
        staff.setRole(req.role());
    }

    private StaffResponse toResponse(Staff s) {
        return new StaffResponse(s.getId(), s.getFirstName(), s.getLastName(),
                s.getEmail(), s.getPhone(), s.getLicenseNumber(),
                s.getRole(), s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
    }

    private PageMeta toPageMeta(Page<?> page) {
        return new PageMeta(page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
