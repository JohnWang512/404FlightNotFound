package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "airports")
/**
 * Airport reference data.
 *
 * Why airports are modeled as a simple lookup entity:
 * - Flight data uses airport codes (e.g., `KLAX`) as origin/destination keys.
 * - Using a string primary key makes the REST JSON match the external API concepts without an extra translation step.
 *
 * Relationship usage:
 * - `Flight.originApCode` and `Flight.destinationApCode` reference `Airport.apCode`.
 * - The service layer resolves/creates these reference rows when persisting flights (see `DbService`).
 */
public class Airport {

    /** String primary key for airport code (e.g., `KLAX`). */
    @Id
    @Column(name = "ap_code", length = 10)
    private String apCode;

    /** Human-friendly airport name. */
    @Column(name = "name", length = 255)
    private String name;

    /** City where the airport is located. */
    @Column(name = "city", length = 100)
    private String city;

    /** Country name/code as stored by the data source (free-form string for demo). */
    @Column(name = "country", length = 100)
    private String country;

    /** Latitude of the airport (decimal degrees); used for CO2 distance calculation. */
    @Column(name = "lat")
    private Double lat;

    /** Longitude of the airport (decimal degrees); used for CO2 distance calculation. */
    @Column(name = "lon")
    private Double lon;

    public Airport() {}

    public String getApCode() { return apCode; }
    public void setApCode(String apCode) { this.apCode = apCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }
}
