package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AirplanesLiveService {

    private static final String BASE_URL = "https://api.airplanes.live/v2";

    private final RestTemplate restTemplate = new RestTemplate();

    private ResponseEntity<String> doGet(String path) {
        String url = BASE_URL + path;
        try {
            return restTemplate.getForEntity(url, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(ex.getResponseHeaders())
                    .body(ex.getResponseBodyAsString());
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            return ResponseEntity.status(503).body("{\"error\": \"Service unavailable: " + ex.getMessage() + "\"}");
        }
    }

    // Get aircraft by registration (tail number)
    public ResponseEntity<String> getByReg(String reg) {
        return doGet("/reg/" + reg);
    }

    // Get aircraft by ICAO hex identifier
    public ResponseEntity<String> getByHex(String hex) {
        return doGet("/hex/" + hex);
    }

    // Get aircraft by callsign
    public ResponseEntity<String> getByCallsign(String callsign) {
        return doGet("/callsign/" + callsign);
    }

    // Get aircraft near a geographic point (radius in nautical miles, max 250)
    public ResponseEntity<String> getByPoint(double lat, double lon, int radius) {
        return doGet("/point/" + lat + "/" + lon + "/" + radius);
    }

    // Get military aircraft
    public ResponseEntity<String> getMilitary() {
        return doGet("/mil");
    }

    // Get FAA LADD (Limiting Aircraft Data Displayed) aircraft
    public ResponseEntity<String> getLadd() {
        return doGet("/ladd");
    }

    // Get PIA (Privacy ICAO Address) aircraft
    public ResponseEntity<String> getPia() {
        return doGet("/pia");
    }
}
