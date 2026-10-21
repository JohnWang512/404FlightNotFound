package com.example.demo;

import com.example.demo.entity.Aircraft;
import com.example.demo.entity.Airport;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Celebrity;
import com.example.demo.entity.Flight;
import com.example.demo.entity.UserCelebrityCache;
import com.example.demo.entity.UserTrackedFlight;
import com.example.demo.repository.AircraftRepository;
import com.example.demo.repository.AirportRepository;
import com.example.demo.repository.CelebrityRepository;
import com.example.demo.repository.FlightRepository;
import com.example.demo.repository.UserCelebrityCacheRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserTrackedFlightRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;



@Tag("manual_backend")
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:manualdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({DbService.class, EnvironmentalService.class, DbServiceManualDataJpaTests.MockConfig.class})
class DbServiceManualDataJpaTests {

    @Autowired
    private DbService dbService;

    @Autowired
    private CelebrityRepository celebrityRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlightService flightService;

    @Autowired
    private AirplanesLiveService airplanesLiveService;

    @Autowired
    private UserCelebrityCacheRepository userCelebrityCacheRepository;

    @Autowired
    private UserTrackedFlightRepository userTrackedFlightRepository;

    @Test
    void getStatusReturnsRepositoryCounts() {
        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N11111");
        aircraftRepository.save(aircraft);

        assertThat(dbService.getStatus())
                .containsEntry("status", "ok")
                .containsEntry("celebrities", 0L)
                .containsEntry("aircraft", 1L)
                .containsEntry("airports", 0L)
                .containsEntry("flights", 0L)
                .containsEntry("users", 0L);
    }

