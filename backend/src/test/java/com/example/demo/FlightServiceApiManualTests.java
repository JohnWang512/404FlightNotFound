package com.example.demo;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("manual_api")
class FlightServiceApiManualTests {

    private FlightService buildServiceWithEnvKey() {
        String key = System.getenv("AEROAPI_KEY");
        Assumptions.assumeTrue(key != null && !key.isBlank(),
                "Set AEROAPI_KEY env var to run manual API tests");

        FlightService service = new FlightService();
        ReflectionTestUtils.setField(service, "apiKey", key);
        return service;
    }

    // -------------------------------------------------------------------------
    // Flights
    // -------------------------------------------------------------------------

    @Test
    void routeEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getFlightRoute("UAL1");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void identEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getFlightByIdent("UAL1");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void canonicalFlightEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getCanonicalFlight("UAL1");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void flightMapEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getFlightMap("UAL1");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void flightPositionEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getFlightPosition("UAL1");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void flightTrackEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getFlightTrack("UAL1");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Flight Search
    // -------------------------------------------------------------------------

    @Test
    void searchEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.searchFlights(Map.of("origin", "KMSN"));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void searchAdvancedEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.searchAdvanced(Map.of("query", "-origin KMSN"));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void searchCountEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.searchCount(Map.of("query", "-origin KMSN"));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void searchPositionsEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.searchPositions(Map.of("query", "-origin KMSN"));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Airports
    // -------------------------------------------------------------------------

    @Test
    void airportsSearchEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirports(Map.of("query", "Madison"));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportByCodeEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirport("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportCanonicalEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportCanonical("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportDelaysEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportDelays("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportFlightsEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportFlights("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportFlightsArrivalsEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportFlightsArrivals("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportFlightCountsEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportFlightCounts("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportFlightsDeparturesEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportFlightsDepartures("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportScheduledArrivalsEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportFlightsScheduledArrivals("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportScheduledDeparturesEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportFlightsScheduledDepartures("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportWeatherForecastEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportWeatherForecast("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportWeatherObservationsEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportWeatherObservations("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportNearbyEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportNearby("KMSN");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportsDelaysEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportsDelays(Map.of("region", "us-mw"));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void airportsNearbyEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getAirportsNearby(Map.of("lat", "43.07", "lon", "-89.4"));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Operators
    // -------------------------------------------------------------------------

    @Test
    void operatorsSearchEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getOperators(Map.of("query", "United"));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void operatorByCodeEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getOperator("UAL");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void operatorCanonicalEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getOperatorCanonical("UAL");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void operatorFlightsEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getOperatorFlights("UAL");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void operatorFlightsArrivalsEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getOperatorFlightsArrivals("UAL");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void operatorFlightCountsEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getOperatorFlightCounts("UAL");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void operatorFlightsEnrouteEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getOperatorFlightsEnroute("UAL");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void operatorFlightsScheduledEndpointSmokeTest() {
        FlightService service = buildServiceWithEnvKey();

        var response = service.getOperatorFlightsScheduled("UAL");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isBetween(200, 599);
        assertThat(response.getBody()).isNotNull();
    }
}