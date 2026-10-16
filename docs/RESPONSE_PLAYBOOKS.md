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

The current release is a catalog only. It does not start case runs, create tasks, execute containment, notify external parties, or claim tenant isolation. Those features must be added only after case-level tenant authorization and auditable human approval are implemented. Never paste secrets, personal data or raw evidence into playbook metadata.

## Case-linked execution (implemented on the development branch)

- `POST /api/response-playbooks/cases/{caseId}/runs` with JSON `{"playbookSlug":"phishing"}` starts a versioned run and persists a snapshot of its steps.
- `GET /api/response-playbooks/cases/{caseId}/runs` lists runs for an authorized case.
- `GET /api/response-playbooks/runs/{runId}` returns one authorized run.
- `PATCH /api/response-playbooks/runs/{runId}/steps/{stepId}` accepts `{"status":"COMPLETED","notes":"..." }` or `{"status":"SKIPPED","notes":"reason"}`.
- Step completion is persisted and added to the case timeline. Skipping requires a reason; finalized steps cannot be edited again. A run is marked completed when every step is finalized.
- Authorization is checked server-side on each operation. ADMIN, CYBER_PRO and FORENSICS can manage cases; ANALYST access is limited to cases where they are involved in the underlying incident and are the case creator, assignee or lead investigator. Closed/archived cases cannot start new runs.

The run history is an operational record, not a replacement for a formal evidence chain-of-custody record. Tenant isolation across existing incident/case/evidence APIs remains a separate required release gate.
