-- ============================================================================
-- OpsPilot V2 Database Migration: FinOps Cloud Financial Intelligence
-- Tables: cloud_cost_items, cost_anomalies
-- ============================================================================

CREATE TABLE IF NOT EXISTS cloud_cost_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID           NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    provider            VARCHAR(50)    NOT NULL, -- AWS, GCP, AZURE
    service_name        VARCHAR(255)   NOT NULL,
    environment         VARCHAR(50)    NOT NULL,
    amount              DECIMAL(12, 4) NOT NULL,
    currency            VARCHAR(10)    NOT NULL DEFAULT 'USD',
    usage_date          DATE           NOT NULL,
    resource_id         VARCHAR(255),
    tags                JSONB          NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cloud_cost_org_date ON cloud_cost_items (organization_id, usage_date DESC);
CREATE INDEX idx_cloud_cost_org_service ON cloud_cost_items (organization_id, service_name, usage_date DESC);

CREATE TABLE IF NOT EXISTS cost_anomalies (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID           NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    service_name        VARCHAR(255)   NOT NULL,
    environment         VARCHAR(50)    NOT NULL,
    detection_date      DATE           NOT NULL,
    actual_amount       DECIMAL(12, 4) NOT NULL,
    expected_amount     DECIMAL(12, 4) NOT NULL,
    percentage_change   DECIMAL(8, 2)  NOT NULL,
    z_score             DECIMAL(8, 2)  NOT NULL,
    severity            VARCHAR(20)    NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    correlated_incident_id UUID        REFERENCES incidents(id) ON DELETE SET NULL,
    correlated_deployment_version VARCHAR(100),
    status              VARCHAR(20)    NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    explanation         TEXT,
    detected_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cost_anomalies_org_date ON cost_anomalies (organization_id, detection_date DESC);
CREATE INDEX idx_cost_anomalies_service ON cost_anomalies (organization_id, service_name);
