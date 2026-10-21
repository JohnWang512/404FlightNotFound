package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Background scheduler that pulls live aircraft positions from airplanes.live
 * and persists them to the aircraft table every 5 minutes.
 *
 * Three jobs run at staggered offsets so they don't all hit the external API
 * at the same moment:
 *   - Military (/mil)  → fires at startup, then every sync.interval.ms
 *   - LADD    (/ladd)  → fires 60 s after startup, then every sync.interval.ms
 *   - PIA     (/pia)   → fires 120 s after startup, then every sync.interval.ms
 *
 * Requires @EnableScheduling on BackendApplication (already added).
 */
@Service
public class LivePositionSyncService {

    @Autowired
    private AirplanesLiveService liveService;

    @Autowired
    private DbService dbService;

    /** Sync military aircraft — fires immediately on startup, then every 5 min. */
    @Scheduled(fixedRateString = "${sync.interval.ms:300000}", initialDelay = 0)
    public void syncMilitary() {
        dbService.bulkSaveAircraftPositions(liveService.getMilitary(), "mil");
    }

    /** Sync LADD aircraft — starts 1 min after startup, then every 5 min. */
    @Scheduled(fixedRateString = "${sync.interval.ms:300000}", initialDelayString = "60000")
    public void syncLadd() {
        dbService.bulkSaveAircraftPositions(liveService.getLadd(), "ladd");
    }

    /** Sync PIA aircraft — starts 2 min after startup, then every 5 min. */
    @Scheduled(fixedRateString = "${sync.interval.ms:300000}", initialDelayString = "120000")
    public void syncPia() {
        dbService.bulkSaveAircraftPositions(liveService.getPia(), "pia");
    }
}
