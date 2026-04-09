package com.veterinaria.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest extends BaseControllerIT {

    @Test
    void summary_withAuth_returns200() throws Exception {
        String token = registerAndLogin("dashboard@test.com");

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayAppointments").isNumber())
                .andExpect(jsonPath("$.pendingInvoices").isNumber())
                .andExpect(jsonPath("$.lowStockProducts").isNumber());
    }

    @Test
    void summary_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isForbidden());
    }
}
