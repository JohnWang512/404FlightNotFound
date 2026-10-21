package com.example.demo;

import com.example.demo.entity.Airport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link EnvironmentalService}.
 *
 * Why no Spring context:
 * - EnvironmentalService is a pure calculation service with no repository or HTTP dependencies.
 * - Instantiating it directly keeps these tests fast and isolated.
 */
@Tag("manual_backend")
class EnvironmentalServiceTests {

    private EnvironmentalService service;

    @BeforeEach
    void setUp() {
        service = new EnvironmentalService();
    }

    // --- calculateCarbonFootprint ---

    @Test
    void calculateCarbonFootprintReturnsZeroForZeroDistance() {
        assertThat(service.calculateCarbonFootprint(0)).isEqualTo(0.0);
    }

    @Test
    void calculateCarbonFootprintScalesWithDistance() {
        double co2At1000 = service.calculateCarbonFootprint(1000);
        double co2At2000 = service.calculateCarbonFootprint(2000);
        assertThat(co2At2000).isCloseTo(co2At1000 * 2, within(0.01));
    }

    @Test
    void calculateCarbonFootprintUsesExpectedRate() {
        // 1000 km * 7.6 kg/km = 7600 kg
        assertThat(service.calculateCarbonFootprint(1000))
                .isCloseTo(7600.0, within(0.01));
    }

    // --- compareWithCommercial ---

    @Test
    void compareWithCommercialReturnsExpectedKeys() {
        Map<String, Object> result = service.compareWithCommercial(1000);
        assertThat(result).containsKeys(
                "distanceKm", "privateJetCo2Kg",
                "privateJetPerPaxCo2Kg", "commercialPerPaxCo2Kg", "multiplier");
    }

    @Test
    void compareWithCommercialMultiplierIsGreaterThanOne() {
        // Private jet per-pax is always much higher than commercial per-pax
        Map<String, Object> result = service.compareWithCommercial(1000);
        double multiplier = ((Number) result.get("multiplier")).doubleValue();
        assertThat(multiplier).isGreaterThan(1.0);
    }

    @Test
    void compareWithCommercialDistanceMatchesInput() {
        Map<String, Object> result = service.compareWithCommercial(1234.0);
        assertThat(((Number) result.get("distanceKm")).doubleValue())
                .isCloseTo(1234.0, within(0.1));
    }

    @Test
    void compareWithCommercialHandlesZeroDistance() {
        Map<String, Object> result = service.compareWithCommercial(0);
        assertThat(((Number) result.get("privateJetCo2Kg")).doubleValue()).isEqualTo(0.0);
        assertThat(((Number) result.get("multiplier")).doubleValue()).isEqualTo(0.0);
    }

    // --- haversineDistanceKm ---

    @Test
    void haversineDistanceIsZeroForSamePoint() {
        assertThat(service.haversineDistanceKm(40.0, -74.0, 40.0, -74.0)).isEqualTo(0.0);
    }

    @Test
    void haversineDistanceIsSymmetric() {
        double ab = service.haversineDistanceKm(43.14, -89.34, 41.97, -87.90);
        double ba = service.haversineDistanceKm(41.97, -87.90, 43.14, -89.34);
        assertThat(ab).isCloseTo(ba, within(0.001));
    }

    @Test
    void haversineDistanceMSNToORDIsApproximatelyCorrect() {
        // KMSN (Madison, WI) to KORD (Chicago): ~176 km great-circle
        double dist = service.haversineDistanceKm(43.14, -89.34, 41.97, -87.90);
        assertThat(dist).isBetween(160.0, 200.0);
    }

    // --- estimateCo2Kg ---

    @Test
    void estimateCo2KgReturnsNullWhenOriginIsNull() {
        Airport dest = airportWithCoords("KORD", 41.97, -87.90);
        assertThat(service.estimateCo2Kg(null, dest)).isNull();
    }

