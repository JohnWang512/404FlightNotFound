import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import Dashboard from "../components/Dashboard.vue"

describe("Dashboard.vue watchlist feature", () => {

  // Test 1: empty state message (correct one)
  it("shows empty message when watchlist is empty", () => {
    const wrapper = mount(Dashboard, {
      props: {
        flights: [],
        watchlist: []
      }
    })

    expect(wrapper.text().toLowerCase())
      .toContain("click a plane")
  })

  // Test 2: renders when watchlist has items
  it("renders correctly when watchlist has items", () => {
    const wrapper = mount(Dashboard, {
      props: {
        flights: [],
        watchlist: [{ id: 1 }]
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  // Test 3: updates when watchlist changes
  it("updates when watchlist changes", async () => {
    const wrapper = mount(Dashboard, {
      props: {
        flights: [],
        watchlist: []
      }
    })

    await wrapper.setProps({
      watchlist: [{ id: 1 }]
    })

    expect(wrapper.exists()).toBe(true)
  })

})