import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import L from "leaflet";
import MapContainer from "../components/MapContainer.vue";

vi.mock("leaflet", () => {
  const markerInstance = {
    addTo: vi.fn().mockReturnThis(),
    bindPopup: vi.fn().mockReturnThis(),
    openPopup: vi.fn().mockReturnThis(),
    on: vi.fn().mockReturnThis(),
    setIcon: vi.fn().mockReturnThis(),
    setLatLng: vi.fn().mockReturnThis(),
    remove: vi.fn(),
  };
  const mapInstance = {
    setView: vi.fn().mockReturnThis(),
    flyTo: vi.fn().mockReturnThis(),
    invalidateSize: vi.fn().mockReturnThis(),
    removeLayer: vi.fn().mockReturnThis(),
    remove: vi.fn(),
  };
  return {
    default: {
      map: vi.fn(() => mapInstance),
      tileLayer: vi.fn(() => ({ addTo: vi.fn() })),
      marker: vi.fn(() => markerInstance),
      divIcon: vi.fn((opts) => opts),
      latLng: vi.fn((lat, lng) => [lat, lng]),
      control: {
        zoom: vi.fn(() => ({ addTo: vi.fn() })),
      },
    },
  };
});

const mockFlight = {
  callsign: "TEST101",
  lat: 40.0,
  lng: -70.0,
  altitude: 30000,
  velocity: 450,
  bearing: 90,
  origin: "JFK",
  destination: "LHR",
};

describe("MapContainer.vue - Advanced Integration", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("initializes the map and handles resize on mount", () => {
    mount(MapContainer, { props: { flights: [], watchlist: [] } });
    const mapInstance = vi.mocked(L.map)();
    expect(mapInstance.invalidateSize).toHaveBeenCalled();
  });

  it("clears old markers when the flights array is updated", async () => {
    const wrapper = mount(MapContainer, {
      props: { flights: [mockFlight], watchlist: [] },
    });
    expect(L.marker).toHaveBeenCalledTimes(1);

    // Trigger the watcher to clear markers
    await wrapper.setProps({ flights: [] });
    const mapInstance = vi.mocked(L.map)();
    expect(mapInstance.removeLayer).toHaveBeenCalled();
  });

  it("flies to coordinates and opens popup when a flight is selected", async () => {
    const wrapper = mount(MapContainer, {
      props: { flights: [mockFlight], watchlist: [] },
    });

    await wrapper.setProps({ selectedFlight: mockFlight });

    const mapInstance = vi.mocked(L.map)();

    expect(mapInstance.flyTo).toHaveBeenCalledWith([40.0, -70.0], 6, {
      duration: 0.8,
    });
  });
});
