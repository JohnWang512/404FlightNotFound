package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
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

class AirplanesLiveServiceTests {

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
    // getByReg
    // -------------------------------------------------------------------------

    @Test
    void getByRegReturns200WithBody() {
        server.expect(once(), requestTo(BASE_URL + "/reg/N12345"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"reg\":\"N12345\"}]}", MediaType.APPLICATION_JSON));

        var resp = service.getByReg("N12345");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"reg\":\"N12345\"}]}");
        server.verify();
    }

    @Test
    void getByRegOnNotFoundReturns404() {
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
    void getByRegOnConnectionFailureReturns503() {
        server.expect(once(), requestTo(BASE_URL + "/reg/N12345"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Connection refused")));

        var resp = service.getByReg("N12345");

        assertThat(resp.getStatusCode().value()).isEqualTo(503);
        assertThat(resp.getBody()).contains("Service unavailable");
        server.verify();
    }

    // -------------------------------------------------------------------------
    // getByHex
    // -------------------------------------------------------------------------

    @Test
    void getByHexReturns200WithBody() {
        server.expect(once(), requestTo(BASE_URL + "/hex/A1B2C3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"hex\":\"A1B2C3\"}]}", MediaType.APPLICATION_JSON));

        var resp = service.getByHex("A1B2C3");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"hex\":\"A1B2C3\"}]}");
        server.verify();
    }

    @Test
    void getByHexOnNotFoundReturns404() {
        server.expect(once(), requestTo(BASE_URL + "/hex/000000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"not found\"}"));

        var resp = service.getByHex("000000");

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
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

    // -------------------------------------------------------------------------
    // getByCallsign
    // -------------------------------------------------------------------------

    @Test
    void getByCallsignReturns200WithBody() {
        server.expect(once(), requestTo(BASE_URL + "/callsign/UAL123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"flight\":\"UAL123\"}]}", MediaType.APPLICATION_JSON));

        var resp = service.getByCallsign("UAL123");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"flight\":\"UAL123\"}]}");
        server.verify();
    }

    @Test
    void getByCallsignOnNotFoundReturns404() {
        server.expect(once(), requestTo(BASE_URL + "/callsign/GHOST99"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"not found\"}"));

        var resp = service.getByCallsign("GHOST99");

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        server.verify();
    }

    @Test
    void getByCallsignOnConnectionFailureReturns503() {
        server.expect(once(), requestTo(BASE_URL + "/callsign/UAL123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Network unreachable")));

        var resp = service.getByCallsign("UAL123");

        assertThat(resp.getStatusCode().value()).isEqualTo(503);
        assertThat(resp.getBody()).contains("Service unavailable");
        server.verify();
    }

    // -------------------------------------------------------------------------
    // getMilitary
    // -------------------------------------------------------------------------

    @Test
    void getMilitaryReturns200WithBody() {
        server.expect(once(), requestTo(BASE_URL + "/mil"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"mil\":true}]}", MediaType.APPLICATION_JSON));

        var resp = service.getMilitary();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"mil\":true}]}");
        server.verify();
    }

    @Test
    void getMilitaryOnClientErrorReturns429() {
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

    // -------------------------------------------------------------------------
    // getLadd
    // -------------------------------------------------------------------------

    @Test
    void getLaddReturns200WithBody() {
        server.expect(once(), requestTo(BASE_URL + "/ladd"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"ladd\":true}]}", MediaType.APPLICATION_JSON));

        var resp = service.getLadd();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"ladd\":true}]}");
        server.verify();
    }

    @Test
    void getLaddOnClientErrorReturns403() {
        server.expect(once(), requestTo(BASE_URL + "/ladd"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"forbidden\"}"));

        var resp = service.getLadd();

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
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

    // -------------------------------------------------------------------------
    // getPia
    // -------------------------------------------------------------------------

    @Test
    void getPiaReturns200WithBody() {
        server.expect(once(), requestTo(BASE_URL + "/pia"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ac\":[{\"pia\":true}]}", MediaType.APPLICATION_JSON));

        var resp = service.getPia();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ac\":[{\"pia\":true}]}");
        server.verify();
    }

    @Test
    void getPiaOnClientErrorReturns401() {
        server.expect(once(), requestTo(BASE_URL + "/pia"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"unauthorized\"}"));

        var resp = service.getPia();

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
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
}
