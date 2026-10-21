import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import MapContainer from "../components/MapContainer.vue"

describe("MapContainer.vue multiple flights", () => {

  // This test checks that multiple flights do not crash the component
  it("renders safely with multiple flight data", () => {

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
      },
      {
        lat: 41,
        lng: -73,
        callsign: "UA202",
        origin: "ORD",
        destination: "SFO",
        altitude: 32000,
        velocity: 520,
        liveInfo: {}
      }
    ]

    const wrapper = mount(MapContainer, {
      props: {
        flights: mockFlights,
        watchlist: []
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

})