-- =============================================================================
-- V2__create_schema.sql — Schema Principal
-- Sistema Veterinario Profesional
-- =============================================================================


-- =============================================================================
-- SECCIÓN 1: CATÁLOGOS
-- =============================================================================

CREATE TABLE species (
    id   UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE
);

COMMENT ON TABLE  species      IS 'Catálogo de especies animales atendidas en la clínica (Perro, Gato, Ave, etc.).';
COMMENT ON COLUMN species.name IS 'Nombre de la especie. Debe ser único para evitar duplicados (ej: no "Perro" y "perro").';


CREATE TABLE breeds (
    id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    species_id UUID         NOT NULL REFERENCES species(id) ON DELETE RESTRICT,
    name       VARCHAR(100) NOT NULL,
    UNIQUE (species_id, name)
);

COMMENT ON TABLE  breeds            IS 'Catálogo de razas. Cada raza pertenece a una especie.';
COMMENT ON COLUMN breeds.species_id IS 'Especie a la que pertenece la raza. ON DELETE RESTRICT: no se puede borrar una especie si tiene razas asociadas.';


-- =============================================================================
-- SECCIÓN 2: CLIENTES Y PACIENTES
-- =============================================================================

CREATE TABLE clients (
    id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    phone      VARCHAR(30),
    address    TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

COMMENT ON TABLE  clients            IS 'Propietarios de mascotas. Son los responsables legales y titulares de las facturas.';
COMMENT ON COLUMN clients.email      IS 'Email único por cliente. Canal principal de contacto y notificaciones.';
COMMENT ON COLUMN clients.deleted_at IS 'Soft delete. Si no es NULL, el cliente está inactivo. Su historial de facturas se conserva.';


CREATE TABLE patients (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name             VARCHAR(100) NOT NULL,
    species_id       UUID         NOT NULL REFERENCES species(id) ON DELETE RESTRICT,
    breed_id         UUID         REFERENCES breeds(id) ON DELETE SET NULL,
    birth_date       DATE,
    sex              VARCHAR(10)  NOT NULL DEFAULT 'UNKNOWN'
                                  CHECK (sex IN ('M', 'F', 'UNKNOWN')),
    weight_kg        NUMERIC(6,2) CHECK (weight_kg > 0),
    coat_color       VARCHAR(100),
    microchip_number VARCHAR(50)  UNIQUE,
    is_sterilized    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,
    deleted_at       TIMESTAMPTZ
);

COMMENT ON TABLE  patients                  IS 'Los animales (mascotas) que reciben atención en la clínica.';
COMMENT ON COLUMN patients.breed_id         IS 'Nullable: un paciente sin raza conocida (mestizo) es válido.';
COMMENT ON COLUMN patients.microchip_number IS 'Número de microchip ISO 11784/11785. Único a nivel global.';
COMMENT ON COLUMN patients.deleted_at       IS 'Soft delete. Un paciente inactivo no puede agendar citas nuevas, pero su historial clínico es inmutable.';


-- BR-01: Co-propiedad. Un paciente puede tener múltiples dueños.
CREATE TABLE client_patients (
    id               UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id        UUID        NOT NULL REFERENCES clients(id)  ON DELETE RESTRICT,
    patient_id       UUID        NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    is_primary_owner BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (client_id, patient_id)
);

COMMENT ON TABLE  client_patients                  IS 'Relación N:M entre clientes y pacientes. Permite co-propiedad (BR-01).';
COMMENT ON COLUMN client_patients.is_primary_owner IS 'Exactamente un propietario por paciente debe tener este campo en TRUE. Garantizado por índice parcial único.';


-- =============================================================================
-- SECCIÓN 3: PERSONAL
-- =============================================================================

CREATE TABLE staff (
    id             UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    phone          VARCHAR(30),
    license_number VARCHAR(50)  UNIQUE,
    role           VARCHAR(20)  NOT NULL
                                CHECK (role IN ('VETERINARIAN', 'ASSISTANT', 'RECEPTIONIST')),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ
);

-- BR-26: El número de licencia es obligatorio para veterinarios.
ALTER TABLE staff ADD CONSTRAINT staff_vet_requires_license
    CHECK (role <> 'VETERINARIAN' OR license_number IS NOT NULL);

COMMENT ON TABLE  staff                IS 'Personal de la clínica. Nunca se elimina físicamente.';
COMMENT ON COLUMN staff.license_number IS 'Obligatorio para role = VETERINARIAN (BR-26). NULL para otros roles.';
COMMENT ON COLUMN staff.is_active      IS 'Desactivación lógica. Un staff inactivo conserva su historial de consultas.';


-- =============================================================================
-- SECCIÓN 4: AGENDA Y CITAS
-- =============================================================================

CREATE TABLE appointments (
    id           UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id   UUID         NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    staff_id     UUID         NOT NULL REFERENCES staff(id)    ON DELETE RESTRICT,
    scheduled_at TIMESTAMPTZ  NOT NULL,
    status       VARCHAR(15)  NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    reason       VARCHAR(255) NOT NULL,
    notes        TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ
);

COMMENT ON TABLE  appointments              IS 'Agenda de citas. Solo citas COMPLETED pueden generar una consulta clínica (BR-06).';
COMMENT ON COLUMN appointments.scheduled_at IS 'Fecha y hora de la cita en UTC (TIMESTAMPTZ). La UI convierte al timezone del usuario.';
COMMENT ON COLUMN appointments.status       IS 'Estados: PENDING→CONFIRMED→IN_PROGRESS→COMPLETED. Alternativamente: CANCELLED o NO_SHOW.';


-- =============================================================================
-- SECCIÓN 5: CONSULTAS CLÍNICAS
-- =============================================================================

CREATE TABLE consultations (
    id             UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    appointment_id UUID         NOT NULL UNIQUE  -- BR-10: 1 consulta por cita
                                REFERENCES appointments(id) ON DELETE RESTRICT,
    staff_id       UUID         NOT NULL REFERENCES staff(id) ON DELETE RESTRICT,
    anamnesis      TEXT,
    physical_exam  TEXT,
    treatment_plan TEXT,
    weight_kg      NUMERIC(6,2) CHECK (weight_kg > 0),
    temperature_c  NUMERIC(4,1) CHECK (temperature_c BETWEEN 30.0 AND 45.0),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ
);

COMMENT ON TABLE  consultations               IS 'Registro clínico de una visita. Relación 1:1 con appointments (UNIQUE en appointment_id, BR-10).';
COMMENT ON COLUMN consultations.appointment_id IS 'FK UNIQUE: garantiza que no existan dos consultas para la misma cita (BR-10).';
COMMENT ON COLUMN consultations.staff_id       IS 'Veterinario que realizó la consulta. Validado en capa de aplicación y trigger (BR-08).';


CREATE TABLE diagnoses (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    consultation_id UUID         NOT NULL REFERENCES consultations(id) ON DELETE RESTRICT,
    cie_code        VARCHAR(20),
    description     VARCHAR(500) NOT NULL,
    severity        VARCHAR(10)  NOT NULL
                                 CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE', 'CRITICAL')),
    is_primary      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- BR-13: Exactamente un diagnóstico primario por consulta.
CREATE UNIQUE INDEX idx_diagnoses_one_primary_per_consultation
    ON diagnoses (consultation_id)
    WHERE is_primary = TRUE;

COMMENT ON TABLE  diagnoses            IS 'Diagnósticos de una consulta. Requiere al menos uno antes de facturar (BR-12).';
COMMENT ON COLUMN diagnoses.is_primary IS 'Exactamente un diagnóstico por consulta debe ser primario (BR-13). Garantizado por índice parcial único.';


-- =============================================================================
-- SECCIÓN 6: INVENTARIO Y PRODUCTOS
-- =============================================================================

CREATE TABLE products (
    id                    UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                  VARCHAR(255)  NOT NULL,
    type                  VARCHAR(15)   NOT NULL
                                        CHECK (type IN ('MEDICATION', 'VACCINE', 'SUPPLY', 'SERVICE')),
    description           TEXT,
    sku                   VARCHAR(100)  NOT NULL UNIQUE,
    stock_quantity        INTEGER       CHECK (stock_quantity >= 0),
    unit_price            NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
    cost_price            NUMERIC(10,2) CHECK (cost_price >= 0),
    min_stock_alert       INTEGER       CHECK (min_stock_alert >= 0),
    requires_prescription BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ
);

-- BR-15: Productos de tipo SERVICE no tienen stock.
ALTER TABLE products ADD CONSTRAINT products_service_no_stock
    CHECK (type <> 'SERVICE' OR stock_quantity IS NULL);

-- BR-15: Productos físicos deben tener stock definido.
ALTER TABLE products ADD CONSTRAINT products_physical_requires_stock
    CHECK (type = 'SERVICE' OR stock_quantity IS NOT NULL);

COMMENT ON TABLE  products                       IS 'Catálogo unificado de medicamentos, vacunas, insumos y servicios.';
COMMENT ON COLUMN products.stock_quantity        IS 'NULL para type=SERVICE. CHECK >= 0 garantiza que nunca sea negativo (BR-14).';
COMMENT ON COLUMN products.requires_prescription IS 'Si TRUE, el producto solo puede dispensarse vinculado a una prescripción activa (BR-16).';


-- =============================================================================
-- SECCIÓN 7: PRESCRIPCIONES Y VACUNAS
-- =============================================================================

CREATE TABLE prescriptions (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    consultation_id UUID         NOT NULL REFERENCES consultations(id) ON DELETE RESTRICT,
    product_id      UUID         NOT NULL REFERENCES products(id)      ON DELETE RESTRICT,
    dosage          VARCHAR(100) NOT NULL,
    frequency       VARCHAR(100) NOT NULL,
    duration_days   INTEGER      CHECK (duration_days > 0),
    instructions    TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  prescriptions IS 'Medicamentos recetados dentro de una consulta clínica. Habilita dispensación de productos con requires_prescription = TRUE (BR-16).';


CREATE TABLE vaccinations (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id      UUID         NOT NULL REFERENCES patients(id)  ON DELETE RESTRICT,
    product_id      UUID         NOT NULL REFERENCES products(id)  ON DELETE RESTRICT,
    staff_id        UUID         NOT NULL REFERENCES staff(id)     ON DELETE RESTRICT,
    administered_at DATE         NOT NULL DEFAULT CURRENT_DATE,
    next_due_date   DATE         CHECK (next_due_date > administered_at),
    batch_number    VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  vaccinations            IS 'Historial vacunal del paciente. Cada registro es una aplicación física de una vacuna (BR-18).';
COMMENT ON COLUMN vaccinations.product_id IS 'El producto debe tener type = VACCINE (BR-18). Validado en capa de aplicación y trigger.';


-- =============================================================================
-- SECCIÓN 8: FACTURACIÓN
-- =============================================================================

CREATE TABLE invoices (
    id              UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id       UUID          NOT NULL REFERENCES clients(id)       ON DELETE RESTRICT,
    consultation_id UUID          UNIQUE REFERENCES consultations(id)   ON DELETE RESTRICT,
    status          VARCHAR(15)   NOT NULL DEFAULT 'DRAFT'
                                  CHECK (status IN ('DRAFT', 'ISSUED', 'PAID', 'CANCELLED', 'REFUNDED')),
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    tax_rate        NUMERIC(5,4)  NOT NULL DEFAULT 0 CHECK (tax_rate >= 0),
    tax_amount      NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    total           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    payment_method  VARCHAR(10)   CHECK (payment_method IN ('CASH', 'CARD', 'TRANSFER')),
    notes           TEXT,
    issued_at       TIMESTAMPTZ,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

-- AUD-06: Si la factura está pagada, el método de pago es obligatorio.
ALTER TABLE invoices ADD CONSTRAINT invoices_paid_requires_payment_method
    CHECK (status <> 'PAID' OR payment_method IS NOT NULL);

-- AUD-07: Una factura en estado terminal debe tener fecha de emisión.
ALTER TABLE invoices ADD CONSTRAINT invoices_non_draft_requires_issued_at
    CHECK (status = 'DRAFT' OR issued_at IS NOT NULL);

COMMENT ON TABLE  invoices                IS 'Facturas emitidas a clientes. Pueden estar asociadas a una consulta o ser libres (BR-24).';
COMMENT ON COLUMN invoices.consultation_id IS 'FK UNIQUE nullable. Si NULL, la factura es por venta directa sin consulta asociada (BR-24).';
COMMENT ON COLUMN invoices.status          IS 'Estados: DRAFT→ISSUED→PAID. Flujos alternativos: CANCELLED, REFUNDED. Ítems inmutables desde ISSUED (BR-25).';


CREATE TABLE invoice_items (
    id          UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id  UUID          NOT NULL REFERENCES invoices(id)  ON DELETE RESTRICT,
    product_id  UUID          REFERENCES products(id) ON DELETE RESTRICT,
    description VARCHAR(500)  NOT NULL,
    quantity    INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
    -- AUD-04: columna generada — elimina riesgo de inconsistencia.
    subtotal    NUMERIC(12,2) GENERATED ALWAYS AS (quantity * unit_price) STORED
);

COMMENT ON TABLE  invoice_items            IS 'Líneas de detalle de una factura. El precio se congela al momento de agregar el ítem (BR-17).';
COMMENT ON COLUMN invoice_items.subtotal   IS 'GENERATED ALWAYS AS (quantity * unit_price) STORED (AUD-04).';
COMMENT ON COLUMN invoice_items.unit_price IS 'Precio unitario congelado al momento de agregar el ítem (BR-17).';


-- =============================================================================
-- SECCIÓN 9: ÍNDICES
-- =============================================================================

CREATE INDEX idx_clients_deleted_at         ON clients (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_clients_name               ON clients (last_name, first_name);

CREATE INDEX idx_patients_species_id        ON patients (species_id);
CREATE INDEX idx_patients_breed_id          ON patients (breed_id);
CREATE INDEX idx_patients_deleted_at        ON patients (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_patients_name              ON patients (name);

CREATE INDEX idx_client_patients_client_id  ON client_patients (client_id);
CREATE INDEX idx_client_patients_patient_id ON client_patients (patient_id);

-- BR-01: Exactamente un propietario primario por paciente.
CREATE UNIQUE INDEX idx_client_patients_one_primary
    ON client_patients (patient_id)
    WHERE is_primary_owner = TRUE;

CREATE INDEX idx_staff_role                 ON staff (role);
CREATE INDEX idx_staff_is_active            ON staff (is_active) WHERE is_active = TRUE;

CREATE INDEX idx_appointments_patient_id    ON appointments (patient_id);
CREATE INDEX idx_appointments_scheduled_at  ON appointments (scheduled_at);
CREATE INDEX idx_appointments_status        ON appointments (status);
CREATE INDEX idx_appointments_staff_date    ON appointments (staff_id, scheduled_at);

CREATE INDEX idx_consultations_staff_id     ON consultations (staff_id);

CREATE INDEX idx_diagnoses_consultation_id  ON diagnoses (consultation_id);
CREATE INDEX idx_diagnoses_cie_code         ON diagnoses (cie_code) WHERE cie_code IS NOT NULL;

CREATE INDEX idx_products_type              ON products (type);
CREATE INDEX idx_products_is_active         ON products (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_products_low_stock         ON products (stock_quantity, min_stock_alert)
    WHERE type <> 'SERVICE' AND is_active = TRUE;

CREATE INDEX idx_prescriptions_consultation_id ON prescriptions (consultation_id);
CREATE INDEX idx_prescriptions_product_id      ON prescriptions (product_id);

CREATE INDEX idx_vaccinations_patient_id    ON vaccinations (patient_id);
CREATE INDEX idx_vaccinations_product_id    ON vaccinations (product_id);
CREATE INDEX idx_vaccinations_next_due      ON vaccinations (next_due_date) WHERE next_due_date IS NOT NULL;

CREATE INDEX idx_invoices_client_id         ON invoices (client_id);
CREATE INDEX idx_invoices_status            ON invoices (status);
CREATE INDEX idx_invoices_issued_at         ON invoices (issued_at);

CREATE INDEX idx_invoice_items_invoice_id   ON invoice_items (invoice_id);
CREATE INDEX idx_invoice_items_product_id   ON invoice_items (product_id) WHERE product_id IS NOT NULL;


-- =============================================================================
-- SECCIÓN 10: TRIGGERS DE INTEGRIDAD
-- AUD-01, AUD-02, AUD-03
-- =============================================================================

-- AUD-01 / BR-05: La raza del paciente debe pertenecer a su especie.
CREATE OR REPLACE FUNCTION fn_check_patient_breed_species()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.breed_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM breeds
            WHERE id = NEW.breed_id
              AND species_id = NEW.species_id
        ) THEN
            RAISE EXCEPTION
                'breed_id % no pertenece a la especie % del paciente. (BR-05)',
                NEW.breed_id, NEW.species_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_patients_breed_species
    BEFORE INSERT OR UPDATE OF breed_id, species_id ON patients
    FOR EACH ROW EXECUTE FUNCTION fn_check_patient_breed_species();


-- AUD-02 / BR-08: Solo personal con role = VETERINARIAN puede realizar consultas.
CREATE OR REPLACE FUNCTION fn_check_consultation_staff_is_vet()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM staff
        WHERE id = NEW.staff_id
          AND role = 'VETERINARIAN'
          AND is_active = TRUE
    ) THEN
        RAISE EXCEPTION
            'staff_id % no es un VETERINARIAN activo. Solo veterinarios pueden realizar consultas. (BR-08)',
            NEW.staff_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_consultations_staff_is_vet
    BEFORE INSERT OR UPDATE OF staff_id ON consultations
    FOR EACH ROW EXECUTE FUNCTION fn_check_consultation_staff_is_vet();


-- AUD-03 / BR-18: Solo productos de tipo VACCINE pueden registrarse en vaccinations.
CREATE OR REPLACE FUNCTION fn_check_vaccination_product_is_vaccine()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM products
        WHERE id = NEW.product_id
          AND type = 'VACCINE'
    ) THEN
        RAISE EXCEPTION
            'product_id % no es de tipo VACCINE. Solo vacunas pueden registrarse en vaccinations. (BR-18)',
            NEW.product_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_vaccinations_product_is_vaccine
    BEFORE INSERT OR UPDATE OF product_id ON vaccinations
    FOR EACH ROW EXECUTE FUNCTION fn_check_vaccination_product_is_vaccine();
