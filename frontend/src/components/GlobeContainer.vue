<script setup>
/**
 * GlobeContainer — renders the interactive 3D globe using globe.gl (Three.js under the hood).
 * Displays live flight markers as HTML plane icons, animated arcs to destinations,
 * and optional flight path history trails.
 *
 * All state lives in HomeView; this component is purely presentational and
 * communicates back via select-flight / toggle-watchlist events.
 */
import { ref, onMounted, onUnmounted, watch } from "vue";
import Globe from "globe.gl";
import * as THREE from "three";

const props = defineProps({
  flights: { type: Array, required: true },
  visibleCallsigns: { type: Array, default: () => [] },
  useVisibilityFilter: { type: Boolean, default: false },
  watchlist: { type: Array, default: () => [] },
  selectedFlight: { type: Object, default: null },
  destinationCache: { type: Object, default: () => ({}) },
  weatherData: { type: Object, default: null },
  sigmetData: { type: Object, default: null },
  activeWeatherLayer: { type: String, default: "none" },

  colorMode: { type: String, default: "category" },
  selectedCategory: { type: String, default: "" },
  pathHistory: { type: Array, default: () => [] },
  showHistory: { type: Boolean, default: false },
  nearbyAirports: { type: Array, default: () => [] },
  airportVisualizationMode: { type: Boolean, default: false },
  celebOnlyMode: { type: Boolean, default: false },
  watchlistOnly: { type: Boolean, default: false },
  airportsOnlyMode: { type: Boolean, default: false },
  allAirports: { type: Array, default: () => [] },
});

const emit = defineEmits(["select-flight", "toggle-watchlist"]);

const globeEl = ref(null);
let globeInstance = null;
let radarMesh = null;
let radarInterval = null;
const planeElementMap = new Map();

// Static demo celebrity flights injected client-side so the globe always shows
// celebrity markers even when the live API returns no data for that category.
// Live data takes priority: if the same callsign arrives from the backend it
// replaces the demo entry (see applyData merge logic below).
const DEMO_CELEB_FLIGHTS = [
  {
    callsign: "N621MM",
    tailNumber: "N621MM",
    celebrity: "Taylor Swift",
    lat: 34.05,
    lng: -118.24,
    bearing: 60,
    altitude: 41000,
    velocity: 480,
    category: "celebrity",
    origin: "LAX",
    destination: "JFK",
  },
  {
    callsign: "XA-UUS",
    tailNumber: "XA-UUS",
    celebrity: "Bad Bunny",
    lat: 19.43,
    lng: -99.13,
    bearing: 200,
    altitude: 35000,
    velocity: 430,
    category: "celebrity",
    origin: "MEX",
    destination: "CUN",
  },
  {
    callsign: "G-CEYL",
    tailNumber: "G-CEYL",
    celebrity: "Ed Sheeran",
    lat: 51.51,
    lng: -0.12,
    bearing: 130,
    altitude: 39000,
    velocity: 460,
    category: "celebrity",
    origin: "LHR",
    destination: "CDG",
  },
  {
    callsign: "F-HJDV",
    tailNumber: "F-HJDV",
    celebrity: "Kylie Jenner",
    lat: 48.86,
    lng: 2.35,
    bearing: 270,
    altitude: 37000,
    velocity: 445,
    category: "celebrity",
    origin: "CDG",
    destination: "BCN",
  },
  {
    callsign: "JA8091",
    tailNumber: "JA8091",
    celebrity: "BTS",
    lat: 35.68,
    lng: 139.69,
    bearing: 90,
    altitude: 43000,
    velocity: 510,
    category: "celebrity",
    origin: "HND",
    destination: "ICN",
  },
];

const CAT_COLORS = {
  commercial: "#00d4ff",
  mil: "#ff4444",
  ladd: "#f59e0b",
  pia: "#a855f7",
  celebrity: "#fbbf24",
  default: "#00d4ff",
};

function flightColor(f) {
  return CAT_COLORS[f.category] || CAT_COLORS.default;
}

