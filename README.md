# kitchensink-springboot

Migration of the [JBoss EAP kitchensink quickstart](https://github.com/jboss-developer/jboss-eap-quickstarts/tree/master/kitchensink) to Spring Boot 3.5.0 + Java 21 + MongoDB.

## What was migrated

| Java EE (original) | Spring Boot (migrated) |
|---|---|
| JSF / Facelets | REST API (Thymeleaf UI optional) |
| JAX-RS `@GET` / `@POST` | Spring MVC `@RestController` |
| EJB `@Stateless` | `@Service` |
| JPA `@Entity` + H2 | `@Document` + MongoDB |
| CDI `@Inject` | Constructor injection |
| Bean Validation (`@NotNull`, `@Email`) | Same API, `spring-boot-starter-validation` |
| Arquillian integration tests | JUnit 5 + real MongoDB |

## Prerequisites

- Java 21
- Docker Desktop

## Run locally

```bash
docker compose up --build
```

The app starts on `http://localhost:8080/kitchensink`.

### API

```bash
# List all members
curl http://localhost:8080/kitchensink/rest/members

# Register a member
curl -X POST http://localhost:8080/kitchensink/rest/members \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","phoneNumber":"1234567890"}'

# Validation — invalid email returns 400
curl -X POST http://localhost:8080/kitchensink/rest/members \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"not-valid","phoneNumber":"1234567890"}'

# Duplicate email returns 409
curl -X POST http://localhost:8080/kitchensink/rest/members \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","phoneNumber":"1234567890"}'
```

## Run tests

Tests require MongoDB running on `localhost:27017`. Start it first:

```bash
docker run -d --name mongodb-test -p 27017:27017 mongo:7.0
```

Then run:

```bash
./mvnw test
```

Stop the test database when done:

```bash
docker stop mongodb-test && docker rm mongodb-test
```

> **Note on Testcontainers:** The ideal approach for CI is to use Testcontainers so each test run starts its own isolated MongoDB container automatically. This requires Docker Desktop's Java client to work correctly. On some macOS configurations (particularly with "Use containerd for pulling and storing images" enabled), the Java Docker client is incompatible with Docker Desktop's socket proxy. See `docs/migration-notes.md` for details.

## Build JAR

```bash
./mvnw package -DskipTests
java -jar target/kitchensink-*.jar
```

Requires MongoDB on `localhost:27017`, or override:

```bash
SPRING_DATA_MONGODB_URI=mongodb://<host>:27017/kitchensink java -jar target/kitchensink-*.jar
```

## Cloud deployment

The app is a standard Spring Boot JAR configured entirely via the `SPRING_DATA_MONGODB_URI` environment variable. It deploys to any platform that supports Docker or JVM runtimes:

| Platform | Steps |
|---|---|
| **Railway** | Connect repo → set `SPRING_DATA_MONGODB_URI` to a MongoDB Atlas URI → deploy |
| **Render** | Use Docker runtime → add `SPRING_DATA_MONGODB_URI` env var |
| **MongoDB Atlas** | Free M0 cluster provides the connection string |

## Migration notes

See [docs/migration-notes.md](docs/migration-notes.md) for key decisions, trade-offs, and what a larger-scale migration would look like.
