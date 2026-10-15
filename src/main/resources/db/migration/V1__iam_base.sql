-- ============================================================================
-- V1: Esquema Base IAM (Identity & Access Management)
-- Alineado con la Decisión D2 (Roles de plataforma ADMIN y USER)
-- ============================================================================

CREATE TABLE IF NOT EXISTS iam_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    phone_e164 VARCHAR(30),
    platform_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_iam_users_email UNIQUE (email),
    CONSTRAINT ck_iam_users_platform_role CHECK (platform_role IN ('ADMIN', 'USER')),
    CONSTRAINT ck_iam_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'))
);

CREATE TABLE IF NOT EXISTS iam_refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_iam_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES iam_users (id) ON DELETE CASCADE,
    CONSTRAINT uk_iam_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_iam_refresh_tokens_user_id ON iam_refresh_tokens (user_id);