    @Test
    void estimateCo2KgReturnsNullWhenDestinationIsNull() {
        Airport origin = airportWithCoords("KMSN", 43.14, -89.34);
        assertThat(service.estimateCo2Kg(origin, null)).isNull();
    }

    @Test
    void estimateCo2KgReturnsNullWhenCoordsAreMissing() {
        Airport origin = new Airport();
        origin.setApCode("KMSN");
        Airport dest = airportWithCoords("KORD", 41.97, -87.90);
        assertThat(service.estimateCo2Kg(origin, dest)).isNull();
    }

    @Test
    void estimateCo2KgReturnsPositiveValueForValidAirports() {
        Airport origin = airportWithCoords("KMSN", 43.14, -89.34);
        Airport dest = airportWithCoords("KORD", 41.97, -87.90);
        Double co2 = service.estimateCo2Kg(origin, dest);
        assertThat(co2).isNotNull().isGreaterThan(0.0);
    }

    @Test
    void estimateCo2KgMatchesManualCalculation() {
        Airport lax = airportWithCoords("KLAX", 33.94, -118.41);
        Airport jfk = airportWithCoords("KJFK", 40.64, -73.78);
        double distanceKm = service.haversineDistanceKm(33.94, -118.41, 40.64, -73.78);
        double expected = service.calculateCarbonFootprint(distanceKm);
        assertThat(service.estimateCo2Kg(lax, jfk)).isCloseTo(expected, within(0.01));
    }

    // --- estimateCo2KgFromVelocity ---

    @Test
    void estimateCo2KgFromVelocityReturnsNullWhenVelocityIsZero() {
        assertThat(service.estimateCo2KgFromVelocity(0, java.time.LocalDateTime.now().minusHours(1))).isNull();
    }

    @Test
    void estimateCo2KgFromVelocityReturnsNullWhenVelocityIsNegative() {
        assertThat(service.estimateCo2KgFromVelocity(-100, java.time.LocalDateTime.now().minusHours(1))).isNull();
    }

    @Test
    void estimateCo2KgFromVelocityReturnsNullWhenDepartureIsInFuture() {
        assertThat(service.estimateCo2KgFromVelocity(500, java.time.LocalDateTime.now().plusHours(1))).isNull();
    }

    @Test
    void estimateCo2KgFromVelocityReturnsPositiveValueForValidInputs() {
        Double result = service.estimateCo2KgFromVelocity(500, java.time.LocalDateTime.now().minusHours(2));
        assertThat(result).isNotNull().isGreaterThan(0.0);
    }

    // --- estimateCo2Kg additional coordinate null branches ---

    @Test
    void estimateCo2KgReturnsNullWhenOriginLonIsNull() {
        Airport origin = new Airport();
        origin.setApCode("KMSN");
        origin.setLat(43.14); // lat set, lon missing
        Airport dest = airportWithCoords("KORD", 41.97, -87.90);
        assertThat(service.estimateCo2Kg(origin, dest)).isNull();
    }

    @Test
    void estimateCo2KgReturnsNullWhenDestinationLatIsNull() {
        Airport origin = airportWithCoords("KMSN", 43.14, -89.34);
        Airport dest = new Airport();
        dest.setApCode("KORD"); // no lat or lon
        assertThat(service.estimateCo2Kg(origin, dest)).isNull();
    }

    @Test
    void estimateCo2KgReturnsNullWhenDestinationLonIsNull() {
        Airport origin = airportWithCoords("KMSN", 43.14, -89.34);
        Airport dest = new Airport();
        dest.setApCode("KORD");
        dest.setLat(41.97); // lat set, lon missing
        assertThat(service.estimateCo2Kg(origin, dest)).isNull();
    }

    // --- helpers ---

    private Airport airportWithCoords(String code, double lat, double lon) {
        Airport a = new Airport();
        a.setApCode(code);
        a.setLat(lat);
        a.setLon(lon);
        return a;
    }
}
