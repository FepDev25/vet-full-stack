package com.veterinaria.domain.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.Invoice;
import com.veterinaria.domain.enums.InvoiceStatus;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    boolean existsByConsultationId(UUID consultationId);

    // filtros opcionales por cliente y estado (DRAFT, ISSUED, PAID).
    @Query("""
            SELECT i FROM Invoice i
            WHERE (:clientId IS NULL OR i.client.id = :clientId)
              AND (:status   IS NULL OR i.status   = :status)
            """)
    Page<Invoice> findByFilters(
            @Param("clientId") UUID clientId,
            @Param("status")   InvoiceStatus status,
            Pageable pageable);

    // BR-11: verifica si existe factura PAID para una consulta (para bloquear updateConsultation).
    @Query("SELECT COUNT(i) > 0 FROM Invoice i WHERE i.consultation.id = :consultationId AND i.status = com.veterinaria.domain.enums.InvoiceStatus.PAID")
    boolean existsPaidInvoiceForConsultation(@Param("consultationId") UUID consultationId);

    // Dashboard: contar facturas en estado DRAFT o ISSUED.
    long countByStatusIn(java.util.List<InvoiceStatus> statuses);
}
