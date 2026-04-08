package com.veterinaria.application.dto.page;

public record PageMeta(
        long totalElements,
        int totalPages,
        int number,
        int size
) {}
