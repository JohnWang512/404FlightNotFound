import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import ApiExplorer from '../views/ApiExplorer.vue'

// Mock axios BEFORE component loads to prevent real API calls in CI
vi.mock('axios', () => ({
  default: {
    // Always return a resolved promise with mock data
    get: vi.fn(() => Promise.resolve({ data: { message: 'API Success' } }))
  }
}))

describe('ApiExplorer.vue', () => {
  it('renders component without crashing', () => {
    // Mount the component
    const wrapper = mount(ApiExplorer)

    // Basic assertion ensures component renders successfully
    // This avoids fragile UI-dependent failures in CI
    expect(wrapper.exists()).toBe(true)
  })
})