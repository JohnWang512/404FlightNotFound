package com.example.demo;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class DbService {

    @Autowired private CelebrityRepository celebrityRepo;
    @Autowired private AircraftRepository aircraftRepo;
    @Autowired private AirportRepository airportRepo;
    @Autowired private FlightRepository flightRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private FlightService flightService;
    @Autowired private AirplanesLiveService airplanesLiveService;
    @Autowired private EnvironmentalService environmentalService;
    @Autowired private com.example.demo.repository.UserCelebrityCacheRepository userCelebrityCacheRepo;
    @Autowired private UserTrackedFlightRepository userTrackedFlightRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    // --- Status ---

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "ok");
        status.put("celebrities", celebrityRepo.count());
        status.put("aircraft", aircraftRepo.count());
        status.put("airports", airportRepo.count());
        status.put("flights", flightRepo.count());
        status.put("users", userRepo.count());
        return status;
    }

    // --- List all ---

    public Iterable<Celebrity> getAllCelebrities() { return celebrityRepo.findAll(); }
    public Iterable<Aircraft> getAllAircraft() { return aircraftRepo.findAll(); }
    public Iterable<Airport> getAllAirports() { return airportRepo.findAll(); }
    public List<Flight> getAllFlights() { return flightRepo.findAll(); }
    public Iterable<AppUser> getAllUsers() { return userRepo.findAll(); }

    // --- Get by ID ---

    public Optional<Celebrity> getCelebrityById(Long id) { return celebrityRepo.findById(id); }
    public Optional<Aircraft> getAircraftById(Long id) { return aircraftRepo.findById(id); }
    public Optional<Airport> getAirportByCode(String code) { return airportRepo.findById(code); }
    public Optional<Flight> getFlightById(Long id) { return flightRepo.findById(id); }
    public Optional<AppUser> getUserById(Long id) { return userRepo.findById(id); }

    // --- Save (upsert) ---

    public Celebrity saveCelebrity(Celebrity c) { return celebrityRepo.save(c); }
    public Aircraft saveAircraft(Aircraft a) { return aircraftRepo.save(a); }
    public Airport saveAirport(Airport a) { return airportRepo.save(a); }
    public Flight saveFlight(Flight f) { return flightRepo.save(f); }
    public AppUser saveUser(AppUser u) { return userRepo.save(u); }

    // --- Delete ---

    public void deleteCelebrity(Long id) { celebrityRepo.deleteById(id); }
    public void deleteAircraft(Long id) { aircraftRepo.deleteById(id); }
    public void deleteAirport(String code) { airportRepo.deleteById(code); }
    public void deleteFlight(Long id) { flightRepo.deleteById(id); }
    public void deleteUser(Long id) { userRepo.deleteById(id); }

    public List<Map<String, Object>> getUserTrackedFlights(Long userId) {
        List<UserTrackedFlight> trackedFlights = userTrackedFlightRepo.findByUserIdOrderByUpdatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserTrackedFlight tracked : trackedFlights) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("callsign", tracked.getCallsign());
            item.put("tailNumber", tracked.getTailNumber());
            item.put("origin", tracked.getOrigin() == null ? "N/A" : tracked.getOrigin());
            item.put("destination", tracked.getDestination() == null ? "N/A" : tracked.getDestination());
            item.put("altitude", tracked.getAltitude() == null ? 0 : tracked.getAltitude());
            item.put("velocity", tracked.getVelocity() == null ? 0 : tracked.getVelocity());
            item.put("lat", tracked.getLat());
            item.put("lng", tracked.getLng());
            item.put("bearing", tracked.getBearing());
            item.put("estimatedCo2Kg", tracked.getEstimatedCo2Kg());
            item.put("category", tracked.getCategory());
            item.put("celebrity", tracked.getCelebrity());
            item.put("updatedAt", tracked.getUpdatedAt());
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> upsertUserTrackedFlight(Long userId, Map<String, Object> payload) {
        String callsign = asTrimmedString(payload.get("callsign"));
        if (callsign.isBlank()) {
            return Map.of("ok", false, "error", "callsign is required");
        }

        UserTrackedFlight tracked = userTrackedFlightRepo
                .findByUserIdAndCallsign(userId, callsign)
                .orElseGet(UserTrackedFlight::new);

        tracked.setUserId(userId);
        tracked.setCallsign(callsign);
        tracked.setTailNumber(asNullableString(payload.get("tailNumber")));
        tracked.setOrigin(asNullableString(payload.get("origin")));
        tracked.setDestination(asNullableString(payload.get("destination")));
        tracked.setAltitude(asNullableInteger(payload.get("altitude")));
        tracked.setVelocity(asNullableInteger(payload.get("velocity")));
        tracked.setLat(asNullableDouble(payload.get("lat")));
        tracked.setLng(asNullableDouble(payload.get("lng")));
        tracked.setBearing(asNullableDouble(payload.get("bearing")));
        tracked.setEstimatedCo2Kg(asNullableDouble(payload.get("estimatedCo2Kg")));
        tracked.setCategory(asNullableString(payload.get("category")));
        tracked.setCelebrity(asNullableString(payload.get("celebrity")));
        tracked.setUpdatedAt(LocalDateTime.now());

        UserTrackedFlight saved = userTrackedFlightRepo.save(tracked);
        return Map.of(
                "ok", true,
                "id", saved.getId(),
                "callsign", saved.getCallsign(),
                "updatedAt", saved.getUpdatedAt()
        );
    }

    @Transactional
    public Map<String, Object> deleteUserTrackedFlight(Long userId, String callsign) {
        String normalized = callsign == null ? "" : callsign.trim();
        if (normalized.isBlank()) {
            return Map.of("ok", false, "error", "callsign is required");
        }

        userTrackedFlightRepo.deleteByUserIdAndCallsign(userId, normalized);
        return Map.of("ok", true, "callsign", normalized);
    }

    @Transactional
    public Map<String, Object> clearUserTrackedFlights(Long userId) {
        userTrackedFlightRepo.deleteByUserId(userId);
        return Map.of("ok", true);
    }

    private static String asTrimmedString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static String asNullableString(Object value) {
        String normalized = asTrimmedString(value);
        return normalized.isBlank() ? null : normalized;
    }

    private static Integer asNullableInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String normalized = asTrimmedString(value);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double asNullableDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String normalized = asTrimmedString(value);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    // --- Search ---

    public List<Flight> searchFlightsByTailNumber(String tailNumber) {
        return flightRepo.findByTailNumber(tailNumber);
    }

    public Map<String, Object> getRecentCachedFlights(int minutes) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(Math.max(1, minutes));
        List<Flight> recent = flightRepo.findByDepartureTimeAfterOrderByDepartureTimeDesc(cutoff);

        Set<Long> aircraftIds = new HashSet<>();
        for (Flight f : recent) {
            if (f.getAircraftId() != null) {
                aircraftIds.add(f.getAircraftId());
            }
        }

        Map<Long, String> callsignsByAircraftId = new HashMap<>();
        for (Aircraft a : aircraftRepo.findAllById(aircraftIds)) {
            callsignsByAircraftId.put(a.getId(), a.getTailNumber());
        }

        List<Map<String, Object>> flights = new ArrayList<>();
        for (Flight f : recent) {
            if (f.getLat() == null || f.getLng() == null) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            String callsign = f.getCallsign();
            if (callsign == null || callsign.isBlank()) {
                callsign = callsignsByAircraftId.getOrDefault(f.getAircraftId(), "UNKNOWN");
            }

            item.put("callsign", callsign);
            item.put("origin", f.getOriginApCode() == null ? "???" : f.getOriginApCode());
            item.put("destination", f.getDestinationApCode() == null ? "???" : f.getDestinationApCode());
            item.put("altitude", f.getAltitudeFt() == null ? 0 : f.getAltitudeFt());
            item.put("velocity", f.getVelocityKts() == null ? 0 : f.getVelocityKts());
            item.put("lat", f.getLat());
            item.put("lng", f.getLng());
            item.put("bearing", f.getBearing());
            item.put("estimatedCo2Kg", f.getEstimatedCo2Kg());

            Object liveInfo = null;
            if (f.getLiveInfoJson() != null && !f.getLiveInfoJson().isBlank()) {
                try {
                    liveInfo = mapper.readValue(f.getLiveInfoJson(), Object.class);
                } catch (Exception ignored) {
                    liveInfo = null;
                }
            }
            item.put("liveInfo", liveInfo);

            // Use the persisted CO2 estimate when available; fall back to a velocity-based
            // running estimate for live flights that have no route data (??? -> ???).
            Double co2 = f.getEstimatedCo2Kg();
            if (co2 == null) {
                co2 = environmentalService.estimateCo2KgFromVelocity(f.getVelocityKts(), f.getDepartureTime());
            }
            item.put("estimatedCo2Kg", co2);

            flights.add(item);
        }

        result.put("flights", flights);
        result.put("count", flights.size());
        result.put("source", "database");
        return result;
    }

    public Map<String, Object> batchSaveFlights(List<Map<String, Object>> flights) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        int savedCount = 0;

        for (Map<String, Object> item : flights) {
            try {
                String callsign = (String) item.get("callsign");
                String originCode = (String) item.get("origin");
                String destinationCode = (String) item.get("destination");
                Integer altitude = item.get("altitude") instanceof Number ? ((Number) item.get("altitude")).intValue() : 0;
                Integer velocity = item.get("velocity") instanceof Number ? ((Number) item.get("velocity")).intValue() : 0;
                Double lat = item.get("lat") instanceof Number ? ((Number) item.get("lat")).doubleValue() : null;
                Double lng = item.get("lng") instanceof Number ? ((Number) item.get("lng")).doubleValue() : null;
                Double bearing = item.get("bearing") instanceof Number ? ((Number) item.get("bearing")).doubleValue() : null;

                if (originCode != null && !originCode.isBlank() && !"???".equals(originCode) && !airportRepo.existsById(originCode)) {
                    Airport origin = new Airport();
                    origin.setApCode(originCode);
                    origin.setName(originCode);
                    airportRepo.save(origin);
                }

                if (destinationCode != null && !destinationCode.isBlank() && !"???".equals(destinationCode) && !airportRepo.existsById(destinationCode)) {
                    Airport destination = new Airport();
                    destination.setApCode(destinationCode);
                    destination.setName(destinationCode);
                    airportRepo.save(destination);
                }

                String tailNumber = (callsign != null && !callsign.isBlank()) ? callsign : "UNKNOWN";
                Aircraft aircraft = aircraftRepo.findByTailNumber(tailNumber)
                        .orElseGet(() -> {
                            Aircraft a = new Aircraft();
                            a.setTailNumber(tailNumber);
                            a.setModel(null);
                            return aircraftRepo.save(a);
                        });

                Flight flight = new Flight();
                flight.setAircraftId(aircraft.getId());
                flight.setCallsign(callsign);
                flight.setOriginApCode((originCode != null && !"???".equals(originCode)) ? originCode : null);
                flight.setDestinationApCode((destinationCode != null && !"???".equals(destinationCode)) ? destinationCode : null);
                flight.setAltitudeFt(altitude);
                flight.setVelocityKts(velocity);
                flight.setLat(lat);
                flight.setLng(lng);
                flight.setBearing(bearing);
                if (item.containsKey("liveInfo")) {
                    try {
                        flight.setLiveInfoJson(mapper.writeValueAsString(item.get("liveInfo")));
                    } catch (Exception ignored) {
                        flight.setLiveInfoJson(null);
                    }
                }
                flight.setDepartureTime(LocalDateTime.now());
                flightRepo.save(flight);
                savedCount++;
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        result.put("flightsSaved", savedCount);
        result.put("errors", errors);
        return result;
    }

    // --- Bulk JSON Import ---

    public Map<String, Object> importJson(Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        int celebCount = 0, aircraftCount = 0, airportCount = 0, flightCount = 0;

        Object celebList = payload.get("celebrities");
        if (celebList instanceof List) {
            for (Object item : (List<?>) celebList) {
                try {
                    Celebrity c = mapper.convertValue(item, Celebrity.class);
                    celebrityRepo.save(c);
                    celebCount++;
                } catch (Exception e) {
                    errors.add("celebrity: " + e.getMessage());
                }
            }
        }

        Object aircraftList = payload.get("aircraft");
        if (aircraftList instanceof List) {
            for (Object item : (List<?>) aircraftList) {
                try {
                    Aircraft a = mapper.convertValue(item, Aircraft.class);
                    aircraftRepo.save(a);
                    aircraftCount++;
                } catch (Exception e) {
                    errors.add("aircraft: " + e.getMessage());
                }
            }
        }

        Object airportList = payload.get("airports");
        if (airportList instanceof List) {
            for (Object item : (List<?>) airportList) {
                try {
                    Airport a = mapper.convertValue(item, Airport.class);
                    airportRepo.save(a);
                    airportCount++;
                } catch (Exception e) {
                    errors.add("airport: " + e.getMessage());
                }
            }
        }

        Object flightList = payload.get("flights");
        if (flightList instanceof List) {
            for (Object item : (List<?>) flightList) {
                try {
                    Flight f = mapper.convertValue(item, Flight.class);
                    flightRepo.save(f);
                    flightCount++;
                } catch (Exception e) {
                    errors.add("flight: " + e.getMessage());
                }
            }
        }

        result.put("celebrities", celebCount);
        result.put("aircraft", aircraftCount);
        result.put("airports", airportCount);
        result.put("flights", flightCount);
        result.put("errors", errors);
        return result;
    }

    // --- DB-First Fetch and Save ---

    public Map<String, Object> fetchAndSaveFlight(String ident) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Step 1: Check DB first
        Optional<Aircraft> existingAircraft = aircraftRepo.findByTailNumber(ident);
        if (existingAircraft.isPresent()) {
            List<Flight> dbFlights = flightRepo.findByAircraftId(existingAircraft.get().getId());
            if (!dbFlights.isEmpty()) {
                result.put("source", "database");
                result.put("aircraft", existingAircraft.get());
                result.put("flights", dbFlights);
                return result;
            }
        }

        // Step 2: Fetch from AeroAPI
        ResponseEntity<String> apiResponse;
        try {
            apiResponse = flightService.getFlightByIdent(ident);
        } catch (Exception e) {
            result.put("error", "AeroAPI request failed: " + e.getMessage());
            return result;
        }

        if (!apiResponse.getStatusCode().is2xxSuccessful() || apiResponse.getBody() == null) {
            result.put("error", "AeroAPI returned " + apiResponse.getStatusCode());
            return result;
        }

        // Step 3: Parse response
        JsonNode root;
        try {
            root = mapper.readTree(apiResponse.getBody());
        } catch (Exception e) {
            result.put("error", "Failed to parse AeroAPI response: " + e.getMessage());
            return result;
        }

        JsonNode flightsNode = root.path("flights");
        if (!flightsNode.isArray() || flightsNode.isEmpty()) {
            // Step 3: AeroAPI had no results — try airplanes.live
            return fetchFromAirplanesLive(ident, result);
        }

        List<String> airportsSaved = new ArrayList<>();
        List<Flight> savedFlights = new ArrayList<>();
        Aircraft savedAircraft = null;

        for (JsonNode flightNode : flightsNode) {
            // Step 4a: Upsert airports first (store lat/lon when AeroAPI provides them)
            JsonNode originNode = flightNode.path("origin");
            String originCode = originNode.path("code").asText(null);
            if (originCode != null) {
                Airport origin = airportRepo.findById(originCode).orElseGet(() -> {
                    Airport a = new Airport();
                    a.setApCode(originCode);
                    airportsSaved.add(originCode);
                    return a;
                });
                origin.setName(originNode.path("name").asText(origin.getName()));
                origin.setCity(originNode.path("city").asText(origin.getCity()));
                origin.setCountry(originNode.path("country_code").asText(origin.getCountry()));
                if (originNode.path("latitude").isNumber()) origin.setLat(originNode.path("latitude").asDouble());
                if (originNode.path("longitude").isNumber()) origin.setLon(originNode.path("longitude").asDouble());
                airportRepo.save(origin);
            }

            JsonNode destNode = flightNode.path("destination");
            String destCode = destNode.path("code").asText(null);
            if (destCode != null) {
                Airport dest = airportRepo.findById(destCode).orElseGet(() -> {
                    Airport a = new Airport();
                    a.setApCode(destCode);
                    airportsSaved.add(destCode);
                    return a;
                });
                dest.setName(destNode.path("name").asText(dest.getName()));
                dest.setCity(destNode.path("city").asText(dest.getCity()));
                dest.setCountry(destNode.path("country_code").asText(dest.getCountry()));
                if (destNode.path("latitude").isNumber()) dest.setLat(destNode.path("latitude").asDouble());
                if (destNode.path("longitude").isNumber()) dest.setLon(destNode.path("longitude").asDouble());
                airportRepo.save(dest);
            }

            // Step 4b: Upsert aircraft
            String tailNumber = flightNode.path("registration").isMissingNode()
                    ? ident
                    : flightNode.path("registration").asText(ident);
            String model = flightNode.path("aircraft_type").asText(null);

            if (savedAircraft == null) {
                Optional<Aircraft> existing = aircraftRepo.findByTailNumber(tailNumber);
                if (existing.isPresent()) {
                    savedAircraft = existing.get();
                } else {
                    Aircraft newAircraft = new Aircraft();
                    newAircraft.setTailNumber(tailNumber);
                    newAircraft.setModel(model);
                    savedAircraft = aircraftRepo.save(newAircraft);
                }
            }

            // Step 4c: Parse timestamps
            LocalDateTime departureTime = parseTimestamp(flightNode, "actual_out", "scheduled_out");
            LocalDateTime arrivalTime = parseTimestamp(flightNode, "actual_in", "scheduled_in");

            // Step 4d: Save flight, including CO2 estimate when airport coords are available
            Flight flight = new Flight();
            flight.setAircraftId(savedAircraft.getId());
            flight.setOriginApCode(originCode);
            flight.setDestinationApCode(destCode);
            flight.setDepartureTime(departureTime);
            flight.setArrivalTime(arrivalTime);
            Double co2 = environmentalService.estimateCo2Kg(
                    originCode != null ? airportRepo.findById(originCode).orElse(null) : null,
                    destCode   != null ? airportRepo.findById(destCode).orElse(null)   : null);
            flight.setEstimatedCo2Kg(co2);
            savedFlights.add(flightRepo.save(flight));
        }

        result.put("source", "aeroapi");
        result.put("aircraft", savedAircraft);
        result.put("flights", savedFlights);
        result.put("airportsSaved", airportsSaved);
        return result;
    }

    // --- airplanes.live Step 3 fallback (used inside fetchAndSaveFlight) ---

    private Map<String, Object> fetchFromAirplanesLive(String tailNumber, Map<String, Object> result) {
        try {
            ResponseEntity<String> liveResponse = airplanesLiveService.getByReg(tailNumber);
            if (!liveResponse.getStatusCode().is2xxSuccessful() || liveResponse.getBody() == null) {
                result.put("source", "none");
                result.put("message", "No data found in DB, AeroAPI, or airplanes.live for: " + tailNumber);
                return result;
            }

            JsonNode liveRoot = mapper.readTree(liveResponse.getBody());
            JsonNode acArray = liveRoot.path("ac");

            if (!acArray.isArray() || acArray.isEmpty()) {
                result.put("source", "none");
                result.put("message", "No data found in DB, AeroAPI, or airplanes.live for: " + tailNumber);
                return result;
            }

            JsonNode ac = acArray.get(0);

            // Upsert aircraft (use ICAO type code for model if available)
            String model = ac.path("t").isMissingNode() ? null : ac.path("t").asText(null);
            Aircraft savedAircraft;
            Optional<Aircraft> existing = aircraftRepo.findByTailNumber(tailNumber);
            if (existing.isPresent()) {
                savedAircraft = existing.get();
            } else {
                Aircraft newAircraft = new Aircraft();
                newAircraft.setTailNumber(tailNumber);
                newAircraft.setModel(model);
                savedAircraft = aircraftRepo.save(newAircraft);
            }

            // Parse live position (alt_baro may be "ground" string — handle gracefully)
            Map<String, Object> livePosition = new LinkedHashMap<>();
            livePosition.put("hex", ac.path("hex").asText(null));
            livePosition.put("callsign", ac.path("flight").asText(null));
            livePosition.put("lat", ac.path("lat").isNumber() ? ac.path("lat").asDouble() : null);
            livePosition.put("lon", ac.path("lon").isNumber() ? ac.path("lon").asDouble() : null);
            JsonNode altNode = ac.path("alt_baro");
            livePosition.put("altitude", altNode.isNumber() ? altNode.asInt() : altNode.asText(null));
            livePosition.put("speed", ac.path("gs").isNumber() ? ac.path("gs").asDouble() : null);
            livePosition.put("heading", ac.path("track").isNumber() ? ac.path("track").asDouble() : null);
            livePosition.put("type", model);
            livePosition.put("desc", ac.path("desc").asText(null));

            result.put("source", "airplanes.live");
            result.put("aircraft", savedAircraft);
            result.put("livePosition", livePosition);
            return result;

        } catch (Exception e) {
            result.put("source", "none");
            result.put("message", "airplanes.live request failed: " + e.getMessage());
            return result;
        }
    }

    // --- Live position lookup (direct, no DB write) ---

    public Map<String, Object> fetchLivePosition(String tailNumber) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            ResponseEntity<String> liveResponse = airplanesLiveService.getByReg(tailNumber);
            if (!liveResponse.getStatusCode().is2xxSuccessful() || liveResponse.getBody() == null) {
                result.put("tailNumber", tailNumber);
                result.put("airborne", false);
                result.put("message", "No response from airplanes.live");
                return result;
            }

            JsonNode liveRoot = mapper.readTree(liveResponse.getBody());
            JsonNode acArray = liveRoot.path("ac");

            if (!acArray.isArray() || acArray.isEmpty()) {
                result.put("tailNumber", tailNumber);
                result.put("airborne", false);
                result.put("message", "Aircraft not currently tracked");
                return result;
            }

            JsonNode ac = acArray.get(0);

            // Enrich with DB owner/model data if available
            Optional<Aircraft> dbAircraft = aircraftRepo.findByTailNumber(tailNumber);
            if (dbAircraft.isPresent()) {
                result.put("dbAircraftId", dbAircraft.get().getId());
                result.put("model", dbAircraft.get().getModel());
                result.put("ownerId", dbAircraft.get().getOwnerId());
            }

            JsonNode altNode = ac.path("alt_baro");
            result.put("tailNumber", tailNumber);
            result.put("airborne", true);
            result.put("hex", ac.path("hex").asText(null));
            result.put("callsign", ac.path("flight").asText(null));
            result.put("lat", ac.path("lat").isNumber() ? ac.path("lat").asDouble() : null);
            result.put("lon", ac.path("lon").isNumber() ? ac.path("lon").asDouble() : null);
            result.put("altitude", altNode.isNumber() ? altNode.asInt() : altNode.asText(null));
            result.put("speed", ac.path("gs").isNumber() ? ac.path("gs").asDouble() : null);
            result.put("heading", ac.path("track").isNumber() ? ac.path("track").asDouble() : null);
            result.put("type", ac.path("t").asText(null));
            result.put("desc", ac.path("desc").asText(null));
            return result;

        } catch (Exception e) {
            result.put("tailNumber", tailNumber);
            result.put("airborne", false);
            result.put("message", "airplanes.live request failed: " + e.getMessage());
            return result;
        }
    }

    // --- Save live position to DB (fetch from airplanes.live, write to aircraft row) ---

    public Map<String, Object> saveLivePosition(String tailNumber) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            ResponseEntity<String> liveResponse = airplanesLiveService.getByReg(tailNumber);
            if (!liveResponse.getStatusCode().is2xxSuccessful() || liveResponse.getBody() == null) {
                result.put("tailNumber", tailNumber);
                result.put("saved", false);
                result.put("message", "No response from airplanes.live");
                return result;
            }

            JsonNode liveRoot = mapper.readTree(liveResponse.getBody());
            JsonNode acArray = liveRoot.path("ac");

            // Find or create aircraft row
            Aircraft aircraft = aircraftRepo.findByTailNumber(tailNumber).orElseGet(() -> {
                Aircraft a = new Aircraft();
                a.setTailNumber(tailNumber);
                return a;
            });

            if (!acArray.isArray() || acArray.isEmpty()) {
                aircraft.setAirborne(false);
                aircraftRepo.save(aircraft);
                result.put("tailNumber", tailNumber);
                result.put("airborne", false);
                result.put("saved", true);
                result.put("message", "Not airborne — airborne=false written to DB");
                return result;
            }

            JsonNode ac = acArray.get(0);
            aircraft.setAirborne(true);
            aircraft.setLastHex(ac.path("hex").asText(null));
            aircraft.setLastCallsign(ac.path("flight").asText(null));
            aircraft.setLastLat(ac.path("lat").isNumber() ? ac.path("lat").asDouble() : null);
            aircraft.setLastLon(ac.path("lon").isNumber() ? ac.path("lon").asDouble() : null);
            JsonNode altNode = ac.path("alt_baro");
            aircraft.setLastAltitude(altNode.isNumber() ? altNode.asInt() : null);
            aircraft.setLastSpeed(ac.path("gs").isNumber() ? ac.path("gs").asDouble() : null);
            aircraft.setLastHeading(ac.path("track").isNumber() ? ac.path("track").asDouble() : null);
            aircraft.setLastSeen(LocalDateTime.now());
            aircraftRepo.save(aircraft);

            result.put("tailNumber", tailNumber);
            result.put("airborne", true);
            result.put("saved", true);
            result.put("aircraft", aircraft);
            return result;

        } catch (Exception e) {
            result.put("tailNumber", tailNumber);
            result.put("saved", false);
            result.put("message", "Request failed: " + e.getMessage());
            return result;
        }
    }

    // --- Live aircraft read endpoint (for map polling) ---

    public List<Map<String, Object>> getLiveAircraft(String category) {
        Iterable<Aircraft> source = (category != null && !category.isBlank())
                ? aircraftRepo.findByCategory(category)
                : aircraftRepo.findAll();

        // Collect aircraft that have a known position
        List<Aircraft> positioned = new ArrayList<>();
        for (Aircraft a : source) {
            if (a.getLastLat() == null || a.getLastLon() == null) continue;
            positioned.add(a);
        }

        // Batch-fetch the most recent CO2 value for each aircraft in one query
        List<Long> ids = positioned.stream()
                .map(Aircraft::getId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toList());
        Map<Long, Double> co2ByAircraftId = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Flight f : flightRepo.findByAircraftIdIn(ids)) {
                // findByAircraftIdIn returns newest-first; first entry per aircraft wins
                if (f.getEstimatedCo2Kg() != null && !co2ByAircraftId.containsKey(f.getAircraftId())) {
                    co2ByAircraftId.put(f.getAircraftId(), f.getEstimatedCo2Kg());
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Aircraft a : positioned) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tailNumber", a.getTailNumber());
            item.put("lastLat", a.getLastLat());
            item.put("lastLon", a.getLastLon());
            item.put("lastHeading", a.getLastHeading());
            item.put("lastCallsign", a.getLastCallsign());
            item.put("airborne", a.getAirborne());
            item.put("lastSeen", a.getLastSeen());
            item.put("category", a.getCategory());
            // Use the stored CO2 from the most recent flight when available.
            // Fall back to a velocity-based estimate for airborne aircraft that have
            // no stored route CO2 (e.g. celebrity aircraft whose flights were fetched
            // without airport coordinates, or aircraft only seen via live position feed).
            Double co2 = co2ByAircraftId.get(a.getId());
            if (co2 == null && Boolean.TRUE.equals(a.getAirborne())
                    && a.getLastSpeed() != null && a.getLastSpeed() > 0) {
                co2 = environmentalService.estimateCo2KgFromVelocity(
                        a.getLastSpeed().intValue(),
                        LocalDateTime.now().minusHours(2));
            }
            item.put("estimatedCo2Kg", co2);
            result.add(item);
        }
        return result;
    }

    // --- Save live positions for all aircraft in DB (manual trigger / DbExplorer) ---

    public Map<String, Object> saveLiveAll() {
        Iterable<Aircraft> all = aircraftRepo.findAll();
        int saved = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Aircraft a : all) {
            if (a.getTailNumber() == null || a.getTailNumber().isBlank()) { skipped++; continue; }
            try {
                Map<String, Object> r = saveLivePosition(a.getTailNumber());
                if (Boolean.TRUE.equals(r.get("saved"))) saved++; else skipped++;
                Thread.sleep(1000); // 1 req/s rate limit
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                errors.add(a.getTailNumber() + ": " + e.getMessage());
                skipped++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("saved", saved);
        result.put("skipped", skipped);
        if (!errors.isEmpty()) result.put("errors", errors);
        return result;
    }

    // --- Celebrity flight history (fetches from AeroAPI for each owned aircraft, persists to DB) ---

    /**
     * Returns flight history for a celebrity.
     *
     * Persistence strategy:
     * - Delegates per-aircraft fetching to {@link #fetchAndSaveFlight}, which is DB-first:
     *   it returns cached flights immediately on repeat calls and only hits AeroAPI when the
     *   aircraft has no stored flights yet.
     * - When {@code userId} is provided, an entry is upserted in {@code user_celebrity_cache}
     *   so the user's history can be served cheaply on their next login.
     */
    public Map<String, Object> getCelebrityFlights(Long celebrityId, Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        Optional<Celebrity> celebOpt = celebrityRepo.findById(celebrityId);
        if (celebOpt.isEmpty()) {
            // Celebrity no longer exists in the DB (e.g. deleted between list load and click).
            // Return an empty flights response rather than leaking an internal error message.
            result.put("flights", Collections.emptyList());
            result.put("flightCount", 0);
            result.put("aircraftCount", 0);
            return result;
        }
        Celebrity celeb = celebOpt.get();
        result.put("celebrity", celeb.getName());
        result.put("industry", celeb.getIndustry());

        List<Aircraft> owned = aircraftRepo.findByOwnerId(celebrityId);
        if (owned.isEmpty()) {
            result.put("flights", Collections.emptyList());
            result.put("message", "No aircraft linked to this celebrity");
            return result;
        }

        List<Map<String, Object>> allFlights = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Aircraft aircraft : owned) {
            String tail = aircraft.getTailNumber();
            if (tail == null || tail.isBlank()) continue;

            // fetchAndSaveFlight is DB-first: on first call it fetches AeroAPI and saves to DB;
            // on subsequent calls it returns the persisted flights directly.
            Map<String, Object> fetched = fetchAndSaveFlight(tail);

            @SuppressWarnings("unchecked")
            List<Flight> flights = (List<Flight>) fetched.get("flights");
            if (flights != null) {
                for (Flight f : flights) {
                    allFlights.add(buildFlightRow(f, aircraft));
                }
            } else if (fetched.containsKey("error")) {
                errors.add(tail + ": " + fetched.get("error"));
            }
        }

        // Drop degenerate rows that have no route information at all (both airports null).
        // These are DB artefacts from partial saves and have no meaningful display value.
        allFlights.removeIf(row ->
            row.get("origin") == null && row.get("destination") == null
        );

        // Newest first
        allFlights.sort((a, b) -> {
            String ta = String.valueOf(a.getOrDefault("departureTime", ""));
            String tb = String.valueOf(b.getOrDefault("departureTime", ""));
            return tb.compareTo(ta);
        });

        // Record that this user has viewed this celebrity's flights
        if (userId != null) {
            recordUserCelebrityView(userId, celebrityId);
        }

        result.put("aircraftCount", owned.size());
        result.put("flights", allFlights);
        result.put("flightCount", allFlights.size());
        result.put("cachedForUser", userId != null);
        if (!errors.isEmpty()) result.put("errors", errors);
        return result;
    }

    /**
     * Builds a serialization-friendly flight row, enriched with airport city/name from the DB.
     */
    private Map<String, Object> buildFlightRow(Flight f, Aircraft aircraft) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("flightId", f.getId());
        row.put("source", "database");
        row.put("tailNumber", aircraft.getTailNumber());
        row.put("model", aircraft.getModel());
        row.put("origin", f.getOriginApCode());
        row.put("destination", f.getDestinationApCode());
        if (f.getOriginApCode() != null) {
            airportRepo.findById(f.getOriginApCode()).ifPresent(a -> {
                row.put("originCity", a.getCity());
                row.put("originName", a.getName());
            });
        }
        if (f.getDestinationApCode() != null) {
            airportRepo.findById(f.getDestinationApCode()).ifPresent(a -> {
                row.put("destinationCity", a.getCity());
                row.put("destinationName", a.getName());
            });
        }
        row.put("departureTime", f.getDepartureTime());
        row.put("arrivalTime", f.getArrivalTime());

        // Use the stored estimate when available; otherwise compute on-the-fly from
        // airport coordinates so celebrity flights always show a CO2 figure when
        // both airports have lat/lon in the DB (coordinates may arrive later than
        // the initial AeroAPI save).
        Double co2 = f.getEstimatedCo2Kg();
        if (co2 == null && f.getOriginApCode() != null && f.getDestinationApCode() != null) {
            Airport origin = airportRepo.findById(f.getOriginApCode()).orElse(null);
            Airport dest   = airportRepo.findById(f.getDestinationApCode()).orElse(null);
            co2 = environmentalService.estimateCo2Kg(origin, dest);
        }
        row.put("estimatedCo2Kg", co2);
        return row;
    }

    /** Upserts the (userId, celebrityId) row in user_celebrity_cache with the current timestamp. */
    private void recordUserCelebrityView(Long userId, Long celebrityId) {
        userCelebrityCacheRepo.findByUserIdAndCelebrityId(userId, celebrityId).ifPresentOrElse(
            cache -> {
                cache.setLastFetchedAt(LocalDateTime.now());
                userCelebrityCacheRepo.save(cache);
            },
            () -> {
                com.example.demo.entity.UserCelebrityCache cache =
                        new com.example.demo.entity.UserCelebrityCache();
                cache.setUserId(userId);
                cache.setCelebrityId(celebrityId);
                cache.setLastFetchedAt(LocalDateTime.now());
                userCelebrityCacheRepo.save(cache);
            }
        );
    }

    /**
     * Returns all celebrities a user has previously viewed, with their DB-cached flight history.
     *
     * Why DB-only:
     * - Called at login time to pre-warm the UI with previously fetched data.
     * - No AeroAPI calls are made; the response is always fast.
     */
    public List<Map<String, Object>> getUserCelebrityHistory(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<com.example.demo.entity.UserCelebrityCache> caches =
                userCelebrityCacheRepo.findByUserId(userId);

        for (com.example.demo.entity.UserCelebrityCache cache : caches) {
            Celebrity celeb = celebrityRepo.findById(cache.getCelebrityId()).orElse(null);
            if (celeb == null) continue;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("celebrityId", celeb.getId());
            entry.put("celebrity", celeb.getName());
            entry.put("industry", celeb.getIndustry());
            entry.put("lastFetchedAt", cache.getLastFetchedAt());

            List<Map<String, Object>> flights = new ArrayList<>();
            for (Aircraft aircraft : aircraftRepo.findByOwnerId(celeb.getId())) {
                for (Flight f : flightRepo.findByAircraftId(aircraft.getId())) {
                    flights.add(buildFlightRow(f, aircraft));
                }
            }
            flights.sort((a, b) -> {
                String ta = String.valueOf(a.getOrDefault("departureTime", ""));
                String tb = String.valueOf(b.getOrDefault("departureTime", ""));
                return tb.compareTo(ta);
            });

            entry.put("flights", flights);
            entry.put("flightCount", flights.size());
            result.add(entry);
        }

        // Most recently viewed celebrity first
        result.sort((a, b) -> {
            String ta = String.valueOf(a.getOrDefault("lastFetchedAt", ""));
            String tb = String.valueOf(b.getOrDefault("lastFetchedAt", ""));
            return tb.compareTo(ta);
        });

        return result;
    }

    // --- Live celebrity status (celebrities joined with their aircraft's live position) ---

    public List<Map<String, Object>> getLiveCelebrities() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Celebrity celeb : celebrityRepo.findAll()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", celeb.getId());
            item.put("name", celeb.getName());
            item.put("industry", celeb.getIndustry());

            List<Aircraft> owned = aircraftRepo.findByOwnerId(celeb.getId());
            if (owned.isEmpty()) {
                item.put("airborne", null);
                item.put("lastLat", null);
                item.put("lastLon", null);
                item.put("lastSeen", null);
                item.put("tailNumber", null);
                item.put("model", null);
            } else {
                // Prefer the aircraft with the most recent lastSeen; fall back to first row
                Aircraft a = owned.stream()
                    .filter(ac -> ac.getLastSeen() != null)
                    .max(Comparator.comparing(Aircraft::getLastSeen))
                    .orElse(owned.get(0));
                item.put("airborne", a.getAirborne());
                item.put("lastLat", a.getLastLat());
                item.put("lastLon", a.getLastLon());
                item.put("lastSeen", a.getLastSeen());
                item.put("tailNumber", a.getTailNumber());
                item.put("model", a.getModel());
            }
            result.add(item);
        }
        return result;
    }

    // --- Bulk save live positions from a category endpoint (mil / ladd / pia) ---

    public Map<String, Object> bulkSaveAircraftPositions(ResponseEntity<String> response, String category) {
        Map<String, Object> result = new LinkedHashMap<>();
        int saved = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        try {
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                result.put("saved", 0);
                result.put("message", "No response from airplanes.live for category: " + category);
                return result;
            }

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode acArray = root.path("ac");

            if (!acArray.isArray()) {
                result.put("saved", 0);
                result.put("message", "Unexpected response shape — no 'ac' array");
                return result;
            }

            for (JsonNode ac : acArray) {
                try {
                    // Prefer registration ("r") as tail number; fall back to hex
                    String reg = ac.path("r").asText(null);
                    String hex = ac.path("hex").asText(null);
                    String key = (reg != null && !reg.isBlank()) ? reg : hex;
                    if (key == null || key.isBlank()) { skipped++; continue; }

                    Aircraft aircraft = (reg != null && !reg.isBlank())
                        ? aircraftRepo.findByTailNumber(reg).orElseGet(() -> { Aircraft a = new Aircraft(); a.setTailNumber(reg); return a; })
                        : aircraftRepo.findByLastHex(hex).orElseGet(() -> { Aircraft a = new Aircraft(); a.setLastHex(hex); return a; });

                    aircraft.setCategory(category);
                    aircraft.setAirborne(true);
                    if (hex != null && !hex.isBlank()) aircraft.setLastHex(hex);
                    aircraft.setLastCallsign(ac.path("flight").asText(null));
                    aircraft.setLastLat(ac.path("lat").isNumber() ? ac.path("lat").asDouble() : null);
                    aircraft.setLastLon(ac.path("lon").isNumber() ? ac.path("lon").asDouble() : null);
                    JsonNode altNode = ac.path("alt_baro");
                    aircraft.setLastAltitude(altNode.isNumber() ? altNode.asInt() : null);
                    aircraft.setLastSpeed(ac.path("gs").isNumber() ? ac.path("gs").asDouble() : null);
                    aircraft.setLastHeading(ac.path("track").isNumber() ? ac.path("track").asDouble() : null);
                    aircraft.setLastSeen(LocalDateTime.now());

                    aircraftRepo.save(aircraft);
                    saved++;
                } catch (Exception e) {
                    errors.add(e.getMessage());
                    skipped++;
                }
            }

            result.put("category", category);
            result.put("saved", saved);
            result.put("skipped", skipped);
            if (!errors.isEmpty()) result.put("errors", errors);
            return result;

        } catch (Exception e) {
            result.put("saved", 0);
            result.put("message", "Failed to parse response: " + e.getMessage());
            return result;
        }
    }

    // --- One-shot commercial flight sync (called by manual "Load" button) ---

    public Map<String, Object> syncCommercialFlights() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("query", "-airline AAL");
            params.put("max_pages", "1");
            ResponseEntity<String> response = flightService.searchFlights(params);
            return bulkSaveAeroApiPositions(response, "commercial");
        } catch (Exception e) {
            result.put("saved", 0);
            result.put("message", "Sync failed: " + e.getMessage());
            return result;
        }
    }

    // --- Bulk save live positions from AeroAPI /flights/search/positions ---

    public Map<String, Object> bulkSaveAeroApiPositions(ResponseEntity<String> response, String category) {
        Map<String, Object> result = new LinkedHashMap<>();
        int saved = 0, skipped = 0;

        try {
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                result.put("saved", 0);
                result.put("message", "No response from AeroAPI: " + response.getStatusCode());
                return result;
            }

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode flights = root.path("flights");

            if (!flights.isArray()) {
                result.put("saved", 0);
                result.put("message", "Unexpected AeroAPI response shape — no 'flights' array. Body: " + response.getBody().substring(0, Math.min(200, response.getBody().length())));
                return result;
            }

            for (JsonNode flight : flights) {
                try {
                    String ident = flight.path("ident").asText(null);
                    if (ident == null || ident.isBlank()) { skipped++; continue; }

                    JsonNode pos = flight.path("last_position");
                    if (pos.isMissingNode() || !pos.path("latitude").isNumber()) { skipped++; continue; }

                    Aircraft aircraft = aircraftRepo.findByTailNumber(ident)
                        .orElseGet(() -> { Aircraft a = new Aircraft(); a.setTailNumber(ident); return a; });

                    aircraft.setCategory(category);
                    aircraft.setAirborne(true);
                    aircraft.setLastCallsign(ident);
                    aircraft.setLastLat(pos.path("latitude").asDouble());
                    aircraft.setLastLon(pos.path("longitude").asDouble());
                    JsonNode altNode = pos.path("altitude");
                    if (altNode.isNumber()) aircraft.setLastAltitude(altNode.asInt());
                    JsonNode gsNode = pos.path("groundspeed");
                    if (gsNode.isNumber()) aircraft.setLastSpeed(gsNode.asDouble());
                    JsonNode hdgNode = pos.path("heading");
                    if (hdgNode.isNumber()) aircraft.setLastHeading(hdgNode.asDouble());
                    aircraft.setLastSeen(LocalDateTime.now());

                    aircraftRepo.save(aircraft);
                    saved++;
                } catch (Exception e) {
                    skipped++;
                }
            }

            result.put("category", category);
            result.put("saved", saved);
            result.put("skipped", skipped);
            return result;

        } catch (Exception e) {
            result.put("saved", 0);
            result.put("message", "Failed to parse AeroAPI response: " + e.getMessage());
            return result;
        }
    }

    // --- CO2 summary and recalculation ---

    /**
     * Return CO2 stats for a flight, including a commercial-route comparison.
     *
     * Why this lives in the service layer:
     * - It combines a flight DB lookup with {@link EnvironmentalService} logic, and needs access
     *   to both the airport repo (for coordinates) and the environmental calculations.
     *
     * @param flightId primary key of the flight to look up
     * @return map with co2, distance, and comparison stats; or a "not found"/"no data" map
     */
    public Map<String, Object> getCo2Summary(Long flightId) {
        Optional<Flight> flightOpt = flightRepo.findById(flightId);
        if (flightOpt.isEmpty()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("error", "Flight not found: " + flightId);
            return r;
        }

        Flight flight = flightOpt.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flightId", flightId);
        result.put("origin", flight.getOriginApCode());
        result.put("destination", flight.getDestinationApCode());
        result.put("estimatedCo2Kg", flight.getEstimatedCo2Kg());

        // Build comparison stats when we have coordinates for both airports
        Airport origin = flight.getOriginApCode() != null
                ? airportRepo.findById(flight.getOriginApCode()).orElse(null) : null;
        Airport dest = flight.getDestinationApCode() != null
                ? airportRepo.findById(flight.getDestinationApCode()).orElse(null) : null;

        if (origin != null && dest != null
                && origin.getLat() != null && origin.getLon() != null
                && dest.getLat() != null && dest.getLon() != null) {
            double distanceKm = environmentalService.haversineDistanceKm(
                    origin.getLat(), origin.getLon(), dest.getLat(), dest.getLon());
            result.put("comparison", environmentalService.compareWithCommercial(distanceKm));
        } else {
            result.put("comparison", null);
        }

        return result;
    }

    /**
     * Recalculate and persist the CO2 estimate for a specific flight.
     *
     * Useful when airport coordinates have been added after the flight was saved,
     * or to correct a previously missing estimate.
     *
     * @param flightId primary key of the flight to update
     * @return updated flight, or an error map
     */
    public Map<String, Object> recalculateCo2(Long flightId) {
        Optional<Flight> flightOpt = flightRepo.findById(flightId);
        if (flightOpt.isEmpty()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("error", "Flight not found: " + flightId);
            return r;
        }

        Flight flight = flightOpt.get();
        Airport origin = flight.getOriginApCode() != null
                ? airportRepo.findById(flight.getOriginApCode()).orElse(null) : null;
        Airport dest = flight.getDestinationApCode() != null
                ? airportRepo.findById(flight.getDestinationApCode()).orElse(null) : null;

        Double co2 = environmentalService.estimateCo2Kg(origin, dest);
        flight.setEstimatedCo2Kg(co2);
        flightRepo.save(flight);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flightId", flightId);
        result.put("estimatedCo2Kg", co2);
        result.put("recalculated", true);
        return result;
    }

    private LocalDateTime parseTimestamp(JsonNode node, String preferredField, String fallbackField) {
        String value = null;
        if (!node.path(preferredField).isMissingNode() && !node.path(preferredField).isNull()) {
            value = node.path(preferredField).asText(null);
        } else if (!node.path(fallbackField).isMissingNode() && !node.path(fallbackField).isNull()) {
            value = node.path(fallbackField).asText(null);
        }
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(value);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
