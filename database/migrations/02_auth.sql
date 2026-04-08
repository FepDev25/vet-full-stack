-- 02_auth.sql — Credenciales de autenticación
-- Sistema Veterinario

-- UserCredentials referencia a staff o clients por entity_id + entity_type,

CREATE TABLE user_credentials (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_id     UUID         NOT NULL,
    entity_type   VARCHAR(10)  NOT NULL CHECK (entity_type IN ('STAFF', 'CLIENT')),
    password_hash TEXT         NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    UNIQUE (entity_id, entity_type)   -- Un staff/client solo tiene un set de credenciales
);

COMMENT ON TABLE  user_credentials              IS 'Credenciales de autenticación desacopladas del modelo de negocio. Referencia a staff o clients por entity_id + entity_type.';
COMMENT ON COLUMN user_credentials.entity_id    IS 'FK lógica al id del staff o client. No FK real para evitar acoplamiento; validado en capa de aplicación.';
COMMENT ON COLUMN user_credentials.entity_type  IS 'Discriminador: STAFF o CLIENT.';
COMMENT ON COLUMN user_credentials.password_hash IS 'Hash BCrypt de la contraseña. Nunca almacenar en claro.';
COMMENT ON COLUMN user_credentials.last_login_at IS 'Timestamp del último login exitoso. Útil para auditoría de seguridad.';

-- Índice para lookup por entity (operación más frecuente en login)
CREATE INDEX idx_user_credentials_entity ON user_credentials (entity_id, entity_type);
