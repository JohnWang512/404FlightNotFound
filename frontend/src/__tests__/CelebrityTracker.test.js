import { describe, it, expect, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import CelebrityTracker from '../components/CelebrityTracker.vue'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const mockCelebrity = {
  id: 1,
  name: 'Taylor Swift',
  tailNumber: 'N621MM',
  model: 'Dassault Falcon',
  airborne: false,
  industry: 'Music',
  lastSeen: null,
  lastLat: null,
  lastLon: null
}

/**
 * Creates a fetch mock that serves both the /celebrities/live endpoint (on mount)
 * and the /celebrities/:id/flights endpoint (on celebrity click).
 *
 * @param {Array}           celebrities  - Response for /celebrities/live
 * @param {Object|Function} flightsBody  - Response for /celebrities/:id/flights.
 *                                         Pass a function (url) => body for URL-aware responses.
 */
function makeFetch (celebrities, flightsBody) {
  return vi.fn().mockImplementation((url) => {
    if (url.includes('/celebrities/live')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve(celebrities) })
    }
    if (url.includes('/flights')) {
      const body = typeof flightsBody === 'function'
        ? flightsBody(url)
        : (flightsBody ?? { flights: [] })
      return Promise.resolve({ ok: true, json: () => Promise.resolve(body) })
    }
    return Promise.reject(new Error(`Unmocked fetch URL: ${url}`))
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
})

// ---------------------------------------------------------------------------
// deduplicatedFlights — filtering of degenerate rows
// ---------------------------------------------------------------------------

describe('CelebrityTracker.vue – deduplicatedFlights filtering', () => {
  it('hides flight card when a row has null origin, destination, and departureTime', async () => {
    vi.stubGlobal('fetch', makeFetch([mockCelebrity], {
      flights: [{ origin: null, destination: null, departureTime: null, tailNumber: 'N99999' }]
    }))

    const wrapper = mount(CelebrityTracker)
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    expect(wrapper.findAll('.flight-card')).toHaveLength(0)
  })

  it('keeps flight card when only destination is set (origin is null)', async () => {
    vi.stubGlobal('fetch', makeFetch([mockCelebrity], {
      flights: [{ origin: null, destination: 'KORD', departureTime: '2026-01-01T10:00:00', tailNumber: 'N99999' }]
    }))

    const wrapper = mount(CelebrityTracker)
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    expect(wrapper.findAll('.flight-card')).toHaveLength(1)
  })

  it('keeps flight card when only departureTime is set (origin and destination are null)', async () => {
    vi.stubGlobal('fetch', makeFetch([mockCelebrity], {
      flights: [{ origin: null, destination: null, departureTime: '2026-02-01T08:00:00', tailNumber: 'N99999' }]
    }))

    const wrapper = mount(CelebrityTracker)
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    expect(wrapper.findAll('.flight-card')).toHaveLength(1)
  })

  it('deduplicates flights sharing the same origin, destination, and departure date', async () => {
    vi.stubGlobal('fetch', makeFetch([mockCelebrity], {
      flights: [
        { origin: 'KMSN', destination: 'KORD', departureTime: '2026-01-01T10:00:00', tailNumber: 'N99999' },
        { origin: 'KMSN', destination: 'KORD', departureTime: '2026-01-01T23:59:00', tailNumber: 'N99999' }
      ]
    }))

    const wrapper = mount(CelebrityTracker)
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    // Same date (2026-01-01) → dedup collapses two rows into one
    expect(wrapper.findAll('.flight-card')).toHaveLength(1)
  })
})

// ---------------------------------------------------------------------------
// Null-safe airport code display  (flight.origin || '—')
// ---------------------------------------------------------------------------

