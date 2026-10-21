# Frontend

## Tech Stack

| Tool | Purpose |
|------|---------|
| Vue 3 | UI framework (Composition API / `<script setup>`) |
| Vite | Dev server & bundler |
| Vue Router 4 | Client-side routing |
| Axios | HTTP requests to the backend API |
| Leaflet | Interactive map rendering |

## Project Structure

```
frontend/
├── src/
│   ├── main.js               # App entry point
│   ├── App.vue               # Root component
│   ├── style.css             # Global styles
│   ├── router/
│   │   └── index.js          # Route definitions
│   ├── views/
│   │   ├── HomeView.vue        # Live flight tracking (main state + polling)
│   │   ├── FlightApiTest.vue   # Manual FlightAware AeroAPI testing page
│   │   ├── ApiExplorer.vue     # Interactive backend API request builder
│   │   └── DbExplorer.vue      # DB CRUD and tooling UI
│   ├── components/
│   │   ├── Dashboard.vue         # Watchlist + flight details panel
│   │   ├── MapContainer.vue      # Leaflet map + marker/popups
│   │   └── CelebrityTracker.vue # Placeholder celebrity list/detail UI
│   ├── assets/
│   │   ├── logo.png
│   │   └── vue.svg
│   └── __tests__/
│       ├── Frontend-Unit-Tests.md
│       ├── dashboard.test.js
│       ├── dashboard.behavior.test.js
│       ├── homeview.test.js
│       ├── homeview.structure.test.js
│       ├── mapcontainer.test.js
│       ├── mapcontainer.props.test.js
│       └── DbExplorer.test.js
├── public/                   # Static assets
├── index.html
├── vite.config.js
├── package.json
└── Dockerfile
```

## Routes

| Path | Component | Description |
|------|-----------|-------------|
| `/` | `HomeView` | Live flight tracking (polling + DB cache fallback + map + dashboard) |
| `/flight_api` | `FlightApiTest` | Manual FlightAware AeroAPI testing page |
| `/api-explorer` | `ApiExplorer` | Interactive backend API request builder |
| `/db-explorer` | `DbExplorer` | DB CRUD and tooling UI |
| `/*` | — | Redirects to `/` |

## Key Features

- **DB-first caching flow** — before calling FlightAware, the app tries `GET /api/db/flights/cache?minutes=180` so repeated refreshes don't hammer upstream APIs.
- **FlightAware search + persistence** — if cache is empty, the app calls `/api/flights/search?query=...` and then saves marker records into the DB via `POST /api/db/flights/batch`.
- **Rate-limit handling** — on `429`, the app retries with exponential backoff and (if needed) falls back to a longer DB cache window.
- **Interactive map** — Leaflet map centered on the U.S. with custom SVG airplane markers (rotation is computed from destination coordinates when available).
- **Dashboard sidebar** — watchlist (searchable) + selected flight details (coordinates + optional FlightAware details).
- **Polling loop** — refresh cadence is implemented in `HomeView.vue` (tuned for demo stability).

## Dataflow (high-level)

`HomeView.vue` intentionally centralizes this flow because it coordinates rate-limited API calls and keeps the UI responsive:

1. Read DB cache: `GET /api/db/flights/cache?minutes=180`
2. If empty, fetch from FlightAware via backend proxy: `GET /api/flights/search?...`
3. Transform results into the UI marker shape `{ callsign, origin, destination, lat, lng, ... }`
4. Enrich popups/details with per-flight calls: `GET /api/flights/{ident}`
5. Persist marker records: `POST /api/db/flights/batch`

## Getting Started

```bash
# Install dependencies
npm install

# Start dev server (http://localhost:5173)
npm run dev

# Build for production
npm run build
```

## Testing

This project uses **Vitest** for Vue component tests.

Run:
```bash
cd frontend
npm test
```

Mocking strategy (what tests intentionally avoid):
- **Axios** is mocked so tests don't hit real FlightAware/AeroAPI or the real backend DB.
- **Leaflet** is mocked in `src/__tests__/mapcontainer.test.js` because JSDOM can't render/execute real Leaflet map logic.
- View tests may stub child components (e.g., `MapContainer`, `Dashboard`) so assertions focus on state transitions and props wiring.

Per-file notes: [`src/__tests__/Frontend-Unit-Tests.md`](src/__tests__/Frontend-Unit-Tests.md)

## Docker

```bash
docker build -t frontend .
docker run -p 5173:5173 frontend
```

