package com.veterinaria.infrastructure.web.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
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

import com.veterinaria.application.dto.page.StaffPage;
import com.veterinaria.application.dto.request.StaffPatchRequest;
import com.veterinaria.application.dto.request.StaffRequest;
import com.veterinaria.application.dto.response.StaffResponse;
import com.veterinaria.application.service.StaffService;
import com.veterinaria.domain.enums.StaffRole;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public ResponseEntity<StaffPage> list(
            @RequestParam(required = false) StaffRole role,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(staffService.listStaff(role, isActive, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(staffService.getStaff(id));
    }

    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createStaff(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffResponse> replace(
            @PathVariable UUID id,
            @Valid @RequestBody StaffRequest req) {
        return ResponseEntity.ok(staffService.replaceStaff(id, req));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StaffResponse> update(
            @PathVariable UUID id,
            @RequestBody StaffPatchRequest req) {
        return ResponseEntity.ok(staffService.updateStaff(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        staffService.deactivateStaff(id);
        return ResponseEntity.noContent().build();
    }
}
