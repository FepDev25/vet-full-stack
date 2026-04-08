package com.veterinaria.application.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.page.ClientPage;
import com.veterinaria.application.dto.page.PageMeta;
import com.veterinaria.application.dto.request.ClientPatchRequest;
import com.veterinaria.application.dto.request.ClientRequest;
import com.veterinaria.application.dto.response.ClientResponse;
import com.veterinaria.application.dto.response.PatientSummaryResponse;
import com.veterinaria.domain.entity.Client;
import com.veterinaria.domain.repository.ClientPatientRepository;
import com.veterinaria.domain.repository.ClientRepository;
import com.veterinaria.exception.ConflictException;
import com.veterinaria.exception.ResourceNotFoundException;

// servicio de clientes
@Service
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository        clientRepo;
    private final ClientPatientRepository clientPatientRepo;

    public ClientService(ClientRepository clientRepo,
                         ClientPatientRepository clientPatientRepo) {
        this.clientRepo        = clientRepo;
        this.clientPatientRepo = clientPatientRepo;
    }

    // listar todos los clientes activos
    @Transactional(readOnly = true)
    public ClientPage listClients(String search, Pageable pageable) {
        Page<Client> page = clientRepo.findActive(search, pageable);
        List<ClientResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new ClientPage(content, toPageMeta(page));
    }

    // obtener un cliente por ID
    @Transactional(readOnly = true)
    public ClientResponse getClient(UUID id) {
        return toResponse(findActiveOrThrow(id));
    }

    // crear un nuevo cliente
    @Transactional
    public ClientResponse createClient(ClientRequest req) {
        if (clientRepo.existsByEmailAndDeletedAtIsNull(req.email())) {
            throw new ConflictException("DUPLICATE_EMAIL",
                    "Ya existe un cliente con el email '" + req.email() + "'");
        }
        Client client = new Client();
        client.setFirstName(req.firstName());
        client.setLastName(req.lastName());
        client.setEmail(req.email());
        client.setPhone(req.phone());
        client.setAddress(req.address());
        return toResponse(clientRepo.save(client));
    }

    // reemplazar un cliente existente o actualizar parcialmente
    @Transactional
    public ClientResponse replaceClient(UUID id, ClientRequest req) {
        Client client = findActiveOrThrow(id);
        checkEmailConflict(req.email(), id);
        client.setFirstName(req.firstName());
        client.setLastName(req.lastName());
        client.setEmail(req.email());
        client.setPhone(req.phone());
        client.setAddress(req.address());
        return toResponse(clientRepo.save(client));
    }

    // actualizar parcialmente un cliente existente
    @Transactional
    public ClientResponse updateClient(UUID id, ClientPatchRequest req) {
        Client client = findActiveOrThrow(id);
        if (req.email() != null) {
            checkEmailConflict(req.email(), id);
            client.setEmail(req.email());
        }
        if (req.firstName() != null) client.setFirstName(req.firstName());
        if (req.lastName()  != null) client.setLastName(req.lastName());
        if (req.phone()     != null) client.setPhone(req.phone());
        if (req.address()   != null) client.setAddress(req.address());
        return toResponse(clientRepo.save(client));
    }

    // BR-04 soft delete. BR-02: rechaza si es único propietario de algún paciente activo.
    @Transactional
    public void deleteClient(UUID id) {
        Client client = findActiveOrThrow(id);

        long sole = clientPatientRepo.countSoleOwnedActivePatients(id);
        if (sole > 0) {
            throw new ConflictException("SOLE_OWNER_CONFLICT",
                    "El cliente es el único propietario de " + sole + " paciente(s) activo(s). " +
                    "Asigne otro propietario primario antes de eliminar.");
        }

        client.setDeletedAt(OffsetDateTime.now());
        clientRepo.save(client);
    }

    // Pacientes activos vinculados al cliente (para GET /clients/{id}/patients).
    public List<PatientSummaryResponse> listClientPatients(UUID clientId) {
        findActiveOrThrow(clientId);
        return clientPatientRepo.findByClientId(clientId).stream()
                .filter(cp -> cp.getPatient().getDeletedAt() == null)
                .map(cp -> toPatientSummary(cp.getPatient()))
                .toList();
    }

    // HELPERS

    private Client findActiveOrThrow(UUID id) {
        return clientRepo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND",
                        "Cliente no encontrado: " + id));
    }

    private void checkEmailConflict(String email, UUID ownerId) {
        clientRepo.findByEmailAndDeletedAtIsNull(email)
                .filter(c -> !c.getId().equals(ownerId))
                .ifPresent(c -> { throw new ConflictException("DUPLICATE_EMAIL",
                        "Ya existe un cliente con el email '" + email + "'"); });
    }

    private ClientResponse toResponse(Client c) {
        return new ClientResponse(c.getId(), c.getFirstName(), c.getLastName(),
                c.getEmail(), c.getPhone(), c.getAddress(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private PatientSummaryResponse toPatientSummary(com.veterinaria.domain.entity.Patient p) {
        return new PatientSummaryResponse(
                p.getId(), p.getName(),
                p.getSpecies().getId(), p.getSpecies().getName(),
                p.getBreed() != null ? p.getBreed().getId()   : null,
                p.getBreed() != null ? p.getBreed().getName() : null,
                p.getSex(), p.getBirthDate(), p.getCreatedAt());
    }

    private PageMeta toPageMeta(Page<?> page) {
        return new PageMeta(page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
