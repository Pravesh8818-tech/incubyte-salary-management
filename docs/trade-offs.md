# Trade-offs and limitations

This document describes design choices visible in the current implementation and the limits they create.

## Local SQLite persistence

SQLite keeps local assessment setup simple and stores data in `${user.home}/salary-management.db` by default. It avoids the need to install a separate database server, but the project does not provide production-grade backup and recovery, coordination for concurrent writers, deployment volume management, or scale testing. Treat this as a single-machine assessment and demo database. Hibernate uses `ddl-auto=update`; the repository does not include a versioned schema migration system.

## Currency-safe analytics

Annual salary is stored as a `BigDecimal` with two decimal places. Salary analytics require one currency and aggregate only employee records in that currency. Country and department filters are applied to the summary and both grouped breakdowns. Currency conversion is not implemented because the project has no exchange-rate source, effective-date policy, or conversion rules. The seed generator chooses country and currency independently, so the application does not define or enforce a one-currency-per-country relationship.

## Seed safeguards and synthetic data

Seeding is disabled by default; the runner is registered only when `app.seed.enabled=true`. Before inserting records, the seed service checks that the employee table is empty. If any employee already exists, the run stops without changing the existing rows. Against an empty database, the service inserts exactly 10,000 deterministic synthetic records in batches. A partially populated database is preserved and reported as an error rather than topped up. Run the seed command only against a fresh, empty database; do not delete a populated developer database to make seeding work.

Seeded names, locations, job titles, email addresses, and salaries are fabricated demo values. They do not represent real employees, compensation bands, or country-specific salary and currency rules.

## Read-only employee workflows

The application provides `GET /api/employees` for paged employee-list reads and `GET /api/employees/{id}` for reads by ID. It does not implement employee creation, editing, or deletion. The employee-list API supports search, pagination, sorting, country and department filters, and salary bounds. The Angular directory exposes search and pagination, but it has no sorting, country or department filters, salary-range filters, or employee detail screen.

## Security and deployment

The application has no authentication or authorization. CORS is configured for the local frontend origin, which does not make the API safe to expose publicly. The repository does not provide TLS setup, secret management, monitoring, production deployment configuration, or an operational runbook. Do not store real employee information in this assessment database.

## Scope exclusions

Payroll execution, taxes, benefits, payslips, currency conversion, audit logs, import and export, approval workflows, multitenancy, and employee identity or access control are not implemented. These capabilities require additional business and operational rules and are outside the current assessment demo.
