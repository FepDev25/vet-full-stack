package com.veterinaria.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PortalControllerTest extends BaseControllerIT {

    @Test
    void myPatients_withClientToken_returns200() throws Exception {
        String token = registerAndLogin("portal.client@test.com");

        mockMvc.perform(get("/api/v1/portal/patients")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void myAppointments_withClientToken_returns200() throws Exception {
        String token = registerAndLogin("portal.appts@test.com");

        mockMvc.perform(get("/api/v1/portal/appointments")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void myInvoices_withClientToken_returns200() throws Exception {
        String token = registerAndLogin("portal.invoices@test.com");

        mockMvc.perform(get("/api/v1/portal/invoices")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void portal_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/portal/patients"))
                .andExpect(status().isForbidden());
    }
}
