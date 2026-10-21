<script setup>
/**
 * HomeView — main application view and central state owner.
 *
 * Data flow overview:
 *  - `flights`     : enriched AeroAPI data (origin, destination, CO₂, etc.), refreshed every 2 min.
 *  - `liveMarkers` : lightweight position snapshots polled from the DB every 25 s.
 *                    When available these take precedence over `flights` for map rendering
 *                    because they update faster than the AeroAPI quota allows.
 *  - `sourceFlights` / `baseFlights` / `displayedFlights` : progressive computed layers
 *                    that apply filters and the count slider without mutating the source arrays.
 *
 * Polling strategy:
 *  - 25 s  → fetchLiveMarkers()  : fast DB position poll (no AeroAPI cost)
 *  - 120 s → fetchFlights()      : AeroAPI enrichment (rate-limited, cached 1 min client-side)
 */
import { ref, computed, inject, onMounted, onUnmounted } from "vue";
import axios from "axios";
import Dashboard from "../components/Dashboard.vue";
import GlobeContainer from "../components/GlobeContainer.vue";
import CelebrityTracker from "../components/CelebrityTracker.vue";
import {
  clearTrackedFlights,
  loadTrackedFlights,
  removeTrackedFlight,
  saveTrackedFlight,
} from "../services/trackedFlights";
import SettingsModal from "../components/SettingsModal.vue";

const showSettings = ref(false);

const flights = ref([]);
const selectedFlight = ref(null);
const watchlist = ref([]);
const initialLoading = ref(true); // only true until first data arrives
const fetchError = ref(null);
const isRefreshing = ref(false);
const lastUpdateTime = ref(null);
const showDashboard = ref(true);
const showCelebrity = ref(true);
const currentUser = inject("currentUser", ref(null));

// Live DB position poll state
const liveMarkers = ref([]);

const flightHistory = ref([]);
const showHistory = ref(false);
const historyFetched = ref(false); // true after first history attempt

// Live Weather State
const weatherData = ref(null);
const isLoadingWeather = ref(false);

// --- NEW: Global Storm & Radar State (Weather Layer Switch) ---
const sigmetData = ref(null);
const activeWeatherLayer = ref("none"); // 'none', 'sigmet', or 'radar'

// Commercial flight load state
const comLoading = ref(false);
const comError = ref(false);

// Nearby airports visualization state
const nearbyAirports = ref([]);
const airportVisualizationMode = ref(false);
const isLoadingNearbyAirports = ref(false);

// Celebrity / watchlist / airports filter modes
const celebOnlyMode = ref(false);
const airportsOnlyMode = ref(false);
const celebLive = ref([]);
const CELEB_TRACKED_KEY = "celebrity_tracker_tracked_ids";

function getTrackedCelebIds() {
  try {
    const saved = localStorage.getItem(CELEB_TRACKED_KEY);
    return saved ? new Set(JSON.parse(saved)) : new Set();
  } catch {
    return new Set();
  }
}

async function fetchCelebLive() {
  try {
    const res = await axios.get("/api/db/celebrities/live");
    celebLive.value = res.data || [];
  } catch { /* silent */ }
}

const inFlightCelebs = computed(() => {
  const tracked = getTrackedCelebIds();
  return celebLive.value.filter(
    (c) => c.airborne === true && tracked.has(c.id)
  );
});

// Tail numbers of tracked in-flight celebrities — used to filter globe in celeb mode.
// Demo flights share their callsign with their tail number, so this also matches demos.
const trackedCelebTailNumbers = computed(() =>
  new Set(inFlightCelebs.value.map((c) => c.tailNumber).filter(Boolean))
);

async function toggleWatchlistMode() {
  watchlistOnly.value = !watchlistOnly.value;
  if (watchlistOnly.value) {
    celebOnlyMode.value = false;
    airportsOnlyMode.value = false;
  }
}

async function toggleCelebMode() {
  if (!celebOnlyMode.value) {
    await fetchCelebLive();
  }
  celebOnlyMode.value = !celebOnlyMode.value;
  if (celebOnlyMode.value) {
    watchlistOnly.value = false;
    airportsOnlyMode.value = false;
  }
}

function toggleAirportsOnlyMode() {
  airportsOnlyMode.value = !airportsOnlyMode.value;
  if (airportsOnlyMode.value) {
    watchlistOnly.value = false;
    celebOnlyMode.value = false;
  }
}

async function loadCommercialFlights() {
  comLoading.value = true;
  comError.value = false;
  try {
    await axios.post("/api/db/aircraft/sync-commercial");
    await fetchLiveMarkers();
  } catch {
    comError.value = true;
    setTimeout(() => { comError.value = false; }, 3000);
  } finally {
    comLoading.value = false;
  }
}

// Filter / display controls (committed state — only updated on Save)
const watchlistOnly = ref(false);
const showFilters = ref(false);
const selectedCategories = ref(new Set()); // empty = All
const minAltitude = ref(0);
const minSpeed = ref(0);
const colorMode = ref("category");
const planeDisplayCount = ref(10000);

// Draft state — lives only while the filter modal is open
const draftCategories = ref(new Set());
const draftMinAltitude = ref(0);
const draftMinSpeed = ref(0);
const draftColorMode = ref("category");
const draftWeatherLayer = ref("none");

function openFilters() {
  draftCategories.value = new Set(selectedCategories.value);
  draftMinAltitude.value = minAltitude.value;
  draftMinSpeed.value = minSpeed.value;
  draftColorMode.value = colorMode.value;
  draftWeatherLayer.value = activeWeatherLayer.value;
  showFilters.value = true;
}

function saveFilters() {
  selectedCategories.value = new Set(draftCategories.value);
  minAltitude.value = draftMinAltitude.value;
  minSpeed.value = draftMinSpeed.value;
  colorMode.value = draftColorMode.value;
  if (draftWeatherLayer.value !== activeWeatherLayer.value) {
    onChangeWeatherLayer(draftWeatherLayer.value);
  }
  showFilters.value = false;
  fetchLiveMarkers();
}

function cancelFilters() {
  showFilters.value = false;
}

function toggleDraftCategory(cat) {
  const next = new Set(draftCategories.value);
  if (next.has(cat)) {
    next.delete(cat);
  } else {
    next.add(cat);
  }
  draftCategories.value = next;
}



// sourceFlights is intentionally unfiltered — the stats panel must always show
// real totals, not the count after the user's active filters are applied.
const sourceFlights = computed(() =>
  liveMarkers.value.length > 0 ? liveMarkers.value : flights.value,
);

// Watchlist items enriched with the latest live position/route data when available.
// Stored watchlist entries may have stale origin/destination; live data takes priority.
const enrichedWatchlist = computed(() =>
  watchlist.value.map((wf) => {
    const live = sourceFlights.value.find(
      (f) =>
        f.callsign === wf.callsign ||
        (wf.tailNumber && f.tailNumber === wf.tailNumber),
    );
    // _watchlistCallsign preserves the stored identifier so onRemoveFromWatchlist
    // can match against watchlist.value even when the live flight has a different callsign.
    return { ...(live ?? wf), _watchlistCallsign: wf.callsign };
  }),
);

// baseFlights applies every user filter except the count slider, so the slider
// max reflects how many flights actually pass the current filter combination.
const baseFlights = computed(() => {
  let list = liveMarkers.value.length > 0 ? liveMarkers.value : flights.value;
  if (watchlistOnly.value && watchlist.value.length > 0) {
    const callsigns = new Set(enrichedWatchlist.value.map((f) => f.callsign));
    list = list.filter((f) => callsigns.has(f.callsign));
  } else if (celebOnlyMode.value) {
    const tails = trackedCelebTailNumbers.value;
    list = list.filter((f) => tails.has(f.tailNumber) || tails.has(f.callsign));
  }
  if (!celebOnlyMode.value && selectedCategories.value.size > 0) {
    list = list.filter((f) => {
      const cat = f.category || "commercial";
      return selectedCategories.value.has(cat);
    });
  }
  if (minAltitude.value > 0)
    list = list.filter((f) => (f.altitude ?? 0) >= minAltitude.value);
  if (minSpeed.value > 0)
    list = list.filter((f) => (f.velocity ?? 0) >= minSpeed.value);
  return list;
});

