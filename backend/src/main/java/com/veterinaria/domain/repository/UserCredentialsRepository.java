package com.veterinaria.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.domain.entity.UserCredentials;

public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {

    // Método para encontrar UserCredentials por entityId y entityType
    Optional<UserCredentials> findByEntityIdAndEntityType(UUID entityId, String entityType);

    // Método para verificar si existen UserCredentials por entityId y entityType
    boolean existsByEntityIdAndEntityType(UUID entityId, String entityType);
}
