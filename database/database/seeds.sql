-- seeds.sql — Datos de Prueba Realistas
-- Sistema Veterinario

-- Cubre 8 escenarios clínicos y de negocio.
-- Los UUIDs son fijos para garantizar integridad referencial entre INSERTs.

-- SECCIÓN 1: CATÁLOGOS — Especies y Razas

INSERT INTO species (id, name) VALUES
    ('00000000-0001-0001-0001-000000000001', 'Perro'),
    ('00000000-0001-0001-0001-000000000002', 'Gato'),
    ('00000000-0001-0001-0001-000000000003', 'Ave'),
    ('00000000-0001-0001-0001-000000000004', 'Conejo');

INSERT INTO breeds (id, species_id, name) VALUES
    -- Perro
    ('00000000-0002-0002-0002-000000000001', '00000000-0001-0001-0001-000000000001', 'Labrador Retriever'),
    ('00000000-0002-0002-0002-000000000002', '00000000-0001-0001-0001-000000000001', 'Golden Retriever'),
    ('00000000-0002-0002-0002-000000000003', '00000000-0001-0001-0001-000000000001', 'Bulldog Francés'),
    ('00000000-0002-0002-0002-000000000004', '00000000-0001-0001-0001-000000000001', 'Beagle'),
    ('00000000-0002-0002-0002-000000000005', '00000000-0001-0001-0001-000000000001', 'Pastor Alemán'),
    -- Gato
    ('00000000-0002-0002-0002-000000000006', '00000000-0001-0001-0001-000000000002', 'Persa'),
    ('00000000-0002-0002-0002-000000000007', '00000000-0001-0001-0001-000000000002', 'Siamés'),
    ('00000000-0002-0002-0002-000000000008', '00000000-0001-0001-0001-000000000002', 'Maine Coon');
    -- Ave y Conejo no tienen razas registradas en esta clínica

-- SECCIÓN 2: PERSONAL

INSERT INTO staff (id, first_name, last_name, email, phone, license_number, role, is_active) VALUES
    -- Veterinario principal, 8 años de experiencia en medicina interna
    ('00000000-0003-0003-0003-000000000001',
     'Carlos', 'Mendoza Ríos',
     'c.mendoza@vetclinica.com', '+57-310-4521890',
     'VET-COL-2018-0042', 'VETERINARIAN', TRUE),

    -- Veterinaria especializada en felinos y exóticos
    ('00000000-0003-0003-0003-000000000002',
     'Ana', 'Jiménez Salcedo',
     'a.jimenez@vetclinica.com', '+57-315-7834521',
     'VET-COL-2020-0117', 'VETERINARIAN', TRUE),

    -- Asistente veterinario (no puede realizar consultas)
    ('00000000-0003-0003-0003-000000000003',
     'Pedro', 'Ramírez Cruz',
     'p.ramirez@vetclinica.com', '+57-300-9823410',
     NULL, 'ASSISTANT', TRUE),

    -- Recepcionista
    ('00000000-0003-0003-0003-000000000004',
     'Laura', 'Vega Montoya',
     'l.vega@vetclinica.com', '+57-311-2340987',
     NULL, 'RECEPTIONIST', TRUE);


-- SECCIÓN 3: CLIENTES

INSERT INTO clients (id, first_name, last_name, email, phone, address) VALUES
    -- Dueño de Max (Labrador). Cliente frecuente.
    ('00000000-0004-0004-0004-000000000001',
     'Roberto', 'Gómez Herrera',
     'roberto.gomez@gmail.com', '+57-312-5670234',
     'Calle 85 # 14-32, Bogotá'),

    -- Dueña de Luna, Coco y Simba. Amante de los animales, 3 mascotas.
    ('00000000-0004-0004-0004-000000000002',
     'María', 'Torres Pinto',
     'maria.torres@outlook.com', '+57-318-4521076',
     'Carrera 11 # 93-44, Bogotá'),

    -- Co-dueño de Rocky (propietario principal). Empresario.
    ('00000000-0004-0004-0004-000000000003',
     'Carlos', 'Herrera Valencia',
     'c.herrera@empresa.co', '+57-321-8907654',
     'Av. El Dorado # 68B-31, Bogotá'),

    -- Co-dueña de Rocky (esposa). Registrada como propietaria secundaria.
    ('00000000-0004-0004-0004-000000000004',
     'Sofía', 'Herrera Valencia',
     'sofia.herrera@gmail.com', '+57-321-1234567',
     'Av. El Dorado # 68B-31, Bogotá'),

    -- Ex-dueño de Milo (Golden, fallecido). Historial preservado.
    ('00000000-0004-0004-0004-000000000005',
     'Diana', 'Morales Acosta',
     'diana.morales@gmail.com', '+57-317-6543210',
     'Calle 116 # 52-18, Bogotá'),

    -- Dueño de Pelusa (conejo). Paciente con NO_SHOW.
    ('00000000-0004-0004-0004-000000000006',
     'Andrés', 'Peña Castellanos',
     'andres.pena@hotmail.com', '+57-300-1122334',
     'Cra 7 # 45-20, Bogotá');


-- SECCIÓN 4: PACIENTES

INSERT INTO patients (id, name, species_id, breed_id, birth_date, sex, weight_kg,
                      coat_color, microchip_number, is_sterilized) VALUES

    -- S1: Max — Labrador Retriever, 4 años, macho, activo
    ('00000000-0005-0005-0005-000000000001',
     'Max',
     '00000000-0001-0001-0001-000000000001',   -- Perro
     '00000000-0002-0002-0002-000000000001',   -- Labrador Retriever
     '2022-01-15', 'M', 32.50,
     'Amarillo dorado', '941000023456789', FALSE),

    -- S2: Luna — Gata Persa, 3 años, hembra, esterilizada
    ('00000000-0005-0005-0005-000000000002',
     'Luna',
     '00000000-0001-0001-0001-000000000002',   -- Gato
     '00000000-0002-0002-0002-000000000006',   -- Persa
     '2023-04-20', 'F', 4.20,
     'Blanca con manchas grises', '941000034567890', TRUE),

    -- S3: Rocky — Bulldog Francés, 2 años, macho, con dos dueños
    ('00000000-0005-0005-0005-000000000003',
     'Rocky',
     '00000000-0001-0001-0001-000000000001',   -- Perro
     '00000000-0002-0002-0002-000000000003',   -- Bulldog Francés
     '2024-03-10', 'M', 11.80,
     'Atigrado negro y blanco', '941000045678901', FALSE),

    -- S4 / cliente Torres: Simba — Maine Coon, 1 año, macho
    ('00000000-0005-0005-0005-000000000004',
     'Simba',
     '00000000-0001-0001-0001-000000000002',   -- Gato
     '00000000-0002-0002-0002-000000000008',   -- Maine Coon
     '2025-02-08', 'M', 5.10,
     'Marrón tabby', NULL, FALSE),

    -- S6: Pelusa — Conejo, sin raza registrada (mestizo), hembra
    ('00000000-0005-0005-0005-000000000005',
     'Pelusa',
     '00000000-0001-0001-0001-000000000004',   -- Conejo
     NULL,                                      -- Sin raza (CE: BR-05 N/A)
     '2024-08-01', 'F', 1.85,
     'Blanca', NULL, FALSE),

    -- Coco — Canario de María Torres, sin raza
    ('00000000-0005-0005-0005-000000000006',
     'Coco',
     '00000000-0001-0001-0001-000000000003',   -- Ave
     NULL,
     '2023-11-12', 'M', 0.02,
     'Amarillo', NULL, FALSE),

    -- S7: Milo — Golden Retriever, fallecido el 2025-11-30 (soft delete)
    ('00000000-0005-0005-0005-000000000007',
     'Milo',
     '00000000-0001-0001-0001-000000000001',   -- Perro
     '00000000-0002-0002-0002-000000000002',   -- Golden Retriever
     '2019-06-22', 'M', 28.30,
     'Dorado oscuro', '941000056789012', TRUE);

-- Soft delete de Milo: fallecido por insuficiencia renal crónica
UPDATE patients
SET deleted_at = '2025-11-30 14:20:00+00'
WHERE id = '00000000-0005-0005-0005-000000000007';


-- SECCIÓN 5: CO-PROPIEDAD DE PACIENTES

INSERT INTO client_patients (id, client_id, patient_id, is_primary_owner) VALUES
    -- Roberto Gómez → Max (propietario único)
    ('00000000-0006-0006-0006-000000000001',
     '00000000-0004-0004-0004-000000000001',
     '00000000-0005-0005-0005-000000000001', TRUE),

    -- María Torres → Luna, Simba, Coco (propietaria única de los tres)
    ('00000000-0006-0006-0006-000000000002',
     '00000000-0004-0004-0004-000000000002',
     '00000000-0005-0005-0005-000000000002', TRUE),

    ('00000000-0006-0006-0006-000000000003',
     '00000000-0004-0004-0004-000000000002',
     '00000000-0005-0005-0005-000000000004', TRUE),

    ('00000000-0006-0006-0006-000000000004',
     '00000000-0004-0004-0004-000000000002',
     '00000000-0005-0005-0005-000000000006', TRUE),

    -- Rocky: Carlos Herrera (PRIMARY) + Sofía Herrera (co-dueña)
    -- BR-01: exactamente uno con is_primary_owner = TRUE por paciente
    ('00000000-0006-0006-0006-000000000005',
     '00000000-0004-0004-0004-000000000003',
     '00000000-0005-0005-0005-000000000003', TRUE),   -- Carlos: titular

    ('00000000-0006-0006-0006-000000000006',
     '00000000-0004-0004-0004-000000000004',
     '00000000-0005-0005-0005-000000000003', FALSE),  -- Sofía: co-dueña

    -- Andrés Peña → Pelusa
    ('00000000-0006-0006-0006-000000000007',
     '00000000-0004-0004-0004-000000000006',
     '00000000-0005-0005-0005-000000000005', TRUE),

    -- Diana Morales → Milo (fallecido; historial preservado)
    ('00000000-0006-0006-0006-000000000008',
     '00000000-0004-0004-0004-000000000005',
     '00000000-0005-0005-0005-000000000007', TRUE);


