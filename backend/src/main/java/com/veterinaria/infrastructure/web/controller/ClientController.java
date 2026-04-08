package com.veterinaria.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import com.veterinaria.application.dto.page.ClientPage;
import com.veterinaria.application.dto.request.ClientPatchRequest;
import com.veterinaria.application.dto.request.ClientRequest;
import com.veterinaria.application.dto.response.ClientResponse;
import com.veterinaria.application.dto.response.PatientSummaryResponse;
import com.veterinaria.application.service.ClientService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<ClientPage> listClients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        PageRequest pageable = buildPageable(page, size, sort);
        return ResponseEntity.ok(clientService.listClients(search, pageable));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(clientService.getClient(clientId));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(req));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ClientResponse> replaceClient(@PathVariable UUID clientId,
                                                         @Valid @RequestBody ClientRequest req) {
        return ResponseEntity.ok(clientService.replaceClient(clientId, req));
    }

    @PatchMapping("/{clientId}")
    public ResponseEntity<ClientResponse> updateClient(@PathVariable UUID clientId,
                                                        @Valid @RequestBody ClientPatchRequest req) {
        return ResponseEntity.ok(clientService.updateClient(clientId, req));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{clientId}/patients")
    public ResponseEntity<List<PatientSummaryResponse>> listClientPatients(
            @PathVariable UUID clientId) {
        return ResponseEntity.ok(clientService.listClientPatients(clientId));
    }

    // HELPERS

    private PageRequest buildPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, Math.min(size, 100), Sort.by(dir, parts[0]));
    }
}
