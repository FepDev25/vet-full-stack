-- =============================================================================
-- V1__init_extensions.sql — Extensiones de PostgreSQL
-- Sistema Veterinario Profesional
-- =============================================================================

-- Generación de UUIDs (uuid_generate_v4())
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Funciones criptográficas adicionales
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
