-- =====================================================
-- 🇹🇿 National Cyber Security & Digital Forensics System
-- PostgreSQL Database Schema
-- =====================================================

CREATE DATABASE incident_db;
-- Kisha unganisha: \c incident_db

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

CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    reporter VARCHAR(100) NOT NULL,
    reporter_user_id BIGINT REFERENCES users(id),
    date_reported TIMESTAMP NOT NULL,
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
    assigned_to BIGINT REFERENCES users(id),
    is_closed BOOLEAN DEFAULT FALSE,
    closed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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
    uploaded_by BIGINT REFERENCES users(id),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    source_device VARCHAR(200),
    acquisition_method VARCHAR(200),
    verified BOOLEAN DEFAULT FALSE,
    verified_by BIGINT REFERENCES users(id),
    verified_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chain_of_custody (
    id BIGSERIAL PRIMARY KEY,
    evidence_id BIGINT NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT REFERENCES users(id),
    performed_by_name VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    purpose TEXT,
    notes TEXT
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    username VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_incident_id ON incidents(incident_id);
CREATE INDEX IF NOT EXISTS idx_incident_status ON incidents(status);
CREATE INDEX IF NOT EXISTS idx_incident_severity ON incidents(severity);
CREATE INDEX IF NOT EXISTS idx_evidence_incident ON evidence(incident_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);

-- Default admin: admin / Admin@123
INSERT INTO users (username, email, password, full_name, role, organization)
VALUES ('admin', 'admin@cyber.go.tz',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
        'System Administrator', 'ADMIN', 'National Cyber Security')
ON CONFLICT (username) DO NOTHING;

-- Sample incidents
INSERT INTO incidents (incident_id, title, description, reporter, date_reported, status, severity, cvss_score, category, region, financial_loss_tzs)
VALUES
('SEC-20250101-001', 'Data Breach - Bank Customer Records',
 'Unauthorized access to customer database with 50,000 records', 'Baharia Nkasi',
 NOW(), 'Critical', 'CRITICAL', 9.5, 'Data Breach', 'Dar es Salaam', 50000000),
('SEC-20250101-002', 'Phishing Campaign - Government Employees',
 'Spear-phishing emails targeting government accounts', 'Jackline Mushi',
 NOW(), 'Under Investigation', 'HIGH', 7.8, 'Phishing', 'Dodoma', 5000000),
('SEC-20250101-003', 'Ransomware Attempt - Hospital',
 'Ransomware detected and blocked before encryption', 'Dominick Mkapa',
 NOW(), 'Resolved', 'HIGH', 8.0, 'Ransomware', 'Mwanza', 0)
ON CONFLICT (incident_id) DO NOTHING;
