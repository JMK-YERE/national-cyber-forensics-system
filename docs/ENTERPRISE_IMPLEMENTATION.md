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
- [x] Add tenant, membership, policy and tenant-audit entities/repositories plus global-admin-only tenant management endpoints.
- [ ] Inspect the live database schema and actual Render/Neon connection target.
- [ ] Verify backup and restore, then apply migration only to a disposable clone.
- [x] Map nullable `tenant_id` fields on Incident, CaseFile and Evidence entities; add tenant-filtered repository query methods for these resources.
- [x] Require active tenant membership for tenant-owned Incident, Case and Evidence access checks in the corresponding web controllers; this remains an incremental guard, not complete isolation.
- [x] Add session-backed active tenant selection at `/api/tenants/{tenantId}/activate`, guarded by active membership; incident creation persists the selected tenant after re-validating membership.
- [ ] Require tenant selection for all new institutional incidents and enforce tenant context across every service, download, search, notification and report/export.
- [ ] Apply tenant-scoped authorization to every controller, service, repository query, file download, search, notification and report/export.
- [x] Add service-level tests for missing membership, revoked membership, insufficient tenant role, suspended tenant, and unknown user; add a Case tenant-inheritance test. [ ] Add controller integration tests for cross-tenant denial before assigning any legacy data to tenants.
- [ ] Backfill legacy data from a reviewed mapping; reconcile counts; only then enforce non-null ownership.

### P1 — evidence and compliance
- [x] Evidence acquisition records derive tenant ownership from the parent Incident and reject a linked Case whose tenant differs from the parent Incident.
- [x] Evidence acquisition for tenant-owned incidents requires an active tenant membership.
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

## Current API surface (foundation only)

### Tenant ownership mapping progress

