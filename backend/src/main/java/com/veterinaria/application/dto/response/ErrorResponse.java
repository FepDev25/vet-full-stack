package com.veterinaria.application.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        String path,
        List<FieldDetail> details
) {

    public record FieldDetail(String field, String message) {}

    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, OffsetDateTime.now(), path, null);
    }

    public static ErrorResponse withDetails(String code, String message, String path, List<FieldDetail> details) {
        return new ErrorResponse(code, message, OffsetDateTime.now(), path, details);
    }
}
