import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import HomeView from "../views/HomeView.vue"

describe("HomeView.vue content", () => {

  // This test checks that the homepage renders meaningful content
  // and is not blank
  it("renders non-empty content", () => {
    const wrapper = mount(HomeView, {
      global: {
        stubs: ["router-link", "GlobeContainer"]
      }
    })

    // Ensure page has visible text content
    expect(wrapper.text().length).toBeGreaterThan(0)
  })

})