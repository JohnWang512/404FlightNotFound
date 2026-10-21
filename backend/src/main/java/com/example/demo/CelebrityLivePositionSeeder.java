package com.example.demo;

import com.example.demo.entity.Aircraft;
import com.example.demo.repository.AircraftRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Seeds hardcoded live-position snapshots for celebrity aircraft that have no known position.
 *
 * Why this exists:
 * - The live-position fields are normally written by DbService.saveLivePosition(), which calls
 *   the airplanes.live API.  Celebrity jets are often LADD-blocked or simply not broadcasting,
 *   so many aircraft rows have null lastLat/lastLon and never appear on the globe/map.
 * - This seeder fills in plausible static positions so the Celebrity filter always shows
 *   every tracked aircraft on the map, regardless of API availability.
 * - Only aircraft with a null lastLat are updated — real live-position data (written by the
 *   sync service) is never overwritten.
 *
 * @Order(10) ensures this runs after CelebritySeeder (@Order unspecified = lowest priority first).
 */
@Order(10)
@Component
public class CelebrityLivePositionSeeder implements ApplicationRunner {

    private final AircraftRepository aircraftRepo;

    public CelebrityLivePositionSeeder(AircraftRepository aircraftRepo) {
        this.aircraftRepo = aircraftRepo;
    }

    /**
     * Hardcoded position data keyed by tail number.
     * Fields: lat, lon, heading (degrees), altitude (ft), speed (kts), callsign
     *
     * Positions are chosen to reflect each owner's real-world associations:
     * - American celebrities fly over / depart from their home cities.
     * - Max Verstappen (Dutch) routes through Western Europe.
     */
    private static final Map<String, double[]> POSITIONS = Map.of(
            // Caesars Palace Casino — Las Vegas to New York (over Arizona)
            "N898CE", new double[]{ 35.20, -112.80,  85, 37000, 455 },

            // Max Verstappen — Amsterdam to Monaco (over southern France)
            "PH-DFT", new double[]{ 46.50,    5.80, 170, 41000, 470 },

            // Steve Ballmer — Seattle to Los Angeles (over Oregon)
            "N709DS", new double[]{ 43.50, -122.65, 185, 39000, 460 },

            // Sergey Brin — San Francisco Bay Area heading east
            "N232G",  new double[]{ 37.62, -122.38,  80, 44000, 490 },

            // Mark Cuban — Dallas to New York (over Tennessee)
            "N921MT", new double[]{ 35.80,  -88.60,  82, 40000, 455 },

            // Michael Bloomberg — New York area heading south
            "N5MV",   new double[]{ 40.64,  -73.78, 190, 38000, 440 },

            // Dan Bilzerian — Las Vegas to Miami (over New Mexico)
            "N701DB", new double[]{ 34.20, -106.65,  98, 41000, 465 }
    );

    // Extra entries beyond Map.of()'s 10-key limit
    private static final Map<String, double[]> POSITIONS_EXTRA = Map.of(
            // Taylor Swift — New York to Nashville (over Pennsylvania)
            "N898TS", new double[]{ 40.50,  -76.50, 230, 41000, 480 },

            // Elon Musk — Austin TX to Washington DC (over Arkansas)
            "N628TS", new double[]{ 35.20,  -92.40,  82, 43000, 490 }
    );

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();

        // Step 1: Ensure every celebrity-owned aircraft has category="celebrity" so the
        // Celebrity filter on the map works correctly.
        for (Aircraft a : aircraftRepo.findAll()) {
            if (a.getOwnerId() == null) continue;
            if ("celebrity".equals(a.getCategory())) continue;
            a.setCategory("celebrity");
            aircraftRepo.save(a);
        }

        // Step 2: Seed hardcoded positions for any aircraft that have no live-position data.
        Map<String, double[]> allPositions = new java.util.HashMap<>(POSITIONS);
        allPositions.putAll(POSITIONS_EXTRA);
        for (Map.Entry<String, double[]> entry : allPositions.entrySet()) {
            String tailNumber = entry.getKey();
            double[] p = entry.getValue();

            Optional<Aircraft> opt = aircraftRepo.findByTailNumber(tailNumber);
            if (opt.isEmpty()) continue;

            Aircraft a = opt.get();
            if (a.getLastLat() != null) continue; // real data already present — don't overwrite

            a.setLastLat(p[0]);
            a.setLastLon(p[1]);
            a.setLastHeading(p[2]);
            a.setLastAltitude((int) p[3]);
            a.setLastSpeed(p[4]);
            a.setAirborne(true);
            a.setLastCallsign(tailNumber);
            a.setLastSeen(now);

            aircraftRepo.save(a);
        }
    }
}
