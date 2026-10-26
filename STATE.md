# 🧭 ESTADO DEL PROYECTO: HEIMDALL (STATE.md)

> **Documento de Memoria Compartida y Seguimiento de Estado**  
> **Última actualización:** 22 de Octubre de 2026  
> **Proyecto:** Heimdall — Sistema de Alerta Temprana frente a Amenazas Hidrometeorológicas (Reescritura de Sarada)  
> **Stack:** Java 25 · Spring Boot 4.1.1 · PostgreSQL 16 · Redis 7 · Flyway · ArchUnit · Gradle (Kotlin DSL)

---

## 1. Resumen Ejecutivo y Visión

Heimdall es la reescritura técnica y arquitectónica de la plataforma **Sarada**. Su propósito es monitorear variables meteorológicas y emitir alertas tempranas multicanal (email, SMS, WhatsApp) para proyectos de infraestructura y comunidades vulnerables en Colombia.

### Principios Rectores de Arquitectura
1. **Clean Architecture & DDD Estricto:** Cada contexto delimitado (`bounded context`) tiene su propia separación en capas (`domain`, `application`, `infrastructure`, `presentation`).
2. **Pureza del Dominio:** La capa `domain` es 100% agnóstica de frameworks (cero imports de Spring, Jakarta Persistence, etc.). Se valida de forma automatizada mediante pruebas de arquitectura con ArchUnit (`DomainArchitectureTest`).
3. **Desacoplamiento entre Contextos (Decisión D8):** 
   - Las relaciones **dentro** del mismo contexto usan Foreign Keys (FKs) físicas en base de datos.
   - Las referencias **entre** contextos son estrictamente lógicas (UUIDs o códigos DIVIPOLA) sin restricciones de FK en la base de datos, garantizando autonomía e independencia de evolución.

---

## 2. Mapa de Contextos Delimitados (Bounded Contexts)

| Prefijo | Contexto | Estado | Migración DB | Responsabilidad Principal |
| :--- | :--- | :---: | :---: | :--- |
| **`iam`** | Identity & Access | ✅ Completado | `V1__iam_base.sql` | Autenticación JWT sin estado, refresh tokens, BCrypt, roles de sistema (`ADMIN`, `USER`). |
| **`geo`** | Geographic Catalogs | ✅ Completado | `V2__geo_catalogs.sql` | Catálogo territorial oficial DIVIPOLA (Departamentos y Municipios de Colombia). |
| **`prj`** | Project Management | ✅ Completado | `V3__prj_projects_and_contacts.sql` | Proyectos de monitoreo, coordenadas, políticas de notificación y contactos operacionales (`SCI_CHIEF`, etc.). |
| **`thr`** | Thresholds & Rules | ⏳ **PENDIENTE (FASE 3)** | `V4` (planeada) | Perfiles de umbrales versionados, reglas por amenaza (precipitación, inundación, deslizamiento) y climatología mensual. |
| **`wea`** | Weather Ingestion | ⏳ Pendiente (Fase 4) | `V5` (planeada) | Ingesta de datos de pronósticos (Open-Meteo, IDEAM), persistencia de series temporales. |
| **`alr`** | Alert Evaluation | ⏳ Pendiente (Fase 5) | `V6` (planeada) | Motor de evaluación de reglas frente a pronósticos para emitir alertas operativas. |
| **`ntf`** | Notifications | ⏳ Pendiente (Fase 6) | `V7` (planeada) | Despacho multicanal con colas, rate limiting e idempotencia. |
| **`rep`** | Reports & Bulletins | ⏳ Pendiente (Fase 7) | `V8` (planeada) | Generación y exportación de boletines hidrometeorológicos periódicos. |

---

## 3. Estado Detallado de lo Construido

### ✅ Contexto IAM (`com.coronel.heimdall.iam`)
- **Dominio:** Agregados `User`, `RefreshToken`, Value Objects `EmailAddress`, `RawPassword`, `HashedPassword`, `Role` (`ADMIN`, `USER`).
- **Aplicación:** `AuthenticationUseCases` (registro, login, refresco, logout/revocación).
- **Infraestructura:** Adaptadores JPA (`PostgresUserRepositoryAdapter`, `PostgresRefreshTokenRepositoryAdapter`), Spring Security Filter Chain, `JwtTokenServiceAdapter` (HMAC-SHA256).
- **Base de Datos:** Tablas `iam_users`, `iam_refresh_tokens`.

