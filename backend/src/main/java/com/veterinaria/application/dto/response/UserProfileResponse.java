package com.veterinaria.application.dto.response;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role
) {}
