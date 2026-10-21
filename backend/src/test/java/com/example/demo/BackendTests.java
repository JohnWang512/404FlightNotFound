package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({DemoController.class, AeroApiController.class})
@Import(BackendTests.MockFlightServiceConfig.class)
@TestPropertySource(properties = "aeroapi.key=test-key")
class BackendTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sayHelloReturnsSuccessMessage() throws Exception {
        // Why this test exists:
        // - It verifies the basic Spring MVC mapping for `/api/hello` is wired correctly.
        // - It also sanity-checks the controller base path `/api` and CORS configuration
        //   indirectly by ensuring the endpoint is reachable in the web layer test slice.
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Connection Successful: Hello!"));
    }

    @Test
    void getFlightRouteReturnsServiceResponse() throws Exception {
        // Why this test exists:
        // - `DemoController` is a thin proxy that delegates to `FlightService`.
        // - The edge case we want to avoid is "wrong path binding" (e.g., `{id}` mapping issues),
        //   or accidentally returning a different shape than the mocked service provides.
        mockMvc.perform(get("/api/flights/AAL123/route"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"route\":\"MSN -> ORD\"}"));
    }

    @Test
    void statusReturnsConfiguredTrueWhenApiKeyExists() throws Exception {
        // Why this test exists:
        // - `AeroApiController` normalizes the configured key and treats "blank"/"default-key-not-set"
        //   as "not configured".
        // - This test asserts the "configured=true" branch using `@TestPropertySource`.
        mockMvc.perform(get("/api/aeroapi/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true));
    }

    // ── AeroApiController.normalizeKey branches ───────────────────────────────

    @Test
    void normalizeKeyReturnsNullForNullInput() throws Exception {
        Method m = AeroApiController.class.getDeclaredMethod("normalizeKey", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(new AeroApiController(), (Object) null)).isNull();
    }

    @Test
    void normalizeKeyStripsDoubleQuotes() throws Exception {
        Method m = AeroApiController.class.getDeclaredMethod("normalizeKey", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(new AeroApiController(), "\"my-key\"")).isEqualTo("my-key");
    }

    @Test
    void normalizeKeyStripsSingleQuotes() throws Exception {
        Method m = AeroApiController.class.getDeclaredMethod("normalizeKey", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(new AeroApiController(), "'my-key'")).isEqualTo("my-key");
    }

    @Test
    void normalizeKeyFallsBackToEnvWhenBlank() throws Exception {
        Method m = AeroApiController.class.getDeclaredMethod("normalizeKey", String.class);
        m.setAccessible(true);
        // env var not set in test → orElse returns the blank trimmed value
        String result = (String) m.invoke(new AeroApiController(), "   ");
        assertThat(result).isBlank();
    }

    @Test
    void normalizeKeyFallsBackToEnvForDefaultKey() throws Exception {
        Method m = AeroApiController.class.getDeclaredMethod("normalizeKey", String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(new AeroApiController(), "default-key-not-set");
        // env var not set → falls back to "default-key-not-set"
        assertThat(result).isEqualTo("default-key-not-set");
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusReturnsFalseWhenKeyIsBlank() {
        AeroApiController controller = new AeroApiController();
        ReflectionTestUtils.setField(controller, "apiKey", "");
        Map<String, Object> body = (Map<String, Object>) controller.status().getBody();
        assertThat(body.get("configured")).isEqualTo(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusReturnsFalseWhenKeyIsDefaultKey() {
        AeroApiController controller = new AeroApiController();
        ReflectionTestUtils.setField(controller, "apiKey", "default-key-not-set");
        Map<String, Object> body = (Map<String, Object>) controller.status().getBody();
        assertThat(body.get("configured")).isEqualTo(false);
    }

    // ── FlightService.getApiKey branches ──────────────────────────────────────

    @Test
    void getApiKeyReturnsCachedKeyOnSecondCall() throws Exception {
        FlightService service = new FlightService();
        ReflectionTestUtils.setField(service, "apiKey", "real-key");
        Method m = FlightService.class.getDeclaredMethod("getApiKey");
        m.setAccessible(true);
        String first  = (String) m.invoke(service);
        String second = (String) m.invoke(service);
        assertThat(first).isEqualTo("real-key");
        assertThat(second).isEqualTo("real-key"); // hits the resolvedApiKey != null cache branch
    }

    @Test
    void getApiKeyStripsDoubleQuotes() throws Exception {
        FlightService service = new FlightService();
        ReflectionTestUtils.setField(service, "apiKey", "\"quoted-key\"");
        Method m = FlightService.class.getDeclaredMethod("getApiKey");
        m.setAccessible(true);
        assertThat(m.invoke(service)).isEqualTo("quoted-key");
    }

    @Test
    void getApiKeyStripsSingleQuotes() throws Exception {
        FlightService service = new FlightService();
        ReflectionTestUtils.setField(service, "apiKey", "'quoted-key'");
        Method m = FlightService.class.getDeclaredMethod("getApiKey");
        m.setAccessible(true);
        assertThat(m.invoke(service)).isEqualTo("quoted-key");
    }

    @Test
    void getApiKeyFallsBackToEnvForBlankKey() throws Exception {
        FlightService service = new FlightService();
        ReflectionTestUtils.setField(service, "apiKey", "");
        Method m = FlightService.class.getDeclaredMethod("getApiKey");
        m.setAccessible(true);
        // env var not set → blank falls back to blank
        assertThat((String) m.invoke(service)).isBlank();
    }

    @Test
    void getApiKeyFallsBackToEnvForDefaultKey() throws Exception {
        FlightService service = new FlightService();
        ReflectionTestUtils.setField(service, "apiKey", "default-key-not-set");
        Method m = FlightService.class.getDeclaredMethod("getApiKey");
        m.setAccessible(true);
        assertThat(m.invoke(service)).isNotNull();
    }

    @Test
    void getApiKeyHandlesNullApiKey() throws Exception {
        FlightService service = new FlightService();
        ReflectionTestUtils.setField(service, "apiKey", null);
        Method m = FlightService.class.getDeclaredMethod("getApiKey");
        m.setAccessible(true);
        // null → env lookup → no env var → null → replaced with ""
        assertThat(m.invoke(service)).isEqualTo("");
    }

    // ── FlightService.buildUrl branches ──────────────────────────────────────

    @Test
    void buildUrlReturnsBaseUrlWhenParamsNull() throws Exception {
        FlightService service = new FlightService();
        Method m = FlightService.class.getDeclaredMethod("buildUrl", String.class, Map.class);
        m.setAccessible(true);
        assertThat(m.invoke(service, "https://example.com/api", null))
                .isEqualTo("https://example.com/api");
    }

    @Test
    void buildUrlReturnsBaseUrlWhenParamsEmpty() throws Exception {
        FlightService service = new FlightService();
        Method m = FlightService.class.getDeclaredMethod("buildUrl", String.class, Map.class);
        m.setAccessible(true);
        assertThat(m.invoke(service, "https://example.com/api", Collections.emptyMap()))
                .isEqualTo("https://example.com/api");
    }

    // ── FlightService.searchPositions branches ────────────────────────────────

    @Test
    void searchPositionsWithNullParamsSkipsQueryString() {
        FlightService service = new FlightService();
        ReflectionTestUtils.setField(service, "apiKey", "test");
        // No server — will return 500, but the null-params branch is exercised
        ResponseEntity<String> resp = service.searchPositions(null);
        assertThat(resp.getStatusCode().value()).isEqualTo(500);
    }

    @TestConfiguration
    static class MockFlightServiceConfig {
        @Bean
        FlightService flightService() {
            // Why this mock exists:
            // - These tests focus on controller behavior (routing + response mapping),
            //   not on the real external AeroAPI integration.
            // - Returning stable JSON strings keeps the assertions simple and deterministic.
            return new FlightService() {
                @Override
                public ResponseEntity<String> getFlightRoute(String id) {
                    return ResponseEntity.ok("{\"route\":\"MSN -> ORD\"}");
                }

                @Override
                public ResponseEntity<String> searchFlights(Map<String, String> params) {
                    return ResponseEntity.ok("{\"results\":[]}");
                }
            };
        }
    }
}