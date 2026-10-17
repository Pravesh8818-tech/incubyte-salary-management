# ACME Salary Management — Requirements (MVP)

**Status:** Proposed for assessment | **Primary user:** HR Manager

## Goal
Replace spreadsheet-based salary tracking with a simple, reliable web application that lets an HR manager maintain and understand salary records for approximately 10,000 employees across multiple countries.

## Scope and features
- **Employee directory:** paginated list, stable sorting, search, and filters for country, department and salary range.
- **Employee records:** view, create, edit and delete employee records with clear validation and error messages.
- **Data integrity:** unique employee code and email; required-field validation; non-negative annual salary stored as a decimal with an explicit ISO-style three-letter currency code.
- **Salary insights:** employee count, total annual salary and average annual salary, grouped/filterable by currency, country and department where supported. Never add values expressed in different currencies.
- **Seed data:** repeatable process to create 10,000 deterministic synthetic employees for demo and performance checks.
- **Quality and handover:** automated tests for core business rules/API behavior, responsive Angular UI, setup instructions, design notes and incremental Git commits.

## Deliberately out of scope (and why)
- **Authentication/authorization:** not specified in the brief and would expand MVP scope. The demo must not be used with real employee data until access control is implemented.
- **Payroll execution, payslips, tax, benefits and country-specific compliance:** no business rules or legal requirements were provided.
- **Currency conversion:** no approved exchange-rate source, effective date or accounting policy was supplied; analytics remain currency-specific.
- **Bulk import/export, audit history, approval workflows and notifications:** valuable follow-up features, but not required to prove the core end-to-end workflow.
- **Multi-tenant organization support:** the brief describes one organization (ACME), so tenant isolation is not part of this MVP.

## Success / acceptance criteria
1. HR can list, search, sort, filter and paginate the employee directory without loading all records into the browser.
2. HR can view, create, edit and delete a record; invalid input and duplicate identifiers produce actionable errors.
3. A fresh local database can be seeded with exactly 10,000 synthetic employees; rerunning the seed process does not create duplicates.
4. Salary summaries use a selected currency and correctly report employee count, total and average.
5. Automated tests cover validation, business rules, API behavior and salary aggregation; the frontend builds and its core user flows are tested.
6. A reviewer can run the project using the README, inspect meaningful incremental commits, and follow a short demo.

## Assumptions and constraints
- Employee code and email are unique; salary is annual, non-negative and represented to two decimal places.
- Currency is stored per employee. Currency conversion is not implied.
- SQLite is selected for low setup friction and the assessment/demo workload of 10,000 records; concurrency and deployment limitations are documented.
- All seeded people and salary figures are synthetic. This MVP is an assessment demonstration, not a production HR system.
