package com.veterinaria.application.dto.response;

public record DashboardSummaryResponse(
        long todayAppointments,
        long pendingInvoices,
        long lowStockProducts
) {}
