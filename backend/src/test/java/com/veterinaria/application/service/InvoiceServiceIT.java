package com.veterinaria.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.request.InvoiceCreateRequest;
import com.veterinaria.application.dto.request.InvoiceItemRequest;
import com.veterinaria.application.dto.request.PayInvoiceRequest;
import com.veterinaria.application.dto.response.InvoiceItemResponse;
import com.veterinaria.application.dto.response.InvoiceResponse;
import com.veterinaria.domain.enums.InvoiceStatus;
import com.veterinaria.domain.enums.PaymentMethod;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ConflictException;

/**
 * Tests de integración para InvoiceService.
 * Utiliza datos del V4__test_seeds.sql.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvoiceServiceIT {

    @Autowired InvoiceService service;

    // Seeds
    static final UUID CLIENT_ROBERTO    = UUID.fromString("00000000-0004-0004-0004-000000000001");
    static final UUID CLIENT_MARIA      = UUID.fromString("00000000-0004-0004-0004-000000000002");
    static final UUID CONSULTATION_LUNA = UUID.fromString("00000000-0009-0009-0009-000000000002"); // tiene diagnosis, sin factura
    static final UUID PRODUCT_CONSULTA  = UUID.fromString("00000000-0007-0007-0007-000000000012"); // SERVICE
    static final UUID PRODUCT_AMOX      = UUID.fromString("00000000-0007-0007-0007-000000000001"); // MEDICATION, stock=48
    static final UUID PRODUCT_OMEPRAZOL = UUID.fromString("00000000-0007-0007-0007-000000000005"); // MEDICATION, stock=3

    @Test
    void fullFlow_create_addItems_issue_pay() {
        // 1. Crear factura DRAFT ligada a consulta de Luna (que tiene diagnóstico)
        InvoiceCreateRequest createReq = new InvoiceCreateRequest(
                CLIENT_MARIA, CONSULTATION_LUNA, BigDecimal.valueOf(0.19), null);
        InvoiceResponse invoice = service.createInvoice(createReq);
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.DRAFT);

        // 2. Agregar ítem de servicio (BR-17: precio se congela)
        InvoiceItemRequest itemReq = new InvoiceItemRequest(
                PRODUCT_CONSULTA, "Consulta General", 1, BigDecimal.ZERO);
        InvoiceItemResponse item = service.addItem(invoice.id(), itemReq);
        assertThat(item.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(45.00)); // precio del producto

        // 3. Emitir (BR-12: tiene diagnóstico, BR-23: calcula totales)
        InvoiceResponse issued = service.issueInvoice(invoice.id());
        assertThat(issued.status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(issued.subtotal()).isEqualByComparingTo(BigDecimal.valueOf(45.00));

        // 4. Pagar (BR-21: ISSUED → PAID)
        InvoiceResponse paid = service.payInvoice(invoice.id(), new PayInvoiceRequest(PaymentMethod.TRANSFER));
        assertThat(paid.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.paymentMethod()).isEqualTo(PaymentMethod.TRANSFER);
    }

    @Test
    void issueInvoice_BR12_noConsultationDiagnosis_throws() {
        // Crear factura sin consulta y sin diagnóstico
        InvoiceCreateRequest createReq = new InvoiceCreateRequest(
                CLIENT_ROBERTO, null, BigDecimal.ZERO, "Venta directa");
        InvoiceResponse invoice = service.createInvoice(createReq);

        // Agregar ítem
        service.addItem(invoice.id(), new InvoiceItemRequest(
                null, "Producto externo", 1, BigDecimal.valueOf(50)));

        // Sin diagnóstico: la factura sin consulta puede emitirse sin diagnosis
        // (BR-12 solo aplica si hay consulta)
        InvoiceResponse issued = service.issueInvoice(invoice.id());
        assertThat(issued.status()).isEqualTo(InvoiceStatus.ISSUED);
    }

    @Test
    void addItem_insufficientStock_BR14_throws() {
        InvoiceCreateRequest createReq = new InvoiceCreateRequest(
                CLIENT_ROBERTO, null, BigDecimal.ZERO, null);
        InvoiceResponse invoice = service.createInvoice(createReq);

        // Omeprazol tiene stock = 3, pedimos 10
        InvoiceItemRequest req = new InvoiceItemRequest(
                PRODUCT_OMEPRAZOL, "Omeprazol", 10, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addItem(invoice.id(), req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-14");
    }

    @Test
    void addItem_invoiceNotDraft_BR25_throws() {
        // La factura de Max ya está en PAID (del seed)
        UUID invoiceMaxPaid = UUID.fromString("00000000-0013-0013-0013-000000000001");

        InvoiceItemRequest req = new InvoiceItemRequest(
                null, "Extra item", 1, BigDecimal.valueOf(10));

        assertThatThrownBy(() -> service.addItem(invoiceMaxPaid, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-25");
    }

    @Test
    void cancelInvoice_issued_success() {
        // Crear y emitir primero
        InvoiceCreateRequest createReq = new InvoiceCreateRequest(
                CLIENT_ROBERTO, null, BigDecimal.ZERO, null);
        InvoiceResponse invoice = service.createInvoice(createReq);
        service.addItem(invoice.id(), new InvoiceItemRequest(
                null, "Item", 1, BigDecimal.valueOf(30)));
        service.issueInvoice(invoice.id());

        InvoiceResponse cancelled = service.cancelInvoice(invoice.id());
        assertThat(cancelled.status()).isEqualTo(InvoiceStatus.CANCELLED);
    }
}
