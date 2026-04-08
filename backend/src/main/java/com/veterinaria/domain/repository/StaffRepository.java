package com.veterinaria.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.enums.StaffRole;

public interface StaffRepository extends JpaRepository<Staff, UUID> {

    Optional<Staff> findByEmail(String email);

    boolean existsByEmail(String email);

    // filtrar por rol y estado activo, con paginación
    @Query("""
            SELECT s FROM Staff s
            WHERE (:role IS NULL OR s.role = :role)
              AND (:isActive IS NULL OR s.isActive = :isActive)
            """)
    Page<Staff> findByFilters(
            @Param("role") StaffRole role,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