// displayedFlights is the final list rendered on the globe — sliced by the count slider
const displayedFlights = computed(() =>
  baseFlights.value.slice(0, planeDisplayCount.value),
);

const visibleCallsigns = computed(() =>
  displayedFlights.value.map((f) => f.callsign),
);

const globeVisibleCallsigns = computed(() => {
  if (airportVisualizationMode.value && selectedFlight.value) {
    return [selectedFlight.value.callsign];
  }
  if (celebOnlyMode.value) {
    // Include real flights from baseFlights + tracked celebrity tail numbers
    // (tail numbers double as callsigns for demo flights in GlobeContainer)
    const fromFlights = visibleCallsigns.value;
    const fromTails = [...trackedCelebTailNumbers.value];
    return [...new Set([...fromFlights, ...fromTails])];
  }
  return visibleCallsigns.value;
});

let pollInterval = null;
let livePollInterval = null;
let lastFetchTime = 0;
const CACHE_DURATION = 60000; // 1-minute client-side cache prevents redundant AeroAPI calls
const destinationCache = new Map();

function calculateBearing(lat1, lon1, lat2, lon2) {
  const lat1Rad = (lat1 * Math.PI) / 180;
  const lat2Rad = (lat2 * Math.PI) / 180;
  const dLonRad = ((lon2 - lon1) * Math.PI) / 180;

  const y = Math.sin(dLonRad) * Math.cos(lat2Rad);
  const x =
    Math.cos(lat1Rad) * Math.sin(lat2Rad) -
    Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLonRad);

  return ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
}

function parseAirportCoords(payload) {
  const candidates = [
    payload?.position,
    payload?.location,
    payload?.airport?.position,
    payload?.airport?.location,
  ];

  for (const c of candidates) {
    if (!c) continue;
    const lat = Number(c.latitude ?? c.lat);
    const lng = Number(c.longitude ?? c.lon ?? c.lng);
    if (Number.isFinite(lat) && Number.isFinite(lng)) {
      return { lat, lng };
    }
  }
  return null;
}

async function getDestinationCoords(code) {
  if (!code || code === "N/A" || code === "???") {
    return null;
  }

  if (destinationCache.has(code)) {
    return destinationCache.get(code);
  }

  try {
    const resp = await axios.get(`/api/airports/${encodeURIComponent(code)}`);
    const coords = parseAirportCoords(resp.data);
    destinationCache.set(code, coords);
    return coords;
  } catch {
    destinationCache.set(code, null);
    return null;
  }
}

async function attachDestinationBearings(flightList) {
  const uniqueCodes = [
    ...new Set(
      flightList
        .map((f) => f.destination)
        .filter((code) => code && code !== "N/A"),
    ),
  ];
  await Promise.all(uniqueCodes.map((code) => getDestinationCoords(code)));

  flightList.forEach((flight) => {
    const coords = destinationCache.get(flight.destination);
    if (coords && Number.isFinite(flight.lat) && Number.isFinite(flight.lng)) {
      flight.bearing = calculateBearing(
        flight.lat,
        flight.lng,
        coords.lat,
        coords.lng,
      );
    }
  });
}

async function fetchFlights(retryCount = 0, forceRefresh = false) {
  const now = Date.now();

  if (
    !forceRefresh &&
    flights.value.length > 0 &&
    now - lastFetchTime < CACHE_DURATION &&
    !retryCount
  ) {
    return;
  }

  const maxRetries = 3;
  const baseDelay = 2000;

  const isFirstLoad =
    flights.value.length === 0 && liveMarkers.value.length === 0;
  if (isFirstLoad) initialLoading.value = true;

  fetchError.value = null;
  try {
    if (!forceRefresh) {
      const cachedResp = await axios.get("/api/db/flights/cache?minutes=180");
      const cachedFlights = cachedResp.data?.flights ?? [];
      if (cachedFlights.length > 0) {
        flights.value = cachedFlights;
        lastFetchTime = now;
        lastUpdateTime.value = new Date();
        return;
      }
    }

    const query = "-type B738";
    const res = await axios.get(
      `/api/flights/search?query=${encodeURIComponent(query)}&max_pages=1`,
    );
    const flightList = res.data.flights ?? [];

    const transformed = flightList
      .filter((f) => f.last_position?.latitude && f.last_position?.longitude)
      .map((f) => ({
        callsign: f.ident,
        origin: f.origin?.code ?? "N/A",
        destination: f.destination?.code ?? "N/A",
        altitude: (f.last_position.altitude ?? 0) * 100,
        velocity: f.last_position.groundspeed ?? 0,
        lat: f.last_position.latitude,
        lng: f.last_position.longitude,
        bearing: null,
        liveInfo: null,
      }));

    await attachDestinationBearings(transformed);
    await saveFlightsToDatabase(transformed);

    flights.value = transformed;
    lastFetchTime = now;
    lastUpdateTime.value = new Date();
  } catch (e) {
    if (e.response?.status === 429 && retryCount < maxRetries) {
      const delay = baseDelay * Math.pow(2, retryCount);
      await new Promise((resolve) => setTimeout(resolve, delay));
      return fetchFlights(retryCount + 1);
    } else if (e.response?.status === 429) {
      try {
        const cachedResp = await axios.get("/api/db/flights/cache?minutes=720");
        const cachedFlights = cachedResp.data?.flights ?? [];
        if (cachedFlights.length > 0) {
          flights.value = cachedFlights;
          lastFetchTime = now;
          lastUpdateTime.value = new Date();
        }
      } catch {
        /* keep existing data */
      }
    }
    if (
      isFirstLoad &&
      flights.value.length === 0 &&
      liveMarkers.value.length === 0
    ) {
      fetchError.value =
        "Could not load flight data. Check your network connection and try again.";
    }
  } finally {
    initialLoading.value = false;
  }
}

async function saveFlightsToDatabase(flightList) {
  if (!flightList || flightList.length === 0) {
    return;
  }
  try {
    const flightsToSave = flightList.map((f) => ({
      callsign: f.callsign,
      origin: f.origin,
      destination: f.destination,
      altitude: f.altitude,
      velocity: f.velocity,
      lat: f.lat,
      lng: f.lng,
      bearing: f.bearing,
      liveInfo: f.liveInfo,
    }));
    await axios.post("/api/db/flights/batch", flightsToSave);
  } catch (error) {
    console.error("Error saving flights to database:", error.message);
  }
}

async function fetchLiveMarkers() {
  try {
    const res = await axios.get("/api/db/aircraft/live");
    liveMarkers.value = (res.data || []).map((a) => {
      const callsign = a.lastCallsign || a.tailNumber || "UNKNOWN";
      const enriched = flights.value.find(
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
        liveInfo: enriched?.liveInfo || null,
        estimatedCo2Kg: a.estimatedCo2Kg ?? enriched?.estimatedCo2Kg ?? null,
        category: a.category,
      };
    });
  } catch {
    // silent
  }
}

async function fetchHistory(callsign) {
  if (historyFetched.value) return;
  flightHistory.value = [];
  const cleanCallsign = callsign.trim();
  try {
    const identRes = await axios.get(
      `/api/flights/${encodeURIComponent(cleanCallsign)}`,
    );
    const flights = identRes.data?.flights ?? [];
    const departed =
      flights.find((f) => f.actual_off != null) ??
      flights.find((f) => f.progress_percent > 0);
    const faId = departed?.fa_flight_id;
    if (!faId) {
      historyFetched.value = true;
      return;
    }

    const trackRes = await axios.get(
      `/api/flights/${encodeURIComponent(faId)}/track`,
    );
    const positions = trackRes.data?.positions ?? [];
    flightHistory.value = positions
      .filter((p) => p.latitude != null && p.longitude != null)
      .map((p) => ({ lat: p.latitude, lng: p.longitude }));
  } catch {
    flightHistory.value = [];
  } finally {
    historyFetched.value = true;
  }
}

