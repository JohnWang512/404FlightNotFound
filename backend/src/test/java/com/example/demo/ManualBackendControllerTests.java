package com.example.demo;

import com.example.demo.entity.Airport;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Celebrity;
import com.example.demo.entity.Flight;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@Tag("manual_backend")
@WebMvcTest({
        DemoController.class,
        AeroApiController.class,
        AirplanesLiveController.class,
        DbController.class,
        FrontendRedirectController.class
})
@TestPropertySource(properties = "aeroapi.key=test-key")
class ManualBackendControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlightService flightService;

    @MockitoBean
    private DbService dbService;

    @MockitoBean
    private AirplanesLiveService airplanesLiveService;

    @Test
    void helloEndpointReturnsExpectedMessage() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Connection Successful: Hello!"));
    }

    @Test
    void flightRouteEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getFlightRoute("UAL123"))
                .thenReturn(ResponseEntity.ok("{\"route\":\"MSN -> ORD\"}"));

        mockMvc.perform(get("/api/flights/UAL123/route"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"route\":\"MSN -> ORD\"}"));
    }

    @Test
    void flightByIdentEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getFlightByIdent("UAL123"))
                .thenReturn(ResponseEntity.ok("{\"ident\":\"UAL123\"}"));

        mockMvc.perform(get("/api/flights/UAL123"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ident\":\"UAL123\"}"));
    }

    @Test
    void canonicalFlightEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getCanonicalFlight("UAL123"))
                .thenReturn(ResponseEntity.ok("{\"canonical\":\"UAL123\"}"));

        mockMvc.perform(get("/api/flights/UAL123/canonical"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"canonical\":\"UAL123\"}"));
    }

    @Test
    void flightPositionEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getFlightPosition("UAL123"))
                .thenReturn(ResponseEntity.ok("{\"position\":\"ok\"}"));

        mockMvc.perform(get("/api/flights/UAL123/position"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"position\":\"ok\"}"));
    }

    @Test
    void flightTrackEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getFlightTrack("UAL123"))
                .thenReturn(ResponseEntity.ok("{\"track\":\"ok\"}"));

        mockMvc.perform(get("/api/flights/UAL123/track"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"track\":\"ok\"}"));
    }

    @Test
    void flightMapEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getFlightMap("UAL123"))
                .thenReturn(ResponseEntity.ok("{\"map\":\"ok\"}"));

        mockMvc.perform(get("/api/flights/UAL123/map"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"map\":\"ok\"}"));
    }

    @Test
    void flightSearchEndpointPassesQueryParameters() throws Exception {
        when(flightService.searchFlights(any()))
                .thenReturn(ResponseEntity.ok("{\"results\":[]}"));

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "MSN")
                        .param("destination", "ORD"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"results\":[]}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(flightService).searchFlights(captor.capture());
        assertThat(captor.getValue())
                .containsEntry("origin", "MSN")
                .containsEntry("destination", "ORD");
    }

    @Test
    void advancedSearchEndpointPassesQueryParameters() throws Exception {
        when(flightService.searchAdvanced(any()))
                .thenReturn(ResponseEntity.ok("{\"advanced\":true}"));

        mockMvc.perform(get("/api/flights/search/advanced")
                        .param("query", "-origin KMSN"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"advanced\":true}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(flightService).searchAdvanced(captor.capture());
        assertThat(captor.getValue()).containsEntry("query", "-origin KMSN");
    }

    @Test
    void searchCountEndpointPassesQueryParameters() throws Exception {
        when(flightService.searchCount(any()))
                .thenReturn(ResponseEntity.ok("{\"count\":12}"));

        mockMvc.perform(get("/api/flights/search/count")
                        .param("query", "-origin KMSN"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"count\":12}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(flightService).searchCount(captor.capture());
        assertThat(captor.getValue()).containsEntry("query", "-origin KMSN");
    }

    @Test
    void searchPositionsEndpointPassesQueryParameters() throws Exception {
        when(flightService.searchPositions(any()))
                .thenReturn(ResponseEntity.ok("{\"positions\":[]}"));

        mockMvc.perform(get("/api/flights/search/positions")
                        .param("query", "-origin KMSN"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"positions\":[]}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(flightService).searchPositions(captor.capture());
        assertThat(captor.getValue()).containsEntry("query", "-origin KMSN");
    }

    @Test
    void airportsEndpointPassesQueryParameters() throws Exception {
        when(flightService.getAirports(any()))
                .thenReturn(ResponseEntity.ok("{\"airports\":[]}"));

        mockMvc.perform(get("/api/airports")
                        .param("query", "Madison"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"airports\":[]}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(flightService).getAirports(captor.capture());
        assertThat(captor.getValue()).containsEntry("query", "Madison");
    }

    @Test
    void airportByCodeEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirport("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"code\":\"KMSN\"}"));

        mockMvc.perform(get("/api/airports/KMSN"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\":\"KMSN\"}"));
    }

    @Test
    void airportCanonicalEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportCanonical("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"canonical\":\"KMSN\"}"));

        mockMvc.perform(get("/api/airports/KMSN/canonical"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"canonical\":\"KMSN\"}"));
    }

    @Test
    void airportDelaysEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportDelays("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"delays\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/delays"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"delays\":[]}"));
    }

    @Test
    void airportFlightsEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportFlights("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"flights\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/flights"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"flights\":[]}"));
    }

    @Test
    void airportFlightsArrivalsEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportFlightsArrivals("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"arrivals\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/flights/arrivals"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"arrivals\":[]}"));
    }

    @Test
    void airportFlightCountsEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportFlightCounts("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"count\":3}"));

        mockMvc.perform(get("/api/airports/KMSN/flights/counts"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"count\":3}"));
    }

    @Test
    void airportFlightsDeparturesEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportFlightsDepartures("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"departures\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/flights/departures"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"departures\":[]}"));
    }

    @Test
    void airportScheduledArrivalsEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportFlightsScheduledArrivals("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"scheduledArrivals\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/flights/scheduled_arrivals"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"scheduledArrivals\":[]}"));
    }

    @Test
    void airportScheduledDeparturesEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportFlightsScheduledDepartures("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"scheduledDepartures\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/flights/scheduled_departures"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"scheduledDepartures\":[]}"));
    }

    @Test
    void airportWeatherForecastEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportWeatherForecast("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"forecast\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/weather/forecast"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"forecast\":[]}"));
    }

    @Test
    void airportWeatherObservationsEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportWeatherObservations("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"observations\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/weather/observations"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"observations\":[]}"));
    }

    @Test
    void airportsDelaysEndpointPassesQueryParameters() throws Exception {
        when(flightService.getAirportsDelays(any()))
                .thenReturn(ResponseEntity.ok("{\"delays\":[]}"));

        mockMvc.perform(get("/api/airports/delays")
                        .param("region", "midwest"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"delays\":[]}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(flightService).getAirportsDelays(captor.capture());
        assertThat(captor.getValue()).containsEntry("region", "midwest");
    }

    @Test
    void airportNearbyEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getAirportNearby("KMSN"))
                .thenReturn(ResponseEntity.ok("{\"nearby\":[\"KORD\"]}"));

        mockMvc.perform(get("/api/airports/KMSN/nearby"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"nearby\":[\"KORD\"]}"));
    }

    @Test
    void airportsNearbyEndpointPassesQueryParameters() throws Exception {
        when(flightService.getAirportsNearby(any()))
                .thenReturn(ResponseEntity.ok("{\"nearby\":[]}"));

        mockMvc.perform(get("/api/airports/nearby")
                        .param("lat", "43.07")
                        .param("lon", "-89.40"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"nearby\":[]}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(flightService).getAirportsNearby(captor.capture());
        assertThat(captor.getValue())
                .containsEntry("lat", "43.07")
                .containsEntry("lon", "-89.40");
    }

    @Test
    void operatorsEndpointPassesQueryParameters() throws Exception {
        when(flightService.getOperators(any()))
                .thenReturn(ResponseEntity.ok("{\"operators\":[]}"));

        mockMvc.perform(get("/api/operators")
                        .param("query", "United"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"operators\":[]}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(flightService).getOperators(captor.capture());
        assertThat(captor.getValue()).containsEntry("query", "United");
    }

    @Test
    void operatorByCodeEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getOperator("UAL"))
                .thenReturn(ResponseEntity.ok("{\"operator\":\"UAL\"}"));

        mockMvc.perform(get("/api/operators/UAL"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"operator\":\"UAL\"}"));
    }

    @Test
    void operatorCanonicalEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getOperatorCanonical("UAL"))
                .thenReturn(ResponseEntity.ok("{\"canonical\":\"UAL\"}"));

        mockMvc.perform(get("/api/operators/UAL/canonical"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"canonical\":\"UAL\"}"));
    }

    @Test
    void operatorFlightsEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getOperatorFlights("UAL"))
                .thenReturn(ResponseEntity.ok("{\"flights\":1}"));

        mockMvc.perform(get("/api/operators/UAL/flights"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"flights\":1}"));
    }

    @Test
    void operatorFlightsArrivalsEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getOperatorFlightsArrivals("UAL"))
                .thenReturn(ResponseEntity.ok("{\"arrivals\":[]}"));

        mockMvc.perform(get("/api/operators/UAL/flights/arrivals"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"arrivals\":[]}"));
    }

    @Test
    void operatorFlightCountsEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getOperatorFlightCounts("UAL"))
                .thenReturn(ResponseEntity.ok("{\"count\":2}"));

        mockMvc.perform(get("/api/operators/UAL/flights/counts"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"count\":2}"));
    }

    @Test
    void operatorFlightsEnrouteEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getOperatorFlightsEnroute("UAL"))
                .thenReturn(ResponseEntity.ok("{\"enroute\":[]}"));

        mockMvc.perform(get("/api/operators/UAL/flights/enroute"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"enroute\":[]}"));
    }

    @Test
    void operatorFlightsScheduledEndpointDelegatesToFlightService() throws Exception {
        when(flightService.getOperatorFlightsScheduled("UAL"))
                .thenReturn(ResponseEntity.ok("{\"scheduled\":[]}"));

        mockMvc.perform(get("/api/operators/UAL/flights/scheduled"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"scheduled\":[]}"));
    }

    @Test
    void postFlightIntentsEndpointDelegatesToFlightService() throws Exception {
        when(flightService.postFlightIntents("UAL123", "{\"intent\":\"divert\"}"))
                .thenReturn(ResponseEntity.ok("{\"accepted\":true}"));

        mockMvc.perform(post("/api/flights/UAL123/intents")
                        .contentType("application/json")
                        .content("{\"intent\":\"divert\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"accepted\":true}"));
    }

    @Test
    void airportFlightsToEndpointPassesBothPathVariables() throws Exception {
        when(flightService.getAirportFlightsTo("KMSN", "KORD"))
                .thenReturn(ResponseEntity.ok("{\"flights\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/flights/to/KORD"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"flights\":[]}"));
    }

    @Test
    void airportRoutesEndpointPassesBothPathVariables() throws Exception {
        when(flightService.getAirportRoutes("KMSN", "KORD"))
                .thenReturn(ResponseEntity.ok("{\"routes\":[]}"));

        mockMvc.perform(get("/api/airports/KMSN/routes/KORD"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"routes\":[]}"));
    }

    @Test
    void aeroApiStatusReportsConfiguredWhenKeyExists() throws Exception {
        mockMvc.perform(get("/api/aeroapi/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    void liveRegEndpointDelegatesToService() throws Exception {
        when(airplanesLiveService.getByReg("N12345"))
                .thenReturn(ResponseEntity.ok("{\"ac\":[{\"reg\":\"N12345\"}]}"));

        mockMvc.perform(get("/api/live/reg/N12345"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ac\":[{\"reg\":\"N12345\"}]}"));
    }

    @Test
    void liveHexEndpointDelegatesToService() throws Exception {
        when(airplanesLiveService.getByHex("A1B2C3"))
                .thenReturn(ResponseEntity.ok("{\"ac\":[{\"hex\":\"A1B2C3\"}]}"));

        mockMvc.perform(get("/api/live/hex/A1B2C3"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ac\":[{\"hex\":\"A1B2C3\"}]}"));
    }

    @Test
    void liveCallsignEndpointDelegatesToService() throws Exception {
        when(airplanesLiveService.getByCallsign("UAL123"))
                .thenReturn(ResponseEntity.ok("{\"ac\":[{\"flight\":\"UAL123\"}]}"));

        mockMvc.perform(get("/api/live/callsign/UAL123"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ac\":[{\"flight\":\"UAL123\"}]}"));
    }

    @Test
    void livePointEndpointPassesPathVariables() throws Exception {
        when(airplanesLiveService.getByPoint(43.07, -89.40, 25))
                .thenReturn(ResponseEntity.ok("{\"ac\":[]}"));

        mockMvc.perform(get("/api/live/point/43.07/-89.40/25"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ac\":[]}"));
    }

    @Test
    void liveMilEndpointDelegatesToService() throws Exception {
        when(airplanesLiveService.getMilitary())
                .thenReturn(ResponseEntity.ok("{\"ac\":[{\"mil\":true}]}"));

        mockMvc.perform(get("/api/live/mil"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ac\":[{\"mil\":true}]}"));
    }

    @Test
    void liveLaddEndpointDelegatesToService() throws Exception {
        when(airplanesLiveService.getLadd())
                .thenReturn(ResponseEntity.ok("{\"ac\":[]}"));

        mockMvc.perform(get("/api/live/ladd"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ac\":[]}"));
    }

    @Test
    void livePiaEndpointDelegatesToService() throws Exception {
        when(airplanesLiveService.getPia())
                .thenReturn(ResponseEntity.ok("{\"ac\":[]}"));

        mockMvc.perform(get("/api/live/pia"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ac\":[]}"));
    }

    @Test
    void dbStatusEndpointReturnsServiceStatusMap() throws Exception {
        when(dbService.getStatus()).thenReturn(Map.of(
                "status", "ok",
                "celebrities", 1,
                "aircraft", 2,
                "airports", 3,
                "flights", 4,
                "users", 5
        ));

        mockMvc.perform(get("/api/db/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.aircraft").value(2))
                .andExpect(jsonPath("$.users").value(5));
    }

    @Test
    void saveCelebrityEndpointAcceptsJsonBody() throws Exception {
        Celebrity saved = new Celebrity();
        saved.setId(7L);
        saved.setName("Taylor Swift");
        saved.setIndustry("Music");

        when(dbService.saveCelebrity(any(Celebrity.class))).thenReturn(saved);

        mockMvc.perform(post("/api/db/celebrities")
                        .contentType("application/json")
                        .content("{\"name\":\"Taylor Swift\",\"industry\":\"Music\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Taylor Swift"));
    }

    @Test
    void getAircraftByIdReturnsNotFoundWhenMissing() throws Exception {
        when(dbService.getAircraftById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/aircraft/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAircraftByIdReturnsEntityWhenPresent() throws Exception {
        var aircraft = new com.example.demo.entity.Aircraft();
        aircraft.setId(99L);
        aircraft.setTailNumber("N12345");

        when(dbService.getAircraftById(99L)).thenReturn(Optional.of(aircraft));

        mockMvc.perform(get("/api/db/aircraft/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.tailNumber").value("N12345"));
    }

    @Test
    void batchFlightSaveEndpointReturnsSummary() throws Exception {
        when(dbService.batchSaveFlights(any())).thenReturn(Map.of(
                "flightsSaved", 2,
                "errors", List.of()
        ));

        mockMvc.perform(post("/api/db/flights/batch")
                        .contentType("application/json")
                        .content("[{\"callsign\":\"N12345\",\"origin\":\"KMSN\",\"destination\":\"KORD\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightsSaved").value(2));
    }

    @Test
    void cachedFlightsEndpointUsesMinutesParameter() throws Exception {
        when(dbService.getRecentCachedFlights(30))
                .thenReturn(Map.of("count", 1, "source", "database"));

        mockMvc.perform(get("/api/db/flights/cache").param("minutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.source").value("database"));
    }

    @Test
    void redirectControllerSendsUsersToFrontendExplorer() throws Exception {
        mockMvc.perform(get("/api-explorer"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:5173/api-explorer"));
    }

    @Test
        void getAllCelebritiesEndpointReturnsList() throws Exception {
        Celebrity a = new Celebrity();
        a.setId(1L);
        a.setName("Taylor Swift");
        Celebrity b = new Celebrity();
        b.setId(2L);
        b.setName("Zendaya");

        when(dbService.getAllCelebrities()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/db/celebrities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Taylor Swift"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Zendaya"));
        }

        @Test
        void getCelebrityByIdReturnsEntityWhenPresent() throws Exception {
        Celebrity celebrity = new Celebrity();
        celebrity.setId(7L);
        celebrity.setName("Taylor Swift");
        celebrity.setIndustry("Music");

        when(dbService.getCelebrityById(7L)).thenReturn(Optional.of(celebrity));

        mockMvc.perform(get("/api/db/celebrities/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Taylor Swift"))
                .andExpect(jsonPath("$.industry").value("Music"));
        }

        @Test
        void getCelebrityByIdReturnsNotFoundWhenMissing() throws Exception {
        when(dbService.getCelebrityById(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/celebrities/7"))
                .andExpect(status().isNotFound());
        }

        @Test
        void deleteCelebrityEndpointReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/db/celebrities/7"))
                .andExpect(status().isNoContent());

        verify(dbService).deleteCelebrity(7L);
        }

        @Test
        void getAllAircraftEndpointReturnsList() throws Exception {
        var a = new com.example.demo.entity.Aircraft();
        a.setId(1L);
        a.setTailNumber("N11111");

        var b = new com.example.demo.entity.Aircraft();
        b.setId(2L);
        b.setTailNumber("N22222");

        when(dbService.getAllAircraft()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/db/aircraft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tailNumber").value("N11111"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].tailNumber").value("N22222"));
        }

        @Test
        void saveAircraftEndpointAcceptsJsonBody() throws Exception {
        var aircraft = new com.example.demo.entity.Aircraft();
        aircraft.setId(5L);
        aircraft.setTailNumber("N55555");

        when(dbService.saveAircraft(any(com.example.demo.entity.Aircraft.class))).thenReturn(aircraft);

        mockMvc.perform(post("/api/db/aircraft")
                        .contentType("application/json")
                        .content("{\"tailNumber\":\"N55555\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.tailNumber").value("N55555"));
        }

        @Test
        void deleteAircraftEndpointReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/db/aircraft/99"))
                .andExpect(status().isNoContent());

        verify(dbService).deleteAircraft(99L);
        }

        @Test
        void getAllAirportsEndpointReturnsList() throws Exception {
        Airport a = new Airport();
        a.setApCode("KMSN");
        a.setName("Madison");
        a.setCity("Madison");
        a.setCountry("USA");

        Airport b = new Airport();
        b.setApCode("KORD");
        b.setName("OHare");
        b.setCity("Chicago");
        b.setCountry("USA");

        when(dbService.getAllAirports()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/db/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apCode").value("KMSN"))
                .andExpect(jsonPath("$[0].name").value("Madison"))
                .andExpect(jsonPath("$[1].apCode").value("KORD"))
                .andExpect(jsonPath("$[1].name").value("OHare"));
        }

        @Test
        void getAirportByCodeReturnsEntityWhenPresent() throws Exception {
        Airport airport = new Airport();
        airport.setApCode("KMSN");
        airport.setName("Madison");
        airport.setCity("Madison");
        airport.setCountry("USA");

        when(dbService.getAirportByCode("KMSN")).thenReturn(Optional.of(airport));

        mockMvc.perform(get("/api/db/airports/KMSN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apCode").value("KMSN"))
                .andExpect(jsonPath("$.name").value("Madison"));
        }

        @Test
        void getAirportByCodeReturnsNotFoundWhenMissing() throws Exception {
        when(dbService.getAirportByCode("KMSN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/airports/KMSN"))
                .andExpect(status().isNotFound());
        }

        @Test
        void saveAirportEndpointAcceptsJsonBody() throws Exception {
        Airport airport = new Airport();
        airport.setApCode("KMSN");
        airport.setName("Madison");
        airport.setCity("Madison");
        airport.setCountry("USA");

        when(dbService.saveAirport(any(Airport.class))).thenReturn(airport);

        mockMvc.perform(post("/api/db/airports")
                        .contentType("application/json")
                        .content("{\"apCode\":\"KMSN\",\"name\":\"Madison\",\"city\":\"Madison\",\"country\":\"USA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apCode").value("KMSN"))
                .andExpect(jsonPath("$.name").value("Madison"));
        }

        @Test
        void deleteAirportEndpointReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/db/airports/KMSN"))
                .andExpect(status().isNoContent());

        verify(dbService).deleteAirport("KMSN");
        }

        @Test
        void getAllFlightsEndpointReturnsList() throws Exception {
        Flight a = new Flight();
        a.setId(1L);
        a.setCallsign("UAL100");

        Flight b = new Flight();
        b.setId(2L);
        b.setCallsign("DAL200");

        when(dbService.getAllFlights()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/db/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].callsign").value("UAL100"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].callsign").value("DAL200"));
        }

        @Test
        void getFlightByIdReturnsEntityWhenPresent() throws Exception {
        Flight flight = new Flight();
        flight.setId(7L);
        flight.setCallsign("UAL700");

        when(dbService.getFlightById(7L)).thenReturn(Optional.of(flight));

        mockMvc.perform(get("/api/db/flights/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.callsign").value("UAL700"));
        }

        @Test
        void getFlightByIdReturnsNotFoundWhenMissing() throws Exception {
        when(dbService.getFlightById(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/flights/7"))
                .andExpect(status().isNotFound());
        }

        @Test
        void saveFlightEndpointAcceptsJsonBody() throws Exception {
        Flight flight = new Flight();
        flight.setId(11L);
        flight.setCallsign("UAL1100");

        when(dbService.saveFlight(any(Flight.class))).thenReturn(flight);

        mockMvc.perform(post("/api/db/flights")
                        .contentType("application/json")
                        .content("{\"callsign\":\"UAL1100\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.callsign").value("UAL1100"));
        }

        @Test
        void deleteFlightEndpointReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/db/flights/11"))
                .andExpect(status().isNoContent());

        verify(dbService).deleteFlight(11L);
        }

        @Test
        void getAllUsersEndpointReturnsList() throws Exception {
        AppUser a = new AppUser();
        a.setUId(1L);
        a.setUname("chance");
        a.setUpass("pw1");

        AppUser b = new AppUser();
        b.setUId(2L);
        b.setUname("spencer");
        b.setUpass("pw2");

        when(dbService.getAllUsers()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/db/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uId").value(1))
                .andExpect(jsonPath("$[0].uname").value("chance"))
                .andExpect(jsonPath("$[1].uId").value(2))
                .andExpect(jsonPath("$[1].uname").value("spencer"));
        }

        @Test
        void getUserByIdReturnsEntityWhenPresent() throws Exception {
        AppUser user = new AppUser();
        user.setUId(5L);
        user.setUname("chance");
        user.setUpass("pw");

        when(dbService.getUserById(5L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/db/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uId").value(5))
                .andExpect(jsonPath("$.uname").value("chance"));
        }

        @Test
        void getUserByIdReturnsNotFoundWhenMissing() throws Exception {
        when(dbService.getUserById(5L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/users/5"))
                .andExpect(status().isNotFound());
        }

        @Test
        void saveUserEndpointAcceptsJsonBody() throws Exception {
        AppUser user = new AppUser();
        user.setUId(5L);
        user.setUname("chance");
        user.setUpass("pw");

        when(dbService.saveUser(any(AppUser.class))).thenReturn(user);

        mockMvc.perform(post("/api/db/users")
                        .contentType("application/json")
                        .content("{\"uname\":\"chance\",\"upass\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uId").value(5))
                .andExpect(jsonPath("$.uname").value("chance"));
        }

        @Test
        void deleteUserEndpointReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/db/users/5"))
                .andExpect(status().isNoContent());

        verify(dbService).deleteUser(5L);
        }

        @Test
        void searchFlightsByTailNumberEndpointReturnsResults() throws Exception {
        Flight flight = new Flight();
        flight.setId(1L);
        flight.setCallsign("N12345");

        when(dbService.searchFlightsByTailNumber("N12345")).thenReturn(List.of(flight));

        mockMvc.perform(get("/api/db/search/flights").param("tailNumber", "N12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].callsign").value("N12345"));
        }

    // --- Live / save / fetch / import endpoints ---

    @Test
    void getLiveCelebritiesEndpointDelegatesToDbService() throws Exception {
        when(dbService.getLiveCelebrities()).thenReturn(List.of(
                Map.of("name", "Taylor Swift", "airborne", true)
        ));

        mockMvc.perform(get("/api/db/celebrities/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Taylor Swift"))
                .andExpect(jsonPath("$[0].airborne").value(true));
    }

    @Test
    void getLiveAircraftEndpointForwardsCategoryParam() throws Exception {
        when(dbService.getLiveAircraft("mil")).thenReturn(List.of(
                Map.of("tailNumber", "N12345", "airborne", true)
        ));

        mockMvc.perform(get("/api/db/aircraft/live").param("category", "mil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tailNumber").value("N12345"));
    }

    @Test
    void getLiveAircraftEndpointWorksWithoutCategoryParam() throws Exception {
        when(dbService.getLiveAircraft(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/db/aircraft/live"))
                .andExpect(status().isOk());
    }

    @Test
    void saveLiveEndpointForwardsTailNumberParam() throws Exception {
        when(dbService.saveLivePosition("N12345")).thenReturn(Map.of("saved", true));

        mockMvc.perform(get("/api/db/save-live").param("tailNumber", "N12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));
    }

    @Test
    void saveLiveAllEndpointDelegatesToDbService() throws Exception {
        when(dbService.saveLiveAll()).thenReturn(Map.of("updated", 3));

        mockMvc.perform(get("/api/db/save-live-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(3));
    }

    @Test
    void fetchAndSaveEndpointForwardsIdentParam() throws Exception {
        when(dbService.fetchAndSaveFlight("UAL123")).thenReturn(Map.of("ident", "UAL123", "saved", true));

        mockMvc.perform(get("/api/db/fetch-save").param("ident", "UAL123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ident").value("UAL123"))
                .andExpect(jsonPath("$.saved").value(true));
    }

    @Test
    void importEndpointDelegatesToDbServiceAndReturnsCounts() throws Exception {
        when(dbService.importJson(any())).thenReturn(Map.of(
                "celebrities", 1,
                "aircraft", 2,
                "airports", 0,
                "flights", 3
        ));

        mockMvc.perform(post("/api/db/import")
                        .contentType("application/json")
                        .content("{\"celebrities\":[{\"name\":\"Elon Musk\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrities").value(1))
                .andExpect(jsonPath("$.flights").value(3));
    }

    // --- CO2 endpoints ---

    @Test
    void flightCo2EndpointDelegatesToDbService() throws Exception {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("flightId", 42L);
        summary.put("estimatedCo2Kg", 4567.89);
        summary.put("comparison", Map.of(
                "distanceKm", 1234.5,
                "multiplier", 10.7
        ));
        when(dbService.getCo2Summary(42L)).thenReturn(summary);

        mockMvc.perform(get("/api/db/flights/42/co2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(42))
                .andExpect(jsonPath("$.estimatedCo2Kg").value(4567.89))
                .andExpect(jsonPath("$.comparison.multiplier").value(10.7));
    }

    @Test
    void flightCo2RecalculateEndpointDelegatesToDbService() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flightId", 42L);
        result.put("estimatedCo2Kg", 4567.89);
        result.put("recalculated", true);
        when(dbService.recalculateCo2(42L)).thenReturn(result);

        mockMvc.perform(post("/api/db/flights/42/co2/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(42))
                .andExpect(jsonPath("$.estimatedCo2Kg").value(4567.89))
                .andExpect(jsonPath("$.recalculated").value(true));
    }

    // ── RateLimitFilter ───────────────────────────────────────────────────────

    @Test
    void rateLimitFilterAllowsFirstRequest() {
        RateLimitFilter filter = new RateLimitFilter();
        assertThat(filter.isOverLimit("1.2.3.4")).isFalse();
    }

    @Test
    void rateLimitFilterAllowsRequestsUpToLimit() {
        RateLimitFilter filter = new RateLimitFilter();
        String ip = "1.2.3.5";
        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS_PER_WINDOW; i++) {
            assertThat(filter.isOverLimit(ip)).isFalse();
        }
    }

    @Test
    void rateLimitFilterBlocksAfterExceedingLimit() {
        RateLimitFilter filter = new RateLimitFilter();
        String ip = "1.2.3.6";
        for (int i = 0; i <= RateLimitFilter.MAX_REQUESTS_PER_WINDOW; i++) {
            filter.isOverLimit(ip);
        }
        assertThat(filter.isOverLimit(ip)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void rateLimitFilterResetsWindowAfterExpiry() {
        RateLimitFilter filter = new RateLimitFilter();
        String ip = "1.2.3.7";

        // Exhaust the window
        for (int i = 0; i <= RateLimitFilter.MAX_REQUESTS_PER_WINDOW; i++) {
            filter.isOverLimit(ip);
        }
        assertThat(filter.isOverLimit(ip)).isTrue();

        // Expire the window by back-dating its start time
        Map<String, long[]> windows =
                (Map<String, long[]>) ReflectionTestUtils.getField(filter, "windows");
        windows.get(ip)[0] = System.currentTimeMillis() - RateLimitFilter.WINDOW_MS - 1;

        // First request after reset must be allowed
        assertThat(filter.isOverLimit(ip)).isFalse();
    }

    @Test
    void rateLimitFilterTracksIpsSeparately() {
        RateLimitFilter filter = new RateLimitFilter();
        // Exhaust limit for one IP
        for (int i = 0; i <= RateLimitFilter.MAX_REQUESTS_PER_WINDOW; i++) {
            filter.isOverLimit("10.0.0.1");
        }
        assertThat(filter.isOverLimit("10.0.0.1")).isTrue();
        // Different IP must still be allowed
        assertThat(filter.isOverLimit("10.0.0.2")).isFalse();
    }

    @Test
    void rateLimitFilterPassesThroughToChainWhenUnderLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("192.168.1.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
    }

    @Test
    void rateLimitFilterReturns429WhenOverLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        String ip = "10.0.0.9";
        for (int i = 0; i <= RateLimitFilter.MAX_REQUESTS_PER_WINDOW; i++) {
            filter.isOverLimit(ip);
        }

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn(ip);

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(429);
        verify(chain, never()).doFilter(any(), any());
        assertThat(sw.toString()).contains("Too many requests");
    }

    @Test
    void rateLimitFilterUsesXForwardedForHeader() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        // X-Forwarded-For present — should use this IP, not remoteAddr
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
        when(req.getRemoteAddr()).thenReturn("10.0.0.1");

        filter.doFilter(req, resp, chain);

        // First call with a fresh IP must pass through
        verify(chain).doFilter(req, resp);
    }

    // ── GlobalExceptionHandler ────────────────────────────────────────────────

    @Test
    void globalExceptionHandlerReturns500ForUnhandledException() throws Exception {
        when(dbService.getStatus()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/db/status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("An unexpected error occurred. Please try again."));
    }

    @Test
    void globalExceptionHandlerReturns400WithSingleFieldError() throws Exception {
        // CelebrityRequest has @NotBlank on name — blank name triggers handleValidation
        mockMvc.perform(post("/api/db/celebrities")
                        .contentType("application/json")
                        .content("{\"name\":\"\",\"industry\":\"Music\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void globalExceptionHandlerReturns400WithMultipleFieldErrors() {
        // Test the list branch directly: build a mock with 2 field errors
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "field1", "must not be blank"),
                new FieldError("obj", "field2", "must not be blank")
        ));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> result =
                new GlobalExceptionHandler().handleValidation(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().get("error")).isInstanceOf(List.class);
    }

    // ── WebConfig ─────────────────────────────────────────────────────────────

    @Test
    void webConfigRegistersRateLimitFilterForApiPaths() {
        FilterRegistrationBean<RateLimitFilter> bean = new WebConfig().rateLimitFilter();

        assertThat(bean.getFilter()).isInstanceOf(RateLimitFilter.class);
        assertThat(bean.getUrlPatterns()).contains("/api/*");
    }

    @Test
    void webConfigSetsFilterNameAndOrder() {
        FilterRegistrationBean<RateLimitFilter> bean = new WebConfig().rateLimitFilter();

        assertThat(bean.getFilterName()).isEqualTo("rateLimitFilter");
        assertThat(bean.getOrder()).isEqualTo(1);
    }
}