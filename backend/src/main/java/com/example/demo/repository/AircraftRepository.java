package com.example.demo.repository;

import com.example.demo.entity.Aircraft;
import org.springframework.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

/**
 * Persistence queries for {@link com.example.demo.entity.Aircraft}.
 *
 * Why this repository exposes tail-number and owner-based lookups:
 * - Tail numbers are the external identifier used by airplanes.live and most UI flows.
 * - Owner ids are how the UI groups aircraft under celebrity metadata.
 */
public interface AircraftRepository extends CrudRepository<Aircraft, Long> {
    /**
     * Find an aircraft by its UNIQUE tail number.
     *
     * Why {@link Optional}:
     * - A tail number may not exist yet; callers often treat absence as "create a row" (for example in
     *   `DbService.batchSaveFlights` and `DbService.saveLivePosition`).
     */
    Optional<Aircraft> findByTailNumber(String tailNumber);

    /**
     * Find all aircraft owned by a specific celebrity.
     *
     * Why:
     * - Ownership is nullable (`Aircraft.ownerId`), so this method is useful for filtering only those aircraft
     *   with a known owner id.
     */
    List<Aircraft> findByOwnerId(Long ownerId);
    Optional<Aircraft> findByLastHex(String lastHex);
    List<Aircraft> findByCategory(String category);
}
