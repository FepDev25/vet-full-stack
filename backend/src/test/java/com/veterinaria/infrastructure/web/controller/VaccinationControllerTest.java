package com.veterinaria.infrastructure.web.controller;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.veterinaria.application.dto.request.VaccinationRequest;

class VaccinationControllerTest extends BaseControllerIT {

    @Test
    void createVaccination_returns201() throws Exception {
        String token = registerAndLogin("vax.create@test.com");

        VaccinationRequest req = new VaccinationRequest(
                PATIENT_ID,
                java.util.UUID.fromString("00000000-0007-0007-0007-000000000006"),
                VET_ID,
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                "BATCH-NEW-001");

        mockMvc.perform(post("/api/v1/vaccinations")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()));
    }

    @Test
    void dueVaccinations_returns200() throws Exception {
        String token = registerAndLogin("vax.due@test.com");

        mockMvc.perform(get("/api/v1/vaccinations/due")
                        .param("daysAhead", "45")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getVaccination_existing_returns200() throws Exception {
        String token = registerAndLogin("vax.get@test.com");

        VaccinationRequest req = new VaccinationRequest(
                PATIENT_ID,
                java.util.UUID.fromString("00000000-0007-0007-0007-000000000006"),
                VET_ID,
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                "BATCH-GET-001");

        String id = jsonId(mockMvc.perform(post("/api/v1/vaccinations")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/vaccinations/{id}", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void deleteVaccination_returns204() throws Exception {
        String token = registerAndLogin("vax.delete@test.com");

        VaccinationRequest req = new VaccinationRequest(
                PATIENT_ID,
                java.util.UUID.fromString("00000000-0007-0007-0007-000000000006"),
                VET_ID,
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                "BATCH-DEL-001");

        String id = jsonId(mockMvc.perform(post("/api/v1/vaccinations")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(delete("/api/v1/vaccinations/{id}", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNoContent());
    }
}
