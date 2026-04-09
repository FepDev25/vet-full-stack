package com.veterinaria.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.application.dto.request.InvoiceCreateRequest;
import com.veterinaria.application.dto.request.InvoiceItemRequest;
import com.veterinaria.application.dto.request.PayInvoiceRequest;
import com.veterinaria.application.dto.response.InvoiceItemResponse;
import com.veterinaria.application.dto.response.InvoiceResponse;
import com.veterinaria.domain.entity.Client;
import com.veterinaria.domain.entity.Invoice;
import com.veterinaria.domain.entity.InvoiceItem;
import com.veterinaria.domain.entity.Product;
import com.veterinaria.domain.enums.InvoiceStatus;
import com.veterinaria.domain.enums.PaymentMethod;
import com.veterinaria.domain.enums.ProductType;
import com.veterinaria.domain.repository.ClientRepository;
import com.veterinaria.domain.repository.ConsultationRepository;
import com.veterinaria.domain.repository.DiagnosisRepository;
import com.veterinaria.domain.repository.InvoiceItemRepository;
import com.veterinaria.domain.repository.InvoiceRepository;
import com.veterinaria.domain.repository.ProductRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ConflictException;
import com.veterinaria.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository      invoiceRepo;
    @Mock InvoiceItemRepository  itemRepo;
    @Mock ClientRepository       clientRepo;
    @Mock ConsultationRepository consultationRepo;
    @Mock DiagnosisRepository    diagnosisRepo;
    @Mock ProductRepository      productRepo;

    @InjectMocks InvoiceService service;

    private Client  client;
    private Invoice draftInvoice;
    private Invoice issuedInvoice;
    private Invoice paidInvoice;
    private Product medication;
    private Product service_product;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(UUID.randomUUID());
        client.setFirstName("Roberto");
        client.setLastName("Gómez");

        draftInvoice = buildInvoice(InvoiceStatus.DRAFT);
        issuedInvoice = buildInvoice(InvoiceStatus.ISSUED);
        paidInvoice   = buildInvoice(InvoiceStatus.PAID);

        medication = new Product();
        medication.setId(UUID.randomUUID());
        medication.setName("Amoxicilina");
        medication.setType(ProductType.MEDICATION);
        medication.setStockQuantity(50);
        medication.setUnitPrice(BigDecimal.valueOf(18.00));
        medication.setRequiresPrescription(false);

        service_product = new Product();
        service_product.setId(UUID.randomUUID());
        service_product.setName("Consulta General");
        service_product.setType(ProductType.SERVICE);
        service_product.setStockQuantity(null);
        service_product.setUnitPrice(BigDecimal.valueOf(45.00));
        service_product.setRequiresPrescription(false);
    }

    // ── createInvoice ────────────────────────────────────────────────────────

    @Test
    void createInvoice_noConsultation_success() {
        when(clientRepo.findByIdAndDeletedAtIsNull(client.getId())).thenReturn(Optional.of(client));
        when(invoiceRepo.save(any())).thenReturn(draftInvoice);

        InvoiceCreateRequest req = new InvoiceCreateRequest(client.getId(), null,
                BigDecimal.valueOf(0.19), null);
        InvoiceResponse resp = service.createInvoice(req);

        assertThat(resp).isNotNull();
        assertThat(resp.status()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    void createInvoice_clientNotFound_throws() {
        when(clientRepo.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        InvoiceCreateRequest req = new InvoiceCreateRequest(UUID.randomUUID(), null,
                BigDecimal.ZERO, null);

        assertThatThrownBy(() -> service.createInvoice(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── addItem ──────────────────────────────────────────────────────────────

    @Test
    void addItem_withProduct_freezesPrice_BR17() {
        when(invoiceRepo.findById(draftInvoice.getId())).thenReturn(Optional.of(draftInvoice));
        when(productRepo.findById(medication.getId())).thenReturn(Optional.of(medication));

        InvoiceItem savedItem = buildItem(draftInvoice, medication, 1);
        when(itemRepo.save(any())).thenReturn(savedItem);

        InvoiceItemRequest req = new InvoiceItemRequest(medication.getId(),
                "Amoxicilina antibiótico", 1, BigDecimal.ZERO);
        InvoiceItemResponse resp = service.addItem(draftInvoice.getId(), req);

        // BR-17: el precio se congela del producto, no del request
        assertThat(resp.unitPrice()).isEqualByComparingTo(medication.getUnitPrice());
    }

    @Test
    void addItem_insufficientStock_BR14_throws() {
        medication.setStockQuantity(2);
        when(invoiceRepo.findById(draftInvoice.getId())).thenReturn(Optional.of(draftInvoice));
        when(productRepo.findById(medication.getId())).thenReturn(Optional.of(medication));

        InvoiceItemRequest req = new InvoiceItemRequest(medication.getId(),
                "Amoxicilina", 5, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addItem(draftInvoice.getId(), req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-14");
    }

    @Test
    void addItem_serviceProduct_skipsStockCheck_BR15() {
        when(invoiceRepo.findById(draftInvoice.getId())).thenReturn(Optional.of(draftInvoice));
        when(productRepo.findById(service_product.getId())).thenReturn(Optional.of(service_product));

        InvoiceItem savedItem = buildItem(draftInvoice, service_product, 1);
        when(itemRepo.save(any())).thenReturn(savedItem);

        InvoiceItemRequest req = new InvoiceItemRequest(service_product.getId(),
                "Consulta", 1, BigDecimal.ZERO);

        // No debe lanzar BusinessRuleException por stock
        assertThatCode(() -> service.addItem(draftInvoice.getId(), req))
                .doesNotThrowAnyException();
    }

    @Test
    void addItem_invoiceNotDraft_BR25_throws() {
        when(invoiceRepo.findById(issuedInvoice.getId())).thenReturn(Optional.of(issuedInvoice));

        InvoiceItemRequest req = new InvoiceItemRequest(null, "Item libre", 1, BigDecimal.TEN);

        assertThatThrownBy(() -> service.addItem(issuedInvoice.getId(), req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-25");
    }

    @Test
    void addItem_freeItem_usesRequestPrice_CE09() {
        when(invoiceRepo.findById(draftInvoice.getId())).thenReturn(Optional.of(draftInvoice));

        InvoiceItem savedItem = buildFreeItem(draftInvoice, BigDecimal.valueOf(100));
        when(itemRepo.save(any())).thenReturn(savedItem);

        InvoiceItemRequest req = new InvoiceItemRequest(null, "Servicio externo", 1,
                BigDecimal.valueOf(100));
        InvoiceItemResponse resp = service.addItem(draftInvoice.getId(), req);

        assertThat(resp.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    // ── issueInvoice ─────────────────────────────────────────────────────────

    @Test
    void issueInvoice_noItems_throws() {
        when(invoiceRepo.findById(draftInvoice.getId())).thenReturn(Optional.of(draftInvoice));
        when(itemRepo.findByInvoiceId(draftInvoice.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.issueInvoice(draftInvoice.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("al menos un ítem");
    }

    @Test
    void issueInvoice_withItemsNoConsultation_success_BR23() {
        InvoiceItem item = buildItem(draftInvoice, service_product, 1);
        draftInvoice.setConsultation(null);

        when(invoiceRepo.findById(draftInvoice.getId())).thenReturn(Optional.of(draftInvoice));
        when(itemRepo.findByInvoiceId(draftInvoice.getId())).thenReturn(List.of(item));
        when(itemRepo.sumSubtotalByInvoiceId(draftInvoice.getId()))
                .thenReturn(BigDecimal.valueOf(45.00));
        when(invoiceRepo.save(any())).thenReturn(draftInvoice);

        InvoiceResponse resp = service.issueInvoice(draftInvoice.getId());

        assertThat(resp.status()).isEqualTo(InvoiceStatus.ISSUED);
    }

    // ── state transitions ─────────────────────────────────────────────────────

    @Test
    void payInvoice_issued_becomesPaid_BR21() {
        when(invoiceRepo.findById(issuedInvoice.getId())).thenReturn(Optional.of(issuedInvoice));
        when(invoiceRepo.save(any())).thenReturn(issuedInvoice);

        PayInvoiceRequest req = new PayInvoiceRequest(PaymentMethod.CARD);
        InvoiceResponse resp = service.payInvoice(issuedInvoice.getId(), req);

        assertThat(resp.status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void payInvoice_draft_throws_BR21() {
        when(invoiceRepo.findById(draftInvoice.getId())).thenReturn(Optional.of(draftInvoice));

        assertThatThrownBy(() -> service.payInvoice(draftInvoice.getId(), new PayInvoiceRequest(PaymentMethod.CASH)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-21");
    }

    @Test
    void cancelInvoice_issued_becomesCancelled_BR21() {
        when(invoiceRepo.findById(issuedInvoice.getId())).thenReturn(Optional.of(issuedInvoice));
        when(invoiceRepo.save(any())).thenReturn(issuedInvoice);

        InvoiceResponse resp = service.cancelInvoice(issuedInvoice.getId());

        assertThat(resp.status()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void refundInvoice_paid_becomesRefunded_BR21() {
        when(invoiceRepo.findById(paidInvoice.getId())).thenReturn(Optional.of(paidInvoice));
        when(invoiceRepo.save(any())).thenReturn(paidInvoice);

        InvoiceResponse resp = service.refundInvoice(paidInvoice.getId());

        assertThat(resp.status()).isEqualTo(InvoiceStatus.REFUNDED);
    }

    @Test
    void refundInvoice_draft_throws_BR21() {
        when(invoiceRepo.findById(draftInvoice.getId())).thenReturn(Optional.of(draftInvoice));

        assertThatThrownBy(() -> service.refundInvoice(draftInvoice.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-21");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Invoice buildInvoice(InvoiceStatus status) {
        Invoice i = new Invoice();
        i.setId(UUID.randomUUID());
        i.setClient(client);
        i.setStatus(status);
        i.setTaxRate(BigDecimal.valueOf(0.19));
        i.setSubtotal(BigDecimal.ZERO);
        i.setTaxAmount(BigDecimal.ZERO);
        i.setTotal(BigDecimal.ZERO);
        return i;
    }

    private InvoiceItem buildItem(Invoice invoice, Product product, int quantity) {
        InvoiceItem item = new InvoiceItem();
        item.setId(UUID.randomUUID());
        item.setInvoice(invoice);
        item.setProduct(product);
        item.setDescription(product.getName());
        item.setQuantity(quantity);
        item.setUnitPrice(product.getUnitPrice());
        return item;
    }

    private InvoiceItem buildFreeItem(Invoice invoice, BigDecimal price) {
        InvoiceItem item = new InvoiceItem();
        item.setId(UUID.randomUUID());
        item.setInvoice(invoice);
        item.setDescription("Item libre");
        item.setQuantity(1);
        item.setUnitPrice(price);
        return item;
    }
}
