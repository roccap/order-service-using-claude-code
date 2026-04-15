# Order Service

A Spring Boot 4.0.5 microservice for managing customer orders, built with Java 25 and Maven.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.5 |
| Persistence | Spring Data JPA + Hibernate, Liquibase |
| Database | PostgreSQL 18 (prod/staging), H2 (dev) |
| Mapping | MapStruct 1.6.3 |
| Security | Spring Security (stateless HTTP Basic) |
| API Docs | springdoc-openapi 3.0.2 (Swagger UI) |
| Metrics | Micrometer + Prometheus |
| Logging | SLF4J + Logstash encoder (JSON in prod) |
| Testing | JUnit 5, Mockito, Testcontainers |

## Prerequisites

- Java 25+
- Maven 3.9+
- Docker (for containerised run or integration tests)

## Build

```bash
# Compile and run all tests
mvn verify

# Skip tests for a faster build
mvn package -DskipTests
```

## Run Locally (H2 in-memory)

The `dev` profile is active by default and uses an H2 in-memory database — no external dependencies needed.

```bash
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

| URL | Description |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/h2-console | H2 browser console |

## Run with Docker

### Build the image

```bash
docker build -t order-service:latest .
```

### Run against a local PostgreSQL instance

```bash
docker run \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=staging \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=orders \
  -e DB_USER=postgres \
  -e DB_PASSWORD=secret \
  order-service:latest
```

### Docker Compose (quick stack)

```yaml
services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: orders
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"

  order-service:
    image: order-service:latest
    depends_on: [postgres]
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: staging
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: orders
      DB_USER: postgres
      DB_PASSWORD: secret
```

## Environment Variables (staging / prod profiles)

| Variable | Description | Default |
|---|---|---|
| `DB_HOST` | PostgreSQL hostname | — |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | — |
| `DB_USER` | Database username | — |
| `DB_PASSWORD` | Database password | — |
| `SPRING_PROFILES_ACTIVE` | Active profile (`staging` or `prod`) | `dev` |

## API Endpoints

All endpoints are prefixed with `/api/v1/`.

| Method | Path | Description |
|---|---|---|
| `POST` | `/orders` | Create a new order |
| `GET` | `/orders/{id}` | Get order by ID |
| `GET` | `/orders?customerId={id}` | List orders for a customer |
| `GET` | `/orders?status={status}` | List orders by status |
| `PATCH` | `/orders/{id}/status` | Update order status |
| `DELETE` | `/orders/{id}` | Cancel an order |

### Example: Create an order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "items": [
      {
        "productId": "7b9e1c3a-1234-4567-89ab-cdef01234567",
        "productName": "Widget Pro",
        "quantity": 2,
        "unitPrice": 29.99
      }
    ],
    "notes": "Please gift-wrap"
  }'
```

## Order Status Flow

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
   └──────────────────────────────────────────→ CANCELLED
```

A `DELIVERED` order cannot be cancelled.

## Running Tests

```bash
# Unit tests only (no Docker required)
mvn test

# Full suite including Testcontainers integration tests (requires Docker)
mvn verify
```

## Profiles Summary

| Profile | Database | Pool Size | Log Level |
|---|---|---|---|
| `dev` | H2 in-memory | — | DEBUG |
| `staging` | PostgreSQL (env vars) | 10 | INFO |
| `prod` | PostgreSQL (env vars) | 30 | WARN |
