import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import MapContainer from "../components/MapContainer.vue"

// Test suite for MapContainer props handling
describe("MapContainer.vue props", () => {

  // This test checks whether props are correctly passed into the component
  it("receives and stores flights prop correctly", () => {

    // Provide FULL mock data matching what the component expects
    const testFlights = [
      {
        id: 1,
        lat: 43.0731,
        lng: -89.4012,
        altitude: 30000,
        velocity: 450,
        callsign: "DAL123",
        origin: "MSN",
        destination: "ORD",
        liveInfo: null
      },
      {
        id: 2,
        lat: 40.7128,
        lng: -74.0060,
        altitude: 28000,
        velocity: 420,
        callsign: "UAL456",
        origin: "JFK",
        destination: "LAX",
        liveInfo: null
      }
    ]

    // Mount component with mock props
    const wrapper = mount(MapContainer, {
      props: {
        flights: testFlights,
        watchlist: []
      }
    })

    // Verify props are passed correctly
    expect(wrapper.props("flights")).toHaveLength(2)

    // Check one value to ensure correctness
    expect(wrapper.props("flights")[0].callsign).toBe("DAL123")
  })
})