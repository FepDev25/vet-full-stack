package com.veterinaria.application.dto.page;

import java.util.List;

import com.veterinaria.application.dto.PageMeta;
import com.veterinaria.application.dto.response.StaffResponse;

public record StaffPage(List<StaffResponse> content, PageMeta page) {}
