-- ============================================================================
-- V3: Bounded Context PRJ (Gestión de Proyectos y Contactos Operativos SCI)
-- Alineado con Decisiones D1, D3, D4, D6, D8 y D14
-- ============================================================================

CREATE TABLE IF NOT EXISTS prj_projects (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,               -- FK Lógica -> iam_users(id) [D1, D8]
    code VARCHAR(50),                     -- Código nemotécnico opcional único por dueño [D4]
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
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

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

CREATE TABLE IF NOT EXISTS prj_contacts (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    user_id UUID,                         -- FK Lógica -> iam_users(id) [D3, D8, nullable]
    full_name VARCHAR(150) NOT NULL,
    sci_position VARCHAR(30) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

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

CREATE TABLE IF NOT EXISTS prj_contact_channels (
    id UUID PRIMARY KEY,
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
