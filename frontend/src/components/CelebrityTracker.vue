<script setup>
/**
 * CelebrityTracker — sidebar panel that lists tracked celebrities and their live flight status.
 *
 * Auth-aware: when a logged-in user selects a celebrity the flight history is served
 * from a pre-loaded in-memory cache (populated at login by App.vue) before falling back
 * to a live DB fetch. This avoids unnecessary API calls on repeated selections.
 */
import { ref, computed, onMounted, onUnmounted, inject } from 'vue'

const emit = defineEmits(['focus-aircraft'])

const celebrities = ref([])
const searchQuery = ref('')
const selectedCelebrity = ref(null)
const lastRefreshed = ref(null)
const flightHistory = ref(null)
const flightsLoading = ref(false)

const TRACKED_KEY = 'celebrity_tracker_tracked_ids'
function loadTracked() {
  try {
    const saved = localStorage.getItem(TRACKED_KEY)
    return saved ? new Set(JSON.parse(saved)) : new Set()
  } catch { return new Set() }
}
const trackedIds = ref(loadTracked())

function saveTracked() {
  localStorage.setItem(TRACKED_KEY, JSON.stringify([...trackedIds.value]))
}

function trackCelebrity(id) {
  trackedIds.value = new Set([...trackedIds.value, id])
  saveTracked()
}

function untrackCelebrity(id) {
  const next = new Set(trackedIds.value)
  next.delete(id)
  trackedIds.value = next
  saveTracked()
  if (selectedCelebrity.value?.id === id) {
    selectedCelebrity.value = null
    flightHistory.value = null
  }
}

// Injected from App.vue — avoids prop-drilling through intermediate components.
// currentUser is null when not logged in (or when login is disabled via env var).
const currentUser = inject('currentUser', ref(null))
const userFlightHistory = inject('userFlightHistory', ref({}))

async function fetchCelebrities() {
  try {
    const res = await fetch('/api/db/celebrities/live')
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    celebrities.value = await res.json()
    lastRefreshed.value = new Date()
  } catch {
    // keep existing data on error; footer will show stale time
  }
}

async function fetchFlightHistory(id) {
  // Serve from pre-loaded cache first (populated at login from DB, no AeroAPI call needed)
  const cached = userFlightHistory.value?.[id]
  if (cached?.flights?.length > 0) {
    flightHistory.value = cached
    return
  }

  flightsLoading.value = true
  flightHistory.value = null
  try {
    const userId = currentUser.value?.uId
    const url = userId
      ? `/api/db/celebrities/${id}/flights?userId=${userId}`
      : `/api/db/celebrities/${id}/flights`
    const res = await fetch(url)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = await res.json()
    flightHistory.value = data

    // Update the local cache so sibling / future selects are instant
    if (userFlightHistory.value && data.flights?.length > 0) {
      userFlightHistory.value = {
        ...userFlightHistory.value,
        [id]: { ...data, celebrityId: id, lastFetchedAt: new Date().toISOString() }
      }
    }
  } catch {
    flightHistory.value = { flights: [], error: 'Could not load flight history' }
  } finally {
    flightsLoading.value = false
  }
}

// airborne is a tri-state: true (in-flight), false (on ground), null (no recent data)
function statusFor(celeb) {
  if (celeb.airborne === true) return 'in-flight'
  if (celeb.airborne === false) return 'on-ground'
  return 'unknown'
}

const filteredAll = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return celebrities.value
  return celebrities.value.filter(c =>
    c.name.toLowerCase().includes(q) ||
    (c.model && c.model.toLowerCase().includes(q)) ||
    (c.tailNumber && c.tailNumber.toLowerCase().includes(q))
  )
})

const trackedCelebrities = computed(() =>
  filteredAll.value.filter(c => trackedIds.value.has(c.id))
)

const untrackedCelebrities = computed(() =>
  filteredAll.value.filter(c => !trackedIds.value.has(c.id))
)

function selectCelebrity(celeb) {
  if (selectedCelebrity.value?.id === celeb.id) {
    selectedCelebrity.value = null
    flightHistory.value = null
  } else {
    selectedCelebrity.value = celeb
    fetchFlightHistory(celeb.id)
    if (celeb.tailNumber) emit('focus-aircraft', celeb.tailNumber)
  }
}

function formatLastSeen(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleString()
}

