# Database Controller — Documentation

## Overview

The DB layer adds persistent local storage to the flight tracker. The frontend always reads from MySQL — it never calls an external API directly.

**Three independent data sources, each callable on its own:**

| Layer | Prefix | Source | Purpose |
|-------|--------|--------|---------|
| DB CRUD + utilities | `/api/db/*` | MySQL | Read/write local data and provide DB-only helpers (import, search, cached summaries) |
| AeroAPI proxy | `/api/flights/*`, `/api/airports/*` | FlightAware AeroAPI | Historical flights, routes, airport details |
| airplanes.live proxy | `/api/live/*` | airplanes.live (free ADS-B) | Real-time live position, no auth needed |

---

## File Map

```
backend/src/main/java/com/example/demo/
├── DbController.java            ← REST endpoints at /api/db/*
├── DbService.java               ← Business logic and orchestration
├── FlightService.java           ← FlightAware AeroAPI proxy (called by DbService)
├── AirplanesLiveService.java    ← airplanes.live API proxy (called by DbService)
├── AirplanesLiveController.java ← Direct airplanes.live endpoints at /api/live/*
│
├── entity/
│   ├── Celebrity.java           ← maps to `celebrities` table
│   ├── Aircraft.java            ← maps to `aircraft` table (includes live position fields)
│   ├── Airport.java             ← maps to `airports` table (String PK)
│   ├── Flight.java              ← maps to `flights` table
│   └── AppUser.java             ← maps to `users` table
│
└── repository/
    ├── CelebrityRepository.java
    ├── AircraftRepository.java
    ├── AirportRepository.java
    ├── FlightRepository.java
    └── UserRepository.java
```

---

## Architecture

**No background scheduler.** All API calls happen on-demand when an endpoint is hit. This avoids rate limit issues and keeps the system predictable.

Four patterns for getting data:

### 1. DB-only (fast, no external calls)
```
Frontend → GET /api/db/aircraft → MySQL
```
Returns whatever is stored. Note that the aircraft "live position" columns are not currently populated via an exposed `/api/db/save-live*` route in this codebase; they are only written by the `DbService` helper methods (which currently are not wired to `DbController` endpoints).

### 2. DB-first fallback chain (historical data + live fallback)
```
GET /api/db/fetch-and-save/{ident}
  → Check DB first (return if found)
  → FlightAware AeroAPI (save airports + flights to DB if found)
  → airplanes.live (save aircraft row if found, return live position)
  → "none" if not found anywhere
```
NOTE: `DbController` does not currently expose `GET /api/db/fetch-and-save/{ident}`. The fallback chain logic exists as `DbService.fetchAndSaveFlight`, but it is currently not routed.

### 3. Direct API proxy (no DB involvement)
```
GET /api/live/reg/{reg}    ← airplanes.live directly
GET /api/flights/{ident}   ← FlightAware AeroAPI directly
```

### 4. Live → DB snapshot (manual trigger)
```
GET /api/db/save-live/{tailNumber}
  → Calls airplanes.live, writes position fields to aircraft row in MySQL
  → Frontend then reads from GET /api/db/aircraft (no external call needed)
```
NOTE: `DbController` does not currently expose `/api/db/save-live/{tailNumber}` or `/api/db/save-live-all`. The underlying code path is implemented in `DbService.saveLivePosition`, but it is currently not wired to routes.

---

## Database Schema

```sql
celebrities  (id, name, industry)

aircraft     (id, tail_number UNIQUE, model, owner_id → celebrities.id,
              -- live position fields:
              -- These columns are mapped by JPA (`Aircraft.java`) and may be added/updated at runtime by Hibernate (`spring.jpa.hibernate.ddl-auto=update`).
              airborne, last_lat, last_lon, last_altitude, last_speed,
              last_heading, last_callsign, last_hex, last_seen)

airports     (ap_code PK, name, city, country)

flights      (id, aircraft_id → aircraft.id,
              origin_ap_code → airports.ap_code,
              destination_ap_code → airports.ap_code,
              departure_time, arrival_time, estimated_co2_kg)

users        (u_id, uname, upass)
```

