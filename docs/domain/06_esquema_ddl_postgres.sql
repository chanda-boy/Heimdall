-- ============================================================================
-- PROYECTO HEIMDALL: SISTEMA DE ALERTA TEMPRANA Y MONITOREO HIDROMETEOROLÓGICO
-- ESQUEMA RELACIONAL DDL COMPLETO (PostgreSQL 16)
-- Archivo: docs/domain/06_esquema_ddl_postgres.sql
-- ============================================================================
--
-- ARQUITECTURA Y DECISIONES DE DOMINIO APLICADAS:
-- ----------------------------------------------------------------------------
-- 1. Bounded Contexts (Prefijos de tabla):
--    - iam_ : Identity & Access Management (usuarios, credenciales, tokens)
--    - geo_ : Catálogo geográfico territorial (DIVIPOLA Colombia)
--    - prj_ : Gestión de proyectos, contactos operativos SCI y canales
--    - thr_ : Perfiles de umbrales versionados, reglas y climatología mensual
--    - wea_ : Clima multi-proveedor, códigos WMO y series de pronóstico
--    - alr_ : Motor de monitoreo, alertas detectadas y directivas SCI
--    - ntf_ : Plantillas y despachos de notificación (WhatsApp, etc.)
--    - rep_ : Registro de reportes analíticos generados (Excel, PDF)
--
-- 2. Decisión D8 - Integración entre Bounded Contexts (FKs Físicas vs Lógicas):
--    - Claves foráneas físicas (FOREIGN KEY ... REFERENCES ...):
--      ÚNICAMENTE se aplican DENTRO del mismo Bounded Context para garantizar
--      la integridad referencial y de cascada de agregados y entidades hijas
--      (ej. prj_contact_channels -> prj_contacts, geo_municipalities -> geo_departments,
--      thr_rules -> thr_profiles, wea_daily_forecasts -> wea_forecast_runs,
--      alr_alerts -> alr_monitoring_runs).
--    - Referencias entre distintos contextos (Cross-Context):
--      Se implementan como CLAVES FORÁNEAS LÓGICAS. Se almacena el identificador
--      (UUID o código de catálogo) SIN constraint físico de clave foránea.
--      Esto preserva la autonomía de los agregados de DDD, previene acoplamientos
--      rígidos a nivel de base de datos, facilita pruebas aisladas y sienta las bases
--      para evolucionar hacia microservicios o esquemas/bases de datos independientes.
--      Cada referencia lógica se encuentra explícitamente documentada con comentarios SQL.
--
-- 3. Decisiones de Tenencia, Identidad y Seguridad (D1, D2, D3, D4):
--    - D1: Modelo multi-inquilino donde cada proyecto pertenece a un único dueño
--      (prj_projects.owner_id -> iam_users.id lógico). Sin tabla de miembros en v1.
--    - D2: Rol de plataforma global (iam_users.platform_role IN ('ADMIN', 'USER')).
--      Los roles operativos de terreno (Jefe SCI, Auxiliar) pertenecen a prj_contacts.
--    - D3: Contactos operativos modelados como entidad independiente del usuario de login.
--      Pueden vincularse opcionalmente a un usuario del sistema (prj_contacts.user_id).
--    - D4: Código opcional de proyecto único por usuario dueño (UNIQUE(owner_id, code)).
--
-- 4. Geografía y Localización (D5):
--    - Catálogo estándar DIVIPOLA (departamentos 2 dígitos, municipios 5 dígitos).
--    - Prj_projects soporta coordenadas geocodificadas o manuales y zona horaria.
--
-- 5. Umbrales, Reglas y Climatología (D6, D7, D9):
--    - D6: Suscripciones granulares por contacto eligiendo amenaza y nivel mínimo.
--    - D7: Perfiles de umbral inmutables y versionados (thr_profiles + thr_rules).
--      Índice único parcial garantiza exactamente un perfil activo por proyecto.
--    - D9: Línea base mensual con temperatura máxima y probabilidad de lluvia (0..100%).
--
-- 6. Ingesta Meteorológica Multi-Proveedor (D10, D11):
--    - D10: Persistencia de corridas de pronóstico y días individuales para auditoría.
--    - D11: Catálogo multi-proveedor con prioridad única para fallback automático.
--
-- 7. Alertas, Protocolos SCI y Notificaciones (D12, D13, D14, D15, D16):
--    - D12: Ciclo de vida estricto OPEN -> ACKNOWLEDGED -> RESOLVED con auditoría.
--    - D13: Protocolos de respuesta SCI jerárquicos: plantilla global (project_id IS NULL)
--      con posibilidad de sobrescritura por proyecto (UNIQUE NULLS NOT DISTINCT).
--    - D14: Política de notificación por proyecto (antelación configurable y nivel mínimo).
--    - D16: Almacenamiento desacoplado de reportes mediante clave lógica (storage_key).
-- ============================================================================

-- Configuración de sesión recomendada
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;


-- ============================================================================
-- 1. BOUNDED CONTEXT: IAM (Identity & Access Management)
-- ============================================================================

-- Tabla: iam_users
-- Usuarios autenticables en la plataforma Heimdall.
CREATE TABLE IF NOT EXISTS iam_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    phone_e164 VARCHAR(30),
    platform_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_iam_users_email UNIQUE (email),
    CONSTRAINT ck_iam_users_platform_role CHECK (platform_role IN ('ADMIN', 'USER')),
    CONSTRAINT ck_iam_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'))
);

COMMENT ON TABLE iam_users IS 'Usuarios autenticables con rol de plataforma (ADMIN/USER) según decisión D2.';
COMMENT ON COLUMN iam_users.email IS 'Correo electrónico normalizado en minúsculas (RFC 5322). Clave de autenticación.';
COMMENT ON COLUMN iam_users.password_hash IS 'Hash criptográfico de contraseña (BCrypt / Argon2).';
COMMENT ON COLUMN iam_users.platform_role IS 'Rol administrativo global. No confundir con posiciones operativas del SCI.';

-- Tabla: iam_refresh_tokens
-- Tokens de refresco para renovación de sesiones JWT (Fase 2 de IAM).
CREATE TABLE IF NOT EXISTS iam_refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK física intra-contexto: iam_refresh_tokens -> iam_users
    CONSTRAINT fk_iam_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES iam_users (id) ON DELETE CASCADE,
    CONSTRAINT uk_iam_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_iam_refresh_tokens_user_id ON iam_refresh_tokens (user_id);

