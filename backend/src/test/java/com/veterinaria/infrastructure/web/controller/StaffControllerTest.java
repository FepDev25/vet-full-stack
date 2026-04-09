package com.veterinaria.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.veterinaria.application.dto.request.StaffPatchRequest;
import com.veterinaria.application.dto.request.StaffRequest;
import com.veterinaria.domain.enums.StaffRole;

class StaffControllerTest extends BaseControllerIT {

    @Test
    void listStaff_returns200() throws Exception {
        String token = registerAndLogin("staff.list@test.com");

        mockMvc.perform(get("/api/v1/staff")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void getStaff_existing_returns200() throws Exception {
        String token = registerAndLogin("staff.get@test.com");

        mockMvc.perform(get("/api/v1/staff/{id}", VET_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VET_ID.toString()));
    }

    @Test
    void createStaff_returns201() throws Exception {
        String token = registerAndLogin("staff.create@test.com");

        StaffRequest req = new StaffRequest(
                "Marco", "Ruiz", "marco.staff@test.com", "+57-3333333", null, StaffRole.RECEPTIONIST);

        mockMvc.perform(post("/api/v1/staff")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("marco.staff@test.com"));
    }

    @Test
    void replaceStaff_returns200() throws Exception {
        String token = registerAndLogin("staff.put@test.com");

        StaffRequest req = new StaffRequest(
                "Carlos", "Mendoza", "c.mendoza.updated@test.com", "+57-3444444", "VET-COL-2018-0042", StaffRole.VETERINARIAN);

        mockMvc.perform(put("/api/v1/staff/{id}", VET_ID)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("c.mendoza.updated@test.com"));
    }

    @Test
    void patchStaff_returns200() throws Exception {
        String token = registerAndLogin("staff.patch@test.com");

        StaffPatchRequest req = new StaffPatchRequest("Carlos", null, null, "+57-3555555", null, true);

        mockMvc.perform(patch("/api/v1/staff/{id}", VET_ID)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+57-3555555"));
    }

    @Test
    void deactivateStaff_returns204() throws Exception {
        String token = registerAndLogin("staff.delete@test.com");

        StaffRequest req = new StaffRequest(
                "Temp", "Staff", "temp.staff.delete@test.com", null, null, StaffRole.ASSISTANT);

        String id = jsonId(mockMvc.perform(post("/api/v1/staff")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(delete("/api/v1/staff/{id}", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNoContent());
    }
}
