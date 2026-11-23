-- ==============================================================================
-- V1__initial_schema.sql
-- OpsPilot Database Migration: Initial Schema
-- Establishes the multi-tenant foundation for the control plane
-- ==============================================================================

-- ============================================================================
-- Organizations (Tenants)
-- ============================================================================
CREATE TABLE IF NOT EXISTS organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    plan            VARCHAR(50)  NOT NULL DEFAULT 'free',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_organizations_slug ON organizations (slug);

-- ============================================================================
-- Users
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(255) NOT NULL,
    role                VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'OPERATOR', 'VIEWER')),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_email_per_org UNIQUE (organization_id, email)
);

CREATE INDEX idx_users_org ON users (organization_id);
CREATE INDEX idx_users_email ON users (email);

-- ============================================================================
-- Incidents
-- ============================================================================
CREATE TABLE IF NOT EXISTS incidents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    title               VARCHAR(500) NOT NULL,
    severity            VARCHAR(20)  NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    status              VARCHAR(20)  NOT NULL CHECK (status IN ('OPEN', 'INVESTIGATING', 'IDENTIFIED', 'MONITORING', 'RESOLVED')),
    service_name        VARCHAR(255) NOT NULL,
    environment         VARCHAR(50)  NOT NULL CHECK (environment IN ('production', 'staging', 'development', 'qa')),
    alert_count         INTEGER      NOT NULL DEFAULT 1,
    summary             TEXT,
    resolved_at         TIMESTAMPTZ,
    acknowledged_by     UUID         REFERENCES users(id),
    acknowledged_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incidents_org_status ON incidents (organization_id, status);
CREATE INDEX idx_incidents_org_service ON incidents (organization_id, service_name);
CREATE INDEX idx_incidents_created_at ON incidents (created_at DESC);
CREATE INDEX idx_incidents_severity ON incidents (organization_id, severity);

-- ============================================================================
-- Alerts (individual alert events grouped into incidents)
-- ============================================================================
CREATE TABLE IF NOT EXISTS alerts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id         UUID         REFERENCES incidents(id) ON DELETE SET NULL,
    organization_id     UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    external_alert_id   VARCHAR(255),
    source              VARCHAR(100) NOT NULL,
    service_name        VARCHAR(255) NOT NULL,
    severity            VARCHAR(20)  NOT NULL,
    message             TEXT         NOT NULL,
    environment         VARCHAR(50)  NOT NULL,
    fingerprint         VARCHAR(255) NOT NULL,
    labels              JSONB        NOT NULL DEFAULT '{}',
    metadata            JSONB        NOT NULL DEFAULT '{}',
    received_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_org_fingerprint ON alerts (organization_id, fingerprint, received_at DESC);
CREATE INDEX idx_alerts_incident ON alerts (incident_id);
CREATE INDEX idx_alerts_org_service ON alerts (organization_id, service_name);

-- ============================================================================
-- Outbox Events (Transactional Outbox pattern for Kafka publishing)
-- ============================================================================
CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        VARCHAR(255) NOT NULL UNIQUE,
    organization_id UUID         NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    partition_key   VARCHAR(255),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    payload         JSONB        NOT NULL,
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending ON outbox_events (status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_event_id ON outbox_events (event_id);

-- ============================================================================
-- Triage Reports (AI investigation results persisted from Kafka consumer)
-- ============================================================================
CREATE TABLE IF NOT EXISTS triage_reports (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id             UUID         NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    organization_id         UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    triage_id               UUID         NOT NULL UNIQUE,
    confidence_score        DECIMAL(4,3),
    investigation_duration_ms INTEGER,
    observations            JSONB        NOT NULL DEFAULT '[]',
    supporting_evidence     JSONB        NOT NULL DEFAULT '[]',
    potential_root_causes   JSONB        NOT NULL DEFAULT '[]',
    suggested_remediations  JSONB        NOT NULL DEFAULT '[]',
    runbook_citations       JSONB        NOT NULL DEFAULT '[]',
    uncertainties           JSONB        NOT NULL DEFAULT '[]',
    additional_steps        JSONB        NOT NULL DEFAULT '[]',
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_triage_incident ON triage_reports (incident_id);
CREATE INDEX idx_triage_org ON triage_reports (organization_id);

-- ============================================================================
-- Audit Logs (immutable record of every significant action)
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id         UUID         REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(255),
    details         JSONB        NOT NULL DEFAULT '{}',
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_org_created ON audit_logs (organization_id, created_at DESC);
CREATE INDEX idx_audit_entity ON audit_logs (organization_id, entity_type, entity_id);

-- ============================================================================
-- Idempotency Keys (processed events tracking to prevent duplicate handling)
-- ============================================================================
CREATE TABLE IF NOT EXISTS processed_events (
    event_id        VARCHAR(255) NOT NULL,
    organization_id UUID         NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, organization_id)
);

CREATE INDEX idx_processed_events_cleanup ON processed_events (processed_at);
