# Migration Notes

## Key decisions

### Spring Boot over Quarkus
Spring Boot is the dominant enterprise Java migration target. For MongoDB's Modernization Factory role it represents the platform most customers migrate *to*. `spring-data-mongodb` is the most mature MongoDB integration in the Java ecosystem and interviewers will recognise the patterns immediately.

### MongoDB replaces H2 / JPA
The relational `members` table maps cleanly to a `members` collection. Key differences worth calling out in a migration:

- **`id` type**: relational uses auto-increment `Long`; MongoDB uses `ObjectId` (represented as `String`) — no sequence table required
- **Unique email**: JPA uses a DDL `UNIQUE` constraint; MongoDB uses `@Indexed(unique=true)` declared on the field and enforced by the driver — same guarantee, different mechanism
- **Queries**: JPQL disappears entirely — Spring Data resolves method names like `findAllByOrderByNameAsc()` to MongoDB queries automatically
- **Index creation**: `auto-index-creation: true` is set in `application.yml` for development; in production indexes should be managed via a migration tool (e.g. [Mongock](https://mongock.io)) to avoid startup overhead and ensure auditability

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
- **Database migration tooling**: Use [Mongock](https://mongock.io) (MongoDB's equivalent of Flyway/Liquibase) to manage index creation and data migrations as versioned, auditable changesets.
- **Parallel run period**: Run both legacy and new system simultaneously for a defined soak period, comparing responses. Any divergence is investigated as a bug.

### At 100× (full enterprise application, multiple teams)

- **Migration runbook per service**: Formal document covering rollback procedure, data validation queries, traffic cutover steps, and rollback criteria — reviewed by the team before any cutover.
- **Shadow mode testing**: Duplicate production traffic to the new service without serving responses. Compare outputs at scale before any real cutover.
- **Observability from day one**: Spring Boot Actuator + structured logging + metrics instrumented from the first phase commit. You need visibility before you start handling production traffic, not after.
- **Feature flags for cutover**: Traffic is shifted gradually (1% → 10% → 50% → 100%) using a feature flag, not a hard cutover. Rollback is a config change.

---

## Lessons learned

1. The Java EE → Spring Boot mapping is largely mechanical for the framework layers (EJB → `@Service`, JAX-RS → `@RestController`, CDI → constructor injection). The non-mechanical part is the **database**: JPA's relational model and MongoDB's document model require genuine design thinking even for a simple entity.

2. Writing contract tests *before* any migration code forces you to understand the *observable behaviour* of the legacy system rather than its intended behaviour. This is the highest-leverage risk-reduction step in any migration.

3. `@Indexed(unique=true)` in Spring Data MongoDB requires `spring.data.mongodb.auto-index-creation=true` in `application.yml` to take effect at startup. It's easy to miss, and the failure mode is silent — the index never gets created, duplicate emails are accepted, and the bug only surfaces in production.

4. Docker Desktop for Mac has a Java client incompatibility with its socket proxy when "Use containerd" mode is enabled. Always verify Testcontainers works in your specific Docker Desktop configuration before committing to it as the CI test infrastructure.
