<script setup>
import { computed, ref } from "vue";
import logoUrl from "../assets/logo.png";

/**
 * Dashboard renders the "control" side of the UI:
 * - A searchable watchlist
 * - A details panel for the currently selected flight
 *
 * Why it doesn't own the data:
 * - State lives in `HomeView.vue` (`flights`, `watchlist`, `selectedFlight`).
 * - Dashboard is intentionally a presentational component that emits events to update state.
 */
const props = defineProps({
  flights: { type: Array, required: true },
  watchlist: { type: Array, required: true },
  selectedFlight: { type: Object, default: null },
  watchlistOnly: { type: Boolean, default: false },
  comLoading: { type: Boolean, default: false },
  comError: { type: Boolean, default: false },
  weatherData: { type: Object, default: null },
  isLoadingWeather: { type: Boolean, default: false },
  isVisualizingAirports: { type: Boolean, default: false },
  isLoadingNearbyAirports: { type: Boolean, default: false },
});

// Why explicit events:
// - The map component can also trigger selection/watchlist changes.
// - Using events keeps the interaction contract consistent across components.
const emit = defineEmits([
  "select-flight",
  "remove-from-watchlist",
  "clear-watchlist",
  "toggle-watchlist-only",
  "change-weather-layer",
  "load-commercial",
  "visualize-airports",
  "clear-weather",
]);

const milCount = computed(
  () => props.flights.filter((f) => f.category === "mil").length,
);
const laddCount = computed(
  () => props.flights.filter((f) => f.category === "ladd").length,
);
const piaCount = computed(
  () => props.flights.filter((f) => f.category === "pia").length,
);
const comCount = computed(
  () =>
    props.flights.filter((f) => !f.category || f.category === "commercial")
      .length,
);

// User input is local UI state; it does not need to be shared with siblings.
const searchQuery = ref("");

/**
 * Derived watchlist view.
 *
 * Why computed instead of manual filtering:
 * - It keeps filtering logic declarative and automatically updates when either
 *   the watchlist array or the search query changes.
 * - It avoids subtle bugs where filtering might not re-run after state updates.
 */
const filteredWatchlist = computed(() => {
  const q = searchQuery.value.trim().toLowerCase();
  if (!q) return props.watchlist;
  return props.watchlist.filter(
    (f) =>
      (f.callsign ?? "").toLowerCase().includes(q) ||
      (f.origin ?? "").toLowerCase().includes(q) ||
      (f.destination ?? "").toLowerCase().includes(q),
  );
});
</script>