COMMENT ON TABLE iam_refresh_tokens IS 'Tokens de refresco revocables asociados a sesiones de usuario.';


-- ============================================================================
-- 2. BOUNDED CONTEXT: GEO (Catálogo Geográfico Territorial)
-- ============================================================================

-- Tabla: geo_departments
-- Departamentos territoriales normalizados según codificación oficial DIVIPOLA (DANE).
CREATE TABLE IF NOT EXISTS geo_departments (
    code VARCHAR(2) PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL DEFAULT 'CO',
    name VARCHAR(100) NOT NULL,

    CONSTRAINT ck_geo_departments_code CHECK (length(code) = 2)
);

COMMENT ON TABLE geo_departments IS 'Catálogo de departamentos según DIVIPOLA (DANE Colombia) con soporte internacional.';
COMMENT ON COLUMN geo_departments.code IS 'Código DIVIPOLA de 2 dígitos (ej. 05 para Antioquia).';

-- Tabla: geo_municipalities
-- Municipios de Colombia vinculados a su departamento respectivo.
CREATE TABLE IF NOT EXISTS geo_municipalities (
    code VARCHAR(5) PRIMARY KEY,
    department_code VARCHAR(2) NOT NULL,
    name VARCHAR(100) NOT NULL,

    -- FK física intra-contexto: geo_municipalities -> geo_departments
    CONSTRAINT fk_geo_municipalities_department FOREIGN KEY (department_code)
        REFERENCES geo_departments (code) ON DELETE RESTRICT,
    CONSTRAINT ck_geo_municipalities_code CHECK (length(code) = 5)
);

CREATE INDEX IF NOT EXISTS idx_geo_municipalities_department ON geo_municipalities (department_code);

COMMENT ON TABLE geo_municipalities IS 'Catálogo oficial de municipios DIVIPOLA de 5 dígitos.';
COMMENT ON COLUMN geo_municipalities.code IS 'Código municipal oficial de 5 dígitos (ej. 05113 para Buriticá).';


-- ============================================================================
-- 3. BOUNDED CONTEXT: PRJ (Project Management & Contactos Operativos)
-- ============================================================================

-- Tabla: prj_projects
-- Proyectos monitoreados por el Sistema de Alerta Temprana.
CREATE TABLE IF NOT EXISTS prj_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,               -- FK Lógica -> iam_users(id) [D1, D8]
    code VARCHAR(50),                     -- Código nemotécnico opcional del cliente [D4]
    name VARCHAR(150) NOT NULL,
    country_code VARCHAR(2) NOT NULL DEFAULT 'CO',
    municipality_code VARCHAR(5),         -- FK Lógica -> geo_municipalities(code) [D5, D8]
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    coordinates_source VARCHAR(20) NOT NULL DEFAULT 'GEOCODED',
    timezone VARCHAR(50) NOT NULL DEFAULT 'America/Bogota',
    daily_run_time TIME NOT NULL DEFAULT '06:00:00',
    schedule_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notify_lead_days SMALLINT NOT NULL DEFAULT 1,
    notify_min_level VARCHAR(20) NOT NULL DEFAULT 'ORANGE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_prj_projects_owner_code UNIQUE (owner_id, code),
    CONSTRAINT ck_prj_projects_coordinates CHECK (
        latitude BETWEEN -90.000000 AND 90.000000 AND
        longitude BETWEEN -180.000000 AND 180.000000
    ),
    CONSTRAINT ck_prj_projects_coordinates_source CHECK (coordinates_source IN ('GEOCODED', 'MANUAL')),
    CONSTRAINT ck_prj_projects_notify_lead_days CHECK (notify_lead_days >= 0),
    CONSTRAINT ck_prj_projects_notify_min_level CHECK (notify_min_level IN ('YELLOW', 'ORANGE', 'RED')),
    CONSTRAINT ck_prj_projects_status CHECK (status IN ('ACTIVE', 'PAUSED', 'ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_prj_projects_owner_id ON prj_projects (owner_id);
CREATE INDEX IF NOT EXISTS idx_prj_projects_schedule ON prj_projects (schedule_enabled, daily_run_time);

COMMENT ON TABLE prj_projects IS 'Proyectos operacionales monitoreados por el SAT. Cada proyecto pertenece a un dueño (D1).';
COMMENT ON COLUMN prj_projects.owner_id IS 'FK LÓGICA -> iam_users(id). Usuario propietario del proyecto (D1, D8).';
COMMENT ON COLUMN prj_projects.municipality_code IS 'FK LÓGICA -> geo_municipalities(code). Ubicación territorial DIVIPOLA (D5, D8).';
COMMENT ON COLUMN prj_projects.notify_lead_days IS 'Días de anticipación mínima para despacho de alertas por mensajería (D14, default 1).';
COMMENT ON COLUMN prj_projects.notify_min_level IS 'Severidad mínima para despachar notificaciones (D14, default ORANGE).';

-- Tabla: prj_contacts
-- Contactos operativos y roles del Sistema de Comando de Incidentes (SCI).
CREATE TABLE IF NOT EXISTS prj_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    user_id UUID,                         -- FK Lógica -> iam_users(id) [D3, D8, nullable]
    full_name VARCHAR(150) NOT NULL,
    sci_position VARCHAR(30) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK física intra-contexto: prj_contacts -> prj_projects
    CONSTRAINT fk_prj_contacts_project FOREIGN KEY (project_id)
        REFERENCES prj_projects (id) ON DELETE CASCADE,
    CONSTRAINT ck_prj_contacts_sci_position CHECK (
        sci_position IN ('SCI_CHIEF', 'DEPUTY_SCI_CHIEF', 'BRIGADE_LEADER', 'PMU_LIAISON', 'OTHER')
    ),
    CONSTRAINT ck_prj_contacts_priority CHECK (priority > 0)
);

CREATE INDEX IF NOT EXISTS idx_prj_contacts_project_active ON prj_contacts (project_id, active);
CREATE INDEX IF NOT EXISTS idx_prj_contacts_user_id ON prj_contacts (user_id);

COMMENT ON TABLE prj_contacts IS 'Integrantes del SCI y cadena de notificación de emergencias del proyecto (D3).';
COMMENT ON COLUMN prj_contacts.user_id IS 'FK LÓGICA -> iam_users(id). Opcional si el contacto posee una cuenta de acceso (D3, D8).';
COMMENT ON COLUMN prj_contacts.sci_position IS 'Posición formal en el Sistema de Comando de Incidentes (D3).';

-- Tabla: prj_contact_channels
-- Canales de comunicación asociados a un contacto (WhatsApp, SMS, Email).
CREATE TABLE IF NOT EXISTS prj_contact_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_id UUID NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,

    -- FK física intra-contexto: prj_contact_channels -> prj_contacts
    CONSTRAINT fk_prj_contact_channels_contact FOREIGN KEY (contact_id)
        REFERENCES prj_contacts (id) ON DELETE CASCADE,
    CONSTRAINT uk_prj_contact_channels UNIQUE (contact_id, channel_type, destination),
    CONSTRAINT ck_prj_contact_channels_channel_type CHECK (channel_type IN ('WHATSAPP', 'SMS', 'EMAIL'))
);

