package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Tag("manual_backend")
class SigmetServiceTests {

    private SigmetService service;
    private RestTemplate mockRestTemplate;

    @BeforeEach
    void setUp() throws Exception {
        service = new SigmetService();

        // Inject a mock RestTemplate via reflection (field is private final)
        mockRestTemplate = Mockito.mock(RestTemplate.class);
        Field field = SigmetService.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        field.set(service, mockRestTemplate);
    }

    // ── getCachedSigmets ──────────────────────────────────────────────────────

    @Test
    void getCachedSigmetsReturnsEmptyFeatureCollectionByDefault() {
        String cached = service.getCachedSigmets();
        assertThat(cached).contains("FeatureCollection");
        assertThat(cached).contains("features");
    }

    @Test
    void getCachedSigmetsIsValidJsonByDefault() {
        String cached = service.getCachedSigmets();
        assertThat(cached).startsWith("{");
        assertThat(cached).endsWith("}");
    }

    // ── fetchGlobalStorms ─────────────────────────────────────────────────────

    @Test
    void fetchGlobalStormsUpdatesCacheOnSuccess() {
        String fakeGeoJson = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\"}]}";
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(fakeGeoJson);

        service.fetchGlobalStorms();

        assertThat(service.getCachedSigmets()).isEqualTo(fakeGeoJson);
    }

    @Test
    void fetchGlobalStormsKeepsPreviousCacheOnHttpFailure() {
        // Seed a known good cache first
        String goodCache = "{\"type\":\"FeatureCollection\",\"features\":[]}";
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(goodCache);
        service.fetchGlobalStorms();

        // Now simulate a network error
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("timeout"));
        service.fetchGlobalStorms();

        // Cache should still hold the last good value
        assertThat(service.getCachedSigmets()).isEqualTo(goodCache);
    }

    @Test
    void fetchGlobalStormsKeepsDefaultCacheWhenFirstCallFails() {
        String defaultCache = service.getCachedSigmets();

        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("connection refused"));
        service.fetchGlobalStorms();

        assertThat(service.getCachedSigmets()).isEqualTo(defaultCache);
    }

    @Test
    void fetchGlobalStormsDoesNotCacheNullResponse() {
        String defaultCache = service.getCachedSigmets();
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(null);

        service.fetchGlobalStorms();

        // null response should not overwrite the cache
        assertThat(service.getCachedSigmets()).isEqualTo(defaultCache);
    }

    @Test
    void fetchGlobalStormsCallsNoaaUrl() {
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"type\":\"FeatureCollection\",\"features\":[]}");

        service.fetchGlobalStorms();

        Mockito.verify(mockRestTemplate).getForObject(
                org.mockito.ArgumentMatchers.contains("aviationweather.gov"),
                eq(String.class)
        );
    }
}