<template>
  <aside class="dashboard">
    <header class="dashboard-header">
      <img :src="logoUrl" alt="Flight Tracker logo" class="header-logo" />
      <span class="header-title">Flight Tracker</span>
    </header>

    <!-- Panel: Live Stats -->
    <section class="panel panel--stats">
      <h2 class="panel-title">
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#4a9eff"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="18" y1="20" x2="18" y2="10" />
          <line x1="12" y1="20" x2="12" y2="4" />
          <line x1="6" y1="20" x2="6" y2="14" />
        </svg>
        Live Stats
      </h2>
      <div class="stats-row">
        <div class="stats-card">
          <span class="stats-dot stats-dot--total"></span>
          <span class="stats-label">Total</span>
          <span class="stats-count stats-count--total">{{
            flights.length
          }}</span>
        </div>
        <div class="stats-card stats-card--com">
          <span class="stats-dot stats-dot--com"></span>
          <span class="stats-label">COM</span>
          <span class="stats-count stats-count--com">{{ comCount }}</span>
          <button
            class="load-com-btn"
            :class="{ 'load-com-btn--loading': comLoading, 'load-com-btn--error': comError }"
            :disabled="comLoading"
            @click.stop="emit('load-commercial')"
            :title="comCount === 0 ? 'Load commercial flights' : 'Reload commercial flights'"
          >
            <span v-if="comLoading" class="load-com-spinner"></span>
            <span v-else-if="comError">!</span>
            <span v-else>{{ comCount === 0 ? 'Load' : 'Reload' }}</span>
          </button>
        </div>
        <div class="stats-card">
          <span class="stats-dot stats-dot--mil"></span>
          <span class="stats-label">MIL</span>
          <span class="stats-count stats-count--mil">{{ milCount }}</span>
        </div>
        <div class="stats-card">
          <span class="stats-dot stats-dot--ladd"></span>
          <span class="stats-label">LADD</span>
          <span class="stats-count stats-count--ladd">{{ laddCount }}</span>
        </div>
        <div class="stats-card stats-card--pia-only">
          <span class="stats-dot stats-dot--pia"></span>
          <span class="stats-label">PIA</span>
          <span class="stats-count stats-count--pia">{{ piaCount }}</span>
        </div>
      </div>
    </section>

    <!-- Search Bar -->
    <div class="search-wrapper">
      <svg
        class="search-icon"
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="#64748b"
        stroke-width="2.5"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      <input
        v-model="searchQuery"
        type="text"
        class="search-input"
        placeholder="Search callsign, origin, destination…"
      />
      <button
        v-if="searchQuery"
        class="search-clear"
        @click="searchQuery = ''"
        aria-label="Clear search"
      >
        <svg
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#64748b"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>

    <!-- Panel 1: Watchlist -->
    <section class="panel">
      <div class="panel-title-row">
        <h2 class="panel-title">
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#4a9eff"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
          Watchlist
        </h2>
        <div class="panel-title-actions" v-if="watchlist.length > 0">
          <button
            :class="[
              'watchonly-btn',
              { 'watchonly-btn--active': watchlistOnly },
            ]"
            @click="emit('toggle-watchlist-only')"
            :title="watchlistOnly ? 'Show all flights' : 'Show tracked only'"
          >
            {{ watchlistOnly ? "Tracked Only" : "All" }}
          </button>
          <button
            class="clear-all-btn"
            @click="emit('clear-watchlist')"
            title="Clear all"
          >
            Clear all
          </button>
        </div>
      </div>

      <p v-if="watchlist.length === 0" class="empty-hint">
        Click a plane on the map to add it to your watchlist.
      </p>

      <p v-else-if="filteredWatchlist.length === 0" class="search-no-results">
        No flights match "{{ searchQuery }}"
      </p>

      <ul v-else class="route-list">
        <li
          v-for="flight in filteredWatchlist"
          :key="flight.callsign"
          class="route-row"
          :class="{
            'route-row--selected': selectedFlight?.callsign === flight.callsign,
          }"
          @click="emit('select-flight', flight)"
        >
          <div class="route-info">
            <div class="route-header">
              <span class="callsign">{{ flight.callsign }}</span>
              <button
                class="remove-btn"
                @click.stop.prevent="emit('remove-from-watchlist', flight)"
                aria-label="Remove from watchlist"
              >
                <svg
                  width="12"
                  height="12"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="#64748b"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <div class="route-path">
              {{ flight.origin }} → {{ flight.destination }}
            </div>
          </div>
        </li>
      </ul>
    </section>

    <!-- Panel 2: Route Details -->
    <section class="panel panel--details">
      <h2 class="panel-title">
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#4a9eff"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        Route Details
      </h2>

      <div v-if="selectedFlight" class="details-content">
        <div class="details-callsign-row">
          <span class="details-callsign">{{ selectedFlight.callsign }}</span>
          <span class="live-badge">
            <span class="pulse-dot"></span>
            LIVE
          </span>
        </div>

        <div class="details-route">
          <span class="airport">{{ selectedFlight.origin }}</span>
          <svg
            width="24"
            height="12"
            viewBox="0 0 24 12"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M0 6h20M16 2l4 4-4 4"
              stroke="#4a9eff"
              stroke-width="1.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span class="airport">{{ selectedFlight.destination }}</span>
        </div>

        <div class="stat-grid">
          <div class="stat-card">
            <div class="stat-label">Altitude</div>
            <div class="stat-value">
              {{ selectedFlight.altitude.toLocaleString() }}
            </div>
            <div class="stat-unit">ft</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Speed</div>
            <div class="stat-value">{{ selectedFlight.velocity }}</div>
            <div class="stat-unit">kts</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Latitude</div>
            <div class="stat-value">{{ selectedFlight.lat.toFixed(2) }}</div>
            <div class="stat-unit">deg</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Longitude</div>
            <div class="stat-value">{{ selectedFlight.lng.toFixed(2) }}</div>
            <div class="stat-unit">deg</div>
          </div>
        </div>

        <div
          v-if="
            selectedFlight.estimatedCo2Kg != null ||
            selectedFlight.liveInfo?.flights?.[0]?.co2_estimate?.amount != null
          "
          class="stat-card stat-card--co2"
        >
          <div class="stat-label">
            <svg
              width="10"
              height="10"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#94a3b8"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
              style="margin-right: 4px"
            >
              <path d="M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2z" />
              <path d="M12 8v4l3 3" />
            </svg>
            CO₂ Estimate
          </div>
          <div class="stat-value stat-value--co2">
            {{
              (
                selectedFlight.estimatedCo2Kg ??
                selectedFlight.liveInfo.flights[0].co2_estimate.amount
              ).toLocaleString()
            }}
          </div>
          <div class="stat-unit">kg</div>
        </div>

        <!-- Weather at aircraft position -->
        <div class="weather-section" v-if="isLoadingWeather || weatherData">
          <div class="weather-section-label">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9z"/>
            </svg>
            Weather at Position
          </div>

          <div v-if="isLoadingWeather" class="weather-skeleton">
            <div class="weather-skeleton-main"></div>
            <div class="weather-skeleton-row">
              <div class="weather-skeleton-chip"></div>
              <div class="weather-skeleton-chip"></div>
              <div class="weather-skeleton-chip"></div>
            </div>
          </div>

          <div v-else-if="weatherData" class="weather-card weather-overlay" :class="{ 'weather-card--stormy': weatherData.isStormy }">
            <div class="weather-main-row">
              <div class="weather-condition">{{ weatherData.condition }}</div>
              <button class="clear-weather-btn" @click="emit('clear-weather')" title="Dismiss weather">✕</button>
              <div class="weather-temp-display">
                <span class="weather-temp-number">{{ Math.round(weatherData.temp) }}</span>
                <span class="weather-temp-unit">°F</span>
              </div>
            </div>
            <div class="weather-divider"></div>
            <div class="weather-stats-row">
              <div class="weather-stat-item">
                <div class="weather-stat-icon">
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9.59 4.59A2 2 0 1 1 11 8H2m10.59 11.41A2 2 0 1 0 14 16H2m15.73-8.27A2.5 2.5 0 1 1 19.5 12H2"/>
                  </svg>
                </div>
                <div class="weather-stat-label">Wind</div>
                <div class="weather-stat-value">{{ weatherData.windSpeed }}<span class="weather-stat-unit"> mph</span></div>
              </div>
              <div class="weather-stat-item">
                <div class="weather-stat-icon">
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                    <polyline points="17 6 23 6 23 12"/>
                  </svg>
                </div>
                <div class="weather-stat-label">Gusts</div>
                <div class="weather-stat-value">{{ weatherData.windGusts }}<span class="weather-stat-unit"> mph</span></div>
              </div>
              <div class="weather-stat-item">
                <div class="weather-stat-icon">
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9z"/>
                  </svg>
                </div>
                <div class="weather-stat-label">Cloud</div>
                <div class="weather-stat-value">{{ weatherData.cloudCover }}<span class="weather-stat-unit">%</span></div>
              </div>
            </div>
            <div v-if="weatherData.isStormy" class="weather-storm-alert">
              ⚠ Active storm conditions at this position
            </div>
          </div>
        </div>

        <!-- Visualize nearby airports -->
        <button
          :class="['viz-airports-btn', { 'viz-airports-btn--active': isVisualizingAirports }]"
          :disabled="isLoadingNearbyAirports"
          @click="emit('visualize-airports')"
        >
          <span v-if="isLoadingNearbyAirports" class="load-com-spinner"></span>
          <svg v-else width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="2" y1="12" x2="22" y2="12"/>
            <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
          </svg>
          {{ isVisualizingAirports ? 'Exit Airport View' : 'Visualize Nearby Airports' }}
        </button>

        <div class="live-info-panel" v-if="selectedFlight.liveInfo">
          <h3>FlightAware AeroAPI details</h3>
          <pre class="live-info-json">{{
            JSON.stringify(selectedFlight.liveInfo, null, 2)
          }}</pre>
        </div>
      </div>

      <div v-else class="details-empty">
        <svg
          width="40"
          height="40"
          viewBox="0 0 64 64"
          xmlns="http://www.w3.org/2000/svg"
          opacity="0.3"
        >
          <path
            fill="#4a9eff"
            d="M34 2l6 2v20l16 8v6l-16-4v20l6 6v2l-12-4-12 4v-2l6-6V34l-16 4v-6l16-8V4l6-2z"
          />
        </svg>
        <p>Select a route to view details</p>
      </div>
    </section>

    <footer class="dashboard-footer">
      <span class="footer-dot"></span>
      <span>Live tracking active</span>
      <div class="weather-layer-switch">
        <button class="wl-btn" @click="emit('change-weather-layer', 'none')">OFF</button>
        <button class="wl-btn wl-btn--hazards" @click="emit('change-weather-layer', 'sigmet')">HAZARDS</button>
        <button class="wl-btn wl-btn--radar" @click="emit('change-weather-layer', 'radar')">RADAR</button>
      </div>
    </footer>
  </aside>
