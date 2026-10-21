# Incubyte Salary Management

A local assessment/demo application for browsing synthetic employee records and reviewing currency-scoped salary summaries. The backend is a Spring Boot REST API using Spring Data JPA and SQLite. The frontend is an Angular application.

## Implemented scope

- Employee directory with server-side pagination and search by employee code, name, or email.
- Employee detail lookup by database ID through the API.
- Salary Insights with currency selection, dynamically loaded country and department filters, and count, total, average, and country/department breakdowns.
- Opt-in generation of 10,000 deterministic synthetic employees in an empty database.
- Backend and frontend automated tests.

Employee creation, editing, and deletion are not implemented. The Angular directory does not currently expose sorting, country/department, or salary-range filters; the employee-list API accepts those filters and sorting parameters. See [requirements and implementation status](docs/requirements.md) for the full scope assessment.

## Prerequisites

- Java 17 or 21 to run Spring Boot 3.2.0. Maven compiles backend sources for Java 11 bytecode. ([Spring Boot 3.2 requirements](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/getting-started.html))
- Apache Maven 3.6.3 or later. There is no Maven wrapper and no root-level `pom.xml`; run Maven commands from `backend/`.
- Node.js 20.19+, 22.12+, or 24.x, as supported by the Angular 21.2 project. ([Angular compatibility table](https://angular.dev/reference/versions))
- npm 10.x (the package metadata specifies npm 10.8.2).

## Run locally from a clean checkout

Open two PowerShell terminals at the repository root.

### 1. Start the backend

```powershell
Set-Location backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Normal startup does not seed employees.

### 2. Start the frontend

In the second terminal:

```powershell
Set-Location frontend
npm ci
npm start
```

Open `http://localhost:4200`. The frontend calls the backend at `http://localhost:8080`; the API permits the local Angular origin `http://localhost:4200`.

## Database and seed data

SQLite persists to `${user.home}/salary-management.db` (for example, `C:\Users\<username>\salary-management.db` on Windows). The URL can be overridden with Spring's `spring.datasource.url` property. The database is not stored inside this repository, and `.db` files are ignored by Git.

To seed exactly 10,000 deterministic synthetic employees, run the backend once with seeding explicitly enabled. From the repository root:

```powershell
Set-Location backend
mvn spring-boot:run "-Dspring-boot.run.arguments=--app.seed.enabled=true"
```

The configured database must contain zero employees. The seed runner is disabled by default; when enabled it aborts if any employee already exists and reports that no records were changed. It does not clear, replace, or update existing employee data. If the database is already populated, do not run the seed command against it. The successful seed run starts the backend with the inserted records; stop it with `Ctrl+C` when finished. Subsequent starts use the normal command above and do not seed again.

The generator uses deterministic employee codes and email addresses, fixed lists of synthetic names/locations/departments/titles/currencies, and non-negative `BigDecimal` salaries to two decimal places. These are demo records, not real people or payroll data.

## Verify the API and seeded count

With the backend running, request one record:

```text
http://localhost:8080/api/employees?page=0&size=1
```

The paged response includes `totalElements`; after seeding it should equal `10000`. Other implemented routes:

| Route | Purpose |
|---|---|
| `GET /api/employees` | Paged employee listing; accepts `page`, `size`, `search`, `country`, `department`, `minSalary`, `maxSalary`, `sortBy`, and `direction`. Page indices are zero-based and page size is limited to 100. |
| `GET /api/employees/{id}` | Employee lookup by numeric ID; returns 404 when absent. |
| `GET /api/analytics/salary?currency=USD` | Currency-scoped summary and breakdowns; optional exact-match `country` and `department` filters apply to all results. |
| `GET /api/analytics/salary/currencies` | Distinct currencies present in the database. |
| `GET /api/analytics/salary/filters` | Distinct non-empty countries and departments present in the database. |

Analytics require a valid ISO 4217 three-letter currency. The API returns HTTP 400 when currency is missing or invalid. A valid currency/filter combination with no matching employees returns zero totals and empty groups. Monetary totals are computed for one currency per request; currency conversion is not performed.

## Test and build

Run each command from its project directory:

```powershell
Set-Location backend
mvn test "-Dspring.datasource.url=jdbc:sqlite::memory:"
```

```powershell
Set-Location frontend
npm test -- --watch=false
npm run build
```

The Maven property override keeps the Spring Boot context smoke test away from the persistent developer database; repository/integration tests that use JPA also configure in-memory SQLite. Frontend tests and the production build use the checked-in npm lockfile.

## Manual assessment checklist

1. Start the backend and frontend using the commands above.
2. Confirm the directory loads, search by a visible employee name/code, and navigate forward/back through pages.
3. Open `/api/employees?page=0&size=1` and confirm the returned paging metadata; after a seed run, confirm `totalElements` is `10000`.
4. In Salary Insights, choose a currency, then independently select a country and department. Confirm counts and all monetary values update and retain the selected currency label.
5. Select a filter combination with no matches and confirm the UI shows the empty result state.
6. Restart normally and confirm no second seed attempt occurs.

## Limitations and assessment scope

- There is no authentication or authorization. Do not expose this demo to the public internet or use real employee data.
- SQLite is used for a single-machine assessment/demo workload. Its concurrency and write-scaling limits are not addressed here; the project is not a production payroll platform.
- The UI is read-only for employee records. There are no create/edit/delete forms or API operations.
- There is no employee detail screen, currency conversion, payroll execution, audit history, or deployment configuration.
- The directory UI currently provides search and pagination; sorting, country/department filters, and salary-range filters are available only at the employee-list API level.
- No demo video is included in the repository.

## Documentation

See the [documentation index](docs/README.md) for the as-built requirements, planning/design notes, architecture diagrams, trade-offs, and AI-assisted development handover.
