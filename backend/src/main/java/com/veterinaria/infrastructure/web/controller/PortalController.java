package com.veterinaria.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.application.dto.page.AppointmentPage;
import com.veterinaria.application.dto.page.InvoicePage;
import com.veterinaria.application.dto.page.PageMeta;
import com.veterinaria.application.dto.response.PatientResponse;
import com.veterinaria.application.service.AppointmentService;
import com.veterinaria.application.service.InvoiceService;
import com.veterinaria.domain.entity.Client;
import com.veterinaria.domain.entity.ClientPatient;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.enums.AppointmentStatus;
import com.veterinaria.domain.enums.InvoiceStatus;
import com.veterinaria.domain.repository.ClientPatientRepository;
import com.veterinaria.domain.repository.ClientRepository;
import com.veterinaria.exception.ResourceNotFoundException;

// controller para endpoints del portal del cliente que permiten a los clientes 
// ver sus pacientes, citas o facturas
@RestController
@RequestMapping("/api/v1/portal")
public class PortalController {

    private final ClientRepository        clientRepo;
    private final ClientPatientRepository clientPatientRepo;
    private final AppointmentService      appointmentService;
    private final InvoiceService          invoiceService;

    public PortalController(ClientRepository clientRepo,
                            ClientPatientRepository clientPatientRepo,
                            AppointmentService appointmentService,
                            InvoiceService invoiceService) {
        this.clientRepo         = clientRepo;
        this.clientPatientRepo  = clientPatientRepo;
        this.appointmentService = appointmentService;
        this.invoiceService     = invoiceService;
    }

    @GetMapping("/patients")
    public ResponseEntity<List<PatientResponse>> myPatients(
            @AuthenticationPrincipal UserDetails principal) {
        Client client = resolveClient(principal.getUsername());
        List<PatientResponse> patients = clientPatientRepo.findByClientId(client.getId())
                .stream()
                .map(ClientPatient::getPatient)
                .filter(p -> p.getDeletedAt() == null)
                .map(this::toPatientResponse)
                .toList();
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/appointments")
    public ResponseEntity<AppointmentPage> myAppointments(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) AppointmentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Client client = resolveClient(principal.getUsername());
        List<UUID> patientIds = clientPatientRepo.findByClientId(client.getId())
                .stream()
                .map(cp -> cp.getPatient().getId())
                .toList();
        if (patientIds.isEmpty()) {
            return ResponseEntity.ok(new AppointmentPage(List.of(),
                    new PageMeta(0, 0, 0, pageable.getPageSize())));
        }
        AppointmentPage page = appointmentService.listAppointmentsByPatientIds(
                patientIds, status, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/invoices")
    public ResponseEntity<InvoicePage> myInvoices(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Client client = resolveClient(principal.getUsername());
        return ResponseEntity.ok(invoiceService.listInvoices(client.getId(), status, pageable));
    }

    // HELPER

    private Client resolveClient(String email) {
        return clientRepo.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND",
                        "Cliente no encontrado para el email: " + email));
    }

    private PatientResponse toPatientResponse(Patient p) {
        UUID   speciesId   = p.getSpecies() != null ? p.getSpecies().getId()   : null;
        String speciesName = p.getSpecies() != null ? p.getSpecies().getName() : null;
        UUID   breedId     = p.getBreed()   != null ? p.getBreed().getId()     : null;
        String breedName   = p.getBreed()   != null ? p.getBreed().getName()   : null;
        return new PatientResponse(p.getId(), p.getName(), speciesId, speciesName,
                breedId, breedName, p.getSex(), p.getBirthDate(),
                p.getWeightKg(), p.getCoatColor(), p.getMicrochipNumber(), p.isSterilized(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
