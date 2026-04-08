package com.veterinaria.infrastructure.web.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.application.dto.response.DashboardSummaryResponse;
import com.veterinaria.domain.enums.InvoiceStatus;
import com.veterinaria.domain.repository.AppointmentRepository;
import com.veterinaria.domain.repository.InvoiceRepository;
import com.veterinaria.domain.repository.ProductRepository;

// controlador para endpoints del dashboard administrativo que muestran resúmenes de datos 
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final AppointmentRepository appointmentRepo;
    private final InvoiceRepository     invoiceRepo;
    private final ProductRepository     productRepo;

    public DashboardController(AppointmentRepository appointmentRepo,
                               InvoiceRepository invoiceRepo,
                               ProductRepository productRepo) {
        this.appointmentRepo = appointmentRepo;
        this.invoiceRepo     = invoiceRepo;
        this.productRepo     = productRepo;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary() {
        OffsetDateTime startOfDay = OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        long todayAppointments = appointmentRepo.countByScheduledAtBetween(startOfDay, endOfDay);
        long pendingInvoices   = invoiceRepo.countByStatusIn(
                List.of(InvoiceStatus.DRAFT, InvoiceStatus.ISSUED));
        long lowStockProducts  = productRepo.findLowStock().size();

        return ResponseEntity.ok(new DashboardSummaryResponse(
                todayAppointments, pendingInvoices, lowStockProducts));
    }
}
