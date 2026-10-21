import axios from "axios";

const WATCHLIST_KEY = "ft_watchlist";

export function getUserId(currentUserRef) {
  const id = currentUserRef?.value?.uId;
  return Number.isFinite(id) ? id : null;
}

export async function loadTrackedFlights(currentUserRef) {
  const userId = getUserId(currentUserRef);
  if (!userId) {
    return JSON.parse(localStorage.getItem(WATCHLIST_KEY) || "[]");
  }

  const res = await axios.get(`/api/db/users/${userId}/tracked-flights`);
  return Array.isArray(res.data) ? res.data : [];
}

export async function saveTrackedFlight(currentUserRef, flight) {
  const userId = getUserId(currentUserRef);
  if (!userId) {
    const existing = JSON.parse(localStorage.getItem(WATCHLIST_KEY) || "[]");
    const idx = existing.findIndex((f) => f.callsign === flight.callsign);
    if (idx === -1) {
      existing.push(flight);
    } else {
      existing[idx] = flight;
    }
    localStorage.setItem(WATCHLIST_KEY, JSON.stringify(existing));
    return;
  }

  await axios.post(`/api/db/users/${userId}/tracked-flights`, flight);
}

export async function removeTrackedFlight(currentUserRef, callsign) {
  const userId = getUserId(currentUserRef);
  if (!userId) {
    const existing = JSON.parse(localStorage.getItem(WATCHLIST_KEY) || "[]");
    const next = existing.filter((f) => f.callsign !== callsign);
    localStorage.setItem(WATCHLIST_KEY, JSON.stringify(next));
    return;
  }

  await axios.delete(
    `/api/db/users/${userId}/tracked-flights/${encodeURIComponent(callsign)}`,
  );
}

export async function clearTrackedFlights(currentUserRef) {
  const userId = getUserId(currentUserRef);
  if (!userId) {
    localStorage.removeItem(WATCHLIST_KEY);
    return;
  }

  await axios.delete(`/api/db/users/${userId}/tracked-flights`);
}
