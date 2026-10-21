package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Collections;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;


@Service
public class FlightService {

    private final RestTemplate restTemplate;
    private final String apiBaseUrl = "https://aeroapi.flightaware.com/aeroapi";

    @Value("${aeroapi.key}")
    private String apiKey;

    private String resolvedApiKey;

    public FlightService() {
        this.restTemplate = new RestTemplate();
    }

    //http header
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", getApiKey());
        headers.set("Accept", "application/json");
        return headers;
    }

    private String getApiKey() {
        if (resolvedApiKey != null) {
            return resolvedApiKey;
        }

        String value = apiKey;
        if (value == null || value.isBlank() || "default-key-not-set".equals(value)) {
            value = EnvUtils.get("AEROAPI_KEY").orElse(value);
        }

        if (value == null) {
            value = "";
        }

        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        resolvedApiKey = trimmed;
        return resolvedApiKey;
    }

    /**
     * GET /flights/{id}/map - Get flight map data
     */
    public ResponseEntity<String> getFlightMap(String id) {
        String url = apiBaseUrl + "/flights/" + id + "/map";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /flights/{id}/position - Get flight position
     */
    public ResponseEntity<String> getFlightPosition(String id) {
        String url = apiBaseUrl + "/flights/" + id + "/position";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /flights/{id}/route - Get flight route
     */
    public ResponseEntity<String> getFlightRoute(String id) {
        String url = apiBaseUrl + "/flights/" + id + "/route";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /flights/{id}/track - Get flight track
     */
    public ResponseEntity<String> getFlightTrack(String id) {
        String url = apiBaseUrl + "/flights/" + id + "/track";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /flights/{ident} - Get flight by ident
     */
    public ResponseEntity<String> getFlightByIdent(String ident) {
        String url = apiBaseUrl + "/flights/" + ident;
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /flights/{ident}/canonical - Get canonical flight
     */
    public ResponseEntity<String> getCanonicalFlight(String ident) {
        String url = apiBaseUrl + "/flights/" + ident + "/canonical";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * POST /flights/{ident}/intents - Post flight intents
     */
    public ResponseEntity<String> postFlightIntents(String ident, String body) {
        String url = apiBaseUrl + "/flights/" + ident + "/intents";
        return makeRequest(url, HttpMethod.POST, body);
    }

    /**
     * GET /flights/search - Search flights
     */
    public ResponseEntity<String> searchFlights(Map<String, String> params) {
        String url = buildUrl(apiBaseUrl + "/flights/search", params);
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /flights/search/advanced - Advanced flight search
     */
    public ResponseEntity<String> searchAdvanced(Map<String, String> params) {
        String url = buildUrl(apiBaseUrl + "/flights/search/advanced", params);
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /flights/search/count - Get flight search count
     */
    public ResponseEntity<String> searchCount(Map<String, String> params) {
        String url = buildUrl(apiBaseUrl + "/flights/search/count", params);
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /flights/search/positions - Get flight search positions
     */
    public ResponseEntity<String> searchPositions(Map<String, String> params) {
    try {
        String rawUrl = apiBaseUrl + "/flights/search/positions";
        if (params != null && !params.isEmpty()) {
            rawUrl += "?" + params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));
        }
        URI uri = new URI(rawUrl);
        HttpHeaders headers = getHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        return restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
    } catch (Exception e) {
        return ResponseEntity.status(500).body("{\"error\": \"" + e.getMessage() + "\"}");
    }
}

    /**
     * GET /airports - Get airports list
     */
    public ResponseEntity<String> getAirports(Map<String, String> params) {
        String url = buildUrl(apiBaseUrl + "/airports", params);
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id} - Get airport by id
     */
    public ResponseEntity<String> getAirport(String id) {
        String url = apiBaseUrl + "/airports/" + id;
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/canonical - Get canonical airport info
     */
    public ResponseEntity<String> getAirportCanonical(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/canonical";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/delays - Get airport delays
     */
    public ResponseEntity<String> getAirportDelays(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/delays";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/flights - Get flights for an airport
     */
    public ResponseEntity<String> getAirportFlights(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/flights";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/flights/arrivals - Get arriving flights for an airport
     */
    public ResponseEntity<String> getAirportFlightsArrivals(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/flights/arrivals";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/flights/counts - Get flight counts for an airport
     */
    public ResponseEntity<String> getAirportFlightCounts(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/flights/counts";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/flights/departures - Get departing flights for an airport
     */
    public ResponseEntity<String> getAirportFlightsDepartures(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/flights/departures";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/flights/scheduled_arrivals - Get scheduled arrivals
     */
    public ResponseEntity<String> getAirportFlightsScheduledArrivals(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/flights/scheduled_arrivals";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/flights/scheduled_departures - Get scheduled departures
     */
    public ResponseEntity<String> getAirportFlightsScheduledDepartures(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/flights/scheduled_departures";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/flights/to/{destId} - Get flights from an airport to a destination
     */
    public ResponseEntity<String> getAirportFlightsTo(String id, String destId) {
        String url = apiBaseUrl + "/airports/" + id + "/flights/to/" + destId;
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/nearby - Get nearby airports
     */
    public ResponseEntity<String> getAirportNearby(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/nearby";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/routes/{destId} - Get routes between airports
     */
    public ResponseEntity<String> getAirportRoutes(String id, String destId) {
        String url = apiBaseUrl + "/airports/" + id + "/routes/" + destId;
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/weather/forecast - Get weather forecast for an airport
     */
    public ResponseEntity<String> getAirportWeatherForecast(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/weather/forecast";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/{id}/weather/observations - Get weather observations for an airport
     */
    public ResponseEntity<String> getAirportWeatherObservations(String id) {
        String url = apiBaseUrl + "/airports/" + id + "/weather/observations";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/delays - Get airport delays
     */
    public ResponseEntity<String> getAirportsDelays(Map<String, String> params) {
        String url = buildUrl(apiBaseUrl + "/airports/delays", params);
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /airports/nearby - Get nearby airports
     */
    public ResponseEntity<String> getAirportsNearby(Map<String, String> params) {
        String url = buildUrl(apiBaseUrl + "/airports/nearby", params);
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /operators - Get operators list
     */
    public ResponseEntity<String> getOperators(Map<String, String> params) {
        String url = buildUrl(apiBaseUrl + "/operators", params);
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /operators/{id} - Get operator by id
     */
    public ResponseEntity<String> getOperator(String id) {
        String url = apiBaseUrl + "/operators/" + id;
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /operators/{id}/canonical - Get canonical operator info
     */
    public ResponseEntity<String> getOperatorCanonical(String id) {
        String url = apiBaseUrl + "/operators/" + id + "/canonical";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /operators/{id}/flights - Get flights for an operator
     */
    public ResponseEntity<String> getOperatorFlights(String id) {
        String url = apiBaseUrl + "/operators/" + id + "/flights";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /operators/{id}/flights/arrivals - Get arriving flights for an operator
     */
    public ResponseEntity<String> getOperatorFlightsArrivals(String id) {
        String url = apiBaseUrl + "/operators/" + id + "/flights/arrivals";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /operators/{id}/flights/counts - Get flight counts for an operator
     */
    public ResponseEntity<String> getOperatorFlightCounts(String id) {
        String url = apiBaseUrl + "/operators/" + id + "/flights/counts";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /operators/{id}/flights/enroute - Get enroute flights for an operator
     */
    public ResponseEntity<String> getOperatorFlightsEnroute(String id) {
        String url = apiBaseUrl + "/operators/" + id + "/flights/enroute";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * GET /operators/{id}/flights/scheduled - Get scheduled flights for an operator
     */
    public ResponseEntity<String> getOperatorFlightsScheduled(String id) {
        String url = apiBaseUrl + "/operators/" + id + "/flights/scheduled";
        return makeRequest(url, HttpMethod.GET, null);
    }

    /**
     * Make HTTP request with authentication header
     */
    private ResponseEntity<String> makeRequest(String url, HttpMethod method, String body) {
        HttpHeaders headers = getHeaders();
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.exchange(url, method, entity, String.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Build URL with query parameters — let UriComponentsBuilder handle encoding once
     */
   private String buildUrl(String baseUrl, Map<String, String> params) {
    if (params == null || params.isEmpty()) {
        return baseUrl;
    }

    StringBuilder sb = new StringBuilder(baseUrl).append("?");
    params.forEach((k, v) -> {
        sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
          .append("=")
          .append(URLEncoder.encode(v, StandardCharsets.UTF_8))
          .append("&");
    });
    // remove trailing &
    sb.setLength(sb.length() - 1);
    return sb.toString();
}
}