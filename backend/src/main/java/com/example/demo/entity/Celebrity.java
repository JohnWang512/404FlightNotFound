package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "celebrities")
/**
 * Celebrities/public figures tracked by the application.
 *
 * Why this entity exists:
 * - Aircraft in this demo are optionally "owned" by a celebrity so the UI can show friendly names
 *   alongside raw tail numbers.
 * - The ownership relationship is stored as a nullable foreign-key id (`Aircraft.ownerId`) rather than
 *   as a JPA association, keeping the REST payloads flat and serialization simple.
 */
public class Celebrity {

    /** Primary key (auto-increment). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name for the celebrity. */
    @Column(name = "name", length = 255)
    private String name;

    /** Category/tag describing the celebrity's industry (free-form string). */
    @Column(name = "industry", length = 255)
    private String industry;

    public Celebrity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
}
