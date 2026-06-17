package com.veterinaria.ai.audit;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiInteractionLogRepository extends JpaRepository<AiInteractionLog, UUID> {

    List<AiInteractionLog> findByFeatureOrderByCreatedAtDesc(AiFeature feature);

    List<AiInteractionLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, UUID entityId);

    List<AiInteractionLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("""
            SELECT COALESCE(SUM(a.costUsd), 0)
            FROM AiInteractionLog a
            WHERE a.feature = :feature
              AND a.status = com.veterinaria.ai.audit.AiStatus.SUCCESS
            """)
    BigDecimal sumCostByFeature(@Param("feature") AiFeature feature);

    @Modifying
    @Query("UPDATE AiInteractionLog a SET a.feedback = :feedback WHERE a.id = :id")
    int updateFeedback(@Param("id") UUID id, @Param("feedback") Short feedback);
}
