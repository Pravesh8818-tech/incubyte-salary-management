# Performance considerations

This document describes query and insertion choices visible in the source. The repository contains no benchmark results; none are inferred here.

## Pagination

`GET /api/employees` uses Spring Data page requests and caps page size at 100. Results are ordered by the requested whitelisted field with `id` as a secondary key; the default is `employeeCode`. The page response also includes total counts, which require a count query. Page-number pagination over SQLite may become more expensive for large offsets. The Angular directory uses a page size of 10.

## Search and filters

Search matches employee code, full name, or email after lowercasing both the field and a contains pattern (`%term%`). The leading wildcard and lower-case expression prevent ordinary B-tree indexes from efficiently serving these searches, so the query can scan employee rows. Country and department equality filters and salary bounds are applied in the database. There are no explicit secondary indexes for country, department, currency, salary, or timestamps. The table has its primary-key index and unique constraints on employee code and email, which provide uniqueness indexes.

Add or change indexes only after checking representative query plans and measuring the actual workload. The current 10,000-record assessment data size is not itself evidence of a measured response-time target.

## Salary analytics

Analytics run currency-scoped summary and grouped aggregate queries in SQLite instead of loading all employee rows into the Angular client. Filter-option endpoints use database `DISTINCT` queries. The current endpoint executes separate aggregate queries for the summary, country groups, and department groups. No cache, query-plan measurement, or concurrency benchmark is included.

Salary is a two-decimal `BigDecimal` mapped to SQLite `NUMERIC(10,2)`. SQLite uses dynamic typing and numeric affinity, so analytics precision should be verified against cent-level and large-total fixtures. Current tests exercise cents, half-up average rounding, large sums, and currency scoping; these checks verify specific cases, not every possible aggregate size.

## Seed insertion and SQLite limits

The seed service constructs 10,000 employees in application-side lists of 500, calls `saveAll`, and flushes after each list within one transaction. This bounds the temporary list size, but Hibernate JDBC batch size is not configured. Because the entity uses `GenerationType.IDENTITY`, do not assume the inserts are emitted as efficient JDBC batches. No seed duration or throughput has been measured.

SQLite stores the database in one local file and is intended here for assessment/demo use. Its single-writer behavior and deployment/backup concerns limit suitability for concurrent production workloads. Keep the database on persistent local storage for a demo, and use a separately designed production database strategy if workload requirements grow.