-- SECCIÓN 6: PRODUCTOS (Medicamentos, Vacunas, Insumos, Servicios)

INSERT INTO products (id, name, type, description, sku, stock_quantity,
                      unit_price, cost_price, min_stock_alert,
                      requires_prescription, is_active) VALUES

    -- ── MEDICAMENTOS ────────────────────────────────────────────────────────
    ('00000000-0007-0007-0007-000000000001',
     'Amoxicilina 500 mg × 10 comprimidos',
     'MEDICATION',
     'Antibiótico de amplio espectro. Indicado en infecciones bacterianas de piel, oídos y tracto respiratorio en pequeños animales.',
     'MED-AMOX-500-10', 48, 18.00, 8.50, 10, TRUE, TRUE),

    ('00000000-0007-0007-0007-000000000002',
     'Ivermectina 1% Solución Inyectable 10 ml',
     'MEDICATION',
     'Antiparasitario de amplio espectro. Eficaz contra nemátodos, ácaros y ectoparásitos. No usar en Collies ni razas MDR1-mutantes.',
     'MED-IVER-1-10ML', 30, 12.00, 5.00, 5, TRUE, TRUE),

    ('00000000-0007-0007-0007-000000000003',
     'Prednisolona 5 mg × 20 comprimidos',
     'MEDICATION',
     'Corticosteroide antiinflamatorio. Primera línea en dermatitis alérgica, prurito severo y enfermedades autoinmunes.',
     'MED-PRED-5-20', 55, 22.00, 10.00, 10, TRUE, TRUE),

    ('00000000-0007-0007-0007-000000000004',
     'Metronidazol 250 mg × 14 comprimidos',
     'MEDICATION',
     'Antibiótico y antiprotozoario. Indicado en giardiasis, infecciones anaerobias y enfermedad inflamatoria intestinal.',
     'MED-METR-250-14', 62, 15.00, 6.00, 10, TRUE, TRUE),

    ('00000000-0007-0007-0007-000000000005',
     'Omeprazol 20 mg × 30 cápsulas',
     'MEDICATION',
     'Inhibidor de la bomba de protones. Protector gástrico en úlceras, gastritis y uso crónico de AINEs o corticosteroides.',
     'MED-OMEP-20-30', 3, 19.00, 8.00, 8, FALSE, TRUE),
    -- stock_quantity = 3: por debajo del min_stock_alert (8) → escenario de alerta de bajo stock

    -- ── VACUNAS ─────────────────────────────────────────────────────────────
    ('00000000-0007-0007-0007-000000000006',
     'Nobivac DHPPi — Parvovirus / Moquillo / Hepatitis / Parainfluenza',
     'VACCINE',
     'Vacuna polivalente canina. Protección contra Distemper, Hepatitis, Parvovirus y Parainfluenza. Esquema anual.',
     'VAC-NOBIVAC-DHPPI', 22, 35.00, 16.00, 5, FALSE, TRUE),

    ('00000000-0007-0007-0007-000000000007',
     'Defensor 3 — Antirrábica 1 dosis',
     'VACCINE',
     'Vacuna antirrábica inactivada para perros y gatos. De aplicación anual. Obligatoria por normativa sanitaria.',
     'VAC-DEFENSOR-3-1D', 18, 28.00, 12.00, 5, FALSE, TRUE),

    ('00000000-0007-0007-0007-000000000008',
     'Leucofeligen — Leucemia Felina + Rinotraqueítis / Calicivirus',
     'VACCINE',
     'Vacuna combinada felina contra leucemia (FeLV) y complejo respiratorio. Protocolo anual en gatos con acceso al exterior.',
     'VAC-LEUCOFELIGEN-1D', 14, 42.00, 19.00, 5, FALSE, TRUE),

    ('00000000-0007-0007-0007-000000000009',
     'Feligen CRP — Triple Felina (Rinotraqueítis, Calicivirus, Panleucopenia)',
     'VACCINE',
     'Vacuna core felina. Protección esencial para todo gato independientemente de su estilo de vida. Esquema anual.',
     'VAC-FELIGEN-CRP-1D', 19, 38.00, 17.00, 5, FALSE, TRUE),

    -- ── INSUMOS ─────────────────────────────────────────────────────────────
    ('00000000-0007-0007-0007-000000000010',
     'Jeringas Desechables 5 ml × 10 unidades',
     'SUPPLY',
     'Jeringas estériles de un solo uso con aguja 23G. Uso general para aplicación de inyectables y toma de muestras.',
     'SUP-JERINGAS-5ML-10', 200, 4.50, 1.80, 30, FALSE, TRUE),

    ('00000000-0007-0007-0007-000000000011',
     'Vendas Elásticas Cohesivas 5 cm × 4 m',
     'SUPPLY',
     'Venda autoadherente para vendajes postoperatorios, esguinces y protección de accesos vasculares.',
     'SUP-VENDA-COH-5CM', 120, 3.20, 1.00, 20, FALSE, TRUE),

    -- ── SERVICIOS (sin stock) ────────────────────────────────────────────────
    ('00000000-0007-0007-0007-000000000012',
     'Consulta General',
     'SERVICE',
     'Consulta clínica estándar: anamnesis, examen físico completo, orientación diagnóstica y plan terapéutico.',
     'SVC-CONSULTA-GEN', NULL, 45.00, NULL, NULL, FALSE, TRUE),

    ('00000000-0007-0007-0007-000000000013',
     'Consulta de Urgencias',
     'SERVICE',
     'Atención prioritaria fuera de horario habitual o en casos críticos que requieren intervención inmediata.',
     'SVC-CONSULTA-URG', NULL, 75.00, NULL, NULL, FALSE, TRUE),

    ('00000000-0007-0007-0007-000000000014',
     'Cirugía Menor Ambulatoria',
     'SERVICE',
     'Procedimientos quirúrgicos de baja complejidad: sutura de heridas, extracción de masas superficiales, drenaje de abscesos.',
     'SVC-CIRUGIA-MENOR', NULL, 120.00, NULL, NULL, FALSE, TRUE),

    ('00000000-0007-0007-0007-000000000015',
     'Baño Medicado y Estética',
     'SERVICE',
     'Baño terapéutico con champú antimicótico o antiséptico según indicación veterinaria, más corte de uñas y limpieza de oídos.',
     'SVC-BANIO-MEDICADO', NULL, 25.00, NULL, NULL, FALSE, TRUE);


