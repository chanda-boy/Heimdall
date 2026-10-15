-- ============================================================================
-- V2: Catálogo Geográfico Territorial (DIVIPOLA Colombia)
-- Alineado con la Decisión D5
-- ============================================================================

CREATE TABLE IF NOT EXISTS geo_departments (
    code VARCHAR(2) PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL DEFAULT 'CO',
    name VARCHAR(100) NOT NULL,

    CONSTRAINT ck_geo_departments_code CHECK (length(code) = 2)
);

CREATE TABLE IF NOT EXISTS geo_municipalities (
    code VARCHAR(5) PRIMARY KEY,
    department_code VARCHAR(2) NOT NULL,
    name VARCHAR(100) NOT NULL,

    CONSTRAINT fk_geo_municipalities_department FOREIGN KEY (department_code)
        REFERENCES geo_departments (code) ON DELETE RESTRICT,
    CONSTRAINT ck_geo_municipalities_code CHECK (length(code) = 5)
);

CREATE INDEX IF NOT EXISTS idx_geo_municipalities_department ON geo_municipalities (department_code);

-- Seed Data: DIVIPOLA Departamentos de Colombia
INSERT INTO geo_departments (code, country_code, name) VALUES
    ('05', 'CO', 'Antioquia'),
    ('11', 'CO', 'Bogotá, D.C.'),
    ('25', 'CO', 'Cundinamarca'),
    ('68', 'CO', 'Santander'),
    ('76', 'CO', 'Valle del Cauca');

-- Seed Data: DIVIPOLA Municipios de Colombia
INSERT INTO geo_municipalities (code, department_code, name) VALUES
    ('05001', '05', 'Medellín'),
    ('05113', '05', 'Buriticá'),
    ('11001', '11', 'Bogotá, D.C.'),
    ('68001', '68', 'Bucaramanga'),
    ('76001', '76', 'Cali');
