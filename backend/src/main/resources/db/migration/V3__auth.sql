-- =============================================================================
-- V3__auth.sql — Credenciales de autenticación
-- Sistema Veterinario Profesional
-- =============================================================================
-- Separación de datos clínicos/de negocio de credenciales de acceso.
-- UserCredentials referencia a staff o clients por entity_id + entity_type,
-- sin acoplar el esquema de negocio a la autenticación.
-- =============================================================================

CREATE TABLE user_credentials (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_id     UUID         NOT NULL,
    entity_type   VARCHAR(10)  NOT NULL CHECK (entity_type IN ('STAFF', 'CLIENT')),
    password_hash TEXT         NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    UNIQUE (entity_id, entity_type)
);

COMMENT ON TABLE  user_credentials               IS 'Credenciales de autenticación desacopladas del modelo de negocio.';
COMMENT ON COLUMN user_credentials.entity_id     IS 'FK lógica al id del staff o client. No FK real para evitar acoplamiento.';
COMMENT ON COLUMN user_credentials.entity_type   IS 'Discriminador: STAFF o CLIENT.';
COMMENT ON COLUMN user_credentials.password_hash IS 'Hash BCrypt de la contraseña. Nunca almacenar en claro.';

-- Índice para lookup por entity (operación más frecuente en login)
CREATE INDEX idx_user_credentials_entity ON user_credentials (entity_id, entity_type);