CREATE INDEX IF NOT EXISTS idx_prj_contact_channels_contact_id ON prj_contact_channels (contact_id);

COMMENT ON TABLE prj_contact_channels IS 'Destinos de notificación verificables de un contacto (número E.164 o correo).';

-- Tabla: prj_contact_subscriptions
-- Suscripciones específicas de cada contacto a amenazas y niveles de severidad.
CREATE TABLE IF NOT EXISTS prj_contact_subscriptions (
    contact_id UUID NOT NULL,
    hazard VARCHAR(30) NOT NULL,
    min_level VARCHAR(20) NOT NULL,

    -- FK física intra-contexto: prj_contact_subscriptions -> prj_contacts
    CONSTRAINT pk_prj_contact_subscriptions PRIMARY KEY (contact_id, hazard),
    CONSTRAINT fk_prj_contact_subscriptions_contact FOREIGN KEY (contact_id)
        REFERENCES prj_contacts (id) ON DELETE CASCADE,
    CONSTRAINT ck_prj_contact_subscriptions_hazard CHECK (
        hazard IN ('FOREST_FIRE', 'MASS_MOVEMENT', 'WINDSTORM', 'LIGHTNING')
    ),
    CONSTRAINT ck_prj_contact_subscriptions_min_level CHECK (
        min_level IN ('YELLOW', 'ORANGE', 'RED')
    )
);

COMMENT ON TABLE prj_contact_subscriptions IS 'Filtro de suscripción de alertas por amenaza y nivel mínimo para cada contacto (D6).';


-- ============================================================================
-- 4. BOUNDED CONTEXT: THR (Thresholds, Reglas y Climatología)
-- ============================================================================

-- Tabla: thr_profiles
-- Perfiles inmutables y versionados de configuración de umbrales por proyecto.
CREATE TABLE IF NOT EXISTS thr_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,             -- FK Lógica -> prj_projects(id) [D8]
    version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,                      -- FK Lógica -> iam_users(id) [D8]
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_thr_profiles_project_version UNIQUE (project_id, version),
    CONSTRAINT ck_thr_profiles_version CHECK (version > 0)
);

-- D7: Índice único parcial para garantizar exactamente un único perfil activo por proyecto.
CREATE UNIQUE INDEX IF NOT EXISTS uq_thr_profiles_active ON thr_profiles (project_id) WHERE active;
CREATE INDEX IF NOT EXISTS idx_thr_profiles_project_id ON thr_profiles (project_id);

COMMENT ON TABLE thr_profiles IS 'Versiones auditables e inmutables del perfil de umbrales de un proyecto (D7).';
COMMENT ON COLUMN thr_profiles.project_id IS 'FK LÓGICA -> prj_projects(id). Proyecto al que pertenece el perfil (D8).';
COMMENT ON COLUMN thr_profiles.created_by IS 'FK LÓGICA -> iam_users(id). Usuario autor de esta versión de umbrales (D8).';

