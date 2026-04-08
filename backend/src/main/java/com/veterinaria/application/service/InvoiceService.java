package com.veterinaria.application.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.page.InvoicePage;
import com.veterinaria.application.dto.page.PageMeta;
import com.veterinaria.application.dto.request.InvoiceCreateRequest;
import com.veterinaria.application.dto.request.InvoiceItemRequest;
import com.veterinaria.application.dto.request.InvoicePatchRequest;
import com.veterinaria.application.dto.request.PayInvoiceRequest;
import com.veterinaria.application.dto.response.InvoiceItemResponse;
import com.veterinaria.application.dto.response.InvoiceResponse;
import com.veterinaria.domain.entity.Client;
import com.veterinaria.domain.entity.Consultation;
import com.veterinaria.domain.entity.Invoice;
import com.veterinaria.domain.entity.InvoiceItem;
import com.veterinaria.domain.entity.Product;
import com.veterinaria.domain.enums.InvoiceStatus;
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

@Service
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository      invoiceRepo;
    private final InvoiceItemRepository  itemRepo;
    private final ClientRepository       clientRepo;
    private final ConsultationRepository consultationRepo;
    private final DiagnosisRepository    diagnosisRepo;
    private final ProductRepository      productRepo;

    public InvoiceService(InvoiceRepository invoiceRepo,
                          InvoiceItemRepository itemRepo,
                          ClientRepository clientRepo,
                          ConsultationRepository consultationRepo,
                          DiagnosisRepository diagnosisRepo,
                          ProductRepository productRepo) {
        this.invoiceRepo      = invoiceRepo;
        this.itemRepo         = itemRepo;
        this.clientRepo       = clientRepo;
        this.consultationRepo = consultationRepo;
        this.diagnosisRepo    = diagnosisRepo;
        this.productRepo      = productRepo;
    }

    // LIST / GET

    // listar facturas con filtros
    public InvoicePage listInvoices(UUID clientId, InvoiceStatus status, Pageable pageable) {
        Page<Invoice> page = invoiceRepo.findByFilters(clientId, status, pageable);
        return new InvoicePage(page.getContent().stream().map(this::toResponse).toList(),
                toPageMeta(page));
    }

    // obtener detalles de factura por ID
    public InvoiceResponse getInvoice(UUID id) {
        return toResponse(findOrThrow(id));
    }

    // listar ítems de una factura
    public List<InvoiceItemResponse> listItems(UUID invoiceId) {
        findOrThrow(invoiceId);
        return itemRepo.findByInvoiceId(invoiceId).stream().map(this::toItemResponse).toList();
    }

    // CREATE

    // crear factura (estado DRAFT)
    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest req) {
        Client client = clientRepo.findByIdAndDeletedAtIsNull(req.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND",
                        "Cliente no encontrado: " + req.clientId()));

        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setTaxRate(req.taxRate());
        invoice.setNotes(req.notes());

        // BR-24: vincular consulta si se provee
        if (req.consultationId() != null) {
            if (invoiceRepo.existsByConsultationId(req.consultationId())) {
                throw new ConflictException("INVOICE_ALREADY_EXISTS",
                        "Ya existe una factura para esta consulta");
            }
            Consultation c = consultationRepo.findById(req.consultationId())
                    .orElseThrow(() -> new ResourceNotFoundException("CONSULTATION_NOT_FOUND",
                            "Consulta no encontrada: " + req.consultationId()));
            invoice.setConsultation(c);
        }

        return toResponse(invoiceRepo.save(invoice));
    }

    // ── UPDATE (solo en DRAFT)

    // actualizar datos generales de la factura
    @Transactional
    public InvoiceResponse updateInvoice(UUID id, InvoicePatchRequest req) {
        Invoice invoice = findOrThrow(id);
        requireDraft(invoice, "modificar");

        // BR-22: reasignación de titular (solo entre co-propietarios del paciente)
        if (req.clientId() != null) {
            Client client = clientRepo.findByIdAndDeletedAtIsNull(req.clientId())
                    .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND",
                            "Cliente no encontrado: " + req.clientId()));
            invoice.setClient(client);
        }
        if (req.taxRate() != null) invoice.setTaxRate(req.taxRate());
        if (req.notes()   != null) invoice.setNotes(req.notes());

        return toResponse(invoiceRepo.save(invoice));
    }

    // ITEMS

    // agregar ítem a factura
    @Transactional
    public InvoiceItemResponse addItem(UUID invoiceId, InvoiceItemRequest req) {
        Invoice invoice = findOrThrow(invoiceId);
        requireDraft(invoice, "agregar ítems a");

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(req.description());
        item.setQuantity(req.quantity());

        if (req.productId() != null) {
            Product product = productRepo.findById(req.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND",
                            "Producto no encontrado: " + req.productId()));

            // BR-17: congelar precio actual del producto
            item.setUnitPrice(product.getUnitPrice());
            item.setProduct(product);

            // BR-14: validar stock suficiente para productos físicos
            if (product.getType() != ProductType.SERVICE) {
                if (product.getStockQuantity() == null || product.getStockQuantity() < req.quantity()) {
                    throw new BusinessRuleException("INSUFFICIENT_STOCK",
                            "Stock insuficiente para el producto '" + product.getName()
                            + "' (disponible: " + product.getStockQuantity()
                            + ", solicitado: " + req.quantity() + ") (BR-14)", 422);
                }
            }

            // BR-16: prescripción activa requerida (solo si hay consulta/paciente)
            if (product.isRequiresPrescription() && invoice.getConsultation() != null) {
                UUID patientId = invoice.getConsultation().getAppointment().getPatient().getId();
                OffsetDateTime since90Days = OffsetDateTime.now().minusDays(90);
                long activePrescriptions = productRepo.countActivePrescriptions(
                        req.productId(), patientId, since90Days);
                if (activePrescriptions == 0) {
                    throw new BusinessRuleException("PRESCRIPTION_REQUIRED",
                            "El producto '" + product.getName()
                            + "' requiere prescripción activa en los últimos 90 días (BR-16)", 422);
                }
            }
        } else {
            // CE-09: ítem libre — precio provisto por el request
            item.setUnitPrice(req.unitPrice());
        }

        return toItemResponse(itemRepo.save(item));
    }

    // eliminar ítem de factura
    @Transactional
    public void removeItem(UUID invoiceId, UUID itemId) {
        Invoice invoice = findOrThrow(invoiceId);
        requireDraft(invoice, "eliminar ítems de");

        InvoiceItem item = itemRepo.findById(itemId)
                .filter(i -> i.getInvoice().getId().equals(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND",
                        "Ítem no encontrado: " + itemId));
        itemRepo.delete(item);
    }

    // STATE TRANSITIONS

    // emitir factura (DRAFT → ISSUED)
    @Transactional
    public InvoiceResponse issueInvoice(UUID id) {
        Invoice invoice = findOrThrow(id);
        requireDraft(invoice, "emitir");

        // Debe tener al menos un ítem
        List<InvoiceItem> items = itemRepo.findByInvoiceId(id);
        if (items.isEmpty()) {
            throw new ConflictException("NO_ITEMS",
                    "La factura debe tener al menos un ítem antes de emitirse");
        }

        // BR-12: si tiene consulta, debe tener ≥1 diagnóstico
        if (invoice.getConsultation() != null) {
            boolean hasDiagnosis = diagnosisRepo
                    .existsByConsultationIdAndIsPrimaryTrue(invoice.getConsultation().getId())
                    || !diagnosisRepo.findByConsultationId(invoice.getConsultation().getId()).isEmpty();
            if (!hasDiagnosis) {
                throw new ConflictException("NO_DIAGNOSIS",
                        "La consulta debe tener al menos un diagnóstico antes de facturar (BR-12)");
            }
        }

        // BR-23: calcular totales
        BigDecimal subtotal = itemRepo.sumSubtotalByInvoiceId(id);
        BigDecimal taxAmount = subtotal.multiply(invoice.getTaxRate());
        BigDecimal total = subtotal.add(taxAmount);

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotal(total);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(OffsetDateTime.now());

        // Descontar stock de productos físicos
        for (InvoiceItem item : items) {
            if (item.getProduct() != null
                    && item.getProduct().getType() != ProductType.SERVICE
                    && item.getProduct().getStockQuantity() != null) {
                Product p = item.getProduct();
                p.setStockQuantity(p.getStockQuantity() - item.getQuantity());
                productRepo.save(p);
            }
        }

        return toResponse(invoiceRepo.save(invoice));
    }

    // pagar factura (ISSUED → PAID)
    @Transactional
    public InvoiceResponse payInvoice(UUID id, PayInvoiceRequest req) {
        Invoice invoice = findOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Solo se puede pagar una factura en estado ISSUED (BR-21)");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(req.paymentMethod());
        invoice.setPaidAt(OffsetDateTime.now());
        return toResponse(invoiceRepo.save(invoice));
    }

    // cancelar factura (ISSUED → CANCELLED)
    @Transactional
    public InvoiceResponse cancelInvoice(UUID id) {
        Invoice invoice = findOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Solo se puede cancelar una factura en estado ISSUED (BR-21)");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return toResponse(invoiceRepo.save(invoice));
    }

    // reembolsar factura (PAID → REFUNDED)
    @Transactional
    public InvoiceResponse refundInvoice(UUID id) {
        Invoice invoice = findOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Solo se puede reembolsar una factura en estado PAID (BR-21)");
        }
        invoice.setStatus(InvoiceStatus.REFUNDED);
        return toResponse(invoiceRepo.save(invoice));
    }

    // HELPERS

    Invoice findOrThrow(UUID id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("INVOICE_NOT_FOUND",
                        "Factura no encontrada: " + id));
    }

    private void requireDraft(Invoice invoice, String action) {
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ConflictException("INVOICE_NOT_DRAFT",
                    "Solo se puede " + action + " una factura en estado DRAFT (BR-25)");
        }
    }

    private InvoiceResponse toResponse(Invoice i) {
        UUID consultationId = i.getConsultation() != null ? i.getConsultation().getId() : null;
        String clientName = i.getClient().getFirstName() + " " + i.getClient().getLastName();
        return new InvoiceResponse(i.getId(), i.getClient().getId(), clientName,
                consultationId, i.getStatus(), i.getSubtotal(), i.getTaxRate(),
                i.getTaxAmount(), i.getTotal(), i.getPaymentMethod(), i.getNotes(),
                i.getIssuedAt(), i.getPaidAt(), i.getCreatedAt(), i.getUpdatedAt());
    }

    private InvoiceItemResponse toItemResponse(InvoiceItem ii) {
        UUID productId = ii.getProduct() != null ? ii.getProduct().getId() : null;
        return new InvoiceItemResponse(ii.getId(), ii.getInvoice().getId(), productId,
                ii.getDescription(), ii.getQuantity(), ii.getUnitPrice(), ii.getSubtotal());
    }

    private PageMeta toPageMeta(Page<?> page) {
        return new PageMeta(page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
