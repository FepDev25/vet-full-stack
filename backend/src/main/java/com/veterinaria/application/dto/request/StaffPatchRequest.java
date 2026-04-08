package com.veterinaria.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record StaffPatchRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 50) String licenseNumber,
        Boolean isActive
) {}
