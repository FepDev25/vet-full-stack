package com.veterinaria.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.application.dto.request.VaccinationRequest;
import com.veterinaria.application.dto.response.VaccinationDueResponse;
import com.veterinaria.application.dto.response.VaccinationResponse;
import com.veterinaria.application.service.VaccinationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/vaccinations")
public class VaccinationController {

    private final VaccinationService vaccinationService;

    public VaccinationController(VaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    @PostMapping
    public ResponseEntity<VaccinationResponse> create(
            @Valid @RequestBody VaccinationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaccinationService.createVaccination(req));
    }

    @GetMapping("/due")
    public ResponseEntity<List<VaccinationDueResponse>> due(
            @RequestParam(defaultValue = "30") int daysAhead) {
        return ResponseEntity.ok(vaccinationService.listDueVaccinations(daysAhead));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VaccinationResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(vaccinationService.getVaccination(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vaccinationService.deleteVaccination(id);
        return ResponseEntity.noContent().build();
    }
}