- `airports` uses a string primary key (`ap_code`, e.g. `KLAX`). All others use auto-increment `BIGINT`.
- `aircraft.owner_id` is a nullable FK to `celebrities`.
- Live position fields are `null` until `save-live` has been called for that aircraft.

---

## REST Endpoints — `/api/db` (DB + orchestration)

### Status
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/db/status` | Row counts for all 5 tables + `"status":"ok"` |

### CRUD (same pattern for all entities)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/db/{entity}` | List all records |
| GET | `/api/db/{entity}/{id}` | Get one record (404 if not found) |
| POST | `/api/db/{entity}` | Create or update (upsert — include `id`/`apCode` to update) |
| DELETE | `/api/db/{entity}/{id}` | Delete by ID (204 No Content) |

`{entity}` = `celebrities` / `aircraft` / `airports` / `flights` / `users`
For airports, `{id}` is the string airport code (e.g. `KLAX`).
For users, the ID field in JSON is `uId`.
For updates: `POST /api/db/{entity}` uses JPA `save(...)`, so updates generally require the JSON to include the entity's primary key field.
In particular, for `aircraft` the stable identity is `id` (not `tailNumber`), while `tailNumber` is only unique data used for lookup in some `DbService` flows.

### Fetch, search, import
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/db/import` | Bulk JSON import (see format below) |
| POST | `/api/db/flights/batch` | Best-effort bulk save flights; infers airports/aircraft rows as needed |
| GET | `/api/db/search/flights?tailNumber=N628TS` | Search flights by tail number |
| GET | `/api/db/flights/cache?minutes=180` | Summary of recent flights stored in DB (DB-only); parses `liveInfoJson` when present |

### Live position — not currently routed via `DbController`
Some live-position helper logic exists in `DbService` (for example, `fetchLivePosition` and `saveLivePosition`), but `DbController` currently does not expose the corresponding HTTP routes.
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/db/live/{tailNumber}` | Intended: airplanes.live lookup enriched with DB owner/model (no DB write) |
| GET | `/api/db/live-all` | Intended: iterate aircraft in DB and enrich with live results |
| GET | `/api/db/save-live/{tailNumber}` | Intended: persist live position fields to `aircraft` row |
| GET | `/api/db/save-live-all` | Intended: persist live position fields for all tracked aircraft |

---

## REST Endpoints — `/api/live` (airplanes.live direct proxy)

No DB involved. Useful for testing connectivity or one-off lookups.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/live/reg/{reg}` | Live position by tail number |
| GET | `/api/live/hex/{hex}` | Live position by ICAO hex ID |
| GET | `/api/live/callsign/{callsign}` | Live position by callsign |
| GET | `/api/live/point/{lat}/{lon}/{radius}` | All aircraft within radius (nautical miles, max 250) |
| GET | `/api/live/mil` | Currently airborne military aircraft |
| GET | `/api/live/ladd` | FAA LADD program aircraft |
| GET | `/api/live/pia` | Privacy ICAO address aircraft |

**Test connectivity:** `GET /api/live/mil` — returns a large list if the API is reachable.

---

## REST Endpoints — `/api/flights` (FlightAware AeroAPI proxy)

Full proxy to FlightAware AeroAPI — historical flight data, routes, operator info. Requires `AEROAPI_KEY` in `backend/.env`.

Key endpoints:
- `GET /api/flights/{ident}` — historical flights for a tail number
- `GET /api/airports/{id}` — airport details
- `GET /api/aeroapi/status` — check if API key is configured

---

## DB-First Fetch and Save (`/api/db/fetch-and-save/{ident}`)
NOTE: This endpoint is documented historically, but it is not currently implemented in `DbController.java`. The related implementation lives in `DbService.fetchAndSaveFlight`.

Three-step fallback chain:

```
1. Check DB → if aircraft + flights exist, return { source: "database", aircraft, flights }

2. Call FlightAware AeroAPI GET /flights/{ident}
   → If flights found: upsert airports + aircraft + flights to DB, return { source: "aeroapi", ... }
   → If no flights:

