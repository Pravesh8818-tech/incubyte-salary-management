# Architecture Diagrams

## 1. System context and request flow

```mermaid
flowchart LR
    HR[HR Manager] --> UI[Angular Web UI]
    UI -->|HTTP / JSON| API[Spring Boot REST API]
    API --> C[Controller + Request Validation]
    C --> S[Service / Business Rules]
    S --> R[Spring Data Repository]
    R --> DB[(SQLite Database File)]
    S --> A[Salary Analytics]
    A --> R
```

The browser talks only to the REST API. Controllers translate HTTP requests, services enforce business rules, and repositories perform persistence. Analytics use currency-scoped queries/calculations.

## 2. Create or update employee sequence

```mermaid
sequenceDiagram
    actor HR as HR Manager
    participant UI as Angular UI
    participant API as Spring Boot API
    participant S as Employee Service
    participant DB as SQLite
    HR->>UI: Submit employee form
    UI->>UI: Client-side validation
    UI->>API: POST or PUT /api/employees
    API->>API: Validate request
    API->>S: Execute business operation
    S->>DB: Check uniqueness / persist record
    DB-->>S: Saved record or constraint error
    S-->>API: Result
    API-->>UI: 201/200 or actionable error
    UI-->>HR: Show saved record or validation message
```

## 3. Salary analytics flow

```mermaid
flowchart TD
    HR[HR selects currency and filters] --> UI[Angular analytics view]
    UI --> API[GET /api/analytics/salary?currency=USD]
    API --> VALIDATE[Validate currency/filter inputs]
    VALIDATE --> QUERY[Query matching employee salaries]
    QUERY --> CALC[Calculate count, total and average]
    CALC --> RESP[Return currency-labelled response]
    RESP --> UI
    UI --> HR
```

**Invariant:** amounts in different currencies must never be summed into one total. Conversion is excluded until a trusted exchange-rate source and conversion date policy are specified.

## 4. Deployment shape (target)

```mermaid
flowchart TB
    Browser[User Browser] --> Web[Deployed Angular Assets]
    Web -->|HTTPS /api| App[Spring Boot Application]
    App --> Persist[(Persistent SQLite Volume / File)]
```

This is a target deployment sketch, not evidence that deployment has already happened. SQLite file storage must be persistent and backed up. For horizontally scaled application instances or sustained concurrent writes, reconsider SQLite and use a server database such as PostgreSQL.
