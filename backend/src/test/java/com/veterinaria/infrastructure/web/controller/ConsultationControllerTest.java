package com.veterinaria.infrastructure.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.veterinaria.application.dto.request.AppointmentRequest;
import com.veterinaria.application.dto.request.ConsultationRequest;
import com.veterinaria.application.dto.request.DiagnosisRequest;
import com.veterinaria.application.dto.request.PrescriptionRequest;
import com.veterinaria.domain.enums.DiagnosisSeverity;

class ConsultationControllerTest extends BaseControllerIT {

    @Test
    void getConsultation_existing_returns200() throws Exception {
        String token = registerAndLogin("cons.get@test.com");

        mockMvc.perform(get("/api/v1/consultations/{id}", CONSULTATION_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CONSULTATION_ID.toString()))
                .andExpect(jsonPath("$.appointmentId").isNotEmpty())
                .andExpect(jsonPath("$.diagnoses").isArray());
    }

    @Test
    void createConsultation_success_returns201() throws Exception {
        String token = registerAndLogin("cons.create@test.com");

        String appointmentId = createCompletedAppointmentAndGetId(token);
        ConsultationRequest req = new ConsultationRequest(
                UUID.fromString(appointmentId), VET_ID,
                "Paciente con tos", "Examen normal",
                "Tratamiento sintomático", new BigDecimal("10.5"), new BigDecimal("38.5"));

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentId").value(appointmentId))
                .andExpect(jsonPath("$.staffId").value(VET_ID.toString()))
                .andExpect(jsonPath("$.temperatureC").value(38.5));
    }

    @Test
    void listDiagnoses_returns200() throws Exception {
        String token = registerAndLogin("cons.diag.list@test.com");

        mockMvc.perform(get("/api/v1/consultations/{id}/diagnoses", CONSULTATION_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void addDiagnosis_nonPrimary_returns201() throws Exception {
        String token = registerAndLogin("cons.diag.add@test.com");

        DiagnosisRequest req = new DiagnosisRequest(
                "R51", "Dolor de cabeza secundario", DiagnosisSeverity.MILD, false);

        mockMvc.perform(post("/api/v1/consultations/{id}/diagnoses", CONSULTATION_ID)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Dolor de cabeza secundario"))
                .andExpect(jsonPath("$.isPrimary").value(false));
    }

    @Test
    void deleteDiagnosis_existing_returns204() throws Exception {
        String token = registerAndLogin("cons.diag.del@test.com");

        mockMvc.perform(delete("/api/v1/consultations/{consultationId}/diagnoses/{diagnosisId}",
                        CONSULTATION_ID, DIAGNOSIS_1_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void listPrescriptions_returns200() throws Exception {
        String token = registerAndLogin("cons.rx.list@test.com");

        mockMvc.perform(get("/api/v1/consultations/{id}/prescriptions", CONSULTATION_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void addPrescription_returns201() throws Exception {
        String token = registerAndLogin("cons.rx.add@test.com");

        PrescriptionRequest req = new PrescriptionRequest(
                PRODUCT_AMOXICILINA, "10 mg/kg", "Cada 12 horas", 7, "Con comida");

        mockMvc.perform(post("/api/v1/consultations/{id}/prescriptions", CONSULTATION_2_ID)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(PRODUCT_AMOXICILINA.toString()))
                .andExpect(jsonPath("$.dosage").value("10 mg/kg"));
    }

    @Test
    void createConsultation_withInvalidTemperature_returns422() throws Exception {
        String token = registerAndLogin("cons.val@test.com");

        String appointmentId = createCompletedAppointmentAndGetId(token);
        ConsultationRequest req = new ConsultationRequest(
                UUID.fromString(appointmentId), VET_ID,
                null, null, null, new BigDecimal("10.5"), new BigDecimal("50.0"));

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String createCompletedAppointmentAndGetId(String token) throws Exception {
        AppointmentRequest req = new AppointmentRequest(
                PATIENT_ID, VET_ID,
                OffsetDateTime.now().plusDays(4).withHour(10).withMinute(0),
                "Consulta para generar consulta", null);

        String id = jsonId(mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/appointments/{id}/confirm", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/appointments/{id}/start", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/appointments/{id}/complete", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        return id;
    }
}
