<script setup>
import { onMounted, ref } from 'vue'
import axios from 'axios'

const query = ref('-type B738')
const loading = ref(false)
const error = ref(null)
const flights = ref([])
const raw = ref('')
const keyConfigured = ref(null)
const keyStatusError = ref(null)

async function fetchKeyStatus() {
  keyStatusError.value = null
  try {
    const res = await axios.get('/api/aeroapi/status')
    keyConfigured.value = !!res.data?.configured
  } catch (e) {
    keyConfigured.value = null
    keyStatusError.value = e.response
      ? `${e.response.status} — ${JSON.stringify(e.response.data)}`
      : e.message
  }
}

async function search() {
  loading.value = true
  error.value = null
  flights.value = []
  raw.value = ''
  try {
    const res = await axios.get('/api/flights/search', {
      params: { query: query.value, max_pages: 1 }
    })
    flights.value = (res.data.flights ?? []).slice(0, 5)
    raw.value = JSON.stringify(res.data, null, 2)
  } catch (e) {
    error.value = e.response
      ? `${e.response.status} — ${JSON.stringify(e.response.data)}`
      : e.message
  } finally {
    loading.value = false
  }
}

onMounted(fetchKeyStatus)
</script>

<template>
  <div class="page">
    <h1>Flight API Test</h1>
    <p class="hint">Uses <code>GET /api/flights/search</code> via the FlightAware AeroAPI.</p>

    <div class="status">
      <div class="status-row">
        <strong>AeroAPI key configured:</strong>
        <span v-if="keyConfigured === true" class="ok">Yes</span>
        <span v-else-if="keyConfigured === false" class="bad">No (set AEROAPI_KEY)</span>
        <span v-else class="muted">Unknown</span>
      </div>
      <div v-if="keyStatusError" class="error">Status check error: {{ keyStatusError }}</div>
      <button class="secondary" @click="fetchKeyStatus">Re-check key status</button>
    </div>

    <div class="search-bar">
      <input v-model="query" placeholder="FXML query, e.g. -type B738" />
      <button @click="search" :disabled="loading">
        {{ loading ? 'Searching...' : 'Search' }}
      </button>
    </div>

    <div v-if="error" class="error">Error: {{ error }}</div>

    <table v-if="flights.length" class="results">
      <thead>
        <tr>
          <th>Ident</th>
          <th>Origin</th>
          <th>Destination</th>
          <th>Status</th>
          <th>Aircraft</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="f in flights" :key="f.fa_flight_id ?? f.ident">
          <td>{{ f.ident }}</td>
          <td>{{ f.origin?.code ?? '—' }}</td>
          <td>{{ f.destination?.code ?? '—' }}</td>
          <td>{{ f.status }}</td>
          <td>{{ f.aircraft_type ?? '—' }}</td>
        </tr>
      </tbody>
    </table>

    <details v-if="raw" class="raw">
      <summary>Raw JSON response</summary>
      <pre>{{ raw }}</pre>
    </details>
  </div>
</template>

<style scoped>
.page {
  max-width: 900px;
  margin: 2rem auto;
  padding: 0 1rem;
  font-family: sans-serif;
  color: #e2e8f0;
}

h1 {
  margin-bottom: 0.25rem;
}

.hint {
  color: #94a3b8;
  margin-bottom: 1.5rem;
}

.hint code {
  background: #1e293b;
  padding: 2px 6px;
  border-radius: 4px;
}

.status {
  background: #0b1220;
  border: 1px solid #1f2a44;
  border-radius: 10px;
  padding: 0.9rem 1rem;
  margin: 1rem 0 1.25rem;
}

.status-row {
  display: flex;
  gap: 0.5rem;
  align-items: baseline;
  margin-bottom: 0.5rem;
}

.ok {
  color: #34d399;
}

.bad {
  color: #f87171;
}

.muted {
  color: #94a3b8;
}

.secondary {
  padding: 0.4rem 0.75rem;
  background: transparent;
  color: #cbd5e1;
  border: 1px solid #334155;
  border-radius: 8px;
  cursor: pointer;
}

.secondary:hover {
  border-color: #4a9eff;
}

.search-bar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}

.search-bar input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 6px;
  color: #e2e8f0;
  font-size: 0.95rem;
}

.search-bar button {
  padding: 0.5rem 1.25rem;
  background: #4a9eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.95rem;
}

.search-bar button:disabled {
  opacity: 0.5;
  cursor: default;
}

.error {
  color: #f87171;
  background: #1e293b;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  margin-bottom: 1rem;
}

.results {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1.5rem;
  font-size: 0.9rem;
}

.results th,
.results td {
  text-align: left;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid #334155;
}

.results th {
  color: #94a3b8;
  font-weight: 600;
}

.raw summary {
  cursor: pointer;
  color: #94a3b8;
  margin-bottom: 0.5rem;
}

.raw pre {
  background: #0f172a;
  padding: 1rem;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 0.8rem;
  color: #cbd5e1;
}
</style>
