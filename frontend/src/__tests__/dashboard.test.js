import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import Dashboard from "../components/Dashboard.vue";

const mockFlight = {
  callsign: "UAL123",
  origin: "ORD",
  destination: "LAX",
  altitude: 35000.55,
  velocity: 450,
  lat: 41.9012,
  lng: -87.6034,
  liveInfo: { status: "En Route", aircraft_type: "B738" },
};

describe("Dashboard.vue - Advanced Logic", () => {
  it("formats coordinates and altitude with correct precision", () => {
    const wrapper = mount(Dashboard, {
      props: { flights: [], watchlist: [], selectedFlight: mockFlight },
    });
    // Check if lat/lng are fixed to 2 decimal places as per template
    expect(wrapper.find(".stat-grid").text()).toContain("41.90");
    expect(wrapper.find(".stat-grid").text()).toContain("-87.60");
    // Check if altitude is formatted with commas (toLocaleString)
    expect(wrapper.find(".stat-grid").text()).toContain("35,000");
  });

  it("dynamically renders the liveInfo JSON panel only when present", async () => {
    const wrapper = mount(Dashboard, {
      props: {
        flights: [],
        watchlist: [],
        selectedFlight: { ...mockFlight, liveInfo: null },
      },
    });
    expect(wrapper.find(".live-info-panel").exists()).toBe(false);

    await wrapper.setProps({ selectedFlight: mockFlight });
    expect(wrapper.find(".live-info-panel").exists()).toBe(true);
    expect(wrapper.find(".live-info-json").text()).toContain("B738");
  });

  it("clears search input and resets list via the Clear button", async () => {
    const wrapper = mount(Dashboard, {
      props: { flights: [], watchlist: [mockFlight] },
    });
    const input = wrapper.find(".search-input");
    await input.setValue("NonExistent");
    expect(wrapper.find(".search-no-results").exists()).toBe(true);

    await wrapper.find(".search-clear").trigger("click");
    expect(input.element.value).toBe("");
    expect(wrapper.findAll(".route-row")).toHaveLength(1);
  });

  it("maintains 'selected' class only on the active flight", () => {
    const secondFlight = { ...mockFlight, callsign: "DAL456" };
    const wrapper = mount(Dashboard, {
      props: {
        flights: [],
        watchlist: [mockFlight, secondFlight],
        selectedFlight: mockFlight,
      },
    });
    const rows = wrapper.findAll(".route-row");
    expect(rows[0].classes()).toContain("route-row--selected");
    expect(rows[1].classes()).not.toContain("route-row--selected");
  });
});
