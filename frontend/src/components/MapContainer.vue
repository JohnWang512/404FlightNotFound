<script setup>
/**
 * MapContainer — 2D Leaflet map alternative to the 3D globe.
 *
 * Markers are cached in `markerMap` (keyed by callsign) so position/icon updates
 * only patch existing Leaflet markers instead of recreating them on every poll cycle.
 * `polylineMap` stores optional route lines keyed the same way.
 *
 * Watchlist button clicks inside Leaflet popups are handled via a single delegated
 * listener on the map container rather than attaching a new listener each time a
 * popup opens — this prevents duplicate handlers stacking up over time.
 */
import { onMounted, onUnmounted, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  flights: { type: Array, required: true },
  watchlist: { type: Array, required: true },
  selectedFlight: { type: Object, default: null },
  colorMode: { type: String, default: 'category' },
})

const emit = defineEmits(['select-flight', 'toggle-watchlist'])

const mapEl = ref(null)
let mapInstance = null
const markerMap = {}
const polylineMap = {}


const CAT_COLORS = {
  mil:       '#ff4444',
  ladd:      '#f59e0b',
  pia:       '#a855f7',
  celebrity: '#fbbf24',
  default:   '#00d4ff',
}

function flightColor(flight) {
  return CAT_COLORS[flight.category] || CAT_COLORS.default
}

function flightColorByMode(flight) {
  if (props.colorMode === 'altitude') {
    const alt = flight.altitude ?? 0
    if (alt < 10000) return '#4ade80'
    if (alt < 30000) return '#f59e0b'
    return '#ff4444'
  }
  if (props.colorMode === 'speed') {
    const spd = flight.velocity ?? 0
    if (spd < 200) return '#4ade80'
    if (spd < 450) return '#f59e0b'
    return '#ff4444'
  }
  return flightColor(flight)
}

function isWatchlisted(callsign) {
  return props.watchlist.some(f => f.callsign === callsign)
}

// Top-down commercial plane SVG
function makeCommercialSvg(color, bearing, size) {
  const rot = Number.isFinite(bearing) ? bearing : 0
  return `
    <svg width="${size}" height="${size}" viewBox="0 0 36 36" xmlns="http://www.w3.org/2000/svg"
         style="transform:rotate(${rot}deg);display:block;">
      <rect x="16.5" y="3" width="3" height="22" rx="1.5" fill="${color}"/>
      <path d="M18 13 L3 21 L3 24 L18 19 L33 24 L33 21 Z" fill="${color}"/>
      <path d="M18 27 L11 31 L11 33 L18 31 L25 33 L25 31 Z" fill="${color}"/>
    </svg>`
}

// Angular delta-wing fighter for military
function makeMilitarySvg(color, bearing, size) {
  const rot = Number.isFinite(bearing) ? bearing : 0
  return `
    <svg width="${size}" height="${size}" viewBox="0 0 36 36" xmlns="http://www.w3.org/2000/svg"
         style="transform:rotate(${rot}deg);display:block;">
      <path d="M18 3 L30 30 L18 25 L6 30 Z" fill="${color}"/>
      <path d="M13 25 L9 33 L18 30 L27 33 L23 25 Z" fill="${color}"/>
    </svg>`
}

function makeIcon(flight, selected) {
  const color = flightColorByMode(flight)
  const bearing = flight.bearing
  const size = selected ? 44 : 32
  const isMil = flight.category === 'mil'
  const svgBody = isMil
    ? makeMilitarySvg(color, bearing, size)
    : makeCommercialSvg(color, bearing, size)

  const ring = selected
    ? `<div class="target-ring" style="border-color:${color};"></div>`
    : ''

  const watched = isWatchlisted(flight.callsign)
  const watchedDot = watched
    ? `<div class="watched-dot"></div>`
    : ''

  return L.divIcon({
    className: 'plane-marker-wrap',
    html: `<div class="plane-marker${selected ? ' plane-marker--selected' : ''}"
                style="--plane-color:${color}">
             ${ring}
             ${svgBody}
             ${watchedDot}
           </div>`,
    iconSize: [size + 16, size + 16],
    iconAnchor: [(size + 16) / 2, (size + 16) / 2],
    popupAnchor: [0, -(size / 2 + 8)],
  })
}

