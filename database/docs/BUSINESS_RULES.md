# BUSINESS RULES — Sistema Veterinario Profesional
> @Architect | Versión 1.0 | 2026-03-22

---

## Dominio del Negocio

Una clínica veterinaria gestiona la salud de animales (pacientes) cuyos dueños (clientes) son los responsables legales y de pago. El personal (staff) atiende citas, realiza consultas clínicas, prescribe tratamientos y genera facturas. La clínica también mantiene un inventario de medicamentos, vacunas e insumos.

---

## Módulo 1: Clientes y Pacientes

### BR-01 — Co-propiedad de pacientes
Una mascota puede tener **múltiples dueños** (ej: pareja, familia). La tabla `client_patients` modela esta relación N:M. Sin embargo, **exactamente un** propietario debe ser marcado como `is_primary_owner = TRUE`. Este propietario es el contacto principal para notificaciones y el titular por defecto de la factura.

### BR-02 — Paciente sin dueño activo
Un paciente **no puede existir sin al menos un cliente** vinculado. Si el último propietario activo de un paciente es eliminado (soft delete), el sistema debe alertar o bloquear la operación.

### BR-03 — Microchip único
El número de microchip, cuando se registra, debe ser **único a nivel global** en el sistema. No pueden existir dos pacientes con el mismo microchip.

### BR-04 — Soft Delete en clientes y pacientes
Ni los clientes ni los pacientes se eliminan físicamente. Se usa `deleted_at` para marcarlos como inactivos. Un paciente con `deleted_at` no puede agendar nuevas citas, pero su historial médico e historial de facturas se conservan íntegros.

### BR-05 — Raza opcional
La raza (`breed_id`) es **opcional** (nullable), ya que puede ser una mezcla o desconocida. Si se registra, debe pertenecer a la especie del paciente.

---

## Módulo 2: Agenda y Citas

### BR-06 — Estados de cita
El ciclo de vida de una cita sigue esta máquina de estados:
```
PENDING → CONFIRMED → IN_PROGRESS → COMPLETED
                   ↘ CANCELLED
         → NO_SHOW
```
- Solo una cita en estado `COMPLETED` puede generar una consulta clínica.
- Una cita `CANCELLED` o `NO_SHOW` **no** genera consulta ni factura.

### BR-07 — Sin doble reserva
Un veterinario **no puede tener dos citas** en estado activo (`PENDING`, `CONFIRMED`, `IN_PROGRESS`) solapadas en el tiempo.

### BR-08 — Solo veterinarios atienden citas
Solo el personal con `role = 'VETERINARIAN'` puede ser asignado como responsable de una cita o consulta. Asistentes y recepcionistas no pueden ser el vet titular de una consulta.

### BR-09 — Citas pasadas
No se pueden crear citas con `scheduled_at` en el pasado (validación de aplicación).

---

## Módulo 3: Consultas Clínicas

### BR-10 — 1:1 entre cita y consulta
Una cita completada genera **exactamente una** consulta clínica. No pueden existir dos consultas para la misma cita.

### BR-11 — Inmutabilidad de consultas cerradas
Una consulta asociada a una factura en estado `PAID` no puede ser modificada. Representa un registro médico-legal cerrado.

### BR-12 — Diagnóstico obligatorio
Una consulta debe tener **al menos un diagnóstico** registrado en `diagnoses` antes de poder generar una factura.

### BR-13 — Diagnóstico primario único
De todos los diagnósticos de una consulta, exactamente **uno** debe marcarse como `is_primary = TRUE`.

---

## Módulo 4: Inventario y Productos

### BR-14 — Stock no negativo
El `stock_quantity` de un producto físico **nunca puede ser negativo**. Si al generar una prescripción o ítem de factura el stock es insuficiente, la operación debe bloquearse o alertarse.

### BR-15 — Productos de tipo SERVICE
Los productos de tipo `SERVICE` (ej: consulta, cirugía, baño) no tienen stock (`stock_quantity = NULL`). Solo los tipos `MEDICATION`, `VACCINE` y `SUPPLY` tienen control de inventario.

### BR-16 — Productos con receta
Los productos con `requires_prescription = TRUE` solo pueden dispensarse si existe una prescripción activa vinculada en `prescriptions`. No pueden venderse libremente como ítem de factura sin este requisito.

### BR-17 — Precio histórico en factura
Al agregar un ítem a una factura, el `unit_price` en `invoice_items` se copia del precio actual del producto **en ese momento**. Si el precio del producto cambia después, las facturas antiguas no se modifican.

---

## Módulo 5: Vacunaciones

### BR-18 — Solo vacunas
El producto vinculado en `vaccinations.product_id` debe tener `type = 'VACCINE'`.

### BR-19 — Número de lote obligatorio
Por trazabilidad legal, el `batch_number` es **obligatorio** en cada registro de vacunación.

### BR-20 — Fecha de próxima dosis
La `next_due_date` es calculada por el sistema o ingresada por el veterinario según el protocolo de la vacuna. Es la base para el módulo de recordatorios/alertas.

---

## Módulo 6: Facturación

### BR-21 — Estados de factura
```
DRAFT → ISSUED → PAID
              ↘ CANCELLED
PAID → REFUNDED
```
- Una factura en `PAID` o `REFUNDED` no puede cancelarse directamente; requiere un proceso de reembolso.

### BR-22 — Titular de factura
La factura siempre se emite al **propietario primario** del paciente por defecto, pero puede reasignarse a otro co-propietario.

### BR-23 — Total calculado
`total = subtotal + tax_amount`. El `subtotal` es la suma de todos los `invoice_items.subtotal`. Estos valores deben calcularse y validarse antes de emitir la factura.

### BR-24 — Factura sin consulta
Es posible crear facturas sin una consulta asociada para servicios no clínicos (ej: venta de alimento, baño y estética). En este caso, `consultation_id = NULL`.

### BR-25 — Eliminación de ítems
Los ítems de una factura en `DRAFT` pueden eliminarse o modificarse. Una vez en `ISSUED` o superior, los ítems son **inmutables**.

---

## Módulo 7: Personal

### BR-26 — Número de licencia
El campo `license_number` es obligatorio solo para el rol `VETERINARIAN` y debe ser único. Para otros roles puede ser NULL.

### BR-27 — Desactivación en lugar de eliminación
El personal no se elimina del sistema. Se marca `is_active = FALSE` para preservar la integridad del historial médico y de facturación.

---

## Casos Borde Identificados

| # | Escenario | Decisión |
|---|-----------|----------|
| CE-01 | Producto eliminado pero aparece en receta/factura | Los productos nunca se eliminan físicamente; se usan `is_active = FALSE`. Las relaciones históricas se conservan. |
| CE-02 | Veterinario que deja la clínica | Se desactiva (`is_active = FALSE`). Sus consultas y citas históricas permanecen intactas. |
| CE-03 | Paciente fallece | Se registra `deleted_at` en patients con una nota. El historial médico y vacunal se preserva. |
| CE-04 | Cambio de propietario | Se elimina el `client_patients` antiguo y se crea uno nuevo. El historial del paciente permanece. |
| CE-05 | Cita reagendada | Se cancela la cita original (status = CANCELLED) y se crea una nueva. No se modifica el `scheduled_at` de la original. |
| CE-06 | Descuento o precio especial | Se maneja directamente en `invoice_items.unit_price` al momento de facturar; no hay tabla de descuentos en V1. |
