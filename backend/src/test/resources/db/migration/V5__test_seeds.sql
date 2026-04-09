-- =============================================================================
-- V4__test_seeds.sql — Datos de referencia para tests de integración
-- Solo se carga en perfil "test" (src/test/resources)
-- UUIDs fijos para garantizar integridad referencial entre tests
-- =============================================================================

-- Especies
INSERT INTO species (id, name) VALUES
    ('00000000-0001-0001-0001-000000000001', 'Perro'),
    ('00000000-0001-0001-0001-000000000002', 'Gato');

-- Razas
INSERT INTO breeds (id, species_id, name) VALUES
    ('00000000-0002-0002-0002-000000000001', '00000000-0001-0001-0001-000000000001', 'Labrador Retriever'),
    ('00000000-0002-0002-0002-000000000006', '00000000-0001-0001-0001-000000000002', 'Persa');

-- Personal
INSERT INTO staff (id, first_name, last_name, email, phone, license_number, role, is_active) VALUES
    ('00000000-0003-0003-0003-000000000001',
     'Carlos', 'Mendoza', 'c.mendoza@vetclinica.com', '+57-310-0000001',
     'VET-COL-2018-0042', 'VETERINARIAN', TRUE),
    ('00000000-0003-0003-0003-000000000003',
     'Pedro', 'Ramírez', 'p.ramirez@vetclinica.com', '+57-300-0000003',
     NULL, 'ASSISTANT', TRUE);

-- Clientes
INSERT INTO clients (id, first_name, last_name, email, phone) VALUES
    ('00000000-0004-0004-0004-000000000001',
     'Roberto', 'Gómez', 'roberto.gomez@gmail.com', '+57-312-0000001'),
    ('00000000-0004-0004-0004-000000000002',
     'María', 'Torres', 'maria.torres@outlook.com', '+57-318-0000002');

-- Pacientes
INSERT INTO patients (id, name, species_id, breed_id, birth_date, sex, weight_kg, is_sterilized) VALUES
    ('00000000-0005-0005-0005-000000000001',
     'Max', '00000000-0001-0001-0001-000000000001', '00000000-0002-0002-0002-000000000001',
     '2022-01-15', 'M', 32.50, FALSE),
    ('00000000-0005-0005-0005-000000000002',
     'Luna', '00000000-0001-0001-0001-000000000002', '00000000-0002-0002-0002-000000000006',
     '2023-04-20', 'F', 4.20, TRUE),
    ('00000000-0005-0005-0005-000000000003',
     'Rocky', '00000000-0001-0001-0001-000000000001', '00000000-0002-0002-0002-000000000001',
     '2024-03-10', 'M', 11.80, FALSE);

-- Soft-delete de Rocky para test CE-10
-- (No lo soft-deletamos aquí; lo hace el test)

-- Co-propiedad
INSERT INTO client_patients (id, client_id, patient_id, is_primary_owner) VALUES
    ('00000000-0006-0006-0006-000000000001',
     '00000000-0004-0004-0004-000000000001',
     '00000000-0005-0005-0005-000000000001', TRUE),
    ('00000000-0006-0006-0006-000000000002',
     '00000000-0004-0004-0004-000000000002',
     '00000000-0005-0005-0005-000000000002', TRUE),
    ('00000000-0006-0006-0006-000000000003',
     '00000000-0004-0004-0004-000000000001',
     '00000000-0005-0005-0005-000000000003', TRUE);

-- Productos
INSERT INTO products (id, name, type, description, sku, stock_quantity,
                      unit_price, cost_price, min_stock_alert, requires_prescription, is_active) VALUES
    ('00000000-0007-0007-0007-000000000001',
     'Amoxicilina 500 mg', 'MEDICATION', 'Antibiótico',
     'MED-AMOX-500-10', 48, 18.00, 8.50, 10, TRUE, TRUE),
    ('00000000-0007-0007-0007-000000000006',
     'Nobivac DHPPi', 'VACCINE', 'Vacuna polivalente canina',
     'VAC-NOBIVAC-DHPPI', 22, 35.00, 16.00, 5, FALSE, TRUE),
    ('00000000-0007-0007-0007-000000000012',
     'Consulta General', 'SERVICE', 'Consulta clínica estándar',
     'SVC-CONSULTA-GEN', NULL, 45.00, NULL, NULL, FALSE, TRUE),
    ('00000000-0007-0007-0007-000000000005',
     'Omeprazol 20mg', 'MEDICATION', 'Gastroprotector',
     'MED-OMEP-20-30', 3, 19.00, 8.00, 8, FALSE, TRUE);