function makePopupContent(flight) {
  const watched = isWatchlisted(flight.callsign)
  const color = flightColorByMode(flight)
  const catLabel = flight.category ? flight.category.toUpperCase() : ''
  return `
    <div class="custom-popup">
      <div class="cp-header">
        <strong style="color:${color}">${flight.callsign}</strong>
        ${catLabel ? `<span class="cp-badge" style="color:${color};border-color:${color}">${catLabel}</span>` : ''}
      </div>
      <div class="cp-route" title="Route data from AeroAPI — may be unavailable for some aircraft">${flight.origin} → ${flight.destination}</div>
      <div class="cp-stats">
        <span>ALT ${flight.altitude?.toLocaleString()} ft</span>
        <span>SPD ${flight.velocity} kts</span>
        ${Number.isFinite(flight.bearing) ? `<span>HDG ${Math.round(flight.bearing)}°</span>` : ''}
      </div>
      <button class="watchlist-btn ${watched ? 'watchlist-btn--remove' : ''}"
              data-callsign="${flight.callsign}">
        ${watched ? '★ Remove' : '☆ Track'}
      </button>
    </div>`
}

function addMarkers(flights) {
  if (!mapInstance) return

  const incomingKeys = new Set(flights.map(f => f.callsign))

  // Remove stale markers and paths
  Object.keys(markerMap).forEach(key => {
    if (!incomingKeys.has(key)) {
      mapInstance.removeLayer(markerMap[key])
      delete markerMap[key]
      if (polylineMap[key]) { mapInstance.removeLayer(polylineMap[key]); delete polylineMap[key] }
    }
  })

  flights.forEach((flight) => {
    const isSelected = props.selectedFlight?.callsign === flight.callsign

    if (markerMap[flight.callsign]) {
      // Update existing marker position + icon
      markerMap[flight.callsign].setLatLng([flight.lat, flight.lng])
      markerMap[flight.callsign].setIcon(makeIcon(flight, isSelected))
    } else {
      // New marker
      const marker = L.marker([flight.lat, flight.lng], { icon: makeIcon(flight, isSelected) })
        .addTo(mapInstance)
        .bindPopup(makePopupContent(flight), { className: 'dark-popup' })

      marker.on('click', () => emit('select-flight', flight))

      marker.on('popupopen', () => {
        marker.setPopupContent(makePopupContent(flight))
      })

      markerMap[flight.callsign] = marker
    }
  })
}

onMounted(() => {
  mapInstance = L.map(mapEl.value, { zoomControl: false }).setView([39.5, -98.35], 4)

  // Dark tactical tile layer
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap &copy; CARTO',
    subdomains: 'abcd',
    maxZoom: 19,
  }).addTo(mapInstance)

  // Custom zoom position
  L.control.zoom({ position: 'bottomright' }).addTo(mapInstance)

  addMarkers(props.flights)
  mapInstance.invalidateSize()

  // Delegated listener for watchlist buttons inside Leaflet popups.
  // A single listener on the container avoids stacking duplicate handlers
  // every time a popup reopens or its content is refreshed.
  mapEl.value.addEventListener('click', (e) => {
    const btn = e.target.closest('.watchlist-btn')
    if (!btn) return
    const callsign = btn.dataset.callsign
    const flight = props.flights.find(f => f.callsign === callsign)
    if (flight) {
      emit('toggle-watchlist', flight)
      // Refresh popup content so the button label flips immediately
      const marker = markerMap[callsign]
      if (marker) setTimeout(() => marker.setPopupContent(makePopupContent(flight)), 0)
    }
  })
})

watch(
  () => props.flights,
  (newFlights) => { if (mapInstance) addMarkers(newFlights) },
  { deep: true }
)

watch(
  () => props.colorMode,
  () => { if (mapInstance) addMarkers(props.flights) }
)

watch(
  () => props.selectedFlight,
  (newFlight, oldFlight) => {
    if (!mapInstance) return
    // Re-render old and new markers with correct selected state
    if (oldFlight && markerMap[oldFlight.callsign]) {
      const f = props.flights.find(x => x.callsign === oldFlight.callsign) || oldFlight
      markerMap[oldFlight.callsign].setIcon(makeIcon(f, false))
    }
    if (newFlight && markerMap[newFlight.callsign]) {
      markerMap[newFlight.callsign].setIcon(makeIcon(newFlight, true))
      mapInstance.flyTo([newFlight.lat, newFlight.lng], 6, { duration: 0.8 })
    }
  }
)

onUnmounted(() => {
  if (mapInstance) { mapInstance.remove(); mapInstance = null }
})
</script>

