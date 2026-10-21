
package com.example.demo.repository;
import com.example.demo.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence queries for {@link com.example.demo.entity.Flight}.
 *
 * Why this repository is more query-heavy than others:
 * - Flights reference aircraft and airports by ID/code, so useful lookups often require bridging
 *   between different identifier types (tail number -> aircraft id).
 * - Keeping this logic in the repository layer makes `DbService` simpler and keeps query intent explicit.
 */
public interface FlightRepository extends JpaRepository<Flight, Long> {
    /**
     * Retrieve flights belonging to a specific aircraft id.
     *
     * Why this is useful:
     * - It supports DB-first flows where the app has an aircraft row already and needs its flight history.
     */
    List<Flight> findByAircraftId(Long aircraftId);

    /**
     * Find flights by origin airport code.
     *
     * Why:
     * - Enables simple analytics/visualization (e.g., "show me flights leaving KLAX").
     */
    List<Flight> findByOriginApCode(String originApCode);

    /**
     * Find flights by destination airport code.
     *
     * Why:
     * - Symmetric with origin code search; destination is often needed for routes/filters.
     */
    List<Flight> findByDestinationApCode(String destinationApCode);

    /**
     * Find flights by tail number.
     *
     * Why the query uses a subquery:
     * - `Flight` rows store `aircraftId`, not tail numbers.
     * - `Aircraft` rows store `tailNumber` as a UNIQUE natural key and have a numeric `id`.
     * - This method bridges the identifiers by selecting the aircraft ids whose `tailNumber` matches,
     *   then returning all flights whose `aircraftId` is in that set.
     */
    @Query("SELECT f FROM Flight f WHERE f.aircraftId IN " +
           "(SELECT a.id FROM Aircraft a WHERE a.tailNumber = :tailNumber)")
    List<Flight> findByTailNumber(@Param("tailNumber") String tailNumber);

    /**
     * Retrieve flights whose departure time is after the provided cutoff, ordered newest-first.
     *
     * Why:
     * - The service layer uses this for the "recent flights" cache/summary endpoint,
     *   where ordering matters for correct UI recency rendering.
     */
    List<Flight> findByDepartureTimeAfterOrderByDepartureTimeDesc(LocalDateTime departureTime);

    /**
     * Retrieve all flights whose aircraft id is in the provided set, ordered newest-first by id.
     *
     * Why:
     * - Used by getLiveAircraft to batch-fetch CO2 values for all live aircraft in one query
     *   instead of issuing one query per aircraft (N+1 problem).
     */
    @Query("SELECT f FROM Flight f WHERE f.aircraftId IN :ids ORDER BY f.id DESC")
    List<Flight> findByAircraftIdIn(@Param("ids") List<Long> ids);
}