function toggleHistory() {
  showHistory.value = !showHistory.value;
  if (showHistory.value && !historyFetched.value && selectedFlight.value) {
    fetchHistory(selectedFlight.value.callsign);
  }
}

// -------- Fetch Live Weather

function getWeatherCondition(code) {
  if (code <= 1) return "Clear Skies ☀️";
  if (code <= 3) return "Partly Cloudy ⛅";
  if (code <= 49) return "Fog/Overcast ☁️";
  if (code <= 69) return "Rain 🌧️";
  if (code <= 79) return "Snow ❄️";
  if (code <= 99) return "Thunderstorm ⛈️";
  return "Unknown";
}

async function fetchWeather(lat, lng) {
  isLoadingWeather.value = true;
  try {
    const res = await axios.get(`/api/weather?lat=${lat}&lng=${lng}`);
    const data = res.data.current;

    weatherData.value = {
      lat,
      lng,
      temp: data.temperature_2m,
      windSpeed: data.wind_speed_10m,
      windGusts: data.wind_gusts_10m,
      windDirection: data.wind_direction_10m,
      cloudCover: data.cloud_cover,
      condition: getWeatherCondition(data.weather_code),
      isStormy: data.weather_code >= 95,
    };
  } catch (e) {
    console.error("Failed to fetch weather", e);
    weatherData.value = null;
  } finally {
    isLoadingWeather.value = false;
  }
}

function clearWeather() {
  weatherData.value = null;
}

// THE NEW WEATHER SWITCH CONTROLLER
async function onChangeWeatherLayer(layerType) {
  activeWeatherLayer.value = layerType;

  if (layerType === "sigmet") {
    try {
      const res = await axios.get("/api/weather/sigmets");
      sigmetData.value = res.data;
    } catch (e) {
      console.error("Failed to load SIGMETs", e);
    }
  } else if (layerType === "radar") {
    sigmetData.value = null;
  } else {
    sigmetData.value = null;
  }
}

// -------------------------

// Static list of major airports with known coordinates used for nearby-airport visualization.
// These are always available regardless of what flights are currently cached.
const MAJOR_AIRPORTS = [
  // ── United States ──────────────────────────────
  { code: "KATL", lat: 33.6407, lng: -84.4277 },
  { code: "KLAX", lat: 33.9425, lng: -118.4081 },
  { code: "KORD", lat: 41.9742, lng: -87.9073 },
  { code: "KDFW", lat: 32.8998, lng: -97.0403 },
  { code: "KDEN", lat: 39.8561, lng: -104.6737 },
  { code: "KJFK", lat: 40.6413, lng: -73.7781 },
  { code: "KSFO", lat: 37.6213, lng: -122.3790 },
  { code: "KSEA", lat: 47.4502, lng: -122.3088 },
  { code: "KLAS", lat: 36.0840, lng: -115.1537 },
  { code: "KMCO", lat: 28.4294, lng: -81.3090 },
  { code: "KEWR", lat: 40.6895, lng: -74.1745 },
  { code: "KPHX", lat: 33.4373, lng: -112.0078 },
  { code: "KMIA", lat: 25.7959, lng: -80.2870 },
  { code: "KIAH", lat: 29.9902, lng: -95.3368 },
  { code: "KBOS", lat: 42.3656, lng: -71.0096 },
  { code: "KMSP", lat: 44.8848, lng: -93.2223 },
  { code: "KDTW", lat: 42.2162, lng: -83.3554 },
  { code: "KPHL", lat: 39.8719, lng: -75.2411 },
  { code: "KFLL", lat: 26.0726, lng: -80.1527 },
  { code: "KBWI", lat: 39.1754, lng: -76.6683 },
  { code: "KSLC", lat: 40.7884, lng: -111.9778 },
  { code: "KDCA", lat: 38.8521, lng: -77.0377 },
  { code: "KIAD", lat: 38.9531, lng: -77.4565 },
  { code: "KMDW", lat: 41.7860, lng: -87.7424 },
  { code: "KSTL", lat: 38.7487, lng: -90.3700 },
  { code: "KBNA", lat: 36.1245, lng: -86.6782 },
  { code: "KAUS", lat: 30.1975, lng: -97.6664 },
  { code: "KHOU", lat: 29.6454, lng: -95.2789 },
  { code: "KPDX", lat: 45.5898, lng: -122.5951 },
  { code: "KSAN", lat: 32.7336, lng: -117.1897 },
  { code: "KMEM", lat: 35.0424, lng: -89.9767 },
  { code: "KCLT", lat: 35.2140, lng: -80.9431 },
  { code: "KRDU", lat: 35.8776, lng: -78.7875 },
  { code: "KIND", lat: 39.7173, lng: -86.2944 },
  { code: "KCMH", lat: 39.9999, lng: -82.8919 },
  { code: "KMKE", lat: 42.9472, lng: -87.8966 },
  { code: "KPIT", lat: 40.4915, lng: -80.2329 },
  { code: "KCLE", lat: 41.4117, lng: -81.8498 },
  { code: "KOAK", lat: 37.7213, lng: -122.2208 },
  { code: "KSJC", lat: 37.3626, lng: -121.9290 },
  { code: "KABQ", lat: 35.0402, lng: -106.6091 },
  { code: "KANC", lat: 61.1744, lng: -149.9961 },
  { code: "PHNL", lat: 21.3187, lng: -157.9224 },
  // ── Canada ─────────────────────────────────────
  { code: "CYYZ", lat: 43.6772, lng: -79.6306 },
  { code: "CYVR", lat: 49.1947, lng: -123.1792 },
  { code: "CYUL", lat: 45.4706, lng: -73.7408 },
  { code: "CYYC", lat: 51.1139, lng: -114.0200 },
  { code: "CYEG", lat: 53.3097, lng: -113.5797 },
  { code: "CYYJ", lat: 48.6469, lng: -123.4258 },
  { code: "CYWG", lat: 49.9100, lng: -97.2398 },
  { code: "CYOW", lat: 45.3225, lng: -75.6692 },
  // ── Mexico & Caribbean ─────────────────────────
  { code: "MMMX", lat: 19.4363, lng: -99.0721 },
  { code: "MMCU", lat: 21.0365, lng: -86.8771 },
  { code: "MMGL", lat: 20.5218, lng: -103.3110 },
  { code: "MMMT", lat: 19.8434, lng: -90.0023 },
  { code: "MBPV", lat: 21.7736, lng: -72.1886 },
  { code: "MKJP", lat: 17.9357, lng: -76.7875 },
  // ── Central & South America ────────────────────
  { code: "SBGR", lat: -23.4356, lng: -46.4731 },
  { code: "SBGL", lat: -22.8099, lng: -43.2505 },
  { code: "SAEZ", lat: -34.8222, lng: -58.5358 },
  { code: "SCEL", lat: -33.3930, lng: -70.7858 },
  { code: "SKBO", lat: 4.7016, lng: -74.1469 },
  { code: "SEQM", lat: -0.1292, lng: -78.3575 },
  { code: "SPJC", lat: -12.0219, lng: -77.1143 },
  { code: "SBCF", lat: -19.6244, lng: -43.9717 },
  { code: "SBBR", lat: -15.8711, lng: -47.9186 },
  { code: "SVMI", lat: 10.6031, lng: -66.9906 },
  { code: "SPIM", lat: -12.0219, lng: -77.1143 },
  // ── Europe ─────────────────────────────────────
  { code: "EGLL", lat: 51.4775, lng: -0.4614 },
  { code: "LFPG", lat: 49.0097, lng: 2.5479 },
  { code: "EDDF", lat: 50.0379, lng: 8.5622 },
  { code: "EHAM", lat: 52.3086, lng: 4.7639 },
  { code: "LEMD", lat: 40.4936, lng: -3.5668 },
  { code: "LIRF", lat: 41.8003, lng: 12.2389 },
  { code: "LSZH", lat: 47.4647, lng: 8.5492 },
  { code: "EDDM", lat: 48.3537, lng: 11.7750 },
  { code: "LEBL", lat: 41.2971, lng: 2.0785 },
  { code: "EGKK", lat: 51.1481, lng: -0.1903 },
  { code: "EKCH", lat: 55.6180, lng: 12.6561 },
  { code: "ESSA", lat: 59.6519, lng: 17.9186 },
  { code: "EFHK", lat: 60.3172, lng: 24.9633 },
  { code: "EPWA", lat: 52.1657, lng: 20.9671 },
  { code: "LKPR", lat: 50.1008, lng: 14.2600 },
  { code: "LOWW", lat: 48.1102, lng: 16.5697 },
  { code: "EBBR", lat: 50.9014, lng: 4.4844 },
  { code: "ENGM", lat: 60.1939, lng: 11.1004 },
  { code: "EIDW", lat: 53.4213, lng: -6.2701 },
  { code: "LPPT", lat: 38.7813, lng: -9.1359 },
  { code: "LGAV", lat: 37.9364, lng: 23.9445 },
  { code: "LHBP", lat: 47.4369, lng: 19.2556 },
  { code: "LROP", lat: 44.5711, lng: 26.0850 },
  { code: "UUEE", lat: 55.9726, lng: 37.4146 },
  { code: "UUDD", lat: 55.4088, lng: 37.9063 },
  { code: "UKBB", lat: 50.3450, lng: 30.8947 },
  { code: "LIMC", lat: 45.6306, lng: 8.7281 },
  { code: "LFMN", lat: 43.6584, lng: 7.2159 },
  { code: "LFBO", lat: 43.6293, lng: 1.3638 },
  { code: "EDDL", lat: 51.2895, lng: 6.7668 },
  // ── Middle East ────────────────────────────────
  { code: "OMDB", lat: 25.2532, lng: 55.3657 },
  { code: "OBBI", lat: 25.2731, lng: 51.6081 },
  { code: "OMAA", lat: 24.4330, lng: 54.6511 },
  { code: "OERK", lat: 24.9576, lng: 46.6988 },
  { code: "LLBG", lat: 32.0114, lng: 34.8867 },
  { code: "OJAM", lat: 31.7226, lng: 35.9932 },
  { code: "OAKB", lat: 34.5659, lng: 69.2123 },
  // ── Africa ─────────────────────────────────────
  { code: "FAOR", lat: -26.1392, lng: 28.2460 },
  { code: "HECA", lat: 30.1219, lng: 31.4056 },
  { code: "DNMM", lat: 6.5774, lng: 3.3216 },
  { code: "HAAB", lat: 8.9779, lng: 38.7993 },
  { code: "GMMN", lat: 33.3675, lng: -7.5898 },
  { code: "DAAG", lat: 36.6910, lng: 3.2154 },
  { code: "FKYS", lat: 3.8362, lng: 11.5233 },
  { code: "HTDA", lat: -6.8781, lng: 39.2026 },
  { code: "FALE", lat: -29.6144, lng: 31.1197 },
  // ── South & Southeast Asia ─────────────────────
  { code: "VHHH", lat: 22.3080, lng: 113.9185 },
  { code: "WSSS", lat: 1.3644, lng: 103.9915 },
  { code: "VTBS", lat: 13.6811, lng: 100.7475 },
  { code: "VIDP", lat: 28.5562, lng: 77.1000 },
  { code: "VABB", lat: 19.0896, lng: 72.8656 },
  { code: "VECC", lat: 22.6547, lng: 88.4467 },
  { code: "WMKK", lat: 2.7456, lng: 101.7099 },
  { code: "WIII", lat: -6.1256, lng: 106.6559 },
  { code: "RPLL", lat: 14.5086, lng: 121.0194 },
  { code: "VVTS", lat: 10.8188, lng: 106.6520 },
  { code: "VNKT", lat: 27.6966, lng: 85.3591 },
  { code: "VOCB", lat: 12.9499, lng: 77.6682 },
  // ── East Asia ──────────────────────────────────
  { code: "ZBAA", lat: 40.0799, lng: 116.6031 },
  { code: "ZSPD", lat: 31.1443, lng: 121.8083 },
  { code: "ZGGG", lat: 23.3924, lng: 113.2990 },
  { code: "ZUCK", lat: 29.7192, lng: 106.6419 },
  { code: "RJTT", lat: 35.5533, lng: 139.7811 },
  { code: "RJAA", lat: 35.7653, lng: 140.3856 },
  { code: "RKSI", lat: 37.4602, lng: 126.4407 },
  { code: "RCTP", lat: 25.0777, lng: 121.2330 },
  { code: "VHHH", lat: 22.3080, lng: 113.9185 },
  { code: "RJBB", lat: 34.4347, lng: 135.2440 },
  { code: "RJFF", lat: 33.5853, lng: 130.4511 },
  { code: "ZSSS", lat: 31.1979, lng: 121.3362 },
  // ── Central Asia & Russia ──────────────────────
  { code: "UAAA", lat: 43.3521, lng: 77.0405 },
  { code: "UTTT", lat: 41.2579, lng: 69.2812 },
  { code: "ORMM", lat: 30.5491, lng: 47.6621 },
  // ── Oceania ────────────────────────────────────
  { code: "YSSY", lat: -33.9461, lng: 151.1772 },
  { code: "YMML", lat: -37.6733, lng: 144.8430 },
  { code: "YBBN", lat: -27.3842, lng: 153.1175 },
  { code: "YPPH", lat: -31.9403, lng: 115.9669 },
  { code: "NZAA", lat: -37.0082, lng: 174.7917 },
  { code: "NZCH", lat: -43.4894, lng: 172.5322 },
  { code: "NFFN", lat: -17.7554, lng: 177.4431 },
];

