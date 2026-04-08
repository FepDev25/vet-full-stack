package com.veterinaria.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByMicrochipNumberAndDeletedAtIsNull(String microchipNumber);

    // búsqueda paginada de pacientes activos con filtro de texto y por especie.
    @Query("""
            SELECT p FROM Patient p
            WHERE p.deletedAt IS NULL
              AND (:speciesId IS NULL OR p.species.id = :speciesId)
              AND (:search IS NULL
                   OR LOWER(p.name)            LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.microchipNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Patient> findActive(
            @Param("search") String search,
            @Param("speciesId") UUID speciesId,
            Pageable pageable);
}
