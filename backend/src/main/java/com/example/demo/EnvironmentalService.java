package com.example.demo;

import com.example.demo.entity.Airport;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Environmental impact calculations for private jet flights.
 *
 * Why this service exists:
 * - The project spec (README class diagram) calls for a dedicated EnvironmentalService that can
 *   calculate a carbon footprint given a flight distance and compare it against commercial travel.
 * - Keeping this logic isolated makes it easy to swap fuel-burn constants or add aircraft-type
 *   coefficients later without touching the persistence layer.
 *
 * Emission model:
 * - Average private jet fuel burn: ~3 L/km (typical mid-size jet, e.g. Gulfstream G450).
 * - Jet-A density: ~0.8 kg/L.
 * - CO2 per kg of jet fuel burned: ~3.16 kg (ICAO standard).
 * - Combined: 3 * 0.8 * 3.16 ≈ 7.6 kg CO2 per km.
 *
 * Commercial baseline (for comparison):
 * - ICAO average narrow-body CO2: ~89 g per passenger per km (~0.089 kg/pax/km).
 * - Private jet assumed occupancy: 8 passengers.
 */
@Service
public class EnvironmentalService {

    static final double FUEL_RATE_KG_CO2_PER_KM = 7.6;
    static final int PRIVATE_JET_CAPACITY = 8;
    static final double COMMERCIAL_CO2_KG_PER_PAX_PER_KM = 0.089;

    /**
     * Calculate the estimated CO2 output in kg for a private jet flight.
     *
     * @param distanceKm great-circle distance in kilometres
     * @return CO2 estimate in kg, rounded to two decimal places
     */
    public double calculateCarbonFootprint(double distanceKm) {
        double raw = distanceKm * FUEL_RATE_KG_CO2_PER_KM;
        return Math.round(raw * 100.0) / 100.0;
    }

    /**
     * Compare a private jet flight's per-passenger CO2 against the equivalent commercial route.
     *
     * @param distanceKm great-circle distance in kilometres
     * @return comparison stats as a keyed map
     */
    public Map<String, Object> compareWithCommercial(double distanceKm) {
        double privateTotal = calculateCarbonFootprint(distanceKm);
        double privatePerPax = Math.round((privateTotal / PRIVATE_JET_CAPACITY) * 100.0) / 100.0;
        double commercialPerPax = Math.round(distanceKm * COMMERCIAL_CO2_KG_PER_PAX_PER_KM * 100.0) / 100.0;
        double multiplier = commercialPerPax > 0
                ? Math.round((privatePerPax / commercialPerPax) * 10.0) / 10.0
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("distanceKm", Math.round(distanceKm * 10.0) / 10.0);
        result.put("privateJetCo2Kg", privateTotal);
        result.put("privateJetPerPaxCo2Kg", privatePerPax);
        result.put("commercialPerPaxCo2Kg", commercialPerPax);
        result.put("multiplier", multiplier);
        return result;
    }

    /**
     * Compute the great-circle distance between two coordinates using the Haversine formula.
     *
     * @param lat1 latitude of point 1 in decimal degrees
     * @param lon1 longitude of point 1 in decimal degrees
     * @param lat2 latitude of point 2 in decimal degrees
     * @param lon2 longitude of point 2 in decimal degrees
     * @return distance in kilometres
     */
    public double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Estimate CO2 emitted so far for a live flight using speed and elapsed time.
     *
     * @param velocityKts  ground speed in knots; returns null if zero or null
     * @param departureTime when the flight was first recorded; returns null if null
     * @return estimated kg CO2 emitted so far, or null if inputs are insufficient
     */
    public Double estimateCo2KgFromVelocity(Integer velocityKts, java.time.LocalDateTime departureTime) {
        if (velocityKts == null || velocityKts <= 0 || departureTime == null) return null;
        long seconds = java.time.Duration.between(departureTime, java.time.LocalDateTime.now()).getSeconds();
        if (seconds <= 0) return null;
        double distanceKm = velocityKts * (seconds / 3600.0) * 1.852;
        return calculateCarbonFootprint(distanceKm);
    }

    /**
     * Estimate CO2 for a flight given origin and destination airport entities.
     *
     * Returns {@code null} when either airport is missing or lacks coordinate data.
     *
     * @param origin      origin {@link Airport}, may be null
     * @param destination destination {@link Airport}, may be null
     * @return estimated kg CO2, or null if coordinates are unavailable
     */
    public Double estimateCo2Kg(Airport origin, Airport destination) {
        if (origin == null || destination == null) return null;
        if (origin.getLat() == null || origin.getLon() == null) return null;
        if (destination.getLat() == null || destination.getLon() == null) return null;
        double distanceKm = haversineDistanceKm(
                origin.getLat(), origin.getLon(),
                destination.getLat(), destination.getLon());
        return calculateCarbonFootprint(distanceKm);
    }
}