function haversineDistanceMiles(lat1, lon1, lat2, lon2) {
  const R = 3958.8;
  const toRad = (d) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

async function onVisualizeAirports() {
  if (!selectedFlight.value) return;

  if (airportVisualizationMode.value) {
    airportVisualizationMode.value = false;
    nearbyAirports.value = [];
    return;
  }

  isLoadingNearbyAirports.value = true;
  try {
    const { lat, lng } = selectedFlight.value;

    const sorted = MAJOR_AIRPORTS
      .map((a) => ({
        ...a,
        distanceMiles: Math.round(haversineDistanceMiles(lat, lng, a.lat, a.lng)),
      }))
      .sort((a, b) => a.distanceMiles - b.distanceMiles)
      .slice(0, 5);

    nearbyAirports.value = sorted;
    airportVisualizationMode.value = true;
  } catch (e) {
    console.error("Failed to compute nearby airports", e);
  } finally {
    isLoadingNearbyAirports.value = false;
  }
}

// -------------------------

function onSelectFlight(flight) {
  airportVisualizationMode.value = false;
  nearbyAirports.value = [];
  if (selectedFlight.value?.callsign === flight.callsign) {
    selectedFlight.value = null;
    showHistory.value = false;
    flightHistory.value = [];
    historyFetched.value = false;
    clearWeather();
  } else {
    selectedFlight.value = flight;
    showHistory.value = false;
    flightHistory.value = [];
    historyFetched.value = false;
    fetchWeather(flight.lat, flight.lng);
  }
}

function onFocusCelebrityAircraft(tailNumber) {
  const match = liveMarkers.value.find((f) => f.tailNumber === tailNumber);
  if (match) onSelectFlight(match);
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
  }
}

async function onRemoveFromWatchlist(flight) {
  const storedCallsign = flight._watchlistCallsign ?? flight.callsign;
  const previous = [...watchlist.value];
  watchlist.value = watchlist.value.filter(
    (f) => f.callsign !== storedCallsign,
  );
  try {
    await removeTrackedFlight(currentUser, storedCallsign);
  } catch {
    watchlist.value = previous;
  }
}

async function onClearWatchlist() {
  const previous = [...watchlist.value];
  watchlist.value = [];
  try {
    await clearTrackedFlights(currentUser);
  } catch {
    watchlist.value = previous;
  }
}

async function manualRefresh() {
  isRefreshing.value = true;
  lastFetchTime = 0;
  try {
    await fetchFlights(0, true);
  } finally {
    isRefreshing.value = false;
  }
}