</template>

<style scoped>
.dashboard {
  width: clamp(200px, 26vw, 380px);
  min-width: 0;
  height: 100vh;
  background: #06111f;
  border-right: 1px solid #1e3a5f;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  font-family:
    Inter,
    system-ui,
    -apple-system,
    sans-serif;
  color: #e2e8f0;
  flex-shrink: 0;
}

/* Header */
.dashboard-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  border-bottom: 1px solid #1e3a5f;
  background: #06111f;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 6px;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #f1f5f9;
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

.clear-all-btn {
  margin-left: auto;
  background: none;
  border: none;
  color: #475569;
  font-size: 10px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 3px;
  transition: color 0.15s;
}
.clear-all-btn:hover {
  color: #f87171;
}

/* Route list */
.route-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.route-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: #0d2039;
  cursor: pointer;
  transition:
    border-color 0.15s,
    background 0.15s;
}

.route-row:hover {
  background: #0f2847;
  border-color: #1e3a5f;
}

.route-row--selected {
  border-color: #4a9eff;
  background: #0f2847;
}

.live-info-panel {
  margin-top: 16px;
  background: #0f1e35;
  border: 1px solid #1e3a5f;
  border-radius: 8px;
  padding: 10px;
  font-size: 12px;
  color: #cbd5e1;
}

