package com.veterinaria.domain.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.Appointment;
import com.veterinaria.domain.enums.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    // metodo para el filtro de citas en el dashboard del admin y staff, con paginacion
    @Query("""
            SELECT a FROM Appointment a
            WHERE (:staffId IS NULL OR a.staff.id = :staffId)
              AND (:patientId IS NULL OR a.patient.id = :patientId)
              AND (:status IS NULL OR a.status = :status)
              AND (:date IS NULL
                   OR (a.scheduledAt >= :date AND a.scheduledAt < :datePlusOne))
            """)
    Page<Appointment> findByFilters(
            @Param("staffId") UUID staffId,
            @Param("patientId") UUID patientId,
            @Param("status") AppointmentStatus status,
            @Param("date") OffsetDateTime date,
            @Param("datePlusOne") OffsetDateTime datePlusOne,
            Pageable pageable);

    // BR-07: detecta solapamiento de agenda para un veterinario 
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.staff.id = :staffId
              AND a.status IN :activeStatuses
              AND a.scheduledAt > :from
              AND a.scheduledAt < :to
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    long countConflictingAppointments(
            @Param("staffId") UUID staffId,
            @Param("activeStatuses") List<AppointmentStatus> activeStatuses,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("excludeId") UUID excludeId);

    // CE-10: cancela todas las citas PENDING/CONFIRMED de un paciente al hacer soft-delete.
    @Modifying
    @Query("""
            UPDATE Appointment a
            SET a.status = com.veterinaria.domain.enums.AppointmentStatus.CANCELLED,
                a.notes  = CASE WHEN a.notes IS NULL
                                THEN 'Cancelada automáticamente al eliminar el paciente.'
                                ELSE CONCAT(a.notes, ' | Cancelada automáticamente al eliminar el paciente.')
                           END
            WHERE a.patient.id = :patientId
              AND a.status IN :cancelableStatuses
            """)
    int cancelAppointmentsForDeletedPatient(
            @Param("patientId") UUID patientId,
            @Param("cancelableStatuses") List<AppointmentStatus> cancelableStatuses);

    Page<Appointment> findByPatientIdOrderByScheduledAtDesc(UUID patientId, Pageable pageable);

    // Portal cliente: citas de los pacientes del cliente autenticado
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.patient.id IN :patientIds
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.scheduledAt DESC
            """)
    Page<Appointment> findByPatientIdsAndStatus(
            @Param("patientIds") List<UUID> patientIds,
            @Param("status") AppointmentStatus status,
            Pageable pageable);

    // Dashboard: contar citas del día por estado (activas)
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.scheduledAt >= :from
              AND a.scheduledAt < :to
            """)
    long countByScheduledAtBetween(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