-- Tabla: thr_rules
-- Reglas cuantitativas y de conjuntos que conforman un perfil de umbral.
CREATE TABLE IF NOT EXISTS thr_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    hazard VARCHAR(30) NOT NULL,
    variable VARCHAR(40) NOT NULL,
    alert_level VARCHAR(20) NOT NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    comparator VARCHAR(20) NOT NULL DEFAULT 'GTE',
    threshold_value NUMERIC(10, 2),
    code_set SMALLINT[],

    -- FK física intra-contexto: thr_rules -> thr_profiles
    CONSTRAINT fk_thr_rules_profile FOREIGN KEY (profile_id)
        REFERENCES thr_profiles (id) ON DELETE CASCADE,
    CONSTRAINT uk_thr_rules_profile_hazard_var UNIQUE (profile_id, hazard, variable, scope, alert_level),
    CONSTRAINT ck_thr_rules_hazard CHECK (hazard IN ('FOREST_FIRE', 'MASS_MOVEMENT', 'WINDSTORM', 'LIGHTNING')),
    CONSTRAINT ck_thr_rules_variable CHECK (
        variable IN ('TEMPERATURE_MAX', 'PRECIPITATION_SUM', 'PRECIPITATION_PROBABILITY',
                     'WIND_SPEED_MAX', 'RELATIVE_HUMIDITY_MEAN', 'WEATHER_CODE')
    ),
    CONSTRAINT ck_thr_rules_alert_level CHECK (alert_level IN ('YELLOW', 'ORANGE', 'RED')),
    CONSTRAINT ck_thr_rules_scope CHECK (scope IN ('DAILY', 'MONTHLY')),
    CONSTRAINT ck_thr_rules_comparator CHECK (comparator IN ('GTE', 'IN_SET')),
    CONSTRAINT ck_thr_rules_comparator_value CHECK (
        (comparator = 'GTE' AND threshold_value IS NOT NULL) OR
        (comparator = 'IN_SET' AND variable = 'WEATHER_CODE' AND code_set IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_thr_rules_profile_id ON thr_rules (profile_id);

COMMENT ON TABLE thr_rules IS 'Reglas de evaluación matemática y por conjunto asociadas a un perfil de umbral.';
COMMENT ON COLUMN thr_rules.code_set IS 'Conjunto de códigos meteorológicos WMO (ej. {95,96,99} para tormentas ceráunicas).';

-- Tabla: thr_monthly_climatology
-- Línea base histórica y valores esperados mensuales por proyecto (12 meses).
CREATE TABLE IF NOT EXISTS thr_monthly_climatology (
    project_id UUID NOT NULL,             -- FK Lógica -> prj_projects(id) [D8]
    month SMALLINT NOT NULL,
    expected_temperature_max NUMERIC(5, 2),
    expected_precipitation_probability NUMERIC(5, 2),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_thr_monthly_climatology PRIMARY KEY (project_id, month),
    CONSTRAINT ck_thr_monthly_climatology_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT ck_thr_monthly_climatology_precip_prob CHECK (
        expected_precipitation_probability IS NULL OR
        expected_precipitation_probability BETWEEN 0.00 AND 100.00
    ),
    CONSTRAINT ck_thr_monthly_climatology_temp_max CHECK (
        expected_temperature_max IS NULL OR
        expected_temperature_max BETWEEN -50.00 AND 60.00
    )
);

COMMENT ON TABLE thr_monthly_climatology IS 'Climatología mensual de referencia (línea base) para evaluar anomalías estacionales (D9).';
COMMENT ON COLUMN thr_monthly_climatology.project_id IS 'FK LÓGICA -> prj_projects(id) (D8).';
COMMENT ON COLUMN thr_monthly_climatology.expected_precipitation_probability IS 'Porcentaje de probabilidad histórica de lluvia esperada en el mes (D9).';


-- ============================================================================
-- 5. BOUNDED CONTEXT: WEA (Weather Ingestion & Catálogos Meteorológicos)
-- ============================================================================

-- Tabla: wea_providers
-- Proveedores de pronóstico meteorológico disponibles y estrategia de fallback.
CREATE TABLE IF NOT EXISTS wea_providers (
    code VARCHAR(30) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority SMALLINT NOT NULL,

    CONSTRAINT uk_wea_providers_priority UNIQUE (priority),
    CONSTRAINT ck_wea_providers_priority CHECK (priority > 0)
);

COMMENT ON TABLE wea_providers IS 'Catálogo de proveedores meteorológicos con prioridad para fallback automático (D11).';

-- Tabla: wea_wmo_codes
-- Catálogo oficial de códigos meteorológicos de la Organización Meteorológica Mundial (WMO).
CREATE TABLE IF NOT EXISTS wea_wmo_codes (
    code SMALLINT PRIMARY KEY,
    description_es VARCHAR(150) NOT NULL,
    is_thunderstorm BOOLEAN NOT NULL DEFAULT FALSE,
    is_precipitation BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE wea_wmo_codes IS 'Catálogo de condiciones climáticas según la tabla oficial WMO (0 a 99).';
COMMENT ON COLUMN wea_wmo_codes.is_thunderstorm IS 'Indica si el fenómeno califica como tormenta eléctrica para alertas ceráunicas.';

-- Tabla: wea_forecast_runs
-- Ejecuciones de ingesta de pronósticos meteorológicos desde proveedores externos.
CREATE TABLE IF NOT EXISTS wea_forecast_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_code VARCHAR(30) NOT NULL,
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    horizon_days SMALLINT NOT NULL DEFAULT 14,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK física intra-contexto: wea_forecast_runs -> wea_providers
    CONSTRAINT fk_wea_forecast_runs_provider FOREIGN KEY (provider_code)
        REFERENCES wea_providers (code) ON DELETE RESTRICT,
    CONSTRAINT ck_wea_forecast_runs_coordinates CHECK (
        latitude BETWEEN -90.000000 AND 90.000000 AND
        longitude BETWEEN -180.000000 AND 180.000000
    ),
    CONSTRAINT ck_wea_forecast_runs_horizon CHECK (horizon_days > 0)
);

-- D10: Índice de optimización para reusar y auditar consultas recientes por coordenadas
CREATE INDEX IF NOT EXISTS idx_wea_forecast_runs_lookup
    ON wea_forecast_runs (provider_code, latitude, longitude, fetched_at DESC);

COMMENT ON TABLE wea_forecast_runs IS 'Instantáneas de peticiones meteorológicas realizadas para un punto geográfico (D10).';

-- Tabla: wea_daily_forecasts
-- Serie de días individuales pronosticados en una corrida (horizonte de 14 días).
CREATE TABLE IF NOT EXISTS wea_daily_forecasts (
    forecast_run_id UUID NOT NULL,
    forecast_date DATE NOT NULL,
    temperature_max NUMERIC(5, 2),
    wind_speed_max NUMERIC(5, 2),
    precipitation_probability NUMERIC(5, 2),
    precipitation_sum NUMERIC(6, 2),
    relative_humidity_mean NUMERIC(5, 2),
    weather_code SMALLINT,

    -- FKs físicas intra-contexto
    CONSTRAINT pk_wea_daily_forecasts PRIMARY KEY (forecast_run_id, forecast_date),
    CONSTRAINT fk_wea_daily_forecasts_run FOREIGN KEY (forecast_run_id)
        REFERENCES wea_forecast_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_wea_daily_forecasts_wmo FOREIGN KEY (weather_code)
        REFERENCES wea_wmo_codes (code) ON DELETE RESTRICT,
    CONSTRAINT ck_wea_daily_forecasts_precip_prob CHECK (
        precipitation_probability IS NULL OR precipitation_probability BETWEEN 0.00 AND 100.00
    ),
    CONSTRAINT ck_wea_daily_forecasts_humidity CHECK (
        relative_humidity_mean IS NULL OR relative_humidity_mean BETWEEN 0.00 AND 100.00
    ),
    CONSTRAINT ck_wea_daily_forecasts_precip_sum CHECK (
        precipitation_sum IS NULL OR precipitation_sum >= 0.00
    ),
    CONSTRAINT ck_wea_daily_forecasts_wind_speed CHECK (
        wind_speed_max IS NULL OR wind_speed_max >= 0.00
    )
);

CREATE INDEX IF NOT EXISTS idx_wea_daily_forecasts_date ON wea_daily_forecasts (forecast_date);

COMMENT ON TABLE wea_daily_forecasts IS 'Datos diarios específicos del pronóstico atmosférico a 14 días.';


-- ============================================================================
-- 6. BOUNDED CONTEXT: ALR (Alerting, Monitoreo y Protocolos SCI)
-- ============================================================================

-- Tabla: alr_monitoring_runs
-- Ejecución del motor de monitoreo para un proyecto en una fecha particular.
CREATE TABLE IF NOT EXISTS alr_monitoring_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,             -- FK Lógica -> prj_projects(id) [D8]
    run_date DATE NOT NULL,
    trigger_type VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    forecast_run_id UUID,                 -- FK Lógica -> wea_forecast_runs(id) [D8, D10]
    threshold_profile_id UUID,            -- FK Lógica -> thr_profiles(id) [D7, D8]
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ,
    error_message TEXT,

    CONSTRAINT ck_alr_monitoring_runs_trigger_type CHECK (trigger_type IN ('SCHEDULED', 'MANUAL')),
    CONSTRAINT ck_alr_monitoring_runs_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

-- Idempotencia: solo puede existir una corrida programada (SCHEDULED) por proyecto y fecha.
CREATE UNIQUE INDEX IF NOT EXISTS uq_alr_monitoring_runs_scheduled
    ON alr_monitoring_runs (project_id, run_date) WHERE trigger_type = 'SCHEDULED';

CREATE INDEX IF NOT EXISTS idx_alr_monitoring_runs_project_date
    ON alr_monitoring_runs (project_id, run_date DESC);
CREATE INDEX IF NOT EXISTS idx_alr_monitoring_runs_status
    ON alr_monitoring_runs (status);

COMMENT ON TABLE alr_monitoring_runs IS 'Registro de corridas diarias de evaluación de umbrales e idempotencia (D10).';
COMMENT ON COLUMN alr_monitoring_runs.project_id IS 'FK LÓGICA -> prj_projects(id) (D8).';
COMMENT ON COLUMN alr_monitoring_runs.forecast_run_id IS 'FK LÓGICA -> wea_forecast_runs(id). Corrida meteorológica insumo (D8, D10).';
COMMENT ON COLUMN alr_monitoring_runs.threshold_profile_id IS 'FK LÓGICA -> thr_profiles(id). Perfil de umbral contrastado (D7, D8).';

-- Tabla: alr_alerts
-- Alertas tempranas detectadas por superación de umbrales y su ciclo de vida.
CREATE TABLE IF NOT EXISTS alr_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monitoring_run_id UUID NOT NULL,
    project_id UUID NOT NULL,             -- FK Lógica -> prj_projects(id) [D8]
    rule_id UUID,                         -- FK Lógica -> thr_rules(id) [D8]
    hazard VARCHAR(30) NOT NULL,
    variable VARCHAR(40) NOT NULL,
    alert_level VARCHAR(20) NOT NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    target_date DATE NOT NULL,
    lead_time_days SMALLINT NOT NULL,
    observed_value NUMERIC(10, 2),
    threshold_value NUMERIC(10, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    acknowledged_by UUID,                 -- FK Lógica -> iam_users(id) [D8, D12]
    acknowledged_at TIMESTAMPTZ,
    resolved_by UUID,                     -- FK Lógica -> iam_users(id) [D8, D12]
    resolved_at TIMESTAMPTZ,
    resolution_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK física intra-contexto: alr_alerts -> alr_monitoring_runs
    CONSTRAINT fk_alr_alerts_run FOREIGN KEY (monitoring_run_id)
        REFERENCES alr_monitoring_runs (id) ON DELETE CASCADE,
    CONSTRAINT ck_alr_alerts_hazard CHECK (hazard IN ('FOREST_FIRE', 'MASS_MOVEMENT', 'WINDSTORM', 'LIGHTNING')),
    CONSTRAINT ck_alr_alerts_variable CHECK (
        variable IN ('TEMPERATURE_MAX', 'PRECIPITATION_SUM', 'PRECIPITATION_PROBABILITY',
                     'WIND_SPEED_MAX', 'RELATIVE_HUMIDITY_MEAN', 'WEATHER_CODE')
    ),
    CONSTRAINT ck_alr_alerts_alert_level CHECK (alert_level IN ('YELLOW', 'ORANGE', 'RED')),
    CONSTRAINT ck_alr_alerts_scope CHECK (scope IN ('DAILY', 'MONTHLY')),
    CONSTRAINT ck_alr_alerts_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    -- D12: Integridad del ciclo de vida de acuse y resolución
    CONSTRAINT ck_alr_alerts_ack_lifecycle CHECK (status <> 'ACKNOWLEDGED' OR acknowledged_at IS NOT NULL),
    CONSTRAINT ck_alr_alerts_res_lifecycle CHECK (status <> 'RESOLVED' OR resolved_at IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_alr_alerts_project_status_target
    ON alr_alerts (project_id, status, target_date);
CREATE INDEX IF NOT EXISTS idx_alr_alerts_monitoring_run_id
    ON alr_alerts (monitoring_run_id);
CREATE INDEX IF NOT EXISTS idx_alr_alerts_target_date
    ON alr_alerts (target_date);

COMMENT ON TABLE alr_alerts IS 'Alertas de superación de umbral generadas con ciclo de vida OPEN/ACKNOWLEDGED/RESOLVED (D12).';
COMMENT ON COLUMN alr_alerts.project_id IS 'FK LÓGICA -> prj_projects(id) (D8).';
COMMENT ON COLUMN alr_alerts.rule_id IS 'FK LÓGICA -> thr_rules(id). Regla de umbral detonante (D8).';
COMMENT ON COLUMN alr_alerts.acknowledged_by IS 'FK LÓGICA -> iam_users(id). Usuario que acusó recibo (D8, D12).';
COMMENT ON COLUMN alr_alerts.resolved_by IS 'FK LÓGICA -> iam_users(id). Usuario que cerró la alerta (D8, D12).';

-- Tabla: alr_protocol_directives
-- Directivas institucionales y protocolos de mando del SCI (globales o personalizadas por proyecto).
CREATE TABLE IF NOT EXISTS alr_protocol_directives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID,                      -- FK Lógica -> prj_projects(id) [D8, D13: NULL = plantilla global]
    hazard VARCHAR(30) NOT NULL,
    alert_level VARCHAR(20) NOT NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    alert_type_text TEXT NOT NULL,
    call_chain_text TEXT NOT NULL,
    action_text TEXT NOT NULL,

    -- D13: PostgreSQL 15+ UNIQUE NULLS NOT DISTINCT garantiza unicidad estricta para globales (project_id IS NULL)
    -- y unicidad por cada proyecto con override específico.
    CONSTRAINT uk_alr_protocol_directives UNIQUE NULLS NOT DISTINCT (project_id, hazard, alert_level, scope),
    CONSTRAINT ck_alr_protocol_directives_hazard CHECK (hazard IN ('FOREST_FIRE', 'MASS_MOVEMENT', 'WINDSTORM', 'LIGHTNING')),
    CONSTRAINT ck_alr_protocol_directives_alert_level CHECK (alert_level IN ('YELLOW', 'ORANGE', 'RED')),
    CONSTRAINT ck_alr_protocol_directives_scope CHECK (scope IN ('DAILY', 'MONTHLY'))
);

CREATE INDEX IF NOT EXISTS idx_alr_protocol_directives_lookup
    ON alr_protocol_directives (project_id, hazard, alert_level, scope);

COMMENT ON TABLE alr_protocol_directives IS 'Directivas operativas del Sistema de Comando de Incidentes. NULL = global (D13).';
COMMENT ON COLUMN alr_protocol_directives.project_id IS 'FK LÓGICA -> prj_projects(id). Si es NULL representa el protocolo estándar nacional (D13).';


-- ============================================================================
-- 7. BOUNDED CONTEXT: NTF (Notificaciones y Mensajería)
-- ============================================================================

-- Tabla: ntf_templates
-- Plantillas de mensajes transaccionales autorizadas por proveedor (Meta WhatsApp Cloud API, etc.).
CREATE TABLE IF NOT EXISTS ntf_templates (
    code VARCHAR(50) PRIMARY KEY,
    channel_type VARCHAR(20) NOT NULL,
    provider_template_name VARCHAR(100) NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    body_pattern TEXT NOT NULL,

    CONSTRAINT ck_ntf_templates_channel_type CHECK (channel_type IN ('WHATSAPP', 'SMS', 'EMAIL'))
);

COMMENT ON TABLE ntf_templates IS 'Catálogo de plantillas oficiales aprobadas en canales de mensajería (D15).';

-- Tabla: ntf_dispatches
-- Registro histórico y estado de entrega de notificaciones despachadas.
CREATE TABLE IF NOT EXISTS ntf_dispatches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id UUID NOT NULL,               -- FK Lógica -> alr_alerts(id) [D8]
    contact_id UUID NOT NULL,             -- FK Lógica -> prj_contacts(id) [D8]
    channel_type VARCHAR(20) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_message_id VARCHAR(100),
    attempts SMALLINT NOT NULL DEFAULT 0,
    last_error TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK física intra-contexto: ntf_dispatches -> ntf_templates
    CONSTRAINT fk_ntf_dispatches_template FOREIGN KEY (template_code)
        REFERENCES ntf_templates (code) ON DELETE RESTRICT,
    CONSTRAINT ck_ntf_dispatches_channel_type CHECK (channel_type IN ('WHATSAPP', 'SMS', 'EMAIL')),
    CONSTRAINT ck_ntf_dispatches_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED')),
    CONSTRAINT ck_ntf_dispatches_attempts CHECK (attempts >= 0)
);

CREATE INDEX IF NOT EXISTS idx_ntf_dispatches_status_created ON ntf_dispatches (status, created_at);
CREATE INDEX IF NOT EXISTS idx_ntf_dispatches_alert_id ON ntf_dispatches (alert_id);
CREATE INDEX IF NOT EXISTS idx_ntf_dispatches_contact_id ON ntf_dispatches (contact_id);

COMMENT ON TABLE ntf_dispatches IS 'Registro de despachos automáticos de alerta a los contactos operativos.';
COMMENT ON COLUMN ntf_dispatches.alert_id IS 'FK LÓGICA -> alr_alerts(id) (D8).';
COMMENT ON COLUMN ntf_dispatches.contact_id IS 'FK LÓGICA -> prj_contacts(id) (D8).';


-- ============================================================================
-- 8. BOUNDED CONTEXT: REP (Generación y Almacenamiento de Reportes)
-- ============================================================================

-- Tabla: rep_reports
-- Metadatos de reportes formales generados (Excel Apache POI, PDF) y su clave de almacenamiento.
CREATE TABLE IF NOT EXISTS rep_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,             -- FK Lógica -> prj_projects(id) [D8]
    monitoring_run_id UUID NOT NULL,      -- FK Lógica -> alr_monitoring_runs(id) [D8]
    format VARCHAR(10) NOT NULL DEFAULT 'XLSX',
    storage_key VARCHAR(500) NOT NULL,
    size_bytes BIGINT,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_rep_reports_format CHECK (format IN ('XLSX', 'PDF')),
    CONSTRAINT ck_rep_reports_size_bytes CHECK (size_bytes IS NULL OR size_bytes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_rep_reports_project_generated ON rep_reports (project_id, generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_rep_reports_monitoring_run_id ON rep_reports (monitoring_run_id);

COMMENT ON TABLE rep_reports IS 'Archivos formales de reporte generados a partir de una corrida de monitoreo (D16).';
COMMENT ON COLUMN rep_reports.project_id IS 'FK LÓGICA -> prj_projects(id) (D8).';
COMMENT ON COLUMN rep_reports.monitoring_run_id IS 'FK LÓGICA -> alr_monitoring_runs(id) (D8).';
COMMENT ON COLUMN rep_reports.storage_key IS 'Clave lógica en el storage port (volumen local o bucket S3/MinIO) (D16).';


-- ============================================================================
-- 9. DATOS SEMILLA (SEED DATA)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 9.1 Catálogo Geográfico Inicial (DIVIPOLA Colombia)
-- ----------------------------------------------------------------------------
INSERT INTO geo_departments (code, country_code, name) VALUES
    ('05', 'CO', 'Antioquia'),
    ('11', 'CO', 'Bogotá, D.C.'),
    ('25', 'CO', 'Cundinamarca'),
    ('68', 'CO', 'Santander'),
    ('76', 'CO', 'Valle del Cauca')
ON CONFLICT (code) DO NOTHING;

INSERT INTO geo_municipalities (code, department_code, name) VALUES
    ('05001', '05', 'Medellín'),
    ('05113', '05', 'Buriticá'),
    ('11001', '11', 'Bogotá, D.C.'),
    ('68001', '68', 'Bucaramanga'),
    ('76001', '76', 'Cali')
ON CONFLICT (code) DO NOTHING;


-- ----------------------------------------------------------------------------
-- 9.2 Catálogo de Códigos Meteorológicos WMO (World Meteorological Organization)
-- ----------------------------------------------------------------------------
INSERT INTO wea_wmo_codes (code, description_es, is_thunderstorm, is_precipitation) VALUES
    (0,  'Cielo despejado', FALSE, FALSE),
    (1,  'Principalmente despejado', FALSE, FALSE),
    (2,  'Parcialmente nublado', FALSE, FALSE),
    (3,  'Nublado / Cubierto', FALSE, FALSE),
    (45, 'Niebla', FALSE, FALSE),
    (48, 'Niebla con escarcha depositante', FALSE, FALSE),
    (51, 'Llovizna ligera', FALSE, TRUE),
    (53, 'Llovizna moderada', FALSE, TRUE),
    (55, 'Llovizna densa', FALSE, TRUE),
    (61, 'Lluvia ligera', FALSE, TRUE),
    (63, 'Lluvia moderada', FALSE, TRUE),
    (65, 'Lluvia fuerte', FALSE, TRUE),
    (71, 'Nevada ligera', FALSE, TRUE),
    (73, 'Nevada moderada', FALSE, TRUE),
    (75, 'Nevada fuerte', FALSE, TRUE),
    (77, 'Granos de nieve', FALSE, TRUE),
    (80, 'Chubasco / Aguacero ligero', FALSE, TRUE),
    (81, 'Chubasco / Aguacero moderado', FALSE, TRUE),
    (82, 'Chubasco / Aguacero violento', FALSE, TRUE),
    (85, 'Chubasco de nieve ligero', FALSE, TRUE),
    (86, 'Chubasco de nieve fuerte', FALSE, TRUE),
    (95, 'Tormenta eléctrica ligera o moderada', TRUE, TRUE),
    (96, 'Tormenta eléctrica con granizo ligero', TRUE, TRUE),
    (99, 'Tormenta eléctrica con granizo fuerte', TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;


-- ----------------------------------------------------------------------------
-- 9.3 Proveedores Meteorológicos (D11)
-- ----------------------------------------------------------------------------
INSERT INTO wea_providers (code, name, enabled, priority) VALUES
    ('OPEN_METEO', 'Open-Meteo Weather Forecast API', TRUE, 1),
    ('IDEAM', 'Instituto de Hidrología, Meteorología y Estudios Ambientales', FALSE, 2)
ON CONFLICT (code) DO NOTHING;


-- ----------------------------------------------------------------------------
-- 9.4 Directivas Institucionales Globales de Protocolos SCI (D13)
-- (project_id IS NULL: Aplica a nivel global como plantilla por defecto)
-- Textos corregidos y saneados del inventario original de Sarada.
-- ----------------------------------------------------------------------------

-- A. Incendios Forestales (FOREST_FIRE)
INSERT INTO alr_protocol_directives (id, project_id, hazard, alert_level, scope, alert_type_text, call_chain_text, action_text) VALUES
    (
        gen_random_uuid(),
        NULL,
        'FOREST_FIRE',
        'ORANGE',
        'DAILY',
        'ALERTA POR IDENTIFICACIÓN DE LECTURAS ANÓMALAS EN MONITOREO DIARIO PARA NIVEL DE ALERTA NARANJA',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando nivel de alerta Naranja.',
        'ALISTAMIENTO DE BRIGADAS PARA LA ATENCIÓN DE EMERGENCIAS POR ALERTA NARANJA DE INCENDIO FORESTAL.'
    ),
    (
        gen_random_uuid(),
        NULL,
        'FOREST_FIRE',
        'RED',
        'DAILY',
        'ALERTA POR IDENTIFICACIÓN DE LECTURAS ANÓMALAS EN MONITOREO DIARIO PARA NIVEL DE ALERTA ROJA',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando nivel de alerta Roja y activación de mecanismos de alarma.',
        'ACTIVACIÓN DE CADENAS DE LLAMADO, ORDENAR LA VERIFICACIÓN DE OCURRENCIA DEL EVENTO Y DAR INICIO A LAS ACCIONES DE RESPUESTA INMEDIATA POR ALERTA ROJA DE INCENDIO FORESTAL. SI EL EVENTO ES DE GRAN MAGNITUD SOLICITAR LA INSTALACIÓN DE PMU.'
    ),
    (
        gen_random_uuid(),
        NULL,
        'FOREST_FIRE',
        'ORANGE',
        'MONTHLY',
        'ALERTA NARANJA POR IDENTIFICACIÓN DE MES PREVISTO DE AUMENTO ANÓMALO EN EL VALOR PROMEDIO DE LAS LECTURAS DE TEMPERATURA EN MONITOREO MENSUAL',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando anomalía en la climatología mensual prevista.',
        'ALISTAMIENTO DE BRIGADAS PARA LA ATENCIÓN DE EMERGENCIAS POR ALERTA NARANJA DE INCENDIO FORESTAL.'
    ),
    (
        gen_random_uuid(),
        NULL,
        'FOREST_FIRE',
        'RED',
        'MONTHLY',
        'ALERTA ROJA POR IDENTIFICACIÓN DE MES PREVISTO DE AUMENTO ANÓMALO EN EL VALOR PROMEDIO DE LAS LECTURAS DE TEMPERATURA EN MONITOREO MENSUAL',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando anomalía crítica en la climatología mensual prevista.',
        'ACTIVACIÓN DE CADENAS DE LLAMADO, ORDENAR LA VERIFICACIÓN DE OCURRENCIA DEL EVENTO Y DAR INICIO A LAS ACCIONES DE RESPUESTA INMEDIATA POR ALERTA ROJA DE INCENDIO FORESTAL. SI EL EVENTO ES DE GRAN MAGNITUD SOLICITAR LA INSTALACIÓN DE PMU.'
    ),

-- B. Movimientos en Masa / Deslizamientos (MASS_MOVEMENT)
    (
        gen_random_uuid(),
        NULL,
        'MASS_MOVEMENT',
        'ORANGE',
        'DAILY',
        'ALERTA POR IDENTIFICACIÓN DE LECTURAS ANÓMALAS EN MONITOREO DIARIO PARA NIVEL DE ALERTA NARANJA',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando nivel de alerta Naranja.',
        'ALISTAMIENTO DE BRIGADAS PARA LA ATENCIÓN DE EMERGENCIAS POR ALERTA NARANJA DE MOVIMIENTOS EN MASA.'
    ),
    (
        gen_random_uuid(),
        NULL,
        'MASS_MOVEMENT',
        'RED',
        'DAILY',
        'ALERTA POR IDENTIFICACIÓN DE LECTURAS ANÓMALAS EN MONITOREO DIARIO PARA NIVEL DE ALERTA ROJA',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando nivel de alerta Roja y activación de mecanismos de alarma.',
        'ACTIVACIÓN DE CADENAS DE LLAMADO, ORDENAR LA VERIFICACIÓN DE OCURRENCIA DEL EVENTO Y DAR INICIO A LAS ACCIONES DE RESPUESTA INMEDIATA POR ALERTA ROJA DE MOVIMIENTOS EN MASA. SI EL EVENTO ES DE GRAN MAGNITUD SOLICITAR LA INSTALACIÓN DE PMU.'
    ),
    (
        gen_random_uuid(),
        NULL,
        'MASS_MOVEMENT',
        'YELLOW',
        'MONTHLY',
        'ALERTA AMARILLA POR IDENTIFICACIÓN DE CONDICIÓN PREVENTIVA EN MONITOREO MENSUAL DE PRECIPITACIÓN',
        'Comunicar al jefe del sistema de comando de incidentes y al Jefe de Área para seguimiento preventivo y alistamiento de recursos.',
        'PREPARACIÓN PARA LA RESPUESTA, ALISTAMIENTO DE RECURSOS, SUMINISTROS Y SERVICIOS E IDENTIFICACIÓN DE LAS RUTAS DE INGRESO Y EGRESO.'
    ),
    (
        gen_random_uuid(),
        NULL,
        'MASS_MOVEMENT',
        'ORANGE',
        'MONTHLY',
        'ALERTA NARANJA POR IDENTIFICACIÓN DE MES PREVISTO DE AUMENTO ANÓMALO EN EL VALOR PROMEDIO DE LAS LECTURAS DE PRECIPITACIÓN EN MONITOREO MENSUAL',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando anomalía en la climatología mensual prevista.',
        'ALISTAMIENTO DE BRIGADAS PARA LA ATENCIÓN DE EMERGENCIAS POR ALERTA NARANJA DE MOVIMIENTOS EN MASA.'
    ),
    (
        gen_random_uuid(),
        NULL,
        'MASS_MOVEMENT',
        'RED',
        'MONTHLY',
        'ALERTA ROJA POR IDENTIFICACIÓN DE MES PREVISTO DE AUMENTO ANÓMALO EN EL VALOR PROMEDIO DE LAS LECTURAS DE PRECIPITACIÓN EN MONITOREO MENSUAL',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando anomalía crítica en la climatología mensual prevista.',
        'ACTIVACIÓN DE CADENAS DE LLAMADO, ORDENAR LA VERIFICACIÓN DE OCURRENCIA DEL EVENTO Y DAR INICIO A LAS ACCIONES DE RESPUESTA INMEDIATA POR ALERTA ROJA DE MOVIMIENTOS EN MASA. SI EL EVENTO ES DE GRAN MAGNITUD SOLICITAR LA INSTALACIÓN DE PMU.'
    ),

-- C. Vendavales (WINDSTORM)
    (
        gen_random_uuid(),
        NULL,
        'WINDSTORM',
        'ORANGE',
        'DAILY',
        'ALERTA POR IDENTIFICACIÓN DE LECTURAS ANÓMALAS EN MONITOREO DIARIO PARA NIVEL DE ALERTA NARANJA',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando nivel de alerta Naranja.',
        'ALISTAMIENTO DE BRIGADAS PARA LA ATENCIÓN DE EMERGENCIAS POR ALERTA NARANJA DE VENDAVALES.'
    ),
    (
        gen_random_uuid(),
        NULL,
        'WINDSTORM',
        'RED',
        'DAILY',
        'ALERTA POR IDENTIFICACIÓN DE LECTURAS ANÓMALAS EN MONITOREO DIARIO PARA NIVEL DE ALERTA ROJA',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando nivel de alerta Roja y activación de mecanismos de alarma.',
        'ACTIVACIÓN DE CADENAS DE LLAMADO, ORDENAR LA VERIFICACIÓN DE OCURRENCIA DEL EVENTO Y DAR INICIO A LAS ACCIONES DE RESPUESTA INMEDIATA POR ALERTA ROJA DE VENDAVALES. SI EL EVENTO ES DE GRAN MAGNITUD SOLICITAR LA INSTALACIÓN DE PMU.'
    ),

-- D. Fenómenos Ceráunicos / Tormentas Eléctricas (LIGHTNING)
    (
        gen_random_uuid(),
        NULL,
        'LIGHTNING',
        'RED',
        'DAILY',
        'ALERTA POR IDENTIFICACIÓN DE LECTURAS ANÓMALAS EN MONITOREO DIARIO PARA NIVEL DE ALERTA ROJA',
        'Comunicar de manera inmediata al jefe del sistema de comando de incidentes y al Jefe de Área y/o Auxiliar del Comandante de Incidentes indicando nivel de alerta Roja y activación de mecanismos de alarma.',
        'ACTIVAR EN PREVENTIVO EL PMU Y ALISTAMIENTO DE BRIGADAS DE EMERGENCIAS (EQUIPOS LISTOS PARA REACCIÓN INMEDIATA) ALERTA ROJA POR FENÓMENOS CERÁUNICOS.'
    )
ON CONFLICT (project_id, hazard, alert_level, scope) DO NOTHING;


-- ----------------------------------------------------------------------------
-- 9.5 Plantillas de Notificación por Defecto (NTF)
-- ----------------------------------------------------------------------------
INSERT INTO ntf_templates (code, channel_type, provider_template_name, language, body_pattern) VALUES
    (
        'alert_threshold',
        'WHATSAPP',
        'alert_threshold',
        'en',
        'Header: {{header}} | Directiva: {{action}} | Fecha prevista: {{target_date}}'
    )
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- FIN DEL ESQUEMA DDL POSTGRESQL 16
-- ============================================================================
