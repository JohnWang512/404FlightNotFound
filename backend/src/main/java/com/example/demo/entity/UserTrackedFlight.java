package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_tracked_flights",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_tracked_callsign", columnNames = {"user_id", "callsign"})
        }
)
public class UserTrackedFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "callsign", nullable = false, length = 32)
    private String callsign;

    @Column(name = "tail_number", length = 32)
    private String tailNumber;

    @Column(name = "origin_ap_code", length = 8)
    private String origin;

    @Column(name = "destination_ap_code", length = 8)
    private String destination;

    @Column(name = "altitude_ft")
    private Integer altitude;

    @Column(name = "velocity_kts")
    private Integer velocity;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "bearing")
    private Double bearing;

    @Column(name = "estimated_co2_kg")
    private Double estimatedCo2Kg;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "celebrity", length = 120)
    private String celebrity;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public String getTailNumber() {
        return tailNumber;
    }

    public void setTailNumber(String tailNumber) {
        this.tailNumber = tailNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getAltitude() {
        return altitude;
    }

    public void setAltitude(Integer altitude) {
        this.altitude = altitude;
    }

    public Integer getVelocity() {
        return velocity;
    }

    public void setVelocity(Integer velocity) {
        this.velocity = velocity;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getBearing() {
        return bearing;
    }

    public void setBearing(Double bearing) {
        this.bearing = bearing;
    }

    public Double getEstimatedCo2Kg() {
        return estimatedCo2Kg;
    }

    public void setEstimatedCo2Kg(Double estimatedCo2Kg) {
        this.estimatedCo2Kg = estimatedCo2Kg;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCelebrity() {
        return celebrity;
    }

    public void setCelebrity(String celebrity) {
        this.celebrity = celebrity;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
