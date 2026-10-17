# Planning and Design Notes

## 1. Delivery principles
1. Build the smallest complete vertical slice first: database → API → Angular screen.
2. Prefer readable, boring architecture over premature abstraction.
3. Keep business rules in services/domain validation, not in controllers or UI components.
4. Use synthetic data only; verify behavior with automated tests and repeatable local commands.
5. Make incremental commits so reviewers can see how decisions evolved.

## 2. Proposed implementation sequence

| Phase | Work | Exit check |
|---|---|---|
| 0. Requirements | Confirm MVP, exclusions, assumptions and acceptance criteria | One-page requirements committed before feature work |
| 1. Foundation | Spring Boot app, Angular v22 app, SQLite connection, health/startup check | Both apps start; database file is created persistently |
| 2. Domain and schema | Employee model, request validation, unique constraints, indexes | Valid/invalid data behavior covered by tests |
| 3. Read workflow | List endpoint with pagination, sorting, search and filters; Angular directory | API returns bounded pages and accurate totals |
| 4. Write workflow | View/create/edit/delete endpoints and UI forms | CRUD happy paths and error states work end to end |
| 5. Seed data | Deterministic 10,000-record seeder, batching and safe rerun behavior | Count verified; no duplicate seeding on restart |
| 6. Analytics | Currency-scoped count, total and average; country/department breakdown where supported | Aggregation tests include mixed-currency records |
| 7. Quality | Service/controller tests, frontend tests/build, validation and error handling | Clean test/build results; documented known limitations |
| 8. Handover | README, architecture, trade-offs, AI prompts, deployment and demo | A reviewer can reproduce setup and follow demo steps |

## 3. Domain and data design

**Employee fields:** `id`, `employeeCode`, `fullName`, `email`, `country`, `department`, `jobTitle`, `currency`, `annualSalary`, `createdAt`, `updatedAt`.

- Use a database-generated primary key and unique constraints for employee code and email.
- Store money as a decimal type (`BigDecimal` in Java; suitable decimal/numeric database representation) rather than binary floating point.
- Validate the currency code format and salary range at the API boundary; database constraints should protect key invariants where practical.
- Add indexes only for demonstrated query paths (employee code/email uniqueness and commonly filtered/sorted fields). Confirm actual SQLite query behavior before claiming performance improvements.
- Keep synthetic seed data deterministic so tests and demos are reproducible. Seeding must be opt-in/configurable and must never overwrite real data.

## 4. API design

- `GET /api/employees`: page-based response; accepts `page`, `size`, `sortBy`, `direction`, `search`, `country`, `department`, `minSalary`, `maxSalary`.
- `GET /api/employees/{id}`: one employee or `404`.
- `POST /api/employees`: create; return `201` and created record.
- `PUT /api/employees/{id}`: replace editable employee fields; return updated record or `404`.
- `DELETE /api/employees/{id}`: delete; return `204` or `404`.
- `GET /api/analytics/salary?currency=USD`: summaries limited to one currency per request.

Use consistent JSON errors. Invalid fields/filters should return `400`; missing records should return `404`; uniqueness conflicts should be translated to a useful client error. Whitelist sortable fields rather than interpolating arbitrary client input into queries.

## 5. Testing strategy

- **Unit:** request validation, salary rules, currency-aware aggregation logic and service behavior.
- **Repository/integration:** SQLite persistence, uniqueness constraints, pagination/filter behavior, transactions and error mapping.
- **API:** status codes and response shapes for valid, invalid, duplicate and missing-record cases.
- **Frontend:** form validation, loading/empty/error states, pagination/filter interactions, and API service behavior.
- **Seed/performance sanity:** assert 10,000 records and bounded page responses; record measured results rather than claiming unmeasured performance.

Tests should use isolated temporary/test databases and deterministic fixtures. Avoid relying on production-like local database contents.

## 6. Suggested commit sequence

1. `docs: add MVP requirements and acceptance criteria`
2. `chore: scaffold Spring Boot and Angular applications`
3. `feat: add employee persistence and validation`
4. `feat: add paginated employee search API`
5. `feat: add employee CRUD UI`
6. `feat: seed 10000 synthetic employees safely`
7. `feat: add currency-aware salary analytics`
8. `test: cover employee workflows and salary rules`
9. `docs: document setup, architecture, trade-offs and demo`

Use these as examples; commits must reflect the actual changes made and must not be fabricated retroactively.

## 7. Risks and checks

- **SQLite dialect/configuration:** confirm startup, schema creation and tests with the exact chosen driver/dialect versions.
- **Currency correctness:** test mixed-currency fixtures to prevent cross-currency totals.
- **Data safety:** keep seeding disabled or guarded outside local/demo environments; never use real employee data in the demo.
- **Security:** no authentication is in MVP; do not expose the app publicly with real data. Use environment-specific configuration and HTTPS for deployment.
- **Assessment mismatch:** brief says “AngularJS for Java”; decided to use Modern Angular version 22 (see requirements.md); version stated in README.
