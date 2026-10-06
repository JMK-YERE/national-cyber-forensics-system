-- =====================================================
-- 🇹🇿 National Cyber Security & Digital Forensics System
-- PostgreSQL Database Schema
-- Tanzania National Incident Reporting System


-- ============================================
--TABLES
-- ============================================

-- ---------- USERS TABLE ----------
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    organization VARCHAR(100),
    role VARCHAR(30) DEFAULT 'REPORTER',
    enabled BOOLEAN DEFAULT TRUE,
    account_locked BOOLEAN DEFAULT FALSE,
    failed_attempts INT DEFAULT 0,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- INCIDENTS TABLE ----------
CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    reporter VARCHAR(100) NOT NULL,
    reporter_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    date_reported TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_occurred TIMESTAMP,
    status VARCHAR(50) DEFAULT 'Under Investigation',
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    cvss_score NUMERIC(3,1),
    category VARCHAR(50),
    mitre_tactic VARCHAR(100),
    mitre_technique VARCHAR(100),
    region VARCHAR(50),
    organization VARCHAR(100),
    financial_loss_tzs NUMERIC(15,2),
    police_case_number VARCHAR(50),
    tcra_reference VARCHAR(50),
    assigned_to BIGINT REFERENCES users(id) ON DELETE SET NULL,
    is_closed BOOLEAN DEFAULT FALSE,
    closed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- EVIDENCE TABLE ----------
CREATE TABLE IF NOT EXISTS evidence (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    sha256_hash VARCHAR(64) NOT NULL,
    md5_hash VARCHAR(32),
    encrypted BOOLEAN DEFAULT TRUE,
    uploaded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    source_device VARCHAR(200),
    acquisition_method VARCHAR(200),
    verified BOOLEAN DEFAULT FALSE,
    verified_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    verified_at TIMESTAMP
);

-- ---------- CHAIN OF CUSTODY TABLE ----------
CREATE TABLE IF NOT EXISTS chain_of_custody (
    id BIGSERIAL PRIMARY KEY,
    evidence_id BIGINT NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    performed_by_name VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    purpose TEXT,
    notes TEXT
);

-- ---------- AUDIT LOGS TABLE ----------
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    username VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- IOCS TABLE (Indicators of Compromise) ----------
CREATE TABLE IF NOT EXISTS iocs (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
    ioc_type VARCHAR(30) NOT NULL,
    ioc_value VARCHAR(500) NOT NULL,
    description TEXT,
    confidence VARCHAR(20) DEFAULT 'MEDIUM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- STEP 3: INDEXES (kwa performance)
-- ============================================

CREATE INDEX IF NOT EXISTS idx_incident_id ON incidents(incident_id);
CREATE INDEX IF NOT EXISTS idx_incident_status ON incidents(status);
CREATE INDEX IF NOT EXISTS idx_incident_severity ON incidents(severity);
CREATE INDEX IF NOT EXISTS idx_incident_date ON incidents(date_reported DESC);
CREATE INDEX IF NOT EXISTS idx_incident_region ON incidents(region);
CREATE INDEX IF NOT EXISTS idx_incident_category ON incidents(category);

CREATE INDEX IF NOT EXISTS idx_evidence_incident ON evidence(incident_id);
CREATE INDEX IF NOT EXISTS idx_evidence_hash ON evidence(sha256_hash);
CREATE INDEX IF NOT EXISTS idx_evidence_verified ON evidence(verified);

CREATE INDEX IF NOT EXISTS idx_custody_evidence ON chain_of_custody(evidence_id);
CREATE INDEX IF NOT EXISTS idx_custody_timestamp ON chain_of_custody(timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action);

CREATE INDEX IF NOT EXISTS idx_ioc_incident ON iocs(incident_id);
CREATE INDEX IF NOT EXISTS idx_ioc_type ON iocs(ioc_type);

-- ============================================
-- STEP 4: INITIAL ADMINISTRATION
-- ============================================
-- No default credentials are created by this schema.
-- Create the first administrator only through controlled deployment
-- configuration (app.initial-admin-username / app.initial-admin-password)
-- or an existing administrative provisioning process.
