# incubyte-salary-management

## Seed demo employees

The backend seeds 10,000 deterministic synthetic employees only when explicitly enabled. The target database must be empty; if it already contains employees, the seed run stops without changing them. The normal application startup keeps seeding disabled.

From the repository root, run:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.arguments=--app.seed.enabled=true"
```

Prerequisites: Java 21 and Maven 3.9.6 (the backend compiles to Java 11 bytecode). The command uses the configured persistent SQLite database at `${user.home}/salary-management.db`. Run it once on an empty database, then restart normally to serve the populated directory. Confirm the result with `GET http://localhost:8080/api/employees?page=0&size=1` and check that `totalElements` is `10000`; the Angular directory uses the same API.

## Salary analytics

`GET /api/analytics/salary?currency=USD` returns employee count, total annual salary and average annual salary for one ISO 4217 currency, with country and department breakdowns. Optional exact-match `country` and `department` parameters filter all summary values and breakdowns. Monetary totals are never combined across currencies. Missing or invalid currency codes return HTTP 400; a valid currency with no matching employees returns zero totals and empty breakdown lists. `GET /api/analytics/salary/currencies` lists currencies currently present in the database for the Angular selector.
