package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/weather")
public class WeatherController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private SigmetService sigmetService;

    
    @GetMapping
    public ResponseEntity<String> getWeather(@RequestParam double lat, @RequestParam double lng) {
        // UPGRADED API CALL: Now requesting gusts, wind direction, cloud cover, and weather codes (storms)
        String url = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m,weather_code,cloud_cover&temperature_unit=fahrenheit&wind_speed_unit=mph", 
            lat, lng
        );
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\": \"Could not fetch advanced weather\"}");
        }
    }

    @GetMapping("/sigmets")
    public ResponseEntity<String> getSigmets() {
        return ResponseEntity.ok(sigmetService.getCachedSigmets());
    }
}