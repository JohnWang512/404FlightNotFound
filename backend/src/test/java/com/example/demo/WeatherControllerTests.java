package com.example.demo;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("manual_backend")
@WebMvcTest(WeatherController.class)
class WeatherControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SigmetService sigmetService;

    // ── GET /api/weather/sigmets ───────────────────────────────────────────────

    @Test
    void getSigmetsReturns200WithCachedData() throws Exception {
        String fakeGeoJson = "{\"type\":\"FeatureCollection\",\"features\":[]}";
        when(sigmetService.getCachedSigmets()).thenReturn(fakeGeoJson);

        mockMvc.perform(get("/api/weather/sigmets"))
                .andExpect(status().isOk())
                .andExpect(content().json(fakeGeoJson));
    }

    @Test
    void getSigmetsReturnsFeatureCollectionType() throws Exception {
        when(sigmetService.getCachedSigmets())
                .thenReturn("{\"type\":\"FeatureCollection\",\"features\":[]}");

        mockMvc.perform(get("/api/weather/sigmets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }

    @Test
    void getSigmetsReturnsFeaturesArray() throws Exception {
        String withFeatures = "{\"type\":\"FeatureCollection\",\"features\":[" +
                "{\"type\":\"Feature\",\"properties\":{\"hazard\":\"TURB\"}}" +
                "]}";
        when(sigmetService.getCachedSigmets()).thenReturn(withFeatures);

        mockMvc.perform(get("/api/weather/sigmets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features[0].properties.hazard").value("TURB"));
    }

    @Test
    void getSigmetsReturnsEmptyFeaturesWhenNoneActive() throws Exception {
        when(sigmetService.getCachedSigmets())
                .thenReturn("{\"type\":\"FeatureCollection\",\"features\":[]}");

        mockMvc.perform(get("/api/weather/sigmets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features").isEmpty());
    }

    // ── GET /api/weather ──────────────────────────────────────────────────────

    @Test
    void getWeatherReturnsBadRequestWhenParamsMissing() throws Exception {
        mockMvc.perform(get("/api/weather"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWeatherReturnsBadRequestWhenOnlyLatProvided() throws Exception {
        mockMvc.perform(get("/api/weather?lat=41.9"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWeatherReturnsBadRequestWhenOnlyLngProvided() throws Exception {
        mockMvc.perform(get("/api/weather?lng=-87.6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWeatherAcceptsValidLatLng() throws Exception {
        // WeatherController calls Open-Meteo via RestTemplate internally.
        // In a unit test the real HTTP call will fail, which returns 500.
        // We just verify the controller accepts the params and doesn't 400.
        mockMvc.perform(get("/api/weather?lat=41.9&lng=-87.6"))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getStatus()
                        ).isNotEqualTo(400)
                );
    }
}
