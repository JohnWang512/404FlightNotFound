<script setup>
import { ref, computed, inject, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import axios from "axios";
import GlobeContainer from "../components/GlobeContainer.vue";
import {
  loadTrackedFlights,
  removeTrackedFlight,
  saveTrackedFlight,
} from "../services/trackedFlights";

const router = useRouter();

const currentUser = inject("currentUser", ref(null));

// Initialise synchronously from localStorage when no user is logged in so
// that the watchlist is available on the very first render (before any async
// tick).  When a user IS logged in the onMounted hook will replace this with
// the server-side list.
const watchlist = ref(
  (() => {
    if (currentUser?.value?.uId) return [];
    try {
      return JSON.parse(localStorage.getItem("ft_watchlist") || "[]");
    } catch {
      return [];
    }
  })(),
);

// Live marker poll — same strategy as HomeView
const liveMarkers = ref([]);
let livePollInterval = null;

async function fetchLiveMarkers() {
  try {
    const res = await axios.get("/api/db/aircraft/live");
    const watchlistCallsigns = new Set(watchlist.value.map((f) => f.callsign));
    const watchlistTails = new Set(
      watchlist.value.map((f) => f.tailNumber).filter(Boolean),
    );

    liveMarkers.value = (res.data || [])
      .map((a) => {
        const callsign = a.lastCallsign || a.tailNumber || "UNKNOWN";
        const enriched = watchlist.value.find(
          (f) => f.callsign === callsign || f.tailNumber === a.tailNumber,
        );
        return {
          callsign,
          tailNumber: a.tailNumber,
          origin: enriched?.origin || "N/A",
          destination: enriched?.destination || "N/A",
          altitude:
            enriched?.altitude ??
            (a.lastAltitude != null ? a.lastAltitude * 100 : 0),
          velocity: enriched?.velocity ?? a.lastSpeed ?? 0,
          lat: a.lastLat,
          lng: a.lastLon,
          bearing: a.lastHeading ?? enriched?.bearing ?? 0,
          estimatedCo2Kg: a.estimatedCo2Kg ?? enriched?.estimatedCo2Kg ?? null,
          category: a.category,
          celebrity: enriched?.celebrity ?? null,
        };
      })
      // Only keep aircraft in the watchlist
      .filter(
        (m) =>
          watchlistCallsigns.has(m.callsign) || watchlistTails.has(m.tailNumber),
      );
  } catch {
    // silent — stale watchlist entries remain on the globe
  }
}

// Flights shown on the globe: live data where available, watchlist entry as fallback
const displayedFlights = computed(() => {
  if (liveMarkers.value.length === 0) return watchlist.value;

  const liveCallsigns = new Set(liveMarkers.value.map((m) => m.callsign));
  const liveTails = new Set(
    liveMarkers.value.map((m) => m.tailNumber).filter(Boolean),
  );

  // Watchlist entries that have no current live position (recently added but not yet airborne, etc.)
  const stale = watchlist.value.filter(
    (f) =>
      !liveCallsigns.has(f.callsign) &&
      !(f.tailNumber && liveTails.has(f.tailNumber)),
  );

  return [...liveMarkers.value, ...stale];
});

// --- Flight selection & detail ---
const selectedFlight = ref(null);

function onSelectFlight(flight) {
  if (selectedFlight.value?.callsign === flight.callsign) {
    selectedFlight.value = null;
  } else {
    selectedFlight.value = flight;
  }
}

async function onToggleWatchlist(flight) {
  const index = watchlist.value.findIndex(
    (f) => f.callsign === flight.callsign,
  );
  if (index === -1) {
    watchlist.value.push(flight);
    try {
      await saveTrackedFlight(currentUser, flight);
    } catch {
      watchlist.value = watchlist.value.filter((f) => f.callsign !== flight.callsign);
    }
  } else {
    const removed = watchlist.value[index];
    watchlist.value.splice(index, 1);
    try {
      await removeTrackedFlight(currentUser, removed.callsign);
    } catch {
      watchlist.value.push(removed);
    }
    if (selectedFlight.value?.callsign === flight.callsign) {
      selectedFlight.value = null;
    }
  }
}

async function removeFromWatchlist(flight) {
  const previous = [...watchlist.value];
  watchlist.value = watchlist.value.filter(
    (f) => f.callsign !== flight.callsign,
  );
  try {
    await removeTrackedFlight(currentUser, flight.callsign);
  } catch {
    watchlist.value = previous;
  }
  if (selectedFlight.value?.callsign === flight.callsign) {
    selectedFlight.value = null;
  }
}

// --- Sidebar ---
const searchQuery = ref("");
const showSidebar = ref(true);

const filteredWatchlist = computed(() => {
  const q = searchQuery.value.trim().toLowerCase();
  if (!q) return watchlist.value;
  return watchlist.value.filter(
    (f) =>
      (f.callsign ?? "").toLowerCase().includes(q) ||
      (f.origin ?? "").toLowerCase().includes(q) ||
      (f.destination ?? "").toLowerCase().includes(q),
  );
});

// Whether a watchlist entry is currently being tracked live
function isLive(flight) {
  return liveMarkers.value.some(
    (m) =>
      m.callsign === flight.callsign ||
      (m.tailNumber && m.tailNumber === flight.tailNumber),
  );
}

onMounted(() => {
  // Only hit the API when a user is logged in; localStorage is already loaded
  // synchronously above so there is nothing to do for the guest path.
  if (currentUser?.value?.uId) {
    loadTrackedFlights(currentUser)
      .then((items) => {
        watchlist.value = items;
      })
      .catch(() => {
        watchlist.value = [];
      });
  }

  fetchLiveMarkers();
  livePollInterval = setInterval(fetchLiveMarkers, 25000);
});

onUnmounted(() => {
  clearInterval(livePollInterval);
});
</script>

<template>
  <div class="wl-layout">
    <!-- Left sidebar -->
    <div class="wl-sidebar-wrapper" :class="{ collapsed: !showSidebar }">
      <aside class="wl-sidebar">
        <header class="wl-sidebar-header">
          <span class="wl-sidebar-title">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="#f59e0b"
              style="flex-shrink: 0"
            >
              <polygon
                points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
              />
            </svg>
            Watchlist
          </span>
          <span class="wl-count">{{ watchlist.length }}</span>
        </header>

        <div class="wl-search-wrap">
          <input
            v-model="searchQuery"
            class="wl-search"
            placeholder="Search callsign, origin, dest…"
          />
        </div>

        <div class="wl-list">
          <p v-if="watchlist.length === 0" class="wl-empty">
            No flights on your watchlist yet. Go back to the globe and star a
            flight.
          </p>
          <p
            v-else-if="filteredWatchlist.length === 0"
            class="wl-no-results"
          >
            No results for "{{ searchQuery }}"
          </p>
          <div
            v-for="flight in filteredWatchlist"
            :key="flight.callsign"
            class="wl-row"
            :class="{ 'wl-row--selected': selectedFlight?.callsign === flight.callsign }"
            @click="onSelectFlight(flight)"
          >
            <div class="wl-row-main">
              <span class="wl-callsign">{{ flight.callsign }}</span>
              <span v-if="isLive(flight)" class="wl-live-dot" title="Live position">●</span>
            </div>
            <div class="wl-row-route">
              {{ flight.origin || "N/A" }} → {{ flight.destination || "N/A" }}
            </div>
            <div v-if="flight.celebrity" class="wl-celebrity">
              {{ flight.celebrity }}
            </div>
            <button
              class="wl-remove-btn"
              title="Remove from watchlist"
              @click.stop="removeFromWatchlist(flight)"
            >
              ✕
            </button>
          </div>
        </div>
      </aside>

      <button
        class="wl-sidebar-toggle"
        @click="showSidebar = !showSidebar"
      >
        <svg
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
        >
          <polyline
            :points="showSidebar ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"
          />
        </svg>
      </button>
    </div>

    <!-- Main globe area -->
    <div class="wl-map-area">
      <!-- Header -->
      <header class="wl-header">
        <div class="wl-header-left">
          <button class="wl-back-btn" @click="router.push('/')">
            ← Home
          </button>
        </div>
        <div class="wl-header-center">
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="#f59e0b"
            style="flex-shrink: 0"
          >
            <polygon
              points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
            />
          </svg>
          <span class="wl-header-title">My Watchlist</span>
        </div>
        <div class="wl-header-right">
          <span class="wl-ac-count">
            <span class="wl-ac-count-num">{{ displayedFlights.length }}</span>
            <span class="wl-ac-count-label"> tracked</span>
          </span>
        </div>
      </header>

      <!-- Globe -->
      <GlobeContainer
        :flights="displayedFlights"
        :watchlist="watchlist"
        :selected-flight="selectedFlight"
        :destination-cache="{}"
        color-mode="category"
        selected-category=""
        :path-history="[]"
        :show-history="false"
        :weather-data="null"
        :sigmet-data="null"
        active-weather-layer="none"
        @select-flight="onSelectFlight"
        @toggle-watchlist="onToggleWatchlist"
      />

      <!-- Empty state overlay -->
      <div v-if="watchlist.length === 0" class="wl-overlay">
        <svg
          width="40"
          height="40"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#1e3a5f"
          stroke-width="1.5"
          style="margin-bottom: 0.75rem"
        >
          <polygon
            points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
          />
        </svg>
        <p>Your watchlist is empty.</p>
        <button class="wl-back-btn" @click="router.push('/')">
          ← Go add some flights
        </button>
      </div>

      <!-- Flight detail panel -->
      <div v-if="selectedFlight" class="wl-flight-panel">
        <div class="fi-header">
          <span class="fi-callsign">{{ selectedFlight.callsign }}</span>
          <span
            v-if="selectedFlight.category"
            :class="['fi-badge', `fi-badge--${selectedFlight.category}`]"
          >
            {{ selectedFlight.category.toUpperCase() }}
          </span>
          <button class="fi-close" @click="selectedFlight = null">✕</button>
        </div>
        <div class="fi-route">
          {{ selectedFlight.origin }} → {{ selectedFlight.destination }}
        </div>
        <div v-if="selectedFlight.celebrity" class="fi-celebrity-name">
          {{ selectedFlight.celebrity }}
        </div>
        <div class="fi-stats">
          <span>ALT {{ selectedFlight.altitude?.toLocaleString() }} ft</span>
          <span>SPD {{ selectedFlight.velocity }} kts</span>
          <span v-if="selectedFlight.bearing">
            HDG {{ Math.round(selectedFlight.bearing) }}°
          </span>
        </div>
        <div
          v-if="selectedFlight.estimatedCo2Kg != null"
          class="fi-co2"
        >
          ~{{ Math.round(selectedFlight.estimatedCo2Kg).toLocaleString() }} kg
          CO₂
        </div>
        <button
          :class="[
            'fi-watchlist-btn',
            {
              'fi-watchlist-btn--remove': watchlist.some(
                (f) => f.callsign === selectedFlight.callsign,
              ),
            },
          ]"
          @click="onToggleWatchlist(selectedFlight)"
        >
          {{
            watchlist.some((f) => f.callsign === selectedFlight.callsign)
              ? "★ Remove from Watchlist"
              : "☆ Add to Watchlist"
          }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Layout */
.wl-layout {
  display: flex;
  flex-direction: row;
  height: 100vh;
  width: 100%;
  overflow: hidden;
  background: #030a16;
  font-family: Inter, system-ui, sans-serif;
}

/* ── Sidebar ─────────────────────────────────────────── */
.wl-sidebar-wrapper {
  position: relative;
  display: flex;
  flex-direction: row;
  width: 260px;
  flex-shrink: 0;
  transition: width 0.25s ease;
  z-index: 10;
}
.wl-sidebar-wrapper.collapsed {
  width: 0;
}
.wl-sidebar-wrapper.collapsed .wl-sidebar {
  overflow: hidden;
}

.wl-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: #06111f;
  border-right: 1px solid rgba(0, 212, 255, 0.07);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.wl-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.9rem 1rem 0.5rem;
  border-bottom: 1px solid rgba(0, 212, 255, 0.07);
}

