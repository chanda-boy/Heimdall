# Registro de Decisiones de Dominio (ADR-lite)

Fecha: 2026-10-13 · Contexto: definición del modelo de dominio y relacional de Heimdall.

| # | Tema | Decisión | Consecuencia en el modelo |
|---|---|---|---|
| D1 | Tenencia | Cada proyecto pertenece a **un único usuario** (dueño), sin compartir por ahora. | `prj_projects.owner_id`; **no** hay tabla de miembros. Compartir se agregará después si se necesita. |
| D2 | Roles | Solo **rol global de plataforma**: `ADMIN`, `USER`. Sin roles por proyecto. | `iam_users.platform_role`. El enum actual `Role (ADMIN, SCI_CHIEF, OPERATOR)` deberá migrar a `ADMIN/USER`; "Jefe SCI" pasa a ser una **posición de contacto**, no un rol de login. |
| D3 | Contactos | Entidad independiente del usuario; vínculo opcional a un usuario. | `prj_contacts.user_id` nullable (FK lógica). |
| D4 | Código de proyecto | Opcional, único por dueño. | `UNIQUE (owner_id, code)` (permite varios NULL). |
| D5 | Geografía | Colombia primero con catálogo **DIVIPOLA**, con `country_code` para crecer. | Tablas `geo_departments`, `geo_municipalities`; `prj_projects.country_code` (por defecto `CO`). |
| D6 | Suscripciones | Cada contacto elige amenazas y nivel mínimo. | `prj_contact_subscriptions (contact_id, hazard, min_level)`. |
| D7 | Versionado de umbrales | Cada cambio crea una nueva versión del perfil. | `thr_profiles (project_id, version, active)`; las corridas guardan `threshold_profile_id`. |
| D8 | Integración entre contextos | Solo IDs lógicos entre contextos; FKs físicas solo dentro de cada contexto. | Columnas `*_id` sin constraint entre prefijos distintos. |
| D9 | Precipitación mensual | El "%" de Sarada es **probabilidad de precipitación**. | `thr_monthly_climatology.expected_precipitation_probability`; reglas MONTHLY usan `PRECIPITATION_PROBABILITY`. |
| D10 | Pronósticos | Redis como caché + persistir en Postgres los pronósticos usados en cada corrida. | `wea_forecast_runs`, `wea_daily_forecasts`; `alr_monitoring_runs.forecast_run_id`. |
| D11 | Multi-proveedor | Proveedor primario global con **fallback automático** por prioridad. | `wea_providers.priority`, `enabled`; sin preferencia por proyecto. |
| D12 | Alertas | Ciclo de vida `OPEN → ACKNOWLEDGED → RESOLVED`, registrando quién y cuándo. | Columnas `status`, `acknowledged_by/at`, `resolved_by/at`, `resolution_note` en `alr_alerts`. |
| D13 | Protocolos SCI | Catálogo global como plantilla, sobrescribible por proyecto. | `alr_protocol_directives.project_id` nullable (NULL = global). |
| D14 | Política de notificación | Configurable por proyecto; por defecto 1 día de antelación y nivel mínimo `ORANGE`. | `prj_projects.notify_lead_days`, `notify_min_level`. |
| D15 | Credenciales WhatsApp | Una cuenta de plataforma (secreto en configuración); por usuario queda como opción futura. | Nada en BD por ahora. |
| D16 | Reportes | Se almacenan vía puerto de almacenamiento: volumen Docker local ahora, S3/MinIO después. | `rep_reports.storage_key` (clave lógica, no ruta del SO). |

## Pendientes / fuera de alcance por ahora
- Compartir proyectos entre usuarios (reintroducir `prj_project_members`).
- Credenciales de notificación por usuario (cifradas).
- Refresh tokens (fase 2 de IAM; la tabla ya está prevista en el modelo).
- Herramienta de migraciones (**Flyway** recomendado antes de crear el segundo contexto).
