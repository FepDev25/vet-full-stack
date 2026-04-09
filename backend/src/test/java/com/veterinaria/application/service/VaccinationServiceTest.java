package com.veterinaria.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.application.dto.request.VaccinationRequest;
import com.veterinaria.application.dto.response.VaccinationResponse;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Product;
import com.veterinaria.domain.entity.Species;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.entity.Vaccination;
import com.veterinaria.domain.enums.PatientSex;
import com.veterinaria.domain.enums.ProductType;
import com.veterinaria.domain.enums.StaffRole;
import com.veterinaria.domain.repository.PatientRepository;
import com.veterinaria.domain.repository.ProductRepository;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.domain.repository.VaccinationRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class VaccinationServiceTest {

    @Mock VaccinationRepository vaccinationRepo;
    @Mock PatientRepository     patientRepo;
    @Mock ProductRepository     productRepo;
    @Mock StaffRepository       staffRepo;

    @InjectMocks VaccinationService service;

    private Patient patient;
    private Product vaccine;
    private Product medication;
    private Staff   vet;

    @BeforeEach
    void setUp() {
        Species species = new Species();
        species.setId(UUID.randomUUID());
        species.setName("Perro");

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setName("Max");
        patient.setSpecies(species);
        patient.setSex(PatientSex.M);

        vaccine = new Product();
        vaccine.setId(UUID.randomUUID());
        vaccine.setName("Nobivac DHPPi");
        vaccine.setType(ProductType.VACCINE);
        vaccine.setUnitPrice(BigDecimal.valueOf(35.00));

        medication = new Product();
        medication.setId(UUID.randomUUID());
        medication.setName("Amoxicilina");
        medication.setType(ProductType.MEDICATION);
        medication.setUnitPrice(BigDecimal.valueOf(18.00));

        vet = new Staff();
        vet.setId(UUID.randomUUID());
        vet.setFirstName("Carlos");
        vet.setLastName("Mendoza");
        vet.setRole(StaffRole.VETERINARIAN);
        vet.setActive(true);
    }

    @Test
    void createVaccination_success_BR18() {
        LocalDate today = LocalDate.now();
        LocalDate nextYear = today.plusYears(1);

        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId())).thenReturn(Optional.of(patient));
        when(productRepo.findById(vaccine.getId())).thenReturn(Optional.of(vaccine));
        when(staffRepo.findById(vet.getId())).thenReturn(Optional.of(vet));

        Vaccination saved = new Vaccination();
        saved.setId(UUID.randomUUID());
        saved.setPatient(patient);
        saved.setProduct(vaccine);
        saved.setStaff(vet);
        saved.setAdministeredAt(today);
        saved.setNextDueDate(nextYear);
        saved.setBatchNumber("BATCH-001");
        saved.setCreatedAt(OffsetDateTime.now());
        when(vaccinationRepo.save(any())).thenReturn(saved);

        VaccinationRequest req = new VaccinationRequest(
                patient.getId(), vaccine.getId(), vet.getId(),
                today, nextYear, "BATCH-001");

        VaccinationResponse resp = service.createVaccination(req);

        assertThat(resp).isNotNull();
        assertThat(resp.productName()).isEqualTo("Nobivac DHPPi");
    }

    @Test
    void createVaccination_notVaccineProduct_BR18_throws() {
        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId())).thenReturn(Optional.of(patient));
        when(productRepo.findById(medication.getId())).thenReturn(Optional.of(medication));

        VaccinationRequest req = new VaccinationRequest(
                patient.getId(), medication.getId(), vet.getId(),
                LocalDate.now(), null, "BATCH-001");

        assertThatThrownBy(() -> service.createVaccination(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-18");
    }

    @Test
    void createVaccination_nextDueDateNotAfterAdministered_BR20_throws() {
        LocalDate administered = LocalDate.now();
        LocalDate nextDue = administered; // igual, no posterior

        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId())).thenReturn(Optional.of(patient));
        when(productRepo.findById(vaccine.getId())).thenReturn(Optional.of(vaccine));
        when(staffRepo.findById(vet.getId())).thenReturn(Optional.of(vet));

        VaccinationRequest req = new VaccinationRequest(
                patient.getId(), vaccine.getId(), vet.getId(),
                administered, nextDue, "BATCH-001");

        assertThatThrownBy(() -> service.createVaccination(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-20");
    }

    @Test
    void createVaccination_patientNotFound_throws() {
        when(patientRepo.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        VaccinationRequest req = new VaccinationRequest(
                UUID.randomUUID(), vaccine.getId(), vet.getId(),
                LocalDate.now(), null, "BATCH-001");

        assertThatThrownBy(() -> service.createVaccination(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Paciente no encontrado");
    }

    @Test
    void createVaccination_productNotFound_throws() {
        when(patientRepo.findByIdAndDeletedAtIsNull(patient.getId())).thenReturn(Optional.of(patient));
        when(productRepo.findById(any())).thenReturn(Optional.empty());

        VaccinationRequest req = new VaccinationRequest(
                patient.getId(), UUID.randomUUID(), vet.getId(),
                LocalDate.now(), null, "BATCH-001");

        assertThatThrownBy(() -> service.createVaccination(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Producto no encontrado");
    }
}
