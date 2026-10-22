# Trade-offs and limitations

This document describes design choices visible in the current implementation and the limits they create.

## Local SQLite persistence

SQLite keeps local assessment setup simple and stores data in `${user.home}/salary-management.db` by default. It avoids the need to install a separate database server, but the project does not provide production-grade backup and recovery, coordination for concurrent writers, deployment volume management, or scale testing. Treat this as a single-machine assessment and demo database. Hibernate uses `ddl-auto=update`; the repository does not include a versioned schema migration system.

## Currency-safe analytics

Annual salary is stored as a `BigDecimal` with two decimal places. Salary analytics require one currency and aggregate only employee records in that currency. Country and department filters are applied to the summary and both grouped breakdowns. Currency conversion is not implemented because the project has no exchange-rate source, effective-date policy, or conversion rules. In the current seed generator, country and currency arrays use the same index, pairing United States/USD, India/INR, United Kingdom/GBP, Canada/CAD, and Australia/AUD. This is a property of the synthetic seed distribution; the entity and analytics API do not enforce a country-to-currency relationship for other stored records.

## Seed safeguards and synthetic data

Seeding is disabled by default; the runner is registered only when `app.seed.enabled=true`. Before inserting records, the seed service checks that the employee table is empty. If any employee already exists, the run stops without changing the existing rows. Against an empty database, the service inserts exactly 10,000 deterministic synthetic records in batches. A partially populated database is preserved and reported as an error rather than topped up. Run the seed command only against a fresh, empty database; do not delete a populated developer database to make seeding work.

Seeded names, locations, job titles, email addresses, and salaries are fabricated demo values. They do not represent real employees, compensation bands, or country-specific salary and currency rules.

## Employee workflows

The application provides `GET /api/employees` for paged employee-list reads and `GET /api/employees/{id}` for reads by ID. It also supports `POST /api/employees`, `PUT /api/employees/{id}`, and `DELETE /api/employees/{id}` through validated Angular create/edit forms and delete confirmation. The employee-list API supports search, pagination, sorting, country and department filters, and salary bounds. The Angular directory exposes search and pagination, but it has no sorting, country or department filters, salary-range filters, or employee detail screen. Create/update requests exclude generated IDs and timestamps, check employee-code and email uniqueness, and return structured validation/conflict errors.

## Security and deployment

Employee list/by-ID reads and salary analytics are public. Employee POST, PUT, and DELETE requests require `Authorization: Bearer <token>` matching the backend environment variable `APP_WRITE_API_TOKEN`; if it is unset, writes fail closed with HTTP 401. The Angular operator enters the matching token through a masked UI field. The token remains in browser memory, is cleared on reload, and can be cleared explicitly; it is not placed in frontend configuration or browser storage. The interceptor sends it only on matching write methods to the configured API origin and path. This is one shared write token, not individual user authentication, identity, or role-based authorization. CORS restricts browser origins but does not replace authorization. Reads expose employee data, so any public deployment must use synthetic, non-sensitive records. The repository does not provide TLS setup, secret management beyond the backend environment variable, monitoring, production deployment configuration, or an operational runbook.

The public frontend API base URL is configured in `frontend/public/app-config.js`. Local development defaults to `http://localhost:8080/api`; a deployed frontend must use the backend's HTTPS API base URL, including `/api`. This file is public and must never contain credentials. Because it runs in the browser, server-side rendering/prerender does not read this runtime value; configure SSR separately if that mode is deployed.

## Scope exclusions

Payroll execution, taxes, benefits, payslips, currency conversion, audit logs, import and export, approval workflows, multitenancy, and individual employee identity/account management are not implemented. These capabilities require additional business and operational rules and are outside the current assessment demo. The shared write-token gate is a limited implementation control and does not provide those identity features.
