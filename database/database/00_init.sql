-- 00_init.sql — Extensiones de PostgreSQL
-- Sistema Veterinario

-- Generación de UUIDs (uuid_generate_v4())
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Funciones criptográficas adicionales (gen_random_uuid, hash passwords si aplica)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
