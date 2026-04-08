package com.veterinaria.application.dto.page;

import java.util.List;

import com.veterinaria.application.dto.response.AppointmentResponse;

public record AppointmentPage(List<AppointmentResponse> content, PageMeta page) {}
