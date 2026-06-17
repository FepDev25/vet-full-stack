-- =============================================================================
-- V6__ai_audit.sql — Auditoria de interacciones con IA
-- Sistema Veterinario
-- =============================================================================
-- Tabla de log para llamadas a providers de IA (Anthropic, Google GenAI, etc.).
-- Una fila por llamada. Inmutable salvo `feedback` (set via endpoint dedicado).
-- Permite auditar uso, costos, latencia y calidad a lo largo del tiempo.
-- =============================================================================

CREATE TABLE ai_interaction_log (
    id                UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    feature           VARCHAR(50)   NOT NULL,
    entity_type       VARCHAR(50)   NOT NULL,
    entity_id         UUID          NOT NULL,
    user_id           UUID,
    model             VARCHAR(100)  NOT NULL,
    prompt_hash       VARCHAR(64)   NOT NULL,
    output_hash       VARCHAR(64)   NOT NULL,
    prompt_tokens     INTEGER,
    completion_tokens INTEGER,
    cost_usd          NUMERIC(10, 6),
    latency_ms        INTEGER       NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    error_message     TEXT,
    feedback          SMALLINT,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_ai_log_status   CHECK (status IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_ai_log_feedback CHECK (feedback IS NULL OR feedback IN (-1, 1))
);

COMMENT ON TABLE  ai_interaction_log                  IS 'Auditoria inmutable de llamadas a providers de IA. Solo se actualiza feedback.';
COMMENT ON COLUMN ai_interaction_log.feature         IS 'Feature IA que origino la llamada (SOAP_ASSISTANT, etc).';
COMMENT ON COLUMN ai_interaction_log.entity_type     IS 'Tipo de entidad afectada (consultation, patient, etc).';
COMMENT ON COLUMN ai_interaction_log.entity_id       IS 'ID de la entidad afectada.';
COMMENT ON COLUMN ai_interaction_log.user_id         IS 'ID del usuario que disparo la llamada (NULL para system-triggered).';
COMMENT ON COLUMN ai_interaction_log.model           IS 'Modelo del provider usado (ej: claude-haiku-4-5).';
COMMENT ON COLUMN ai_interaction_log.prompt_hash     IS 'SHA-256 del prompt final enviado al provider.';
COMMENT ON COLUMN ai_interaction_log.output_hash     IS 'SHA-256 del output crudo del provider.';
COMMENT ON COLUMN ai_interaction_log.prompt_tokens   IS 'Tokens consumidos en el prompt (input).';
COMMENT ON COLUMN ai_interaction_log.completion_tokens IS 'Tokens generados en la respuesta (output).';
COMMENT ON COLUMN ai_interaction_log.cost_usd         IS 'Costo estimado en USD al momento de la llamada.';
COMMENT ON COLUMN ai_interaction_log.latency_ms       IS 'Latencia end-to-end en milisegundos.';
COMMENT ON COLUMN ai_interaction_log.status          IS 'SUCCESS o FAILURE.';
COMMENT ON COLUMN ai_interaction_log.error_message   IS 'Mensaje de error si status=FAILURE.';
COMMENT ON COLUMN ai_interaction_log.feedback        IS '1=positivo, -1=negativo, NULL=sin feedback.';

CREATE INDEX idx_ai_log_feature      ON ai_interaction_log (feature);
CREATE INDEX idx_ai_log_entity       ON ai_interaction_log (entity_type, entity_id);
CREATE INDEX idx_ai_log_user_created ON ai_interaction_log (user_id, created_at DESC);
