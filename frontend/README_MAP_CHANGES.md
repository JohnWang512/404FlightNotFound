# Map Feature Changes

This file summarizes the frontend map work completed so far.

## What Was Added

- Added a Leaflet map container component in `src/components/MapContainer.vue`.
- Initialized the map with OpenStreetMap tiles and a U.S.-centered default view.
- Rendered sample flight markers with popup details:
  - Callsign
  - Altitude
  - Velocity

## Marker Icon Fix

- Removed default Leaflet marker image imports:
  - `marker-icon-2x.png`
  - `marker-icon.png`
  - `marker-shadow.png`
- Removed `L.Icon.Default.mergeOptions(...)`.
- Replaced default markers with a custom `L.divIcon` using an inline SVG airplane (top-down style).
- Styled the icon in a professional dark navy color and set it to ~`32x32`.

## Result

- Marker icons no longer break due to missing asset paths.
- Flights now display with consistent custom airplane icons.