    @Test
    void saveCelebrityPersistsEntity() {
        Celebrity celebrity = new Celebrity();
        celebrity.setName("Taylor Swift");
        celebrity.setIndustry("Music");

        Celebrity saved = dbService.saveCelebrity(celebrity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Taylor Swift");
        assertThat(celebrityRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void saveAircraftPersistsEntity() {
        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N90001");

        Aircraft saved = dbService.saveAircraft(aircraft);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTailNumber()).isEqualTo("N90001");
        assertThat(aircraftRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void saveAirportPersistsEntity() {
        Airport airport = new Airport();
        airport.setApCode("KMSN");
        airport.setName("Dane County Regional");
        airport.setCity("Madison");
        airport.setCountry("USA");

        Airport saved = dbService.saveAirport(airport);

        assertThat(saved.getApCode()).isEqualTo("KMSN");
        assertThat(saved.getName()).isEqualTo("Dane County Regional");
        assertThat(airportRepository.findById("KMSN")).isPresent();
    }

    @Test
    void saveFlightPersistsEntity() {
        Flight flight = new Flight();
        flight.setCallsign("UAL100");
        flight.setOriginApCode("KMSN");
        flight.setDestinationApCode("KORD");
        flight.setDepartureTime(LocalDateTime.now());

        Flight saved = dbService.saveFlight(flight);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCallsign()).isEqualTo("UAL100");
        assertThat(flightRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void saveUserPersistsEntity() {
        AppUser user = new AppUser();
        user.setUname("chance");
        user.setUpass("secret");

        AppUser saved = dbService.saveUser(user);

        assertThat(saved.getUId()).isNotNull();
        assertThat(saved.getUname()).isEqualTo("chance");
        assertThat(userRepository.findById(saved.getUId())).isPresent();
    }

    @Test
    void getCelebrityByIdReturnsSavedEntity() {
        Celebrity celebrity = new Celebrity();
        celebrity.setName("Zendaya");
        celebrity.setIndustry("Film");
        celebrity = celebrityRepository.save(celebrity);

        var result = dbService.getCelebrityById(celebrity.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Zendaya");
    }

    @Test
    void getAircraftByIdReturnsSavedEntity() {
        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N70001");
        aircraft = aircraftRepository.save(aircraft);

        var result = dbService.getAircraftById(aircraft.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTailNumber()).isEqualTo("N70001");
    }

    @Test
    void getAirportByCodeReturnsSavedEntity() {
        Airport airport = new Airport();
        airport.setApCode("KORD");
        airport.setName("OHare");
        airport.setCity("Chicago");
        airport.setCountry("USA");
        airportRepository.save(airport);

        var result = dbService.getAirportByCode("KORD");

        assertThat(result).isPresent();
        assertThat(result.get().getApCode()).isEqualTo("KORD");
    }

    @Test
    void getFlightByIdReturnsSavedEntity() {
        Flight flight = new Flight();
        flight.setCallsign("DAL200");
        flight.setDepartureTime(LocalDateTime.now());
        flight = flightRepository.save(flight);

        var result = dbService.getFlightById(flight.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getCallsign()).isEqualTo("DAL200");
    }

    @Test
    void getUserByIdReturnsSavedEntity() {
        AppUser user = new AppUser();
        user.setUname("spencer");
        user.setUpass("pw");
        user = userRepository.save(user);

        var result = dbService.getUserById(user.getUId());

        assertThat(result).isPresent();
        assertThat(result.get().getUname()).isEqualTo("spencer");
    }

    @Test
    void getAllCelebritiesReturnsSavedRows() {
        Celebrity a = new Celebrity();
        a.setName("A");
        Celebrity b = new Celebrity();
        b.setName("B");
        celebrityRepository.saveAll(List.of(a, b));

        var result = dbService.getAllCelebrities();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllAircraftReturnsSavedRows() {
        Aircraft a = new Aircraft();
        a.setTailNumber("N10001");
        Aircraft b = new Aircraft();
        b.setTailNumber("N10002");
        aircraftRepository.saveAll(List.of(a, b));

        var result = dbService.getAllAircraft();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllAirportsReturnsSavedRows() {
        Airport a = new Airport();
        a.setApCode("KMSN");
        a.setName("Madison");
        a.setCity("Madison");
        a.setCountry("USA");

        Airport b = new Airport();
        b.setApCode("KORD");
        b.setName("OHare");
        b.setCity("Chicago");
        b.setCountry("USA");

        airportRepository.saveAll(List.of(a, b));

        var result = dbService.getAllAirports();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllFlightsReturnsSavedRows() {
        Flight a = new Flight();
        a.setCallsign("AAL1");
        a.setDepartureTime(LocalDateTime.now());
        Flight b = new Flight();
        b.setCallsign("AAL2");
        b.setDepartureTime(LocalDateTime.now());
        flightRepository.saveAll(List.of(a, b));

        var result = dbService.getAllFlights();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllUsersReturnsSavedRows() {
        AppUser a = new AppUser();
        a.setUname("u1");
        a.setUpass("p1");

        AppUser b = new AppUser();
        b.setUname("u2");
        b.setUpass("p2");

        userRepository.saveAll(List.of(a, b));

        var result = dbService.getAllUsers();

        assertThat(result).hasSize(2);
    }

    @Test
    void deleteCelebrityRemovesRow() {
        Celebrity celebrity = new Celebrity();
        celebrity.setName("Delete Me");
        celebrity = celebrityRepository.save(celebrity);

        dbService.deleteCelebrity(celebrity.getId());

        assertThat(celebrityRepository.findById(celebrity.getId())).isEmpty();
    }

    @Test
    void deleteAircraftRemovesRow() {
        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("NDEL1");
        aircraft = aircraftRepository.save(aircraft);

        dbService.deleteAircraft(aircraft.getId());

        assertThat(aircraftRepository.findById(aircraft.getId())).isEmpty();
    }

    @Test
    void deleteAirportRemovesRow() {
        Airport airport = new Airport();
        airport.setApCode("KDEL");
        airport.setName("Delete Airport");
        airport.setCity("Nowhere");
        airport.setCountry("USA");
        airportRepository.save(airport);

        dbService.deleteAirport("KDEL");

        assertThat(airportRepository.findById("KDEL")).isEmpty();
    }

    @Test
    void deleteFlightRemovesRow() {
        Flight flight = new Flight();
        flight.setCallsign("DEL100");
        flight.setDepartureTime(LocalDateTime.now());
        flight = flightRepository.save(flight);

        dbService.deleteFlight(flight.getId());

        assertThat(flightRepository.findById(flight.getId())).isEmpty();
    }

    @Test
    void deleteUserRemovesRow() {
        AppUser user = new AppUser();
        user.setUname("delete-user");
        user.setUpass("pw");
        user = userRepository.save(user);

        dbService.deleteUser(user.getUId());

        assertThat(userRepository.findById(user.getUId())).isEmpty();
    }

    @Test
    void searchFlightsByTailNumberFindsFlightsThroughAircraftJoin() {
        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N22222");
        aircraft = aircraftRepository.save(aircraft);

        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setCallsign("N22222");
        flight.setDepartureTime(LocalDateTime.now());
        flightRepository.save(flight);

        List<Flight> results = dbService.searchFlightsByTailNumber("N22222");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCallsign()).isEqualTo("N22222");
    }

    @Test
    void getRecentCachedFlightsUsesAircraftTailNumberWhenCallsignMissing() {
        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N33333");
        aircraft = aircraftRepository.save(aircraft);

        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setOriginApCode("KMSN");
        flight.setDestinationApCode("KORD");
        flight.setAltitudeFt(12000);
        flight.setVelocityKts(320);
        flight.setLat(43.07);
        flight.setLng(-89.40);
        flight.setBearing(135.0);
        flight.setLiveInfoJson("{\"source\":\"seed\"}");
        flight.setDepartureTime(LocalDateTime.now().minusMinutes(5));
        flightRepository.save(flight);

        Map<String, Object> response = dbService.getRecentCachedFlights(30);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) response.get("flights");

        assertThat(response)
                .containsEntry("count", 1)
                .containsEntry("source", "database");
        assertThat(flights).hasSize(1);
        assertThat(flights.get(0)).containsEntry("callsign", "N33333");
        assertThat(flights.get(0)).containsEntry("origin", "KMSN");
        assertThat(flights.get(0)).containsEntry("destination", "KORD");
        assertThat(flights.get(0).get("liveInfo").toString()).contains("seed");
    }

    @Test
    void batchSaveFlightsCreatesAircraftAirportsAndFlightRows() {
        Map<String, Object> flight = Map.of(
                "callsign", "N44444",
                "origin", "KMSN",
                "destination", "KORD",
                "altitude", 9000,
                "velocity", 250,
                "lat", 43.12,
                "lng", -89.3,
                "bearing", 180.0,
                "liveInfo", Map.of("source", "manual-test")
        );

        Map<String, Object> result = dbService.batchSaveFlights(List.of(flight));

        assertThat(result).containsEntry("flightsSaved", 1);
        assertThat((List<?>) result.get("errors")).isEmpty();
        assertThat(aircraftRepository.findByTailNumber("N44444")).isPresent();
        assertThat(airportRepository.findById("KMSN")).isPresent();
        assertThat(airportRepository.findById("KORD")).isPresent();
        assertThat(flightRepository.findAll()).hasSize(1);
        assertThat(flightRepository.findAll().get(0).getLiveInfoJson())
                .contains("manual-test");
    }

    // -------------------------------------------------------------------------
    // getLiveAircraft
    // -------------------------------------------------------------------------

    @Test
    void getLiveAircraftWithCategoryReturnsOnlyMatchingAircraftWithCoords() {
        Aircraft mil = new Aircraft();
        mil.setTailNumber("N-MIL1");
        mil.setCategory("mil");
        mil.setLastLat(43.0);
        mil.setLastLon(-89.0);
        aircraftRepository.save(mil);

        Aircraft ladd = new Aircraft();
        ladd.setTailNumber("N-LADD1");
        ladd.setCategory("ladd");
        ladd.setLastLat(44.0);
        ladd.setLastLon(-90.0);
        aircraftRepository.save(ladd);

        var result = dbService.getLiveAircraft("mil");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("tailNumber", "N-MIL1")
                .containsEntry("lastLat", 43.0)
                .containsEntry("lastLon", -89.0)
                .containsKey("airborne")
                .containsKey("lastHeading")
                .containsKey("lastCallsign")
                .containsKey("lastSeen")
                .containsKey("category");
    }

    @Test
    void getLiveAircraftWithNullCategoryReturnsAllWithCoords() {
        Aircraft a1 = new Aircraft();
        a1.setTailNumber("N-ALL1");
        a1.setLastLat(10.0);
        a1.setLastLon(20.0);
        aircraftRepository.save(a1);

        Aircraft a2 = new Aircraft();
        a2.setTailNumber("N-ALL2");
        a2.setLastLat(11.0);
        a2.setLastLon(21.0);
        aircraftRepository.save(a2);

        var result = dbService.getLiveAircraft(null);

        assertThat(result).extracting("tailNumber")
                .contains("N-ALL1", "N-ALL2");
    }

    @Test
    void getLiveAircraftExcludesAircraftWithNullCoords() {
        Aircraft noCoords = new Aircraft();
        noCoords.setTailNumber("N-NOPOS");
        noCoords.setCategory("mil");
        // lastLat and lastLon intentionally left null
        aircraftRepository.save(noCoords);

        var result = dbService.getLiveAircraft("mil");

        assertThat(result).extracting("tailNumber")
                .doesNotContain("N-NOPOS");
    }

    // -------------------------------------------------------------------------
    // bulkSaveAircraftPositions
    // -------------------------------------------------------------------------

    @Test
    void bulkSaveAircraftPositionsNon2xxResponseReturnsZeroSaved() {
        ResponseEntity<String> badResponse = ResponseEntity.status(503).body(null);

        Map<String, Object> result = dbService.bulkSaveAircraftPositions(badResponse, "mil");

        assertThat(result).containsEntry("saved", 0);
        assertThat(result.get("message").toString()).contains("mil");
    }

    @Test
    void bulkSaveAircraftPositionsNullBodyReturnsZeroSaved() {
        ResponseEntity<String> nullBody = ResponseEntity.ok((String) null);

        Map<String, Object> result = dbService.bulkSaveAircraftPositions(nullBody, "ladd");

        assertThat(result).containsEntry("saved", 0);
    }

    @Test
    void bulkSaveAircraftPositionsNoAcArrayReturnsUnexpectedShape() {
        ResponseEntity<String> noAc = ResponseEntity.ok("{\"total\":0}");

        Map<String, Object> result = dbService.bulkSaveAircraftPositions(noAc, "pia");

        assertThat(result).containsEntry("saved", 0);
        assertThat(result.get("message").toString()).contains("ac");
    }

    @Test
    void bulkSaveAircraftPositionsInvalidJsonReturnsParseError() {
        ResponseEntity<String> bad = ResponseEntity.ok("NOT_JSON");

        Map<String, Object> result = dbService.bulkSaveAircraftPositions(bad, "mil");

        assertThat(result).containsEntry("saved", 0);
        assertThat(result.get("message").toString()).contains("Failed to parse");
    }

    @Test
    void bulkSaveAircraftPositionsSavesNewAircraftByReg() {
        String json = "{\"ac\":[{\"r\":\"N-BULK1\",\"hex\":\"AABBCC\",\"flight\":\"UAL1\"," +
                "\"lat\":43.1,\"lon\":-89.1,\"alt_baro\":30000,\"gs\":450.0,\"track\":270.0}]}";

        Map<String, Object> result = dbService.bulkSaveAircraftPositions(
                ResponseEntity.ok(json), "mil");

        assertThat(result).containsEntry("saved", 1).containsEntry("skipped", 0);
        var saved = aircraftRepository.findByTailNumber("N-BULK1");
        assertThat(saved).isPresent();
        assertThat(saved.get().getCategory()).isEqualTo("mil");
        assertThat(saved.get().getAirborne()).isTrue();
        assertThat(saved.get().getLastLat()).isEqualTo(43.1);
        assertThat(saved.get().getLastCallsign()).isEqualTo("UAL1");
    }

    @Test
    void bulkSaveAircraftPositionsUpdatesExistingAircraftByReg() {
        Aircraft existing = new Aircraft();
        existing.setTailNumber("N-EXIST1");
        existing.setCategory("ladd");
        aircraftRepository.save(existing);

        String json = "{\"ac\":[{\"r\":\"N-EXIST1\",\"lat\":44.0,\"lon\":-90.0}]}";

        Map<String, Object> result = dbService.bulkSaveAircraftPositions(
                ResponseEntity.ok(json), "mil");

        assertThat(result).containsEntry("saved", 1);
        var updated = aircraftRepository.findByTailNumber("N-EXIST1");
        assertThat(updated).isPresent();
        assertThat(updated.get().getCategory()).isEqualTo("mil");
        assertThat(updated.get().getLastLat()).isEqualTo(44.0);
    }

    @Test
    void bulkSaveAircraftPositionsSkipsEntryWithNoRegOrHex() {
        String json = "{\"ac\":[{\"flight\":\"UAL99\",\"lat\":43.0,\"lon\":-89.0}]}";

        Map<String, Object> result = dbService.bulkSaveAircraftPositions(
                ResponseEntity.ok(json), "mil");

        assertThat(result).containsEntry("saved", 0).containsEntry("skipped", 1);
    }

    // -------------------------------------------------------------------------
    // saveLiveAll
    // -------------------------------------------------------------------------

    @Test
    void saveLiveAllWithEmptyRepoReturnsSavedZero() {
        Map<String, Object> result = dbService.saveLiveAll();

        assertThat(result).containsEntry("saved", 0).containsEntry("skipped", 0);
    }

    @Test
    void saveLiveAllSkipsAircraftWithNullTailNumber() {
        Aircraft noTail = new Aircraft();
        // tailNumber intentionally null — unique constraint requires we not set it
        aircraftRepository.save(noTail);

        Map<String, Object> result = dbService.saveLiveAll();

        assertThat((Integer) result.get("skipped")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void saveLiveAllUsesLiveServiceForEachAircraft() {
        Aircraft a = new Aircraft();
        a.setTailNumber("N-SAVEALL1");
        aircraftRepository.save(a);

        when(airplanesLiveService.getByReg("N-SAVEALL1"))
                .thenReturn(ResponseEntity.ok("{\"ac\":[]}"));

        Map<String, Object> result = dbService.saveLiveAll();

        // aircraft found but ac array empty → airborne=false written, saved=1
        assertThat((Integer) result.get("saved")).isGreaterThanOrEqualTo(1);
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        FlightService flightService() {
            return mock(FlightService.class);
        }

        @Bean
        AirplanesLiveService airplanesLiveService() {
            return mock(AirplanesLiveService.class);
        }
    }

    @Test
    void importJsonSavesAllSupportedEntityTypes() {
        Map<String, Object> payload = Map.of(
                "celebrities", List.of(Map.of("name", "Taylor Swift", "industry", "Music")),
                "aircraft", List.of(Map.of("tailNumber", "N12345", "model", "737")),
                "airports", List.of(Map.of("apCode", "KMSN", "name", "Madison", "city", "Madison", "country", "USA")),
                "flights", List.of(Map.of("callsign", "UAL100"))
        );

        Map<String, Object> result = dbService.importJson(payload);

        assertThat(result)
                .containsEntry("celebrities", 1)
                .containsEntry("aircraft", 1)
                .containsEntry("airports", 1)
                .containsEntry("flights", 1);

        assertThat((List<?>) result.get("errors")).isEmpty();
        assertThat(celebrityRepository.count()).isEqualTo(1);
        assertThat(aircraftRepository.count()).isEqualTo(1);
        assertThat(airportRepository.count()).isEqualTo(1);
        assertThat(flightRepository.count()).isEqualTo(1);
    }

    @Test
    void fetchAndSaveFlightReturnsDatabaseResultWhenAlreadyCached() {
        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N22222");
        aircraft = aircraftRepository.save(aircraft);

        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setCallsign("N22222");
        flight.setDepartureTime(LocalDateTime.now());
        flight = flightRepository.save(flight);

        Map<String, Object> result = dbService.fetchAndSaveFlight("N22222");

        assertThat(result).containsEntry("source", "database");
        assertThat(result.get("aircraft")).isEqualTo(aircraft);

        @SuppressWarnings("unchecked")
        List<Flight> flights = (List<Flight>) result.get("flights");
        assertThat(flights).hasSize(1);
        assertThat(flights.get(0).getId()).isEqualTo(flight.getId());
    }

    @Test
    void fetchAndSaveFlightReturnsErrorWhenAeroApiThrows() {
        when(flightService.getFlightByIdent("FAIL123"))
                .thenThrow(new RuntimeException("boom"));

        Map<String, Object> result = dbService.fetchAndSaveFlight("FAIL123");

        assertThat(result.get("error").toString()).contains("AeroAPI request failed");
    }

    @Test
    void fetchAndSaveFlightSavesAeroApiResultWhenFlightsReturned() {
        String body = """
                {
                "flights": [
                    {
                    "registration": "N90000",
                    "aircraft_type": "B738",
                    "origin": {
                        "code": "KMSN",
                        "name": "Madison",
                        "city": "Madison",
                        "country_code": "US"
                    },
                    "destination": {
                        "code": "KORD",
                        "name": "OHare",
                        "city": "Chicago",
                        "country_code": "US"
                    },
                    "scheduled_out": "2026-03-17T10:00:00Z",
                    "scheduled_in": "2026-03-17T11:30:00Z"
                    }
                ]
                }
                """;

        when(flightService.getFlightByIdent("N90000"))
                .thenReturn(ResponseEntity.ok(body));

        Map<String, Object> result = dbService.fetchAndSaveFlight("N90000");

        assertThat(result).containsEntry("source", "aeroapi");
        assertThat(airportRepository.findById("KMSN")).isPresent();
        assertThat(airportRepository.findById("KORD")).isPresent();
        assertThat(aircraftRepository.findByTailNumber("N90000")).isPresent();
        assertThat(flightRepository.findAll()).hasSize(1);

        Aircraft savedAircraft = (Aircraft) result.get("aircraft");
        assertThat(savedAircraft.getTailNumber()).isEqualTo("N90000");

        @SuppressWarnings("unchecked")
        List<Flight> flights = (List<Flight>) result.get("flights");
        assertThat(flights).hasSize(1);
        assertThat(flights.get(0).getOriginApCode()).isEqualTo("KMSN");
        assertThat(flights.get(0).getDestinationApCode()).isEqualTo("KORD");
        assertThat(flights.get(0).getDepartureTime()).isNotNull();
        assertThat(flights.get(0).getArrivalTime()).isNotNull();
    }

    @Test
    void fetchLivePositionReturnsNoResponseWhenServiceNotSuccessful() {
        when(airplanesLiveService.getByReg("N11111"))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null));

        Map<String, Object> result = dbService.fetchLivePosition("N11111");

        assertThat(result)
                .containsEntry("tailNumber", "N11111")
                .containsEntry("airborne", false);
        assertThat(result.get("message").toString()).contains("No response from airplanes.live");
    }

    @Test
    void fetchLivePositionReturnsNotTrackedWhenArrayEmpty() {
        when(airplanesLiveService.getByReg("N11111"))
                .thenReturn(ResponseEntity.ok("{\"ac\":[]}"));

        Map<String, Object> result = dbService.fetchLivePosition("N11111");

        assertThat(result)
                .containsEntry("tailNumber", "N11111")
                .containsEntry("airborne", false);
        assertThat(result.get("message").toString()).contains("Aircraft not currently tracked");
    }

    @Test
    void fetchLivePositionReturnsTrackedAircraftAndDbMetadata() {
        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N33333");
        aircraft.setModel("C172");
        aircraft.setOwnerId(42L);
        aircraftRepository.save(aircraft);

        String body = """
                {
                "ac": [
                    {
                    "hex": "abc123",
                    "flight": "N33333",
                    "lat": 43.07,
                    "lon": -89.40,
                    "alt_baro": 5000,
                    "gs": 140.5,
                    "track": 180.0,
                    "t": "C172",
                    "desc": "Cessna 172"
                    }
                ]
                }
                """;

        when(airplanesLiveService.getByReg("N33333"))
                .thenReturn(ResponseEntity.ok(body));

        Map<String, Object> result = dbService.fetchLivePosition("N33333");

        assertThat(result)
                .containsEntry("tailNumber", "N33333")
                .containsEntry("airborne", true)
                .containsEntry("hex", "abc123")
                .containsEntry("callsign", "N33333")
                .containsEntry("type", "C172")
                .containsEntry("desc", "Cessna 172")
                .containsEntry("model", "C172")
                .containsEntry("ownerId", 42L);
    }

    @Test
    void saveLivePositionMarksAircraftNotAirborneWhenArrayEmpty() {
        when(airplanesLiveService.getByReg("N44444"))
                .thenReturn(ResponseEntity.ok("{\"ac\":[]}"));

        Map<String, Object> result = dbService.saveLivePosition("N44444");

        assertThat(result)
                .containsEntry("tailNumber", "N44444")
                .containsEntry("airborne", false)
                .containsEntry("saved", true);

        Aircraft aircraft = aircraftRepository.findByTailNumber("N44444").orElseThrow();
        assertThat(aircraft.getAirborne()).isFalse();
    }

    @Test
    void saveLivePositionPersistsTrackedAircraftData() {
        String body = """
                {
                "ac": [
                    {
                    "hex": "def456",
                    "flight": "N55555",
                    "lat": 44.98,
                    "lon": -93.26,
                    "alt_baro": 7200,
                    "gs": 155.0,
                    "track": 270.0
                    }
                ]
                }
                """;

        when(airplanesLiveService.getByReg("N55555"))
                .thenReturn(ResponseEntity.ok(body));

        Map<String, Object> result = dbService.saveLivePosition("N55555");

        assertThat(result)
                .containsEntry("tailNumber", "N55555")
                .containsEntry("airborne", true)
                .containsEntry("saved", true);

        Aircraft aircraft = aircraftRepository.findByTailNumber("N55555").orElseThrow();
        assertThat(aircraft.getAirborne()).isTrue();
        assertThat(aircraft.getLastHex()).isEqualTo("def456");
        assertThat(aircraft.getLastCallsign()).isEqualTo("N55555");
        assertThat(aircraft.getLastLat()).isEqualTo(44.98);
        assertThat(aircraft.getLastLon()).isEqualTo(-93.26);
        assertThat(aircraft.getLastAltitude()).isEqualTo(7200);
        assertThat(aircraft.getLastSpeed()).isEqualTo(155.0);
        assertThat(aircraft.getLastHeading()).isEqualTo(270.0);
        assertThat(aircraft.getLastSeen()).isNotNull();
    }

    @Test
void parseTimestampReturnsParsedDateWhenFieldExists() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree("""
            {
              "scheduled_out": "2026-03-17T10:15:00Z"
            }
            """);

    Object result = ReflectionTestUtils.invokeMethod(
            dbService,
            "parseTimestamp",
            node,
            "scheduled_out",
            "scheduled_out"
    );

    assertThat(result).isNotNull();

    if (result instanceof LocalDateTime parsed) {
        assertThat(parsed.getYear()).isEqualTo(2026);
        assertThat(parsed.getMonthValue()).isEqualTo(3);
        assertThat(parsed.getDayOfMonth()).isEqualTo(17);
        assertThat(parsed.getHour()).isEqualTo(10);
        assertThat(parsed.getMinute()).isEqualTo(15);
    }
}

@Test
    void parseTimestampReturnsNullWhenFieldMissing() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("""
                {
                "other_field": "2026-03-17T10:15:00Z"
                }
                """);

        Object result = ReflectionTestUtils.invokeMethod(
                dbService,
                "parseTimestamp",
                node,
                "scheduled_out",
                "scheduled_out"
        );

        assertThat(result).isNull();
    }

    @Test
void parseTimestampUsesPreferredFieldWhenPresent() {
    JsonNode node;
    try {
        node = new ObjectMapper().readTree("""
                {
                  "actual_out": "2026-03-17T10:15:00Z",
                  "scheduled_out": "2026-03-17T11:30:00Z"
                }
                """);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

    LocalDateTime result = ReflectionTestUtils.invokeMethod(
            dbService,
            "parseTimestamp",
            node,
            "actual_out",
            "scheduled_out"
    );

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2026);
    assertThat(result.getMonthValue()).isEqualTo(3);
    assertThat(result.getDayOfMonth()).isEqualTo(17);
    assertThat(result.getHour()).isEqualTo(10);
    assertThat(result.getMinute()).isEqualTo(15);
}

@Test
void parseTimestampUsesFallbackFieldWhenPreferredMissing() {
    JsonNode node;
    try {
        node = new ObjectMapper().readTree("""
                {
                  "scheduled_out": "2026-03-17T11:30:00Z"
                }
                """);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

    LocalDateTime result = ReflectionTestUtils.invokeMethod(
            dbService,
            "parseTimestamp",
            node,
            "actual_out",
            "scheduled_out"
    );

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2026);
    assertThat(result.getMonthValue()).isEqualTo(3);
    assertThat(result.getDayOfMonth()).isEqualTo(17);
    assertThat(result.getHour()).isEqualTo(11);
    assertThat(result.getMinute()).isEqualTo(30);
}

@Test
void parseTimestampReturnsNullWhenBothFieldsMissing() {
    JsonNode node;
    try {
        node = new ObjectMapper().readTree("""
                {
                  "other_field": "x"
                }
                """);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

    LocalDateTime result = ReflectionTestUtils.invokeMethod(
            dbService,
            "parseTimestamp",
            node,
            "actual_out",
            "scheduled_out"
    );

    assertThat(result).isNull();
}

@Test
void parseTimestampReturnsNullWhenTimestampInvalid() {
    JsonNode node;
    try {
        node = new ObjectMapper().readTree("""
                {
                  "actual_out": "not-a-timestamp"
                }
                """);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

    LocalDateTime result = ReflectionTestUtils.invokeMethod(
            dbService,
            "parseTimestamp",
            node,
            "actual_out",
            "scheduled_out"
    );

    assertThat(result).isNull();
}

@Test
void fetchFromAirplanesLiveReturnsNoneWhenResponseNotSuccessful() {
    when(airplanesLiveService.getByReg("N12345"))
            .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null));

    Map<String, Object> seed = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    Map<String, Object> result = ReflectionTestUtils.invokeMethod(
            dbService,
            "fetchFromAirplanesLive",
            "N12345",
            seed
    );

    assertThat(result)
            .containsEntry("source", "none");
    assertThat(result.get("message").toString())
            .contains("No data found in DB, AeroAPI, or airplanes.live for: N12345");
}

@Test
void fetchFromAirplanesLiveReturnsNoneWhenAircraftArrayEmpty() {
    when(airplanesLiveService.getByReg("N12345"))
            .thenReturn(ResponseEntity.ok("{\"ac\":[]}"));

    Map<String, Object> seed = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    Map<String, Object> result = ReflectionTestUtils.invokeMethod(
            dbService,
            "fetchFromAirplanesLive",
            "N12345",
            seed
    );

    assertThat(result)
            .containsEntry("source", "none");
    assertThat(result.get("message").toString())
            .contains("No data found in DB, AeroAPI, or airplanes.live for: N12345");
}

@Test
void fetchFromAirplanesLiveCreatesAircraftAndReturnsLivePosition() {
    String body = """
            {
              "ac": [
                {
                  "hex": "abc123",
                  "flight": "N88888",
                  "lat": 43.07,
                  "lon": -89.40,
                  "alt_baro": 6100,
                  "gs": 145.0,
                  "track": 182.0,
                  "t": "C172",
                  "desc": "Cessna 172"
                }
              ]
            }
            """;

    when(airplanesLiveService.getByReg("N88888"))
            .thenReturn(ResponseEntity.ok(body));

    Map<String, Object> seed = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    Map<String, Object> result = ReflectionTestUtils.invokeMethod(
            dbService,
            "fetchFromAirplanesLive",
            "N88888",
            seed
    );

    assertThat(result)
            .containsEntry("source", "airplanes.live");
    assertThat(result.get("aircraft")).isInstanceOf(Aircraft.class);

    Aircraft aircraft = (Aircraft) result.get("aircraft");
    assertThat(aircraft.getTailNumber()).isEqualTo("N88888");
    assertThat(aircraft.getModel()).isEqualTo("C172");

    assertThat(aircraftRepository.findByTailNumber("N88888")).isPresent();

    @SuppressWarnings("unchecked")
    Map<String, Object> livePosition = (Map<String, Object>) result.get("livePosition");
    assertThat(livePosition)
            .containsEntry("hex", "abc123")
            .containsEntry("callsign", "N88888")
            .containsEntry("lat", 43.07)
            .containsEntry("lon", -89.40)
            .containsEntry("altitude", 6100)
            .containsEntry("speed", 145.0)
            .containsEntry("heading", 182.0)
            .containsEntry("type", "C172")
            .containsEntry("desc", "Cessna 172");
}

@Test
void fetchFromAirplanesLiveUsesExistingAircraftWhenPresent() {
    Aircraft aircraft = new Aircraft();
    aircraft.setTailNumber("N77777");
    aircraft.setModel("OLDMODEL");
    aircraftRepository.save(aircraft);

    String body = """
            {
              "ac": [
                {
                  "hex": "zzz999",
                  "flight": "N77777",
                  "lat": 40.0,
                  "lon": -88.0,
                  "alt_baro": "ground",
                  "gs": 0.0,
                  "track": 0.0,
                  "t": "B738",
                  "desc": "Boeing 737"
                }
              ]
            }
            """;

    when(airplanesLiveService.getByReg("N77777"))
            .thenReturn(ResponseEntity.ok(body));

    Map<String, Object> seed = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    Map<String, Object> result = ReflectionTestUtils.invokeMethod(
            dbService,
            "fetchFromAirplanesLive",
            "N77777",
            seed
    );

    assertThat(result)
            .containsEntry("source", "airplanes.live");

    Aircraft saved = aircraftRepository.findByTailNumber("N77777").orElseThrow();
    assertThat(saved.getTailNumber()).isEqualTo("N77777");
    assertThat(saved.getModel()).isEqualTo("OLDMODEL");

    @SuppressWarnings("unchecked")
    Map<String, Object> livePosition = (Map<String, Object>) result.get("livePosition");
    assertThat(livePosition)
            .containsEntry("hex", "zzz999")
            .containsEntry("callsign", "N77777")
            .containsEntry("lat", 40.0)
            .containsEntry("lon", -88.0)
            .containsEntry("altitude", "ground")
            .containsEntry("speed", 0.0)
            .containsEntry("heading", 0.0)
            .containsEntry("type", "B738")
            .containsEntry("desc", "Boeing 737");
}

    // ------------------------------------------------------------------ getLiveCelebrities

    @Test
    void getLiveCelebritiesReturnsCelebWithNullsWhenNoAircraftOwned() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Elon Musk");
        celeb.setIndustry("Tech");
        celebrityRepository.save(celeb);

        List<Map<String, Object>> result = dbService.getLiveCelebrities();

        assertThat(result).hasSize(1);
        Map<String, Object> row = result.get(0);
        assertThat(row).containsEntry("name", "Elon Musk");
        assertThat(row).containsEntry("airborne", null);
        assertThat(row).containsEntry("tailNumber", null);
    }

    @Test
    void getLiveCelebritiesPicksMostRecentAircraftByLastSeen() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Taylor Swift");
        celeb.setIndustry("Music");
        Celebrity savedCeleb = celebrityRepository.save(celeb);

        Aircraft older = new Aircraft();
        older.setTailNumber("N_OLD");
        older.setOwnerId(savedCeleb.getId());
        older.setAirborne(false);
        older.setLastSeen(LocalDateTime.now().minusHours(2));
        aircraftRepository.save(older);

        Aircraft newer = new Aircraft();
        newer.setTailNumber("N_NEW");
        newer.setOwnerId(savedCeleb.getId());
        newer.setAirborne(true);
        newer.setLastLat(43.0);
        newer.setLastLon(-89.0);
        newer.setLastSeen(LocalDateTime.now().minusMinutes(5));
        aircraftRepository.save(newer);

        List<Map<String, Object>> result = dbService.getLiveCelebrities();

        assertThat(result).hasSize(1);
        Map<String, Object> row = result.get(0);
        assertThat(row).containsEntry("tailNumber", "N_NEW");
        assertThat(row).containsEntry("airborne", true);
    }

    @Test
    void getLiveCelebritiesFallsBackToFirstAircraftWhenNoneHaveLastSeen() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Jeff Bezos");
        Celebrity savedCeleb = celebrityRepository.save(celeb);

        Aircraft a = new Aircraft();
        a.setTailNumber("N_NOSEEN");
        a.setOwnerId(savedCeleb.getId());
        a.setAirborne(true);
        aircraftRepository.save(a);

        List<Map<String, Object>> result = dbService.getLiveCelebrities();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("tailNumber", "N_NOSEEN");
    }

    // ------------------------------------------------------------------ getCo2Summary

    @Test
    void getCo2SummaryReturnsErrorWhenFlightNotFound() {
        Map<String, Object> result = dbService.getCo2Summary(99999L);

        assertThat(result).containsKey("error");
        assertThat(result.get("error").toString()).contains("99999");
    }

    @Test
    void getCo2SummaryReturnsNullComparisonWhenAirportsMissing() {
        Flight flight = new Flight();
        flight.setCallsign("UAL100");
        flight.setEstimatedCo2Kg(1234.5);
        Flight saved = flightRepository.save(flight);

        Map<String, Object> result = dbService.getCo2Summary(saved.getId());

        assertThat(result).containsEntry("flightId", saved.getId());
        assertThat(result).containsEntry("estimatedCo2Kg", 1234.5);
        assertThat(result).containsEntry("comparison", null);
    }

    @Test
    void getCo2SummaryReturnsComparisonWhenAirportCoordsPresent() {
        Airport origin = new Airport();
        origin.setApCode("KMSN");
        origin.setLat(43.14);
        origin.setLon(-89.34);
        airportRepository.save(origin);

        Airport dest = new Airport();
        dest.setApCode("KORD");
        dest.setLat(41.97);
        dest.setLon(-87.91);
        airportRepository.save(dest);

        Flight flight = new Flight();
        flight.setCallsign("UAL200");
        flight.setOriginApCode("KMSN");
        flight.setDestinationApCode("KORD");
        flight.setEstimatedCo2Kg(1337.0);
        Flight saved = flightRepository.save(flight);

        Map<String, Object> result = dbService.getCo2Summary(saved.getId());

        assertThat(result).containsEntry("flightId", saved.getId());
        assertThat(result.get("comparison")).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> comparison = (Map<String, Object>) result.get("comparison");
        assertThat(comparison).containsKey("distanceKm");
        assertThat(comparison).containsKey("multiplier");
    }

    // ------------------------------------------------------------------ recalculateCo2

    @Test
    void recalculateCo2ReturnsErrorWhenFlightNotFound() {
        Map<String, Object> result = dbService.recalculateCo2(99999L);

        assertThat(result).containsKey("error");
        assertThat(result.get("error").toString()).contains("99999");
    }

    @Test
    void recalculateCo2PersistsNewValueAndReturnsIt() {
        Airport origin = new Airport();
        origin.setApCode("KATL");
        origin.setLat(33.64);
        origin.setLon(-84.43);
        airportRepository.save(origin);

        Airport dest = new Airport();
        dest.setApCode("KLAX");
        dest.setLat(33.94);
        dest.setLon(-118.41);
        airportRepository.save(dest);

        Flight flight = new Flight();
        flight.setCallsign("DAL500");
        flight.setOriginApCode("KATL");
        flight.setDestinationApCode("KLAX");
        flight.setEstimatedCo2Kg(null);
        Flight saved = flightRepository.save(flight);

        Map<String, Object> result = dbService.recalculateCo2(saved.getId());

        assertThat(result).containsEntry("flightId", saved.getId());
        assertThat(result).containsEntry("recalculated", true);
        assertThat(result.get("estimatedCo2Kg")).isNotNull();

        // Confirm the value was persisted to the DB
        Double persisted = flightRepository.findById(saved.getId()).orElseThrow().getEstimatedCo2Kg();
        assertThat(persisted).isNotNull().isGreaterThan(0.0);
    }

    // ------------------------------------------------------------------ bulkSaveAircraftPositions (hex-only path)

    @Test
    void bulkSaveAircraftPositionsSavesNewAircraftByHexWhenRegMissing() throws Exception {
        String body = "{\"ac\":[{\"hex\":\"abc123\",\"flight\":\"TEST01\",\"lat\":43.0,\"lon\":-89.0}]}";
        ResponseEntity<String> response = ResponseEntity.ok(body);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                dbService, "bulkSaveAircraftPositions", response, "mil");

        assertThat(result).containsEntry("saved", 1);
        // Verify aircraft was saved using hex as the key
        assertThat(aircraftRepository.findByLastHex("abc123")).isPresent();
    }

    // ------------------------------------------------------------------ getCelebrityFlights — clean data guarantees

    @Test
    void getCelebrityFlights_returnsEmptyFlightsWhenCelebrityNotFound() {
        Map<String, Object> result = dbService.getCelebrityFlights(99999L, null);

        assertThat(result).doesNotContainKey("error");
        assertThat(result).containsEntry("flightCount", 0);
        assertThat((List<?>) result.get("flights")).isEmpty();
    }

    @Test
    void getCelebrityFlights_elidatesDegenerateFlightWithBothAirportCodesNull() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Degenerate Test");
        celeb = celebrityRepository.save(celeb);

        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N-DEGEN1");
        aircraft.setOwnerId(celeb.getId());
        aircraft = aircraftRepository.save(aircraft);

        // Flight with null origin AND null destination — should be filtered before returning
        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setDepartureTime(LocalDateTime.now().minusHours(2));
        flightRepository.save(flight);

        Map<String, Object> result = dbService.getCelebrityFlights(celeb.getId(), null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) result.get("flights");
        assertThat(flights).isEmpty();
    }

    @Test
    void getCelebrityFlights_keepsFlightWhenOnlyDestinationIsSet() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Partial Route Test");
        celeb = celebrityRepository.save(celeb);

        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N-PART1");
        aircraft.setOwnerId(celeb.getId());
        aircraft = aircraftRepository.save(aircraft);

        // origin intentionally null; destination is set
        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setDestinationApCode("KORD");
        flight.setDepartureTime(LocalDateTime.now().minusHours(1));
        flightRepository.save(flight);

        Map<String, Object> result = dbService.getCelebrityFlights(celeb.getId(), null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) result.get("flights");
        assertThat(flights).hasSize(1);
        // origin must be null — never the old "???" placeholder
        assertThat(flights.get(0).get("origin")).isNull();
        assertThat(flights.get(0).get("origin")).isNotEqualTo("???");
        assertThat(flights.get(0).get("destination")).isEqualTo("KORD");
    }

