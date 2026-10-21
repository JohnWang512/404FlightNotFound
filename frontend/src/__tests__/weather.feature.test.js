import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";

vi.mock("axios", () => ({
  default: { get: vi.fn() },
}));

import HomeView from "../views/HomeView.vue";
import Dashboard from "../components/Dashboard.vue";
import axios from "axios";

const mountOptions = {
  global: {
    stubs: [
      "router-link",
      "MapContainer",
      "GlobeContainer",
      "CelebrityTracker",
    ],
  },
};

const emptyFlightsResponse = { data: { flights: [] } };

// ─── Weather overlay display ───────────────────────────────────────────────

describe("Weather overlay", () => {
  beforeEach(() => vi.clearAllMocks());

  it("is hidden when no flight is selected", async () => {
    axios.get.mockResolvedValue(emptyFlightsResponse);
    const wrapper = mount(HomeView, mountOptions);
    await flushPromises();
    expect(wrapper.find(".weather-overlay").exists()).toBe(false);
  });

  it("shows condition, temperature and wind speed after weather is fetched", async () => {
    axios.get.mockImplementation((url) => {
      if (url.includes("/api/weather")) {
        return Promise.resolve({
          data: {
            current: {
              temperature_2m: 72,
              wind_speed_10m: 15,
              wind_gusts_10m: 20,
              wind_direction_10m: 180,
              cloud_cover: 30,
              weather_code: 0,
            },
          },
        });
      }
      return Promise.resolve(emptyFlightsResponse);
    });

    const wrapper = mount(HomeView, mountOptions);
    await flushPromises();

    // Simulate a flight selection to trigger weather fetch
    const vm = wrapper.vm;
    vm.weatherData = {
      lat: 41.9,
      lng: -87.6,
      temp: 72,
      windSpeed: 15,
      condition: "Clear Skies ☀️",
      isStormy: false,
    };
    await wrapper.vm.$nextTick();

    const overlay = wrapper.find(".weather-overlay");
    expect(overlay.exists()).toBe(true);
    expect(overlay.text()).toContain("72");
    expect(overlay.text()).toContain("15");
  });

  it("disappears when the close button is clicked", async () => {
    axios.get.mockResolvedValue(emptyFlightsResponse);
    const wrapper = mount(HomeView, mountOptions);
    await flushPromises();

    wrapper.vm.weatherData = {
      lat: 0,
      lng: 0,
      temp: 68,
      windSpeed: 10,
      condition: "Clear Skies ☀️",
      isStormy: false,
    };
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".weather-overlay").exists()).toBe(true);
    await wrapper.find(".clear-weather-btn").trigger("click");
    expect(wrapper.find(".weather-overlay").exists()).toBe(false);
  });
});

// ─── SIGMET / RADAR toggle buttons ────────────────────────────────────────

describe("Weather layer toggle buttons in Dashboard", () => {
  const dashProps = { flights: [], watchlist: [], selectedFlight: null };

  it("emits change-weather-layer with 'none' when OFF is clicked", async () => {
    const wrapper = mount(Dashboard, { props: dashProps });
    const buttons = wrapper.findAll("button");
    const offBtn = buttons.find((b) => b.text().includes("OFF"));
    await offBtn.trigger("click");
    expect(wrapper.emitted("change-weather-layer")).toBeTruthy();
    expect(wrapper.emitted("change-weather-layer")[0]).toEqual(["none"]);
  });

  it("emits change-weather-layer with 'sigmet' when HAZARDS is clicked", async () => {
    const wrapper = mount(Dashboard, { props: dashProps });
    const buttons = wrapper.findAll("button");
    const hazBtn = buttons.find((b) => b.text().includes("HAZARDS"));
    await hazBtn.trigger("click");
    expect(wrapper.emitted("change-weather-layer")[0]).toEqual(["sigmet"]);
  });

  it("emits change-weather-layer with 'radar' when RADAR is clicked", async () => {
    const wrapper = mount(Dashboard, { props: dashProps });
    const buttons = wrapper.findAll("button");
    const radarBtn = buttons.find((b) => b.text().includes("RADAR"));
    await radarBtn.trigger("click");
    expect(wrapper.emitted("change-weather-layer")[0]).toEqual(["radar"]);
  });
});

// ─── SIGMET legend ────────────────────────────────────────────────────────

describe("SIGMET legend", () => {
  beforeEach(() => vi.clearAllMocks());

  it("is hidden when activeWeatherLayer is 'none'", async () => {
    axios.get.mockResolvedValue(emptyFlightsResponse);
    const wrapper = mount(HomeView, mountOptions);
    await flushPromises();
    expect(wrapper.find(".sigmet-legend").exists()).toBe(false);
  });

  it("appears when activeWeatherLayer is set to sigmet", async () => {
    axios.get.mockResolvedValue(emptyFlightsResponse);
    const wrapper = mount(HomeView, mountOptions);
    await flushPromises();

    wrapper.vm.activeWeatherLayer = "sigmet";
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".sigmet-legend").exists()).toBe(true);
  });

  it("lists the four hazard types in the legend", async () => {
    axios.get.mockResolvedValue(emptyFlightsResponse);
    const wrapper = mount(HomeView, mountOptions);
    await flushPromises();

    wrapper.vm.activeWeatherLayer = "sigmet";
    await wrapper.vm.$nextTick();

    const legend = wrapper.find(".sigmet-legend").text();
    expect(legend).toContain("Thunderstorms");
    expect(legend).toContain("Icing");
    expect(legend).toContain("Turbulence");
    expect(legend).toContain("Volcanic");
  });
});

// ─── onChangeWeatherLayer logic ───────────────────────────────────────────

describe("onChangeWeatherLayer", () => {
  beforeEach(() => vi.clearAllMocks());

  it("fetches sigmets when layer switches to sigmet", async () => {
    const sigmetPayload = { type: "FeatureCollection", features: [] };
    axios.get.mockImplementation((url) => {
      if (url.includes("sigmets"))
        return Promise.resolve({ data: sigmetPayload });
      return Promise.resolve(emptyFlightsResponse);
    });

    const wrapper = mount(HomeView, mountOptions);
    await flushPromises();

    wrapper.vm.activeWeatherLayer = "sigmet";
    await wrapper.vm.onChangeWeatherLayer("sigmet");
    await flushPromises();

    const calls = axios.get.mock.calls.map((c) => c[0]);
    expect(calls.some((u) => u.includes("sigmets"))).toBe(true);
    expect(wrapper.vm.sigmetData).toEqual(sigmetPayload);
  });

  it("clears sigmetData when layer switches to radar", async () => {
    axios.get.mockResolvedValue(emptyFlightsResponse);
    const wrapper = mount(HomeView, mountOptions);
    await flushPromises();

    wrapper.vm.sigmetData = { type: "FeatureCollection", features: [{}] };
    await wrapper.vm.onChangeWeatherLayer("radar");

    expect(wrapper.vm.sigmetData).toBeNull();
  });

  it("clears sigmetData when layer is turned off", async () => {
    axios.get.mockResolvedValue(emptyFlightsResponse);
    const wrapper = mount(HomeView, mountOptions);
    await flushPromises();

    wrapper.vm.sigmetData = { type: "FeatureCollection", features: [{}] };
    await wrapper.vm.onChangeWeatherLayer("none");

    expect(wrapper.vm.sigmetData).toBeNull();
  });
});
