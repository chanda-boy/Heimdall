# Modelo Relacional de Heimdall (v1)

> Derivado de [03_modelo_dominio.md](03_modelo_dominio.md) y ajustado con las decisiones de [05_decisiones.md](05_decisiones.md) (D1–D16). Motor objetivo: **PostgreSQL 16**.

## Convenciones
- Prefijo de tabla por bounded context: `iam_`, `prj_`, `geo_`, `thr_`, `wea_`, `alr_`, `ntf_`, `rep_`.
- PK `UUID` en entidades de negocio; claves naturales en catálogos (`code`).
- Enums como `VARCHAR` + `CHECK` (igual que `iam_users.role` hoy).
- Auditoría: `created_at`, `updated_at` (`TIMESTAMPTZ`).
- **D8:** FKs físicas solo **dentro** de un contexto. Entre contextos = FK lógica (línea punteada en el diagrama, sin constraint).

---

## 1. Diagrama Entidad-Relación

```mermaid
erDiagram
    %% ===== IAM =====
    iam_users {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar phone_e164
        varchar platform_role "ADMIN | USER"
        varchar status
        timestamptz last_login_at
        timestamptz created_at
        timestamptz updated_at
    }
    iam_refresh_tokens {
        uuid id PK
        uuid user_id FK
        varchar token_hash UK
        timestamptz expires_at
        timestamptz revoked_at
        timestamptz created_at
    }
    iam_users ||--o{ iam_refresh_tokens : "tiene"

    %% ===== GEO (catálogo) =====
    geo_departments {
        varchar code PK "DIVIPOLA 2 dígitos"
        varchar country_code
        varchar name
    }
    geo_municipalities {
        varchar code PK "DIVIPOLA 5 dígitos"
        varchar department_code FK
        varchar name
    }
    geo_departments ||--|{ geo_municipalities : "contiene"

    %% ===== PROJECT =====
    prj_projects {
        uuid id PK
        uuid owner_id "-> iam_users"
        varchar code "opcional"
        varchar name
        varchar country_code
        varchar municipality_code "-> geo_municipalities"
        numeric latitude
        numeric longitude
        varchar coordinates_source "GEOCODED | MANUAL"
        varchar timezone
        time daily_run_time
        boolean schedule_enabled
        smallint notify_lead_days
        varchar notify_min_level
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }
    prj_contacts {
        uuid id PK
        uuid project_id FK
        uuid user_id "nullable -> iam_users"
        varchar full_name
        varchar sci_position
        smallint priority
        boolean active
        timestamptz created_at
    }
    prj_contact_channels {
        uuid id PK
        uuid contact_id FK
        varchar channel_type "WHATSAPP | SMS | EMAIL"
        varchar destination
        boolean verified
    }
    prj_contact_subscriptions {
        uuid contact_id PK,FK
        varchar hazard PK
        varchar min_level
    }
    prj_projects ||--o{ prj_contacts : "contactos"
    prj_contacts ||--|{ prj_contact_channels : "canales"
    prj_contacts ||--o{ prj_contact_subscriptions : "suscripciones"

    %% ===== THRESHOLD =====
    thr_profiles {
        uuid id PK
        uuid project_id "-> prj_projects"
        int version
        boolean active
        timestamptz effective_from
        uuid created_by
        timestamptz created_at
    }
    thr_rules {
        uuid id PK
        uuid profile_id FK
        varchar hazard
        varchar variable
        varchar alert_level
        varchar scope "DAILY | MONTHLY"
        varchar comparator "GTE | IN_SET"
        numeric threshold_value
        smallint_array code_set
    }
    thr_monthly_climatology {
        uuid project_id PK "-> prj_projects"
        smallint month PK "1..12"
        numeric expected_temperature_max
        numeric expected_precipitation_probability
        timestamptz updated_at
    }
    thr_profiles ||--|{ thr_rules : "compuesto por"

    %% ===== WEATHER =====
    wea_providers {
        varchar code PK
        varchar name
        boolean enabled
        smallint priority
    }
    wea_forecast_runs {
        uuid id PK
        varchar provider_code FK
        numeric latitude
        numeric longitude
        smallint horizon_days
        timestamptz fetched_at
    }
    wea_daily_forecasts {
        uuid forecast_run_id PK,FK
        date forecast_date PK
        numeric temperature_max
        numeric wind_speed_max
        numeric precipitation_probability
        numeric precipitation_sum
        numeric relative_humidity_mean
        smallint weather_code FK
    }
    wea_wmo_codes {
        smallint code PK
        varchar description_es
        boolean is_thunderstorm
        boolean is_precipitation
    }
    wea_providers ||--o{ wea_forecast_runs : "produce"
    wea_forecast_runs ||--|{ wea_daily_forecasts : "contiene"
    wea_wmo_codes ||--o{ wea_daily_forecasts : "clasifica"

    %% ===== ALERT / MONITORING =====
    alr_monitoring_runs {
        uuid id PK
        uuid project_id "-> prj_projects"
        date run_date
        varchar trigger_type "SCHEDULED | MANUAL"
        uuid forecast_run_id "-> wea_forecast_runs"
        uuid threshold_profile_id "-> thr_profiles"
        varchar status
        timestamptz started_at
        timestamptz finished_at
        text error_message
    }
    alr_alerts {
        uuid id PK
        uuid monitoring_run_id FK
        uuid project_id "-> prj_projects"
        uuid rule_id "-> thr_rules"
        varchar hazard
        varchar variable
        varchar alert_level
        varchar scope
        date target_date
        smallint lead_time_days
        numeric observed_value
        numeric threshold_value
        varchar status "OPEN | ACKNOWLEDGED | RESOLVED"
        uuid acknowledged_by "-> iam_users"
        timestamptz acknowledged_at
        uuid resolved_by "-> iam_users"
        timestamptz resolved_at
        text resolution_note
        timestamptz created_at
    }
    alr_protocol_directives {
        uuid id PK
        uuid project_id "NULL = global"
        varchar hazard
        varchar alert_level
        varchar scope
        text alert_type_text
        text call_chain_text
        text action_text
    }
    alr_monitoring_runs ||--o{ alr_alerts : "detecta"

    %% ===== NOTIFICATION =====
    ntf_templates {
        varchar code PK
        varchar channel_type
        varchar provider_template_name
        varchar language
        text body_pattern
    }
    ntf_dispatches {
        uuid id PK
        uuid alert_id "-> alr_alerts"
        uuid contact_id "-> prj_contacts"
        varchar channel_type
        varchar destination
        varchar template_code FK
        jsonb payload
        varchar status
        varchar provider_message_id
        smallint attempts
        text last_error
        timestamptz sent_at
        timestamptz created_at
    }
    ntf_templates ||--o{ ntf_dispatches : "usa"

    %% ===== REPORTING =====
    rep_reports {
        uuid id PK
        uuid project_id "-> prj_projects"
        uuid monitoring_run_id "-> alr_monitoring_runs"
        varchar format "XLSX | PDF"
        varchar storage_key
        bigint size_bytes
        timestamptz generated_at
    }

    %% ===== Referencias ENTRE contextos (FK lógicas, D8) =====
    iam_users ||..o{ prj_projects : "owner_id"
    iam_users |o..o{ prj_contacts : "user_id"
    geo_municipalities ||..o{ prj_projects : "municipality_code"
    prj_projects ||..o{ thr_profiles : "project_id"
    prj_projects ||..o{ thr_monthly_climatology : "project_id"
    prj_projects ||..o{ alr_monitoring_runs : "project_id"
    prj_projects |o..o{ alr_protocol_directives : "override"
    wea_forecast_runs ||..o{ alr_monitoring_runs : "forecast_run_id"
    thr_profiles ||..o{ alr_monitoring_runs : "threshold_profile_id"
    thr_rules ||..o{ alr_alerts : "rule_id"
    alr_alerts ||..o{ ntf_dispatches : "alert_id"
    prj_contacts ||..o{ ntf_dispatches : "contact_id"
    alr_monitoring_runs ||..o| rep_reports : "monitoring_run_id"
```

