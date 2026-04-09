package com.veterinaria.infrastructure.web.controller;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.veterinaria.application.dto.request.AppointmentRequest;

class AppointmentControllerTest extends BaseControllerIT {

    @Test
    void createAppointment_success_returns201() throws Exception {
        String token = registerAndLogin("appt.create@test.com");

        AppointmentRequest req = new AppointmentRequest(
                PATIENT_ID, VET_ID,
                OffsetDateTime.now().plusDays(2).withHour(10).withMinute(0),
                "Control anual", null);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.staffId").value(VET_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("Control anual"));
    }

    @Test
    void confirmAppointment_success_returns200() throws Exception {
        String token = registerAndLogin("appt.confirm@test.com");

        AppointmentRequest req = new AppointmentRequest(
                PATIENT_ID, VET_ID,
                OffsetDateTime.now().plusDays(3).withHour(14).withMinute(0),
                "Vacunación", null);

        String id = createAppointmentAndGetId(token, req);

        mockMvc.perform(post("/api/v1/appointments/{id}/confirm", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void completeAppointmentFlow_pendingToCompleted() throws Exception {
        String token = registerAndLogin("appt.flow@test.com");

        AppointmentRequest req = new AppointmentRequest(
                PATIENT_ID, VET_ID,
                OffsetDateTime.now().plusDays(3).withHour(9).withMinute(0),
                "Consulta general", null);

        String id = createAppointmentAndGetId(token, req);

        mockMvc.perform(post("/api/v1/appointments/{id}/confirm", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/v1/appointments/{id}/start", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/appointments/{id}/complete", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void cancelAppointment_success_returns200() throws Exception {
        String token = registerAndLogin("appt.cancel@test.com");

        AppointmentRequest req = new AppointmentRequest(
                PATIENT_ID, VET_ID,
                OffsetDateTime.now().plusDays(3).withHour(11).withMinute(0),
                "Cancel test", null);

        String id = createAppointmentAndGetId(token, req);

        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", id)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Cliente solicitó cancelación\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void markNoShow_returns200() throws Exception {
        String token = registerAndLogin("appt.noshow@test.com");

        AppointmentRequest req = new AppointmentRequest(
                PATIENT_ID, VET_ID,
                OffsetDateTime.now().plusDays(3).withHour(16).withMinute(0),
                "No-show test", null);

        String id = createAppointmentAndGetId(token, req);

        mockMvc.perform(post("/api/v1/appointments/{id}/no-show", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));
    }

    @Test
    void getAppointment_existing_returns200() throws Exception {
        String token = registerAndLogin("appt.get@test.com");

        mockMvc.perform(get("/api/v1/appointments/{id}", APPOINTMENT_COMPLETED)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(APPOINTMENT_COMPLETED.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getAppointment_notFound_returns404() throws Exception {
        String token = registerAndLogin("appt.404@test.com");

        mockMvc.perform(get("/api/v1/appointments/{id}", "00000000-9999-9999-9999-999999999999")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNotFound());
    }

    private String createAppointmentAndGetId(String token, AppointmentRequest req) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return jsonId(result.getResponse().getContentAsString());
    }
}
