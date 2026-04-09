package com.veterinaria.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.request.LoginRequest;
import com.veterinaria.application.dto.request.RegisterRequest;
import com.veterinaria.application.dto.response.AuthResponse;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ConflictException;

/**
 * Tests de integración para AuthService.
 * Crea usuarios vía register() para evitar hardcodear hashes BCrypt.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIT {

    @Autowired AuthService service;

    @Test
    void register_success_returnsToken() {
        RegisterRequest req = new RegisterRequest(
                "Nuevo", "Usuario", "nuevo@test.com", "Seguro123!");

        AuthResponse resp = service.register(req);

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("CLIENT");
        assertThat(resp.expiresAt()).isNotNull();
    }

    @Test
    void register_duplicateEmail_throws() {
        // Registrar primero
        service.register(new RegisterRequest("A", "B", "dup@test.com", "Pass123!"));

        // Segundo registro con el mismo email
        assertThatThrownBy(() ->
                service.register(new RegisterRequest("C", "D", "dup@test.com", "Pass123!")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("DUPLICATE_EMAIL");
    }

    @Test
    void login_afterRegister_success() {
        service.register(new RegisterRequest(
                "Test", "Login", "testlogin@test.com", "Clave123!"));

        AuthResponse resp = service.login(new LoginRequest("testlogin@test.com", "Clave123!"));

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("CLIENT");
    }

    @Test
    void login_invalidPassword_throws() {
        service.register(new RegisterRequest(
                "Test", "Bad", "testbad@test.com", "CorrectPass1!"));

        assertThatThrownBy(() ->
                service.login(new LoginRequest("testbad@test.com", "WrongPass!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("INVALID_CREDENTIALS");
    }

    @Test
    void login_nonExistentEmail_throws() {
        assertThatThrownBy(() ->
                service.login(new LoginRequest("noexiste@test.com", "cualquiera")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("INVALID_CREDENTIALS");
    }
}
