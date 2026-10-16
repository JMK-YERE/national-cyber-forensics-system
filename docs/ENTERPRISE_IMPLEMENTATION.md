# Enterprise implementation: multi-tenancy, compliance and incident response

## Status

This branch introduces the additive database foundation only. It is not yet a complete multi-tenant implementation and must not be promoted to production until application-level tenant authorization, data backfill, tests, and restore validation are complete.

## Existing code found during repository inspection

- Spring Boot 3.5.16, Java 17, PostgreSQL/JPA, Thymeleaf and Spring Security.
- Existing entities/controllers/services for incidents, case files, evidence, chain of custody, timeline, tasks, IOCs, communications, compliance and audit.
- Existing evidence storage abstraction with local and S3 implementations and a production-storage validator.
- application.properties defaults Hibernate DDL mode to update, evidence storage to local, and production-storage-required to false.
- Existing tests cover several services, but a complete cross-tenant authorization test suite was not identified during this initial pass.

## Important rollout guardrails

1. Back up the exact database used by the deployed service and restore it into a disposable environment before any migration rollout.
2. Apply the migration to a disposable PostgreSQL database first. Verify legacy tables and foreign keys.
3. Do not infer tenant ownership from the free-text organization column or user role. Establish and approve a deterministic ownership map.
4. Keep new tenant_id columns nullable during migration. Backfill from an approved mapping, reconcile row counts/orphans, then add NOT NULL and composite constraints only in a later reviewed migration.
5. Do not expose tenant-owned records through new endpoints until tenant context is resolved from authenticated identity and membership, and all reads/writes/downloads/exports enforce it.
6. Do not rely on a browser-supplied tenant ID, hidden form field, URL, or request header as authorization.
7. Do not merge this branch into the production branch or trigger production deployment without explicit approval.

## Implementation sequence

### P0 — safe data and tenant foundation
- [x] Add versioned migration tooling and initial tenant/membership/policy/audit schema.
- [x] Add nullable tenant ownership columns to existing tenant-owned tables, without assigning old records automatically.
- [ ] Inspect the live database schema and actual Render/Neon connection target.
- [ ] Verify backup and restore, then apply migration only to a disposable clone.
- [ ] Add tenant-aware entities/repositories and authenticated tenant context.
- [ ] Apply tenant-scoped authorization to every controller, service, repository query, file download, search, notification and report/export.
- [ ] Add cross-tenant denial tests before assigning any legacy data to tenants.
- [ ] Backfill legacy data from a reviewed mapping; reconcile counts; only then enforce non-null ownership.

### P1 — evidence and compliance
- [ ] Evidence acquisition records include collector, method, source, tool/version, timestamps/time zone and notes.
- [ ] Preserve immutable originals; work on verified copies; keep SHA-256 verification and custody event history.
- [ ] Add retention policy, legal hold, export approval, redaction and policy versioning.
- [ ] Ensure evidence files use persistent object storage with encryption keys managed outside source control.
- [ ] Validate PDF reports against database records and include evidence manifests and custody history.

### P2 — incident response and integrations
- [ ] Tasks, approvals and response playbooks for phishing, ransomware, account compromise, malware and data breach.
- [ ] IOC source/confidence/expiry and MISP/STIX/TAXII interoperability.
- [ ] Authenticated/idempotent SIEM/EDR webhook ingestion, deduplication and alert-to-incident correlation.
- [ ] Vulnerability tracking with CVE/CVSS, remediation owner/SLA and verification.

### P3 — enterprise readiness
- [ ] MFA and optional OIDC/SAML SSO, session revocation and privileged-access review.
- [ ] SLA metrics, tenant-scoped executive reports, restore drills and recovery objectives.
- [ ] Partner/referral workflows for Tanzania institutions, only under approved agreements.

## Acceptance gates

- Tenant A cannot list, read, mutate, upload to, download, search, notify, or export Tenant B records even with guessed IDs.
- Missing/invalid tenant context fails closed.
- Existing users and legacy records remain intact through migration and restore.
- Evidence hash mismatch fails verification; original evidence cannot be overwritten through normal application flows.
- Audit records cannot be edited/deleted through the application; privileged actions and denied access are logged.
- CI runs mvn -B clean verify; migration is tested on an empty database and a production-like clone.
- Production rollout requires explicit approval after staging evidence and rollback plan are reviewed.

## Free/open-source preference

Prefer existing Spring Security, PostgreSQL, Flyway, JUnit, Testcontainers, GitHub Actions and open-source forensic tools. Validate provider free-tier limits before promising availability or retention. Never store secrets in Git.