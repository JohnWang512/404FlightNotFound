<script setup>
/**
 * App.vue — root component, responsible for:
 *  1. Authentication gate: shows LoginView until the user is verified.
 *  2. Session persistence: stores the authenticated user in localStorage so
 *     page refreshes don't force a re-login.
 *  3. Shared state: provides currentUser and userFlightHistory to all
 *     descendants via Vue's provide/inject so child components don't need
 *     prop drilling through intermediate layers.
 */
import { computed, ref, provide, onMounted } from 'vue'
import LoginView from './views/LoginView.vue'

const AUTH_USER_KEY = 'flight_tracker_auth_user'

// VITE_DISABLE_LOGIN=true skips the auth gate entirely (used in local dev/testing)
const loginDisabled = String(import.meta.env.VITE_DISABLE_LOGIN ?? 'false').toLowerCase() === 'true'

const currentUser = ref(null)
// Keyed by celebrityId → { celebrity, flights, flightCount, lastFetchedAt, … }
// Pre-loaded at login so CelebrityTracker can show history instantly without extra API calls
const userFlightHistory = ref({})

// Restore session from a previous login before the app renders
if (loginDisabled) {
  currentUser.value = { uname: 'testing-mode' }
} else {
  const saved = localStorage.getItem(AUTH_USER_KEY)
  if (saved) {
    try {
      currentUser.value = JSON.parse(saved)
    } catch {
      // Corrupt storage entry — clear it so the user can log in cleanly
      localStorage.removeItem(AUTH_USER_KEY)
    }
  }
}

// Provide auth state and flight history to all child components (CelebrityTracker, etc.)
provide('currentUser', currentUser)
provide('userFlightHistory', userFlightHistory)

async function loadUserHistory(userId) {
  if (!userId) return
  try {
    const res = await fetch(`/api/db/users/${userId}/celebrity-history`)
    if (!res.ok) return
    const list = await res.json()
    // Convert array to a map keyed by celebrityId for O(1) lookups in CelebrityTracker
    const map = {}
    for (const entry of list) {
      map[entry.celebrityId] = entry
    }
    userFlightHistory.value = map
  } catch {
    // silent — UI falls back to live fetch on demand
  }
}

const isAuthenticated = computed(() => loginDisabled || !!currentUser.value)

function handleAuthenticated(user) {
  currentUser.value = user
  if (!loginDisabled) {
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
  }
  // Pre-load celebrity history immediately after login so it's ready when the tracker opens
  loadUserHistory(user?.uId)
}



onMounted(() => {
  // Restore history for a user already logged-in from a previous session
  if (currentUser.value?.uId) {
    loadUserHistory(currentUser.value.uId)
  }
})
</script>

<template>
  <RouterView v-if="isAuthenticated" />
  <LoginView v-else @authenticated="handleAuthenticated" />
</template>
