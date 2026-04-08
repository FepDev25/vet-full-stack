package com.veterinaria.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.InvoiceItem;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {

    List<InvoiceItem> findByInvoiceId(UUID invoiceId);

    // Suma del subtotal calculado por la BD para recalcular el total de la factura (BR-23)
    @Query("SELECT COALESCE(SUM(ii.subtotal), 0) FROM InvoiceItem ii WHERE ii.invoice.id = :invoiceId")
    BigDecimal sumSubtotalByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
