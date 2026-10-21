import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import MapContainer from "../components/MapContainer.vue"

describe("MapContainer.vue valid data", () => {

  // This test ensures that the component does NOT crash
  // when valid flight data is passed in
  it("renders safely with valid flight data", () => {

    const mockFlights = [
      {
        lat: 40,
        lng: -74,
        callsign: "AA101",
        origin: "JFK",
        destination: "LAX",
        altitude: 30000,
        velocity: 500,
        liveInfo: {}
      }
    ]

    const wrapper = mount(MapContainer, {
      props: {
        flights: mockFlights,
        watchlist: []
      }
    })

    // If no crash happens, component exists
    expect(wrapper.exists()).toBe(true)
  })

})