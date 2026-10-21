# Research Report

## Spring Boot Implementation Strategy for Flight Tracker Backend

### Summary of Work

I researched Spring Boot implementation approaches relevant to the flight tracker project. This included reviewing recommended project structure, dependency management with Gradle, configuration handling, REST API design patterns, and integration strategies for calling external flight data APIs (such as OpenSky Network or AviationStack). The goal was to establish a clear backend architecture that can serve real-time flight data to the frontend map interface.

---

### Motivation

Our flight tracker project requires a Spring Boot backend to:
- Call external flight tracking APIs (e.g., OpenSky Network, AviationStack, FlightAware)
- Process and transform raw flight data (position, altitude, velocity, callsign)
- Expose REST endpoints to the frontend map (e.g., Leaflet.js or Google Maps)
- Potentially push real-time updates via WebSockets or polling

Before writing any backend code, I needed to understand the standard Spring Boot project structure, how to manage dependencies cleanly, how to handle API keys securely via configuration, and which integration patterns best fit a real-time data use case.

---

### Time Spent

- ~30 minutes reading Spring Boot documentation and reviewing project structure best practices
- ~45 minutes researching Spring RestTemplate vs WebClient for external API calls
- ~30 minutes reviewing configuration management (`application.properties` vs `application.yml`, environment variables, secrets handling)
- ~30 minutes looking into WebSocket and Server-Sent Events (SSE) for real-time data pushing
- ~15 minutes writing this report *(listed as administrative time in WR)*

**Total: ~2.5 hours**

---

### Results

#### 1. Project Structure

The standard recommended Spring Boot project layout follows a layered architecture[^1]:

```
src/
└── main/
    ├── java/com/flighttracker/
    │   ├── FlightTrackerApplication.java   # Entry point
    │   ├── controller/                      # REST controllers
    │   │   └── FlightController.java
    │   ├── service/                         # Business logic
    │   │   └── FlightService.java
    │   ├── client/                          # External API clients
    │   │   └── OpenSkyClient.java
    │   ├── model/                           # Data models / DTOs
    │   │   └── Flight.java
    │   └── config/                          # Config beans
    │       └── AppConfig.java
    └── resources/
        ├── application.yml
        └── application-dev.yml
```

Separating the `client` layer from the `service` layer is especially important here — the client handles raw HTTP calls to the flight API, while the service handles business logic (filtering, transformation, caching).

---

#### 2. Dependency Management

Using **Gradle** (`build.gradle`), the core dependencies needed are[^2]:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.3'
    id 'io.spring.dependency-management' version '1.1.3'
}

dependencies {
    // Spring Web (REST controllers + RestTemplate/WebClient)
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // WebFlux (for WebClient - preferred for external API calls)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    // Configuration metadata support
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'

    // Lombok (reduces boilerplate for models)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

---

#### 3. Configuration Handling

API keys and external URLs should **never** be hardcoded. The recommended pattern is to use `application.yml` with environment variable overrides[^3]:

```yaml
# application.yml
flight-api:
  base-url: https://opensky-network.org/api
  api-key: ${FLIGHT_API_KEY:default-dev-key}

server:
  port: 8080
```

Then bind these to a config class using `@ConfigurationProperties`:

```java
@Configuration
@ConfigurationProperties(prefix = "flight-api")
@Data  // Lombok
public class FlightApiConfig {
    private String baseUrl;
    private String apiKey;
}
```

This approach keeps secrets out of source control and supports easy environment-specific overrides (e.g., dev vs. prod).

---

#### 4. External API Integration — WebClient vs RestTemplate

Spring's older `RestTemplate` is in maintenance mode. The recommended approach for new projects is **WebClient** from Spring WebFlux, even for synchronous use cases[^4]:

```java
@Service
public class OpenSkyClient {

    private final WebClient webClient;

    public OpenSkyClient(FlightApiConfig config) {
        this.webClient = WebClient.builder()
            .baseUrl(config.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public List<Flight> getFlightsInBoundingBox(double minLat, double maxLat,
                                                 double minLon, double maxLon) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/states/all")
                .queryParam("lamin", minLat)
                .queryParam("lamax", maxLat)
                .queryParam("lomin", minLon)
                .queryParam("lomax", maxLon)
                .build())
            .retrieve()
            .bodyToMono(OpenSkyResponse.class)
            .map(OpenSkyResponse::toFlightList)
            .block(); // or return Mono<> for reactive pipeline
    }
}
```

For the flight tracker, querying by bounding box (the map viewport) is the key pattern — we only fetch planes visible in the current map view.

---

#### 5. REST Controller Design

The frontend map will poll the backend (or receive pushed updates). A clean controller looks like:

```java
@RestController
@RequestMapping("/api/flights")
@CrossOrigin(origins = "*")  // Allow frontend dev server
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public ResponseEntity<List<Flight>> getFlights(
        @RequestParam double minLat,
        @RequestParam double maxLat,
        @RequestParam double minLon,
        @RequestParam double maxLon
    ) {
        return ResponseEntity.ok(flightService.getFlightsInView(minLat, maxLat, minLon, maxLon));
    }
}
```

`@CrossOrigin` is important during development when the frontend runs on a different port.

---

#### 6. Real-Time Update Strategy

For showing live plane positions on the map, there are two main options:

**Option A — Frontend Polling (simpler)**
The frontend calls `GET /api/flights` every 5–10 seconds. Easy to implement, works well for moderate update rates.

**Option B — Server-Sent Events (SSE)**
Spring supports SSE via `SseEmitter`, which pushes updates from server to client without repeated HTTP requests[^5]. More efficient for high-frequency updates.

For our use case, **polling every 5–10 seconds is the recommended starting point** since the external flight APIs themselves typically update at that cadence. SSE can be added later if needed.

---

#### 7. Key Takeaways for the Team

- Use the layered structure: `controller → service → client`
- Use `WebClient` (not `RestTemplate`) for external API calls
- Store API keys in `application.yml` with environment variable fallback — never commit secrets
- Start with REST polling from the frontend; add SSE only if needed
- Use `@ConfigurationProperties` for clean, type-safe config binding
- Enable `@CrossOrigin` on controllers during development

---

### Sources

- Spring Boot Reference Documentation[^1]
- Spring Boot Starter Dependencies[^2]
- Spring Boot Externalized Configuration[^3]
- Spring WebClient Documentation[^4]
- Spring Server-Sent Events Guide[^5]
- OpenSky Network REST API[^6]

[^1]: https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.structuring-your-code
[^2]: https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters
[^3]: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config
[^4]: https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client
[^5]: https://spring.io/guides/gs/async-method/
[^6]: https://opensky-network.org/apidoc/rest.html