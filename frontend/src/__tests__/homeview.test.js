import { describe, it, expect, vi, beforeEach } from "vitest"
import { mount, flushPromises } from "@vue/test-utils"

vi.mock('axios', () => ({
  default: { get: vi.fn() }
}))

// WatchlistView uses useRouter for the back button
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush })
}))

// ---------------------------------------------------------------------------
// localStorage mock
// The test runner overrides window.localStorage with an incomplete stub
// (via --localstorage-file). Replace it with a simple in-memory Map so
// getItem / setItem / removeItem all work correctly.
// ---------------------------------------------------------------------------
const _store = new Map()
const localStorageMock = {
  getItem:    (key) => _store.get(key) ?? null,
  setItem:    (key, val) => _store.set(key, String(val)),
  removeItem: (key) => _store.delete(key),
  get length() { return _store.size },
  key: (i) => Array.from(_store.keys())[i] ?? null,
}
vi.stubGlobal('localStorage', localStorageMock)

import HomeView from "../views/HomeView.vue"
import WatchlistView from "../views/WatchlistView.vue"
import axios from 'axios'

// ============================================================
// HomeView
// ============================================================

const mockApiResponse = {
  data: {
    flights: [
      {
        ident: 'UAL123',
        origin: { code: 'ORD' },
        destination: { code: 'LAX' },
        last_position: { latitude: 41.9, longitude: -87.6, altitude: 350, groundspeed: 450 }
      }
    ]
  }
}

const mountOptions = {
  global: { stubs: ['router-link', 'MapContainer', 'Dashboard', 'GlobeContainer'] }
}

describe("HomeView.vue", () => {
  beforeEach(() => {
    _store.clear()
    vi.clearAllMocks()
  })

  it("renders homepage", async () => {
    axios.get.mockResolvedValue(mockApiResponse)
    const wrapper = mount(HomeView, mountOptions)
    expect(wrapper.exists()).toBe(true)
  })

  it("shows loading overlay on initial render", () => {
    axios.get.mockReturnValue(new Promise(() => {}))
    const wrapper = mount(HomeView, mountOptions)
    expect(wrapper.find('.overlay').exists()).toBe(true)
    expect(wrapper.text()).toContain('Loading live flights')
  })

  it("hides loading overlay after successful fetch", async () => {
    axios.get.mockResolvedValue(mockApiResponse)
    const wrapper = mount(HomeView, mountOptions)
    await flushPromises()
    expect(wrapper.find('.overlay').exists()).toBe(false)
  })
})

// ============================================================
// WatchlistView
// ============================================================

const WATCHLIST_KEY = "ft_watchlist"

const flightA = {
  callsign: "UAL123",
  tailNumber: "N12345",
  origin: "ORD",
  destination: "LAX",
  altitude: 35000,
  velocity: 450,
  lat: 41.9,
  lng: -87.6,
  bearing: 270,
  estimatedCo2Kg: 1200,
  category: null,
  celebrity: null,
}

const flightB = {
  callsign: "DAL456",
  tailNumber: "N67890",
  origin: "ATL",
  destination: "JFK",
  altitude: 28000,
  velocity: 420,
  lat: 33.6,
  lng: -84.4,
  bearing: 45,
  estimatedCo2Kg: null,
  category: "celebrity",
  celebrity: "Taylor Swift",
}

function seedWatchlist(flights = []) {
  _store.set(WATCHLIST_KEY, JSON.stringify(flights))
}

function mountWatchlist(options = {}) {
  return mount(WatchlistView, {
    global: { stubs: { GlobeContainer: true }, ...options.global },
    ...options,
  })
}

describe("WatchlistView.vue — mount", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks(); axios.get.mockResolvedValue({ data: [] }) })

  it("mounts without crashing", () => {
    seedWatchlist([])
    expect(mountWatchlist().exists()).toBe(true)
  })

  it("reads watchlist from localStorage on mount", () => {
    seedWatchlist([flightA, flightB])
    const w = mountWatchlist()
    expect(w.text()).toContain("UAL123")
    expect(w.text()).toContain("DAL456")
  })
})

