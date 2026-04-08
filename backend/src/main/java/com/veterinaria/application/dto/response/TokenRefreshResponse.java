package com.veterinaria.application.dto.response;

import java.time.OffsetDateTime;

public record TokenRefreshResponse(String token, OffsetDateTime expiresAt) {}
