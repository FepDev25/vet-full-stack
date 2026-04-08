package com.veterinaria.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.Client;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<Client> findByEmailAndDeletedAtIsNull(String email);

    // búsqueda paginada de clientes activos con filtro de texto.
    @Query("""
            SELECT c FROM Client c
            WHERE c.deletedAt IS NULL
              AND (:search IS NULL
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.email)     LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Client> findActive(@Param("search") String search, Pageable pageable);
}
