package com.veterinaria.infrastructure.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.veterinaria.application.dto.request.InvoiceCreateRequest;
import com.veterinaria.application.dto.request.InvoiceItemRequest;
import com.veterinaria.application.dto.request.PayInvoiceRequest;
import com.veterinaria.domain.enums.PaymentMethod;

class InvoiceControllerTest extends BaseControllerIT {

    @Test
    void createInvoice_returns201() throws Exception {
        String token = registerAndLogin("inv.create@test.com");

        InvoiceCreateRequest req = new InvoiceCreateRequest(CLIENT_ID, null,
                new BigDecimal("0.19"), "Factura de prueba");

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(CLIENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void addItem_issue_pay_flow_returnsExpectedStates() throws Exception {
        String token = registerAndLogin("inv.flow@test.com");
        String invoiceId = createDraftInvoice(token);

        InvoiceItemRequest itemReq = new InvoiceItemRequest(
                PRODUCT_CONSULTA, "Consulta general", 1, new BigDecimal("45.00"));

        mockMvc.perform(post("/api/v1/invoices/{id}/items", invoiceId)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.productId").value(PRODUCT_CONSULTA.toString()));

        mockMvc.perform(post("/api/v1/invoices/{id}/issue", invoiceId)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));

        mockMvc.perform(post("/api/v1/invoices/{id}/pay", invoiceId)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PayInvoiceRequest(PaymentMethod.CARD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"));
    }

    @Test
    void issueThenCancel_returnsCancelled() throws Exception {
        String token = registerAndLogin("inv.cancel@test.com");
        String invoiceId = createDraftInvoice(token);

        mockMvc.perform(post("/api/v1/invoices/{id}/items", invoiceId)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InvoiceItemRequest(PRODUCT_CONSULTA, "Consulta", 1, new BigDecimal("45.00")))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/invoices/{id}/issue", invoiceId)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/invoices/{id}/cancel", invoiceId)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void listInvoices_returnsPage() throws Exception {
        String token = registerAndLogin("inv.list@test.com");

        mockMvc.perform(get("/api/v1/invoices")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void createInvoice_missingClient_returns422() throws Exception {
        String token = registerAndLogin("inv.val@test.com");

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taxRate\":0.19}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String createDraftInvoice(String token) throws Exception {
        InvoiceCreateRequest req = new InvoiceCreateRequest(CLIENT_ID, null,
                new BigDecimal("0.19"), "Borrador");

        String body = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return jsonId(body);
    }
}