.wl-sidebar-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 0.03em;
}

.wl-count {
  background: rgba(245, 158, 11, 0.15);
  border: 1px solid rgba(245, 158, 11, 0.3);
  color: #f59e0b;
  border-radius: 10px;
  font-size: 0.72rem;
  padding: 0.1rem 0.5rem;
  font-weight: 600;
}

.wl-search-wrap {
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid rgba(0, 212, 255, 0.05);
}

.wl-search {
  width: 100%;
  box-sizing: border-box;
  background: rgba(0, 212, 255, 0.04);
  border: 1px solid rgba(0, 212, 255, 0.12);
  border-radius: 6px;
  color: #94a3b8;
  font-size: 0.78rem;
  padding: 0.3rem 0.6rem;
  outline: none;
  font-family: inherit;
}
.wl-search:focus {
  border-color: rgba(0, 212, 255, 0.3);
  color: #e2e8f0;
}

.wl-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.4rem 0;
}
.wl-list::-webkit-scrollbar {
  width: 4px;
}
.wl-list::-webkit-scrollbar-thumb {
  background: rgba(0, 212, 255, 0.12);
  border-radius: 2px;
}

.wl-empty,
.wl-no-results {
  color: #334155;
  font-size: 0.78rem;
  padding: 1.2rem 1rem;
  font-style: italic;
  line-height: 1.5;
}

