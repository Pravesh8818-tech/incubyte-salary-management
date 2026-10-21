# Planning and design (as built)

This note records the current design and practical verification path. It is not a claim that every capability in the original assessment idea was delivered. See [requirements and implementation status](requirements.md) for the feature-by-feature state.

## Architecture

- **Backend:** Java / Spring Boot 3.2.0, Spring Web, Spring Data JPA, Hibernate SQLite community dialect, and SQLite JDBC.
- **Frontend:** Angular 21.2 with standalone components, Angular signals, `HttpClient`, and template-driven filter controls.
- **Persistence:** one SQLite file at `${user.home}/salary-management.db` by default; `spring.datasource.url` can override it.
- **API/UI boundary:** Angular calls the API at `http://localhost:8080`; backend CORS permits `http://localhost:4200`.
- **Data:** the `Employee` entity contains an ID, unique employee code/email, name, country, department, job title, currency, annual salary, and timestamps.

The backend compiler emits Java 11 bytecode, while Spring Boot 3.2.0 needs Java 17 or later to run. Angular 21.2 supports Node.js 20.19+, 22.12+, or 24.x. See the official [Spring Boot system requirements](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/getting-started.html#getting-started.system-requirements) and [Angular version compatibility](https://angular.dev/reference/versions).

## Request and data flow

The employee directory requests bounded pages from `GET /api/employees`, and can search employee code, full name, and email. The employee API also accepts country, department, salary range, and sorting parameters, but the Angular directory currently exposes only search and pagination.

Salary Insights loads currencies and distinct country/department options from separate API endpoints, then requests analytics with one currency plus any selected country and department filters. Summary and both breakdowns use the same filters. Monetary values remain in the requested currency; there is no currency conversion.

## Seed workflow

The `EmployeeSeedRunner` only exists when `app.seed.enabled=true`; configuration defaults it to false. The seed service checks that the employee table is empty, generates 10,000 deterministic records, and saves batches of 500 inside a transaction. If employees already exist, the run throws before insertion and leaves the database unchanged. Use the exact command in the root README, and only against an empty assessment/demo database. Normal startup after seeding must omit the opt-in property.

## Verification approach

Commands from their project directories:

```powershell
# Run from backend/; property override keeps the context smoke test isolated.
mvn test "-Dspring.datasource.url=jdbc:sqlite::memory:"
```

```powershell
# frontend/
npm test -- --watch=false
npm run build
```

Repository/seed/analytics integration tests configure in-memory SQLite. The application context smoke test otherwise inherits the normal datasource path, so use the Maven datasource override above to keep the complete suite off the persistent developer database. The normal database should not be cleared as part of setup or testing. Documentation does not claim a benchmark or performance result; the seed count is an explicit data-volume requirement rather than a performance measurement.

## Current gaps

There are no employee write endpoints or create/edit/delete UI, no employee detail view, no authentication/authorization, no currency conversion, and no deployment setup. Directory-level sorting/filter controls and analytics salary-range filters are not part of the current UI/API combination. These gaps are intentional assessment/demo limits as documented, not inferred features.
