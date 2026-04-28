# Migration Notes

## Key decisions

### Spring Boot over Quarkus
Spring Boot is the dominant enterprise Java migration target. For MongoDB's Modernization Factory role it represents the platform most customers migrate *to*. `spring-data-mongodb` is the most mature MongoDB integration in the Java ecosystem and interviewers will recognise the patterns immediately.

### MongoDB replaces H2 / JPA
The relational `members` table maps cleanly to a `members` collection. Key differences worth calling out in a migration:

- **`id` type**: relational uses auto-increment `Long`; MongoDB uses `ObjectId` (represented as `String`) — no sequence table required
- **Unique email**: JPA uses a DDL `UNIQUE` constraint; MongoDB enforces uniqueness via an index created by the Mongock migration (`InitMigration`). The `@Indexed(unique=true)` annotation is kept on the field for documentation, but `auto-index-creation` is disabled — Mongock is the sole index authority
- **Queries**: JPQL disappears entirely — Spring Data resolves method names like `findAllByOrderByNameAsc()` to MongoDB queries automatically
- **Index creation**: Managed via [Mongock](https://mongock.io) migrations (`InitMigration`), the MongoDB equivalent of Flyway/Liquibase. `auto-index-creation` is kept `false` in `application.yml` to prevent `IndexOptionsConflict` errors if an index already exists from a previous migration run. Mongock tracks each changeset in a `mongockChangeLog` collection so migrations are auditable, idempotent, and safe to run in CI/CD pipelines

### Contract-test-first approach
Contract tests were written before any Spring Boot code, derived from reading the original kitchensink JAX-RS source. This gave a clear, objective definition of "done" — the migration is complete when all contract tests pass on the new stack.

In a real migration the contract tests would first be run against the *running* legacy system to capture actual behaviour (not just intended behaviour from reading the code). This is the most risk-reducing step: it surfaces hidden edge cases before migration begins.

A subtle example caught here: the original kitchensink returns HTTP `200` (not `201`) for successful member creation. That's not RESTful best practice, but it is the contract — changing it would break existing clients.

### No database mocking in tests
The database layer is never mocked. Tests use a real MongoDB instance (same version as Docker Compose). This is deliberate: mocking MongoDB gives false confidence, particularly around:
- Index enforcement (a mocked repo can't catch `@Indexed(unique=true)` violations)
- Query behaviour (method-name-derived queries are only verified against a real driver)

This is exactly the kind of divergence that causes production incidents in migrations.

### Arquillian replaced by JUnit 5
The original app uses Arquillian, which requires a running JBoss EAP container. Replacing it with JUnit 5 + a real MongoDB removes the application server dependency entirely — tests are faster, simpler, and run in any CI environment without JBoss.

### Known issue: Testcontainers on Docker Desktop for Mac
The ideal CI approach is Testcontainers (each test run starts its own isolated MongoDB container). However, Docker Desktop for Mac with certain settings (specifically "Use containerd for pulling and storing images") exposes a socket proxy that the Java docker-java library cannot connect to, returning HTTP 400 with empty Docker info.

Workaround for local development: start a MongoDB container manually before running tests (`docker run -d -p 27017:27017 mongo:7.0`). The test `application.yml` points to `localhost:27017/kitchensink-test`.

In CI (GitHub Actions, GitLab CI) Testcontainers works correctly because Docker is available via standard Linux sockets.

---

## What I'd do differently at scale

### At 10× the codebase (e.g. a multi-module monolith)

- **Strangler Fig pattern**: Rather than migrating the whole app at once, introduce a reverse proxy in front of the legacy system. Route individual endpoints to the new service as they're migrated. The legacy system stays live throughout; rollback is a routing change.
- **Per-component migration PRs**: Each EJB or JAX-RS resource becomes its own PR with its own contract tests. Reviewable, reversible, independently deployable.
- **CI gate per phase**: Each phase runs its own test subset in CI. A failing gate blocks the next phase from merging — prevents partially-migrated states from accumulating.
- **Database migration tooling**: [Mongock](https://mongock.io) (MongoDB's equivalent of Flyway/Liquibase) is already integrated in this migration for index creation. At larger scale it would also handle data migrations, backfills, and schema evolution as versioned, auditable changesets tracked in a `mongockChangeLog` collection.
- **Parallel run period**: Run both legacy and new system simultaneously for a defined soak period, comparing responses. Any divergence is investigated as a bug.

### At 100× (full enterprise application, multiple teams)

- **Migration runbook per service**: Formal document covering rollback procedure, data validation queries, traffic cutover steps, and rollback criteria — reviewed by the team before any cutover.
- **Shadow mode testing**: Duplicate production traffic to the new service without serving responses. Compare outputs at scale before any real cutover.
- **Observability from day one**: Spring Boot Actuator + structured logging + metrics instrumented from the first phase commit. You need visibility before you start handling production traffic, not after.
- **Feature flags for cutover**: Traffic is shifted gradually (1% → 10% → 50% → 100%) using a feature flag, not a hard cutover. Rollback is a config change.

---

## Deliberate deviations from the original contract

These are places where the migrated API consciously differs from the original. HTTP status codes are preserved exactly; only non-functional details changed.

| Endpoint | Original behaviour | Migrated behaviour | Reason |
|---|---|---|---|
| `POST /rest/members` — duplicate email | `{"email": "Email taken"}` with 409 | `{"error": "Email already registered: <email>"}` with 409 | More informative message; status code (409) is identical and no production code performs string comparison on error message bodies |
| `POST /rest/members` — success | 200 with empty body | 200 with created member JSON | Returning the assigned `id` saves clients a follow-up GET; backwards-compatible because existing clients that ignored the body continue to work |
| `GET /rest/members/{id}` — path constraint | `/{id:[0-9][0-9]*}` (numeric Long only) | `/{id}` (any string) | MongoDB ObjectIds are not numeric; the constraint is structurally impossible to preserve |

---

## Lessons learned

1. The Java EE → Spring Boot mapping is largely mechanical for the framework layers (EJB → `@Service`, JAX-RS → `@RestController`, CDI → constructor injection). The non-mechanical part is the **database**: JPA's relational model and MongoDB's document model require genuine design thinking even for a simple entity.

2. Writing contract tests *before* any migration code forces you to understand the *observable behaviour* of the legacy system rather than its intended behaviour. This is the highest-leverage risk-reduction step in any migration.

3. Index management in MongoDB should use a migration tool like [Mongock](https://mongock.io) rather than relying on `auto-index-creation`. Spring Boot's `auto-index-creation` is convenient locally but unsafe in production: it conflicts with existing indexes and gives no audit trail. Mongock tracks each changeset, is idempotent, and integrates with Spring Boot's application lifecycle exactly as Flyway/Liquibase does for relational databases.

4. Docker Desktop for Mac has a Java client incompatibility with its socket proxy when "Use containerd" mode is enabled. Always verify Testcontainers works in your specific Docker Desktop configuration before committing to it as the CI test infrastructure.