function formatFlightTime(ts) {
  if (!ts) return '—'
  const d = new Date(ts)
  if (isNaN(d.getTime())) return ts
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
    + ' ' + d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

function formatCo2(kg) {
  if (kg == null) return null
  if (kg >= 1000) return (kg / 1000).toFixed(1) + ' t CO₂'
  return kg.toFixed(0) + ' kg CO₂'
}

// AeroAPI flight statuses mapped to friendly labels
function flightStatusLabel(status) {
  if (!status) return null
  const map = {
    Scheduled: 'Scheduled',
    Active: 'In Flight',
    Landed: 'Landed',
    Cancelled: 'Cancelled',
    Diverted: 'Diverted',
    ResultUnknown: 'Unknown'
  }
  return map[status] || status
}

// De-duplicate rows by origin+destination+departure date; also strip degenerate records
// (no origin, no destination, no departure time) that should never be rendered.
const deduplicatedFlights = computed(() => {
  if (!flightHistory.value?.flights) return []
  const seen = new Set()
  return flightHistory.value.flights
    .filter(f => f.origin || f.destination || f.departureTime)
    .filter(f => {
      const key = `${f.origin}|${f.destination}|${(f.departureTime || '').slice(0, 10)}`
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
})

// True when the result being shown came from the local pre-loaded cache (no spinner shown)
const isFromCache = computed(() =>
  flightHistory.value != null &&
  userFlightHistory.value?.[selectedCelebrity.value?.id] != null &&
  !flightsLoading.value
)

let pollTimer = null

onMounted(() => {
  fetchCelebrities()
  pollTimer = setInterval(fetchCelebrities, 60_000)
})

onUnmounted(() => {
  clearInterval(pollTimer)
})
</script>

<template>
  <aside class="celebrity-tracker">
    <header class="tracker-header">
      <svg class="header-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
      </svg>
      <span class="header-title">Celebrity Tracker</span>
      <span class="header-badge">{{ trackedIds.size }}</span>
    </header>

    <div class="search-wrapper">
      <svg class="search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="11" cy="11" r="8"/>
        <line x1="21" y1="21" x2="16.65" y2="16.65"/>
      </svg>
      <input
        v-model="searchQuery"
        type="text"
        class="search-input"
        placeholder="Search name, aircraft, tail…"
      />
      <button v-if="searchQuery" class="search-clear" @click="searchQuery = ''" aria-label="Clear search">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"/>
          <line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </button>
    </div>

    <!-- Tracked Celebrities -->
    <section class="panel">
      <h2 class="panel-title">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
          <circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
        </svg>
        Tracked Celebrities
      </h2>

      <p v-if="trackedIds.size === 0" class="empty-hint">
        No celebrities tracked yet. Add some below.
      </p>

      <ul v-else-if="trackedCelebrities.length > 0" class="celeb-list">
        <li
          v-for="celeb in trackedCelebrities"
          :key="celeb.id"
          class="celeb-row"
          :class="{ 'celeb-row--selected': selectedCelebrity?.id === celeb.id }"
          @click="selectCelebrity(celeb)"
        >
          <div class="celeb-avatar">{{ celeb.name.charAt(0) }}</div>
          <div class="celeb-info">
            <div class="celeb-header">
              <span class="celeb-name">{{ celeb.name }}</span>
              <span
                class="status-badge"
                :class="{
                  'status-badge--flying': statusFor(celeb) === 'in-flight',
                  'status-badge--ground': statusFor(celeb) === 'on-ground',
                  'status-badge--unknown': statusFor(celeb) === 'unknown'
                }"
              >
                {{ statusFor(celeb) === 'in-flight' ? 'In Flight' : statusFor(celeb) === 'on-ground' ? 'On Ground' : 'Unknown' }}
              </span>
            </div>
            <div class="celeb-sub">{{ celeb.tailNumber || '—' }} · {{ celeb.model || '—' }}</div>
          </div>
          <button
            class="track-btn track-btn--remove"
            title="Untrack"
            @click.stop="untrackCelebrity(celeb.id)"
          >✕</button>
        </li>
      </ul>

      <p v-else class="empty-hint">No tracked celebrities match "{{ searchQuery }}"</p>
    </section>

    <!-- Browse All Celebrities -->
    <section class="panel">
      <h2 class="panel-title">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        Browse All
      </h2>

      <p v-if="celebrities.length === 0" class="empty-hint">Loading…</p>

      <p v-else-if="untrackedCelebrities.length === 0 && searchQuery" class="empty-hint">
        No untracked celebrities match "{{ searchQuery }}"
      </p>

      <p v-else-if="untrackedCelebrities.length === 0" class="empty-hint">
        All celebrities are tracked.
      </p>

      <ul v-else class="celeb-list">
        <li
          v-for="celeb in untrackedCelebrities"
          :key="celeb.id"
          class="celeb-row celeb-row--browse"
          @click="selectCelebrity(celeb)"
        >
          <div class="celeb-avatar celeb-avatar--dim">{{ celeb.name.charAt(0) }}</div>
          <div class="celeb-info">
            <div class="celeb-header">
              <span class="celeb-name">{{ celeb.name }}</span>
            </div>
            <div class="celeb-sub">{{ celeb.tailNumber || '—' }} · {{ celeb.model || '—' }}</div>
          </div>
          <button
            class="track-btn track-btn--add"
            title="Track this celebrity"
            @click.stop="trackCelebrity(celeb.id)"
          >+</button>
        </li>
      </ul>
    </section>

    <section class="panel panel--details">
      <h2 class="panel-title">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        Flight Details
      </h2>

      <div v-if="selectedCelebrity" class="details-content">
        <div class="details-name-row">
          <div class="details-avatar">{{ selectedCelebrity.name.charAt(0) }}</div>
          <div>
            <div class="details-name">{{ selectedCelebrity.name }}</div>
            <div class="details-tail">{{ selectedCelebrity.tailNumber || 'No aircraft linked' }}</div>
          </div>
          <span
            class="status-badge"
            :class="{
              'status-badge--flying': statusFor(selectedCelebrity) === 'in-flight',
              'status-badge--ground': statusFor(selectedCelebrity) === 'on-ground',
              'status-badge--unknown': statusFor(selectedCelebrity) === 'unknown'
            }"
          >
            <span v-if="statusFor(selectedCelebrity) === 'in-flight'" class="pulse-dot pulse-dot--amber"></span>
            {{ statusFor(selectedCelebrity) === 'in-flight' ? 'In Flight' : statusFor(selectedCelebrity) === 'on-ground' ? 'On Ground' : 'Unknown' }}
          </span>
        </div>

        <div class="detail-card" v-if="selectedCelebrity.model">
          <div class="detail-label">Aircraft</div>
          <div class="detail-value">{{ selectedCelebrity.model }}</div>
        </div>

        <div class="detail-card" v-if="selectedCelebrity.lastLat != null && selectedCelebrity.lastLon != null">
          <div class="detail-label">Last Position</div>
          <div class="detail-value">{{ selectedCelebrity.lastLat.toFixed(3) }}, {{ selectedCelebrity.lastLon.toFixed(3) }}</div>
        </div>

        <div class="detail-card" v-if="selectedCelebrity.lastSeen">
          <div class="detail-label">Last Seen</div>
          <div class="detail-value">{{ formatLastSeen(selectedCelebrity.lastSeen) }}</div>
        </div>

        <!-- Flight history -->
        <div class="flights-section">
          <div class="flights-section-header">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 16.92V19a2 2 0 0 1-2.18 2A19.72 19.72 0 0 1 3 5.18 2 2 0 0 1 5 3h2.09a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 10.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 17z"/>
            </svg>
            <span>Flight History</span>
            <span v-if="isFromCache" class="cached-badge" title="Loaded from your saved history">cached</span>
            <span v-if="flightHistory && !flightsLoading" class="flights-count-badge">
              {{ deduplicatedFlights.length }}
            </span>
          </div>

          <div v-if="flightsLoading" class="flights-loading">
            <span class="loading-spinner"></span>
            Fetching flights…
          </div>

          <div v-else-if="flightHistory?.error" class="flights-error">
            {{ flightHistory.error }}
          </div>

          <div v-else-if="deduplicatedFlights.length === 0 && flightHistory" class="flights-empty">
            No flights found in the database or AeroAPI for this aircraft.
          </div>

          <div v-else class="flights-list">
            <div
              v-for="(flight, idx) in deduplicatedFlights"
              :key="idx"
              class="flight-card"
            >
              <!-- Route row -->
              <div class="flight-route">
                <div class="flight-airport">
                  <span class="airport-code">{{ flight.origin || '—' }}</span>
                  <span v-if="flight.originCity" class="airport-city">{{ flight.originCity }}</span>
                </div>
                <div class="flight-arrow">
                  <svg width="18" height="10" viewBox="0 0 24 12" fill="none">
                    <path d="M2 6 H20 M16 2 L22 6 L16 10" stroke="#475569" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
                <div class="flight-airport flight-airport--right">
                  <span class="airport-code">{{ flight.destination || '—' }}</span>
                  <span v-if="flight.destinationCity" class="airport-city">{{ flight.destinationCity }}</span>
                </div>
              </div>

              <!-- Meta row -->
              <div class="flight-meta">
                <span class="flight-date">{{ formatFlightTime(flight.departureTime) }}</span>
                <div class="flight-badges">
                  <span v-if="flight.status" class="flight-status-badge"
                    :class="{
                      'badge--active': flight.status === 'Active',
                      'badge--landed': flight.status === 'Landed',
                      'badge--scheduled': flight.status === 'Scheduled',
                      'badge--other': !['Active','Landed','Scheduled'].includes(flight.status)
                    }">
                    {{ flightStatusLabel(flight.status) }}
                  </span>
                  <span v-if="flight.estimatedCo2Kg" class="flight-co2-badge">
                    🌿 {{ formatCo2(flight.estimatedCo2Kg) }}
                  </span>
                  <span v-if="flight.progressPercent > 0 && flight.progressPercent < 100" class="flight-progress-badge">
                    {{ flight.progressPercent }}%
                  </span>
                </div>
              </div>

              <!-- Tail / aircraft type sub-line -->
              <div class="flight-tail">
                <span class="tail-chip">{{ flight.tailNumber }}</span>
                <span v-if="flight.aircraftType" class="tail-type">{{ flight.aircraftType }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="details-empty">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" opacity="0.25">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
        </svg>
        <p>Select a celebrity to view details</p>
      </div>
    </section>

    <footer class="tracker-footer">
      <span class="footer-dot footer-dot--amber"></span>
      <span v-if="lastRefreshed">Updated {{ lastRefreshed.toLocaleTimeString() }}</span>
      <span v-else>Connecting…</span>
    </footer>
  </aside>
</template>

<style scoped>
.celebrity-tracker {
  width: clamp(160px, 22vw, 300px);
  min-width: 0;
  height: 100vh;
  background: #06111f;
  border-left: 1px solid #1e3a5f;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  font-family: Inter, system-ui, -apple-system, sans-serif;
  color: #e2e8f0;
  flex-shrink: 0;
}

/* Header */
.tracker-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 16px;
  border-bottom: 1px solid #1e3a5f;
  background: #06111f;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-icon {
  flex-shrink: 0;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #f1f5f9;
  flex: 1;
}

