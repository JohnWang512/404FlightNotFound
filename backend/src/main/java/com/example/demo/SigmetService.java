package com.example.demo;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SigmetService {

    private final RestTemplate restTemplate = new RestTemplate();
    private String cachedSigmets = "{\"type\":\"FeatureCollection\",\"features\":[]}";

    // Fetch from NOAA every 5 minutes (300,000 ms)
    @Scheduled(fixedRate = 300000)
    public void fetchGlobalStorms() {
        try {
            // Official NOAA Aviation Weather API (Free, GeoJSON format)
            String url = "https://aviationweather.gov/api/data/isigmet?format=geojson";
            String result = restTemplate.getForObject(url, String.class);
            if (result != null) {
                cachedSigmets = result;
                System.out.println("✅ Successfully updated Global SIGMET Storm Data");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch SIGMETs: " + e.getMessage());
        }
    }

    public String getCachedSigmets() {
        return cachedSigmets;
    }
}