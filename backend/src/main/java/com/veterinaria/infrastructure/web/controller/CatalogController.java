package com.veterinaria.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.application.dto.request.BreedRequest;
import com.veterinaria.application.dto.request.SpeciesRequest;
import com.veterinaria.application.dto.response.BreedResponse;
import com.veterinaria.application.dto.response.SpeciesResponse;
import com.veterinaria.application.service.CatalogService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // SPECIES

    @GetMapping("/species")
    public ResponseEntity<List<SpeciesResponse>> listSpecies() {
        return ResponseEntity.ok(catalogService.listSpecies());
    }

    @PostMapping("/species")
    public ResponseEntity<SpeciesResponse> createSpecies(@Valid @RequestBody SpeciesRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createSpecies(req));
    }

    // BREEDS 

    @GetMapping("/species/{speciesId}/breeds")
    public ResponseEntity<List<BreedResponse>> listBreeds(@PathVariable UUID speciesId) {
        return ResponseEntity.ok(catalogService.listBreedsBySpecies(speciesId));
    }

    @PostMapping("/species/{speciesId}/breeds")
    public ResponseEntity<BreedResponse> createBreed(@PathVariable UUID speciesId,
                                                      @Valid @RequestBody BreedRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createBreed(speciesId, req));
    }
}
