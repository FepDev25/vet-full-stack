package com.veterinaria.ai.features.soap;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.ai.features.soap.dto.SoapSuggestRequest;
import com.veterinaria.ai.features.soap.dto.SoapSuggestResponse;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.enums.StaffRole;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.exception.BusinessRuleException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/consultations/{consultationId}/ai")
public class ConsultationAiController {

    private final SoapAssistantService soapAssistantService;
    private final StaffRepository staffRepository;

    public ConsultationAiController(SoapAssistantService soapAssistantService,
                                    StaffRepository staffRepository) {
        this.soapAssistantService = soapAssistantService;
        this.staffRepository = staffRepository;
    }

    @PostMapping("/soap-suggest")
    public ResponseEntity<SoapSuggestResponse> suggestSoap(
            @PathVariable UUID consultationId,
            @Valid @RequestBody SoapSuggestRequest req,
            Authentication auth) {
        UUID vetId = resolveActiveVeterinarian(auth);
        boolean includeHistory = Boolean.TRUE.equals(req.includePatientHistory());

        SoapAssistantService.SoapSuggestionResult result = soapAssistantService.suggest(
                consultationId, req.freeText(), includeHistory, vetId);

        return ResponseEntity.ok(new SoapSuggestResponse(result.interactionId(), result.suggestion()));
    }

    private UUID resolveActiveVeterinarian(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessRuleException("UNAUTHENTICATED", "No autenticado", 401);
        }
        String email = auth.getName();
        Staff vet = staffRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("STAFF_NOT_FOUND",
                        "Staff no encontrado: " + email, 404));
        if (vet.getRole() != StaffRole.VETERINARIAN) {
            throw new BusinessRuleException("NOT_VETERINARIAN",
                    "Solo un VETERINARIAN puede usar el asistente IA", 403);
        }
        if (!vet.isActive()) {
            throw new BusinessRuleException("STAFF_INACTIVE",
                    "El veterinario no esta activo", 403);
        }
        return vet.getId();
    }
}
