package com.veterinaria.application.dto.page;

import java.util.List;

import com.veterinaria.application.dto.response.PatientSummaryResponse;

public record PatientPage(List<PatientSummaryResponse> content, PageMeta page) {}
