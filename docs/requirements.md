# Incubyte Salary Management — Requirements and scope

## Goal

Provide an assessment-scale web application for managing synthetic employee salary records and reviewing salary summaries without combining amounts from different currencies.

## Included scope and current status

| Capability | Status | Current implementation |
|---|---|---|
| Relational persistence | Implemented | Spring Data JPA with a persistent SQLite database. |
| Employee directory | Implemented | Angular UI supports search by code/name/email, server-side pagination, and employee create/edit/delete actions. |
| Employee list API | Implemented | API also supports country, department, salary bounds, and whitelisted sorting; these additional controls are not exposed in the Angular directory. |
| Employee detail | Partial | API lookup by ID exists; there is no UI detail screen. |
| Employee create/edit/delete | Implemented | API write endpoints and Angular management form/actions validate inputs and enforce code/email uniqueness. Write calls require the shared bearer token described below. |
| Salary insights | Implemented | Count, total, average, country and department breakdowns. Each request is currency-scoped; country and department filters apply to all queries. |
| Synthetic seed data | Implemented | Opt-in runner inserts exactly 10,000 deterministic records when the employee table is empty and refuses to change a populated table. Seeded country/currency pairs are fixed by list index. |

## Additional implementation security controls

The shared write-token gate is an implementation safeguard, not a claim that the original assessment required user authentication. `GET /api/employees`, employee lookup by ID, and salary analytics remain public. `POST`, `PUT`, and `DELETE` employee requests require `Authorization: Bearer <token>` matching the backend's `APP_WRITE_API_TOKEN`; an unset backend token rejects writes with HTTP 401. The Angular operator enters the matching token in a masked UI field. It is held in browser memory only and cleared on reload or explicitly in the UI. This is not individual user authentication, identity, or role-based authorization. CORS restricts browser origins but does not protect the API from non-browser clients. Public hosting must use synthetic, non-sensitive employee data only.

## Exclusions and reasons

- **Individual user authentication and authorization:** not implemented. The shared bearer token gates employee writes but does not provide individual accounts, identity, roles, or protection for public reads.
- **Currency conversion:** omitted because no approved exchange-rate source, effective date, or accounting policy is specified. Analytics therefore aggregate within one currency only.
- **Payroll, tax, benefits, and compliance:** omitted because no domain rules or jurisdictional requirements are defined.
- **Audit history, approval workflows, notifications, import/export, and multitenancy:** omitted to keep the assessment scope focused on employee reads and salary insights; they need additional operational and business rules.
- **Production deployment and operations:** not implemented; the current service URLs, CORS, and SQLite setup target local assessment use.

## Constraints and acceptance checks

Employee code and email are unique; required columns are non-null in the database; annual salary is stored as a two-decimal `BigDecimal` with a non-negative check. Write requests validate required fields, email format, supported ISO currency, and salary precision/range; invalid requests return 400, duplicate code/email returns 409, and missing update/delete targets return 404. Seed values are synthetic and use deterministic codes/emails. The local acceptance path is to start backend and Angular using the root README, search and page through employees, create/edit/delete a record, apply currency/country/department analytics filters, and verify zero-match results. Run tests with isolated SQLite as documented; do not use real employee data in a public demo.

This is an as-built scope summary, not a claim that unimplemented features are complete. The original assessment email is not stored in this repository, so confirm any additional acceptance criteria against that source.
