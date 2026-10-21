import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import Dashboard from "../components/Dashboard.vue"

describe("Dashboard.vue clear watchlist", () => {

  // This test checks that the button exists and can be clicked
  it("renders and triggers clear button click", async () => {

    const wrapper = mount(Dashboard, {
      props: {
        flights: [],
        watchlist: [{ id: 1 }, { id: 2 }]
      }
    })

    // Find button
    const button = wrapper.findAll("button").find(btn =>
      btn.text().toLowerCase().includes("clear")
    )

    expect(button).toBeTruthy()

    // Trigger click (no crash = success)
    await button.trigger("click")

    expect(wrapper.exists()).toBe(true)
  })

})