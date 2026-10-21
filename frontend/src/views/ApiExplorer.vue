<script setup>
import { computed, ref, watch } from 'vue'
import axios from 'axios'

const method = ref('GET')
const category = ref('flights')
const customPath = ref('')
const query = ref('')

const loading = ref(false)
const error = ref(null)
const responseText = ref('')

const categories = [
  { value: 'flights', label: 'Flights' },
  { value: 'airports', label: 'Airports' },
  { value: 'operators', label: 'Operators' }
]

const endpointTemplates = {
  flights: [
    { label: 'GET /api/flights/{id}', template: 'flights/{id}' },
    { label: 'GET /api/flights/{id}/map', template: 'flights/{id}/map' },
    { label: 'GET /api/flights/{id}/position', template: 'flights/{id}/position' },
    { label: 'GET /api/flights/{id}/route', template: 'flights/{id}/route' },
    { label: 'GET /api/flights/{id}/track', template: 'flights/{id}/track' },
    { label: 'GET /api/flights/{ident}', template: 'flights/{ident}' },
    { label: 'GET /api/flights/{ident}/canonical', template: 'flights/{ident}/canonical' },
    { label: 'GET /api/flights/search', template: 'flights/search' },
    { label: 'GET /api/flights/search/advanced', template: 'flights/search/advanced' },
    { label: 'GET /api/flights/search/count', template: 'flights/search/count' },
    { label: 'GET /api/flights/search/positions', template: 'flights/search/positions' }
  ],
  airports: [
    { label: 'GET /api/airports', template: 'airports' },
    { label: 'GET /api/airports/{id}', template: 'airports/{id}' },
    { label: 'GET /api/airports/{id}/canonical', template: 'airports/{id}/canonical' },
    { label: 'GET /api/airports/{id}/delays', template: 'airports/{id}/delays' },
    { label: 'GET /api/airports/{id}/flights', template: 'airports/{id}/flights' },
    { label: 'GET /api/airports/{id}/flights/arrivals', template: 'airports/{id}/flights/arrivals' },
    { label: 'GET /api/airports/{id}/flights/counts', template: 'airports/{id}/flights/counts' },
    { label: 'GET /api/airports/{id}/flights/departures', template: 'airports/{id}/flights/departures' },
    { label: 'GET /api/airports/{id}/flights/scheduled_arrivals', template: 'airports/{id}/flights/scheduled_arrivals' },
    { label: 'GET /api/airports/{id}/flights/scheduled_departures', template: 'airports/{id}/flights/scheduled_departures' },
    { label: 'GET /api/airports/{id}/flights/to/{dest_id}', template: 'airports/{id}/flights/to/{dest_id}' },
    { label: 'GET /api/airports/{id}/nearby', template: 'airports/{id}/nearby' },
    { label: 'GET /api/airports/{id}/routes/{dest_id}', template: 'airports/{id}/routes/{dest_id}' },
    { label: 'GET /api/airports/{id}/weather/forecast', template: 'airports/{id}/weather/forecast' },
    { label: 'GET /api/airports/{id}/weather/observations', template: 'airports/{id}/weather/observations' },
    { label: 'GET /api/airports/delays', template: 'airports/delays' },
    { label: 'GET /api/airports/nearby', template: 'airports/nearby' }
  ],
  operators: [
    { label: 'GET /api/operators', template: 'operators' },
    { label: 'GET /api/operators/{id}', template: 'operators/{id}' },
    { label: 'GET /api/operators/{id}/canonical', template: 'operators/{id}/canonical' },
    { label: 'GET /api/operators/{id}/flights', template: 'operators/{id}/flights' },
    { label: 'GET /api/operators/{id}/flights/arrivals', template: 'operators/{id}/flights/arrivals' },
    { label: 'GET /api/operators/{id}/flights/counts', template: 'operators/{id}/flights/counts' },
    { label: 'GET /api/operators/{id}/flights/enroute', template: 'operators/{id}/flights/enroute' },
    { label: 'GET /api/operators/{id}/flights/scheduled', template: 'operators/{id}/flights/scheduled' }
  ]
}

const selectedTemplate = ref(endpointTemplates[category.value][0].template)
const params = ref({})

const templateParams = computed(() => {
  const tmpl = customPath.value.trim() || selectedTemplate.value
  const matches = [...tmpl.matchAll(/\{([^}]+)\}/g)].map(m => m[1])
  return [...new Set(matches)]
})

