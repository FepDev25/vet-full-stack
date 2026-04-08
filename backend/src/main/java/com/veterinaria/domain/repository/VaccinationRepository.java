package com.veterinaria.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.Vaccination;

public interface VaccinationRepository extends JpaRepository<Vaccination, UUID> {

    // BR-20: vacunas con próxima dosis antes de :cutoff (incluye las ya vencidas)
    @Query("""
            SELECT v FROM Vaccination v
            JOIN FETCH v.patient
            JOIN FETCH v.product
            WHERE v.nextDueDate IS NOT NULL
              AND v.nextDueDate <= :cutoff
            ORDER BY v.nextDueDate ASC
            """)
    List<Vaccination> findDueVaccinations(@Param("cutoff") LocalDate cutoff);
}