.header-badge {
  font-size: 11px;
  font-weight: 700;
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 12px;
  padding: 2px 7px;
}

/* Search */
.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #1e3a5f;
  background: #06111f;
}

.search-icon {
  position: absolute;
  left: 28px;
  pointer-events: none;
}

.search-input {
  width: 100%;
  background: #0d2039;
  border: 1px solid #1e3a5f;
  border-radius: 8px;
  padding: 7px 32px 7px 34px;
  font-size: 12px;
  color: #e2e8f0;
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s;
}

.search-input::placeholder {
  color: #334155;
}

.search-input:focus {
  border-color: #f59e0b;
}

.search-clear {
  position: absolute;
  right: 26px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
  border-radius: 4px;
  transition: background 0.15s;
}

.search-clear:hover {
  background: #1e3a5f;
}

/* Panels */
.panel {
  padding: 16px;
  border-bottom: 1px solid #1e3a5f;
}

.panel--details {
  flex: 1;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
  margin: 0 0 12px 0;
}

/* Celebrity list */
.celeb-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.celeb-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: #0d2039;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.celeb-row:hover {
  background: #0f2847;
  border-color: #1e3a5f;
}

.celeb-row--selected {
  border-color: #f59e0b;
  background: #0f2847;
}

.celeb-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1e3a5f, #0d2039);
  border: 1px solid #1e3a5f;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #f59e0b;
  flex-shrink: 0;
}

