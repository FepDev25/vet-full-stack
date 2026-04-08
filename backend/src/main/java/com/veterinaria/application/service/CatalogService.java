package com.veterinaria.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.request.BreedRequest;
import com.veterinaria.application.dto.request.SpeciesRequest;
import com.veterinaria.application.dto.response.BreedResponse;
import com.veterinaria.application.dto.response.SpeciesResponse;
import com.veterinaria.domain.entity.Breed;
import com.veterinaria.domain.entity.Species;
import com.veterinaria.domain.repository.BreedRepository;
import com.veterinaria.domain.repository.SpeciesRepository;
import com.veterinaria.exception.ConflictException;
import com.veterinaria.exception.ResourceNotFoundException;

// controlador para manejar especies y razas, que son datos de catálogo usados en el sistema
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final SpeciesRepository speciesRepo;
    private final BreedRepository   breedRepo;

    public CatalogService(SpeciesRepository speciesRepo, BreedRepository breedRepo) {
        this.speciesRepo = speciesRepo;
        this.breedRepo   = breedRepo;
    }

    // SPECIES

    // listar todas las especies
    public List<SpeciesResponse> listSpecies() {
        return speciesRepo.findAll().stream().map(this::toSpeciesResponse).toList();
    }

    // crear una nueva especie
    @Transactional
    public SpeciesResponse createSpecies(SpeciesRequest req) {
        if (speciesRepo.existsByNameIgnoreCase(req.name())) {
            throw new ConflictException("DUPLICATE_SPECIES",
                    "Ya existe una especie con el nombre '" + req.name() + "'");
        }
        Species species = new Species();
        species.setName(req.name());
        return toSpeciesResponse(speciesRepo.save(species));
    }

    // BREEDS

    // listar las razas de una especie dada
    public List<BreedResponse> listBreedsBySpecies(UUID speciesId) {
        if (!speciesRepo.existsById(speciesId)) {
            throw new ResourceNotFoundException("SPECIES_NOT_FOUND",
                    "Especie no encontrada: " + speciesId);
        }
        return breedRepo.findBySpeciesId(speciesId).stream()
                .map(b -> toBreedResponse(b, speciesId))
                .toList();
    }

    // crear una nueva raza para una especie dada
    @Transactional
    public BreedResponse createBreed(UUID speciesId, BreedRequest req) {
        Species species = speciesRepo.findById(speciesId)
                .orElseThrow(() -> new ResourceNotFoundException("SPECIES_NOT_FOUND",
                        "Especie no encontrada: " + speciesId));

        if (breedRepo.existsBySpeciesIdAndNameIgnoreCase(speciesId, req.name())) {
            throw new ConflictException("DUPLICATE_BREED",
                    "Ya existe la raza '" + req.name() + "' para esta especie");
        }

        Breed breed = new Breed();
        breed.setSpecies(species);
        breed.setName(req.name());
        return toBreedResponse(breedRepo.save(breed), speciesId);
    }

    // MAPPERS

    private SpeciesResponse toSpeciesResponse(Species s) {
        return new SpeciesResponse(s.getId(), s.getName());
    }

    private BreedResponse toBreedResponse(Breed b, UUID speciesId) {
        return new BreedResponse(b.getId(), speciesId, b.getName());
    }
}
