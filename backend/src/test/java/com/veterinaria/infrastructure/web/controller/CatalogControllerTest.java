package com.veterinaria.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.veterinaria.application.dto.request.BreedRequest;
import com.veterinaria.application.dto.request.SpeciesRequest;

class CatalogControllerTest extends BaseControllerIT {

    @Test
    void listSpecies_public_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listBreedsBySpecies_public_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/species/{speciesId}/breeds", SPECIES_PERRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createSpecies_withAuth_returns201() throws Exception {
        String token = registerAndLogin("catalog.species@test.com");

        mockMvc.perform(post("/api/v1/species")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SpeciesRequest("Huron"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Huron"));
    }

    @Test
    void createBreed_withAuth_returns201() throws Exception {
        String token = registerAndLogin("catalog.breed@test.com");

        mockMvc.perform(post("/api/v1/species/{speciesId}/breeds", SPECIES_PERRO)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BreedRequest("Criollo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Criollo"));
    }

    @Test
    void createSpecies_withoutAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/species")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SpeciesRequest("Mapache"))))
                .andExpect(status().isForbidden());
    }
}
