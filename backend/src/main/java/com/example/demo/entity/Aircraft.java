package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "aircraft")
/**
 * Aircraft table mapping.
 *
 * Why this entity is slightly different from the others:
 * - `id` is the primary key used by JPA for updates/deletes.
 * - `tailNumber` is UNIQUE and is the "natural key" used when bridging external API data to DB rows.
 * - Live-position columns are stored on the same row as the aircraft identity so the frontend can
 *   render "latest known position" without requiring an external API call for every marker.
 *
 * Note on schema:
 * - `backend/db/schema.sql` provides a baseline set of columns.
 * - Hibernate's `ddl-auto=update` may add the additional live-position columns defined here.
 */
public class Aircraft {

    /** Primary key (auto-increment). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UNIQUE tail number used as the identifier across external APIs and DB lookups. */
    @Column(name = "tail_number", unique = true, length = 50)
    private String tailNumber;

    /** Aircraft model/type descriptor (free-form string for demo purposes). */
    @Column(name = "model", length = 100)
    private String model;

    /**
     * Nullable owner reference.
     *
     * Why nullable:
     * - The DB FK uses `ON DELETE SET NULL`, so deleting a celebrity removes ownership
     *   without deleting the aircraft row or its historical flights.
     */
    @Column(name = "owner_id")
    private Long ownerId;

    /**
     * Live-position snapshot fields.
     *
     * Why these live on the aircraft row:
     * - It allows a DB-only UI rendering path (read from `/api/db/aircraft`) when the snapshot
     *   has been persisted.
     * - In this codebase, they are written by `DbService.saveLivePosition(...)` (not by a controller route).
     */
    @Column(name = "airborne")
    private Boolean airborne;

    /** Last known latitude (persisted live snapshot). */
    @Column(name = "last_lat")
    private Double lastLat;

    /** Last known longitude (persisted live snapshot). */
    @Column(name = "last_lon")
    private Double lastLon;

    /** Last known altitude in feet (persisted live snapshot). */
    @Column(name = "last_altitude")
    private Integer lastAltitude;

    /** Ground speed in knots (persisted live snapshot). */
    @Column(name = "last_speed")
    private Double lastSpeed;

    /** Heading/track in degrees (persisted live snapshot). */
    @Column(name = "last_heading")
    private Double lastHeading;

    /** Last-known callsign (persisted live snapshot). */
    @Column(name = "last_callsign", length = 20)
    private String lastCallsign;

    /** ICAO hex identifier from live data (persisted live snapshot). */
    @Column(name = "last_hex", length = 20)
    private String lastHex;

    /** Timestamp of the last persisted snapshot from live data. */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    // Source category from bulk sync: "mil", "ladd", "pia", "celebrity", or null
    @Column(name = "category", length = 20)
    private String category;

    public Aircraft() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTailNumber() { return tailNumber; }
    public void setTailNumber(String tailNumber) { this.tailNumber = tailNumber; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Boolean getAirborne() { return airborne; }
    public void setAirborne(Boolean airborne) { this.airborne = airborne; }

    public Double getLastLat() { return lastLat; }
    public void setLastLat(Double lastLat) { this.lastLat = lastLat; }

    public Double getLastLon() { return lastLon; }
    public void setLastLon(Double lastLon) { this.lastLon = lastLon; }

    public Integer getLastAltitude() { return lastAltitude; }
    public void setLastAltitude(Integer lastAltitude) { this.lastAltitude = lastAltitude; }

    public Double getLastSpeed() { return lastSpeed; }
    public void setLastSpeed(Double lastSpeed) { this.lastSpeed = lastSpeed; }

    public Double getLastHeading() { return lastHeading; }
    public void setLastHeading(Double lastHeading) { this.lastHeading = lastHeading; }

    public String getLastCallsign() { return lastCallsign; }
    public void setLastCallsign(String lastCallsign) { this.lastCallsign = lastCallsign; }

    public String getLastHex() { return lastHex; }
    public void setLastHex(String lastHex) { this.lastHex = lastHex; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
