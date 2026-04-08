package com.veterinaria.infrastructure.web.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.application.dto.page.AppointmentPage;
import com.veterinaria.application.dto.request.AppointmentPatchRequest;
import com.veterinaria.application.dto.request.AppointmentRequest;
import com.veterinaria.application.dto.request.CancelRequest;
import com.veterinaria.application.dto.response.AppointmentResponse;
import com.veterinaria.application.service.AppointmentService;
import com.veterinaria.domain.enums.AppointmentStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<AppointmentPage> list(
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime date,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                appointmentService.listAppointments(staffId, patientId, status, date, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointment(id));
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody AppointmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(req));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentPatchRequest req) {
        return ResponseEntity.ok(appointmentService.updateAppointment(id, req));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<AppointmentResponse> start(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.startAppointment(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.completeAppointment(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelRequest req) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, req));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<AppointmentResponse> noShow(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.markNoShow(id));
    }
}
