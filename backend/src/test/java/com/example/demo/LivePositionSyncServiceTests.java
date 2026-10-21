package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LivePositionSyncServiceTests {

    @Mock
    private AirplanesLiveService liveService;

    @Mock
    private DbService dbService;

    @InjectMocks
    private LivePositionSyncService syncService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -------------------------------------------------------------------------
    // constructor / bean instantiation
    // -------------------------------------------------------------------------

    @Test
    void serviceCanBeInstantiated() {
        // Verifies the no-arg bean wiring path is exercised (covers the constructor line)
        LivePositionSyncService instance = new LivePositionSyncService();
        // Just needs to not throw
    }

    // -------------------------------------------------------------------------
    // syncMilitary
    // -------------------------------------------------------------------------

    @Test
    void syncMilitaryFetchesFromLiveServiceAndDelegatesToDbService() {
        ResponseEntity<String> milResponse = ResponseEntity.ok("{\"ac\":[{\"mil\":true}]}");
        when(liveService.getMilitary()).thenReturn(milResponse);

        syncService.syncMilitary();

        verify(liveService).getMilitary();
        verify(dbService).bulkSaveAircraftPositions(milResponse, "mil");
    }

    // -------------------------------------------------------------------------
    // syncLadd
    // -------------------------------------------------------------------------

    @Test
    void syncLaddFetchesFromLiveServiceAndDelegatesToDbService() {
        ResponseEntity<String> laddResponse = ResponseEntity.ok("{\"ac\":[]}");
        when(liveService.getLadd()).thenReturn(laddResponse);

        syncService.syncLadd();

        verify(liveService).getLadd();
        verify(dbService).bulkSaveAircraftPositions(laddResponse, "ladd");
    }

    // -------------------------------------------------------------------------
    // syncPia
    // -------------------------------------------------------------------------

    @Test
    void syncPiaFetchesFromLiveServiceAndDelegatesToDbService() {
        ResponseEntity<String> piaResponse = ResponseEntity.ok("{\"ac\":[]}");
        when(liveService.getPia()).thenReturn(piaResponse);

        syncService.syncPia();

        verify(liveService).getPia();
        verify(dbService).bulkSaveAircraftPositions(piaResponse, "pia");
    }
}