---

## 2. Restricciones clave (para el DDL futuro)

| Tabla | Restricción | Decisión |
|---|---|---|
| `iam_users` | `CHECK platform_role IN ('ADMIN','USER')` | D2 |
| `iam_users` | `CHECK status IN ('ACTIVE','SUSPENDED','PENDING_VERIFICATION')` | – |
| `prj_projects` | `UNIQUE (owner_id, code)` (code nullable) | D1, D4 |
| `prj_projects` | `CHECK latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180` | VO `Coordinates` |
| `prj_projects` | `CHECK notify_lead_days >= 0`, default `1`; `notify_min_level` default `'ORANGE'` | D14 |
| `prj_contacts` | `CHECK sci_position IN ('SCI_CHIEF','DEPUTY_SCI_CHIEF','BRIGADE_LEADER','PMU_LIAISON','OTHER')` | D3 |
| `prj_contact_channels` | `UNIQUE (contact_id, channel_type, destination)` | – |
| `prj_contact_subscriptions` | `CHECK min_level IN ('YELLOW','ORANGE','RED')` | D6 |
| `thr_profiles` | `UNIQUE (project_id, version)` + índice parcial único `(project_id) WHERE active` | D7 |
| `thr_rules` | `UNIQUE (profile_id, hazard, variable, scope, alert_level)` | – |
| `thr_rules` | `CHECK ((comparator='GTE' AND threshold_value IS NOT NULL) OR (comparator='IN_SET' AND variable='WEATHER_CODE' AND code_set IS NOT NULL))` | – |
| `thr_monthly_climatology` | `CHECK month BETWEEN 1 AND 12`; `expected_precipitation_probability BETWEEN 0 AND 100` | D9 |
| `wea_providers` | `UNIQUE (priority)` | D11 |
| `wea_forecast_runs` | índice `(provider_code, latitude, longitude, fetched_at DESC)` | D10 |
| `alr_monitoring_runs` | índice parcial único `(project_id, run_date) WHERE trigger_type='SCHEDULED'` | idempotencia |
| `alr_alerts` | `CHECK status IN ('OPEN','ACKNOWLEDGED','RESOLVED')`; índice `(project_id, status, target_date)` | D12 |
| `alr_alerts` | `CHECK (status <> 'ACKNOWLEDGED' OR acknowledged_at IS NOT NULL)`, `CHECK (status <> 'RESOLVED' OR resolved_at IS NOT NULL)` | D12 |
| `alr_protocol_directives` | `UNIQUE NULLS NOT DISTINCT (project_id, hazard, alert_level, scope)` | D13 |
| `ntf_dispatches` | `CHECK status IN ('PENDING','SENT','DELIVERED','FAILED')` | – |

