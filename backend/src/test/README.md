# Backend Gradle Commands

All commands should be run from the `backend` folder.

---

## Running Tests

### Default CI/CD Test Suite
```bash
./gradlew test
```
Runs the standard test suite automatically in CI/CD. Includes all tests that are **not** tagged with `manual_backend`, `manual_api`, or `manual_http`:

- **`BackendTests`** — basic controller routing tests (`/api/hello`, `/api/flights/{id}/route`, `/api/aeroapi/status`)
- **`AirplanesLiveServiceTests`** — mocked HTTP tests for all `AirplanesLiveService` endpoints (happy path, 4xx errors, 503 connection failures)
- **`LivePositionSyncServiceTests`** — unit tests for `LivePositionSyncService` (military, LADD, and PIA sync methods)

### Manual Backend Tests
```bash
./gradlew manualBackendTest
```
Runs tests tagged `manual_backend`. These require more setup (Spring context, in-memory H2 database, MockMvc):

- **`DbServiceManualDataJpaTests`** — full JPA layer tests covering CRUD, batch saves, live position sync, and `fetchAndSaveFlight` using an H2 in-memory database
- **`ManualBackendControllerTests`** — full controller tests using `MockMvc` and mocked services, covering every endpoint across `DemoController`, `AeroApiController`, `AirplanesLiveController`, and `DbController`

### Manual HTTP Tests
```bash
./gradlew manualHttpTest
```
Runs tests tagged `manual_http`. These test HTTP behavior using `MockRestServiceServer`:

- **`AirplanesLiveServiceHttpTests`** — extended HTTP tests for `AirplanesLiveService` including edge cases, boundary radius values, and raw body pass-through assertions
- **`FlightServiceHttpManualTests`** — HTTP tests for every `FlightService` endpoint, verifying correct URL construction, headers (`x-apikey`), query string encoding, and error handling

### Live AeroAPI Tests
```bash
export AEROAPI_KEY="your_key_here"
./gradlew manualApiTest
```
Runs tests tagged `manual_api`. Requires a valid `AEROAPI_KEY` environment variable — tests are skipped automatically if the key is missing:

- **`FlightServiceApiManualTests`** — smoke tests that hit the real AeroAPI for `getFlightRoute`, `getFlightByIdent`, and `searchFlights`

### All Tests
```bash
./gradlew allTests
```
Runs the default test suite plus all manual test suites (backend, HTTP, and API).

---

## Compiling

### Compile Test Classes Only
```bash
./gradlew clean testClasses
```
Compiles test classes without executing them. Useful for catching compilation errors quickly.

---

## Code Coverage (JaCoCo)

### Default Coverage Report
```bash
./gradlew test jacocoTestReport
```
Runs the default test suite and generates a JaCoCo coverage report.

### Aggregate Coverage Report (All Tests)
```bash
./gradlew allTests jacocoAllTestsReport
```
Runs all test suites and generates an aggregate JaCoCo coverage report for the entire backend.

```bash
open build/reports/jacoco/jacocoAllTestsReport/html/index.html
```
Open up HTML file to see code coverage

### Default Coverage Gate Check
```bash
./gradlew jacocoTestCoverageVerification
```
Verifies that the default test suite meets the configured JaCoCo coverage threshold. Fails the build if coverage is below the required minimum.

### All-Tests Coverage Gate Check
```bash
./gradlew jacocoAllTestsCoverageVerification
```
Verifies that the full test suite (all manual suites included) meets the configured JaCoCo coverage threshold. Requires 80% minimum coverage, excluding entities, DTOs, config classes, and the main application entry point.
