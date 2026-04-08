package com.veterinaria.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.ClientPatient;

public interface ClientPatientRepository extends JpaRepository<ClientPatient, UUID> {

    List<ClientPatient> findByPatientId(UUID patientId);

    List<ClientPatient> findByClientId(UUID clientId);

    Optional<ClientPatient> findByClientIdAndPatientId(UUID clientId, UUID patientId);

    boolean existsByClientIdAndPatientId(UUID clientId, UUID patientId);

    Optional<ClientPatient> findByPatientIdAndIsPrimaryOwnerTrue(UUID patientId);

    long countByPatientId(UUID patientId);

    // BR-02: ¿Tiene el cliente algún paciente activo del que es único propietario?
    @Query("""
            SELECT COUNT(cp) FROM ClientPatient cp
            WHERE cp.client.id = :clientId
              AND cp.patient.deletedAt IS NULL
              AND (SELECT COUNT(cp2) FROM ClientPatient cp2 WHERE cp2.patient.id = cp.patient.id) = 1
            """)
    long countSoleOwnedActivePatients(@Param("clientId") UUID clientId);

    // BR-01: quita la bandera primaria actual del paciente (paso 1 del intercambio atómico).
    @Modifying
    @Query("""
            UPDATE ClientPatient cp
            SET cp.isPrimaryOwner = FALSE
            WHERE cp.patient.id = :patientId AND cp.isPrimaryOwner = TRUE
            """)
    void clearPrimaryOwner(@Param("patientId") UUID patientId);

    // BR-01: asigna la bandera primaria al nuevo propietario (paso 2).
    @Modifying
    @Query("""
            UPDATE ClientPatient cp
            SET cp.isPrimaryOwner = TRUE
            WHERE cp.patient.id = :patientId AND cp.client.id = :clientId
            """)
    int setPrimaryOwnerFlag(@Param("patientId") UUID patientId, @Param("clientId") UUID clientId);
}
