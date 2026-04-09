package com.veterinaria.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.application.dto.request.ConsultationPatchRequest;
import com.veterinaria.application.dto.request.ConsultationRequest;
import com.veterinaria.application.dto.request.DiagnosisPatchRequest;
import com.veterinaria.application.dto.request.DiagnosisRequest;
import com.veterinaria.application.dto.request.PrescriptionRequest;
import com.veterinaria.application.dto.response.ConsultationResponse;
import com.veterinaria.application.dto.response.DiagnosisResponse;
import com.veterinaria.application.dto.response.PrescriptionResponse;
import com.veterinaria.application.service.ConsultationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    // CONSULTATION

    @PostMapping
    public ResponseEntity<ConsultationResponse> create(
            @Valid @RequestBody ConsultationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultationService.createConsultation(req));
    }

    @GetMapping("/{consultationId}")
    public ResponseEntity<ConsultationResponse> get(@PathVariable UUID consultationId) {
        return ResponseEntity.ok(consultationService.getConsultation(consultationId));
    }

    @PatchMapping("/{consultationId}")
    public ResponseEntity<ConsultationResponse> update(
            @PathVariable UUID consultationId,
            @Valid @RequestBody ConsultationPatchRequest req) {
        return ResponseEntity.ok(consultationService.updateConsultation(consultationId, req));
    }

    // DIAGNOSES

    @GetMapping("/{consultationId}/diagnoses")
    public ResponseEntity<List<DiagnosisResponse>> listDiagnoses(
            @PathVariable UUID consultationId) {
        return ResponseEntity.ok(consultationService.listDiagnoses(consultationId));
    }

    @PostMapping("/{consultationId}/diagnoses")
    public ResponseEntity<DiagnosisResponse> addDiagnosis(
            @PathVariable UUID consultationId,
            @Valid @RequestBody DiagnosisRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultationService.addDiagnosis(consultationId, req));
    }

    @PatchMapping("/{consultationId}/diagnoses/{diagnosisId}")
    public ResponseEntity<DiagnosisResponse> updateDiagnosis(
            @PathVariable UUID consultationId,
            @PathVariable UUID diagnosisId,
            @Valid @RequestBody DiagnosisPatchRequest req) {
        return ResponseEntity.ok(
                consultationService.updateDiagnosis(consultationId, diagnosisId, req));
    }

    @DeleteMapping("/{consultationId}/diagnoses/{diagnosisId}")
    public ResponseEntity<Void> deleteDiagnosis(
            @PathVariable UUID consultationId,
            @PathVariable UUID diagnosisId) {
        consultationService.deleteDiagnosis(consultationId, diagnosisId);
        return ResponseEntity.noContent().build();
    }

    // PRESCRIPTIONS

    @GetMapping("/{consultationId}/prescriptions")
    public ResponseEntity<List<PrescriptionResponse>> listPrescriptions(
            @PathVariable UUID consultationId) {
        return ResponseEntity.ok(consultationService.listPrescriptions(consultationId));
    }

    @PostMapping("/{consultationId}/prescriptions")
    public ResponseEntity<PrescriptionResponse> addPrescription(
            @PathVariable UUID consultationId,
            @Valid @RequestBody PrescriptionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultationService.addPrescription(consultationId, req));
    }

    @DeleteMapping("/{consultationId}/prescriptions/{prescriptionId}")
    public ResponseEntity<Void> deletePrescription(
            @PathVariable UUID consultationId,
            @PathVariable UUID prescriptionId) {
        consultationService.deletePrescription(consultationId, prescriptionId);
        return ResponseEntity.noContent().build();
    }
}
