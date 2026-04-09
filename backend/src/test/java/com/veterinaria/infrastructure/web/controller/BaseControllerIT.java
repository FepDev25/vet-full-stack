package com.veterinaria.infrastructure.web.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veterinaria.application.dto.request.LoginRequest;
import com.veterinaria.application.dto.request.RegisterRequest;
import com.veterinaria.application.dto.response.AuthResponse;
import com.veterinaria.application.service.AuthService;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public abstract class BaseControllerIT {

    @Autowired protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired protected AuthService authService;

    protected static final UUID VET_ID = UUID.fromString("00000000-0003-0003-0003-000000000001");
    protected static final UUID CLIENT_ID = UUID.fromString("00000000-0004-0004-0004-000000000001");
    protected static final UUID PATIENT_ID = UUID.fromString("00000000-0005-0005-0005-000000000001");
    protected static final UUID APPOINTMENT_COMPLETED = UUID.fromString("00000000-0008-0008-0008-000000000001");
    protected static final UUID CONSULTATION_ID = UUID.fromString("00000000-0009-0009-0009-000000000001");
    protected static final UUID CONSULTATION_2_ID = UUID.fromString("00000000-0009-0009-0009-000000000002");
    protected static final UUID DIAGNOSIS_1_ID = UUID.fromString("00000000-0010-0010-0010-000000000001");
    protected static final UUID DIAGNOSIS_2_ID = UUID.fromString("00000000-0010-0010-0010-000000000002");
    protected static final UUID PRODUCT_AMOXICILINA = UUID.fromString("00000000-0007-0007-0007-000000000001");
    protected static final UUID PRODUCT_CONSULTA = UUID.fromString("00000000-0007-0007-0007-000000000012");
    protected static final UUID SPECIES_PERRO = UUID.fromString("00000000-0001-0001-0001-000000000001");
    protected static final UUID BREED_LABRADOR = UUID.fromString("00000000-0002-0002-0002-000000000001");

    private String vetToken;
    private String clientToken;

    protected String vetToken() {
        if (vetToken == null) {
            AuthResponse resp = authService.login(new LoginRequest("c.mendoza@vetclinica.com", "password"));
            vetToken = resp.token();
        }
        return vetToken;
    }

    protected String clientToken() {
        if (clientToken == null) {
            AuthResponse resp = authService.login(new LoginRequest("roberto.gomez@gmail.com", "password"));
            clientToken = resp.token();
        }
        return clientToken;
    }

    protected String registerAndLogin(String email) {
        authService.register(new RegisterRequest("Test", "User", email, "Password123!"));
        return authService.login(new LoginRequest(email, "Password123!")).token();
    }

    protected String authHeader(String token) {
        return "Bearer " + token;
    }

    protected String jsonId(String body) throws Exception {
        return objectMapper.readTree(body).get("id").asText();
    }
}
