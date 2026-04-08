package com.veterinaria.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.domain.entity.Product;
import com.veterinaria.domain.enums.ProductType;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("""
            SELECT p FROM Product p
            WHERE (:type     IS NULL OR p.type     = :type)
              AND (:isActive IS NULL OR p.isActive = :isActive)
              AND (:search   IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.sku)  LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Product> findByFilters(
            @Param("type")     ProductType type,
            @Param("isActive") Boolean isActive,
            @Param("search")   String search,
            Pageable pageable);

    // BR-14: productos físicos con stock <= minStockAlert
    @Query("""
            SELECT p FROM Product p
            WHERE p.type <> com.veterinaria.domain.enums.ProductType.SERVICE
              AND p.stockQuantity IS NOT NULL
              AND p.minStockAlert IS NOT NULL
              AND p.stockQuantity <= p.minStockAlert
              AND p.isActive = true
            """)
    List<Product> findLowStock();

    // Para BR-16: comprueba prescripción activa en los últimos N días.
    @Query("""
            SELECT COUNT(pr) FROM Prescription pr
            WHERE pr.product.id = :productId
              AND pr.consultation.appointment.patient.id = :patientId
              AND pr.createdAt > :since
            """)
    long countActivePrescriptions(
            @Param("productId")  UUID productId,
            @Param("patientId")  UUID patientId,
            @Param("since")      java.time.OffsetDateTime since);
}
