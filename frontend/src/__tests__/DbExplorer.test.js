import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import axios from "axios";
import DbExplorer from "../views/DbExplorer.vue";

vi.mock("axios");

describe("DbExplorer.vue - CRUD & Tooling", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Default mock behavior to allow the component to mount without crashing
    axios.get.mockResolvedValue({ data: {} });
    axios.post.mockResolvedValue({ data: {} });
  });

  it("switches tabs and fetches corresponding data", async () => {
    const wrapper = mount(DbExplorer);
    await flushPromises(); // Wait for initial onMounted calls

    const tabs = wrapper.findAll(".tab-btn");
    // Click the third tab ('Airports')
    await tabs[2].trigger("click");

    expect(axios.get).toHaveBeenCalledWith("/api/db/airports");
  });

  it("shows an error message when a tool API fetch fails", async () => {
    /**
     * We use mockImplementation to differentiate between the initial load
     * and the specific button click we want to test.
     */
    axios.get.mockImplementation((url) => {
      // Let the initial status and table load succeed
      if (
        url.includes("/api/db/status") ||
        url.includes("/api/db/celebrities")
      ) {
        return Promise.resolve({ data: [] });
      }
      // Make the specific Fetch & Save tool fail
      if (url.includes("/api/db/fetch-save")) {
        return Promise.reject({
          response: { data: { error: "Invalid Ident" } },
        });
      }
      return Promise.resolve({ data: {} });
    });

    const wrapper = mount(DbExplorer);
    await flushPromises(); // Clear the "Loading data..." state

    // Find the second tool card (Fetch & Save AeroAPI)
    const fetchSection = wrapper.findAll(".tool-card")[1];
    await fetchSection.find("input").setValue("INVALID");
    await fetchSection.find("button").trigger("click");

    await flushPromises();

    // Verify the error is shown specifically inside that tool section
    expect(fetchSection.find(".state-msg.error").text()).toContain(
      "Invalid Ident",
    );
  });

  it("uses the computed property to hide 'errors' from the import summary", async () => {
    const wrapper = mount(DbExplorer);
    await flushPromises();

    // Define the mock result that includes an 'errors' key
    const mockResult = {
      flights: 5,
      aircraft: 1,
      errors: ["line 2 failed: malformed data"],
    };

    // Manually trigger the state change.
    // We assign directly and then wait for Vue's next tick.
    wrapper.vm.importResult = mockResult;
    await wrapper.vm.$nextTick();

    const resultGrid = wrapper.find(".result-grid");
    const resultItems = resultGrid.findAll(".result-item");

    // The computed property 'filteredImportResult' should strip 'errors'
    // so the grid should only show 2 items: 'flights' and 'aircraft'.
    expect(resultItems).toHaveLength(2);
    expect(resultGrid.text()).toContain("5 saved");
    expect(resultGrid.text()).not.toContain("line 2 failed");
  });
});
