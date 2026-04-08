package com.veterinaria.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.application.dto.page.PageMeta;
import com.veterinaria.application.dto.page.ProductPage;
import com.veterinaria.application.dto.request.ProductPatchRequest;
import com.veterinaria.application.dto.request.ProductRequest;
import com.veterinaria.application.dto.response.LowStockAlertResponse;
import com.veterinaria.application.dto.response.ProductResponse;
import com.veterinaria.domain.entity.Product;
import com.veterinaria.domain.enums.ProductType;
import com.veterinaria.domain.repository.ProductRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ConflictException;
import com.veterinaria.exception.ResourceNotFoundException;

// servicio de productos
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepo;

    public ProductService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    // listar productos con filtros
    public ProductPage listProducts(ProductType type, Boolean isActive,
                                    String search, Pageable pageable) {
        Page<Product> page = productRepo.findByFilters(type, isActive, search, pageable);
        return new ProductPage(
                page.getContent().stream().map(this::toResponse).toList(),
                toPageMeta(page));
    }

    // obtener producto por ID
    public ProductResponse getProduct(UUID id) {
        return toResponse(findOrThrow(id));
    }

    // listar productos con stock bajo
    public List<LowStockAlertResponse> listLowStock() {
        return productRepo.findLowStock().stream().map(this::toLowStockAlert).toList();
    }

    // crear nuevo producto
    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        validateStockRules(req.type(), req.stockQuantity());

        if (productRepo.existsBySku(req.sku())) {
            throw new ConflictException("DUPLICATE_SKU",
                    "Ya existe un producto con el SKU '" + req.sku() + "'");
        }

        Product product = new Product();
        applyFields(product, req);
        return toResponse(productRepo.save(product));
    }

    // reemplazar producto existente
    @Transactional
    public ProductResponse replaceProduct(UUID id, ProductRequest req) {
        Product product = findOrThrow(id);
        validateStockRules(req.type(), req.stockQuantity());

        productRepo.findBySku(req.sku())
                .filter(p -> !p.getId().equals(id))
                .ifPresent(p -> { throw new ConflictException("DUPLICATE_SKU",
                        "Ya existe un producto con el SKU '" + req.sku() + "'"); });

        applyFields(product, req);
        return toResponse(productRepo.save(product));
    }

    // actualizar parcialmente producto existente
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductPatchRequest req) {
        Product product = findOrThrow(id);

        if (req.name()                != null) product.setName(req.name());
        if (req.description()         != null) product.setDescription(req.description());
        if (req.unitPrice()           != null) product.setUnitPrice(req.unitPrice());
        if (req.costPrice()           != null) product.setCostPrice(req.costPrice());
        if (req.minStockAlert()       != null) product.setMinStockAlert(req.minStockAlert());
        if (req.requiresPrescription()!= null) product.setRequiresPrescription(req.requiresPrescription());
        if (req.isActive()            != null) product.setActive(req.isActive());

        if (req.stockQuantity() != null) {
            // BR-14/BR-15: no permitir setear stock en SERVICE, ni dejar físicos sin stock
            if (product.getType() == ProductType.SERVICE) {
                throw new BusinessRuleException("SERVICE_NO_STOCK",
                        "Los productos de tipo SERVICE no tienen stock (BR-15)", 422);
            }
            product.setStockQuantity(req.stockQuantity());
        }

        return toResponse(productRepo.save(product));
    }

    // desactivar producto
    @Transactional
    public void deactivateProduct(UUID id) {
        Product product = findOrThrow(id);
        product.setActive(false);
        productRepo.save(product);
    }

    // HELPERS

    public Product findOrThrow(UUID id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND",
                        "Producto no encontrado: " + id));
    }

    private void validateStockRules(ProductType type, Integer stockQuantity) {
        if (type == ProductType.SERVICE && stockQuantity != null) {
            throw new BusinessRuleException("SERVICE_NO_STOCK",
                    "Los productos de tipo SERVICE no deben tener stock_quantity (BR-15)", 422);
        }
        if (type != ProductType.SERVICE && stockQuantity == null) {
            throw new BusinessRuleException("PHYSICAL_REQUIRES_STOCK",
                    "Los productos físicos (MEDICATION, VACCINE, SUPPLY) requieren stock_quantity (BR-15)", 422);
        }
    }

    private void applyFields(Product product, ProductRequest req) {
        product.setName(req.name());
        product.setType(req.type());
        product.setDescription(req.description());
        product.setSku(req.sku());
        product.setStockQuantity(req.stockQuantity());
        product.setUnitPrice(req.unitPrice());
        product.setCostPrice(req.costPrice());
        product.setMinStockAlert(req.minStockAlert());
        product.setRequiresPrescription(
                req.requiresPrescription() != null && req.requiresPrescription());
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getType(), p.getDescription(),
                p.getSku(), p.getStockQuantity(), p.getUnitPrice(), p.getCostPrice(),
                p.getMinStockAlert(), p.isRequiresPrescription(), p.isActive(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private LowStockAlertResponse toLowStockAlert(Product p) {
        return new LowStockAlertResponse(p.getId(), p.getName(), p.getSku(), p.getType(),
                p.getStockQuantity(), p.getMinStockAlert());
    }

    private PageMeta toPageMeta(Page<?> page) {
        return new PageMeta(page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
