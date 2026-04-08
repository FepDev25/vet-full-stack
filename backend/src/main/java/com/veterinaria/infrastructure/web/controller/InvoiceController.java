package com.veterinaria.infrastructure.web.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.application.dto.page.InvoicePage;
import com.veterinaria.application.dto.request.InvoiceCreateRequest;
import com.veterinaria.application.dto.request.InvoiceItemRequest;
import com.veterinaria.application.dto.request.InvoicePatchRequest;
import com.veterinaria.application.dto.request.PayInvoiceRequest;
import com.veterinaria.application.dto.response.InvoiceItemResponse;
import com.veterinaria.application.dto.response.InvoiceResponse;
import com.veterinaria.application.service.InvoiceService;
import com.veterinaria.domain.enums.InvoiceStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<InvoicePage> list(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listInvoices(clientId, status, pageable));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InvoiceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody InvoicePatchRequest req) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, req));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<InvoiceItemResponse>> listItems(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.listItems(id));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<InvoiceItemResponse> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody InvoiceItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.addItem(id, req));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        invoiceService.removeItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<InvoiceResponse> issue(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.issueInvoice(id));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<InvoiceResponse> pay(
            @PathVariable UUID id,
            @Valid @RequestBody PayInvoiceRequest req) {
        return ResponseEntity.ok(invoiceService.payInvoice(id, req));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.cancelInvoice(id));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<InvoiceResponse> refund(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.refundInvoice(id));
    }
}