.celeb-row--browse {
  cursor: default;
  opacity: 0.75;
}

.celeb-row--browse:hover {
  opacity: 1;
}

.celeb-avatar--dim {
  color: #64748b;
  border-color: #1e3a5f;
}

.track-btn {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  border: none;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, transform 0.1s;
  line-height: 1;
}

.track-btn--add {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.35);
}

.track-btn--add:hover {
  background: rgba(245, 158, 11, 0.3);
  transform: scale(1.1);
}

.track-btn--remove {
  background: rgba(100, 116, 139, 0.15);
  color: #64748b;
  border: 1px solid rgba(100, 116, 139, 0.25);
  font-size: 10px;
}

.track-btn--remove:hover {
  background: rgba(248, 113, 113, 0.2);
  color: #f87171;
  border-color: rgba(248, 113, 113, 0.3);
}

.celeb-info {
  flex: 1;
  min-width: 0;
}

.celeb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 2px;
}

.celeb-name {
  font-size: 13px;
  font-weight: 600;
  color: #f1f5f9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.celeb-sub {
  font-size: 11px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Status badges */
.status-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  border-radius: 20px;
  padding: 2px 7px;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-badge--flying {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.status-badge--ground {
  color: #94a3b8;
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.status-badge--unknown {
  color: #475569;
  background: rgba(71, 85, 105, 0.08);
  border: 1px solid rgba(71, 85, 105, 0.2);
}

.pulse-dot--amber {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  animation: pulse 1.5s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

/* Details panel */
.details-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.details-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.details-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1e3a5f, #0d2039);
  border: 1px solid rgba(245, 158, 11, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #f59e0b;
  flex-shrink: 0;
}

.details-name {
  font-size: 16px;
  font-weight: 700;
  color: #f1f5f9;
}

.details-tail {
  font-size: 11px;
  color: #64748b;
  margin-top: 1px;
}

.detail-card {
  background: #0d2039;
  border: 1px solid #1e3a5f;
  border-radius: 8px;
  padding: 10px 12px;
}

.detail-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 4px;
}

.detail-value {
  font-size: 13px;
  font-weight: 600;
  color: #f59e0b;
}

/* Empty state */
.empty-hint {
  font-size: 12px;
  color: #334155;
  text-align: center;
  padding: 16px 0 8px;
  margin: 0;
}

.details-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px 0;
  color: #334155;
  font-size: 13px;
  text-align: center;
}

.details-empty p {
  margin: 0;
}

/* Footer */
.tracker-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 11px;
  color: #334155;
  border-top: 1px solid #1e3a5f;
  margin-top: auto;
}

.footer-dot--amber {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f59e0b;
  flex-shrink: 0;
  opacity: 0.5;
}

/* ── Flight history ─────────────────────────────────────────────────────── */

.flights-section {
  margin-top: 6px;
}

.flights-section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 10px;
}