describe('CelebrityTracker.vue – null-safe airport code display', () => {
  it('renders "—" for origin when flight.origin is null', async () => {
    vi.stubGlobal('fetch', makeFetch([mockCelebrity], {
      flights: [{ origin: null, destination: 'KLAX', departureTime: '2026-01-01T12:00:00', tailNumber: 'N99999' }]
    }))

    const wrapper = mount(CelebrityTracker)
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    const codes = wrapper.findAll('.airport-code')
    expect(codes[0].text()).toBe('—')    // origin column
    expect(codes[1].text()).toBe('KLAX') // destination column
  })

  it('renders "—" for destination when flight.destination is null', async () => {
    vi.stubGlobal('fetch', makeFetch([mockCelebrity], {
      flights: [{ origin: 'KMSN', destination: null, departureTime: '2026-01-01T12:00:00', tailNumber: 'N99999' }]
    }))

    const wrapper = mount(CelebrityTracker)
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    const codes = wrapper.findAll('.airport-code')
    expect(codes[0].text()).toBe('KMSN') // origin column
    expect(codes[1].text()).toBe('—')    // destination column
  })
})

// ---------------------------------------------------------------------------
// Cache-first fetch behavior
// ---------------------------------------------------------------------------

describe('CelebrityTracker.vue – cache-first fetch behavior', () => {
  it('serves flight history from cache and skips the live flights fetch', async () => {
    const fetchMock = makeFetch([mockCelebrity]) // no flightsBody \u2014 flights URL must not be called
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(CelebrityTracker, {
      global: {
        provide: {
          currentUser: ref({ uId: 99, uname: 'testuser' }),
          userFlightHistory: ref({
            1: { flights: [{ origin: 'KMSN', destination: 'KORD', departureTime: '2026-01-01T10:00:00', tailNumber: 'N-CACHED' }] }
          })
        }
      }
    })
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    const flightCalls = fetchMock.mock.calls.filter(([url]) => url.includes('/flights'))
    expect(flightCalls).toHaveLength(0)
    expect(wrapper.findAll('.flight-card')).toHaveLength(1)
  })

  it('shows the cached badge when flight history is served from pre-loaded cache', async () => {
    vi.stubGlobal('fetch', makeFetch([mockCelebrity]))

    const wrapper = mount(CelebrityTracker, {
      global: {
        provide: {
          currentUser: ref(null),
          userFlightHistory: ref({
            1: { flights: [{ origin: 'KMSN', destination: 'KORD', departureTime: '2026-01-01T10:00:00', tailNumber: 'N-CACHED' }] }
          })
        }
      }
    })
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    expect(wrapper.find('.cached-badge').exists()).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// userId in fetch URL
// ---------------------------------------------------------------------------

describe('CelebrityTracker.vue – userId in fetch URL', () => {
  it('appends ?userId= to the flights URL when a user is logged in', async () => {
    const fetchMock = makeFetch([mockCelebrity], { flights: [] })
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(CelebrityTracker, {
      global: {
        provide: {
          currentUser: ref({ uId: 42, uname: 'testuser' }),
          userFlightHistory: ref({})
        }
      }
    })
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    const flightCalls = fetchMock.mock.calls.filter(([url]) => url.includes('/flights'))
    expect(flightCalls).toHaveLength(1)
    expect(flightCalls[0][0]).toContain('?userId=42')
  })

  it('omits userId from the URL when no user is logged in', async () => {
    const fetchMock = makeFetch([mockCelebrity], { flights: [] })
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(CelebrityTracker, {
      global: {
        provide: {
          currentUser: ref(null),
          userFlightHistory: ref({})
        }
      }
    })
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    const flightCalls = fetchMock.mock.calls.filter(([url]) => url.includes('/flights'))
    expect(flightCalls).toHaveLength(1)
    expect(flightCalls[0][0]).not.toContain('userId')
  })

  it('saves returned flights into userFlightHistory after a successful live fetch', async () => {
    vi.stubGlobal('fetch', makeFetch([mockCelebrity], {
      flights: [{ origin: 'KORD', destination: 'KMSN', departureTime: '2026-01-01T10:00:00', tailNumber: 'N-LIVE' }]
    }))

    const userFlightHistory = ref({})
    const wrapper = mount(CelebrityTracker, {
      global: {
        provide: {
          currentUser: ref({ uId: 7, uname: 'tester' }),
          userFlightHistory
        }
      }
    })
    await flushPromises()
    await wrapper.find('.celeb-row').trigger('click')
    await flushPromises()

    expect(userFlightHistory.value[1]).toBeDefined()
    expect(userFlightHistory.value[1].flights).toHaveLength(1)
    expect(userFlightHistory.value[1].flights[0].origin).toBe('KORD')
  })
})