The incident, case-file and evidence JPA entities now map the nullable `tenant_id` column and repositories expose tenant-filtered lookup/list methods. This is a schema/repository foundation only: existing controller and service flows still contain global reads and role-only bypasses. Do not treat these methods as an authorization boundary until callers consistently use them and cross-tenant tests pass.\n\n- GET /admin/api/tenants — list tenants; restricted by the existing /admin/** ADMIN security rule.\n- POST /admin/api/tenants — create a tenant.\n- POST /admin/api/tenants/{tenantId}/members — add/reactivate a membership using an existing user ID.\n\nThese endpoints manage tenant metadata only. They do not mean that incidents, cases, evidence, downloads, reports or notifications are tenant-isolated yet. Do not onboard real customer data until all cross-tenant authorization gates pass.\n\n## Acceptance gates

- Tenant A cannot list, read, mutate, upload to, download, search, notify, or export Tenant B records even with guessed IDs.
- Missing/invalid tenant context fails closed.
- Existing users and legacy records remain intact through migration and restore.
- Evidence hash mismatch fails verification; original evidence cannot be overwritten through normal application flows.
- Audit records cannot be edited/deleted through the application; privileged actions and denied access are logged.
- CI runs mvn -B clean verify; migration is tested on an empty database and a production-like clone.
- Production rollout requires explicit approval after staging evidence and rollback plan are reviewed.

## Free/open-source preference

Prefer existing Spring Security, PostgreSQL, Flyway, JUnit, Testcontainers, GitHub Actions and open-source forensic tools. Validate provider free-tier limits before promising availability or retention. Never store secrets in Git.

## New capability: response playbook catalog

- Added staff-only read-only APIs at `GET /api/response-playbooks` and `GET /api/response-playbooks/{slug}`.
- Includes guided templates for phishing/BEC, ransomware, account compromise, suspected data breach, and malware.
- Each template has versioned guidance and ordered phases covering triage, evidence preservation, containment, investigation/eradication, recovery and lessons learned.
- Routes are restricted to ADMIN, CYBER_PRO, FORENSICS and ANALYST authorities.
- Unit tests verify core catalog coverage, step order, required guidance and unknown-slug behavior.
- This is a catalog, not an automated executor. Case-linked runs, task assignment, tenant checks, approval gates and execution audit must come after case-level tenant authorization.

## Deep-development increment: persisted playbook runs

- Added additive migration `V2__response_playbook_execution.sql` for run and step history.
- A playbook run snapshots the catalog version and ordered guidance so later catalog edits do not rewrite historical case workflows.
- Staff can start a run on an authorized, non-closed case, load prior runs, and finalize individual steps as completed or skipped.
- Skipped steps require a reason; notes are bounded; finalized steps cannot be edited again; a run completes when no pending steps remain.
- Start and step updates are appended to the case timeline. The UI uses the authenticated session and CSRF token for write requests.
- Server-side authorization is rechecked for every read/write. Analyst access requires both underlying incident involvement and case involvement.
- The migration conditionally adds legacy foreign keys because Flyway runs before Hibernate may create the legacy schema on a fresh database. A production migration rehearsal is still required.

## Release gate remains

The tenant schema is only a foundation: existing incidents, case files, evidence, downloads, reports, notifications, search and export flows have not all been tenant-scoped. Do not merge/deploy until tenant ownership/backfill is approved, all data access paths are scoped, cross-tenant denial tests pass, migrations are tested against both an empty database and a restored production-like backup, and security review is complete.

## Tenant audit integrity

Tenant administration audit writes now serialize on the tenant row, link each event to the previous event hash, and compute a SHA-256 digest over length-prefixed event fields. This helps detect accidental or unauthorized edits when the chain is independently verified. It is **tamper-evident, not tamper-proof**: a database owner able to rewrite the full chain can forge it. Higher assurance requires external/WORM log export, key separation, monitoring and independent verification.

Admin audit APIs:
- `GET /admin/api/tenants/{tenantId}/audit-events` — latest 100 tenant audit events.
- `GET /admin/api/tenants/{tenantId}/audit-events/verify` — verifies previous-hash links and recalculates each stored event hash, returning the first invalid event ID when a mismatch is detected.

## Tenant policy administration

Admin APIs now support tenant-scoped policy documents:
- `GET /admin/api/tenants/{tenantId}/policies`
- `PUT /admin/api/tenants/{tenantId}/policies/{policyType}`

Supported policy types are `EVIDENCE_RETENTION`, `EXPORT_APPROVAL`, `NOTIFICATIONS`, and `INCIDENT_SLA`. Input is validated by type, each update increments a version, and the change is appended to the tenant audit chain. Secret-like fields (passwords, tokens, API keys, credentials) are rejected so policy JSON is not used as a secret store. These policies are currently stored and audited; existing evidence retention, export and notification execution paths still need to consume them before the policies can be treated as enforced controls.

## Tenant membership lifecycle

- `GET /admin/api/tenants/{tenantId}/members` lists active memberships.
- `PUT /admin/api/tenants/{tenantId}/members/{userId}` changes a tenant role.
- `DELETE /admin/api/tenants/{tenantId}/members/{userId}` revokes membership without deleting history.
- Role changes and revocations are audit-chained. The service refuses to remove or demote the last active `TENANT_ADMIN`.

## Case workflow enhancement: response playbook runs

- The case workspace can start one of five guided playbooks and display active/completed runs alongside the case timeline.
- Each run snapshots the playbook slug/version and ordered guidance steps; step status is finalized as COMPLETED or SKIPPED, with a reason required for skipped steps.
- Starting a run and finalizing a step write case timeline entries. Closed/archived cases cannot start new runs, and finalized steps cannot be edited.
- The API and server-rendered forms both call the same service authorization checks. Analyst access requires the linked incident and case assignment/ownership checks to pass.
- Database migrations are additive and legacy case rows remain unassigned to tenants. This is a foundation, not yet tenant isolation. Production rollout still requires tenant-scoped repositories/controllers, a verified legacy ownership map, cross-tenant denial tests, and backup/restore rehearsal.

## Tenant self-service API added on this branch

The branch now also contains a tenant workspace API separate from global /admin/** management:

- GET /api/tenants — list only active tenants for which the authenticated user has active membership.
- GET /api/tenants/{tenantId}/members — list active members (TENANT_ADMIN only).
- POST /api/tenants/{tenantId}/members, PUT /api/tenants/{tenantId}/members/{userId}, DELETE /api/tenants/{tenantId}/members/{userId} — membership administration (TENANT_ADMIN only).
- GET /api/tenants/{tenantId}/policies and PUT /api/tenants/{tenantId}/policies/{type} — policy administration (TENANT_ADMIN only).
- GET /api/tenants/{tenantId}/audit-events and GET /api/tenants/{tenantId}/audit-events/verify — tenant audit access and hash-chain verification (TENANT_ADMIN only).

Each route resolves the authenticated username to the existing user record, verifies the tenant is active, and checks active membership and role. Supplying or guessing a tenant ID is not sufficient. The platform ADMIN endpoints remain separate for initial tenant provisioning. Automated service tests cover missing membership, insufficient role, and suspended-tenant denial.

**Scope limit remains important:** this protects tenant self-service management endpoints only. It does not yet scope legacy incident/case/evidence/download/search/report/notification queries by tenant. Do not treat the product as fully isolated multi-tenant SaaS until those data paths and their tests are complete.

## CI and evidence-storage guard regression tests

- Build/test workflow now runs for pushes to `main` and `feature/**` branches, as well as pull requests targeting `main`.
- Added unit coverage for the storage configuration guard: local storage is accepted for development, while production-required mode rejects local storage, missing S3 bucket configuration, and encryption keys shorter than 32 UTF-8 bytes.
- This validates configuration guard behavior only. It does not prove object-store connectivity, key management, encryption-at-rest configuration, restore capability, or evidence retention controls. Those require staging integration tests and operational verification.
- Latest checked workflow for commit `6b533c818694d09cd8e0c44e0cd6ddf4fc5ce0e6` completed successfully: https://github.com/JMK-YERE/national-cyber-forensics-system/actions/runs/38938032227.

## Remaining release blockers (do not bypass)

- Legacy incident/case/evidence/file/report/notification/search endpoints are not yet consistently tenant-scoped.
- Existing `tenant_id` values remain NULL until an approved ownership mapping exists.
- Migration rehearsal against both empty PostgreSQL and a restored production-like database is still required.
- Do not merge this draft or deploy it to production until the tenant authorization audit, cross-tenant denial tests, evidence-storage staging checks, backup/restore rehearsal, and explicit release approval are complete.

