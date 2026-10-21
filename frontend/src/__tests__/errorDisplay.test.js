import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import Dashboard from '../components/Dashboard.vue'

// Mock axios to simulate API failure
vi.mock('axios', () => ({
  default: {
    get: vi.fn(() => Promise.reject(new Error('API Error')))
  }
}))

describe('Dashboard Error Handling', () => {
  it('does not crash on API failure', () => {
    const wrapper = mount(Dashboard, {
      // ✅ FIX: provide required props
      props: {
        flights: [],
        watchlist: []
      }
    })

    // Ensure component still renders
    expect(wrapper.exists()).toBe(true)
  })
})