function flightColorByMode(f) {
  if (props.colorMode === "altitude") {
    const alt = f.altitude ?? 0;
    if (alt < 10000) return "#4ade80";
    if (alt < 30000) return "#f59e0b";
    return "#ff4444";
  }
  if (props.colorMode === "speed") {
    const spd = f.velocity ?? 0;
    if (spd < 200) return "#4ade80";
    if (spd < 450) return "#f59e0b";
    return "#ff4444";
  }
  return flightColor(f);
}

// Top-down commercial plane SVG — same shape for all, different colors
function planeSvg(color, bearing, size, glow) {
  const rot = Number.isFinite(bearing) ? bearing : 0;
  const filter = glow ? `filter:drop-shadow(0 0 6px ${color});` : "";
  return `
    <svg width="${size}" height="${size}" viewBox="0 0 36 36" xmlns="http://www.w3.org/2000/svg"
         style="transform:rotate(${rot}deg);display:block;${filter}">
      <rect x="16.5" y="3" width="3" height="22" rx="1.5" fill="${color}"/>
      <path d="M18 13 L3 21 L3 24 L18 19 L33 24 L33 21 Z" fill="${color}"/>
      <path d="M18 27 L11 31 L11 33 L18 31 L25 33 L25 31 Z" fill="${color}"/>
    </svg>`;
}

// Builds a small DOM pin for an airport endpoint (origin = green, destination = amber).
function makeAirportEl(airport) {
  const color = airport.isOrigin ? "#4ade80" : "#f59e0b";
  const el = document.createElement("div");
  el.style.cssText = `
    display: flex;
    flex-direction: column;
    align-items: center;
    pointer-events: none;
    transform: translate(-50%, -50%);
  `;
  el.innerHTML = `
    <div style="
      width: 10px; height: 10px; border-radius: 50%;
      background: ${color};
      border: 2px solid rgba(255,255,255,0.55);
      box-shadow: 0 0 7px ${color};
    "></div>
    <div style="
      font-size: 9px; font-weight: 700; color: ${color};
      font-family: Inter, system-ui, sans-serif;
      text-shadow: 0 1px 3px rgba(0,0,0,0.9);
      margin-top: 2px; white-space: nowrap; line-height: 1;
    ">${airport.code}</div>
  `;
  return el;
}

// Builds a prominent cyan DOM pin for a nearby airport in visualization mode.
function makeNearbyAirportEl(airport) {
  const el = document.createElement("div");
  el.style.cssText = `
    display: flex;
    flex-direction: column;
    align-items: center;
    pointer-events: none;
    transform: translate(-50%, -50%);
  `;
  el.innerHTML = `
    <div style="
      width: 14px; height: 14px; border-radius: 50%;
      background: #00d4ff;
      border: 2px solid rgba(255,255,255,0.75);
      box-shadow: 0 0 14px #00d4ff, 0 0 6px #00d4ff;
    "></div>
    <div style="
      font-size: 9px; font-weight: 700; color: #00d4ff;
      font-family: Inter, system-ui, sans-serif;
      text-shadow: 0 1px 4px rgba(0,0,0,0.95);
      margin-top: 3px; white-space: nowrap; line-height: 1;
    ">${airport.code}</div>
    <div style="
      font-size: 8px; color: rgba(0,212,255,0.75);
      font-family: Inter, system-ui, sans-serif;
      text-shadow: 0 1px 3px rgba(0,0,0,0.9);
      white-space: nowrap; line-height: 1.2;
    ">${airport.distanceMiles} mi</div>
  `;
  return el;
}

// Collects unique origin + destination airport markers for all flights whose
// coordinates are already resolved in destinationCache.
function buildAirportMarkers(flights) {
  const seen = new Set();
  const markers = [];
  for (const f of flights) {
    for (const [code, isOrigin] of [
      [f.origin, true],
      [f.destination, false],
    ]) {
      if (!code || code === "N/A" || code === "???") continue;
      if (seen.has(code)) continue;
      const coords = props.destinationCache[code];
      if (!coords?.lat || !coords?.lng) continue;
      seen.add(code);
      markers.push({
        code,
        lat: coords.lat,
        lng: coords.lng,
        isOrigin,
        isAirportMarker: true,
      });
    }
  }
  return markers;
}

