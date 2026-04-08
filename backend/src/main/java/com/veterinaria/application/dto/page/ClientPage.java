package com.veterinaria.application.dto.page;

import java.util.List;

import com.veterinaria.application.dto.PageMeta;
import com.veterinaria.application.dto.response.ClientResponse;

public record ClientPage(List<ClientResponse> content, PageMeta page) {}
