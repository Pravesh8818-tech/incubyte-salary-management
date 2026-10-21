# Requirements and implementation status

**Application:** Incubyte Salary Management assessment/demo

**Primary demonstrated user:** HR reviewer

**Status:** As-built scope, checked against the backend and Angular source

## Overview

The application stores employee records in a local SQLite database, exposes a Spring Boot read API, and provides an Angular employee directory and salary insights view. The seed process can create 10,000 deterministic synthetic employees when explicitly enabled against an empty employee table.

## Feature status

| Requirement / capability | Status | What exists |
|---|---|---|
| Employee directory | Implemented | Angular table with server-side paging and a search box. |
| Directory search | Implemented | API and UI search across employee code, full name, and email. |
| Directory paging | Implemented | Zero-based API paging, response metadata, and Previous/Next controls. API page size is at most 100. |
| Employee list filters | Partial | API accepts exact country and department plus min/max annual salary. These filters are not currently exposed in the directory UI. |
| Employee sorting | Partial | API accepts a whitelisted `sortBy` and `direction`; the directory UI does not expose sorting controls. |
| Employee detail | Partial | `GET /api/employees/{id}` exists; there is no detail page in Angular. |
| Employee create/edit/delete | Out of scope / not implemented | No write endpoints or corresponding UI forms exist. The entity's unique database constraints do not constitute implemented CRUD validation workflows. |
| Salary Insights | Implemented | Summary count, total annual salary, average annual salary, and breakdowns by country and department. |
| Currency filtering | Implemented | Currency options come from distinct stored values. Each analytics request is scoped to one currency. |
| Country/department analytics filters | Implemented | Dynamic distinct, non-empty options; exact-match selections are applied to the summary and both breakdowns. |
| Salary minimum/maximum analytics | Out of scope | Salary-range filters apply only to the employee-list API; the analytics endpoint does not accept them. |
| Seed data | Implemented | Explicit opt-in inserts 10,000 deterministic synthetic employees only when the employee table is empty; an existing population aborts without mutation. |
| Authentication/authorization | Out of scope | No identity or access-control layer is configured. |
| Currency conversion/payroll | Out of scope | No conversion, payroll, tax, benefits, or compliance behavior exists. |

## Implemented API contract

- `GET /api/employees?page=0&size=20` returns a Spring Data page. Optional parameters: `search`, `country`, `department`, `minSalary`, `maxSalary`, `sortBy`, and `direction`. Page is zero-based; the controller rejects negative pages, sizes outside 1–100, invalid salary values/ranges, and invalid sort directions. Supported sorting fields are whitelisted in the service.
- `GET /api/employees/{id}` returns the matching employee or HTTP 404.
- `GET /api/analytics/salary?currency=USD` returns the currency, employee count, total and average annual salary, and country/department groups. Optional `country` and `department` exact-match filters apply consistently to the summary and breakdown queries. Missing or unsupported currency receives HTTP 400; zero matches produce zero money values and empty groups.
- `GET /api/analytics/salary/currencies` returns currencies currently in the database.
- `GET /api/analytics/salary/filters` returns distinct non-empty countries and departments currently in the database.

The backend permits browser requests from `http://localhost:4200`. Error bodies for controller validation include timestamp, status, error, and message fields. Employee listing has no API request for writing employee data.

## Data rules evidenced in code

An employee has `id`, `employeeCode`, `fullName`, `email`, `country`, `department`, `jobTitle`, `currency`, `annualSalary`, `createdAt`, and `updatedAt`. Employee code and email are unique and non-null in the table. The salary is a `BigDecimal` column with scale 2 and a SQLite non-negative check. The entity does not declare Jakarta Bean Validation annotations for required text fields, nor a currency-code validator; the analytics controller validates the requested currency using Java's ISO currency registry.

The synthetic data generator creates a unique `EMP` code and unique synthetic email for each of 10,000 records. It uses fixed name, country, department, title, and currency lists and deterministic non-negative two-decimal salaries. Seed country and currency are selected independently from their lists; the application does not encode or enforce a country-to-currency relationship.

## Acceptance checks for the implemented scope

1. Start backend and frontend using the root README commands.
2. Confirm the employee API returns paged data and stable page metadata; a seeded database reports `totalElements: 10000`.
3. Search through the directory and navigate between pages.
4. Request analytics for a single currency, then combine country and department filters; verify summary and both breakdowns reflect the same filter combination.
5. Verify no-match analytics return zero totals and no breakdown groups.
6. Run backend tests, Angular tests, and the Angular production build using the commands in the root README.

These checks describe the current application. They do not assert unimplemented CRUD, directory sorting controls, or directory filter controls.