.wl-row {
  position: relative;
  padding: 0.55rem 1rem;
  cursor: pointer;
  border-bottom: 1px solid rgba(0, 212, 255, 0.04);
  transition: background 0.12s;
}
.wl-row:hover {
  background: rgba(0, 212, 255, 0.05);
}
.wl-row--selected {
  background: rgba(0, 212, 255, 0.09);
  border-left: 2px solid #00d4ff;
}

.wl-row-main {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.15rem;
}

.wl-callsign {
  font-size: 0.82rem;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 0.04em;
}

.wl-live-dot {
  font-size: 0.6rem;
  color: #22c55e;
  line-height: 1;
}

.wl-row-route {
  font-size: 0.75rem;
  color: #475569;
}

.wl-celebrity {
  font-size: 0.72rem;
  color: #f59e0b;
  margin-top: 0.1rem;
}

.wl-remove-btn {
  position: absolute;
  top: 50%;
  right: 0.6rem;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #334155;
  font-size: 0.75rem;
  cursor: pointer;
  padding: 0.2rem 0.35rem;
  border-radius: 4px;
  transition: color 0.12s, background 0.12s;
  line-height: 1;
}
.wl-remove-btn:hover {
  color: #f87171;
  background: rgba(248, 113, 113, 0.1);
}