3. Call airplanes.live GET /reg/{ident}
   → If airborne: upsert aircraft to DB, return { source: "airplanes.live", aircraft, livePosition }
   → If not found: return { source: "none", message: "..." }
```

**Response `source` values:** `"database"` / `"aeroapi"` / `"airplanes.live"` / `"none"`

---

## JSON Import Format (`POST /api/db/import`)

```json
{
  "celebrities": [
    { "name": "Elon Musk", "industry": "Tech" }
  ],
  "aircraft": [
    { "tailNumber": "N628TS", "model": "G650ER", "ownerId": 1 }
  ],
  "airports": [
    { "apCode": "KLAX", "name": "Los Angeles International", "city": "Los Angeles", "country": "US" }
  ],
  "flights": [
    {
      "aircraftId": 1,
      "originApCode": "KLAX",
      "destinationApCode": "KTEB",
      "departureTime": "2024-01-15T10:00:00",
      "arrivalTime": "2024-01-15T14:30:00",
      "estimatedCo2Kg": 4200.0
    }
  ]
}
```

**Import order:** celebrities → aircraft → airports → flights (FK order).

---

## curl Examples

```bash
# Check DB status
curl http://localhost:8080/api/db/status

# Add a celebrity
curl -X POST http://localhost:8080/api/db/celebrities \
  -H "Content-Type: application/json" \
  -d '{"name": "Taylor Swift", "industry": "Music"}'

# Fetch flight history + save (3-step chain: DB → AeroAPI → airplanes.live)
# Search what is already saved in DB for this tail number
curl "http://localhost:8080/api/db/search/flights?tailNumber=N1980K"

# Get all aircraft (may include live position columns only if they were written by backend code paths)
curl http://localhost:8080/api/db/aircraft

# Get a cached summary of recent flights stored in DB
curl "http://localhost:8080/api/db/flights/cache?minutes=180"

# Direct airplanes.live — no DB (connectivity test)
curl http://localhost:8080/api/live/mil
curl http://localhost:8080/api/live/reg/N1980K

# Search flights in DB by tail number
curl "http://localhost:8080/api/db/search/flights?tailNumber=N628TS"

# Bulk import
curl -X POST http://localhost:8080/api/db/import \
  -H "Content-Type: application/json" \
  -d @backend/db/celebrity_seed.json

# Delete a celebrity (id=1)
curl -X DELETE http://localhost:8080/api/db/celebrities/1
```

---

## Common Workflows

**Workflow 1 — Seed celebrities + aircraft**
1. Open DB Explorer → JSON Import panel
2. Paste contents of `backend/db/celebrity_seed.json` → Import JSON

**Workflow 2 — Save flight snapshots into DB**
1. Collect flight markers/records on the frontend (or from another upstream source).
2. Send them to `POST /api/db/flights/batch` as a JSON array (best-effort persistence).
3. Use `GET /api/db/flights/cache` to render a DB-only "recent flights" view.

**Workflow 3 — Real-time check without touching DB**
Use the direct airplanes.live proxy and treat the DB as optional enrichment:
- `GET /api/live/reg/{tailNumber}` — raw airplanes.live result (no DB write)

**Workflow 4 — Fetch historical flight data**
1. Call the FlightAware AeroAPI proxy directly (example: `GET /api/flights/{ident}`).
2. Persist results into DB only if your frontend (or a future backend route) converts the response into the `Flight` entity shape and calls `POST /api/db/flights/batch` or `POST /api/db/{entity}`.

**Workflow 5 — Frontend live map**
1. Seed the DB (Workflow 1)
2. For live rendering, prefer direct airplanes.live endpoints (`/api/live/*`) and only use DB data for enrichment (e.g., mapping tail number -> celebrity/owner).
3. If/when live-position columns in `aircraft` are populated by backend code paths, the frontend can also read those fields from `GET /api/db/aircraft`.

**Workflow 6 — Link aircraft to a celebrity**
1. Find or create the celebrity (note their `id`)
2. Find or create the aircraft (note their `id`)
3. Edit the aircraft record → set `ownerId` to the celebrity's `id` → Update
