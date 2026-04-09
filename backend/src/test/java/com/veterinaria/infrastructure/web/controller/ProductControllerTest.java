package com.veterinaria.infrastructure.web.controller;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.veterinaria.application.dto.request.ProductPatchRequest;
import com.veterinaria.application.dto.request.ProductRequest;
import com.veterinaria.domain.enums.ProductType;

class ProductControllerTest extends BaseControllerIT {

    @Test
    void listProducts_returns200() throws Exception {
        String token = registerAndLogin("product.list@test.com");

        mockMvc.perform(get("/api/v1/products")
                        .param("search", "a")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void getProduct_existing_returns200() throws Exception {
        String token = registerAndLogin("product.get@test.com");

        mockMvc.perform(get("/api/v1/products/{id}", PRODUCT_AMOXICILINA)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_AMOXICILINA.toString()));
    }

    @Test
    void createProduct_returns201() throws Exception {
        String token = registerAndLogin("product.create@test.com");

        ProductRequest req = new ProductRequest(
                "Suero oral", ProductType.SUPPLY, "Hidratación", "SUP-ORAL-001",
                10, new BigDecimal("5.00"), new BigDecimal("2.00"), 3, false);

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SUP-ORAL-001"));
    }

    @Test
    void replaceProduct_returns200() throws Exception {
        String token = registerAndLogin("product.put@test.com");

        ProductRequest req = new ProductRequest(
                "Amoxicilina edit", ProductType.MEDICATION, "Desc", "MED-AMOX-500-10",
                40, new BigDecimal("20.00"), new BigDecimal("8.50"), 8, true);

        mockMvc.perform(put("/api/v1/products/{id}", PRODUCT_AMOXICILINA)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Amoxicilina edit"));
    }

    @Test
    void patchProduct_returns200() throws Exception {
        String token = registerAndLogin("product.patch@test.com");

        ProductPatchRequest req = new ProductPatchRequest("Amoxicilina patch", null, 30,
                new BigDecimal("21.00"), null, 7, null, true);

        mockMvc.perform(patch("/api/v1/products/{id}", PRODUCT_AMOXICILINA)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Amoxicilina patch"));
    }

    @Test
    void lowStock_returns200() throws Exception {
        String token = registerAndLogin("product.low@test.com");

        mockMvc.perform(get("/api/v1/products/low-stock")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deactivateProduct_returns204() throws Exception {
        String token = registerAndLogin("product.delete@test.com");

        ProductRequest req = new ProductRequest(
                "Temp product", ProductType.SUPPLY, "Temp", "SUP-TEMP-XYZ",
                5, new BigDecimal("2.00"), new BigDecimal("1.00"), 1, false);

        String id = jsonId(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(delete("/api/v1/products/{id}", id)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isNoContent());
    }
}
