package com.veterinaria.application.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.page.PageMeta;
import com.veterinaria.application.dto.page.PatientPage;
import com.veterinaria.application.dto.request.AddOwnerRequest;
import com.veterinaria.application.dto.request.PatientPatchRequest;
import com.veterinaria.application.dto.request.PatientRequest;
import com.veterinaria.application.dto.request.PatientUpdateRequest;
import com.veterinaria.application.dto.response.OwnerResponse;
import com.veterinaria.application.dto.response.PatientResponse;
import com.veterinaria.application.dto.response.PatientSummaryResponse;
import com.veterinaria.domain.entity.Breed;
import com.veterinaria.domain.entity.Client;
import com.veterinaria.domain.entity.ClientPatient;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Species;
import com.veterinaria.domain.enums.AppointmentStatus;
import com.veterinaria.domain.repository.AppointmentRepository;
import com.veterinaria.domain.repository.BreedRepository;
import com.veterinaria.domain.repository.ClientPatientRepository;
import com.veterinaria.domain.repository.ClientRepository;
import com.veterinaria.domain.repository.PatientRepository;
import com.veterinaria.domain.repository.SpeciesRepository;
import com.veterinaria.exception.ConflictException;
import com.veterinaria.exception.ResourceNotFoundException;

// controlador de pacientes
@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository       patientRepo;
    private final ClientRepository        clientRepo;
    private final SpeciesRepository       speciesRepo;
    private final BreedRepository         breedRepo;
    private final ClientPatientRepository clientPatientRepo;
    private final AppointmentRepository   appointmentRepo;

    public PatientService(PatientRepository patientRepo,
                          ClientRepository clientRepo,
                          SpeciesRepository speciesRepo,
                          BreedRepository breedRepo,
                          ClientPatientRepository clientPatientRepo,
                          AppointmentRepository appointmentRepo) {
        this.patientRepo       = patientRepo;
        this.clientRepo        = clientRepo;
        this.speciesRepo       = speciesRepo;
        this.breedRepo         = breedRepo;
        this.clientPatientRepo = clientPatientRepo;
        this.appointmentRepo   = appointmentRepo;
    }

    // LIST / GET 

    // listar pacientes activos
    public PatientPage listPatients(String search, UUID speciesId, Pageable pageable) {
        Page<Patient> page = patientRepo.findActive(search, speciesId, pageable);
        List<PatientSummaryResponse> content = page.getContent().stream()
                .map(this::toSummary).toList();
        return new PatientPage(content, toPageMeta(page));
    }

    // obtener paciente por ID
    public PatientResponse getPatient(UUID id) {
        return toResponse(findActiveOrThrow(id));
    }

    // CREATE 

    // crear un nuevo paciente
    @Transactional
    public PatientResponse createPatient(PatientRequest req) {
        // Validar propietario primario
        Client owner = clientRepo.findByIdAndDeletedAtIsNull(req.primaryOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND",
                        "Cliente (primaryOwnerId) no encontrado: " + req.primaryOwnerId()));

        // BR-03: microchip único
        if (req.microchipNumber() != null
                && patientRepo.existsByMicrochipNumberAndDeletedAtIsNull(req.microchipNumber())) {
            throw new ConflictException("DUPLICATE_MICROCHIP",
                    "Ya existe un paciente con el microchip '" + req.microchipNumber() + "'");
        }

        Species species = speciesRepo.findById(req.speciesId())
                .orElseThrow(() -> new ResourceNotFoundException("SPECIES_NOT_FOUND",
                        "Especie no encontrada: " + req.speciesId()));

        Breed breed = resolveBreed(req.breedId());

        Patient patient = new Patient();
        patient.setName(req.name());
        patient.setSpecies(species);
        patient.setBreed(breed);
        patient.setBirthDate(req.birthDate());
        patient.setSex(req.sex());
        patient.setWeightKg(req.weightKg());
        patient.setCoatColor(req.coatColor());
        patient.setMicrochipNumber(req.microchipNumber());
        patient.setSterilized(req.isSterilized() != null && req.isSterilized());
        patient = patientRepo.save(patient);

        // El trigger AUD-01 lanzará excepción si breed no pertenece a la especie

        // BR-01: crear vínculo con propietario primario
        ClientPatient link = new ClientPatient();
        link.setClient(owner);
        link.setPatient(patient);
        link.setPrimaryOwner(true);
        clientPatientRepo.save(link);

        return toResponse(patient);
    }

    // REPLACE (PUT)

    // reemplazar un paciente existente o actualizar parcialmente
    @Transactional
    public PatientResponse replacePatient(UUID id, PatientUpdateRequest req) {
        Patient patient = findActiveOrThrow(id);

        if (req.microchipNumber() != null
                && !req.microchipNumber().equals(patient.getMicrochipNumber())
                && patientRepo.existsByMicrochipNumberAndDeletedAtIsNull(req.microchipNumber())) {
            throw new ConflictException("DUPLICATE_MICROCHIP",
                    "Ya existe un paciente con el microchip '" + req.microchipNumber() + "'");
        }

        Species species = speciesRepo.findById(req.speciesId())
                .orElseThrow(() -> new ResourceNotFoundException("SPECIES_NOT_FOUND",
                        "Especie no encontrada: " + req.speciesId()));

        patient.setName(req.name());
        patient.setSpecies(species);
        patient.setBreed(resolveBreed(req.breedId()));
        patient.setBirthDate(req.birthDate());
        patient.setSex(req.sex());
        patient.setWeightKg(req.weightKg());
        patient.setCoatColor(req.coatColor());
        patient.setMicrochipNumber(req.microchipNumber());
        patient.setSterilized(req.isSterilized() != null && req.isSterilized());
        return toResponse(patientRepo.save(patient));
    }

    //  UPDATE (PATCH) 

    // actualizar parcialmente un paciente existente
    @Transactional
    public PatientResponse updatePatient(UUID id, PatientPatchRequest req) {
        Patient patient = findActiveOrThrow(id);

        if (req.microchipNumber() != null
                && !req.microchipNumber().equals(patient.getMicrochipNumber())
                && patientRepo.existsByMicrochipNumberAndDeletedAtIsNull(req.microchipNumber())) {
            throw new ConflictException("DUPLICATE_MICROCHIP",
                    "Ya existe un paciente con el microchip '" + req.microchipNumber() + "'");
        }

        if (req.name()            != null) patient.setName(req.name());
        if (req.breedId()         != null) patient.setBreed(resolveBreed(req.breedId()));
        if (req.birthDate()       != null) patient.setBirthDate(req.birthDate());
        if (req.sex()             != null) patient.setSex(req.sex());
        if (req.weightKg()        != null) patient.setWeightKg(req.weightKg());
        if (req.coatColor()       != null) patient.setCoatColor(req.coatColor());
        if (req.microchipNumber() != null) patient.setMicrochipNumber(req.microchipNumber());
        if (req.isSterilized()    != null) patient.setSterilized(req.isSterilized());
        return toResponse(patientRepo.save(patient));
    }

    //  DELETE

    // eliminar paciente con soft delete
    @Transactional
    public void deletePatient(UUID id) {
        Patient patient = findActiveOrThrow(id);
        // CE-10: cancelar citas PENDING/CONFIRMED antes del soft-delete
        appointmentRepo.cancelAppointmentsForDeletedPatient(
                id, List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED));
        patient.setDeletedAt(OffsetDateTime.now());
        patientRepo.save(patient);
    }

    //  OWNERS 

    // listar duenios
    public List<OwnerResponse> listOwners(UUID patientId) {
        findActiveOrThrow(patientId);
        return clientPatientRepo.findByPatientId(patientId).stream()
                .map(this::toOwnerResponse).toList();
    }

    // BR-01: agrega co-propietario. Si isPrimaryOwner=true, realiza intercambio atómico
    @Transactional
    public OwnerResponse addOwner(UUID patientId, AddOwnerRequest req) {
        Patient patient = findActiveOrThrow(patientId);
        Client  client  = clientRepo.findByIdAndDeletedAtIsNull(req.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND",
                        "Cliente no encontrado: " + req.clientId()));

        if (clientPatientRepo.existsByClientIdAndPatientId(req.clientId(), patientId)) {
            throw new ConflictException("ALREADY_OWNER",
                    "El cliente ya es propietario de este paciente");
        }

        boolean wantsPrimary = Boolean.TRUE.equals(req.isPrimaryOwner());
        if (wantsPrimary) {
            clientPatientRepo.clearPrimaryOwner(patientId);
        }

        ClientPatient link = new ClientPatient();
        link.setClient(client);
        link.setPatient(patient);
        link.setPrimaryOwner(wantsPrimary);
        return toOwnerResponse(clientPatientRepo.save(link));
    }

    // BR-02: no se puede quitar al único propietario
    @Transactional
    public void removeOwner(UUID patientId, UUID clientId) {
        findActiveOrThrow(patientId);
        ClientPatient link = clientPatientRepo.findByClientIdAndPatientId(clientId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("OWNER_NOT_FOUND",
                        "El cliente no es propietario de este paciente"));

        if (clientPatientRepo.countByPatientId(patientId) <= 1) {
            throw new ConflictException("SOLE_OWNER_CONFLICT",
                    "No se puede eliminar al único propietario del paciente (BR-02)");
        }

        clientPatientRepo.delete(link);
    }

    // BR-01: intercambio atómico del propietario primario
    @Transactional
    public OwnerResponse setPrimaryOwner(UUID patientId, UUID clientId) {
        findActiveOrThrow(patientId);
        if (!clientPatientRepo.existsByClientIdAndPatientId(clientId, patientId)) {
            throw new ResourceNotFoundException("OWNER_NOT_FOUND",
                    "El cliente no es propietario de este paciente");
        }

        clientPatientRepo.clearPrimaryOwner(patientId);      // paso 1: quitar actual
        clientPatientRepo.setPrimaryOwnerFlag(patientId, clientId); // paso 2: asignar nuevo

        return clientPatientRepo.findByClientIdAndPatientId(clientId, patientId)
                .map(this::toOwnerResponse)
                .orElseThrow();
    }

    //  HELPERS

    private Patient findActiveOrThrow(UUID id) {
        return patientRepo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("PATIENT_NOT_FOUND",
                        "Paciente no encontrado: " + id));
    }

    private Breed resolveBreed(UUID breedId) {
        if (breedId == null) return null;
        return breedRepo.findById(breedId)
                .orElseThrow(() -> new ResourceNotFoundException("BREED_NOT_FOUND",
                        "Raza no encontrada: " + breedId));
    }

    private PatientSummaryResponse toSummary(Patient p) {
        return new PatientSummaryResponse(
                p.getId(), p.getName(),
                p.getSpecies().getId(), p.getSpecies().getName(),
                p.getBreed() != null ? p.getBreed().getId()   : null,
                p.getBreed() != null ? p.getBreed().getName() : null,
                p.getSex(), p.getBirthDate(), p.getCreatedAt());
    }

    private PatientResponse toResponse(Patient p) {
        return new PatientResponse(
                p.getId(), p.getName(),
                p.getSpecies().getId(), p.getSpecies().getName(),
                p.getBreed() != null ? p.getBreed().getId()   : null,
                p.getBreed() != null ? p.getBreed().getName() : null,
                p.getSex(), p.getBirthDate(),
                p.getWeightKg(), p.getCoatColor(), p.getMicrochipNumber(),
                p.isSterilized(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private OwnerResponse toOwnerResponse(ClientPatient cp) {
        Client c = cp.getClient();
        return new OwnerResponse(
                c.getId(), cp.getPatient().getId(),
                cp.isPrimaryOwner(), cp.getCreatedAt(),
                c.getFirstName(), c.getLastName(), c.getEmail());
    }

    private PageMeta toPageMeta(Page<?> page) {
        return new PageMeta(page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
