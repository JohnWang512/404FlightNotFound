package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173") 
@RequestMapping("/api")
public class DemoController {

    @Autowired
    private FlightService flightService;

    @GetMapping("/hello")
    public String sayHello() {
        return "Connection Successful: Hello!";
    }

    //FLIGHT ENDPOINTS

    /**
     * GET /api/flights/{id}/map - Get flight map data
     */
    @GetMapping("/flights/{id}/map")
    public ResponseEntity<String> getFlightMap(@PathVariable String id) {
        return flightService.getFlightMap(id);
    }

    /**
     * GET /api/flights/{id}/position - Get current flight position
     */
    @GetMapping("/flights/{id}/position")
    public ResponseEntity<String> getFlightPosition(@PathVariable String id) {
        return flightService.getFlightPosition(id);
    }

    /**
     * GET /api/flights/{id}/route - Get flight route
     */
    @GetMapping("/flights/{id}/route")
    public ResponseEntity<String> getFlightRoute(@PathVariable String id) {
        return flightService.getFlightRoute(id);
    }

    /**
     * GET /api/flights/{id}/track - Get flight track
     */
    @GetMapping("/flights/{id}/track")
    public ResponseEntity<String> getFlightTrack(@PathVariable String id) {
        return flightService.getFlightTrack(id);
    }

    /**
     * GET /api/flights/{ident} - Get flight by ident
     */
    @GetMapping("/flights/{ident}")
    public ResponseEntity<String> getFlightByIdent(@PathVariable String ident) {
        return flightService.getFlightByIdent(ident);
    }

    /**
     * GET /api/flights/{ident}/canonical - Get canonical flight info
     */
    @GetMapping("/flights/{ident}/canonical")
    public ResponseEntity<String> getCanonicalFlight(@PathVariable String ident) {
        return flightService.getCanonicalFlight(ident);
    }

    /**
     * POST /api/flights/{ident}/intents - Post flight intents
     */
    @PostMapping("/flights/{ident}/intents")
    public ResponseEntity<String> postFlightIntents(
            @PathVariable String ident,
            @RequestBody String body) {
        return flightService.postFlightIntents(ident, body);
    }

    /**
     * GET /api/flights/search - Search flights with parameters
     */
    @GetMapping("/flights/search")
    public ResponseEntity<String> searchFlights(@RequestParam Map<String, String> params) {
        return flightService.searchFlights(params);
    }

    /**
     * GET /api/flights/search/advanced - Advanced flight search
     */
    @GetMapping("/flights/search/advanced")
    public ResponseEntity<String> searchAdvanced(@RequestParam Map<String, String> params) {
        return flightService.searchAdvanced(params);
    }

    /**
     * GET /api/flights/search/count - Get count of flights in search
     */
    @GetMapping("/flights/search/count")
    public ResponseEntity<String> searchCount(@RequestParam Map<String, String> params) {
        return flightService.searchCount(params);
    }

    /**
     * GET /api/flights/search/positions - Search flight positions
     */
    @GetMapping("/flights/search/positions")
    public ResponseEntity<String> searchPositions(@RequestParam Map<String, String> params) {
        return flightService.searchPositions(params);
    }

    // AIRPORT ENDPOINTS
    //TODO add comments

    @GetMapping("/airports")
    public ResponseEntity<String> getAirports(@RequestParam Map<String, String> params) {
        return flightService.getAirports(params);
    }

    @GetMapping("/airports/{id}")
    public ResponseEntity<String> getAirport(@PathVariable String id) {
        return flightService.getAirport(id);
    }

    @GetMapping("/airports/{id}/canonical")
    public ResponseEntity<String> getAirportCanonical(@PathVariable String id) {
        return flightService.getAirportCanonical(id);
    }

    @GetMapping("/airports/{id}/delays")
    public ResponseEntity<String> getAirportDelays(@PathVariable String id) {
        return flightService.getAirportDelays(id);
    }

    @GetMapping("/airports/{id}/flights")
    public ResponseEntity<String> getAirportFlights(@PathVariable String id) {
        return flightService.getAirportFlights(id);
    }

