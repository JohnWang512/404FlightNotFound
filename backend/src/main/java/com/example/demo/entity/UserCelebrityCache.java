package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_celebrity_cache",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "celebrity_id"}))
/**
 * Tracks which celebrities each user has fetched flight data for.
 *
 * Why this table exists:
 * - When a user views a celebrity's flights, those flights are persisted to the `flights` table.
 * - This join table records the (user, celebrity) pair so the backend can quickly serve
 *   all previously-cached flight data for a user on their next login, without calling AeroAPI again.
 */
public class UserCelebrityCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK -> users.u_id */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** FK -> celebrities.id */
    @Column(name = "celebrity_id", nullable = false)
    private Long celebrityId;

    /** UTC timestamp of the most recent AeroAPI fetch for this (user, celebrity) pair. */
    @Column(name = "last_fetched_at", nullable = false)
    private LocalDateTime lastFetchedAt;

    public UserCelebrityCache() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCelebrityId() { return celebrityId; }
    public void setCelebrityId(Long celebrityId) { this.celebrityId = celebrityId; }

    public LocalDateTime getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(LocalDateTime lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }
}
