package com.veterinaria.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.application.dto.page.AppointmentPage;
import com.veterinaria.application.dto.page.ConsultationPage;
import com.veterinaria.application.dto.page.PatientPage;
import com.veterinaria.application.dto.request.AddOwnerRequest;
import com.veterinaria.application.dto.request.PatientPatchRequest;
import com.veterinaria.application.dto.request.PatientRequest;
import com.veterinaria.application.dto.request.PatientUpdateRequest;
import com.veterinaria.application.dto.response.OwnerResponse;
import com.veterinaria.application.dto.response.PatientResponse;
import com.veterinaria.application.dto.response.VaccinationResponse;
import com.veterinaria.application.service.AppointmentService;
import com.veterinaria.application.service.ConsultationService;
import com.veterinaria.application.service.PatientService;
import com.veterinaria.application.service.VaccinationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService        patientService;
    private final AppointmentService    appointmentService;
    private final ConsultationService   consultationService;
    private final VaccinationService    vaccinationService;

    public PatientController(PatientService patientService,
                             AppointmentService appointmentService,
                             ConsultationService consultationService,
                             VaccinationService vaccinationService) {
        this.patientService      = patientService;
        this.appointmentService  = appointmentService;
        this.consultationService = consultationService;
        this.vaccinationService  = vaccinationService;
    }

    @GetMapping
    public ResponseEntity<PatientPage> listPatients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID speciesId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        PageRequest pageable = buildPageable(page, size, sort);
        return ResponseEntity.ok(patientService.listPatients(search, speciesId, pageable));
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<PatientResponse> getPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(patientService.getPatient(patientId));
    }

    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createPatient(req));
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<PatientResponse> replacePatient(@PathVariable UUID patientId,
                                                           @Valid @RequestBody PatientUpdateRequest req) {
        return ResponseEntity.ok(patientService.replacePatient(patientId, req));
    }

    @PatchMapping("/{patientId}")
    public ResponseEntity<PatientResponse> updatePatient(@PathVariable UUID patientId,
                                                          @Valid @RequestBody PatientPatchRequest req) {
        return ResponseEntity.ok(patientService.updatePatient(patientId, req));
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID patientId) {
        patientService.deletePatient(patientId);
        return ResponseEntity.noContent().build();
    }

    // OWNERS

    @GetMapping("/{patientId}/owners")
    public ResponseEntity<List<OwnerResponse>> listOwners(@PathVariable UUID patientId) {
        return ResponseEntity.ok(patientService.listOwners(patientId));
    }

    @PostMapping("/{patientId}/owners")
    public ResponseEntity<OwnerResponse> addOwner(@PathVariable UUID patientId,
                                                    @Valid @RequestBody AddOwnerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.addOwner(patientId, req));
    }

    @DeleteMapping("/{patientId}/owners/{clientId}")
    public ResponseEntity<Void> removeOwner(@PathVariable UUID patientId,
                                             @PathVariable UUID clientId) {
        patientService.removeOwner(patientId, clientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{patientId}/owners/{clientId}/set-primary")
    public ResponseEntity<OwnerResponse> setPrimaryOwner(@PathVariable UUID patientId,
                                                          @PathVariable UUID clientId) {
        return ResponseEntity.ok(patientService.setPrimaryOwner(patientId, clientId));
    }

    // APPOINTMENTS

    @GetMapping("/{patientId}/appointments")
    public ResponseEntity<AppointmentPage> listAppointments(
            @PathVariable UUID patientId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                appointmentService.listPatientAppointments(patientId, pageable));
    }

    // CONSULTATIONS

    @GetMapping("/{patientId}/consultations")
    public ResponseEntity<ConsultationPage> listConsultations(
            @PathVariable UUID patientId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                consultationService.listByPatient(patientId, pageable));
    }

    // VACCINATIONS

    @GetMapping("/{patientId}/vaccinations")
    public ResponseEntity<List<VaccinationResponse>> listVaccinations(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(vaccinationService.listByPatient(patientId));
    }

    // HELPERS

    private PageRequest buildPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, Math.min(size, 100), Sort.by(dir, parts[0]));
    }
}