    @GetMapping("/airports/{id}/flights/arrivals")
    public ResponseEntity<String> getAirportFlightsArrivals(@PathVariable String id) {
        return flightService.getAirportFlightsArrivals(id);
    }

    @GetMapping("/airports/{id}/flights/counts")
    public ResponseEntity<String> getAirportFlightCounts(@PathVariable String id) {
        return flightService.getAirportFlightCounts(id);
    }

    @GetMapping("/airports/{id}/flights/departures")
    public ResponseEntity<String> getAirportFlightsDepartures(@PathVariable String id) {
        return flightService.getAirportFlightsDepartures(id);
    }

    @GetMapping("/airports/{id}/flights/scheduled_arrivals")
    public ResponseEntity<String> getAirportFlightsScheduledArrivals(@PathVariable String id) {
        return flightService.getAirportFlightsScheduledArrivals(id);
    }

    @GetMapping("/airports/{id}/flights/scheduled_departures")
    public ResponseEntity<String> getAirportFlightsScheduledDepartures(@PathVariable String id) {
        return flightService.getAirportFlightsScheduledDepartures(id);
    }

    @GetMapping("/airports/{id}/flights/to/{destId}")
    public ResponseEntity<String> getAirportFlightsTo(
            @PathVariable String id,
            @PathVariable String destId) {
        return flightService.getAirportFlightsTo(id, destId);
    }

    @GetMapping("/airports/{id}/nearby")
    public ResponseEntity<String> getAirportNearby(@PathVariable String id) {
        return flightService.getAirportNearby(id);
    }

    @GetMapping("/airports/{id}/routes/{destId}")
    public ResponseEntity<String> getAirportRoutes(
            @PathVariable String id,
            @PathVariable String destId) {
        return flightService.getAirportRoutes(id, destId);
    }

    @GetMapping("/airports/{id}/weather/forecast")
    public ResponseEntity<String> getAirportWeatherForecast(@PathVariable String id) {
        return flightService.getAirportWeatherForecast(id);
    }

    @GetMapping("/airports/{id}/weather/observations")
    public ResponseEntity<String> getAirportWeatherObservations(@PathVariable String id) {
        return flightService.getAirportWeatherObservations(id);
    }

    @GetMapping("/airports/delays")
    public ResponseEntity<String> getAirportsDelays(@RequestParam Map<String, String> params) {
        return flightService.getAirportsDelays(params);
    }

    @GetMapping("/airports/nearby")
    public ResponseEntity<String> getAirportsNearby(@RequestParam Map<String, String> params) {
        return flightService.getAirportsNearby(params);
    }

    // OPERATOR ENDPOINTS

    @GetMapping("/operators")
    public ResponseEntity<String> getOperators(@RequestParam Map<String, String> params) {
        return flightService.getOperators(params);
    }

    @GetMapping("/operators/{id}")
    public ResponseEntity<String> getOperator(@PathVariable String id) {
        return flightService.getOperator(id);
    }

    @GetMapping("/operators/{id}/canonical")
    public ResponseEntity<String> getOperatorCanonical(@PathVariable String id) {
        return flightService.getOperatorCanonical(id);
    }

    @GetMapping("/operators/{id}/flights")
    public ResponseEntity<String> getOperatorFlights(@PathVariable String id) {
        return flightService.getOperatorFlights(id);
    }

    @GetMapping("/operators/{id}/flights/arrivals")
    public ResponseEntity<String> getOperatorFlightsArrivals(@PathVariable String id) {
        return flightService.getOperatorFlightsArrivals(id);
    }

    @GetMapping("/operators/{id}/flights/counts")
    public ResponseEntity<String> getOperatorFlightCounts(@PathVariable String id) {
        return flightService.getOperatorFlightCounts(id);
    }

    @GetMapping("/operators/{id}/flights/enroute")
    public ResponseEntity<String> getOperatorFlightsEnroute(@PathVariable String id) {
        return flightService.getOperatorFlightsEnroute(id);
    }

    @GetMapping("/operators/{id}/flights/scheduled")
    public ResponseEntity<String> getOperatorFlightsScheduled(@PathVariable String id) {
        return flightService.getOperatorFlightsScheduled(id);
    }
}