onMounted(() => {
  loadTrackedFlights(currentUser)
    .then((items) => {
      watchlist.value = items;
    })
    .catch(() => {
      watchlist.value = [];
    });

  fetchFlights();
  fetchLiveMarkers();
  pollInterval = setInterval(() => fetchFlights(), 120000); // 2-min AeroAPI enrichment poll
  livePollInterval = setInterval(() => fetchLiveMarkers(), 25000); // 25-s fast DB position poll
});

onUnmounted(() => {
  clearInterval(pollInterval);
  clearInterval(livePollInterval);
});
</script>

<template>
  <div class="app-layout">
    <div
      class="sidebar-wrapper sidebar-wrapper--left"
      :class="{ collapsed: !showDashboard }"
    >
      <Dashboard
        :flights="sourceFlights"
        :watchlist="enrichedWatchlist"
        :selected-flight="selectedFlight"
        :watchlist-only="watchlistOnly"
        :com-loading="comLoading"
        :com-error="comError"
        :weather-data="weatherData"
        :is-loading-weather="isLoadingWeather"
        :is-visualizing-airports="airportVisualizationMode"
        :is-loading-nearby-airports="isLoadingNearbyAirports"
        @select-flight="onSelectFlight"
        @remove-from-watchlist="onRemoveFromWatchlist"
        @clear-watchlist="onClearWatchlist"
        @toggle-watchlist-only="watchlistOnly = !watchlistOnly"
        @change-weather-layer="onChangeWeatherLayer"
        @clear-weather="weatherData = null"
        @load-commercial="loadCommercialFlights"
        @visualize-airports="onVisualizeAirports"
      />
      <button
        class="sidebar-toggle sidebar-toggle--left"
        @click="showDashboard = !showDashboard"
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
            :points="showDashboard ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"
          />
        </svg>
      </button>
    </div>

    <div class="map-area">
      <header class="app-header">
        <div class="header-left">
          <button
            class="api-link watchlist-nav-link"
            :class="{ 'watchlist-nav-link--has-items': watchlist.length > 0, 'watchlist-nav-link--active': watchlistOnly }"
            @click="toggleWatchlistMode"
            title="Show only watchlist flights"
          >
            ★ Watchlist
            <span v-if="watchlist.length > 0" class="watchlist-nav-badge">{{
              watchlist.length
            }}</span>
          </button>
          <div class="celeb-dropdown-wrapper">
            <button
              class="api-link celeb-nav-btn"
              :class="{ 'celeb-nav-btn--active': celebOnlyMode }"
              @click="toggleCelebMode"
              title="Show only tracked celebrity flights"
            >
              ★ Celebrities
              <span v-if="inFlightCelebs.length > 0" class="celeb-nav-badge">{{ inFlightCelebs.length }}</span>
            </button>
          </div>
          <button
            class="api-link airports-nav-btn"
            :class="{ 'airports-nav-btn--active': airportsOnlyMode }"
            @click="toggleAirportsOnlyMode"
            title="Show only airports"
          >
            ✈ Airports
          </button>
          <button class="api-link settings-nav-btn" @click="showSettings = true" title="Settings">
            ⚙ Settings
          </button>
          <button
            class="api-link filters-nav-btn"
            :class="{ 'filters-nav-btn--active': showFilters || minAltitude > 0 || minSpeed > 0 || selectedCategories.size > 0 || activeWeatherLayer !== 'none' }"
            @click="openFilters"
            title="Filters & display options"
          >
            ⊙ Filters
            <span
              v-if="minAltitude > 0 || minSpeed > 0 || selectedCategories.size > 0 || activeWeatherLayer !== 'none'"
              class="filter-active-dot"
            ></span>
          </button>
        </div>

        <div class="header-right">
          <span class="aircraft-count">
            <span class="aircraft-count-num">{{
              displayedFlights.length
            }}</span>
            <span class="aircraft-count-label">ac</span>
          </span>
          <span v-if="lastUpdateTime" class="update-time">{{
            lastUpdateTime.toLocaleTimeString()
          }}</span>
          <button
            class="refresh-btn"
            @click="manualRefresh"
            :disabled="initialLoading || isRefreshing"
            title="Refresh"
          >
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
              :style="
                isRefreshing ? 'animation: spin 0.8s linear infinite' : ''
              "
            >
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path
                d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15"
              />
            </svg>
          </button>
        </div>
      </header>

      <div v-if="showFilters" class="filter-overlay" @click.self="cancelFilters">
        <div class="filter-modal">
          <div class="filter-modal-header">
            <div class="filter-modal-title">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#00d4ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
              </svg>
              Filters
            </div>
            <button class="filter-modal-close" @click="cancelFilters">✕</button>
          </div>

          <div class="filter-modal-body">
            <div class="fm-section">
              <div class="fm-label">Category <span class="fm-label-hint">(select multiple)</span></div>
              <div class="fm-btn-row">
                <button :class="['fm-btn', { 'fm-btn--active': draftCategories.size === 0 }]" @click="draftCategories = new Set()">All</button>
                <button :class="['fm-btn fm-btn--commercial', { 'fm-btn--active': draftCategories.has('commercial') }]" @click="toggleDraftCategory('commercial')">Commercial</button>
                <button :class="['fm-btn fm-btn--mil', { 'fm-btn--active': draftCategories.has('mil') }]" @click="toggleDraftCategory('mil')">Military</button>
                <button :class="['fm-btn fm-btn--ladd', { 'fm-btn--active': draftCategories.has('ladd') }]" @click="toggleDraftCategory('ladd')">LADD</button>
                <button :class="['fm-btn fm-btn--pia', { 'fm-btn--active': draftCategories.has('pia') }]" @click="toggleDraftCategory('pia')">PIA</button>
                <button :class="['fm-btn fm-btn--celebrity', { 'fm-btn--active': draftCategories.has('celebrity') }]" @click="toggleDraftCategory('celebrity')">Celebrity</button>
              </div>
            </div>

            <div class="fm-divider"></div>

            <div class="fm-section">
              <div class="fm-label">Weather Layer</div>
              <div class="fm-btn-row">
                <button :class="['fm-btn', { 'fm-btn--active': draftWeatherLayer === 'none' }]" @click="draftWeatherLayer = 'none'">Off</button>
                <button :class="['fm-btn fm-btn--hazards', { 'fm-btn--active': draftWeatherLayer === 'sigmet' }]" @click="draftWeatherLayer = 'sigmet'">⚠ Hazards</button>
                <button :class="['fm-btn fm-btn--radar', { 'fm-btn--active': draftWeatherLayer === 'radar' }]" @click="draftWeatherLayer = 'radar'">🌧 Radar</button>
              </div>
            </div>

            <div class="fm-divider"></div>

            <div class="fm-section">
              <div class="fm-label">Color By</div>
              <div class="fm-btn-row">
                <button :class="['fm-btn', { 'fm-btn--active': draftColorMode === 'category' }]" @click="draftColorMode = 'category'">Category</button>
                <button :class="['fm-btn', { 'fm-btn--active': draftColorMode === 'altitude' }]" @click="draftColorMode = 'altitude'">Altitude</button>
                <button :class="['fm-btn', { 'fm-btn--active': draftColorMode === 'speed' }]" @click="draftColorMode = 'speed'">Speed</button>
              </div>
            </div>

            <div class="fm-divider"></div>

            <div class="fm-section">
              <div class="fm-slider-label-row">
                <div class="fm-label">Min Altitude</div>
                <div class="fm-slider-controls">
                  <input type="range" class="fm-slider" min="0" max="50000" step="1000" v-model.number="draftMinAltitude" />
                  <input type="number" class="fm-number-input" min="0" max="50000" step="1000" v-model.number="draftMinAltitude" />
                  <span class="fm-unit">ft</span>
                </div>
              </div>
            </div>

            <div class="fm-section">
              <div class="fm-slider-label-row">
                <div class="fm-label">Min Speed</div>
                <div class="fm-slider-controls">
                  <input type="range" class="fm-slider" min="0" max="600" step="10" v-model.number="draftMinSpeed" />
                  <input type="number" class="fm-number-input" min="0" max="600" step="10" v-model.number="draftMinSpeed" />
                  <span class="fm-unit">kts</span>
                </div>
              </div>
            </div>

            <div class="fm-section">
              <div class="fm-slider-label-row">
                <div class="fm-label">Display Limit</div>
                <div class="fm-slider-controls">
                  <input type="range" class="fm-slider" min="1" :max="Math.max(baseFlights.length, 1)" v-model.number="planeDisplayCount" />
                  <input type="number" class="fm-number-input" min="1" :max="Math.max(baseFlights.length, 1)" step="100" v-model.number="planeDisplayCount" />
                  <span class="fm-count-of fm-unit">/ {{ baseFlights.length }}</span>
                </div>
              </div>
            </div>

            <div class="fm-footer">
              <button class="fm-reset-btn" @click="draftCategories = new Set(); draftMinAltitude = 0; draftMinSpeed = 0;">Reset</button>
              <div class="fm-footer-actions">
                <button class="fm-cancel-btn" @click="cancelFilters">Cancel</button>
                <button class="fm-save-btn" @click="saveFilters">Save</button>
              </div>
            </div>
          </div>
        </div>
      </div>

<div v-if="initialLoading" class="overlay">Loading live flights...</div>
      <div v-else-if="fetchError" class="overlay overlay--error">
        <svg
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#f87171"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          style="margin-bottom: 8px"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <span>{{ fetchError }}</span>
        <button class="overlay-retry-btn" @click="manualRefresh">Retry</button>
      </div>
      <div
        v-else-if="displayedFlights.length === 0 && selectedCategories.size > 0"
        class="no-flights-overlay"
      >
        No flights in this category
      </div>

      <div v-if="weatherData" class="weather-overlay">
        <button class="clear-weather-btn" @click="weatherData = null">✕</button>
        <span class="weather-overlay-condition">{{ weatherData.condition }}</span>
        <span class="weather-overlay-temp">{{ Math.round(weatherData.temp) }}°F</span>
        <span class="weather-overlay-wind">Wind {{ weatherData.windSpeed }} mph</span>
      </div>

      <div v-if="activeWeatherLayer === 'sigmet'" class="sigmet-legend">
        <h3 class="legend-title">Active Hazard Warning Zones (Next 4 Hrs)</h3>
        <ul class="legend-list">
          <li>
            <span
              class="legend-color"
              style="background: rgba(239, 68, 68, 0.8)"
            ></span>
            Thunderstorms (Convective)
          </li>
          <li>
            <span
              class="legend-color"
              style="background: rgba(56, 189, 248, 0.8)"
            ></span>
            Severe Icing
          </li>
          <li>
            <span
              class="legend-color"
              style="background: rgba(249, 115, 22, 0.8)"
            ></span>
            Severe Turbulence
          </li>
          <li>
            <span
              class="legend-color"
              style="background: rgba(168, 85, 247, 0.8)"
            ></span>
            Volcanic Ash
          </li>
        </ul>
      </div>

      <GlobeContainer
        v-show="!initialLoading"
        :flights="sourceFlights"
        :visible-callsigns="globeVisibleCallsigns"
        :use-visibility-filter="true"
        :watchlist="watchlist"
        :selected-flight="selectedFlight"
        :destination-cache="Object.fromEntries(destinationCache)"
        :color-mode="colorMode"
        :selected-category="[...selectedCategories].join(',')"
        :path-history="flightHistory"
        :show-history="showHistory"
        :weather-data="weatherData"
        :sigmet-data="sigmetData"
        :active-weather-layer="activeWeatherLayer"
        :nearby-airports="nearbyAirports"
        :airport-visualization-mode="airportVisualizationMode"
        :celeb-only-mode="celebOnlyMode"
        :watchlist-only="watchlistOnly"
        :airports-only-mode="airportsOnlyMode"
        :all-airports="MAJOR_AIRPORTS"
        @select-flight="onSelectFlight"
        @toggle-watchlist="onToggleWatchlist"
      />

      <div v-if="selectedFlight" class="flight-info-panel">
        <div class="fi-header">
          <span class="fi-callsign">{{ selectedFlight.callsign }}</span>
          <span
            v-if="selectedFlight.category"
            :class="['fi-badge', `fi-badge--${selectedFlight.category}`]"
          >
            {{ selectedFlight.category.toUpperCase() }}
          </span>
          <button
            class="fi-close"
            @click="
              selectedFlight = null;
              showHistory = false;
              flightHistory = [];
              historyFetched = false;
            "
          >
            ✕
          </button>
        </div>
        <div class="fi-route">
          {{ selectedFlight.origin }} → {{ selectedFlight.destination }}
        </div>
        <div v-if="selectedFlight.celebrity" class="fi-celebrity-name">
          {{ selectedFlight.celebrity }}
        </div>
        <div class="fi-route">
          {{ selectedFlight.origin }} → {{ selectedFlight.destination }}
        </div>
        <div v-if="selectedFlight.celebrity" class="fi-celebrity-name">
          {{ selectedFlight.celebrity }}
        </div>
        <div class="fi-route">
          {{ selectedFlight.origin }} → {{ selectedFlight.destination }}
        </div>
        <div class="fi-stats">
          <span>ALT {{ selectedFlight.altitude?.toLocaleString() }} ft</span>
          <span>SPD {{ selectedFlight.velocity }} kts</span>
          <span v-if="selectedFlight.bearing"
            >HDG {{ Math.round(selectedFlight.bearing) }}°</span
          >
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
        <button
          :class="['fi-history-btn', { 'fi-history-btn--active': showHistory }]"
          @click="toggleHistory"
        >
          {{ showHistory ? "Hide History" : "Show History" }}
        </button>
        <div v-if="showHistory" class="fi-history">
          <div v-if="!historyFetched" class="fi-history-msg">
            Loading path...
          </div>
          <div
            v-else-if="flightHistory.length === 0"
            class="fi-history-msg fi-history-msg--empty"
          >
            No path history available
          </div>
          <div v-else class="fi-history-msg fi-history-msg--ok">
            Path loaded · {{ flightHistory.length }} position points
          </div>
        </div>
      </div>
    </div>

    <div
      class="sidebar-wrapper sidebar-wrapper--right"
      :class="{ collapsed: !showCelebrity }"
    >
      <button
        class="sidebar-toggle sidebar-toggle--right"
        @click="showCelebrity = !showCelebrity"
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
            :points="showCelebrity ? '9 18 15 12 9 6' : '15 18 9 12 15 6'"
          />
        </svg>
      </button>
      <CelebrityTracker @focus-aircraft="onFocusCelebrityAircraft" />
    </div>
  </div>

  <SettingsModal v-if="showSettings" @close="showSettings = false" />
</template>

<style>
.app-layout {
  display: flex;
  flex-direction: row;
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

.app-header {
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
  font-family: Inter, system-ui, sans-serif;
}

.header-left {
  display: flex;
  gap: 0.3rem;
  align-items: center;
}
.header-center {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-left: auto;
}

.api-link {
  color: #94a3b8;
  padding: 0.22rem 0.65rem;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 6px;
  text-decoration: none;
  font-size: 0.72rem;
  font-weight: 500;
  letter-spacing: 0.02em;
  background: rgba(148, 163, 184, 0.07);
  transition:
    color 0.12s,
    border-color 0.12s,
    background 0.12s;
}
.api-link:hover {
  color: #e2e8f0;
  border-color: rgba(148, 163, 184, 0.55);
  background: rgba(148, 163, 184, 0.14);
}

.settings-nav-btn {
  background: rgba(148, 163, 184, 0.07);
  cursor: pointer;
  font-family: inherit;
}

.watchlist-nav-link--has-items .watchlist-nav-badge {
  background: rgba(245, 158, 11, 0.25);
  color: #f59e0b;
}
.watchlist-nav-link--active {
  color: #fbbf24 !important;
  border-color: rgba(245, 158, 11, 0.8) !important;
  background: rgba(245, 158, 11, 0.2) !important;
  box-shadow: 0 0 0 1px rgba(245, 158, 11, 0.4);
}
.watchlist-nav-badge {
  display: inline-block;
  margin-left: 0.3rem;
  background: rgba(245, 158, 11, 0.2);
  border-radius: 8px;
  padding: 0 0.35rem;
  font-size: 0.63rem;
  font-weight: 700;
  color: #f59e0b;
  vertical-align: middle;
}

.aircraft-count {
  display: flex;
  align-items: baseline;
  gap: 0.2rem;
}
.aircraft-count-num {
  color: #00d4ff;
  font-size: 0.82rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.aircraft-count-label {
  color: #1e3a5f;
  font-size: 0.62rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.source-select {
  background: rgba(0, 212, 255, 0.06);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 6px;
  color: #00d4ff;
  font-size: 0.75rem;
  padding: 0.2rem 0.5rem;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s;
}
.source-select:hover,
.source-select:focus {
  border-color: rgba(0, 212, 255, 0.45);
}
.source-select option {
  background: #030a16;
  color: #e2e8f0;
}

.plane-count {
  color: #475569;
  font-size: 0.72rem;
  font-variant-numeric: tabular-nums;
}

.update-time {
  color: #1e3a5f;
  font-size: 0.63rem;
  font-variant-numeric: tabular-nums;
}

.refresh-btn {
  background: none;
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 5px;
  color: #475569;
  padding: 0.25rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: all 0.12s;
}
.refresh-btn:hover:not(:disabled) {
  color: #00d4ff;
  border-color: rgba(0, 212, 255, 0.4);
}
.refresh-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.filters-nav-btn {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  position: relative;
}
.filters-nav-btn--active {
  color: #00d4ff;
  border-color: rgba(0, 212, 255, 0.5);
  background: rgba(0, 212, 255, 0.12);
}

.filter-active-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #00d4ff;
  flex-shrink: 0;
}

.airports-nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.airports-nav-btn--active {
  color: #34d399;
  border-color: rgba(52, 211, 153, 0.5);
  background: rgba(52, 211, 153, 0.1);
}

/* ── Celebrity Dropdown ──────────────────────────── */
.celeb-dropdown-wrapper {
  position: relative;
}

.celeb-nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.celeb-nav-btn--active {
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.5);
  background: rgba(245, 158, 11, 0.1);
}

.celeb-nav-badge {
  font-size: 10px;
  font-weight: 700;
  background: rgba(245, 158, 11, 0.15);
  border: 1px solid rgba(245, 158, 11, 0.4);
  color: #f59e0b;
  border-radius: 10px;
  padding: 1px 6px;
}

.celeb-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  width: 300px;
  background: #06111f;
  border: 1px solid #1e3a5f;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);
  z-index: 200;
  overflow: hidden;
}

.celeb-dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #f59e0b;
  letter-spacing: 0.04em;
  border-bottom: 1px solid #1e3a5f;
}

.celeb-dropdown-close {
  background: none;
  border: none;
  color: #475569;
  cursor: pointer;
  font-size: 13px;
  padding: 2px 4px;
  border-radius: 4px;
}
.celeb-dropdown-close:hover { color: #94a3b8; }

.celeb-dropdown-empty {
  padding: 20px 14px;
  font-size: 12px;
  color: #334155;
  text-align: center;
}

.celeb-dropdown-list {
  list-style: none;
  margin: 0;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 280px;
  overflow-y: auto;
}

.celeb-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #0d2039;
  border: 1px solid transparent;
  transition: border-color 0.15s, background 0.15s;
  cursor: pointer;
}
.celeb-dropdown-item:hover {
  border-color: rgba(245, 158, 11, 0.3);
  background: #112540;
}

.celeb-dropdown-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1e3a5f, #0d2039);
  border: 1px solid rgba(245, 158, 11, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #f59e0b;
  flex-shrink: 0;
}

.celeb-dropdown-info {
  flex: 1;
  min-width: 0;
}

.celeb-dropdown-name {
  font-size: 13px;
  font-weight: 600;
  color: #f1f5f9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.celeb-dropdown-sub {
  font-size: 10px;
  color: #475569;
  margin-top: 1px;
}

.celeb-dropdown-badge {
  font-size: 9px;
  font-weight: 700;
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 20px;
  padding: 2px 6px;
  white-space: nowrap;
  flex-shrink: 0;
}

.celeb-dropdown-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 10px;
  color: #334155;
  border-top: 1px solid #1e3a5f;
}

.celeb-dropdown-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  opacity: 0.6;
  flex-shrink: 0;
}

/* ── Filter Modal ────────────────────────────────── */
.filter-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  font-family: Inter, system-ui, sans-serif;
}

.filter-modal {
  background: #06111f;
  border: 1px solid rgba(0, 212, 255, 0.14);
  border-radius: 14px;
  width: 500px;
  max-width: calc(100vw - 2rem);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(0, 212, 255, 0.06);
  overflow: hidden;
}

.filter-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem 0.75rem;
  border-bottom: 1px solid rgba(0, 212, 255, 0.1);
}

.filter-modal-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 0.02em;
}

.filter-modal-close {
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 6px;
  color: #64748b;
  font-size: 0.75rem;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}
.filter-modal-close:hover {
  color: #e2e8f0;
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(148, 163, 184, 0.15);
}

.filter-modal-body {
  padding: 0.75rem 1.25rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.fm-section {
  padding: 0.65rem 0;
}

.fm-divider {
  height: 1px;
  background: rgba(0, 212, 255, 0.08);
  margin: 0;
}

.fm-label {
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #475569;
  margin-bottom: 0.55rem;
}

.fm-btn-row {
  display: flex;
  gap: 0.3rem;
  flex-wrap: wrap;
}

.fm-btn {
  background: rgba(0, 212, 255, 0.04);
  border: 1px solid rgba(0, 212, 255, 0.14);
  border-radius: 6px;
  color: #64748b;
  font-size: 0.75rem;
  font-weight: 500;
  padding: 0.28rem 0.7rem;
  cursor: pointer;
  transition: all 0.12s;
  font-family: inherit;
  white-space: nowrap;
}
.fm-btn:hover {
  color: #94a3b8;
  border-color: rgba(0, 212, 255, 0.3);
  background: rgba(0, 212, 255, 0.07);
}
.fm-btn--active {
  background: rgba(0, 212, 255, 0.12);
  border-color: rgba(0, 212, 255, 0.45);
  color: #00d4ff;
  font-weight: 600;
}
.fm-btn--mil.fm-btn--active {
  background: rgba(255, 68, 68, 0.12);
  border-color: rgba(255, 68, 68, 0.45);
  color: #ff6b6b;
}
.fm-btn--ladd.fm-btn--active {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.45);
  color: #f59e0b;
}
.fm-btn--pia.fm-btn--active {
  background: rgba(168, 85, 247, 0.12);
  border-color: rgba(168, 85, 247, 0.45);
  color: #a855f7;
}
.fm-btn--celebrity.fm-btn--active {
  background: rgba(251, 191, 36, 0.12);
  border-color: rgba(251, 191, 36, 0.45);
  color: #fbbf24;
}
.fm-btn--hazards.fm-btn--active {
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.45);
  color: #f87171;
}
.fm-btn--radar.fm-btn--active {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.45);
  color: #4ade80;
}

.fm-slider-label-row {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.fm-slider-controls {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.fm-slider {
  flex: 1;
  accent-color: #00d4ff;
  cursor: pointer;
  height: 4px;
  min-width: 0;
}

.fm-number-input {
  width: 68px;
  background: #0d2039;
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 6px;
  color: #00d4ff;
  font-size: 0.78rem;
  font-family: inherit;
  font-variant-numeric: tabular-nums;
  padding: 0.22rem 0.4rem;
  text-align: right;
  outline: none;
  transition: border-color 0.15s;
  -moz-appearance: textfield;
}
.fm-number-input::-webkit-inner-spin-button,
.fm-number-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.fm-number-input:focus {
  border-color: rgba(0, 212, 255, 0.5);
}

.fm-unit {
  font-size: 0.7rem;
  color: #475569;
  min-width: 22px;
}

.fm-count {
  font-size: 0.78rem;
  font-weight: 600;
  color: #00d4ff;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.fm-count-of {
  color: #334155;
  font-weight: 400;
}

.fm-label-hint {
  font-size: 0.65rem;
  font-weight: 400;
  color: #475569;
  text-transform: none;
  letter-spacing: 0;
  margin-left: 4px;
}

.fm-footer {
  padding-top: 0.75rem;
  border-top: 1px solid rgba(0, 212, 255, 0.08);
  margin-top: 0.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.fm-footer-actions {
  display: flex;
  gap: 8px;
}

.fm-reset-btn {
  background: rgba(248, 113, 113, 0.08);
  border: 1px solid rgba(248, 113, 113, 0.28);
  border-radius: 6px;
  color: #f87171;
  font-size: 0.75rem;
  font-weight: 500;
  padding: 0.3rem 0.9rem;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.fm-reset-btn:hover {
  background: rgba(248, 113, 113, 0.18);
  border-color: rgba(248, 113, 113, 0.45);
}

.fm-cancel-btn {
  background: rgba(100, 116, 139, 0.1);
  border: 1px solid rgba(100, 116, 139, 0.3);
  border-radius: 6px;
  color: #94a3b8;
  font-size: 0.75rem;
  font-weight: 500;
  padding: 0.3rem 0.9rem;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.fm-cancel-btn:hover {
  background: rgba(100, 116, 139, 0.2);
}

.fm-save-btn {
  background: rgba(0, 212, 255, 0.12);
  border: 1px solid rgba(0, 212, 255, 0.4);
  border-radius: 6px;
  color: #00d4ff;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.3rem 1.1rem;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.fm-save-btn:hover {
  background: rgba(0, 212, 255, 0.22);
  border-color: rgba(0, 212, 255, 0.65);
}


.no-flights-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 500;
  background: rgba(3, 10, 22, 0.85);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 212, 255, 0.12);
  border-radius: 10px;
  padding: 1rem 1.5rem;
  color: #475569;
  font-family: Inter, system-ui, sans-serif;
  font-size: 0.85rem;
  pointer-events: none;
}

.sidebar-wrapper {
  position: relative;
  display: flex;
  flex-shrink: 0;
  transition: width 0.3s ease;
}

.sidebar-wrapper--left {
  flex-direction: row;
}

.sidebar-wrapper--right {
  flex-direction: row-reverse;
}

.sidebar-wrapper.collapsed {
  width: 0 !important;
  overflow: visible;
}

.sidebar-wrapper.collapsed > :not(.sidebar-toggle) {
  display: none;
}

.sidebar-toggle {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 48px;
  background: #0d2137;
  border: 1px solid #1e3a5f;
  color: #4a9eff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
  transition:
    background 0.15s,
    color 0.15s;
}

.sidebar-toggle:hover {
  background: #1e3a5f;
  color: #fff;
}

.sidebar-toggle--left {
  right: -20px;
  border-left: none;
  border-radius: 0 6px 6px 0;
}

.sidebar-toggle--right {
  left: -20px;
  border-right: none;
  border-radius: 6px 0 0 6px;
}

.map-area {
  flex: 1;
  position: relative;
  min-width: 0;
}

.overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #030a16;
  color: #00d4ff;
  font-size: 13px;
  font-family: Inter, system-ui, sans-serif;
  letter-spacing: 0.08em;
}

.overlay--error {
  flex-direction: column;
  gap: 8px;
  color: #f87171;
  font-size: 0.85rem;
  text-align: center;
  padding: 2rem;
}

.overlay-retry-btn {
  margin-top: 8px;
  padding: 6px 18px;
  background: none;
  border: 1px solid #f87171;
  border-radius: 5px;
  color: #f87171;
  font-size: 0.78rem;
  cursor: pointer;
  transition: background 0.15s;
}
.overlay-retry-btn:hover {
  background: rgba(248, 113, 113, 0.12);
}

.flight-info-panel {
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
  font-family: Inter, system-ui, sans-serif;
}

.fi-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.3rem;
}
.fi-callsign {
  font-size: 1rem;
  font-weight: 700;
  color: #e2e8f0;
  flex: 1;
}
.fi-badge {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  background: rgba(74, 144, 254, 0.15);
  color: #4a9eff;
  border: 1px solid rgba(74, 144, 254, 0.3);
}
.fi-badge--mil {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.3);
}
.fi-badge--ladd {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.3);
}
.fi-badge--pia {
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
  border-color: rgba(168, 85, 247, 0.3);
}

.fi-close {
  background: none;
  border: none;
  color: #475569;
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  padding: 0;
}
.fi-close:hover {
  color: #e2e8f0;
}

.fi-route {
  color: #4a9eff;
  font-size: 0.85rem;
  margin-bottom: 0.4rem;
}
.fi-celebrity-name {
  color: #fbbf24;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}
.fi-route {
  color: #4a9eff;
  font-size: 0.85rem;
  margin-bottom: 0.4rem;
}
.fi-celebrity-name {
  color: #fbbf24;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}
.fi-route {
  color: #4a9eff;
  font-size: 0.85rem;
  margin-bottom: 0.4rem;
}
.fi-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.78rem;
  color: #94a3b8;
}

.weather-overlay {
  position: absolute;
  top: 60px;
  right: 14px;
  background: rgba(6, 17, 31, 0.88);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #cbd5e1;
  z-index: 500;
  pointer-events: auto;
}
.clear-weather-btn {
  background: none;
  border: none;
  color: #475569;
  font-size: 11px;
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
  order: 99;
}
.clear-weather-btn:hover { color: #94a3b8; }
.weather-overlay-condition { font-weight: 600; color: #f1f5f9; }
.weather-overlay-temp { color: #4a9eff; font-weight: 700; }
.weather-overlay-wind { color: #94a3b8; }

.sigmet-legend {
  position: absolute;
  bottom: 30px;
  right: 20px;
  background: rgba(6, 17, 31, 0.85);
  border: 1px solid #1e3a5f;
  border-radius: 12px;
  padding: 15px;
  backdrop-filter: blur(10px);
  z-index: 1000;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
  pointer-events: none;
}

.legend-title {
  color: #94a3b8;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 10px 0;
  border-bottom: 1px solid #1e3a5f;
  padding-bottom: 6px;
}

.legend-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e2e8f0;
  font-size: 0.8rem;
  font-family: Inter, system-ui, sans-serif;
}

.legend-color {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  display: inline-block;
  box-shadow: 0 0 5px rgba(255, 255, 255, 0.2);
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
.fi-watchlist-btn:hover {
  background: rgba(0, 212, 255, 0.15);
}
.fi-watchlist-btn--remove {
  background: rgba(248, 113, 113, 0.08);
  border-color: rgba(248, 113, 113, 0.3);
  color: #f87171;
}
.fi-watchlist-btn--remove:hover {
  background: rgba(248, 113, 113, 0.15);
}

.fi-history-btn {
  margin-top: 0.4rem;
  width: 100%;
  padding: 0.3rem 0.75rem;
  background: rgba(0, 212, 255, 0.05);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 6px;
  color: #475569;
  font-size: 0.78rem;
  cursor: pointer;
  font-family: inherit;
  transition:
    background 0.15s,
    color 0.15s,
    border-color 0.15s;
}
.fi-history-btn:hover {
  background: rgba(0, 212, 255, 0.1);
  color: #94a3b8;
  border-color: rgba(0, 212, 255, 0.35);
}
.fi-history-btn--active {
  background: rgba(0, 212, 255, 0.12);
  border-color: rgba(0, 212, 255, 0.4);
  color: #00d4ff;
}

.fi-history {
  margin-top: 0.5rem;
  border-top: 1px solid #1e3a5f;
  padding-top: 0.5rem;
}
.fi-history-msg {
  font-size: 0.78rem;
  color: #475569;
  font-style: italic;
}
.fi-history-msg--empty {
  color: #334155;
}
.fi-history-msg--ok {
  color: #22c55e;
  font-style: normal;
}
</style>