-- SECCIÓN 7: CITAS
-- Estado actual del sistema a fecha 2026-03-22

INSERT INTO appointments (id, patient_id, staff_id, scheduled_at, status, reason, notes) VALUES

    -- S1: Max — COMPLETADA. Dueño reportó rascado de oídos desde hace 10 días.
    ('00000000-0008-0008-0008-000000000001',
     '00000000-0005-0005-0005-000000000001',  -- Max
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     '2026-03-08 10:00:00+00', 'COMPLETED',
     'Rascado intenso y secreción en oídos',
     'El dueño menciona mal olor. Primera vez que presenta este cuadro.'),

    -- S2: Luna — COMPLETADA. Estornudos y secreción nasal.
    ('00000000-0008-0008-0008-000000000002',
     '00000000-0005-0005-0005-000000000002',  -- Luna
     '00000000-0003-0003-0003-000000000002',  -- Dra. Jiménez (especialista felinos)
     '2026-02-20 09:00:00+00', 'COMPLETED',
     'Estornudos frecuentes y descarga nasal serosa',
     'Gata indoor, sin contacto con otros gatos según dueña.'),

    -- S3: Rocky — COMPLETADA. Dermatitis con lesiones en abdomen y axilas.
    ('00000000-0008-0008-0008-000000000003',
     '00000000-0005-0005-0005-000000000003',  -- Rocky
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     '2026-03-15 11:00:00+00', 'COMPLETED',
     'Lesiones en piel, rascado excesivo y pérdida de pelo',
     'Cuadro recurrente. Dueño reporta que el perro come alimento de pollo.'),

    -- S4: Simba — CONFIRMADA (futura). Chequeo anual al año de vida.
    ('00000000-0008-0008-0008-000000000004',
     '00000000-0005-0005-0005-000000000004',  -- Simba
     '00000000-0003-0003-0003-000000000002',  -- Dra. Jiménez
     '2026-03-28 09:00:00+00', 'CONFIRMED',
     'Control de crecimiento y vacunación al año',
     'Primera visita del año. Confirmar esquema vacunal pendiente.'),

    -- S5: Max — CANCELADA. El dueño canceló por viaje imprevisto (CE-05).
    ('00000000-0008-0008-0008-000000000005',
     '00000000-0005-0005-0005-000000000001',  -- Max
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     '2026-01-25 14:00:00+00', 'CANCELLED',
     'Seguimiento post-tratamiento de otitis',
     'Cliente llamó 2 horas antes para cancelar. Reagendó para marzo.'),

    -- S6: Pelusa — NO_SHOW. El dueño no se presentó y no avisó.
    ('00000000-0008-0008-0008-000000000006',
     '00000000-0005-0005-0005-000000000005',  -- Pelusa
     '00000000-0003-0003-0003-000000000002',  -- Dra. Jiménez
     '2026-03-01 10:00:00+00', 'NO_SHOW',
     'Primera consulta general y revisión de dieta',
     'Recepción llamó sin éxito a las 10:15. Se cobra penalidad según política clínica.'),

    -- S7: Milo — COMPLETADA (histórica, antes de su fallecimiento).
    ('00000000-0008-0008-0008-000000000007',
     '00000000-0005-0005-0005-000000000007',  -- Milo
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     '2025-10-12 10:00:00+00', 'COMPLETED',
     'Control de enfermedad renal crónica estadio III',
     'Paciente con diagnóstico previo de ERC. Viene a control mensual.');


