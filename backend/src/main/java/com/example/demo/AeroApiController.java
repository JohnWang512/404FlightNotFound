package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/aeroapi")
public class AeroApiController {

    @Value("${aeroapi.key}")
    private String apiKey;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> body = new HashMap<>();
        String cleanedKey = normalizeKey(apiKey);
        boolean configured = cleanedKey != null
                && !cleanedKey.isBlank()
                && !"default-key-not-set".equals(cleanedKey);

        body.put("configured", configured);
        return ResponseEntity.ok(body);
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank() || "default-key-not-set".equals(trimmed)) {
            //.env fails - fb
            return EnvUtils.get("AEROAPI_KEY").orElse(trimmed);
        }
        return trimmed;
    }
}

