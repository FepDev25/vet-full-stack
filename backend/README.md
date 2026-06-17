# Backend - Sistema Veterinario

Backend de la plataforma de gestion veterinaria, construido con Java 21 y Spring Boot 4. Este modulo expone una API REST versionada para gestionar pacientes, clientes, agenda, consultas, inventario, facturacion, autenticacion JWT y portal de cliente.

## Stack tecnico

- Java 21
- Spring Boot 4.0.4
- Spring Web + Spring Data JPA + Spring Security
- PostgreSQL 15+
- Flyway para migraciones
- Maven 3.9+
- Springdoc OpenAPI (Swagger UI)
- JUnit 5 + Mockito + Spring Boot Test + MockMvc

## Arquitectura y estructura

El proyecto sigue arquitectura por capas:

- `com.veterinaria.domain`
  - `entity/`: entidades JPA
  - `enums/`: enums de dominio
  - `repository/`: repositorios Spring Data
- `com.veterinaria.application`
  - `service/`: reglas de negocio y transacciones
  - `dto/`: contratos request/response/page
- `com.veterinaria.infrastructure.web`
  - `controller/`: endpoints HTTP
- `com.veterinaria.config`: configuraciones (security, openapi, etc.)
- `com.veterinaria.exception`: excepciones y manejador global
- `com.veterinaria.security`: JWT, filtros y propiedades de auth

Controladores implementados:

- `AuthController`
- `CatalogController`
- `ClientController`
- `PatientController`
- `StaffController`
- `AppointmentController`
- `ConsultationController`
- `VaccinationController`
- `InvoiceController`
- `PortalController`
- `DashboardController`

## Base de datos y migraciones

Flyway esta habilitado y es la unica fuente de verdad del schema en runtime (`ddl-auto: none`).

Migraciones principales (`src/main/resources/db/migration`):

- `V1__init_extensions.sql`
- `V2__create_schema.sql`
- `V4__auth.sql`

Seeds de desarrollo (`src/main/resources/db/seeds`):

- `V5__seeds.sql`

Seeds de test (`src/test/resources/db/migration`):

- `V5__test_seeds.sql`

## Perfiles

Configurados en `src/main/resources/application.yml` y `src/test/resources/application-test.yml`.

- `default`:
  - `flyway.locations=classpath:db/migration`
  - sin seeds de desarrollo
- `dev`:
  - `flyway.locations=classpath:db/migration,classpath:db/seeds`
  - SQL debug habilitado
- `prod`:
  - logs mas estrictos, sin seeds
- `test`:
  - datasource local de test (`veterinaria_test`)
  - flyway habilitado
  - migraciones desde classpath de test + main

## Variables de entorno

Datasource:

- `DB_HOST` (default: `localhost`)
- `DB_PORT` (default: `5435`)
- `DB_NAME` (default: `veterinaria`)
- `DB_USERNAME` (default: `vetuser`)
- `DB_PASSWORD` (default: `vetpass`)

JWT:

- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `JWT_REFRESH_EXPIRATION_MS`

CORS:

- `CORS_ORIGINS`

## Ejecucion local

Desde `backend/`:

```bash
# 1) Compilar
mvn clean compile

# 2) Ejecutar con perfil dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

La app levanta por defecto en:

- `http://localhost:8080`

## Documentacion API

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

Contrato de referencia del monorepo:

- `../openapi.yaml`

## Testing

Tipos de pruebas en este modulo:

- Unit tests de servicios (Mockito)
- Integration tests de servicios (`@SpringBootTest`)
- HTTP controller tests con MockMvc

Comandos utiles:

```bash
# suite completa
mvn clean test

# solo unit tests (patron)
mvn -Dtest='*ServiceTest' test

# solo tests HTTP de controllers
mvn -Dtest='*ControllerTest' test

# un test especifico
mvn -Dtest=AuthControllerTest test
```

Estado actual de la suite:

- 131 tests
- 0 failures
- 0 errors

## Seguridad

- Autenticacion con JWT (access + refresh)
- Filtro `JwtAuthenticationFilter`
- Endpoints publicos:
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/refresh`
  - `GET /api/v1/species/**`
  - Swagger/OpenAPI
- `portal/**` restringido a rol `CLIENT`

## Comandos utiles adicionales

```bash
# validar solo migraciones en arranque
mvn -DskipTests spring-boot:run

# empaquetar jar
mvn clean package

# ver dependencias
mvn dependency:tree
```

## Notas de trabajo

- No usar `ddl-auto=create/update`; el schema lo controla Flyway.
- Mantener IDs UUID y reglas de negocio en capa `service`.
- Si cambias el contrato, sincronizar con `../openapi.yaml` y frontend.