-- SECCIÓN 8: CONSULTAS CLÍNICAS
-- Solo generadas desde citas COMPLETED

INSERT INTO consultations (id, appointment_id, staff_id, anamnesis, physical_exam,
                           treatment_plan, weight_kg, temperature_c) VALUES

    -- S1: Consulta de Max — Otitis externa bilateral
    ('00000000-0009-0009-0009-000000000001',
     '00000000-0008-0008-0008-000000000001',  -- Cita de Max (COMPLETED)
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     'Propietario refiere rascado de oídos bilateral desde hace 10 días. Noto mal olor al acercarse al oído derecho. Sin antecedentes de otitis previas. Dieta comercial seca. Vive en apartamento, sin acceso a zonas de tierra.',
     'Otoscopia: membrana timpánica íntegra bilateral. Exudado marrón oscuro abundante en oído derecho, moderado en izquierdo. Eritema y edema en canal auditivo externo derecho. T: 38.4°C. FC: 92 lpm. FR: 22 rpm. Linfonodos submandibulares sin alteraciones. Abdomen sin dolor a palpación.',
     'Limpieza ótica profunda con solución al 0.05% de clorhexidina. Amoxicilina 500mg VO cada 12h × 10 días. Control en 14 días. Instrucciones de limpieza ótica semanal para el propietario.',
     32.10, 38.40),

    -- S2: Consulta de Luna — Rinotraqueítis felina
    ('00000000-0009-0009-0009-000000000002',
     '00000000-0008-0008-0008-000000000002',  -- Cita de Luna (COMPLETED)
     '00000000-0003-0003-0003-000000000002',  -- Dra. Jiménez
     'Propietaria refiere estornudos frecuentes (>10 veces/día) y secreción nasal serosa desde hace 5 días. Apetito ligeramente disminuido. La gata vive sola en interiores. Vacunas al día según cartilla del año pasado. Sin cambios recientes en alimentación ni entorno.',
     'Mucosas rosadas y húmedas. Secreción nasal serosa bilateral moderada. Leve enrojecimiento conjuntival bilateral. Temperatura 39.1°C. FC: 168 lpm. Sin ruidos adventicos pulmonares. Linfonodos mandibulares levemente reactivos (+). Sin lesiones orales.',
     'Diagnóstico presuntivo de rinotraqueítis viral felina (FHV-1). Soporte nutricional (dieta húmeda para facilitar ingesta). Metronidazol 250mg cada 12h × 7 días para prevenir sobreinfección bacteriana. Nebulización salina 2 veces/día en casa. Vigilar apetito. Cita de control en 7 días.',
     4.15, 39.10),

    -- S3: Consulta de Rocky — Dermatitis alérgica + infección bacteriana secundaria
    ('00000000-0009-0009-0009-000000000003',
     '00000000-0008-0008-0008-000000000003',  -- Cita de Rocky (COMPLETED)
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     'Propietario refiere aparición progresiva de lesiones en piel abdominal e inguinal hace 3 semanas. Rascado intenso y lamido de zonas afectadas. Episodio similar hace 6 meses que se resolvió espontáneamente. Cambió de alimento a croquetas de pollo hace 2 meses. No ha recibido antiparasitarios en los últimos 4 meses.',
     'Pápulas eritematosas y costras en abdomen ventral, axilas e ingles. Alopecia focal en área inguinal derecha. Olor fétido característico de sobreinfección. Citología: cocos en racimos (Staphylococcus spp.). Sin pulgas visibles. T: 38.9°C. Linfonodos inguinales bilaterales reactivos (++). Resto del examen sin alteraciones significativas.',
     'Plan 1: Tratar infección bacteriana secundaria con Prednisolona 5mg VO SID × 14 días (dosis antiinflamatoria). Plan 2: Prueba de eliminación de pollo de la dieta × 8 semanas (alimento hipoalergénico con proteína única de venado o pescado). Plan 3: Baño medicado con clorhexidina al 2% cada 3 días. Control en 21 días. Solicitar panel de alergias si no mejora.',
     11.60, 38.90),

    -- S7: Consulta de Milo — ERC estadio III, control mensual
    ('00000000-0009-0009-0009-000000000004',
     '00000000-0008-0008-0008-000000000007',  -- Cita de Milo (COMPLETED, histórica)
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     'Propietaria reporta disminución de apetito en los últimos 15 días y un episodio de vómito la semana pasada. Sigue con dieta renal prescrita. Consumo de agua aumentado.',
     'Mucosas pálidas levemente. BCS 3/9. Pelo opaco y deshidratación leve (5%). Halitosis urémica presente. T: 38.1°C. FC: 78 lpm. Palpación renal: riñones pequeños e irregulares. Resto del examen sin cambios respecto a control anterior.',
     'Ajuste de fluidoterapia subcutánea domiciliaria a 150ml cada 48h. Omeprazol 20mg VO SID como gastroprotector. Recomendación de cambio a dieta húmeda renal. Control en 4 semanas con hemograma y perfil renal completo. Pronóstico reservado.',
     27.80, 38.10);