**Invariantes que viven en el dominio (no en DDL):**
- Monotonía `YELLOW < ORANGE < RED` entre reglas `GTE` de la misma (perfil, amenaza, variable, scope).
- El usuario solo opera sobre proyectos donde `owner_id = userId` del JWT (salvo `ADMIN`).
- Transiciones de alerta válidas: `OPEN → ACKNOWLEDGED → RESOLVED` (o `OPEN → RESOLVED`).
- Selección de proveedor: el de menor `priority` habilitado; si falla, el siguiente (D11).
- Protocolo efectivo = override del proyecto si existe, si no el global (D13).

---

## 3. Qué reemplaza a cada artefacto de Sarada

| Sarada | Heimdall |
|---|---|
| `threshold.json` (proyecto + contactos + umbrales diarios) | `prj_projects` + `prj_contacts` + `prj_contact_channels` + `thr_profiles`/`thr_rules` (DAILY) |
| `monthlyThreshold.json` | `thr_monthly_climatology` + `thr_rules` (MONTHLY) |
| `listCode.json` | `wea_wmo_codes` |
| `dashboard.json` (hora, directorio, token) | `prj_projects.daily_run_time` + secreto de config (token) + `rep_reports.storage_key` |
| `recordThreshold.json` | `alr_alerts` + `ntf_dispatches` |
| Enum `AlertThreshold` + textos de `GenerateSectionSheet` | `alr_protocol_directives` (corrigiendo erratas) |
| `report-YYYY-MM-DD.xlsx` en disco | `rep_reports` + puerto de almacenamiento |
| `schtasks` / `crontab` | `prj_projects.daily_run_time` + `alr_monitoring_runs` |

---

## 4. Impacto sobre el código actual (informativo, sin cambios aplicados)
- `iam_users`: agregar `full_name`, `phone_e164`, `status`, timestamps; renombrar `role` → `platform_role` con valores `ADMIN/USER` (D2). El registro asignaría `USER` en vez de `OPERATOR`.
- Introducir **Flyway** antes de crear el segundo contexto (hoy: `schema.sql` + `ddl-auto=validate`).
- Datos semilla necesarios: `geo_departments`/`geo_municipalities` (DIVIPOLA), `wea_wmo_codes`, `wea_providers` (`OPEN_METEO` prioridad 1), `alr_protocol_directives` globales, `ntf_templates` (`alert_threshold`).