.wl-sidebar-toggle {
  position: absolute;
  right: -20px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 40px;
  background: #06111f;
  border: 1px solid rgba(0, 212, 255, 0.1);
  border-left: none;
  border-radius: 0 6px 6px 0;
  color: #475569;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
  transition: background 0.15s, color 0.15s;
}
.wl-sidebar-toggle:hover {
  background: #1e3a5f;
  color: #fff;
}

/* ── Map area ────────────────────────────────────────── */
.wl-map-area {
  flex: 1;
  position: relative;
  min-width: 0;
}

/* ── Header ──────────────────────────────────────────── */
.wl-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 36px;
  padding: 0 0.6rem;
  display: flex;
  align-items: center;
  z-index: 1000;
  background: rgba(3, 10, 22, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 212, 255, 0.07);
}

.wl-header-left {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  min-width: 80px;
}
.wl-header-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
}
.wl-header-right {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  min-width: 80px;
  justify-content: flex-end;
}

.wl-header-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 0.04em;
}

.wl-back-btn {
  padding: 0.18rem 0.55rem;
  background: rgba(0, 212, 255, 0.06);
  border: 1px solid rgba(0, 212, 255, 0.18);
  border-radius: 5px;
  color: #64748b;
  font-size: 0.75rem;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}
.wl-back-btn:hover {
  background: rgba(0, 212, 255, 0.12);
  color: #e2e8f0;
}

