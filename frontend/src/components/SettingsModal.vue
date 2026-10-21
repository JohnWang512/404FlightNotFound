<script setup>
import { ref, inject } from 'vue'

const emit = defineEmits(['close'])

const currentUser = inject('currentUser')
const AUTH_USER_KEY = 'flight_tracker_auth_user'

const usernameForm = ref({ newUsername: '', currentPassword: '' })
const passwordForm = ref({ newPassword: '', currentPassword: '' })
const usernameMsg = ref(null)
const passwordMsg = ref(null)
const usernameLoading = ref(false)
const passwordLoading = ref(false)

async function changeUsername() {
  usernameMsg.value = null
  usernameLoading.value = true
  try {
    const res = await fetch('/api/auth/update', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: currentUser.value.uId,
        currentPassword: usernameForm.value.currentPassword,
        newUsername: usernameForm.value.newUsername
      })
    })
    const data = await res.json()
    if (data.ok) {
      currentUser.value = { ...currentUser.value, uname: data.user.uname }
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(currentUser.value))
      usernameMsg.value = { type: 'success', text: 'Username updated.' }
      usernameForm.value = { newUsername: '', currentPassword: '' }
    } else {
      usernameMsg.value = { type: 'error', text: data.error }
    }
  } catch {
    usernameMsg.value = { type: 'error', text: 'Request failed.' }
  } finally {
    usernameLoading.value = false
  }
}

async function changePassword() {
  passwordMsg.value = null
  passwordLoading.value = true
  try {
    const res = await fetch('/api/auth/update', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: currentUser.value.uId,
        currentPassword: passwordForm.value.currentPassword,
        newPassword: passwordForm.value.newPassword
      })
    })
    const data = await res.json()
    if (data.ok) {
      passwordMsg.value = { type: 'success', text: 'Password updated.' }
      passwordForm.value = { newPassword: '', currentPassword: '' }
    } else {
      passwordMsg.value = { type: 'error', text: data.error }
    }
  } catch {
    passwordMsg.value = { type: 'error', text: 'Request failed.' }
  } finally {
    passwordLoading.value = false
  }
}

function logout() {
  localStorage.removeItem(AUTH_USER_KEY)
  currentUser.value = null
}
</script>

<template>
  <div class="settings-overlay" @click.self="emit('close')">
    <div class="settings-modal">
      <div class="settings-header">
        <span class="settings-title">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#00d4ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
          Settings
        </span>
        <button class="settings-close" @click="emit('close')">✕</button>
      </div>

      <div class="settings-user-badge">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#00d4ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        Logged in as <strong>{{ currentUser?.uname }}</strong>
      </div>

      <div class="settings-section">
        <div class="settings-section-label">Change Username</div>
        <div class="settings-form">
          <input
            v-model="usernameForm.newUsername"
            class="settings-input"
            type="text"
            placeholder="New username"
            autocomplete="off"
          />
          <input
            v-model="usernameForm.currentPassword"
            class="settings-input"
            type="password"
            placeholder="Current password"
            autocomplete="current-password"
          />
          <div v-if="usernameMsg" :class="['settings-msg', usernameMsg.type]">
            {{ usernameMsg.text }}
          </div>
          <button
            class="settings-btn"
            :disabled="usernameLoading || !usernameForm.newUsername || !usernameForm.currentPassword"
            @click="changeUsername"
          >
            {{ usernameLoading ? 'Saving…' : 'Update Username' }}
          </button>
        </div>
      </div>

      <div class="settings-section">
        <div class="settings-section-label">Change Password</div>
        <div class="settings-form">
          <input
            v-model="passwordForm.newPassword"
            class="settings-input"
            type="password"
            placeholder="New password"
            autocomplete="new-password"
          />
          <input
            v-model="passwordForm.currentPassword"
            class="settings-input"
            type="password"
            placeholder="Current password"
            autocomplete="current-password"
          />
          <div v-if="passwordMsg" :class="['settings-msg', passwordMsg.type]">
            {{ passwordMsg.text }}
          </div>
          <button
            class="settings-btn"
            :disabled="passwordLoading || !passwordForm.newPassword || !passwordForm.currentPassword"
            @click="changePassword"
          >
            {{ passwordLoading ? 'Saving…' : 'Update Password' }}
          </button>
        </div>
      </div>

      <div class="settings-footer">
        <button class="settings-btn logout" @click="logout">Log Out</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2100;
  font-family: Inter, system-ui, sans-serif;
}

.settings-modal {
  background: #06111f;
  border: 1px solid rgba(0, 212, 255, 0.14);
  border-radius: 14px;
  width: 380px;
  max-width: calc(100vw - 2rem);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(0, 212, 255, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem 0.75rem;
  border-bottom: 1px solid rgba(0, 212, 255, 0.1);
}

.settings-title {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 0.02em;
}

.settings-close {
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
.settings-close:hover {
  color: #e2e8f0;
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(148, 163, 184, 0.15);
}

.settings-user-badge {
  margin: 0.85rem 1.25rem 0;
  font-size: 0.78rem;
  color: #64748b;
  background: rgba(0, 212, 255, 0.05);
  border: 1px solid rgba(0, 212, 255, 0.1);
  border-radius: 8px;
  padding: 0.5rem 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.settings-user-badge strong {
  color: #00d4ff;
  font-weight: 600;
}

.settings-section {
  padding: 0.85rem 1.25rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.settings-section-label {
  font-size: 0.67rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #475569;
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.settings-input {
  background: #0d2039;
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 8px;
  color: #e2e8f0;
  font-size: 0.82rem;
  font-family: inherit;
  padding: 0.5rem 0.75rem;
  outline: none;
  transition: border-color 0.15s;
  width: 100%;
  box-sizing: border-box;
}
.settings-input:focus {
  border-color: rgba(0, 212, 255, 0.45);
}
.settings-input::placeholder {
  color: #334155;
}

.settings-msg {
  font-size: 0.75rem;
  padding: 0.35rem 0.6rem;
  border-radius: 6px;
}
.settings-msg.success {
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.25);
  color: #4ade80;
}
.settings-msg.error {
  background: rgba(248, 113, 113, 0.1);
  border: 1px solid rgba(248, 113, 113, 0.25);
  color: #f87171;
}

.settings-btn {
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.25);
  border-radius: 8px;
  color: #00d4ff;
  cursor: pointer;
  font-size: 0.8rem;
  font-weight: 500;
  font-family: inherit;
  padding: 0.5rem 1rem;
  text-align: center;
  transition: background 0.15s, border-color 0.15s, opacity 0.15s;
}
.settings-btn:hover:not(:disabled) {
  background: rgba(0, 212, 255, 0.16);
  border-color: rgba(0, 212, 255, 0.45);
}
.settings-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.settings-btn.logout {
  background: rgba(248, 113, 113, 0.08);
  border-color: rgba(248, 113, 113, 0.28);
  color: #f87171;
  width: 100%;
}
.settings-btn.logout:hover {
  background: rgba(248, 113, 113, 0.18);
  border-color: rgba(248, 113, 113, 0.5);
}

.settings-footer {
  padding: 0.85rem 1.25rem 1.25rem;
  border-top: 1px solid rgba(0, 212, 255, 0.08);
  margin-top: 0.85rem;
}
</style>