<template>
  <div class="map-container">
    <div ref="mapEl" class="map" />

    <!-- Color legend overlay -->
    <div class="color-legend">
      <template v-if="colorMode === 'category'">
        <div class="legend-title">Category</div>
        <div class="legend-row"><span class="legend-dot" style="background:#00d4ff"></span><span>Commercial</span></div>
        <div class="legend-row"><span class="legend-dot" style="background:#ff4444"></span><span>Military</span></div>
        <div class="legend-row"><span class="legend-dot" style="background:#f59e0b"></span><span>LADD</span></div>
        <div class="legend-row"><span class="legend-dot" style="background:#a855f7"></span><span>PIA</span></div>
        <div class="legend-row"><span class="legend-dot" style="background:#fbbf24"></span><span>Celebrity</span></div>
      </template>
      <template v-else-if="colorMode === 'altitude'">
        <div class="legend-title">Altitude</div>
        <div class="legend-row"><span class="legend-dot" style="background:#4ade80"></span><span>&lt; 10,000 ft</span></div>
        <div class="legend-row"><span class="legend-dot" style="background:#f59e0b"></span><span>10k – 30k ft</span></div>
        <div class="legend-row"><span class="legend-dot" style="background:#ff4444"></span><span>&gt; 30,000 ft</span></div>
      </template>
      <template v-else-if="colorMode === 'speed'">
        <div class="legend-title">Speed</div>
        <div class="legend-row"><span class="legend-dot" style="background:#4ade80"></span><span>&lt; 200 kts</span></div>
        <div class="legend-row"><span class="legend-dot" style="background:#f59e0b"></span><span>200 – 450 kts</span></div>
        <div class="legend-row"><span class="legend-dot" style="background:#ff4444"></span><span>&gt; 450 kts</span></div>
      </template>
    </div>
  </div>
</template>

<style>
/* Global so Leaflet popup receives styles */
.plane-marker-wrap { background: none !important; border: none !important; }

/* Ensure popups always render above the dense marker layer */
.leaflet-popup-pane { z-index: 800 !important; }

.plane-marker {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.target-ring {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 1.5px solid var(--plane-color, #00d4ff);
  animation: ring-pulse 1.8s ease-in-out infinite;
  pointer-events: none;
}

@keyframes ring-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%       { opacity: 0.5; transform: scale(1.12); }
}

.watched-dot {
  position: absolute;
  top: -3px;
  right: -3px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f59e0b;
  border: 1px solid #000;
}

/* Popup */
.dark-popup .leaflet-popup-content-wrapper {
  background: rgba(4, 15, 30, 0.95);
  border: 1px solid #1a3a5c;
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.15);
  color: #e2e8f0;
}
.dark-popup .leaflet-popup-tip { background: rgba(4,15,30,0.95); }
.dark-popup .leaflet-popup-close-button { color: #475569 !important; }

.custom-popup { font-family: 'Inter', system-ui, sans-serif; font-size: 13px; line-height: 1.5; }
.cp-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.cp-badge {
  font-size: 0.6rem; font-weight: 700; padding: 1px 5px;
  border-radius: 3px; border: 1px solid; opacity: 0.9;
}
.cp-route { color: #94a3b8; font-size: 0.8rem; margin-bottom: 0.3rem; }
.cp-stats { display: flex; gap: 0.75rem; font-size: 0.75rem; color: #64748b; margin-bottom: 0.5rem; }

.watchlist-btn {
  width: 100%; padding: 4px 10px;
  background: rgba(0,212,255,0.08); border: 1px solid rgba(0,212,255,0.3);
  border-radius: 5px; color: #00d4ff; font-size: 11px; cursor: pointer;
  transition: background 0.15s;
}
.watchlist-btn:hover { background: rgba(0,212,255,0.15); }
.watchlist-btn--remove { border-color: rgba(248,113,113,0.4); color: #f87171; background: rgba(248,113,113,0.08); }
.watchlist-btn--remove:hover { background: rgba(248,113,113,0.15); }
</style>

<style scoped>
.map-container { flex: 1; height: 100vh; min-width: 0; position: relative; }
.map { height: 100%; width: 100%; }

.color-legend {
  position: absolute;
  bottom: 60px;
  left: 14px;
  background: rgba(3,10,22,0.82);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(0,212,255,0.12);
  border-radius: 8px;
  padding: 8px 10px;
  font-family: Inter, system-ui, sans-serif;
  font-size: 11px;
  color: #94a3b8;
  display: flex;
  flex-direction: column;
  gap: 4px;
  pointer-events: none;
  z-index: 500;
}

.legend-title {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #475569;
  margin-bottom: 2px;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
