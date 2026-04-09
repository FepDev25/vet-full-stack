package com.veterinaria.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    boolean existsByAppointmentId(UUID appointmentId);

    @Query("""
            SELECT c FROM Consultation c
            JOIN FETCH c.appointment a
            JOIN FETCH a.patient
            JOIN FETCH c.staff
            WHERE a.patient.id = :patientId
            ORDER BY c.createdAt DESC
            """)
    Page<Consultation> findByPatientId(@Param("patientId") UUID patientId, Pageable pageable);

    // Carga appointment + patient + staff en una sola query para evitar lazy loads
    @Query("""
            SELECT c FROM Consultation c
            JOIN FETCH c.appointment a
            JOIN FETCH a.patient
            JOIN FETCH c.staff
            WHERE c.id = :id
            """)
    Optional<Consultation> findByIdWithDetails(@Param("id") UUID id);
}
