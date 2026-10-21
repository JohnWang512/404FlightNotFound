# Research Report

## JUnit Testing Strategy for Flight Tracker Backend

### Summary of Work

I researched how to set up and write JUnit 5 tests for the Spring Boot flight tracker backend. This included reviewing how to test REST controllers, service classes, and external API clients using mocking with Mockito.

---

### Motivation

Our Spring Boot backend needs unit and integration tests to verify that flight data is correctly fetched, transformed, and returned via our REST endpoints. Before writing tests, I needed to understand how JUnit 5 integrates with Spring Boot, how to mock external API calls (so tests don't hit the real flight API), and how to structure tests alongside the existing codebase.

---

### Time Spent

- ~30 minutes reading JUnit 5 and Spring Boot testing documentation
- ~30 minutes reviewing Mockito usage for mocking service/client dependencies
- ~20 minutes looking at `@WebMvcTest` vs `@SpringBootTest` for controller testing
- ~15 minutes writing this report *(listed as administrative time in WR)*

**Total: ~1.5 hours**

---

### Results

#### 1. Dependencies

Spring Boot's test starter includes JUnit 5 and Mockito out of the box — no extra dependencies needed[^1]:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

#### 2. Testing a Service Class

Use `@ExtendWith(MockitoExtension.class)` to unit test service logic by mocking the client layer[^2]:

```java
@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private OpenSkyClient openSkyClient;

    @InjectMocks
    private FlightService flightService;

    @Test
    void shouldReturnFlightsInView() {
        List<Flight> mockFlights = List.of(
            new Flight("DAL452", 34000, 468, 41.87, -87.62)
        );
        when(openSkyClient.getFlightsInBoundingBox(any(), any(), any(), any()))
            .thenReturn(mockFlights);

        List<Flight> result = flightService.getFlightsInView(40.0, 42.0, -89.0, -86.0);

        assertEquals(1, result.size());
        assertEquals("DAL452", result.get(0).getCallsign());
    }
}
```

---

#### 3. Testing a REST Controller

Use `@WebMvcTest` to test only the controller layer without starting the full application[^3]:

```java
@WebMvcTest(FlightController.class)
class FlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FlightService flightService;

    @Test
    void shouldReturnFlights() throws Exception {
        when(flightService.getFlightsInView(any(), any(), any(), any()))
            .thenReturn(List.of(new Flight("UAL910", 38000, 512, 33.94, -118.40)));

        mockMvc.perform(get("/api/flights")
                .param("minLat", "33.0")
                .param("maxLat", "34.0")
                .param("minLon", "-119.0")
                .param("maxLon", "-118.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].callsign").value("UAL910"));
    }
}
```

---

#### 4. Key Takeaways for the Team

- `spring-boot-starter-test` includes everything needed — no extra setup
- Use `@ExtendWith(MockitoExtension.class)` for pure unit tests (service/client layer)
- Use `@WebMvcTest` for controller tests — faster than `@SpringBootTest` since it doesn't load the full context
- Always mock external API clients so tests don't make real HTTP calls
- Tests live in `src/test/java` mirroring the main package structure

---

### Sources

- Spring Boot Testing Documentation[^1]
- JUnit 5 User Guide[^2]
- Spring MVC Test Framework[^3]

[^1]: https://docs.spring.io/spring-boot/docs/current/reference/html/testing.html
[^2]: https://junit.org/junit5/docs/current/user-guide/
[^3]: https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework