# Frontend Unit Tests

## Setup

Dependencies must be installed before running tests:

```bash
cd frontend
npm install
```

## Running Tests

```bash
npm test
```

## Test Files

### `dashboard.test.js`

Tests for the `Dashboard.vue` component.

| Test | Description |
|------|-------------|
| renders successfully | Component mounts without errors |
| shows empty hint when watchlist is empty | Displays prompt to add flights when watchlist is empty |
| renders watchlist flight callsign and route | Watchlist entries show callsign, origin, and destination |
| filters watchlist by callsign search | Search input filters flights by callsign |
| filters watchlist by origin | Search input filters flights by origin airport code |
| filters watchlist by destination | Search input filters flights by destination airport code |
| shows no-results message when search matches nothing | Displays message when search query has no matches |
| emits select-flight when a watchlist row is clicked | Clicking a flight row emits `select-flight` with the flight object |
| emits remove-from-watchlist when remove button is clicked | Clicking the remove button emits `remove-from-watchlist` |
| shows selected flight stats in Route Details panel | Speed and coordinates are displayed when a flight is selected |
| shows empty details state when no flight is selected | Displays placeholder text when no flight is selected |

### `mapcontainer.test.js`

Tests for the `MapContainer.vue` component. Leaflet is mocked to avoid DOM limitations in the test environment.

| Test | Description |
|------|-------------|
| loads map component | Component mounts without errors |
| renders the map container and map div | `.map-container` and `.map` elements are present in the DOM |
| mounts with a flights prop | Component handles a non-empty flights array |
| mounts with a watchlisted flight | Component handles a flight present in both flights and watchlist |
| mounts with a selected flight | Component handles a selectedFlight prop |

### `homeview.test.js`

Tests for the `HomeView.vue` component. Axios is mocked and child components (`Dashboard`, `MapContainer`) are stubbed.

| Test | Description |
|------|-------------|
| renders homepage | Component mounts without errors |
| shows loading overlay on initial render | Loading overlay is visible before the API call resolves |
| shows error overlay when API call fails | Error message is displayed when the API returns an error |
| hides loading overlay after successful fetch | Loading overlay is gone after flights are fetched |
| shows MapContainer after successful fetch | MapContainer is rendered in the DOM after a successful fetch |
| filters out flights missing position data | Flights without latitude/longitude are excluded from the flights list |

## Framework & Tools

- **[Vitest](https://vitest.dev/)** — test runner
- **[@vue/test-utils](https://test-utils.vuejs.org/)** — Vue component mounting and interaction utilities
- **[jsdom](https://github.com/jsdom/jsdom)** — browser-like DOM environment for tests

