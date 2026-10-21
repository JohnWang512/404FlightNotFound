import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import Dashboard from "../components/Dashboard.vue"

describe("Dashboard.vue empty state", () => {

  // This test checks that when the watchlist is empty,
  // the component displays the correct helper message
  it("shows empty watchlist message", () => {
    const wrapper = mount(Dashboard, {
      props: {
        flights: [],
        watchlist: []
      }
    })

    // Look for the hint text rendered when watchlist is empty
    expect(wrapper.text().toLowerCase()).toContain("watchlist")
  })

})