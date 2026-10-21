package com.example.demo;

import com.example.demo.dto.AircraftRequest;
import com.example.demo.dto.AirportRequest;
import com.example.demo.dto.CelebrityRequest;
import com.example.demo.entity.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/db")
/**
 * Thin REST adapter for the application's persistence layer.
 *
 * Why this controller is intentionally lightweight:
 * - The controller does not implement business rules; it only translates HTTP request/response
 *   into calls to {@link DbService} (and therefore into repository/database operations).
 * - This keeps the "why" behind data decisions (upsert behavior, fallback rules, side effects)
 *   centralized in {@link DbService}, so the REST surface stays predictable and easy to reason about.
 */
public class DbController {

    @Autowired
    private DbService dbService;

    // --- Status ---

    /**
     * Health/status endpoint for the DB-backed stack.
     *
     * Why:
     * - Returning table row counts makes it easy to distinguish "API is reachable" from
     *   "database is correctly initialized and persisting data".
     * - This also helps the frontend (or DB explorer) understand whether empty tables are expected
     *   or indicate a connection/schema/config issue.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(dbService.getStatus());
    }

    // --- Celebrities ---

    /**
     * List all celebrities from the database.
     *
     * Why:
     * - The UI typically needs a complete set of owners to render selection lists and ownership labels.
     */
    @GetMapping("/celebrities")
    public ResponseEntity<?> getAllCelebrities() {
        return ResponseEntity.ok(dbService.getAllCelebrities());
    }

