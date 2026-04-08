package com.veterinaria.application.dto.page;

import java.util.List;

import com.veterinaria.application.dto.response.ProductResponse;

public record ProductPage(List<ProductResponse> content, PageMeta page) {}