-- SECCIÓN 9: DIAGNÓSTICOS
-- Mínimo uno por consulta. BR-13: exactamente uno is_primary = TRUE.

INSERT INTO diagnoses (id, consultation_id, cie_code, description, severity, is_primary) VALUES

    -- Max: diagnóstico único (primario)
    ('00000000-0010-0010-0010-000000000001',
     '00000000-0009-0009-0009-000000000001',
     'H60.3',
     'Otitis externa bacteriana bilateral — compatible con Staphylococcus spp. y Malassezia',
     'MODERATE', TRUE),

    -- Luna: diagnóstico único (primario)
    ('00000000-0010-0010-0010-000000000002',
     '00000000-0009-0009-0009-000000000002',
     'J06.9',
     'Rinotraqueítis viral felina aguda (FHV-1 presuntivo) — infección respiratoria superior',
     'MILD', TRUE),

    -- Rocky: dos diagnósticos (dermatitis como primario + infección como secundario)
    -- El índice parcial único garantiza que solo uno sea primario (BR-13 / AUD-01)
    ('00000000-0010-0010-0010-000000000003',
     '00000000-0009-0009-0009-000000000003',
     'L23.9',
     'Dermatitis alérgica — probable alergia alimentaria a proteína de pollo',
     'MODERATE', TRUE),

    ('00000000-0010-0010-0010-000000000004',
     '00000000-0009-0009-0009-000000000003',
     'L08.9',
     'Pioderma superficial bacteriana secundaria — Staphylococcus spp. por citología',
     'MILD', FALSE),   -- diagnóstico secundario (is_primary = FALSE)

    -- Milo: diagnóstico único (primario)
    ('00000000-0010-0010-0010-000000000005',
     '00000000-0009-0009-0009-000000000004',
     'N18.3',
     'Enfermedad renal crónica estadio III — progresión con crisis anoréxica',
     'SEVERE', TRUE);


-- SECCIÓN 10: PRESCRIPCIONES
-- Solo desde productos con type en MEDICATION o VACCINE aplicable

INSERT INTO prescriptions (id, consultation_id, product_id, dosage, frequency,
                           duration_days, instructions) VALUES

    -- Max: Amoxicilina para otitis
    ('00000000-0011-0011-0011-000000000001',
     '00000000-0009-0009-0009-000000000001',
     '00000000-0007-0007-0007-000000000001',   -- Amoxicilina 500mg
     '15 mg/kg (aproximadamente 1 comprimido de 500mg para 32 kg)',
     'Cada 12 horas, preferiblemente con alimento',
     10,
     'Administrar el comprimido entero envuelto en un trozo pequeño de carne o queso. '
     'NO suspender el tratamiento aunque el perro mejore antes de los 10 días. '
     'Si aparecen vómitos o diarrea, consultar. Guardar en lugar fresco y seco.'),

    -- Luna: Metronidazol para sobreinfección
    ('00000000-0011-0011-0011-000000000002',
     '00000000-0009-0009-0009-000000000002',
     '00000000-0007-0007-0007-000000000004',   -- Metronidazol 250mg
     '10 mg/kg (1/2 comprimido de 250mg para 4 kg)',
     'Cada 12 horas, siempre con comida',
     7,
     'Triturar el comprimido y mezclar con comida húmeda. '
     'Puede causar leve somnolencia, es normal. '
     'Si la gata deja de comer por más de 24h o empeora, regresar a consulta de urgencias.'),

    -- Rocky: Prednisolona para dermatitis alérgica
    ('00000000-0011-0011-0011-000000000003',
     '00000000-0009-0009-0009-000000000003',
     '00000000-0007-0007-0007-000000000003',   -- Prednisolona 5mg
     '1 mg/kg/día (2 comprimidos de 5mg para 11.6 kg)',
     'Una vez al día por la mañana con el desayuno',
     14,
     'IMPORTANTE: No suspender bruscamente. Los días 8 al 14 dar solo 1 comprimido (media dosis). '
     'Puede aumentar sed y ganas de orinar, es esperado. '
     'Evitar contacto con otros perros enfermos durante el tratamiento. '
     'Cambiar alimento a proteína de venado o pez — sin pollo ni derivados.'),

    -- Milo: Omeprazol como gastroprotector
    ('00000000-0011-0011-0011-000000000004',
     '00000000-0009-0009-0009-000000000004',
     '00000000-0007-0007-0007-000000000005',   -- Omeprazol 20mg
     '0.7 mg/kg (1 cápsula de 20mg para 27.8 kg)',
     'Una vez al día, en ayunas, 30 minutos antes del desayuno',
     30,
     'Abrir la cápsula y mezclar el polvo con una pequeña cantidad de alimento húmedo. '
     'Continuar con dieta renal húmeda indicada. '
     'Mantener acceso constante a agua fresca y limpia.');


-- SECCIÓN 11: VACUNACIONES
-- BR-18: product_id debe ser type = VACCINE (validado por trigger)
-- BR-19: batch_number siempre obligatorio

