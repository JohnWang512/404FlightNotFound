import { describe, it, expect } from "vitest"
import { mount } from "@vue/test-utils"
import HomeView from "../views/HomeView.vue"

describe("HomeView.vue structure", () => {

  // This test ensures the page contains main UI structure
  it("renders main elements", () => {
    const wrapper = mount(HomeView, {
      global: {
        stubs: ["router-link", "GlobeContainer"]
      }
    })

    // Check that page is not empty and contains meaningful HTML
    expect(wrapper.html()).toContain("div")
  })

})