package com.veterinaria.application.dto.response;

import java.time.OffsetDateTime;

public record AuthResponse(
        String token,
        String refreshToken,
        String role,
        OffsetDateTime expiresAt
) {}
