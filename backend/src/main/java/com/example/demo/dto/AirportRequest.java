package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for creating or updating an Airport.
 *
 * Why a DTO:
 * - Rejects invalid airport codes (e.g., "L@X" or "12345") before they reach the database.
 *
 * Airport-code format:
 * - IATA codes are 3 uppercase letters (e.g., LAX, JFK).
 * - ICAO codes are 4 uppercase letters (e.g., KLAX, EGLL).
 * - This app uses ICAO codes internally but accepts both to be flexible.
 */
public record AirportRequest(
        @NotBlank(message = "Airport code must not be blank")
        @Pattern(
                regexp = "^[A-Z]{3,4}$",
                message = "Airport code must be 3 or 4 uppercase letters (e.g., LAX or KLAX)"
        )
        String apCode,

        String name,
        String city,
        String country,
        Double lat,
        Double lon
) {}