### ✅ Contexto GEO (`com.coronel.heimdall.geo`)
- **Dominio:** Entidades `Department`, `Municipality`, Value Objects `DepartmentCode` (2 dígitos), `MunicipalityCode` (5 dígitos).
- **Aplicación:** `GeoCatalogQueryService` (búsqueda y listado de departamentos y municipios).
- **Infraestructura:** `PostgresGeoRepositoryAdapter` con soporte de caché.
- **Base de Datos:** Tablas `geo_departments`, `geo_municipalities` con semilla completa de DIVIPOLA.

### ✅ Contexto PRJ (`com.coronel.heimdall.project`)
- **Dominio:** Agregados `Project` y `Contact`.
  - VOs de Proyecto: `ProjectId`, `Coordinates`, `CoordinatesSource`, `MonitoringSchedule`, `NotificationPolicy`, `ProjectStatus`.
  - VOs de Contacto: `ContactId`, `SciPosition` (`SCI_CHIEF`, `OPERATIONS_COORDINATOR`, etc.), `ContactChannel` (E.164, email), `ContactSubscription` (amenaza y severidad mínima).
- **Aplicación:** `ProjectManagementService`, `ContactManagementService` (validación de aislamiento multi-tenant por usuario).
- **Infraestructura:** Adaptadores JPA y `GeoValidationAdapter` (validación de existencia de código municipal contra `geo` sin FK física en BD).
- **Base de Datos:** Tablas `prj_projects`, `prj_contacts`, `prj_contact_channels`, `prj_contact_subscriptions`.

### 🛡️ Pruebas y Calidad
- **Total de pruebas automatizadas:** 76 pruebas unitarias, de integración y de arquitectura (100% pasando).
- **ArchUnit (`DomainArchitectureTest`):** Garantiza que ninguna clase en `*.domain..*` importe paquetes de `org.springframework..*`.

---

## 4. Guía de Ejecución y Entorno Local

### Opción A: Desarrollo rápido con base de datos en memoria (H2 - Perfil `dev`)
No requiere Docker ni servicios externos levantados:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```
*En IntelliJ IDEA:*
- `Run/Debug Configurations` -> `Application` -> `HeimdallApplication`.
- En **Active profiles**, colocar: `dev`.
- O en **VM options**, agregar: `-Dspring.profiles.active=dev`.

### Opción B: Ejecución con contenedores (PostgreSQL 16 + Redis 7)
```bash
# Iniciar servicios con Docker o Podman
docker compose up -d
# O con podman
podman-compose up -d

# Ejecutar aplicación apuntando a Postgres (perfil por defecto)
./gradlew bootRun
```

### Ejecución de Pruebas
```bash
# Ejecutar todas las pruebas unitarias y de arquitectura
./gradlew test

# Verificación completa (incluyendo checkstyle si aplica)
./gradlew check
```

---

## 5. Dónde quedamos y Próximos Pasos Inmediatos

### 📍 Dónde quedamos:
- Concluida la **Fase 2** (Contextos `geo` y `project`, migraciones Flyway V1 a V3, y soporte de perfil `dev` sin dependencias externas).
- Verificado el arranque tanto en CLI como documentada la configuración para IntelliJ IDEA.

### 🎯 Siguiente Paso Inmediato: **FASE 3 — Bounded Context `threshold` (THR)**
El siguiente bloque a construir es la gestión de perfiles de umbrales y reglas hidrometeorológicas:
1. **Modelado del Dominio `threshold`:**
   - Agregado `ThresholdProfile` (perfil por defecto o específico por proyecto).
   - Reglas de umbrales (`ThresholdRule`): Tipo de amenaza (`PRECIPITATION`, `FLASH_FLOOD`, `LANDSLIDE`), métrica (`ACCUMULATED_1H`, `ACCUMULATED_24H`), operador (`GREATER_THAN`, etc.), valor umbral y severidad resultante (`GREEN`, `YELLOW`, `ORANGE`, `RED`).
   - Climatología mensual (`MonthlyClimatology`): Valores esperados de lluvia mensual para ajustar percentiles de riesgo.
2. **Migración Flyway `V4__thr_threshold_profiles_and_rules.sql`:**
   - Tablas: `thr_profiles`, `thr_rules`, `thr_monthly_climatologies`.
3. **Casos de Uso en Aplicación:**
   - Creación, versionado y activación de perfiles de umbrales.
   - Consulta de umbrales aplicables a un proyecto.
4. **Infraestructura y API REST:**
   - Repositorios JPA y adaptadores de persistencia.
   - Endpoints `/api/v1/projects/{projectId}/thresholds` y `/api/v1/threshold-profiles`.
5. **Pruebas de Dominio y Arquitectura:**
   - Tests unitarios de evaluación de reglas y verificación con ArchUnit.