.wl-ac-count {
  font-size: 0.78rem;
  color: #475569;
}
.wl-ac-count-num {
  color: #00d4ff;
  font-weight: 600;
}

/* ── Empty overlay ───────────────────────────────────── */
.wl-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: rgba(3, 10, 22, 0.75);
  color: #334155;
  font-size: 0.88rem;
  z-index: 50;
  pointer-events: none;
}
.wl-overlay .wl-back-btn {
  pointer-events: all;
  margin-top: 0.5rem;
}
.wl-overlay p {
  margin: 0;
}

/* ── Flight detail panel ─────────────────────────────── */
.wl-flight-panel {
  position: absolute;
  bottom: 1.5rem;
  left: 50%;
  transform: translateX(-50%);
  min-width: 300px;
  max-width: 420px;
  background: rgba(6, 17, 31, 0.92);
  border: 1px solid #1e3a5f;
  border-radius: 12px;
  backdrop-filter: blur(12px);
  z-index: 1000;
  padding: 0.85rem 1rem;
}

.fi-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.fi-callsign {
  font-size: 1rem;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: 0.06em;
}

.fi-badge {
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  letter-spacing: 0.06em;
}
.fi-badge--mil { background: rgba(255, 68, 68, 0.15); color: #ff4444; border: 1px solid rgba(255, 68, 68, 0.3); }
.fi-badge--ladd { background: rgba(245, 158, 11, 0.15); color: #f59e0b; border: 1px solid rgba(245, 158, 11, 0.3); }
.fi-badge--pia { background: rgba(168, 85, 247, 0.15); color: #a855f7; border: 1px solid rgba(168, 85, 247, 0.3); }
.fi-badge--celebrity { background: rgba(251, 191, 36, 0.15); color: #fbbf24; border: 1px solid rgba(251, 191, 36, 0.3); }

.fi-close {
  margin-left: auto;
  background: none;
  border: none;
  color: #334155;
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0.1rem 0.3rem;
  transition: color 0.12s;
}
.fi-close:hover { color: #f87171; }

.fi-route {
  font-size: 0.8rem;
  color: #475569;
  margin-bottom: 0.2rem;
}

.fi-celebrity-name {
  font-size: 0.78rem;
  color: #f59e0b;
  margin-bottom: 0.2rem;
}

.fi-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.78rem;
  color: #64748b;
  margin-top: 0.3rem;
}

.fi-co2 {
  margin-top: 0.35rem;
  font-size: 0.78rem;
  color: #22c55e;
}

.fi-watchlist-btn {
  margin-top: 0.6rem;
  width: 100%;
  padding: 0.3rem 0.75rem;
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.3);
  border-radius: 6px;
  color: #00d4ff;
  font-size: 0.78rem;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s;
}
.fi-watchlist-btn:hover { background: rgba(0, 212, 255, 0.15); }
.fi-watchlist-btn--remove {
  background: rgba(248, 113, 113, 0.08);
  border-color: rgba(248, 113, 113, 0.3);
  color: #f87171;
}
.fi-watchlist-btn--remove:hover { background: rgba(248, 113, 113, 0.15); }
</style>
