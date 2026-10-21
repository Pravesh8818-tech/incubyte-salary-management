# Architecture diagrams

These diagrams represent current request flows, not proposed employee-write behavior.

## 1. Implemented system context

```mermaid
flowchart LR
    HR[Reviewer / HR user] --> UI[Angular 21.2 UI]
    UI -->|HTTP JSON, localhost:8080| API[Spring Boot REST API]
    API --> EC[Employee controller]
    API --> AC[Salary analytics controller]
    EC --> ES[Employee service / specifications]
    AC --> AS[Salary analytics service]
    ES --> ER[Spring Data JPA repository]
    AS --> ER
    ER --> DB[(SQLite file)]
```

The UI currently reads the employee directory and salary insights. The API also has a single-employee read route. No employee-write route is implemented.

## 2. Implemented employee directory read

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular directory
    participant API as GET /api/employees
    participant Service as Employee service
    participant DB as SQLite
    User->>UI: Search or change page
    UI->>API: page, size, optional search
    API->>Service: Validate request / build specification
    Service->>DB: Query one page and count
    DB-->>Service: Page of employee rows
    Service-->>API: Employee page DTO
    API-->>UI: Content and pagination metadata
    UI-->>User: Render rows and page controls
```

The endpoint supports additional country/department/salary filters and sorting parameters. Those controls are not currently exposed in the directory UI.

## 3. Implemented salary analytics read

```mermaid
sequenceDiagram
    actor User
    participant UI as Angular Salary Insights
    participant API as Salary analytics API
    participant Service as Analytics service
    participant DB as SQLite
    UI->>API: GET /currencies and GET /filters
    API->>DB: DISTINCT currency, country, department
    DB-->>API: Available values
    API-->>UI: Dropdown options
    User->>UI: Select currency and optional country/department
    UI->>API: GET /api/analytics/salary with filters
    API->>Service: Validate currency
    Service->>DB: Filtered summary and grouped queries
    DB-->>Service: Count and salary aggregates
    Service-->>API: Currency-scoped analytics DTO
    API-->>UI: Summary and country/department groups
    UI-->>User: Render labelled values and empty state if needed
```

**Currency invariant:** each analytics query is scoped to one requested currency. Country and department filters are applied to the summary and both breakdown queries. No conversion is performed.

## 4. Explicit seed startup path

```mermaid
flowchart LR
    Start[Backend startup] --> Prop{app.seed.enabled true?}
    Prop -->|No, default| Normal[Start API without seeding]
    Prop -->|Yes, explicit opt-in| Count{Employee table empty?}
    Count -->|No| Abort[Abort and preserve existing rows]
    Count -->|Yes| Insert[Insert deterministic 10,000 in batches]
    Insert --> Normal
```

## 5. Deployment target sketch (not deployed)

```mermaid
flowchart TB
    Browser[Browser] -->|HTTPS in a configured deployment| Web[Angular assets]
    Web -->|API requests| App[Spring Boot application]
    App --> Persist[(Persistent SQLite file / volume)]
```

This is only a conceptual target. The repository does not include deployment automation, HTTPS configuration, or production operations setup. SQLite concurrency and write-scaling limits should be revisited before serving concurrent production workloads; use persistent storage and backups if deploying the demo.