describe("WatchlistView.vue — empty state", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks(); axios.get.mockResolvedValue({ data: [] }) })

  it("shows empty overlay when watchlist is empty", () => {
    seedWatchlist([])
    expect(mountWatchlist().find(".wl-overlay").exists()).toBe(true)
  })

  it("shows empty hint text in sidebar when watchlist is empty", () => {
    seedWatchlist([])
    expect(mountWatchlist().find(".wl-empty").exists()).toBe(true)
  })

  it("does not show flight rows when watchlist is empty", () => {
    seedWatchlist([])
    expect(mountWatchlist().findAll(".wl-row")).toHaveLength(0)
  })

  it("hides empty overlay when watchlist has items", () => {
    seedWatchlist([flightA])
    expect(mountWatchlist().find(".wl-overlay").exists()).toBe(false)
  })
})

describe("WatchlistView.vue — sidebar list", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks(); axios.get.mockResolvedValue({ data: [] }) })

  it("renders one row per watchlist entry", () => {
    seedWatchlist([flightA, flightB])
    expect(mountWatchlist().findAll(".wl-row")).toHaveLength(2)
  })

  it("displays callsign, origin, and destination in each row", () => {
    seedWatchlist([flightA])
    const row = mountWatchlist().find(".wl-row")
    expect(row.text()).toContain("UAL123")
    expect(row.text()).toContain("ORD")
    expect(row.text()).toContain("LAX")
  })

  it("shows celebrity name when present", () => {
    seedWatchlist([flightB])
    expect(mountWatchlist().find(".wl-celebrity").text()).toContain("Taylor Swift")
  })

  it("does not render celebrity element when flight has none", () => {
    seedWatchlist([flightA])
    expect(mountWatchlist().find(".wl-celebrity").exists()).toBe(false)
  })

  it("displays watchlist count badge", () => {
    seedWatchlist([flightA, flightB])
    expect(mountWatchlist().find(".wl-count").text()).toBe("2")
  })
})

describe("WatchlistView.vue — search filter", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks(); axios.get.mockResolvedValue({ data: [] }) })

  it("shows all rows when search is empty", () => {
    seedWatchlist([flightA, flightB])
    expect(mountWatchlist().findAll(".wl-row")).toHaveLength(2)
  })

  it("filters rows by callsign", async () => {
    seedWatchlist([flightA, flightB])
    const w = mountWatchlist()
    await w.find(".wl-search").setValue("UAL")
    expect(w.findAll(".wl-row")).toHaveLength(1)
    expect(w.find(".wl-row").text()).toContain("UAL123")
  })

  it("filters rows by origin airport code", async () => {
    seedWatchlist([flightA, flightB])
    const w = mountWatchlist()
    await w.find(".wl-search").setValue("ATL")
    expect(w.findAll(".wl-row")).toHaveLength(1)
    expect(w.find(".wl-row").text()).toContain("DAL456")
  })

  it("filters rows by destination airport code", async () => {
    seedWatchlist([flightA, flightB])
    const w = mountWatchlist()
    await w.find(".wl-search").setValue("jfk")
    expect(w.findAll(".wl-row")).toHaveLength(1)
    expect(w.find(".wl-row").text()).toContain("DAL456")
  })

  it("shows no-results message when filter matches nothing", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-search").setValue("XXXXXX")
    expect(w.findAll(".wl-row")).toHaveLength(0)
    expect(w.find(".wl-no-results").exists()).toBe(true)
  })

  it("search is case-insensitive", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-search").setValue("ual123")
    expect(w.findAll(".wl-row")).toHaveLength(1)
  })
})

describe("WatchlistView.vue — remove from watchlist", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks(); axios.get.mockResolvedValue({ data: [] }) })

  it("removes a flight when the remove button is clicked", async () => {
    seedWatchlist([flightA, flightB])
    const w = mountWatchlist()
    await w.findAll(".wl-remove-btn")[0].trigger("click")
    expect(w.findAll(".wl-row")).toHaveLength(1)
  })

  it("persists removal to localStorage", async () => {
    seedWatchlist([flightA, flightB])
    const w = mountWatchlist()
    await w.findAll(".wl-remove-btn")[0].trigger("click")
    expect(JSON.parse(_store.get(WATCHLIST_KEY))).toHaveLength(1)
  })

  it("clears selected flight when it is removed", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    expect(w.find(".wl-flight-panel").exists()).toBe(true)
    await w.find(".wl-remove-btn").trigger("click")
    expect(w.find(".wl-flight-panel").exists()).toBe(false)
  })
})

