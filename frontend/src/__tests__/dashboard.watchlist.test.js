import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import Dashboard from "../components/Dashboard.vue"

describe("Dashboard.vue watchlist behavior", () => {

  // This test checks that when watchlist has items,
  // the empty message should NOT be shown
  it("does not show empty message when watchlist has items", () => {
    const wrapper = mount(Dashboard, {
      props: {
        flights: [],
        watchlist: [{ id: 1 }]
      }
    })

    // The empty hint text should not appear
    expect(wrapper.text().toLowerCase()).not.toContain("click a plane")
  })

})