// Builds a DOM element for each flight marker. globe.gl re-calls this whenever
// htmlElementsData changes, so we fully reconstruct the element each time rather
// than patching it in place — simpler and avoids stale closure issues.
function makePlaneEl(flight) {
  const existing = planeElementMap.get(flight.callsign);
  const isSelected = props.selectedFlight?.callsign === flight.callsign;
  const color = flightColorByMode(flight);
  const size = isSelected ? 42 : 30;

  const el = existing ?? document.createElement("div");
  el.style.cssText = `
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    pointer-events: auto;
    width: ${size + 12}px;
    height: ${size + 12}px;
  `;

  if (isSelected) {
    el.innerHTML = `
      <div style="
        position:absolute;inset:0;border-radius:50%;
        border:1.5px solid ${color};
        animation:globe-ring-pulse 1.8s ease-in-out infinite;
        pointer-events:none;
      "></div>
      ${planeSvg(color, flight.bearing, size, true)}
    `;
  } else {
    el.innerHTML = planeSvg(color, flight.bearing, size, false);
  }

  if (!existing) {
    // Use mousedown instead of click to avoid globe drag interference
    el.addEventListener("pointerdown", (e) => {
      e.stopPropagation();
    });
    el.addEventListener("click", (e) => {
      e.stopPropagation();
      e.preventDefault();
      emit("select-flight", flight);
    });
  }

  planeElementMap.set(flight.callsign, el);

  return el;
}

function applyVisibility() {
  if (!globeInstance) return;
  // Airports-only mode: hide every plane unconditionally.
  if (props.airportsOnlyMode) {
    for (const el of planeElementMap.values()) {
      el.style.visibility = "hidden";
      el.style.pointerEvents = "none";
    }
    return;
  }
  const visible = new Set(props.visibleCallsigns);
  // Demos bypass the visibility filter in normal mode, but NOT when an exclusive
  // mode (watchlist or celeb-only) is active — those modes must be strict.
  const exclusiveMode = props.watchlistOnly || props.celebOnlyMode || props.airportVisualizationMode;
  const showDemos = !exclusiveMode &&
    (props.selectedCategory === "" || props.selectedCategory === "celebrity");
  const demoCallsigns = new Set(DEMO_CELEB_FLIGHTS.map((d) => d.callsign));
  for (const [callsign, el] of planeElementMap.entries()) {
    const isVisible =
      !props.useVisibilityFilter ||
      visible.has(callsign) ||
      (showDemos && demoCallsigns.has(callsign));
    el.style.visibility = isVisible ? "visible" : "hidden";
    el.style.pointerEvents = isVisible ? "auto" : "none";
  }
}

function updateSelectionStyles(newSelected, oldSelected) {
  if (!globeInstance) return;
  if (oldSelected && planeElementMap.has(oldSelected.callsign)) {
    const oldFlight = props.flights.find((f) => f.callsign === oldSelected.callsign);
    if (oldFlight) {
      makePlaneEl(oldFlight);
    }
  }
  if (newSelected && planeElementMap.has(newSelected.callsign)) {
    const newFlight = props.flights.find((f) => f.callsign === newSelected.callsign) ?? newSelected;
    makePlaneEl(newFlight);
  }
}

// Builds animated arc data for flights that have a known destination coordinate.
// Flights without a resolved destination are silently skipped.
function buildArcs(flights) {
  return flights.flatMap((f) => {
    const dest = props.destinationCache[f.destination];
    if (!dest?.lat || !dest?.lng) return [];
    return [
      {
        startLat: f.lat,
        startLng: f.lng,
        endLat: dest.lat,
        endLng: dest.lng,
        color: flightColorByMode(f),
      },
    ];
  });
}

function applyData(flights) {
  if (!globeInstance) return;

  const liveCallsigns = new Set(flights.map((f) => f.callsign));
  let demosToAdd;
  if (props.celebOnlyMode) {
    // Only show demo flights that belong to a tracked celebrity (callsign is in visibleCallsigns)
    const visible = new Set(props.visibleCallsigns);
    demosToAdd = DEMO_CELEB_FLIGHTS.filter(
      (d) => !liveCallsigns.has(d.callsign) && visible.has(d.callsign)
    );
  } else if (props.watchlistOnly) {
    // Watchlist mode: never show demo celebrity flights (they're not on the watchlist)
    demosToAdd = [];
  } else {
    const showDemos =
      props.selectedCategory === "" || props.selectedCategory === "celebrity";
    demosToAdd = showDemos
      ? DEMO_CELEB_FLIGHTS.filter((d) => !liveCallsigns.has(d.callsign))
      : [];
  }
  const merged = [...flights, ...demosToAdd];

  const mergedKeys = new Set(merged.map((f) => f.callsign));
  for (const callsign of planeElementMap.keys()) {
    if (!mergedKeys.has(callsign)) {
      planeElementMap.delete(callsign);
    }
  }

  let airportMarkers, nearbyMarkers;
  if (props.airportsOnlyMode) {
    airportMarkers = props.allAirports.map((a) => ({ ...a, isAirportMarker: true }));
    nearbyMarkers = [];
  } else if (props.airportVisualizationMode) {
    airportMarkers = [];
    nearbyMarkers = props.nearbyAirports.map((a) => ({ ...a, isNearbyAirport: true, isAirportMarker: true }));
  } else {
    airportMarkers = buildAirportMarkers(merged);
    nearbyMarkers = [];
  }

  globeInstance
    .arcsData(props.airportsOnlyMode || props.airportVisualizationMode ? [] : buildArcs(merged))
    .htmlElementsData([...merged, ...airportMarkers, ...nearbyMarkers]);
  applyVisibility();
}

let resizeObserver = null;

const ringToPath = (ring, alt = 0.002) =>
  ring.map((coord) => ({ lat: coord[1], lng: coord[0], alt }));

// ==========================================
// MASTER WEATHER RENDERER
// ==========================================
function renderAllWeatherPolygons() {
  if (!globeInstance) return;

  globeInstance.polygonsData([]);

  let paths = [];

  if (props.activeWeatherLayer === "sigmet" && props.sigmetData?.features) {
    const getHazardColor = (feature) => {
      if (!feature?.properties) return "rgba(239, 68, 68, 0.6)";
      const p = JSON.stringify(feature.properties).toUpperCase();
      if (p.includes("ASH") || p.includes("VOLCANIC"))
        return "rgba(168, 85, 247, 0.6)";
      if (p.includes("ICE") || p.includes("ICING"))
        return "rgba(56, 189, 248, 0.6)";
      if (p.includes("TURB")) return "rgba(249, 115, 22, 0.6)";
      return "rgba(239, 68, 68, 0.6)";
    };

    props.sigmetData.features.forEach((feature) => {
      if (!feature.geometry) return;
      const color = getHazardColor(feature);
      if (feature.geometry.type === "Polygon") {
        feature.geometry.coordinates.forEach((ring) =>
          paths.push({ points: ringToPath(ring), color }),
        );
      } else if (feature.geometry.type === "MultiPolygon") {
        feature.geometry.coordinates.forEach((poly) =>
          poly.forEach((ring) =>
            paths.push({ points: ringToPath(ring), color }),
          ),
        );
      }
    });
  }

  globeInstance
    .pathsData(paths)
    .pathPoints((d) => d.points)
    .pathPointLat((p) => p.lat)
    .pathPointLng((p) => p.lng)
    .pathPointAlt((p) => p.alt)
    .pathColor((d) => d.color)
    .pathStroke(2.5)
    .pathDashLength(0)
    .pathDashGap(0)
    .pathDashAnimateTime(0);
}