    /**
     * Fetch a single celebrity by primary key.
     *
     * Why:
     * - Clients should get a 404 when a record does not exist instead of receiving an empty object.
     * - The absence maps directly to the UI decision: show "not found" vs render edit form.
     */
    @GetMapping("/celebrities/{id}")
    public ResponseEntity<?> getCelebrityById(@PathVariable Long id) {
        return dbService.getCelebrityById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update a celebrity record.
     *
     * Important semantics (upsert):
     * - This endpoint delegates to JPA {@code save(...)} via {@link DbService#saveCelebrity(Celebrity)}.
     * - JPA will update when the entity's primary key is present in the JSON; otherwise it inserts a new row.
     */
    @PostMapping("/celebrities")
    public ResponseEntity<?> saveCelebrity(@Valid @RequestBody CelebrityRequest request) {
        Celebrity celebrity = new Celebrity();
        celebrity.setId(request.id());
        celebrity.setName(request.name());
        celebrity.setIndustry(request.industry());
        return ResponseEntity.ok(dbService.saveCelebrity(celebrity));
    }

    /**
     * Delete a celebrity by id.
     *
     * Why 204 No Content:
     * - Deletion has no useful response payload; the UI just needs confirmation that the row is gone.
     * - Returning {@code void} avoids leaking internal entity details after a destructive operation.
     */
    @DeleteMapping("/celebrities/{id}")
    public ResponseEntity<Void> deleteCelebrity(@PathVariable Long id) {
        dbService.deleteCelebrity(id);
        return ResponseEntity.noContent().build();
    }

    // --- Aircraft ---

    /**
     * List all aircraft rows.
     *
     * Why:
     * - The UI uses this as the "tracked objects" list.
     * - Live-position columns (if present in the DB) are read-only from the perspective of this controller.
     */
    @GetMapping("/aircraft")
    public ResponseEntity<?> getAllAircraft() {
        return ResponseEntity.ok(dbService.getAllAircraft());
    }

    /**
     * Fetch a single aircraft by its primary key.
     *
     * Why:
     * - Aircraft are modeled with an auto-increment {@code id} primary key; the JSON must provide that id for updates.
     */
    @GetMapping("/aircraft/{id}")
    public ResponseEntity<?> getAircraftById(@PathVariable Long id) {
        return dbService.getAircraftById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update an aircraft record.
     *
     * Why identity is subtle:
     * - The aircraft table has both an auto-generated {@code id} (JPA primary key) and a UNIQUE {@code tailNumber}.
     * - This controller delegates directly to {@code aircraftRepo.save(...)}; it does not translate a provided
     *   tail number into an id.
     * - As a result, clients should send the aircraft {@code id} when they intend to update an existing row.
     */
    @PostMapping("/aircraft")
    public ResponseEntity<?> saveAircraft(@Valid @RequestBody AircraftRequest request) {
        Aircraft aircraft = new Aircraft();
        aircraft.setId(request.id());
        aircraft.setTailNumber(request.tailNumber());
        aircraft.setModel(request.model());
        aircraft.setOwnerId(request.ownerId());
        return ResponseEntity.ok(dbService.saveAircraft(aircraft));
    }

    /**
     * Delete an aircraft by id.
     *
     * Why:
     * - Deleting an aircraft cascades to dependent flights in the DB (see `flights.aircraft_id` FK behavior),
     *   so the UI can treat aircraft removal as removing its associated history/snapshots.
     */
    @DeleteMapping("/aircraft/{id}")
    public ResponseEntity<Void> deleteAircraft(@PathVariable Long id) {
        dbService.deleteAircraft(id);
        return ResponseEntity.noContent().build();
    }

    // --- Airports ---

    /**
     * List all airports.
     *
     * Why:
     * - Origin/destination codes are used throughout the app; the UI may offer airport pickers and labels.
     */
    @GetMapping("/airports")
    public ResponseEntity<?> getAllAirports() {
        return ResponseEntity.ok(dbService.getAllAirports());
    }

    /**
     * Fetch a single airport by its code (string primary key).
     *
     * Why:
     * - Airports are keyed by `apCode` (e.g., `KLAX`), not by an integer id, because flight data uses IATA/ICAO codes.
     */
    @GetMapping("/airports/{code}")
    public ResponseEntity<?> getAirportByCode(@PathVariable String code) {
        return dbService.getAirportByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update an airport.
     *
     * Why:
     * - The primary key is `apCode`, so updates should include `apCode` in the JSON object.
     */
    @PostMapping("/airports")
    public ResponseEntity<?> saveAirport(@Valid @RequestBody AirportRequest request) {
        Airport airport = new Airport();
        airport.setApCode(request.apCode());
        airport.setName(request.name());
        airport.setCity(request.city());
        airport.setCountry(request.country());
        airport.setLat(request.lat());
        airport.setLon(request.lon());
        return ResponseEntity.ok(dbService.saveAirport(airport));
    }

    /**
     * Delete an airport by its code.
     *
     * Why:
     * - Airport deletion removes reference data by key; any flights referencing it may fail due to FK constraints.
     */
    @DeleteMapping("/airports/{code}")
    public ResponseEntity<Void> deleteAirport(@PathVariable String code) {
        dbService.deleteAirport(code);
        return ResponseEntity.noContent().build();
    }

    // --- Flights ---

    /**
     * List all flights stored in the DB.
     *
     * Why:
     * - This is primarily used for debugging/inspection and for admin-like DB exploration.
     * - For user-facing searches, use the tail-number search or cached summary endpoints.
     */
    @GetMapping("/flights")
    public ResponseEntity<?> getAllFlights() {
        return ResponseEntity.ok(dbService.getAllFlights());
    }

    /**
     * Best-effort bulk persistence for flight-like records.
     *
     * Why a "batch" endpoint exists:
     * - The frontend often receives a list of flight markers/records in one response.
     * - Persisting them one-by-one via individual POSTs would be slower and harder to implement.
     *
     * Expected payload shape (per element) is intentionally "loose" because the controller accepts a Map:
     * - required-ish keys: `callsign`, `origin`, `destination`
     * - numeric keys: `altitude`, `velocity`, `lat`, `lng`, `bearing`
     * - optional key: `liveInfo` (will be serialized to `Flight.liveInfoJson`)
     */
    @PostMapping("/flights/batch")
    public ResponseEntity<?> batchSaveFlights(@RequestBody List<Map<String, Object>> flights) {
        return ResponseEntity.ok(dbService.batchSaveFlights(flights));
    }

    /**
     * Fetch a flight by its primary key.
     *
     * Why:
     * - Flight ids are auto-generated; returning 404 cleanly differentiates "missing" from "present but empty fields".
     */
    @GetMapping("/flights/{id}")
    public ResponseEntity<?> getFlightById(@PathVariable Long id) {
        return dbService.getFlightById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update a single flight record.
     *
     * Why:
     * - This endpoint is primarily used when the client already has a canonical `Flight` JSON shape.
     * - For clients that only have loose marker data, prefer `/api/db/flights/batch`.
     */
    @PostMapping("/flights")
    public ResponseEntity<?> saveFlight(@RequestBody Flight flight) {
        return ResponseEntity.ok(dbService.saveFlight(flight));
    }

    /**
     * Delete a flight by id.
     *
     * Why:
     * - Deletion is destructive and does not require a response payload; 204 provides a clear contract.
     */
    @DeleteMapping("/flights/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        dbService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Return the CO2 estimate and a commercial-route comparison for a specific flight.
     *
     * Why:
     * - The spec calls for an EnvironmentalService that can compare private jet emissions
     *   against commercial travel on the same route (README class diagram, state diagram).
     * - This endpoint surfaces that data without requiring the client to do the math.
     *
     * Response includes:
     * - {@code estimatedCo2Kg}  — total private jet CO2 for this flight
     * - {@code comparison}      — per-passenger comparison vs. commercial (null if airport coords missing)
     */
    @GetMapping("/flights/{id}/co2")
    public ResponseEntity<?> getFlightCo2(@PathVariable Long id) {
        return ResponseEntity.ok(dbService.getCo2Summary(id));
    }

    /**
     * Recalculate and persist the CO2 estimate for a flight using current airport coordinates.
     *
     * Why:
     * - Airport coordinates may be added after a flight is saved (e.g., via a later AeroAPI fetch).
     * - This endpoint allows correcting a previously null {@code estimatedCo2Kg} without re-saving
     *   the entire flight record.
     */
    @PostMapping("/flights/{id}/co2/recalculate")
    public ResponseEntity<?> recalculateFlightCo2(@PathVariable Long id) {
        return ResponseEntity.ok(dbService.recalculateCo2(id));
    }

    // --- Users ---

    /**
     * List all app users.
     *
     * Why:
     * - This is useful for development/testing because authentication is not the focus of this project.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(dbService.getAllUsers());
    }

    /**
     * Fetch a single user by id.
     *
     * Why:
     * - Mirrors other entity GET-by-id endpoints for consistent REST behavior (404 vs 200).
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return dbService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update a user record.
     *
     * Why:
     * - Uses JPA {@code save(...)} semantics: include the primary key in JSON when updating.
     */
    @PostMapping("/users")
    public ResponseEntity<?> saveUser(@RequestBody AppUser user) {
        return ResponseEntity.ok(dbService.saveUser(user));
    }

    /**
     * Delete a user by id.
     *
     * Why:
     * - For this demo schema, the user delete does not affect the FK graph used by flights/aircraft,
     *   so it can safely return 204 without additional payload.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        dbService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/tracked-flights")
    public ResponseEntity<?> getUserTrackedFlights(@PathVariable Long userId) {
        return ResponseEntity.ok(dbService.getUserTrackedFlights(userId));
    }

    @PostMapping("/users/{userId}/tracked-flights")
    public ResponseEntity<?> saveUserTrackedFlight(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(dbService.upsertUserTrackedFlight(userId, payload));
    }

    @DeleteMapping("/users/{userId}/tracked-flights/{callsign}")
    public ResponseEntity<?> deleteUserTrackedFlight(
            @PathVariable Long userId,
            @PathVariable String callsign) {
        return ResponseEntity.ok(dbService.deleteUserTrackedFlight(userId, callsign));
    }

    @DeleteMapping("/users/{userId}/tracked-flights")
    public ResponseEntity<?> clearUserTrackedFlights(@PathVariable Long userId) {
        return ResponseEntity.ok(dbService.clearUserTrackedFlights(userId));
    }

    // --- Celebrity flight history ---

    /**
     * Returns recent flight history for a celebrity by looking up their linked aircraft
     * in the DB and querying AeroAPI for each tail number.
     *
     * When {@code userId} is provided, the (user, celebrity) pair is recorded in
     * {@code user_celebrity_cache} so the user's history can be served from the DB on next login.
     */
    @GetMapping("/celebrities/{id}/flights")
    public ResponseEntity<?> getCelebrityFlights(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(dbService.getCelebrityFlights(id, userId));
    }

    /**
     * Returns all celebrities a user has previously fetched, with their DB-cached flight history.
     *
     * Why DB-only:
     * - Designed for fast pre-loading at login time; no AeroAPI calls are made.
     */
    @GetMapping("/users/{userId}/celebrity-history")
    public ResponseEntity<?> getUserCelebrityHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(dbService.getUserCelebrityHistory(userId));
    }

    // --- Live celebrity status ---

    /**
     * Returns all celebrities joined with their linked aircraft's current live-position snapshot.
     *
     * Why:
     * - The Celebrity Tracker UI needs a single endpoint that combines celebrity metadata (name, industry)
     *   with real-time airborne status, lastLat, lastLon, and lastSeen from the aircraft row.
     * - Celebrities with no linked aircraft return airborne=null so the UI can show "Unknown".
     */
    @GetMapping("/celebrities/live")
    public ResponseEntity<?> getLiveCelebrities() {
        return ResponseEntity.ok(dbService.getLiveCelebrities());
    }

    // --- Live aircraft positions (for map polling) ---

    @GetMapping("/aircraft/live")
    public ResponseEntity<?> getLiveAircraft(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(dbService.getLiveAircraft(category));
    }

    // --- Manual commercial flight sync from AeroAPI ---

    @PostMapping("/aircraft/sync-commercial")
    public ResponseEntity<?> syncCommercial() {
        return ResponseEntity.ok(dbService.syncCommercialFlights());
    }

    // --- Save live position for a single aircraft ---

    @GetMapping("/save-live")
    public ResponseEntity<?> saveLive(@RequestParam String tailNumber) {
        return ResponseEntity.ok(dbService.saveLivePosition(tailNumber));
    }

    // --- Save live positions for all aircraft in DB (one per second) ---

    @GetMapping("/save-live-all")
    public ResponseEntity<?> saveLiveAll() {
        return ResponseEntity.ok(dbService.saveLiveAll());
    }

    // --- Fetch flight from AeroAPI and save to DB ---

    @GetMapping("/fetch-save")
    public ResponseEntity<?> fetchAndSave(@RequestParam String ident) {
        return ResponseEntity.ok(dbService.fetchAndSaveFlight(ident));
    }

    // --- Bulk Import ---

    /**
     * Bulk import multiple entity types in a single request.
     *
     * Why this endpoint exists:
     * - Seeding a full relational dataset (celebrities -> aircraft -> airports -> flights) is otherwise tedious.
     * - The implementation intentionally tolerates per-item conversion/persistence failures and returns counts/errors.
     *
     * Expected payload:
     * - an object with keys: `celebrities`, `aircraft`, `airports`, `flights`
     * - each key maps to a JSON array whose elements are shaped like the corresponding JPA entities.
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importJson(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(dbService.importJson(payload));
    }

    // --- Search ---

    /**
     * Search flights by tail number.
     *
     * Why:
     * - The `Flight` table stores `aircraft_id`, but users typically search using the human-friendly `tail_number`.
     * - `DbService.searchFlightsByTailNumber` relies on `FlightRepository.findByTailNumber(...)` to bridge those keys.
     */
    @GetMapping("/search/flights")
    public ResponseEntity<?> searchFlights(@RequestParam String tailNumber) {
        return ResponseEntity.ok(dbService.searchFlightsByTailNumber(tailNumber));
    }

    /**
     * Return a cached "recent flights" summary directly derived from DB rows.
     *
     * Why:
     * - This endpoint formats flight records into a UI-friendly shape and parses optional `liveInfoJson`.
     * - It gives the frontend a DB-only fast path for rendering recency without calling external APIs.
     */
    @GetMapping("/flights/cache")
    public ResponseEntity<?> getRecentCachedFlights(@RequestParam(defaultValue = "180") int minutes) {
        return ResponseEntity.ok(dbService.getRecentCachedFlights(minutes));
    }
}