.live-info-json {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 150px;
  overflow: auto;
  background: #071223;
  border: 1px solid #314e7d;
  border-radius: 4px;
  padding: 8px;
  color: #dbeafe;
}

.route-info {
  flex: 1;
  min-width: 0;
}

.route-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.callsign {
  font-size: 13px;
  font-weight: 600;
  color: #f1f5f9;
}

.route-path {
  font-size: 11px;
  color: #64748b;
}

.remove-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  margin: -6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.15s;
  flex-shrink: 0;
}

.remove-btn:hover {
  background: #1e3a5f;
}

.remove-btn:hover svg {
  stroke: #f87171;
}

/* Empty states */
.empty-hint {
  font-size: 12px;
  color: #334155;
  text-align: center;
  padding: 16px 0 8px;
  margin: 0;
  line-height: 1.5;
}

.search-no-results {
  font-size: 12px;
  color: #334155;
  text-align: center;
  padding: 16px 0 8px;
  margin: 0;
}

/* Details panel */
.details-callsign-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.details-callsign {
  font-size: 20px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: 0.05em;
}

.live-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #4ade80;
  background: rgba(74, 222, 128, 0.1);
  border: 1px solid rgba(74, 222, 128, 0.3);
  border-radius: 20px;
  padding: 3px 8px;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4ade80;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
}

