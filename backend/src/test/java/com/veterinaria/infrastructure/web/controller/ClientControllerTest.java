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

import com.veterinaria.application.dto.request.ClientPatchRequest;
import com.veterinaria.application.dto.request.ClientRequest;

class ClientControllerTest extends BaseControllerIT {

    @Test
    void listClients_withAuth_returns200() throws Exception {
        String token = registerAndLogin("client.list@test.com");

        mockMvc.perform(get("/api/v1/clients")
                        .param("search", "a")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void getClient_existing_returns200() throws Exception {
        String token = registerAndLogin("client.get@test.com");

        mockMvc.perform(get("/api/v1/clients/{id}", CLIENT_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CLIENT_ID.toString()));
    }

    @Test
    void createClient_returns201() throws Exception {
        String token = registerAndLogin("client.create@test.com");

        ClientRequest req = new ClientRequest("Nora", "Campos", "nora.campos@test.com", "+57-3000000", "Calle 1");

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nora.campos@test.com"));
    }

    @Test
    void replaceClient_returns200() throws Exception {
        String token = registerAndLogin("client.put@test.com");

        ClientRequest req = new ClientRequest("Roberto", "Gomez", "roberto.edit@test.com", "+57-3111111", "Nueva direccion");

        mockMvc.perform(put("/api/v1/clients/{id}", CLIENT_ID)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("roberto.edit@test.com"));
    }

    @Test
    void patchClient_returns200() throws Exception {
        String token = registerAndLogin("client.patch@test.com");

        ClientPatchRequest req = new ClientPatchRequest(null, null, null, "+57-3222222", "Patch address");

        mockMvc.perform(patch("/api/v1/clients/{id}", CLIENT_ID)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+57-3222222"));
    }

    @Test
    void listClientPatients_returns200() throws Exception {
        String token = registerAndLogin("client.patients@test.com");

        mockMvc.perform(get("/api/v1/clients/{id}/patients", CLIENT_ID)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteClient_returns204() throws Exception {
        String token = registerAndLogin("client.delete@test.com");

        ClientRequest req = new ClientRequest("Delete", "Me", "client.delete.me@test.com", null, null);
        String clientId = jsonId(mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(delete("/api/v1/clients/{id}", clientId)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNoContent());
    }
}
