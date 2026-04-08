package com.veterinaria.exception;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.core.JacksonException;
import com.veterinaria.application.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

// Manejador global de excepciones para la API REST, captura y formatea errores de manera consistente.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handle(ConflictException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessRuleException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.FieldDetail(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.withDetails("VALIDATION_ERROR", "Validación de campos fallida",
                        req.getRequestURI(), details));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handle(DataIntegrityViolationException ex, HttpServletRequest req) {
        String cause = ex.getMostSpecificCause().getMessage();
        String code = resolveIntegrityCode(cause);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(code, "Conflicto de integridad de datos", req.getRequestURI()));
    }

    @ExceptionHandler(JacksonException.class)
    public ResponseEntity<ErrorResponse> handle(JacksonException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_JSON", "JSON inválido: " + ex.getOriginalMessage(),
                        req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Error interno del servidor", req.getRequestURI()));
    }

    private String resolveIntegrityCode(String causeMsg) {
        if (causeMsg == null) return "DATA_INTEGRITY_VIOLATION";
        if (causeMsg.contains("clients_email_key"))           return "DUPLICATE_EMAIL";
        if (causeMsg.contains("patients_microchip"))          return "DUPLICATE_MICROCHIP";
        if (causeMsg.contains("species_name_key"))            return "DUPLICATE_SPECIES";
        if (causeMsg.contains("breeds_species_id_name_key"))  return "DUPLICATE_BREED";
        if (causeMsg.contains("staff_email"))                 return "DUPLICATE_EMAIL";
        if (causeMsg.contains("staff_license_number"))        return "DUPLICATE_LICENSE_NUMBER";
        if (causeMsg.contains("products_sku_key"))            return "DUPLICATE_SKU";
        if (causeMsg.contains("BR-05"))                       return "BREED_SPECIES_MISMATCH";
        if (causeMsg.contains("BR-08"))                       return "STAFF_NOT_VETERINARIAN";
        if (causeMsg.contains("BR-18"))                       return "PRODUCT_NOT_VACCINE";
        return "DATA_INTEGRITY_VIOLATION";
    }
}