.details-route {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding: 8px 12px;
  background: #0d2039;
  border-radius: 8px;
  border: 1px solid #1e3a5f;
}

.airport {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #f1f5f9;
}

.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.stat-card {
  background: #0d2039;
  border: 1px solid #1e3a5f;
  border-radius: 8px;
  padding: 10px 12px;
}

.stat-card--co2 {
  margin-top: 8px;
  border-color: #1e4a3a;
  background: #071e14;
}

.stat-value--co2 {
  color: #4ade80;
}

.stat-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #4a9eff;
  line-height: 1;
}

.stat-unit {
  font-size: 10px;
  color: #64748b;
  margin-top: 2px;
}

/* Empty state */
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
.dashboard-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 11px;
  color: #334155;
  border-top: 1px solid #1e3a5f;
  margin-top: auto;
}

.footer-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4ade80;
  animation: pulse 2s ease-in-out infinite;
}

/* Search bar */
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
  flex-shrink: 0;
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
  border-color: #4a9eff;
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
  justify-content: center;
  border-radius: 4px;
  transition: background 0.15s;
}

.search-clear:hover {
  background: #1e3a5f;
}

/* Live stats panel */
.panel--stats {
  padding: 12px 16px;
}

.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.stats-card {
  flex: 1;
  background: #0d2039;
  border: 1px solid #1e3a5f;
  border-radius: 8px;
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.stats-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.stats-dot--total {
  background: #4a9eff;
}
.stats-dot--com {
  background: #00d4ff;
}
.stats-dot--mil {
  background: #ff4444;
}
.stats-dot--ladd {
  background: #f59e0b;
}
.stats-dot--pia {
  background: #a855f7;
}

.stats-label {
  font-size: 9px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #64748b;
}

.stats-count {
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.stats-count--total {
  color: #4a9eff;
}
.stats-count--com {
  color: #00d4ff;
}
.stats-count--mil {
  color: #ff4444;
}
.stats-count--ladd {
  color: #f59e0b;
}
.stats-count--pia {
  color: #a855f7;
}

/* PIA card spans both columns when it's the odd one out */
.stats-card--pia-only {
  grid-column: 1 / -1;
  flex-direction: row;
  justify-content: center;
  gap: 8px;
  padding: 6px 12px;
}

.stats-card--com {
  position: relative;
}

.load-com-btn {
  margin-top: 4px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid rgba(74, 158, 255, 0.4);
  background: rgba(74, 158, 255, 0.08);
  color: #4a9eff;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.load-com-btn:hover:not(:disabled) {
  background: rgba(74, 158, 255, 0.16);
  border-color: rgba(74, 158, 255, 0.7);
}

.load-com-btn:disabled {
  cursor: default;
  opacity: 0.7;
}

.load-com-btn--error {
  border-color: rgba(239, 68, 68, 0.4);
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
}

.viz-airports-btn {
  width: 100%;
  margin-top: 10px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  padding: 7px 10px;
  border-radius: 6px;
  border: 1px solid rgba(0, 212, 255, 0.35);
  background: rgba(0, 212, 255, 0.07);
  color: #00d4ff;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.viz-airports-btn:hover:not(:disabled) {
  background: rgba(0, 212, 255, 0.15);
  border-color: rgba(0, 212, 255, 0.7);
}

.viz-airports-btn:disabled {
  cursor: default;
  opacity: 0.6;
}

.viz-airports-btn--active {
  background: rgba(0, 212, 255, 0.18);
  border-color: rgba(0, 212, 255, 0.8);
  color: #00eeff;
}

.load-com-spinner {
  display: inline-block;
  width: 8px;
  height: 8px;
  border: 1.5px solid rgba(74, 158, 255, 0.3);
  border-top-color: #4a9eff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* ── Weather Widget ─────────────────────────────── */
.weather-section {
  margin-top: 10px;
}

.weather-section-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 8px;
}

.weather-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.weather-skeleton-main {
  height: 56px;
  border-radius: 8px;
  background: linear-gradient(90deg, #0d2039 25%, #142d4d 50%, #0d2039 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.weather-skeleton-row {
  display: flex;
  gap: 6px;
}

.weather-skeleton-chip {
  flex: 1;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(90deg, #0d2039 25%, #142d4d 50%, #0d2039 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.weather-card {
  background: linear-gradient(135deg, #0d2039 0%, #081929 100%);
  border: 1px solid #1e3a5f;
  border-radius: 10px;
  overflow: hidden;
}

.weather-card--stormy {
  border-color: rgba(245, 158, 11, 0.4);
  background: linear-gradient(135deg, #1a1305 0%, #0d2039 100%);
}

.weather-main-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px 10px;
}

.weather-condition {
  font-size: 13px;
  font-weight: 600;
  color: #f1f5f9;
  line-height: 1.3;
  flex: 1;
  margin-right: 8px;
}

.weather-temp-display {
  display: flex;
  align-items: flex-start;
  flex-shrink: 0;
}

.weather-temp-number {
  font-size: 32px;
  font-weight: 700;
  color: #4a9eff;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.weather-temp-unit {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  margin-top: 4px;
  margin-left: 2px;
}

.weather-divider {
  height: 1px;
  background: rgba(30, 58, 95, 0.8);
  margin: 0 14px;
}

.weather-stats-row {
  display: flex;
  padding: 10px 14px;
  gap: 0;
}

.weather-stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 4px 0;
  border-right: 1px solid #1e3a5f;
}

.weather-stat-item:last-child {
  border-right: none;
}

.weather-stat-icon {
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1px;
}

.weather-stat-label {
  font-size: 9px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: #475569;
}

.weather-stat-value {
  font-size: 13px;
  font-weight: 700;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.weather-stat-unit {
  font-size: 9px;
  font-weight: 400;
  color: #64748b;
}

.weather-storm-alert {
  background: rgba(245, 158, 11, 0.1);
  border-top: 1px solid rgba(245, 158, 11, 0.25);
  color: #f59e0b;
  font-size: 10px;
  font-weight: 600;
  padding: 6px 14px;
  letter-spacing: 0.02em;
}

.clear-weather-btn {
  background: none;
  border: none;
  color: #475569;
  font-size: 11px;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  line-height: 1;
  flex-shrink: 0;
}
.clear-weather-btn:hover { color: #94a3b8; }

.weather-layer-switch {
  margin-left: auto;
  display: flex;
  gap: 4px;
}
.wl-btn {
  background: transparent;
  border: 1px solid rgba(100, 116, 139, 0.25);
  border-radius: 4px;
  color: #64748b;
  font-size: 9px;
  font-weight: 600;
  padding: 2px 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.wl-btn:hover { background: rgba(100, 116, 139, 0.15); color: #94a3b8; }
.wl-btn--hazards { color: #f87171; border-color: rgba(248, 113, 113, 0.3); }
.wl-btn--hazards:hover { background: rgba(248, 113, 113, 0.1); }
.wl-btn--radar { color: #4ade80; border-color: rgba(74, 222, 128, 0.3); }
.wl-btn--radar:hover { background: rgba(74, 222, 128, 0.1); }

/* Watchlist panel title row with toggle */
.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-title-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.panel-title-row .panel-title {
  margin-bottom: 0;
}

.watchonly-btn {
  background: rgba(0, 212, 255, 0.04);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 4px;
  color: #475569;
  font-size: 0.62rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.15rem 0.4rem;
  cursor: pointer;
  transition: all 0.12s;
  font-family: inherit;
}
.watchonly-btn:hover {
  color: #94a3b8;
  border-color: rgba(0, 212, 255, 0.35);
}
.watchonly-btn--active {
  background: rgba(0, 212, 255, 0.12);
  border-color: rgba(0, 212, 255, 0.45);
  color: #00d4ff;
}
</style>
