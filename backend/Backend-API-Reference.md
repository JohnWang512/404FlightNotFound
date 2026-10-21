# Backend - API Reference

## Tech Stack

| Tool | Purpose |
|------|---------|
| Java 17 | Primary language |
| Spring Boot 4.0.3 | Web framework |
| Spring Data JPA | Database ORM |
| MySQL | Relational database |
| Gradle 8 | Build tool |
| FlightAware AeroAPI | External flight data source |

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── BackendApplication.java   # Spring Boot entry point
│   │   │   ├── DemoController.java       # REST controller — /api/** endpoints
│   │   │   ├── AeroApiController.java    # REST controller — /api/aeroapi/** endpoints
│   │   │   └── FlightService.java        # Service layer — proxies requests to AeroAPI
│   │   └── resources/
│   │       └── application.properties    # App config (DB URL, AeroAPI key)
│   └── test/
│       └── java/com/example/demo/
│           ├── BackendApplicationTests.java      # Context load test (no DB)
│           ├── BackendApplicationTestsAgro.java  # Additional context tests
│           └── BackendTests.java                 # MockMvc controller tests
├── db/
│   └── schema.sql        # MySQL baseline schema
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
└── Dockerfile
```

## API Endpoints

### General

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/hello` | Health check — returns a success string |

### Airports & Operators (`DemoController`)

These endpoints are FlightAware AeroAPI proxy endpoints.

| Method | Path | Required client headers | Query/body params (client-side) | Return type |
|--------|------|--------------------------|-----------------------------------|-------------|
| GET | `/api/airports` | none | Any query params forwarded | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/canonical` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/delays` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/flights` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/flights/arrivals` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/flights/counts` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/flights/departures` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/flights/scheduled_arrivals` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/flights/scheduled_departures` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/flights/to/{destId}` | none | `id`, `destId` path variables | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/nearby` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/routes/{destId}` | none | `id`, `destId` path variables | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/weather/forecast` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/{id}/weather/observations` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/airports/delays` | none | Any query params forwarded | `200 OK` with body as JSON string |
| GET | `/api/airports/nearby` | none | Any query params forwarded | `200 OK` with body as JSON string |
| GET | `/api/operators` | none | Any query params forwarded | `200 OK` with body as JSON string |
| GET | `/api/operators/{id}` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/operators/{id}/canonical` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/operators/{id}/flights` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/operators/{id}/flights/arrivals` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/operators/{id}/flights/counts` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/operators/{id}/flights/enroute` | none | `id` path variable | `200 OK` with body as JSON string |
| GET | `/api/operators/{id}/flights/scheduled` | none | `id` path variable | `200 OK` with body as JSON string |

### AeroAPI (`AeroApiController`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/aeroapi/status` | Check if AeroAPI key is configured |

Return type:
- `200 OK` body: JSON object like `{ "configured": true|false }`

Why this endpoint exists:
- It lets the frontend/backend developer quickly see whether `AEROAPI_KEY` was supplied, without needing to trigger a failing proxy request.

All endpoints under `/api/**` allow CORS from `http://localhost:5173` (the Vue dev server).

### Frontend Redirect

| Method | Path | Required client headers | Body params | Return type |
|--------|------|--------------------------|-------------|-------------|
| GET | `/api-explorer` | none | none | `302 Found` with `Location: http://localhost:5173/api-explorer` |

### Where to find the rest (`/api/flights`, `/api/live`, `/api/db`)

This repo keeps the detailed reference for:
- DB CRUD and caching utilities (`/api/db/*`)
- airplanes.live proxies (`/api/live/*`)
- FlightAware AeroAPI proxies (`/api/flights/*`)

in `backend/db/DB_CONTROLLER.md`.

## Database Schema

Defined by `backend/db/schema.sql` (baseline) and evolved at runtime by Hibernate using `spring.jpa.hibernate.ddl-auto=update`.

For deeper DB-controller expectations and endpoint behaviors, see:
- `backend/db/Database-Schema-Initialization.md`
- `backend/db/DB_CONTROLLER.md`

## Configuration

Environment variables used in `application.properties`:

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/flight_tracker_db` | MySQL connection URL |
| `AEROAPI_KEY` | `default-key-not-set` | FlightAware AeroAPI key |

## Getting Started

```bash
# Run the dev server (http://localhost:8080)
./gradlew bootRun

# Run standard tests
./gradlew test

# Run manual/integration tests (tagged 'manual')
./gradlew manualTest
```

Set your AeroAPI key before running:

```bash
export AEROAPI_KEY=your_key_here
./gradlew bootRun
```

## Docker

```bash
docker build -t backend .
docker run -p 8080:8080 -e AEROAPI_KEY=your_key_here backend
```

## Testing

Tests use JUnit 5 + Spring MockMvc. Tests tagged `manual` are excluded from the standard `test` task and must be run with `./gradlew manualTest`.

| File | Type | Description |
|------|------|-------------|
| `BackendApplicationTests.java` | Unit | Verifies the Spring context loads without a DB connection |
| `BackendApplicationTestsAgro.java` | Unit | Additional context load tests |
| `BackendTests.java` | MockMvc | Tests controller endpoints with a mocked `FlightService` |

