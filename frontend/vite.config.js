import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    proxy: {
      // Proxy only paths that start with /api/ (not /api-explorer).
      // This avoids accidentally forwarding frontend routes to the backend.
      '/api/': {
        target: process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },

  test: {
    environment: 'jsdom',
    pool: 'threads',
    singleThread: true,
    setupFiles: ['./src/__tests__/setup.js'],
  }
})
