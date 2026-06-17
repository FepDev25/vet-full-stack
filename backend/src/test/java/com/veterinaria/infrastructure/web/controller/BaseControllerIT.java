package com.veterinaria.infrastructure.web.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veterinaria.application.dto.request.LoginRequest;
import com.veterinaria.application.dto.request.RegisterRequest;
import com.veterinaria.application.service.AuthService;
import com.veterinaria.domain.entity.Client;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.entity.UserCredentials;
import com.veterinaria.domain.repository.ClientRepository;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.domain.repository.UserCredentialsRepository;
import com.veterinaria.exception.BusinessRuleException;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public abstract class BaseControllerIT {

    @Autowired protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired protected AuthService authService;
    @Autowired protected StaffRepository staffRepo;
    @Autowired protected ClientRepository clientRepo;
    @Autowired protected UserCredentialsRepository credRepo;
    @Autowired protected PasswordEncoder passwordEncoder;

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

    private static final String VET_EMAIL = "c.mendoza@vetclinica.com";
    private static final String CLIENT_EMAIL = "roberto.gomez@gmail.com";
    private static final String SEED_PASSWORD = "password";

    private String vetToken;
    private String clientToken;

    protected String vetToken() {
        if (vetToken == null) {
            try {
                vetToken = authService.login(new LoginRequest(VET_EMAIL, SEED_PASSWORD)).token();
            } catch (BusinessRuleException e) {
                ensureStaffCredentials(VET_EMAIL, SEED_PASSWORD);
                vetToken = authService.login(new LoginRequest(VET_EMAIL, SEED_PASSWORD)).token();
            }
        }
        return vetToken;
    }

    protected String clientToken() {
        if (clientToken == null) {
            try {
                clientToken = authService.login(new LoginRequest(CLIENT_EMAIL, SEED_PASSWORD)).token();
            } catch (BusinessRuleException e) {
                ensureClientCredentials(CLIENT_EMAIL, SEED_PASSWORD);
                clientToken = authService.login(new LoginRequest(CLIENT_EMAIL, SEED_PASSWORD)).token();
            }
        }
        return clientToken;
    }

    private void ensureStaffCredentials(String email, String password) {
        Staff staff = staffRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Staff not seeded: " + email));
        if (credRepo.findByEntityIdAndEntityType(staff.getId(), "STAFF").isEmpty()) {
            UserCredentials creds = new UserCredentials();
            creds.setEntityId(staff.getId());
            creds.setEntityType("STAFF");
            creds.setPasswordHash(passwordEncoder.encode(password));
            credRepo.save(creds);
        }
    }

    private void ensureClientCredentials(String email, String password) {
        Client client = clientRepo.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalStateException("Client not seeded: " + email));
        if (credRepo.findByEntityIdAndEntityType(client.getId(), "CLIENT").isEmpty()) {
            UserCredentials creds = new UserCredentials();
            creds.setEntityId(client.getId());
            creds.setEntityType("CLIENT");
            creds.setPasswordHash(passwordEncoder.encode(password));
            credRepo.save(creds);
        }
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