// Fetches the latest RainViewer frame URL and updates the radar sphere texture.
async function refreshRadarTexture() {
  if (!radarMesh) return;
  try {
    const res = await fetch(
      "https://api.rainviewer.com/public/weather-maps.json",
    );
    const data = await res.json();
    const latestPath = data.radar.past[data.radar.past.length - 1].path;
    const url = `${data.host}${latestPath}/512/0/0/0/2/1_1.png`;
    radarMesh.material.map = new THREE.TextureLoader().load(url);
    radarMesh.material.needsUpdate = true;
  } catch (e) {
    console.warn("RainViewer refresh failed", e);
  }
}

watch(
  [
    () => props.weatherData,
    () => props.sigmetData,
    () => props.activeWeatherLayer,
  ],
  async ([newWeather], [oldWeather]) => {
    renderAllWeatherPolygons();
    applyData(props.flights);

    if (props.activeWeatherLayer === "radar") {
      if (!radarMesh) {
        const radius = globeInstance.getGlobeRadius() * 1.012;
        const geometry = new THREE.SphereGeometry(radius, 64, 64);
        const material = new THREE.MeshBasicMaterial({
          transparent: true,
          opacity: 0.7,
          depthWrite: false,
        });
        radarMesh = new THREE.Mesh(geometry, material);
        globeInstance.scene().add(radarMesh);
      }
      radarMesh.visible = true;
      await refreshRadarTexture();
      // Only start the interval once — guard against watcher firing multiple times
      if (!radarInterval) {
        radarInterval = setInterval(refreshRadarTexture, 5 * 60 * 1000);
      }
    } else {
      clearInterval(radarInterval);
      radarInterval = null;
      if (radarMesh) radarMesh.visible = false;
    }

    if (newWeather && newWeather !== oldWeather) {
      globeInstance.pointOfView(
        { lat: newWeather.lat, lng: newWeather.lng, altitude: 0.15 },
        1500,
      );
    }
  },
);

// Renders the selected aircraft's historical flight path as an animated trail.
// An empty array clears the path when history is hidden or unavailable.
function applyPath() {
  if (!globeInstance) return;
  const pathData =
    props.showHistory && props.pathHistory.length > 0
      ? [{ points: props.pathHistory }]
      : [];
  globeInstance.pathsData(pathData);
}

onMounted(() => {
  globeInstance = Globe({ animateIn: true })(globeEl.value)
    .globeImageUrl("//unpkg.com/three-globe/example/img/earth-night.jpg")
    .backgroundImageUrl("//unpkg.com/three-globe/example/img/night-sky.png")
    .showAtmosphere(true)
    .atmosphereColor("#0099ff")
    .atmosphereAltitude(0.15)
    .arcColor("color")
    .arcStroke(0.35)
    .arcDashLength(0.4)
    .arcDashGap(0.15)
    .arcDashAnimateTime(2500)
    .arcAltitudeAutoScale(0.25)
    .htmlElement((d) => {
      // If it's our new weather marker, draw a floating emoji
      if (d.isWeatherMarker) {
        const el = document.createElement("div");
        el.innerHTML = d.emoji;
        el.style.cssText = `
          font-size: 28px;
          text-shadow: 0px 4px 10px rgba(0,0,0,1);
          pointer-events: none;
          transform: translate(-15px, -35px);
        `;
        return el;
      }
      if (d.isNearbyAirport) return makeNearbyAirportEl(d);
      if (d.isAirportMarker) return makeAirportEl(d);
      // Otherwise, draw the plane
      return makePlaneEl(d);
    })
    .htmlLat((d) => d.lat)
    .htmlLng((d) => d.lng)
    .htmlAltitude(0.01)
    .pathsData([])
    .pathPoints((d) => d.points)
    .pathPointLat((p) => p.lat)
    .pathPointLng((p) => p.lng)
    .pathPointAlt(() => 0.005)
    .pathColor(() => ["rgba(0,212,255,0.9)", "rgba(0,212,255,0.1)"])
    .pathStroke(1.5)
    .pathDashLength(0.05)
    .pathDashGap(0.005)
    .pathDashAnimateTime(20000);

  const resize = () => {
    const w = globeEl.value.clientWidth;
    const h = globeEl.value.clientHeight;
    if (w > 0 && h > 0) globeInstance.width(w).height(h);
  };

  resize();
  globeInstance.pointOfView({ lat: 39.5, lng: -98.35, altitude: 2.2 });

  resizeObserver = new ResizeObserver(resize);
  resizeObserver.observe(globeEl.value);

  applyData(props.flights);
  applyPath();
});

