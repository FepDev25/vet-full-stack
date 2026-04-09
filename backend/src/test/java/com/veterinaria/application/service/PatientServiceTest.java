package com.veterinaria.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.application.dto.request.AddOwnerRequest;
import com.veterinaria.application.dto.request.PatientRequest;
import com.veterinaria.application.dto.response.PatientResponse;
import com.veterinaria.domain.entity.Client;
import com.veterinaria.domain.entity.ClientPatient;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Species;
import com.veterinaria.domain.enums.AppointmentStatus;
import com.veterinaria.domain.enums.PatientSex;
import com.veterinaria.domain.repository.AppointmentRepository;
import com.veterinaria.domain.repository.BreedRepository;
import com.veterinaria.domain.repository.ClientPatientRepository;
import com.veterinaria.domain.repository.ClientRepository;
import com.veterinaria.domain.repository.PatientRepository;
import com.veterinaria.domain.repository.SpeciesRepository;
import com.veterinaria.exception.ConflictException;
import com.veterinaria.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock PatientRepository       patientRepo;
    @Mock ClientRepository        clientRepo;
    @Mock SpeciesRepository       speciesRepo;
    @Mock BreedRepository         breedRepo;
    @Mock ClientPatientRepository clientPatientRepo;
    @Mock AppointmentRepository   appointmentRepo;

    @InjectMocks PatientService service;

    private Client  owner;
    private Client  coOwner;
    private Species species;
    private Patient patient;

    @BeforeEach
    void setUp() {
        species = new Species();
        species.setId(UUID.randomUUID());
        species.setName("Perro");

        owner = new Client();
        owner.setId(UUID.randomUUID());
        owner.setFirstName("Roberto");
        owner.setLastName("Gómez");
        owner.setEmail("roberto@test.com");

        coOwner = new Client();
        coOwner.setId(UUID.randomUUID());
        coOwner.setFirstName("Sofía");
        coOwner.setLastName("Herrera");
        coOwner.setEmail("sofia@test.com");

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setName("Max");
        patient.setSpecies(species);
        patient.setSex(PatientSex.M);
    }

    // ── createPatient ─────────────────────────────────────────────────────────

    @Test
    void createPatient_success_BR01() {
        when(clientRepo.findByIdAndDeletedAtIsNull(owner.getId())).thenReturn(Optional.of(owner));
        when(patientRepo.existsByMicrochipNumberAndDeletedAtIsNull(any())).thenReturn(false);
        when(speciesRepo.findById(species.getId())).thenReturn(Optional.of(species));
        when(patientRepo.save(any())).thenReturn(patient);
        when(clientPatientRepo.save(any())).thenReturn(new ClientPatient());

        PatientRequest req = new PatientRequest(
                "Max", species.getId(), null, null,
                PatientSex.M, null, "Dorado",
                "CHIP-001", false, owner.getId());

        PatientResponse resp = service.createPatient(req);

        assertThat(resp.name()).isEqualTo("Max");
        verify(clientPatientRepo).save(any()); // BR-01: vínculo creado
    }

    @Test
    void createPatient_ownerNotFound_throws() {
        when(clientRepo.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        PatientRequest req = new PatientRequest(
                "Max", species.getId(), null, null,
                PatientSex.M, null, null, null, false, UUID.randomUUID());

        assertThatThrownBy(() -> service.createPatient(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente");
    }

    @Test
    void createPatient_duplicateMicrochip_BR03_throws() {
        when(clientRepo.findByIdAndDeletedAtIsNull(owner.getId())).thenReturn(Optional.of(owner));
        when(patientRepo.existsByMicrochipNumberAndDeletedAtIsNull("CHIP-DUP")).thenReturn(true);

        PatientRequest req = new PatientRequest(
                "Max", species.getId(), null, null,
                PatientSex.M, null, null, "CHIP-DUP", false, owner.getId());

        assertThatThrownBy(() -> service.createPatient(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("microchip");
    }

    // ── addOwner / removeOwner ────────────────────────────────────────────────

    @Test
    void addOwner_success_BR01() {
        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId())).thenReturn(Optional.of(patient));
        when(clientRepo.findByIdAndDeletedAtIsNull(coOwner.getId())).thenReturn(Optional.of(coOwner));
        when(clientPatientRepo.existsByClientIdAndPatientId(coOwner.getId(), patient.getId())).thenReturn(false);

        ClientPatient link = new ClientPatient();
        link.setId(UUID.randomUUID());
        link.setClient(coOwner);
        link.setPatient(patient);
        link.setPrimaryOwner(false);
        when(clientPatientRepo.save(any())).thenReturn(link);

        AddOwnerRequest req = new AddOwnerRequest(coOwner.getId(), false);
        var resp = service.addOwner(patient.getId(), req);

        assertThat(resp).isNotNull();
        verify(clientPatientRepo).save(any());
    }

    @Test
    void addOwner_alreadyOwner_throws() {
        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId())).thenReturn(Optional.of(patient));
        when(clientRepo.findByIdAndDeletedAtIsNull(owner.getId())).thenReturn(Optional.of(owner));
        when(clientPatientRepo.existsByClientIdAndPatientId(owner.getId(), patient.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.addOwner(patient.getId(), new AddOwnerRequest(owner.getId(), false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya es propietario");
    }

    @Test
    void removeOwner_soleOwner_BR02_throws() {
        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId())).thenReturn(Optional.of(patient));

        ClientPatient link = new ClientPatient();
        link.setClient(owner);
        link.setPatient(patient);
        when(clientPatientRepo.findByClientIdAndPatientId(owner.getId(), patient.getId()))
                .thenReturn(Optional.of(link));
        when(clientPatientRepo.countByPatientId(patient.getId())).thenReturn(1L);

        assertThatThrownBy(() -> service.removeOwner(patient.getId(), owner.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-02");
    }

    @Test
    void removeOwner_coOwner_success() {
        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId())).thenReturn(Optional.of(patient));

        ClientPatient link = new ClientPatient();
        link.setClient(coOwner);
        link.setPatient(patient);
        when(clientPatientRepo.findByClientIdAndPatientId(coOwner.getId(), patient.getId()))
                .thenReturn(Optional.of(link));
        when(clientPatientRepo.countByPatientId(patient.getId())).thenReturn(2L);

        assertThatCode(() -> service.removeOwner(patient.getId(), coOwner.getId()))
                .doesNotThrowAnyException();
        verify(clientPatientRepo).delete(link);
    }

    // ── deletePatient / CE-10 ────────────────────────────────────────────────

    @Test
    void deletePatient_softDelete_cancelsActiveAppointments_CE10() {
        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId())).thenReturn(Optional.of(patient));

        service.deletePatient(patient.getId());

        // CE-10: se cancela antes del soft-delete
        verify(appointmentRepo).cancelAppointmentsForDeletedPatient(
                eq(patient.getId()),
                eq(List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)));
        verify(patientRepo).save(patient);
        assertThat(patient.getDeletedAt()).isNotNull();
    }
}
