package com.veterinaria.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.veterinaria.ai.provider.AiResponse;

class ConsultationAiControllerTest extends BaseControllerIT {

    @MockitoBean
    private com.veterinaria.ai.provider.AiProvider aiProvider;

    private static final String VALID_SOAP_JSON = "{\"subjective\":\"S\",\"objective\":\"O\"," +
            "\"assessment\":\"A\",\"plan\":\"P\",\"suggestedDiagnoses\":[]," +
            "\"suggestedPrescriptions\":[],\"warnings\":[],\"followUp\":null,\"disclaimers\":[]}";

    @Test
    void soapSuggest_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/consultations/{id}/ai/soap-suggest", CONSULTATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freeText\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void soapSuggest_clientAuth_returns404_staffNotFound() throws Exception {
        String token = registerAndLogin("ai.test.client@x.com");
        mockMvc.perform(post("/api/v1/consultations/{id}/ai/soap-suggest", CONSULTATION_ID)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freeText\":\"test\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void soapSuggest_authed_missingFreeText_returns422() throws Exception {
        String token = registerAndLogin("ai.test.client2@x.com");
        mockMvc.perform(post("/api/v1/consultations/{id}/ai/soap-suggest", CONSULTATION_ID)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void feedback_noAuth_returns403() throws Exception {
        String body = "{\"interactionId\":\"00000000-0000-0000-0000-000000000000\",\"rating\":\"UP\"}";
        mockMvc.perform(post("/api/v1/consultations/{id}/ai/feedback", CONSULTATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void apply_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/consultations/{id}/ai/apply-suggestion", CONSULTATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void soapSuggest_vetAuth_mockedProvider_returns200() throws Exception {
        when(aiProvider.complete(any())).thenReturn(
                AiResponse.success(VALID_SOAP_JSON, 100, 50, new BigDecimal("0.000250"), 1500L));

        mockMvc.perform(post("/api/v1/consultations/{id}/ai/soap-suggest", CONSULTATION_ID)
                        .header("Authorization", authHeader(vetToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freeText\":\"Paciente con vomito\",\"includePatientHistory\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interactionId").isNotEmpty())
                .andExpect(jsonPath("$.suggestion.subjective").value("S"));
    }
}