describe("WatchlistView.vue — flight detail panel", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks(); axios.get.mockResolvedValue({ data: [] }) })

  it("shows detail panel when a row is clicked", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    expect(w.find(".wl-flight-panel").exists()).toBe(true)
  })

  it("displays callsign in the detail panel", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    expect(w.find(".fi-callsign").text()).toBe("UAL123")
  })

  it("displays route in the detail panel", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    expect(w.find(".fi-route").text()).toContain("ORD")
    expect(w.find(".fi-route").text()).toContain("LAX")
  })

  it("shows CO2 estimate when present", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    expect(w.find(".fi-co2").text()).toContain("1,200")
  })

  it("hides CO2 element when estimate is null", async () => {
    seedWatchlist([{ ...flightA, estimatedCo2Kg: null }])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    expect(w.find(".fi-co2").exists()).toBe(false)
  })

  it("shows celebrity name in detail panel when present", async () => {
    seedWatchlist([flightB])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    expect(w.find(".fi-celebrity-name").text()).toContain("Taylor Swift")
  })

  it("closes panel when close button is clicked", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    await w.find(".fi-close").trigger("click")
    expect(w.find(".wl-flight-panel").exists()).toBe(false)
  })

  it("deselects flight when the same row is clicked twice", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    await w.find(".wl-row").trigger("click")
    expect(w.find(".wl-flight-panel").exists()).toBe(false)
  })

  it("applies selected class only to the active row", async () => {
    seedWatchlist([flightA, flightB])
    const w = mountWatchlist()
    const rows = w.findAll(".wl-row")
    await rows[0].trigger("click")
    expect(rows[0].classes()).toContain("wl-row--selected")
    expect(rows[1].classes()).not.toContain("wl-row--selected")
  })
})

describe("WatchlistView.vue — watchlist toggle in detail panel", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks(); axios.get.mockResolvedValue({ data: [] }) })

  it("shows 'Remove' label when flight is in watchlist", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    expect(w.find(".fi-watchlist-btn").text()).toContain("Remove")
  })

  it("removes flight when toggle button is clicked", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-row").trigger("click")
    await w.find(".fi-watchlist-btn").trigger("click")
    expect(w.findAll(".wl-row")).toHaveLength(0)
  })
})

describe("WatchlistView.vue — live markers", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks() })

  it("shows live dot when aircraft has a current position", async () => {
    seedWatchlist([flightA])
    axios.get.mockResolvedValue({
      data: [{
        lastCallsign: "UAL123", tailNumber: "N12345",
        lastLat: 41.9, lastLon: -87.6,
        lastAltitude: 350, lastSpeed: 450, lastHeading: 270,
        estimatedCo2Kg: 1200, category: null,
      }],
    })
    const w = mountWatchlist()
    await flushPromises()
    expect(w.find(".wl-live-dot").exists()).toBe(true)
  })

  it("does not show live dot when no live position is available", async () => {
    seedWatchlist([flightA])
    axios.get.mockResolvedValue({ data: [] })
    const w = mountWatchlist()
    await flushPromises()
    expect(w.find(".wl-live-dot").exists()).toBe(false)
  })
})

describe("WatchlistView.vue — sidebar toggle", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks(); axios.get.mockResolvedValue({ data: [] }) })

  it("sidebar is visible by default", () => {
    seedWatchlist([flightA])
    expect(mountWatchlist().find(".wl-sidebar-wrapper").classes()).not.toContain("collapsed")
  })

  it("collapses sidebar when toggle is clicked", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-sidebar-toggle").trigger("click")
    expect(w.find(".wl-sidebar-wrapper").classes()).toContain("collapsed")
  })

  it("expands sidebar when toggle is clicked again", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-sidebar-toggle").trigger("click")
    await w.find(".wl-sidebar-toggle").trigger("click")
    expect(w.find(".wl-sidebar-wrapper").classes()).not.toContain("collapsed")
  })
})

describe("WatchlistView.vue — navigation", () => {
  beforeEach(() => { _store.clear(); vi.clearAllMocks(); axios.get.mockResolvedValue({ data: [] }) })

  it("calls router.push('/') when the back button is clicked", async () => {
    seedWatchlist([flightA])
    const w = mountWatchlist()
    await w.find(".wl-back-btn").trigger("click")
    expect(mockPush).toHaveBeenCalledWith("/")
  })
})
