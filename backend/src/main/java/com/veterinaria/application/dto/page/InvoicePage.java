package com.veterinaria.application.dto.page;

import java.util.List;

import com.veterinaria.application.dto.response.InvoiceResponse;

public record InvoicePage(List<InvoiceResponse> content, PageMeta page) {}
