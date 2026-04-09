package com.veterinaria.application.dto.page;

import java.util.List;

import com.veterinaria.application.dto.response.ConsultationResponse;

public record ConsultationPage(List<ConsultationResponse> content, PageMeta page) {}
