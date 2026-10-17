# Dashboard privacy hardening (2026-10-17)

## Implemented on the feature branch

- Staff dashboard report lists, status metrics, recent reports, map points, and analytics now use the union of reports created by or assigned to the signed-in staff account.
- Duplicate reports are removed by database ID and the result is sorted newest-first.
- Until ReportAttack has an approved legacy-to-tenant ownership mapping and tenant-aware repository queries are used consistently, a privileged role is not treated as permission to read the global legacy report population from the dashboard.
- Global user and protected-report totals are withheld in the staff dashboard rather than displayed as if they belonged to the active tenant. The admin UI explains the limitation.

## Important limitations

This is a temporary fail-closed safety boundary and intentionally narrows operational visibility for ADMIN, CYBER_PRO and FORENSICS accounts. It does not complete tenant isolation in the incident list, protected-report endpoints, PDF routes, or other legacy report screens.

Do not assign existing records to tenants based on role or free-text organization. Approve a deterministic ownership map, add tenant-aware queries and cross-tenant integration tests, and rehearse migrations on a disposable production-like database before release. The draft pull request must remain unmerged and production must remain unchanged until explicit release approval.
