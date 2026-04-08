package com.veterinaria.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.domain.entity.Breed;

public interface BreedRepository extends JpaRepository<Breed, UUID> {

    List<Breed> findBySpeciesId(UUID speciesId);

    boolean existsBySpeciesIdAndNameIgnoreCase(UUID speciesId, String name);
}
