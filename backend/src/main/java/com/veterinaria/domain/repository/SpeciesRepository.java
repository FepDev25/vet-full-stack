package com.veterinaria.domain.repository;

import com.veterinaria.domain.entity.Species;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpeciesRepository extends JpaRepository<Species, UUID> {

    boolean existsByNameIgnoreCase(String name);
}