    @Test
    void getCelebrityFlights_destinationIsNullNotPlaceholderWhenMissing() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Null Dest Test");
        celeb = celebrityRepository.save(celeb);

        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N-NDEST1");
        aircraft.setOwnerId(celeb.getId());
        aircraft = aircraftRepository.save(aircraft);

        // destination intentionally null; origin is set
        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setOriginApCode("KMSN");
        flight.setDepartureTime(LocalDateTime.now().minusHours(3));
        flightRepository.save(flight);

        Map<String, Object> result = dbService.getCelebrityFlights(celeb.getId(), null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) result.get("flights");
        assertThat(flights).hasSize(1);
        assertThat(flights.get(0).get("origin")).isEqualTo("KMSN");
        assertThat(flights.get(0).get("destination")).isNull();
        assertThat(flights.get(0).get("destination")).isNotEqualTo("???");
    }

    // ------------------------------------------------------------------ recordUserCelebrityView

    @Test
    void getCelebrityFlights_createsUserCacheRowWhenUserIdProvided() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Cache Insert Test");
        celeb = celebrityRepository.save(celeb);

        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N-CINS1");
        aircraft.setOwnerId(celeb.getId());
        aircraft = aircraftRepository.save(aircraft);

        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setOriginApCode("KMSN");
        flight.setDestinationApCode("KORD");
        flight.setDepartureTime(LocalDateTime.now().minusDays(1));
        flightRepository.save(flight);

        AppUser user = new AppUser();
        user.setUname("cache-insert-user");
        user.setUpass("pw");
        user = userRepository.save(user);

        dbService.getCelebrityFlights(celeb.getId(), user.getUId());

        List<UserCelebrityCache> rows = userCelebrityCacheRepository.findByUserId(user.getUId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCelebrityId()).isEqualTo(celeb.getId());
        assertThat(rows.get(0).getLastFetchedAt()).isNotNull();
    }

    @Test
    void getCelebrityFlights_doesNotDuplicateCacheRowOnSecondView() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Cache Update Test");
        celeb = celebrityRepository.save(celeb);

        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N-CUPD1");
        aircraft.setOwnerId(celeb.getId());
        aircraft = aircraftRepository.save(aircraft);

        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setOriginApCode("KORD");
        flight.setDestinationApCode("KLAX");
        flight.setDepartureTime(LocalDateTime.now().minusDays(2));
        flightRepository.save(flight);

        AppUser user = new AppUser();
        user.setUname("cache-update-user");
        user.setUpass("pw");
        user = userRepository.save(user);

        dbService.getCelebrityFlights(celeb.getId(), user.getUId());
        dbService.getCelebrityFlights(celeb.getId(), user.getUId());

        // Must remain exactly one row — no duplicate inserted on second view
        List<UserCelebrityCache> rows = userCelebrityCacheRepository.findByUserId(user.getUId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getLastFetchedAt()).isNotNull();
    }

    @Test
    void getCelebrityFlights_doesNotCreateCacheRowWhenUserIdIsNull() {
        Celebrity celeb = new Celebrity();
        celeb.setName("No User Test");
        celeb = celebrityRepository.save(celeb);

        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N-NOUID1");
        aircraft.setOwnerId(celeb.getId());
        aircraft = aircraftRepository.save(aircraft);

        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setOriginApCode("KMSN");
        flight.setDepartureTime(LocalDateTime.now().minusDays(1));
        flightRepository.save(flight);

        dbService.getCelebrityFlights(celeb.getId(), null);

        // No userId supplied → cache table should remain empty
        assertThat(userCelebrityCacheRepository.findByUserId(-1L)).isEmpty();
    }

    // ------------------------------------------------------------------ getUserCelebrityHistory

    @Test
    void getUserCelebrityHistory_returnsEmptyListWhenNoCacheEntries() {
        List<Map<String, Object>> result = dbService.getUserCelebrityHistory(99999L);

        assertThat(result).isEmpty();
    }

    @Test
    void getUserCelebrityHistory_returnsEnrichedEntryWithFlights() {
        Celebrity celeb = new Celebrity();
        celeb.setName("History Celeb");
        celeb.setIndustry("Sports");
        celeb = celebrityRepository.save(celeb);

        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N-HIST1");
        aircraft.setOwnerId(celeb.getId());
        aircraft = aircraftRepository.save(aircraft);

        Flight flight = new Flight();
        flight.setAircraftId(aircraft.getId());
        flight.setOriginApCode("KMSN");
        flight.setDestinationApCode("KORD");
        flight.setDepartureTime(LocalDateTime.now().minusDays(1));
        flightRepository.save(flight);

        AppUser user = new AppUser();
        user.setUname("history-user");
        user.setUpass("pw");
        user = userRepository.save(user);

        UserCelebrityCache cache = new UserCelebrityCache();
        cache.setUserId(user.getUId());
        cache.setCelebrityId(celeb.getId());
        cache.setLastFetchedAt(LocalDateTime.now().minusHours(1));
        userCelebrityCacheRepository.save(cache);

        List<Map<String, Object>> result = dbService.getUserCelebrityHistory(user.getUId());

        assertThat(result).hasSize(1);
        Map<String, Object> entry = result.get(0);
        assertThat(entry).containsEntry("celebrity", "History Celeb");
        assertThat(entry).containsEntry("industry", "Sports");
        assertThat(entry).containsKey("lastFetchedAt");
        assertThat((Integer) entry.get("flightCount")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) entry.get("flights");
        assertThat(flights).hasSize(1);
        assertThat(flights.get(0)).containsEntry("tailNumber", "N-HIST1");
        assertThat(flights.get(0)).containsEntry("origin", "KMSN");
        assertThat(flights.get(0)).containsEntry("destination", "KORD");
    }

    @Test
    void getUserCelebrityHistory_sortsByLastFetchedAtDescending() {
        AppUser user = new AppUser();
        user.setUname("sort-history-user");
        user.setUpass("pw");
        user = userRepository.save(user);

        Celebrity older = new Celebrity();
        older.setName("Older Celeb");
        older = celebrityRepository.save(older);

        Celebrity newer = new Celebrity();
        newer.setName("Newer Celeb");
        newer = celebrityRepository.save(newer);

        UserCelebrityCache cacheOld = new UserCelebrityCache();
        cacheOld.setUserId(user.getUId());
        cacheOld.setCelebrityId(older.getId());
        cacheOld.setLastFetchedAt(LocalDateTime.now().minusDays(2));
        userCelebrityCacheRepository.save(cacheOld);

        UserCelebrityCache cacheNew = new UserCelebrityCache();
        cacheNew.setUserId(user.getUId());
        cacheNew.setCelebrityId(newer.getId());
        cacheNew.setLastFetchedAt(LocalDateTime.now().minusHours(1));
        userCelebrityCacheRepository.save(cacheNew);

        List<Map<String, Object>> result = dbService.getUserCelebrityHistory(user.getUId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("celebrity", "Newer Celeb");
        assertThat(result.get(1)).containsEntry("celebrity", "Older Celeb");
    }

    @Test
    void getUserCelebrityHistory_skipsCelebrityDeletedFromDb() {
        AppUser user = new AppUser();
        user.setUname("orphan-history-user");
        user.setUpass("pw");
        user = userRepository.save(user);

        // Cache row pointing to a celebrity that no longer exists
        UserCelebrityCache orphan = new UserCelebrityCache();
        orphan.setUserId(user.getUId());
        orphan.setCelebrityId(99999L);
        orphan.setLastFetchedAt(LocalDateTime.now());
        userCelebrityCacheRepository.save(orphan);

        List<Map<String, Object>> result = dbService.getUserCelebrityHistory(user.getUId());

        assertThat(result).isEmpty();
    }

    // ── CelebritySeeder ───────────────────────────────────────────────────────

    @Test
    void celebritySeederSkipsAllWhenDataAlreadyExists() throws Exception {
        Celebrity existing = new Celebrity();
        existing.setName("Pre-existing Celeb");
        existing.setIndustry("Test");
        celebrityRepository.save(existing);

        CelebritySeeder seeder = new CelebritySeeder(celebrityRepository, aircraftRepository);
        seeder.run(null);

        // count must still be 1 — seeder must not add any more rows
        assertThat(celebrityRepository.count()).isEqualTo(1);
        assertThat(aircraftRepository.count()).isEqualTo(0);
    }

    @Test
    void celebritySeederSeedsAllCelebritiesWhenEmpty() throws Exception {
        CelebritySeeder seeder = new CelebritySeeder(celebrityRepository, aircraftRepository);
        seeder.run(null);

        assertThat(celebrityRepository.count()).isEqualTo(15);
    }

    @Test
    void celebritySeederSeedsAircraftLinkedToCorrectOwner() throws Exception {
        CelebritySeeder seeder = new CelebritySeeder(celebrityRepository, aircraftRepository);
        seeder.run(null);

        // All saved aircraft must have an owner that exists in the celebrity table
        Iterable<Aircraft> allAircraft = aircraftRepository.findAll();
        Iterable<Celebrity> allCelebrities = celebrityRepository.findAll();
        java.util.Set<Long> celebrityIds = new java.util.HashSet<>();
        allCelebrities.forEach(c -> celebrityIds.add(c.getId()));

        for (Aircraft a : allAircraft) {
            assertThat(celebrityIds).contains(a.getOwnerId());
        }
    }

    // ── CelebrityLivePositionSeeder ───────────────────────────────────────────

    @Test
    void livePositionSeederSetsCategoryOnAircraftWithOwner() throws Exception {
        Aircraft a = new Aircraft();
        a.setTailNumber("N111XX");
        a.setOwnerId(1L);
        a.setCategory("unknown");
        aircraftRepository.save(a);

        new CelebrityLivePositionSeeder(aircraftRepository).run(null);

        assertThat(aircraftRepository.findByTailNumber("N111XX").get().getCategory())
                .isEqualTo("celebrity");
    }

    @Test
    void livePositionSeederSkipsAircraftWithNullOwner() throws Exception {
        Aircraft a = new Aircraft();
        a.setTailNumber("N222XX");
        a.setCategory("unknown");
        // ownerId intentionally left null
        aircraftRepository.save(a);

        new CelebrityLivePositionSeeder(aircraftRepository).run(null);

        assertThat(aircraftRepository.findByTailNumber("N222XX").get().getCategory())
                .isEqualTo("unknown");
    }

    @Test
    void livePositionSeederSkipsAircraftAlreadyCategorizedAsCelebrity() throws Exception {
        Aircraft a = new Aircraft();
        a.setTailNumber("N333XX");
        a.setOwnerId(1L);
        a.setCategory("celebrity");
        aircraftRepository.save(a);

        new CelebrityLivePositionSeeder(aircraftRepository).run(null);

        // category unchanged — no unnecessary save
        assertThat(aircraftRepository.findByTailNumber("N333XX").get().getCategory())
                .isEqualTo("celebrity");
    }

    @Test
    void livePositionSeederSeedsPositionForKnownTailNumber() throws Exception {
        Aircraft a = new Aircraft();
        a.setTailNumber("N898TS"); // in POSITIONS_EXTRA
        a.setLastLat(null);
        aircraftRepository.save(a);

        new CelebrityLivePositionSeeder(aircraftRepository).run(null);

        Aircraft updated = aircraftRepository.findByTailNumber("N898TS").get();
        assertThat(updated.getLastLat()).isNotNull();
        assertThat(updated.getLastLon()).isNotNull();
        assertThat(updated.getAirborne()).isTrue();
        assertThat(updated.getLastCallsign()).isEqualTo("N898TS");
    }

    @Test
    void livePositionSeederDoesNotOverwriteExistingPosition() throws Exception {
        Aircraft a = new Aircraft();
        a.setTailNumber("N628TS"); // in POSITIONS_EXTRA
        a.setLastLat(99.0); // already has real position data
        a.setLastLon(99.0);
        aircraftRepository.save(a);

        new CelebrityLivePositionSeeder(aircraftRepository).run(null);

        Aircraft unchanged = aircraftRepository.findByTailNumber("N628TS").get();
        assertThat(unchanged.getLastLat()).isEqualTo(99.0);
        assertThat(unchanged.getLastLon()).isEqualTo(99.0);
    }

    @Test
    void livePositionSeederSkipsTailNumberNotInDb() throws Exception {
        // No aircraft with "N898CE" in the DB — seeder must not throw
        new CelebrityLivePositionSeeder(aircraftRepository).run(null);

        assertThat(aircraftRepository.findByTailNumber("N898CE")).isEmpty();
    }

    // ── DefaultUserSeeder ─────────────────────────────────────────────────────

    @Test
    void defaultUserSeederCreatesAdminWhenNotPresent() throws Exception {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        new DefaultUserSeeder(userRepository, encoder).run(null);

        java.util.Optional<AppUser> admin = userRepository.findByUname("admin");
        assertThat(admin).isPresent();
        assertThat(encoder.matches("1234", admin.get().getUpass())).isTrue();
    }

    @Test
    void defaultUserSeederSkipsWhenAdminAlreadyExists() throws Exception {
        AppUser existing = new AppUser();
        existing.setUname("admin");
        existing.setUpass("already-hashed");
        userRepository.save(existing);

        PasswordEncoder encoder = new BCryptPasswordEncoder();
        new DefaultUserSeeder(userRepository, encoder).run(null);

        // still exactly one admin row, password untouched
        assertThat(userRepository.findByUname("admin").get().getUpass())
                .isEqualTo("already-hashed");
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void celebritySeederSkipsAircraftWithExistingTailNumber() throws Exception {
        // Pre-save one of the aircraft the seeder would normally create
        Aircraft preExisting = new Aircraft();
        preExisting.setTailNumber("N898TS");
        preExisting.setModel("Some Other Model");
        aircraftRepository.save(preExisting);

        CelebritySeeder seeder = new CelebritySeeder(celebrityRepository, aircraftRepository);
        seeder.run(null);

        // Only one aircraft with that tail number must exist
        assertThat(aircraftRepository.findByTailNumber("N898TS")).isPresent();
        assertThat(aircraftRepository.findByTailNumber("N898TS").get().getModel())
                .isEqualTo("Some Other Model"); // original not overwritten
    }

    // ── getRecentCachedFlights branch coverage ────────────────────────────────

    @Test
    void getRecentCachedFlights_skipsFlightWithNullCoordinates() {
        Flight flight = new Flight();
        flight.setCallsign("N-NOCOORD");
        flight.setDepartureTime(LocalDateTime.now().minusMinutes(5));
        // lat and lng intentionally left null
        flightRepository.save(flight);

        Map<String, Object> response = dbService.getRecentCachedFlights(30);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) response.get("flights");

        assertThat(response).containsEntry("count", 0);
        assertThat(flights).isEmpty();
    }

    @Test
    void getRecentCachedFlights_usesPlaceholderWhenOriginAndDestNull() {
        Flight flight = new Flight();
        flight.setCallsign("N-NROUTE");
        flight.setLat(43.0);
        flight.setLng(-89.0);
        flight.setDepartureTime(LocalDateTime.now().minusMinutes(5));
        // originApCode and destinationApCode intentionally null
        flightRepository.save(flight);

        Map<String, Object> response = dbService.getRecentCachedFlights(30);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) response.get("flights");

        assertThat(flights).hasSize(1);
        assertThat(flights.get(0)).containsEntry("origin", "???");
        assertThat(flights.get(0)).containsEntry("destination", "???");
    }

    @Test
    void getRecentCachedFlights_defaultsAltitudeAndVelocityToZeroWhenNull() {
        Flight flight = new Flight();
        flight.setCallsign("N-NULLAV");
        flight.setLat(43.0);
        flight.setLng(-89.0);
        flight.setDepartureTime(LocalDateTime.now().minusMinutes(5));
        // altitudeFt and velocityKts intentionally null
        flightRepository.save(flight);

        Map<String, Object> response = dbService.getRecentCachedFlights(30);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) response.get("flights");

        assertThat(flights).hasSize(1);
        assertThat(flights.get(0)).containsEntry("altitude", 0);
        assertThat(flights.get(0)).containsEntry("velocity", 0);
    }

    @Test
    void getRecentCachedFlights_setsLiveInfoNullForInvalidJson() {
        Flight flight = new Flight();
        flight.setCallsign("N-BADJSON");
        flight.setLat(43.0);
        flight.setLng(-89.0);
        flight.setLiveInfoJson("NOT_VALID_JSON{{{");
        flight.setDepartureTime(LocalDateTime.now().minusMinutes(5));
        flightRepository.save(flight);

        Map<String, Object> response = dbService.getRecentCachedFlights(30);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) response.get("flights");

        assertThat(flights).hasSize(1);
        assertThat(flights.get(0).get("liveInfo")).isNull();
    }

    @Test
    void getRecentCachedFlights_useVelocityBasedCo2WhenEstimateIsNull() {
        Flight flight = new Flight();
        flight.setCallsign("N-VELCO2");
        flight.setLat(43.0);
        flight.setLng(-89.0);
        flight.setVelocityKts(500);
        flight.setEstimatedCo2Kg(null); // triggers velocity fallback
        flight.setDepartureTime(LocalDateTime.now().minusHours(2));
        flightRepository.save(flight);

        Map<String, Object> response = dbService.getRecentCachedFlights(200);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) response.get("flights");

        assertThat(flights).hasSize(1);
        // velocity-based estimate should be non-null and positive
        Object co2 = flights.get(0).get("estimatedCo2Kg");
        assertThat(co2).isNotNull();
        assertThat(((Number) co2).doubleValue()).isGreaterThan(0.0);
    }

    // ── batchSaveFlights branch coverage ─────────────────────────────────────

    @Test
    void batchSaveFlights_defaultsAltitudeAndVelocityToZeroForNonNumeric() {
        Map<String, Object> flight = new LinkedHashMap<>();
        flight.put("callsign", "N-NONNUM");
        flight.put("altitude", "high"); // non-numeric → 0
        flight.put("velocity", "fast"); // non-numeric → 0
        flight.put("lat", 43.0);
        flight.put("lng", -89.0);

        Map<String, Object> result = dbService.batchSaveFlights(List.of(flight));

        assertThat(result).containsEntry("flightsSaved", 1);
        Flight saved = flightRepository.findAll().get(0);
        assertThat(saved.getAltitudeFt()).isEqualTo(0);
        assertThat(saved.getVelocityKts()).isEqualTo(0);
    }

    @Test
    void batchSaveFlights_treatsQuestionMarkOriginAsNullAndSkipsAirportSave() {
        Map<String, Object> flight = new LinkedHashMap<>();
        flight.put("callsign", "N-QMARK");
        flight.put("origin", "???");      // should NOT create airport, should store null in flight
        flight.put("destination", "???"); // same
        flight.put("lat", 43.0);
        flight.put("lng", -89.0);

        Map<String, Object> result = dbService.batchSaveFlights(List.of(flight));

        assertThat(result).containsEntry("flightsSaved", 1);
        assertThat(airportRepository.count()).isEqualTo(0); // no airports created for "???"
        Flight saved = flightRepository.findAll().get(0);
        assertThat(saved.getOriginApCode()).isNull();
        assertThat(saved.getDestinationApCode()).isNull();
    }

    @Test
    void batchSaveFlights_usesUnknownTailWhenCallsignIsNull() {
        Map<String, Object> flight = new LinkedHashMap<>();
        // callsign intentionally absent → tailNumber becomes "UNKNOWN"
        flight.put("lat", 43.0);
        flight.put("lng", -89.0);

        Map<String, Object> result = dbService.batchSaveFlights(List.of(flight));

        assertThat(result).containsEntry("flightsSaved", 1);
        assertThat(aircraftRepository.findByTailNumber("UNKNOWN")).isPresent();
    }

    // ── fetchAndSaveFlight branch coverage ───────────────────────────────────

    @Test
    void fetchAndSaveFlight_returnsErrorWhenAeroApiReturnsNon2xx() {
        when(flightService.getFlightByIdent("N-FAIL503"))
                .thenReturn(ResponseEntity.status(503).body(null));

        Map<String, Object> result = dbService.fetchAndSaveFlight("N-FAIL503");

        assertThat(result).containsKey("error");
        assertThat(result.get("error").toString()).contains("503");
    }

    @Test
    void fetchAndSaveFlight_returnsErrorWhenAeroApiBodyIsUnparseable() {
        when(flightService.getFlightByIdent("N-BADBODY"))
                .thenReturn(ResponseEntity.ok("NOT_JSON{{"));

        Map<String, Object> result = dbService.fetchAndSaveFlight("N-BADBODY");

        assertThat(result).containsKey("error");
        assertThat(result.get("error").toString()).contains("Failed to parse AeroAPI response");
    }

    @Test
    void fetchAndSaveFlight_usesExistingAircraftWhenAlreadyInDb() {
        // Pre-seed aircraft — fetchAndSaveFlight should find it (existing.isPresent) branch
        Aircraft preExisting = new Aircraft();
        preExisting.setTailNumber("N-PREEX1");
        preExisting.setModel("OldModel");
        aircraftRepository.save(preExisting);

        String body = """
                {
                  "flights": [
                    {
                      "registration": "N-PREEX1",
                      "aircraft_type": "B738",
                      "origin": { "code": "KATL", "name": "Atlanta", "city": "Atlanta", "country_code": "US" },
                      "destination": { "code": "KDEN", "name": "Denver", "city": "Denver", "country_code": "US" },
                      "scheduled_out": "2026-03-17T10:00:00Z",
                      "scheduled_in": "2026-03-17T13:00:00Z"
                    }
                  ]
                }
                """;
        when(flightService.getFlightByIdent("N-PREEX1"))
                .thenReturn(ResponseEntity.ok(body));

        Map<String, Object> result = dbService.fetchAndSaveFlight("N-PREEX1");

        assertThat(result).containsEntry("source", "aeroapi");
        Aircraft aircraft = (Aircraft) result.get("aircraft");
        // Must reuse existing row, not create a second one
        assertThat(aircraftRepository.count()).isEqualTo(1);
        assertThat(aircraft.getModel()).isEqualTo("OldModel"); // not overwritten by AeroAPI
    }

    @Test
    void fetchAndSaveFlight_usesIdentWhenRegistrationNodeMissing() {
        String body = """
                {
                  "flights": [
                    {
                      "aircraft_type": "C172",
                      "origin": { "code": "KMSN", "name": "Madison", "city": "Madison", "country_code": "US" },
                      "destination": { "code": "KORD", "name": "OHare", "city": "Chicago", "country_code": "US" },
                      "scheduled_out": "2026-03-17T10:00:00Z",
                      "scheduled_in": "2026-03-17T11:30:00Z"
                    }
                  ]
                }
                """;
        when(flightService.getFlightByIdent("N-NOREG1"))
                .thenReturn(ResponseEntity.ok(body));

        Map<String, Object> result = dbService.fetchAndSaveFlight("N-NOREG1");

        assertThat(result).containsEntry("source", "aeroapi");
        // Aircraft created using ident as tail number when registration node is missing
        assertThat(aircraftRepository.findByTailNumber("N-NOREG1")).isPresent();
    }

    // ── fetchLivePosition branch coverage ─────────────────────────────────────

    @Test
    void fetchLivePosition_returnsNotAirborneWhenResponseBodyIsNull() {
        when(airplanesLiveService.getByReg("N-NULLBODY"))
                .thenReturn(ResponseEntity.ok((String) null)); // 2xx but null body

        Map<String, Object> result = dbService.fetchLivePosition("N-NULLBODY");

        assertThat(result).containsEntry("airborne", false);
        assertThat(result.get("message").toString()).contains("No response from airplanes.live");
    }

    @Test
    void fetchLivePosition_skipsDbEnrichmentWhenAircraftNotInDb() {
        String body = """
                {
                  "ac": [{
                    "hex": "aaa111",
                    "flight": "N-NODB1",
                    "lat": 43.0, "lon": -89.0,
                    "alt_baro": 8000, "gs": 300.0, "track": 90.0
                  }]
                }
                """;
        when(airplanesLiveService.getByReg("N-NODB1"))
                .thenReturn(ResponseEntity.ok(body));

        // N-NODB1 is NOT in the aircraft repository — no DB enrichment
        Map<String, Object> result = dbService.fetchLivePosition("N-NODB1");

        assertThat(result).containsEntry("airborne", true);
        assertThat(result).doesNotContainKey("dbAircraftId");
        assertThat(result).doesNotContainKey("ownerId");
    }

    @Test
    void fetchLivePosition_handlesNonNumericAltitudeBaro() {
        String body = """
                {
                  "ac": [{
                    "hex": "bbb222",
                    "flight": "N-GROUND1",
                    "lat": 43.0, "lon": -89.0,
                    "alt_baro": "ground",
                    "gs": 0.0, "track": 0.0
                  }]
                }
                """;
        when(airplanesLiveService.getByReg("N-GROUND1"))
                .thenReturn(ResponseEntity.ok(body));

        Map<String, Object> result = dbService.fetchLivePosition("N-GROUND1");

        assertThat(result).containsEntry("airborne", true);
        assertThat(result.get("altitude")).isEqualTo("ground");
    }

    // ── saveLivePosition branch coverage ─────────────────────────────────────

    @Test
    void saveLivePosition_returnsSavedFalseWhenResponseIsNon2xx() {
        when(airplanesLiveService.getByReg("N-SAVEFAIL"))
                .thenReturn(ResponseEntity.status(502).body(null));

        Map<String, Object> result = dbService.saveLivePosition("N-SAVEFAIL");

        assertThat(result).containsEntry("tailNumber", "N-SAVEFAIL");
        assertThat(result).containsEntry("saved", false);
        assertThat(result.get("message").toString()).contains("No response from airplanes.live");
    }

    @Test
    void saveLivePosition_returnsSavedFalseWhenBodyIsNull() {
        when(airplanesLiveService.getByReg("N-SAVNULL"))
                .thenReturn(ResponseEntity.ok((String) null));

        Map<String, Object> result = dbService.saveLivePosition("N-SAVNULL");

        assertThat(result).containsEntry("tailNumber", "N-SAVNULL");
        assertThat(result).containsEntry("saved", false);
    }

    // ── getCelebrityFlights branch coverage ──────────────────────────────────

    @Test
    void getCelebrityFlights_returnsMessageWhenNoAircraftLinked() {
        Celebrity celeb = new Celebrity();
        celeb.setName("No Aircraft Celeb");
        celeb.setIndustry("Tech");
        celeb = celebrityRepository.save(celeb);

        Map<String, Object> result = dbService.getCelebrityFlights(celeb.getId(), null);

        assertThat(result).containsKey("message");
        assertThat(result.get("message").toString()).contains("No aircraft linked");
    }

    @Test
    void getCelebrityFlights_skipsAircraftWithBlankTailNumber() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Blank Tail Celeb");
        celeb = celebrityRepository.save(celeb);

        Aircraft aircraft = new Aircraft();
        aircraft.setOwnerId(celeb.getId());
        aircraft.setTailNumber(""); // blank → should be skipped
        aircraft = aircraftRepository.save(aircraft);

        Map<String, Object> result = dbService.getCelebrityFlights(celeb.getId(), null);

        // Should return empty flights, not throw
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flights = (List<Map<String, Object>>) result.get("flights");
        assertThat(flights).isEmpty();
        assertThat(result).containsEntry("flightCount", 0);
    }

    @Test
    void getCelebrityFlights_collectsErrorsWhenFetchFails() {
        Celebrity celeb = new Celebrity();
        celeb.setName("Error Celeb");
        celeb = celebrityRepository.save(celeb);

        Aircraft aircraft = new Aircraft();
        aircraft.setTailNumber("N-ERRFETCH");
        aircraft.setOwnerId(celeb.getId());
        aircraft = aircraftRepository.save(aircraft);

        // flightService.getFlightByIdent throws → fetchAndSaveFlight wraps in error map
        when(flightService.getFlightByIdent("N-ERRFETCH"))
                .thenThrow(new RuntimeException("simulated AeroAPI failure"));

        Map<String, Object> result = dbService.getCelebrityFlights(celeb.getId(), null);

        assertThat(result).containsKey("errors");
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("N-ERRFETCH");
    }

    // ── getCo2Summary branch coverage ─────────────────────────────────────────

    @Test
    void getCo2Summary_returnsNullComparisonWhenOriginCodeIsNull() {
        // Origin code explicitly null, dest has a code but the airport coords are absent
        Airport dest = new Airport();
        dest.setApCode("KORD");
        dest.setLat(41.97);
        dest.setLon(-87.91);
        airportRepository.save(dest);

        Flight flight = new Flight();
        flight.setCallsign("UAL300");
        flight.setOriginApCode(null);   // null origin → origin object will be null
        flight.setDestinationApCode("KORD");
        flight.setEstimatedCo2Kg(500.0);
        Flight saved = flightRepository.save(flight);

        Map<String, Object> result = dbService.getCo2Summary(saved.getId());

        assertThat(result).containsEntry("flightId", saved.getId());
        assertThat(result).containsEntry("comparison", null);
    }

    @Test
    void getCo2Summary_returnsNullComparisonWhenDestCodeIsNull() {
        Airport origin = new Airport();
        origin.setApCode("KMSN");
        origin.setLat(43.14);
        origin.setLon(-89.34);
        airportRepository.save(origin);

        Flight flight = new Flight();
        flight.setCallsign("UAL400");
        flight.setOriginApCode("KMSN");
        flight.setDestinationApCode(null); // null dest → dest object will be null
        flight.setEstimatedCo2Kg(600.0);
        Flight saved = flightRepository.save(flight);

        Map<String, Object> result = dbService.getCo2Summary(saved.getId());

        assertThat(result).containsEntry("flightId", saved.getId());
        assertThat(result).containsEntry("comparison", null);
    }

    // ------------------------------------------------------------------ getUserTrackedFlights

    @Test
    void getUserTrackedFlightsReturnsEmptyListWhenNoneExist() {
        List<Map<String, Object>> result = dbService.getUserTrackedFlights(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void getUserTrackedFlightsReturnsOnlyFlightsForRequestedUser() {
        UserTrackedFlight a = new UserTrackedFlight();
        a.setUserId(1L);
        a.setCallsign("UAL123");
        a.setUpdatedAt(LocalDateTime.now());
        userTrackedFlightRepository.save(a);

        UserTrackedFlight b = new UserTrackedFlight();
        b.setUserId(2L);
        b.setCallsign("DAL456");
        b.setUpdatedAt(LocalDateTime.now());
        userTrackedFlightRepository.save(b);

        List<Map<String, Object>> result = dbService.getUserTrackedFlights(1L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("callsign")).isEqualTo("UAL123");
    }

    @Test
    void getUserTrackedFlightsMapsAllFields() {
        UserTrackedFlight f = new UserTrackedFlight();
        f.setUserId(10L);
        f.setCallsign("SWA789");
        f.setTailNumber("N789SW");
        f.setOrigin("ORD");
        f.setDestination("LAX");
        f.setAltitude(35000);
        f.setVelocity(450);
        f.setLat(41.9);
        f.setLng(-87.6);
        f.setBearing(270.0);
        f.setEstimatedCo2Kg(1200.0);
        f.setCategory("celebrity");
        f.setCelebrity("Taylor Swift");
        f.setUpdatedAt(LocalDateTime.now());
        userTrackedFlightRepository.save(f);

        List<Map<String, Object>> result = dbService.getUserTrackedFlights(10L);
        assertThat(result).hasSize(1);
        Map<String, Object> row = result.get(0);
        assertThat(row.get("callsign")).isEqualTo("SWA789");
        assertThat(row.get("tailNumber")).isEqualTo("N789SW");
        assertThat(row.get("origin")).isEqualTo("ORD");
        assertThat(row.get("destination")).isEqualTo("LAX");
        assertThat(row.get("altitude")).isEqualTo(35000);
        assertThat(row.get("velocity")).isEqualTo(450);
        assertThat(row.get("lat")).isEqualTo(41.9);
        assertThat(row.get("lng")).isEqualTo(-87.6);
        assertThat(row.get("bearing")).isEqualTo(270.0);
        assertThat(row.get("estimatedCo2Kg")).isEqualTo(1200.0);
        assertThat(row.get("category")).isEqualTo("celebrity");
        assertThat(row.get("celebrity")).isEqualTo("Taylor Swift");
    }

    @Test
    void getUserTrackedFlightsReplacesNullOriginAndDestinationWithNA() {
        UserTrackedFlight f = new UserTrackedFlight();
        f.setUserId(11L);
        f.setCallsign("AAL100");
        f.setOrigin(null);
        f.setDestination(null);
        f.setUpdatedAt(LocalDateTime.now());
        userTrackedFlightRepository.save(f);

        List<Map<String, Object>> result = dbService.getUserTrackedFlights(11L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("origin")).isEqualTo("N/A");
        assertThat(result.get(0).get("destination")).isEqualTo("N/A");
    }

    @Test
    void getUserTrackedFlightsReplacesNullAltitudeAndVelocityWithZero() {
        UserTrackedFlight f = new UserTrackedFlight();
        f.setUserId(12L);
        f.setCallsign("BAW200");
        f.setAltitude(null);
        f.setVelocity(null);
        f.setUpdatedAt(LocalDateTime.now());
        userTrackedFlightRepository.save(f);

        List<Map<String, Object>> result = dbService.getUserTrackedFlights(12L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("altitude")).isEqualTo(0);
        assertThat(result.get(0).get("velocity")).isEqualTo(0);
    }

    @Test
    void getUserTrackedFlightsReturnsNewestFirst() {
        LocalDateTime earlier = LocalDateTime.now().minusHours(1);
        LocalDateTime later   = LocalDateTime.now();

        UserTrackedFlight old = new UserTrackedFlight();
        old.setUserId(13L);
        old.setCallsign("OLD001");
        old.setUpdatedAt(earlier);
        userTrackedFlightRepository.save(old);

        UserTrackedFlight recent = new UserTrackedFlight();
        recent.setUserId(13L);
        recent.setCallsign("NEW001");
        recent.setUpdatedAt(later);
        userTrackedFlightRepository.save(recent);

        List<Map<String, Object>> result = dbService.getUserTrackedFlights(13L);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("callsign")).isEqualTo("NEW001");
        assertThat(result.get(1).get("callsign")).isEqualTo("OLD001");
    }

    // ------------------------------------------------------------------ upsertUserTrackedFlight

    @Test
    void upsertUserTrackedFlightBlankCallsignReturnsError() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callsign", "   ");

        Map<String, Object> result = dbService.upsertUserTrackedFlight(1L, payload);
        assertThat(result.get("ok")).isEqualTo(false);
        assertThat(result.get("error").toString()).contains("callsign");
        assertThat(userTrackedFlightRepository.count()).isEqualTo(0);
    }

    @Test
    void upsertUserTrackedFlightNullCallsignReturnsError() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callsign", null);

        Map<String, Object> result = dbService.upsertUserTrackedFlight(1L, payload);
        assertThat(result.get("ok")).isEqualTo(false);
        assertThat(userTrackedFlightRepository.count()).isEqualTo(0);
    }

    @Test
    void upsertUserTrackedFlightInsertsNewRecord() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callsign", "UAL500");
        payload.put("tailNumber", "N500UA");
        payload.put("origin", "ORD");
        payload.put("destination", "SFO");
        payload.put("altitude", 36000);
        payload.put("velocity", 480);
        payload.put("lat", 41.9);
        payload.put("lng", -87.6);
        payload.put("bearing", 270.0);
        payload.put("estimatedCo2Kg", 1500.0);
        payload.put("category", null);
        payload.put("celebrity", null);

        Map<String, Object> result = dbService.upsertUserTrackedFlight(20L, payload);

        assertThat(result.get("ok")).isEqualTo(true);
        assertThat(result.get("callsign")).isEqualTo("UAL500");
        assertThat(result.get("id")).isNotNull();
        assertThat(result.get("updatedAt")).isNotNull();

        List<Map<String, Object>> stored = dbService.getUserTrackedFlights(20L);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).get("tailNumber")).isEqualTo("N500UA");
        assertThat(stored.get(0).get("origin")).isEqualTo("ORD");
        assertThat(stored.get(0).get("destination")).isEqualTo("SFO");
        assertThat(stored.get(0).get("altitude")).isEqualTo(36000);
        assertThat(stored.get(0).get("velocity")).isEqualTo(480);
    }

    @Test
    void upsertUserTrackedFlightUpdatesExistingRecord() {
        // Insert first
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("callsign", "DAL600");
        first.put("origin", "ATL");
        dbService.upsertUserTrackedFlight(21L, first);

        assertThat(userTrackedFlightRepository.count()).isEqualTo(1);

        // Upsert again with updated origin
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("callsign", "DAL600");
        update.put("origin", "JFK");
        dbService.upsertUserTrackedFlight(21L, update);

        // Still only one row
        assertThat(userTrackedFlightRepository.count()).isEqualTo(1);

        List<Map<String, Object>> stored = dbService.getUserTrackedFlights(21L);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).get("origin")).isEqualTo("JFK");
    }

    @Test
    void upsertUserTrackedFlightStoresNullableFieldsAsNull() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callsign", "NUL001");
        // all optional fields absent

        Map<String, Object> result = dbService.upsertUserTrackedFlight(22L, payload);
        assertThat(result.get("ok")).isEqualTo(true);

        List<Map<String, Object>> stored = dbService.getUserTrackedFlights(22L);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).get("tailNumber")).isNull();
        assertThat(stored.get(0).get("origin")).isEqualTo("N/A");   // null → "N/A" in getter
        assertThat(stored.get(0).get("destination")).isEqualTo("N/A");
        assertThat(stored.get(0).get("altitude")).isEqualTo(0);     // null → 0 in getter
        assertThat(stored.get(0).get("velocity")).isEqualTo(0);
        assertThat(stored.get(0).get("estimatedCo2Kg")).isNull();
        assertThat(stored.get(0).get("celebrity")).isNull();
    }

    @Test
    void upsertUserTrackedFlightTrimsWhitespaceFromCallsign() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callsign", "  AAL999  ");

        Map<String, Object> result = dbService.upsertUserTrackedFlight(23L, payload);
        assertThat(result.get("ok")).isEqualTo(true);
        assertThat(result.get("callsign")).isEqualTo("AAL999");
    }

    @Test
    void upsertUserTrackedFlightIsolatesDataBetweenUsers() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callsign", "UAL700");

        dbService.upsertUserTrackedFlight(30L, payload);
        dbService.upsertUserTrackedFlight(31L, payload);

        // Each user sees only their own record
        assertThat(dbService.getUserTrackedFlights(30L)).hasSize(1);
        assertThat(dbService.getUserTrackedFlights(31L)).hasSize(1);
        assertThat(userTrackedFlightRepository.count()).isEqualTo(2);
    }
}