.flights-count-badge {
  font-size: 10px;
  font-weight: 700;
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.25);
  border-radius: 10px;
  padding: 1px 6px;
  margin-left: auto;
}

.cached-badge {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #34d399;
  background: rgba(52, 211, 153, 0.08);
  border: 1px solid rgba(52, 211, 153, 0.25);
  border-radius: 10px;
  padding: 1px 6px;
}

.flights-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #475569;
  padding: 12px 0;
}

.loading-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid #1e3a5f;
  border-top-color: #f59e0b;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.flights-error {
  font-size: 11px;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.22);
  border-radius: 6px;
  padding: 8px 10px;
}

.flights-empty {
  font-size: 11px;
  color: #334155;
  padding: 8px 0;
  text-align: center;
}

.flights-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.flight-card {
  background: #0a1d32;
  border: 1px solid #1e3a5f;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: border-color 0.15s;
}

.flight-card:hover {
  border-color: rgba(245, 158, 11, 0.35);
}

/* Route row */
.flight-route {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}

.flight-airport {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.flight-airport--right {
  text-align: right;
  align-items: flex-end;
}

.airport-code {
  font-size: 15px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: 0.03em;
  line-height: 1;
}

.airport-city {
  font-size: 10px;
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 72px;
  margin-top: 1px;
}

.flight-arrow {
  flex: 1;
  display: flex;
  justify-content: center;
  opacity: 0.6;
}

/* Meta row */
.flight-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  flex-wrap: wrap;
}

.flight-date {
  font-size: 10px;
  color: #475569;
}

.flight-badges {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.flight-status-badge {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  border-radius: 20px;
  padding: 2px 6px;
}

.badge--active {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.badge--landed {
  color: #34d399;
  background: rgba(52, 211, 153, 0.08);
  border: 1px solid rgba(52, 211, 153, 0.25);
}

.badge--scheduled {
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.08);
  border: 1px solid rgba(96, 165, 250, 0.25);
}

.badge--other {
  color: #94a3b8;
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.flight-co2-badge {
  font-size: 9px;
  color: #34d399;
  background: rgba(52, 211, 153, 0.06);
  border: 1px solid rgba(52, 211, 153, 0.2);
  border-radius: 20px;
  padding: 2px 6px;
}

.flight-progress-badge {
  font-size: 9px;
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 20px;
  padding: 2px 6px;
}

/* Tail chip */
.flight-tail {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tail-chip {
  font-size: 10px;
  font-weight: 700;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  color: #64748b;
  background: #0d2039;
  border: 1px solid #1e3a5f;
  border-radius: 4px;
  padding: 1px 5px;
}

.tail-type {
  font-size: 10px;
  color: #475569;
}
</style>