-- Citas (COMPLETED para tener historial; CONFIRMED para testear flujos)
INSERT INTO appointments (id, patient_id, staff_id, scheduled_at, status, reason) VALUES
    -- Max: COMPLETED → ya tiene consulta + factura PAID (BR-11 test)
    ('00000000-0008-0008-0008-000000000001',
     '00000000-0005-0005-0005-000000000001',
     '00000000-0003-0003-0003-000000000001',
     '2026-03-08 10:00:00+00', 'COMPLETED',
     'Rascado intenso y secreción en oídos'),
    -- Luna: COMPLETED → tiene consulta SIN factura (test de issue con diagnosis)
    ('00000000-0008-0008-0008-000000000002',
     '00000000-0005-0005-0005-000000000002',
     '00000000-0003-0003-0003-000000000001',
     '2026-02-20 09:00:00+00', 'COMPLETED',
     'Estornudos frecuentes'),
    -- Rocky: CONFIRMED → para testear flujos de estado
    ('00000000-0008-0008-0008-000000000004',
     '00000000-0005-0005-0005-000000000003',
     '00000000-0003-0003-0003-000000000001',
     '2026-03-28 09:00:00+00', 'CONFIRMED',
     'Control de crecimiento');

-- Consultas (solo de citas COMPLETED)
INSERT INTO consultations (id, appointment_id, staff_id, anamnesis, physical_exam,
                           treatment_plan, weight_kg, temperature_c) VALUES
    -- Consulta de Max (tiene factura PAID → BR-11)
    ('00000000-0009-0009-0009-000000000001',
     '00000000-0008-0008-0008-000000000001',
     '00000000-0003-0003-0003-000000000001',
     'Rascado de oídos bilateral.', 'Otoscopia: exudado marrón.',
     'Amoxicilina + limpieza ótica.', 32.10, 38.40),
    -- Consulta de Luna (sin factura aún)
    ('00000000-0009-0009-0009-000000000002',
     '00000000-0008-0008-0008-000000000002',
     '00000000-0003-0003-0003-000000000001',
     'Estornudos frecuentes.', 'Secreción nasal serosa bilateral.',
     'Metronidazol + nebulización.', 4.15, 39.10);

-- Diagnósticos (BR-12: consulta necesita ≥1 antes de facturar)
INSERT INTO diagnoses (id, consultation_id, cie_code, description, severity, is_primary) VALUES
    ('00000000-0010-0010-0010-000000000001',
     '00000000-0009-0009-0009-000000000001',
     'H60.3', 'Otitis externa bacteriana bilateral', 'MODERATE', TRUE),
    ('00000000-0010-0010-0010-000000000002',
     '00000000-0009-0009-0009-000000000002',
     'J06.9', 'Rinotraqueítis viral felina aguda', 'MILD', TRUE);

-- Prescripción de Amoxicilina en consulta de Max (para BR-16 test)
INSERT INTO prescriptions (id, consultation_id, product_id, dosage, frequency, duration_days) VALUES
    ('00000000-0011-0011-0011-000000000001',
     '00000000-0009-0009-0009-000000000001',
     '00000000-0007-0007-0007-000000000001',
     '15 mg/kg', 'Cada 12 horas', 10);

-- Factura de Max: PAID (para testear BR-11: consulta bloqueada)
INSERT INTO invoices (id, client_id, consultation_id, status,
                      subtotal, tax_rate, tax_amount, total,
                      payment_method, issued_at, paid_at) VALUES
    ('00000000-0013-0013-0013-000000000001',
     '00000000-0004-0004-0004-000000000001',
     '00000000-0009-0009-0009-000000000001',
     'PAID',
     63.00, 0.1900, 11.97, 74.97,
     'CARD',
     '2026-03-08 11:45:00+00',
     '2026-03-08 11:47:00+00');

INSERT INTO invoice_items (id, invoice_id, product_id, description, quantity, unit_price) VALUES
    ('00000000-0014-0014-0014-000000000001',
     '00000000-0013-0013-0013-000000000001',
     '00000000-0007-0007-0007-000000000012',
     'Consulta General', 1, 45.00),
    ('00000000-0014-0014-0014-000000000002',
     '00000000-0013-0013-0013-000000000001',
     '00000000-0007-0007-0007-000000000001',
     'Amoxicilina 500mg', 1, 18.00);

-- Nota: user_credentials se crean programáticamente en AuthServiceIT
-- para evitar hardcodear hashes BCrypt en SQL.
