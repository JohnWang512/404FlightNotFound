package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
/**
 * Flight record stored by the backend.
 *
 * Why this entity is a "hybrid" model:
 * - The app stores a mix of normalized, queryable fields (origin/destination codes, times, coordinates).
 * - It also stores an optional `liveInfoJson` blob so we can preserve extra API response details
 *   without forcing a schema change for every upstream payload variation.
 *
 * Relationship model (important):
 * - This entity stores FK values (e.g., `aircraftId`) instead of JPA associations.
 * - That keeps the REST payloads flat and avoids join/serialization complexity in this student project.
 */
public class Flight {

    /** Primary key (auto-increment). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owning aircraft identity (FK -> `aircraft.id`).
     *
     * Why `Long aircraftId`:
     * - Flights are stored against the DB's stable aircraft primary key.
     * - Tail-number lookup happens via `FlightRepository.findByTailNumber`, which bridges
     *   tail number -> aircraft id using the `aircraft` table.
     */
    @Column(name = "aircraft_id")
    private Long aircraftId;

    /** Origin airport code (FK -> `airports.ap_code`). */
    @Column(name = "origin_ap_code", length = 10)
    private String originApCode;

    /** Destination airport code (FK -> `airports.ap_code`). */
    @Column(name = "destination_ap_code", length = 10)
    private String destinationApCode;

    /** Departure timestamp. */
    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    /** Arrival timestamp; may be null when unknown or partial data was captured. */
    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    /** Optional CO2 estimate for the flight. */
    @Column(name = "estimated_co2_kg")
    private Double estimatedCo2Kg;

    /** Human-friendly callsign/marker used by the UI (optional). */
    @Column(name = "callsign", length = 32)
    private String callsign;

    /** Optional altitude in feet from the upstream data source. */
    @Column(name = "altitude_ft")
    private Integer altitudeFt;

    /** Optional ground speed in knots from the upstream data source. */
    @Column(name = "velocity_kts")
    private Integer velocityKts;

    /** Optional latitude used for map rendering. */
    @Column(name = "lat")
    private Double lat;

    /** Optional longitude used for map rendering. */
    @Column(name = "lng")
    private Double lng;

    /** Optional bearing/track used for map/icon orientation. */
    @Column(name = "bearing")
    private Double bearing;

    /**
     * Optional JSON blob holding additional live/API enrichment.
     *
     * Why a TEXT/LONGTEXT column:
     * - The structure of upstream fields may evolve, but we still want to store the raw data for the UI.
     * - `DbService.getRecentCachedFlights` parses this JSON back into a tree when it is present.
     */
    @Lob
    @Column(name = "live_info_json", columnDefinition = "LONGTEXT")
    private String liveInfoJson;

    public Flight() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAircraftId() { return aircraftId; }
    public void setAircraftId(Long aircraftId) { this.aircraftId = aircraftId; }

    public String getOriginApCode() { return originApCode; }
    public void setOriginApCode(String originApCode) { this.originApCode = originApCode; }

    public String getDestinationApCode() { return destinationApCode; }
    public void setDestinationApCode(String destinationApCode) { this.destinationApCode = destinationApCode; }

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public Double getEstimatedCo2Kg() { return estimatedCo2Kg; }
    public void setEstimatedCo2Kg(Double estimatedCo2Kg) { this.estimatedCo2Kg = estimatedCo2Kg; }

    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }

    public Integer getAltitudeFt() { return altitudeFt; }
    public void setAltitudeFt(Integer altitudeFt) { this.altitudeFt = altitudeFt; }

    public Integer getVelocityKts() { return velocityKts; }
    public void setVelocityKts(Integer velocityKts) { this.velocityKts = velocityKts; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Double getBearing() { return bearing; }
    public void setBearing(Double bearing) { this.bearing = bearing; }

    public String getLiveInfoJson() { return liveInfoJson; }
    public void setLiveInfoJson(String liveInfoJson) { this.liveInfoJson = liveInfoJson; }
}
