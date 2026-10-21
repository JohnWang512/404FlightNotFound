package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("manual_http")
class AirplanesLiveServiceHttpTests {

    private static final String BASE_URL = "https://api.airplanes.live/v2";

    private AirplanesLiveService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new AirplanesLiveService();

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        assertThat(restTemplate).isNotNull();

        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    // -------------------------------------------------------------------------
    // Happy-path tests: each method hits the correct URL and returns the body
    // -------------------------------------------------------------------------

    @Test
    void getByRegSendsCorrectUrlAndReturnsBody() {
        server.expect(once(), requestTo(BASE_URL + "/reg/N12345"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"reg\":\"N12345\"}]}", MediaType.APPLICATION_JSON));

        var resp = service.getByReg("N12345");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"reg\":\"N12345\"}]}");
        server.verify();
    }

    @Test
    void getByHexSendsCorrectUrlAndReturnsBody() {
        server.expect(once(), requestTo(BASE_URL + "/hex/A1B2C3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"hex\":\"A1B2C3\"}]}", MediaType.APPLICATION_JSON));

        var resp = service.getByHex("A1B2C3");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"hex\":\"A1B2C3\"}]}");
        server.verify();
    }

    @Test
    void getByCallsignSendsCorrectUrlAndReturnsBody() {
        server.expect(once(), requestTo(BASE_URL + "/callsign/UAL123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"flight\":\"UAL123\"}]}", MediaType.APPLICATION_JSON));

        var resp = service.getByCallsign("UAL123");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"flight\":\"UAL123\"}]}");
        server.verify();
    }

    @Test
    void getByPointSendsCorrectUrlAndReturnsBody() {
        server.expect(once(), requestTo(BASE_URL + "/point/43.07/-89.4/50"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[]}", MediaType.APPLICATION_JSON));

        var resp = service.getByPoint(43.07, -89.4, 50);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[]}");
        server.verify();
    }

    @Test
    void getMilitarySendsCorrectUrlAndReturnsBody() {
        server.expect(once(), requestTo(BASE_URL + "/mil"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"mil\":true}]}", MediaType.APPLICATION_JSON));

        var resp = service.getMilitary();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"mil\":true}]}");
        server.verify();
    }

    @Test
    void getLaddSendsCorrectUrlAndReturnsBody() {
        server.expect(once(), requestTo(BASE_URL + "/ladd"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"ladd\":true}]}", MediaType.APPLICATION_JSON));

        var resp = service.getLadd();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"ladd\":true}]}");
        server.verify();
    }

    @Test
    void getPiaSendsCorrectUrlAndReturnsBody() {
        server.expect(once(), requestTo(BASE_URL + "/pia"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"pia\":true}]}", MediaType.APPLICATION_JSON));

        var resp = service.getPia();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"pia\":true}]}");
        server.verify();
    }

    // -------------------------------------------------------------------------
    // Edge-case: point endpoint with boundary radius values
    // -------------------------------------------------------------------------

    @Test
    void getByPointWithZeroRadiusSendsCorrectUrl() {
        server.expect(once(), requestTo(BASE_URL + "/point/0.0/0.0/0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[]}", MediaType.APPLICATION_JSON));

        var resp = service.getByPoint(0.0, 0.0, 0);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        server.verify();
    }

    @Test
    void getByPointWithMaxRadiusSendsCorrectUrl() {
        server.expect(once(), requestTo(BASE_URL + "/point/51.5/-0.12/250"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[]}", MediaType.APPLICATION_JSON));

        var resp = service.getByPoint(51.5, -0.12, 250);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        server.verify();
    }

    // -------------------------------------------------------------------------
    // Error handling: HttpClientErrorException (4xx) propagated correctly
    // -------------------------------------------------------------------------

    @Test
    void getByRegOnNotFoundReturns404WithBody() {
        server.expect(once(), requestTo(BASE_URL + "/reg/UNKNOWN"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"not found\"}"));

        var resp = service.getByReg("UNKNOWN");

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        assertThat(resp.getBody()).contains("not found");
        server.verify();
    }

    @Test
    void getByHexOnUnauthorizedReturns401WithBody() {
        server.expect(once(), requestTo(BASE_URL + "/hex/BADKEY"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"unauthorized\"}"));

        var resp = service.getByHex("BADKEY");

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getBody()).contains("unauthorized");
        server.verify();
    }

    @Test
    void getByCallsignOnForbiddenReturns403() {
        server.expect(once(), requestTo(BASE_URL + "/callsign/BLOCKED"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"forbidden\"}"));

        var resp = service.getByCallsign("BLOCKED");

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
        server.verify();
    }

    @Test
    void getMilitaryOnTooManyRequestsReturns429() {
        server.expect(once(), requestTo(BASE_URL + "/mil"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"rate limit exceeded\"}"));

        var resp = service.getMilitary();

        assertThat(resp.getStatusCode().value()).isEqualTo(429);
        assertThat(resp.getBody()).contains("rate limit");
        server.verify();
    }

    // -------------------------------------------------------------------------
    // Error handling: ResourceAccessException → 503 with JSON error body
    // -------------------------------------------------------------------------

    @Test
    void getByRegOnConnectionFailureReturns503() {
        server.expect(once(), requestTo(BASE_URL + "/reg/N12345"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Connection refused")));

        var resp = service.getByReg("N12345");

        assertThat(resp.getStatusCode().value()).isEqualTo(503);
        assertThat(resp.getBody()).contains("Service unavailable");
        server.verify();
    }

    @Test
    void getByHexOnConnectionFailureReturns503() {
        server.expect(once(), requestTo(BASE_URL + "/hex/A1B2C3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Read timed out")));

        var resp = service.getByHex("A1B2C3");

        assertThat(resp.getStatusCode().value()).isEqualTo(503);
        assertThat(resp.getBody()).contains("Service unavailable");
        server.verify();
    }

    @Test
    void getByPointOnConnectionFailureReturns503() {
        server.expect(once(), requestTo(BASE_URL + "/point/43.07/-89.4/50"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Network unreachable")));

        var resp = service.getByPoint(43.07, -89.4, 50);

        assertThat(resp.getStatusCode().value()).isEqualTo(503);
        assertThat(resp.getBody()).contains("Service unavailable");
        server.verify();
    }

    @Test
    void getMilitaryOnConnectionFailureReturns503() {
        server.expect(once(), requestTo(BASE_URL + "/mil"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Connection refused")));

        var resp = service.getMilitary();

        assertThat(resp.getStatusCode().value()).isEqualTo(503);
        assertThat(resp.getBody()).contains("Service unavailable");
        server.verify();
    }

    @Test
    void getLaddOnConnectionFailureReturns503() {
        server.expect(once(), requestTo(BASE_URL + "/ladd"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Connection refused")));

        var resp = service.getLadd();

        assertThat(resp.getStatusCode().value()).isEqualTo(503);
        assertThat(resp.getBody()).contains("Service unavailable");
        server.verify();
    }

    @Test
    void getPiaOnConnectionFailureReturns503() {
        server.expect(once(), requestTo(BASE_URL + "/pia"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Connection refused")));

        var resp = service.getPia();

        assertThat(resp.getStatusCode().value()).isEqualTo(503);
        assertThat(resp.getBody()).contains("Service unavailable");
        server.verify();
    }

    // -------------------------------------------------------------------------
    // Response body pass-through: service must not alter the raw JSON
    // -------------------------------------------------------------------------

    @Test
    void getByRegReturnsBodyUnmodified() {
        String rawJson = "{\"ac\":[{\"hex\":\"ABC123\",\"flight\":\"DAL456\",\"alt_baro\":35000}],\"total\":1}";

        server.expect(once(), requestTo(BASE_URL + "/reg/N456SW"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(rawJson, MediaType.APPLICATION_JSON));

        var resp = service.getByReg("N456SW");

        assertThat(resp.getBody()).isEqualTo(rawJson);
        server.verify();
    }

    @Test
    void getByCallsignReturnsBodyUnmodified() {
        String rawJson = "{\"ac\":[{\"flight\":\"SWA100\",\"lat\":33.9,\"lon\":-118.4}],\"total\":1}";

        server.expect(once(), requestTo(BASE_URL + "/callsign/SWA100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(rawJson, MediaType.APPLICATION_JSON));

        var resp = service.getByCallsign("SWA100");

        assertThat(resp.getBody()).isEqualTo(rawJson);
        server.verify();
    }
}
