import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import Dashboard from '../components/Dashboard.vue'

// Mock axios to prevent real API calls
vi.mock('axios', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: {} }))
  }
}))

describe('Dashboard Button State', () => {
  it('renders dashboard component', () => {
    const wrapper = mount(Dashboard, {
      // ✅ FIX: provide required props
      props: {
        flights: [],
        watchlist: []
      }
    })

    // Ensure component renders without crashing
    expect(wrapper.exists()).toBe(true)
  })
})