package com.veterinaria.infrastructure.web.controller;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.veterinaria.application.dto.request.PatientRequest;

class PatientControllerTest extends BaseControllerIT {

    @Test
    void listPatients_withAuth_returns200() throws Exception {
        String token = registerAndLogin("patient.list@test.com");

        mockMvc.perform(get("/api/v1/patients")
                        .param("search", "Max")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void listPatients_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPatient_existing_returns200() throws Exception {
        String token = registerAndLogin("patient.get@test.com");

        mockMvc.perform(get("/api/v1/patients/{id}", PATIENT_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Max"))
                .andExpect(jsonPath("$.speciesName").value("Perro"));
    }

    @Test
    void getPatient_notFound_returns404() throws Exception {
        String token = registerAndLogin("patient.404@test.com");

        mockMvc.perform(get("/api/v1/patients/{id}", "00000000-9999-9999-9999-999999999999")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PATIENT_NOT_FOUND"));
    }

    @Test
    void createPatient_success_returns201() throws Exception {
        String token = registerAndLogin("patient.create@test.com");

        PatientRequest req = new PatientRequest(
                "Firulais", SPECIES_PERRO, BREED_LABRADOR,
                null, com.veterinaria.domain.enums.PatientSex.M,
                new BigDecimal("15.5"), "Negro", null, false, CLIENT_ID);

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Firulais"))
                .andExpect(jsonPath("$.sex").value("M"))
                .andExpect(jsonPath("$.speciesId").value(SPECIES_PERRO.toString()));
    }

    @Test
    void createPatient_missingName_returns422() throws Exception {
        String token = registerAndLogin("patient.val@test.com");

        PatientRequest req = new PatientRequest(
                "", SPECIES_PERRO, BREED_LABRADOR,
                null, com.veterinaria.domain.enums.PatientSex.M,
                new BigDecimal("15.5"), "Negro", null, false, CLIENT_ID);

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void deletePatient_existing_returns204() throws Exception {
        String token = registerAndLogin("patient.del@test.com");

        PatientRequest req = new PatientRequest(
                "Temporal", SPECIES_PERRO, null,
                null, com.veterinaria.domain.enums.PatientSex.UNKNOWN,
                null, null, null, false, CLIENT_ID);

        MvcResult created = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String id = jsonId(created.getResponse().getContentAsString());

        mockMvc.perform(delete("/api/v1/patients/{id}", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/patients/{id}", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPatientVaccinations_returns200() throws Exception {
        String token = registerAndLogin("patient.vax@test.com");

        mockMvc.perform(get("/api/v1/patients/{id}/vaccinations", PATIENT_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listPatientConsultations_returns200() throws Exception {
        String token = registerAndLogin("patient.cons@test.com");

        mockMvc.perform(get("/api/v1/patients/{id}/consultations", PATIENT_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void listPatientAppointments_returns200() throws Exception {
        String token = registerAndLogin("patient.appt@test.com");

        mockMvc.perform(get("/api/v1/patients/{id}/appointments", PATIENT_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }
}
