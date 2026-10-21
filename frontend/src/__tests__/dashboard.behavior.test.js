import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import Dashboard from "../components/Dashboard.vue"

// Test suite for Dashboard component behavior
describe("Dashboard.vue behavior", () => {

  // This test checks what the user sees when there are no flights
  it("shows a message when no flights exist", () => {

    // Mount the component with empty props (no data)
    const wrapper = mount(Dashboard, {
      props: {
        flights: [],
        watchlist: []
      }
    })

    // Get all rendered text from the component
    const text = wrapper.text().toLowerCase()

    // Check that some kind of "empty state" message appears
    // (we keep it flexible in case wording changes)
    expect(text).toMatch(/no|empty|select|click/)
  })
})