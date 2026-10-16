# Incident response playbooks

## Purpose

The staff-only, read-only playbook catalog gives responders a consistent starting workflow for common incidents. Templates are operational guidance, not legal advice and not automatic response actions. Responders must follow the organization's authority matrix, preserve evidence, and obtain the required approval before making containment or recovery changes.

## Available playbooks

- `phishing` — suspicious email, credential harvesting and business email compromise.
- `ransomware` — destructive malware, encryption and backup-plane protection.
- `account-compromise` — compromised user or privileged identity.
- `data-breach` — suspected unauthorized access or exfiltration.
- `malware` — suspicious endpoint behavior and safe malware examination.

Each playbook has a version, severity hint, trigger examples and ordered steps with phase, guidance and required flag. The initial phases are detection/triage, preservation, containment, investigation/eradication, recovery and lessons learned.

## UI and API

- `/response-playbooks` — responsive staff workspace with expandable step-by-step guidance, trigger hints and severity labels.

All routes require an authenticated incident-response staff role: `ROLE_ADMIN`, `ROLE_CYBER_PRO`, `ROLE_FORENSICS` or `ROLE_ANALYST`.

- `GET /api/response-playbooks` — list catalog.
- `GET /api/response-playbooks/{slug}` — read one playbook; unknown slugs return 404.

## Safe operating flow

1. Register and classify the incident; identify an incident lead and severity.
2. Preserve source evidence and logs before destructive remediation when feasible.
3. Record containment decisions, approver, actor and timestamps.
4. Investigate scope and distinguish confirmed facts from hypotheses.
5. Recover from trusted sources and verify the outcome.
6. Link evidence, custody history, IOCs and remediation actions to the case report.
7. Record lessons learned and assign owners/due dates for follow-up work.

## Deliberate limitations

The catalog and case-linked run workflow are implemented on the development branch. The system does not create tasks automatically, execute containment, notify external parties, or claim tenant isolation. Automatic actions require separate approval gates and case-level tenant authorization. Never paste secrets, personal data or raw evidence into playbook metadata.

## Case-linked execution (implemented on the development branch)

- `POST /api/response-playbooks/cases/{caseId}/runs` with JSON `{"playbookSlug":"phishing"}` starts a versioned run and persists a snapshot of its steps.
- `GET /api/response-playbooks/cases/{caseId}/runs` lists runs for an authorized case.
- `GET /api/response-playbooks/runs/{runId}` returns one authorized run.
- `PATCH /api/response-playbooks/runs/{runId}/steps/{stepId}` accepts `{"status":"COMPLETED","notes":"..." }` or `{"status":"SKIPPED","notes":"reason"}`.
- Step completion is persisted and added to the case timeline. Steps must be finalized in order; skipping requires a reason; finalized steps cannot be edited again. A run is marked completed when every step is finalized.
- Authorization is checked server-side on each operation. ADMIN, CYBER_PRO and FORENSICS can manage cases; ANALYST access is limited to cases where they are involved in the underlying incident and are the case creator, assignee or lead investigator. Closed/archived cases cannot start new runs.

The run history is an operational record, not a replacement for a formal evidence chain-of-custody record. Tenant isolation across existing incident/case/evidence APIs remains a separate required release gate.

## Case workspace

The case workspace includes a **Response Playbooks** section. Authorized responders can start a playbook on an open case, record step completion or skip a step with a reason, and see run status and notes. Starting and updating a run creates case timeline events. The run stores the playbook version and a snapshot of step guidance so future catalog edits do not rewrite an existing investigation record.

API routes:

- `GET /api/response-playbooks/cases/{caseId}/runs` — list runs for a case.
- `POST /api/response-playbooks/cases/{caseId}/runs` with `{"playbookSlug":"phishing"}` — start a run.
- `GET /api/response-playbooks/runs/{runId}` — get one authorized run.
- `PATCH /api/response-playbooks/runs/{runId}/steps/{stepId}` with `{"status":"COMPLETED","notes":"..." }` — finalize a pending step. `status` is `COMPLETED` or `SKIPPED`; skipping requires a reason.
- `PATCH /api/response-playbooks/runs/{runId}/cancel` with `{"reason":"Duplicate incident"}` — cancel an active run; a 5–1000 character reason is required and the cancellation is recorded in the case timeline.

The case workspace provides equivalent CSRF-protected form actions. Closed or archived cases cannot start new runs; finalized steps cannot be edited; runs are marked complete when every step is finalized. Optimistic-lock version columns on runs and steps prevent concurrent responders from silently overwriting state; callers should handle conflict responses and refresh the run. Access checks require incident-response staff and, for analysts, access to both the underlying incident and case.

## Operational rollout

- Flyway baseline is configured at version 0 for existing installations so the additive V1/V2 migrations can be applied to an existing non-empty database. Verify the target database and take a restorable backup before enabling migrations in any production environment.
- Legacy records retain nullable tenant ownership; do not interpret the tenant foundation as complete isolation.
- This release does not automatically assign step owners or deadlines and does not execute containment actions. Those require explicit approval workflows and stronger tenant-scoped authorization.
