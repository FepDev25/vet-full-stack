package com.veterinaria.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.application.dto.request.ProductPatchRequest;
import com.veterinaria.application.dto.request.ProductRequest;
import com.veterinaria.application.dto.response.ProductResponse;
import com.veterinaria.domain.entity.Product;
import com.veterinaria.domain.enums.ProductType;
import com.veterinaria.domain.repository.ProductRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ConflictException;
import com.veterinaria.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepo;

    @InjectMocks ProductService service;

    private Product medicationProduct;
    private Product serviceProduct;

    @BeforeEach
    void setUp() {
        medicationProduct = new Product();
        medicationProduct.setId(UUID.randomUUID());
        medicationProduct.setName("Amoxicilina 500mg");
        medicationProduct.setType(ProductType.MEDICATION);
        medicationProduct.setSku("MED-AMOX-001");
        medicationProduct.setStockQuantity(50);
        medicationProduct.setUnitPrice(BigDecimal.valueOf(18.00));
        medicationProduct.setActive(true);

        serviceProduct = new Product();
        serviceProduct.setId(UUID.randomUUID());
        serviceProduct.setName("Consulta General");
        serviceProduct.setType(ProductType.SERVICE);
        serviceProduct.setSku("SVC-CONSULTA-001");
        serviceProduct.setStockQuantity(null);
        serviceProduct.setUnitPrice(BigDecimal.valueOf(45.00));
        serviceProduct.setActive(true);
    }

    // ── BR-15: SERVICE sin stock, físicos con stock ───────────────────────────

    @Test
    void createProduct_medication_withStock_success_BR15() {
        when(productRepo.existsBySku("MED-AMOX-001")).thenReturn(false);
        when(productRepo.save(any())).thenReturn(medicationProduct);

        ProductRequest req = new ProductRequest(
                "Amoxicilina 500mg", ProductType.MEDICATION, "Antibiótico",
                "MED-AMOX-001", 50, BigDecimal.valueOf(18), BigDecimal.valueOf(8), 10,
                true);

        ProductResponse resp = service.createProduct(req);

        assertThat(resp.type()).isEqualTo(ProductType.MEDICATION);
        assertThat(resp.stockQuantity()).isEqualTo(50);
    }

    @Test
    void createProduct_service_withoutStock_success_BR15() {
        when(productRepo.existsBySku("SVC-001")).thenReturn(false);
        when(productRepo.save(any())).thenReturn(serviceProduct);

        ProductRequest req = new ProductRequest(
                "Consulta", ProductType.SERVICE, "Desc",
                "SVC-001", null, BigDecimal.valueOf(45), null, null, false);

        ProductResponse resp = service.createProduct(req);

        assertThat(resp.type()).isEqualTo(ProductType.SERVICE);
        assertThat(resp.stockQuantity()).isNull();
    }

    @Test
    void createProduct_service_withStock_BR15_throws() {
        ProductRequest req = new ProductRequest(
                "Consulta", ProductType.SERVICE, "Desc",
                "SVC-001", 10, BigDecimal.valueOf(45), null, null, false);

        assertThatThrownBy(() -> service.createProduct(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-15");
    }

    @Test
    void createProduct_medication_withoutStock_BR15_throws() {
        ProductRequest req = new ProductRequest(
                "Amoxicilina", ProductType.MEDICATION, "Antibiótico",
                "MED-001", null, BigDecimal.valueOf(18), null, null, false);

        assertThatThrownBy(() -> service.createProduct(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-15");
    }

    @Test
    void createProduct_duplicateSku_throws() {
        when(productRepo.existsBySku("MED-AMOX-001")).thenReturn(true);

        ProductRequest req = new ProductRequest(
                "Amoxicilina", ProductType.MEDICATION, null,
                "MED-AMOX-001", 10, BigDecimal.valueOf(18), null, null, false);

        assertThatThrownBy(() -> service.createProduct(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    void updateProduct_setStockOnService_BR14_throws() {
        when(productRepo.findById(serviceProduct.getId())).thenReturn(Optional.of(serviceProduct));

        ProductPatchRequest req = new ProductPatchRequest(null, null, 5, null, null, null, null, null);

        assertThatThrownBy(() -> service.updateProduct(serviceProduct.getId(), req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-15");
    }

    @Test
    void getProduct_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(productRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Producto no encontrado");
    }
}
