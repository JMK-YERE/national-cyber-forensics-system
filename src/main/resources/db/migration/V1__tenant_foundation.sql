-- Enterprise multi-tenancy foundation.
-- Additive only: legacy rows remain tenant_id NULL until an approved, verified backfill.
-- Do not enable tenant-scoped application access until all owned records are assigned and guarded.

CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    sector VARCHAR(80),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenants_slug UNIQUE (slug),
    CONSTRAINT ck_tenants_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

CREATE TABLE IF NOT EXISTS tenant_memberships (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role VARCHAR(40) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_tenant_memberships_tenant_user UNIQUE (tenant_id, user_id),
    CONSTRAINT ck_tenant_memberships_status CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'REVOKED'))
);

CREATE TABLE IF NOT EXISTS tenant_policies (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    policy_type VARCHAR(60) NOT NULL,
    policy_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    version INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_tenant_policies_type UNIQUE (tenant_id, policy_type),
    CONSTRAINT ck_tenant_policies_version CHECK (version > 0)
);

CREATE TABLE IF NOT EXISTS tenant_audit_events (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenants(id) ON DELETE RESTRICT,
    actor_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(160),
    outcome VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    reason TEXT,
    correlation_id VARCHAR(100),
    source_ip VARCHAR(45),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    previous_event_hash CHAR(64),
    event_hash CHAR(64),
    CONSTRAINT ck_tenant_audit_outcome CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILURE'))
);

CREATE INDEX IF NOT EXISTS ix_tenant_memberships_user_status ON tenant_memberships (user_id, status);
CREATE INDEX IF NOT EXISTS ix_tenant_memberships_tenant_status ON tenant_memberships (tenant_id, status);
CREATE INDEX IF NOT EXISTS ix_tenant_policies_tenant ON tenant_policies (tenant_id);
CREATE INDEX IF NOT EXISTS ix_tenant_audit_tenant_time ON tenant_audit_events (tenant_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_tenant_audit_resource ON tenant_audit_events (resource_type, resource_id);

-- Nullable during migration/backfill. Existing records are intentionally not assigned automatically.
DO $$
DECLARE
    table_name TEXT;
    owned_tables TEXT[] := ARRAY[
        'incidents', 'case_files', 'evidence', 'case_timeline', 'chain_of_custody',
        'case_tasks', 'case_ioc', 'case_communications', 'compliance_reports',
        'audit_logs', 'notifications'
    ];
BEGIN
    FOREACH table_name IN ARRAY owned_tables LOOP
        IF to_regclass(format('public.%I', table_name)) IS NOT NULL THEN
            EXECUTE format(
                'ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES tenants(id) ON DELETE RESTRICT',
                table_name
            );
            EXECUTE format(
                'CREATE INDEX IF NOT EXISTS %I ON public.%I (tenant_id)',
                'ix_' || table_name || '_tenant_id',
                table_name
            );
        END IF;
    END LOOP;
END $$;
