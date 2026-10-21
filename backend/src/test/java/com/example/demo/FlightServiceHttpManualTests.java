package com.example.demo;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("manual_http")
class FlightServiceHttpManualTests {

    private FlightService flightService;
    private RestTemplate restTemplate;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        flightService = new FlightService();

        ReflectionTestUtils.setField(flightService, "apiKey", "test-key");

        restTemplate = (RestTemplate) ReflectionTestUtils.getField(flightService, "restTemplate");
        assertThat(restTemplate).isNotNull();

        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    private void expectJsonGet(String url, String responseBody) {
        server.expect(once(), requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-apikey", "test-key"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private void expectJsonGetContains(String... fragments) {
        var matcher = Matchers.allOf(
                java.util.Arrays.stream(fragments)
                        .map(Matchers::containsString)
                        .toArray(org.hamcrest.Matcher[]::new)
        );

        server.expect(once(), requestTo(matcher))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-apikey", "test-key"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));
    }

    @Test
    void getFlightRouteSendsHeadersAndReturnsBody() {
        server.expect(once(), requestTo("https://aeroapi.flightaware.com/aeroapi/flights/AAL123/route"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-apikey", "test-key"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                .andRespond(withSuccess("{\"route\":\"ok\"}", MediaType.APPLICATION_JSON));

        var resp = flightService.getFlightRoute("AAL123");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"route\":\"ok\"}");
        server.verify();
    }

    @Test
    void getFlightByIdentSendsHeadersAndReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/flights/UAL123", "{\"ident\":\"UAL123\"}");

        var resp = flightService.getFlightByIdent("UAL123");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ident\":\"UAL123\"}");
        server.verify();
    }

    @Test
    void searchFlightsBuildsQueryStringAndReturnsBody() {
        server.expect(once(), requestTo(Matchers.allOf(
                        Matchers.containsString("https://aeroapi.flightaware.com/aeroapi/flights/search"),
                        Matchers.containsString("origin=New+York"),
                        Matchers.containsString("destination=San+Jose")
                )))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-apikey", "test-key"))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        var resp = flightService.searchFlights(Map.of(
                "origin", "New York",
                "destination", "San Jose"
        ));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"results\":[]}");
        server.verify();
    }

    @Test
    void searchAdvancedBuildsQueryStringAndReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/flights/search/advanced?query=-origin+KMSN",
                "{\"advanced\":true}");

        var resp = flightService.searchAdvanced(Map.of("query", "-origin KMSN"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"advanced\":true}");
        server.verify();
    }

    @Test
    void searchCountBuildsQueryStringAndReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/flights/search/count?query=-origin+KMSN",
                "{\"count\":12}");

        var resp = flightService.searchCount(Map.of("query", "-origin KMSN"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"count\":12}");
        server.verify();
    }

    @Test
    void searchPositionsBuildsQueryStringAndReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/flights/search/positions?query=-origin+KMSN",
                "{\"positions\":[]}");

        var resp = flightService.searchPositions(Map.of("query", "-origin KMSN"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"positions\":[]}");
        server.verify();
    }

    @Test
    void getFlightMapReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/flights/UAL123/map", "{\"map\":true}");

        var resp = flightService.getFlightMap("UAL123");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"map\":true}");
        server.verify();
    }

    @Test
    void getFlightPositionReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/flights/UAL123/position", "{\"position\":true}");

        var resp = flightService.getFlightPosition("UAL123");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"position\":true}");
        server.verify();
    }

    @Test
    void getFlightTrackReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/flights/UAL123/track", "{\"track\":true}");

        var resp = flightService.getFlightTrack("UAL123");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"track\":true}");
        server.verify();
    }

    @Test
    void getCanonicalFlightReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/flights/UAL123/canonical", "{\"canonical\":\"UAL123\"}");

        var resp = flightService.getCanonicalFlight("UAL123");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"canonical\":\"UAL123\"}");
        server.verify();
    }

    @Test
    void getAirportsBuildsQueryStringAndReturnsBody() {
        expectJsonGetContains(
                "https://aeroapi.flightaware.com/aeroapi/airports",
                "query=Madison"
        );

        var resp = flightService.getAirports(Map.of("query", "Madison"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ok\":true}");
        server.verify();
    }

    @Test
    void getAirportReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/airports/KMSN", "{\"code\":\"KMSN\"}");

        var resp = flightService.getAirport("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"code\":\"KMSN\"}");
        server.verify();
    }

    @Test
    void getAirportCanonicalReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/airports/KMSN/canonical", "{\"canonical\":\"KMSN\"}");

        var resp = flightService.getAirportCanonical("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"canonical\":\"KMSN\"}");
        server.verify();
    }

    @Test
    void getAirportDelaysReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/airports/KMSN/delays", "{\"delays\":[]}");

        var resp = flightService.getAirportDelays("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"delays\":[]}");
        server.verify();
    }

    @Test
    void getAirportFlightsReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/airports/KMSN/flights", "{\"flights\":[]}");

        var resp = flightService.getAirportFlights("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"flights\":[]}");
        server.verify();
    }

    @Test
    void getAirportFlightsArrivalsReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/airports/KMSN/flights/arrivals", "{\"arrivals\":[]}");

        var resp = flightService.getAirportFlightsArrivals("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"arrivals\":[]}");
        server.verify();
    }

    @Test
    void getAirportFlightCountsReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/airports/KMSN/flights/counts", "{\"count\":3}");

        var resp = flightService.getAirportFlightCounts("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"count\":3}");
        server.verify();
    }

    @Test
    void getAirportFlightsDeparturesReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/airports/KMSN/flights/departures", "{\"departures\":[]}");

        var resp = flightService.getAirportFlightsDepartures("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"departures\":[]}");
        server.verify();
    }

    @Test
    void getAirportFlightsScheduledArrivalsReturnsBody() {
        expectJsonGet(
                "https://aeroapi.flightaware.com/aeroapi/airports/KMSN/flights/scheduled_arrivals",
                "{\"scheduledArrivals\":[]}"
        );

        var resp = flightService.getAirportFlightsScheduledArrivals("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"scheduledArrivals\":[]}");
        server.verify();
    }

    @Test
    void getAirportFlightsScheduledDeparturesReturnsBody() {
        expectJsonGet(
                "https://aeroapi.flightaware.com/aeroapi/airports/KMSN/flights/scheduled_departures",
                "{\"scheduledDepartures\":[]}"
        );

        var resp = flightService.getAirportFlightsScheduledDepartures("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"scheduledDepartures\":[]}");
        server.verify();
    }

    @Test
    void getAirportWeatherForecastReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/airports/KMSN/weather/forecast", "{\"forecast\":[]}");

        var resp = flightService.getAirportWeatherForecast("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"forecast\":[]}");
        server.verify();
    }

    @Test
    void getAirportWeatherObservationsReturnsBody() {
        expectJsonGet(
                "https://aeroapi.flightaware.com/aeroapi/airports/KMSN/weather/observations",
                "{\"observations\":[]}"
        );

        var resp = flightService.getAirportWeatherObservations("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"observations\":[]}");
        server.verify();
    }

    @Test
    void getAirportsDelaysBuildsQueryStringAndReturnsBody() {
        expectJsonGetContains(
                "https://aeroapi.flightaware.com/aeroapi/airports/delays",
                "region=midwest"
        );

        var resp = flightService.getAirportsDelays(Map.of("region", "midwest"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ok\":true}");
        server.verify();
    }

    @Test
    void getAirportsNearbyBuildsQueryStringAndReturnsBody() {
        expectJsonGetContains(
                "https://aeroapi.flightaware.com/aeroapi/airports/nearby",
                "lat=43.07",
                "lon=-89.4"
        );

        var resp = flightService.getAirportsNearby(Map.of(
                "lat", "43.07",
                "lon", "-89.4"
        ));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ok\":true}");
        server.verify();
    }

    @Test
    void getAirportNearbyReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/airports/KMSN/nearby", "{\"nearby\":[]}");

        var resp = flightService.getAirportNearby("KMSN");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"nearby\":[]}");
        server.verify();
    }

    @Test
    void getOperatorsBuildsQueryStringAndReturnsBody() {
        expectJsonGetContains(
                "https://aeroapi.flightaware.com/aeroapi/operators",
                "query=United"
        );

        var resp = flightService.getOperators(Map.of("query", "United"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ok\":true}");
        server.verify();
    }

    @Test
    void getOperatorReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/operators/UAL", "{\"operator\":\"UAL\"}");

        var resp = flightService.getOperator("UAL");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"operator\":\"UAL\"}");
        server.verify();
    }

    @Test
    void getOperatorCanonicalReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/operators/UAL/canonical", "{\"canonical\":\"UAL\"}");

        var resp = flightService.getOperatorCanonical("UAL");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"canonical\":\"UAL\"}");
        server.verify();
    }

    @Test
    void getOperatorFlightsReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/operators/UAL/flights", "{\"flights\":[]}");

        var resp = flightService.getOperatorFlights("UAL");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"flights\":[]}");
        server.verify();
    }

    @Test
    void getOperatorFlightsArrivalsReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/operators/UAL/flights/arrivals", "{\"arrivals\":[]}");

        var resp = flightService.getOperatorFlightsArrivals("UAL");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"arrivals\":[]}");
        server.verify();
    }

    @Test
    void getOperatorFlightCountsReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/operators/UAL/flights/counts", "{\"count\":2}");

        var resp = flightService.getOperatorFlightCounts("UAL");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"count\":2}");
        server.verify();
    }

    @Test
    void getOperatorFlightsEnrouteReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/operators/UAL/flights/enroute", "{\"enroute\":[]}");

        var resp = flightService.getOperatorFlightsEnroute("UAL");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"enroute\":[]}");
        server.verify();
    }

    @Test
    void getOperatorFlightsScheduledReturnsBody() {
        expectJsonGet("https://aeroapi.flightaware.com/aeroapi/operators/UAL/flights/scheduled", "{\"scheduled\":[]}");

        var resp = flightService.getOperatorFlightsScheduled("UAL");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"scheduled\":[]}");
        server.verify();
    }

    @Test
    void postFlightIntentsSendsPostAndReturnsBody() {
        server.expect(once(), requestTo("https://aeroapi.flightaware.com/aeroapi/flights/UAL123/intents"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-apikey", "test-key"))
                .andRespond(withSuccess("{\"accepted\":true}", MediaType.APPLICATION_JSON));

        var resp = flightService.postFlightIntents("UAL123", "{\"intent\":\"divert\"}");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"accepted\":true}");
        server.verify();
    }

    @Test
    void getAirportFlightsToBuildsUrlWithBothPathSegments() {
        expectJsonGet(
                "https://aeroapi.flightaware.com/aeroapi/airports/KMSN/flights/to/KORD",
                "{\"flights\":[]}"
        );

        var resp = flightService.getAirportFlightsTo("KMSN", "KORD");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"flights\":[]}");
        server.verify();
    }

    @Test
    void getAirportRoutesBuildsUrlWithBothPathSegments() {
        expectJsonGet(
                "https://aeroapi.flightaware.com/aeroapi/airports/KMSN/routes/KORD",
                "{\"routes\":[]}"
        );

        var resp = flightService.getAirportRoutes("KMSN", "KORD");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"routes\":[]}");
        server.verify();
    }

    @Test
    void makeRequestOnHttpErrorReturns404Payload() {
        server.expect(once(), requestTo("https://aeroapi.flightaware.com/aeroapi/flights/FAIL/route"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"not found\"}"));

        var resp = flightService.getFlightRoute("FAIL");

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        assertThat(resp.getBody()).isNotNull();
        server.verify();
    }
}