INSERT INTO vaccinations (id, patient_id, product_id, staff_id,
                          administered_at, next_due_date, batch_number) VALUES

    -- Max: DA2PP (Nobivac), aplicada en su control anual de 2025
    ('00000000-0012-0012-0012-000000000001',
     '00000000-0005-0005-0005-000000000001',  -- Max
     '00000000-0007-0007-0007-000000000006',  -- Nobivac DHPPi
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     '2025-03-10', '2026-03-10', 'NB-DHPPi-2024-A0342'),

    -- Max: Antirrábica (Defensor 3)
    ('00000000-0012-0012-0012-000000000002',
     '00000000-0005-0005-0005-000000000001',  -- Max
     '00000000-0007-0007-0007-000000000007',  -- Defensor 3 (Rabia)
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     '2025-03-10', '2026-03-10', 'DEF3-2024-RB0891'),

    -- Luna: Triple Felina (Feligen CRP), protocolo anual
    ('00000000-0012-0012-0012-000000000003',
     '00000000-0005-0005-0005-000000000002',  -- Luna
     '00000000-0007-0007-0007-000000000009',  -- Feligen CRP
     '00000000-0003-0003-0003-000000000002',  -- Dra. Jiménez
     '2025-06-15', '2026-06-15', 'FEL-CRP-2025-0561'),

    -- Luna: Leucemia Felina (Leucofeligen), por acceso eventual a balcón
    ('00000000-0012-0012-0012-000000000004',
     '00000000-0005-0005-0005-000000000002',  -- Luna
     '00000000-0007-0007-0007-000000000008',  -- Leucofeligen
     '00000000-0003-0003-0003-000000000002',  -- Dra. Jiménez
     '2025-06-15', '2026-06-15', 'LEUCO-FEL-2025-0203'),

    -- Rocky: DA2PP, último refuerzo aplicado en enero
    ('00000000-0012-0012-0012-000000000005',
     '00000000-0005-0005-0005-000000000003',  -- Rocky
     '00000000-0007-0007-0007-000000000006',  -- Nobivac DHPPi
     '00000000-0003-0003-0003-000000000001',  -- Dr. Mendoza
     '2025-01-20', '2026-01-20', 'NB-DHPPi-2024-B0198');
    -- Nota: la próxima dosis de Rocky es 2026-01-20 → ya venció (2026-03-22 actual)
    -- Esto genera una alerta de revacunación vencida en el sistema


-- SECCIÓN 12: FACTURAS E ÍTEMS
-- BR-17: unit_price congelado al momento de facturar
-- AUD-04: subtotal es GENERATED ALWAYS AS — NO incluir en INSERT

-- ─── S1: Factura de Max — PAID ───────────────────────────────────────────────
-- Subtotal: 45.00 (consulta) + 18.00 (amoxicilina) = 63.00
-- IVA 19%:  63.00 × 0.19 = 11.97
-- Total:    74.97

INSERT INTO invoices (id, client_id, consultation_id, status,
                      subtotal, tax_rate, tax_amount, total,
                      payment_method, issued_at, paid_at) VALUES
    ('00000000-0013-0013-0013-000000000001',
     '00000000-0004-0004-0004-000000000001',  -- Roberto Gómez
     '00000000-0009-0009-0009-000000000001',  -- Consulta de Max
     'PAID',
     63.00, 0.1900, 11.97, 74.97,
     'CARD',
     '2026-03-08 11:45:00+00',
     '2026-03-08 11:47:00+00');

INSERT INTO invoice_items (id, invoice_id, product_id, description, quantity, unit_price) VALUES
    ('00000000-0014-0014-0014-000000000001',
     '00000000-0013-0013-0013-000000000001',
     '00000000-0007-0007-0007-000000000012',  -- Consulta General
     'Consulta General — Otitis externa bilateral', 1, 45.00),

    ('00000000-0014-0014-0014-000000000002',
     '00000000-0013-0013-0013-000000000001',
     '00000000-0007-0007-0007-000000000001',  -- Amoxicilina 500mg
     'Amoxicilina 500 mg × 10 comprimidos — Antibioticoterapia ótica', 1, 18.00);


-- ─── S2: Factura de Luna — PAID ──────────────────────────────────────────────
-- Subtotal: 45.00 + 15.00 = 60.00
-- IVA 19%:  60.00 × 0.19 = 11.40
-- Total:    71.40

INSERT INTO invoices (id, client_id, consultation_id, status,
                      subtotal, tax_rate, tax_amount, total,
                      payment_method, issued_at, paid_at) VALUES
    ('00000000-0013-0013-0013-000000000002',
     '00000000-0004-0004-0004-000000000002',  -- María Torres
     '00000000-0009-0009-0009-000000000002',  -- Consulta de Luna
     'PAID',
     60.00, 0.1900, 11.40, 71.40,
     'TRANSFER',
     '2026-02-20 10:30:00+00',
     '2026-02-20 10:35:00+00');

INSERT INTO invoice_items (id, invoice_id, product_id, description, quantity, unit_price) VALUES
    ('00000000-0014-0014-0014-000000000003',
     '00000000-0013-0013-0013-000000000002',
     '00000000-0007-0007-0007-000000000012',  -- Consulta General
     'Consulta General — Rinotraqueítis viral felina', 1, 45.00),

    ('00000000-0014-0014-0014-000000000004',
     '00000000-0013-0013-0013-000000000002',
     '00000000-0007-0007-0007-000000000004',  -- Metronidazol 250mg
     'Metronidazol 250 mg × 14 comprimidos — Profilaxis antibacteriana', 1, 15.00);


-- ─── S3: Factura de Rocky — DRAFT (pendiente de cobro) ───────────────────────
-- El titular de la factura es Carlos Herrera (propietario primario, BR-22)
-- ítems ya cargados; aún no se ha emitido la factura
-- Subtotal: 45.00 + 22.00 = 67.00 | IVA: 12.73 | Total: 79.73

INSERT INTO invoices (id, client_id, consultation_id, status,
                      subtotal, tax_rate, tax_amount, total,
                      payment_method, issued_at, paid_at) VALUES
    ('00000000-0013-0013-0013-000000000003',
     '00000000-0004-0004-0004-000000000003',  -- Carlos Herrera (titular, BR-22)
     '00000000-0009-0009-0009-000000000003',  -- Consulta de Rocky
     'DRAFT',
     67.00, 0.1900, 12.73, 79.73,
     NULL,       -- BR-21: sin método de pago en DRAFT (AUD-06 no aplica aquí)
     NULL,       -- AUD-07: issued_at NULL válido en DRAFT
     NULL);

INSERT INTO invoice_items (id, invoice_id, product_id, description, quantity, unit_price) VALUES
    ('00000000-0014-0014-0014-000000000005',
     '00000000-0013-0013-0013-000000000003',
     '00000000-0007-0007-0007-000000000012',  -- Consulta General
     'Consulta General — Dermatitis alérgica y pioderma superficial', 1, 45.00),

    ('00000000-0014-0014-0014-000000000006',
     '00000000-0013-0013-0013-000000000003',
     '00000000-0007-0007-0007-000000000003',  -- Prednisolona 5mg
     'Prednisolona 5 mg × 20 comprimidos — Tratamiento antialérgico', 1, 22.00);


-- ─── S7: Factura histórica de Milo — PAID ────────────────────────────────────
-- Subtotal: 45.00 + 19.00 = 64.00
-- IVA 19%:  64.00 × 0.19 = 12.16
-- Total:    76.16
-- El paciente está deleted_at pero la factura e historial se conservan (BR-04)

INSERT INTO invoices (id, client_id, consultation_id, status,
                      subtotal, tax_rate, tax_amount, total,
                      payment_method, issued_at, paid_at) VALUES
    ('00000000-0013-0013-0013-000000000004',
     '00000000-0004-0004-0004-000000000005',  -- Diana Morales
     '00000000-0009-0009-0009-000000000004',  -- Consulta histórica de Milo
     'PAID',
     64.00, 0.1900, 12.16, 76.16,
     'CASH',
     '2025-10-12 11:30:00+00',
     '2025-10-12 11:32:00+00');

INSERT INTO invoice_items (id, invoice_id, product_id, description, quantity, unit_price) VALUES
    ('00000000-0014-0014-0014-000000000007',
     '00000000-0013-0013-0013-000000000004',
     '00000000-0007-0007-0007-000000000012',  -- Consulta General
     'Consulta General — Control ERC estadio III', 1, 45.00),

    ('00000000-0014-0014-0014-000000000008',
     '00000000-0013-0013-0013-000000000004',
     '00000000-0007-0007-0007-000000000005',  -- Omeprazol 20mg
     'Omeprazol 20 mg × 30 cápsulas — Gastroprotección en ERC', 1, 19.00);


-- ─── S8: Venta directa — ISSUED sin consulta (BR-24) ─────────────────────────
-- Roberto Gómez compra Omeprazol para Max (mantenimiento entre consultas)
-- consultation_id = NULL: factura libre, no clínica
-- Subtotal: 2 × 19.00 = 38.00 | Sin IVA (producto exento) | Total: 38.00

INSERT INTO invoices (id, client_id, consultation_id, status,
                      subtotal, tax_rate, tax_amount, total,
                      payment_method, issued_at, paid_at) VALUES
    ('00000000-0013-0013-0013-000000000005',
     '00000000-0004-0004-0004-000000000001',  -- Roberto Gómez
     NULL,    -- BR-24: sin consulta asociada
     'ISSUED',
     38.00, 0.0000, 0.00, 38.00,
     NULL,    -- Emitida pero aún no pagada (el cliente retira y paga en caja)
     '2026-03-20 15:10:00+00',
     NULL);

INSERT INTO invoice_items (id, invoice_id, product_id, description, quantity, unit_price) VALUES
    ('00000000-0014-0014-0014-000000000009',
     '00000000-0013-0013-0013-000000000005',
     '00000000-0007-0007-0007-000000000005',  -- Omeprazol 20mg
     'Omeprazol 20 mg × 30 cápsulas — Venta directa (suministro mensual Max)', 2, 19.00);
