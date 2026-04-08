package com.veterinaria.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.Diagnosis;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    List<Diagnosis> findByConsultationId(UUID consultationId);

    boolean existsByConsultationIdAndIsPrimaryTrue(UUID consultationId);

    // BR-13: limpia el flag is_primary de una consulta antes de asignar uno nuevo
    @Modifying
    @Query("UPDATE Diagnosis d SET d.isPrimary = false WHERE d.consultation.id = :consultationId")
    int clearPrimary(@Param("consultationId") UUID consultationId);
}
