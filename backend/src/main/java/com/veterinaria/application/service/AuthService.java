package com.veterinaria.application.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.request.LoginRequest;
import com.veterinaria.application.dto.request.RefreshTokenRequest;
import com.veterinaria.application.dto.request.RegisterRequest;
import com.veterinaria.application.dto.response.AuthResponse;
import com.veterinaria.application.dto.response.TokenRefreshResponse;
import com.veterinaria.application.dto.response.UserProfileResponse;
import com.veterinaria.domain.entity.Client;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.entity.UserCredentials;
import com.veterinaria.domain.repository.ClientRepository;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.domain.repository.UserCredentialsRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ConflictException;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.security.JwtService;

import io.jsonwebtoken.JwtException;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final ClientRepository          clientRepo;
    private final StaffRepository           staffRepo;
    private final UserCredentialsRepository credRepo;
    private final PasswordEncoder           passwordEncoder;
    private final JwtService                jwtService;
    private final AuthenticationManager     authManager;

    public AuthService(ClientRepository clientRepo,
                       StaffRepository staffRepo,
                       UserCredentialsRepository credRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authManager) {
        this.clientRepo      = clientRepo;
        this.staffRepo       = staffRepo;
        this.credRepo        = credRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
        this.authManager     = authManager;
    }

    // LOGIN

    @Transactional
    public AuthResponse login(LoginRequest req) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));

            // Determinar role y userId según el tipo de usuario
            String role;
            UUID   userId;

            Optional<Staff> staffOpt = staffRepo.findByEmail(req.email());
            if (staffOpt.isPresent()) {
                Staff s = staffOpt.get();
                role   = toJwtRole(s.getRole().name());
                userId = s.getId();
                // Actualizar last_login_at
                credRepo.findByEntityIdAndEntityType(s.getId(), "STAFF")
                        .ifPresent(c -> { c.setLastLoginAt(OffsetDateTime.now()); credRepo.save(c); });
            } else {
                Client c = clientRepo.findByEmailAndDeletedAtIsNull(req.email())
                        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                                "Usuario no encontrado: " + req.email()));
                role   = "CLIENT";
                userId = c.getId();
                credRepo.findByEntityIdAndEntityType(c.getId(), "CLIENT")
                        .ifPresent(cr -> { cr.setLastLoginAt(OffsetDateTime.now()); credRepo.save(cr); });
            }

            long expMs = jwtService.getExpirationMs();
            String token        = jwtService.generateToken(req.email(), role, userId);
            String refreshToken = jwtService.generateRefreshToken(req.email());
            return new AuthResponse(token, refreshToken, role,
                    OffsetDateTime.now().plusSeconds(expMs / 1000));

        } catch (BadCredentialsException e) {
            throw new BusinessRuleException("INVALID_CREDENTIALS",
                    "Email o contraseña incorrectos", 401);
        }
    }

    // REGISTER

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (clientRepo.existsByEmailAndDeletedAtIsNull(req.email())
                || staffRepo.existsByEmail(req.email())) {
            throw new ConflictException("DUPLICATE_EMAIL",
                    "Ya existe una cuenta con el email '" + req.email() + "'");
        }

        // Crear cliente
        Client client = new Client();
        client.setFirstName(req.firstName());
        client.setLastName(req.lastName());
        client.setEmail(req.email());
        client = clientRepo.save(client);

        // Crear credenciales
        UserCredentials creds = new UserCredentials();
        creds.setEntityId(client.getId());
        creds.setEntityType("CLIENT");
        creds.setPasswordHash(passwordEncoder.encode(req.password()));
        credRepo.save(creds);

        long expMs = jwtService.getExpirationMs();
        String token        = jwtService.generateToken(req.email(), "CLIENT", client.getId());
        String refreshToken = jwtService.generateRefreshToken(req.email());
        return new AuthResponse(token, refreshToken, "CLIENT",
                OffsetDateTime.now().plusSeconds(expMs / 1000));
    }

    // REFRESH TOKEN

    public TokenRefreshResponse refreshToken(RefreshTokenRequest req) {
        try {
            if (!jwtService.isTokenValid(req.refreshToken())) {
                throw new BusinessRuleException("INVALID_TOKEN",
                        "Refresh token inválido o expirado", 401);
            }
            String email = jwtService.extractSubject(req.refreshToken());
            String role;
            UUID   userId;

            Optional<Staff> staffOpt = staffRepo.findByEmail(email);
            if (staffOpt.isPresent()) {
                Staff s = staffOpt.get();
                role   = toJwtRole(s.getRole().name());
                userId = s.getId();
            } else {
                Client c = clientRepo.findByEmailAndDeletedAtIsNull(email)
                        .orElseThrow(() -> new BusinessRuleException("INVALID_TOKEN",
                                "Token pertenece a usuario eliminado", 401));
                role   = "CLIENT";
                userId = c.getId();
            }

            long expMs = jwtService.getExpirationMs();
            String newToken = jwtService.generateToken(email, role, userId);
            return new TokenRefreshResponse(newToken,
                    OffsetDateTime.now().plusSeconds(expMs / 1000));

        } catch (JwtException e) {
            throw new BusinessRuleException("INVALID_TOKEN",
                    "Refresh token inválido o expirado", 401);
        }
    }

    // GET CURRENT USER

    public UserProfileResponse getCurrentUser(String email) {
        Optional<Staff> staffOpt = staffRepo.findByEmail(email);
        if (staffOpt.isPresent()) {
            Staff s = staffOpt.get();
            return new UserProfileResponse(s.getId(), s.getEmail(),
                    s.getFirstName(), s.getLastName(), toJwtRole(s.getRole().name()));
        }
        Client c = clientRepo.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "Usuario no encontrado: " + email));
        return new UserProfileResponse(c.getId(), c.getEmail(),
                c.getFirstName(), c.getLastName(), "CLIENT");
    }

    // HELPERS

    private String toJwtRole(String staffRole) {
        return switch (staffRole) {
            case "VETERINARIAN" -> "VETERINARIAN";
            case "RECEPTIONIST" -> "RECEPTIONIST";
            default             -> "RECEPTIONIST";
        };
    }
}