watch(category, (newCategory) => {
  const first = endpointTemplates[newCategory]?.[0]?.template
  if (first) {
    selectedTemplate.value = first
    customPath.value = ''
    params.value = {}
  }
})

watch(selectedTemplate, () => {
  customPath.value = ''
  params.value = {}
})

function buildUrl() {
  const rawPath = customPath.value.trim() || selectedTemplate.value
  const filled = templateParams.value.reduce((path, key) => {
    const val = params.value[key] ?? ''
    return path.replace(`{${key}}`, encodeURIComponent(val))
  }, rawPath)

  const path = filled.replace(/^\/api\//, '')
  const paramsObj = new URLSearchParams(query.value)
  const queryString = paramsObj.toString()
  return queryString ? `/api/${path}?${queryString}` : `/api/${path}`
}

function prettyPrintJson(data) {
  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return String(data)
  }
}

async function sendRequest() {
  loading.value = true
  error.value = null
  responseText.value = ''

  const url = buildUrl()
  try {
    const res = await axios({
      method: method.value.toLowerCase(),
      url
    })
    responseText.value = prettyPrintJson(res.data ?? res.statusText)
  } catch (e) {
    if (e.response) {
      responseText.value = prettyPrintJson(e.response.data ?? e.response.statusText)
      error.value = `${e.response.status} — ${e.response.statusText}`
    } else {
      responseText.value = e.message
      error.value = e.message
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1>API Explorer</h1>
    <p class="hint">
      Quickly call backend endpoints. Pick a category, select an endpoint, fill in the
      required path parameters, and hit send.
    </p>

    <div class="form">
      <label>
        Category
        <select v-model="category">
          <option v-for="cat in categories" :key="cat.value" :value="cat.value">
            {{ cat.label }}
          </option>
        </select>
      </label>

      <label>
        Endpoint
        <select v-model="selectedTemplate">
          <option
            v-for="opt in endpointTemplates[category]"
            :key="opt.template"
            :value="opt.template"
          >
            {{ opt.label }}
          </option>
        </select>
      </label>

      <div v-if="templateParams.length" class="param-section">
        <h4>Parameters</h4>
        <div v-for="param in templateParams" :key="param" class="param-row">
          <label>
            {{ param }}
            <input
              v-model="params[param]"
              :placeholder="param"
            />
          </label>
        </div>
      </div>

      <label>
        Custom endpoint (optional)
        <input v-model="customPath" placeholder="airports/{id}" />
      </label>

      <label>
        Query string (optional)
        <input
          v-model="query"
          placeholder="page=1&limit=10"
        />
      </label>

      <div class="actions">
        <label>
          Method
          <select v-model="method">
            <option>GET</option>
          </select>
        </label>
        <button @click="sendRequest" :disabled="loading">
          {{ loading ? 'Sending…' : 'Send request' }}
        </button>
      </div>

      <div class="result">
        <div class="result-header">
          <div class="result-url">{{ buildUrl() }}</div>
          <div class="result-status">
            <span v-if="error" class="error">{{ error }}</span>
            <span v-else class="ok">OK</span>
          </div>
        </div>
        <pre>{{ responseText }}</pre>
      </div>
    </div>
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

.form label {
  display: block;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.form input,
.form select {
  width: 100%;
  padding: 0.6rem 0.75rem;
  margin-top: 0.25rem;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 6px;
  color: #e2e8f0;
  font-size: 0.95rem;
}

.param-section {
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: #0b1220;
  border: 1px solid #1f2a44;
  border-radius: 10px;
}

.param-section h4 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  color: #cbd5e1;
}

.param-row {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.param-row label {
  flex: 1;
  min-width: 220px;
}

.actions {
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.actions label {
  flex: 1;
  min-width: 200px;
}

.actions button {
  padding: 0.6rem 1.1rem;
  background: #4a9eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.95rem;
  min-width: 180px;
}

.actions button:disabled {
  opacity: 0.5;
  cursor: default;
}

.result {
  background: #0b1220;
  border: 1px solid #1f2a44;
  border-radius: 10px;
  padding: 1rem;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 0.75rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.result-url {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  color: #cbd5e1;
  word-break: break-all;
}

.result-status {
  font-weight: 600;
}

.ok {
  color: #34d399;
}

.error {
  color: #f87171;
}

pre {
  margin: 0;
  padding: 1rem;
  background: #0f172a;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 0.85rem;
  color: #cbd5e1;
  max-height: 480px;
}
</style>