watch(
  () => props.flights,
  (f) => applyData(f),
  { deep: true },
);
watch(
  () => props.visibleCallsigns,
  () => applyVisibility(),
  { deep: true },
);
watch(
  () => props.colorMode,
  () => applyData(props.flights),
);
watch(
  () => props.selectedCategory,
  () => applyData(props.flights),
);
watch(() => props.showHistory, applyPath);
watch(() => props.pathHistory, applyPath, { deep: true });
watch(
  [() => props.nearbyAirports, () => props.airportVisualizationMode, () => props.celebOnlyMode, () => props.watchlistOnly, () => props.airportsOnlyMode],
  () => applyData(props.flights),
  { deep: true },
);

watch(
  () => props.selectedFlight,
  (newSelected, oldSelected) => {
    updateSelectionStyles(newSelected, oldSelected);
    const f = newSelected;
    if (f)
      globeInstance?.pointOfView(
        { lat: f.lat, lng: f.lng, altitude: 1.4 },
        700,
      );
  },
);

onUnmounted(() => {
  clearInterval(radarInterval);
  resizeObserver?.disconnect();
  if (globeInstance) {
    globeInstance._destructor?.();
    globeInstance = null;
  }
});
</script>

<template>
  <div class="globe-root">
    <div class="globe-wrapper">
      <div ref="globeEl" class="globe-canvas" />
    </div>

    <!-- Color legend overlay — outside globe-wrapper so it is never buried under aircraft markers -->
    <div class="color-legend">
      <template v-if="colorMode === 'category'">
        <div class="legend-title">Category</div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #00d4ff"></span
          ><span>Commercial</span>
        </div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #ff4444"></span
          ><span>Military</span>
        </div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #f59e0b"></span
          ><span>LADD</span>
        </div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #a855f7"></span
          ><span>PIA</span>
        </div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #fbbf24"></span
          ><span>Celebrity</span>
        </div>
      </template>
      <template v-else-if="colorMode === 'altitude'">
        <div class="legend-title">Altitude</div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #4ade80"></span
          ><span>&lt; 10,000 ft</span>
        </div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #f59e0b"></span
          ><span>10k – 30k ft</span>
        </div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #ff4444"></span
          ><span>&gt; 30,000 ft</span>
        </div>
      </template>
      <template v-else-if="colorMode === 'speed'">
        <div class="legend-title">Speed</div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #4ade80"></span
          ><span>&lt; 200 kts</span>
        </div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #f59e0b"></span
          ><span>200 – 450 kts</span>
        </div>
        <div class="legend-row">
          <span class="legend-dot" style="background: #ff4444"></span
          ><span>&gt; 450 kts</span>
        </div>
      </template>
    </div>
  </div>
</template>


<style scoped>
.globe-root {
  width: 100%;
  height: 100%;
  position: relative;
}
.globe-wrapper {
  width: 100%;
  height: 100%;
  background: #000;
  position: relative;
  isolation: isolate;
}
.globe-canvas {
  width: 100%;
  height: 100%;
}

.color-legend {
  position: absolute;
  bottom: 60px;
  left: 14px;
  background: rgba(3, 10, 22, 0.82);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(0, 212, 255, 0.12);
  border-radius: 8px;
  padding: 8px 10px;
  font-family: Inter, system-ui, sans-serif;
  font-size: 11px;
  color: #94a3b8;
  display: flex;
  flex-direction: column;
  gap: 4px;
  pointer-events: none;
  z-index: 1000;
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

<style>
@keyframes globe-ring-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.15);
  }
}
</style>
