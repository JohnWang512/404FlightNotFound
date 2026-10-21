<script setup>
import { ref } from 'vue'
import axios from 'axios'

const emit = defineEmits(['authenticated'])

const mode = ref('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')

function switchMode(nextMode) {
  mode.value = nextMode
  error.value = ''
  success.value = ''
  password.value = ''
  confirmPassword.value = ''
}

async function submit() {
  error.value = ''
  success.value = ''

  const uname = username.value.trim()
  const upass = password.value

  if (!uname || !upass) {
    error.value = 'Username and password are required.'
    return
  }

  if (mode.value === 'register' && upass !== confirmPassword.value) {
    error.value = 'Passwords do not match.'
    return
  }

  loading.value = true
  try {
    const endpoint = mode.value === 'login' ? '/api/auth/login' : '/api/auth/register'
    const response = await axios.post(endpoint, { username: uname, password: upass })
    success.value = mode.value === 'login' ? 'Login successful.' : 'Account created successfully.'
    emit('authenticated', response.data?.user ?? { uname })
  } catch (err) {
    error.value = err?.response?.data?.error ?? 'Authentication request failed.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <h1>Flight Tracker Login</h1>
      <p class="subtitle">Sign in to continue, or create a new account.</p>

      <div class="mode-switch" role="tablist" aria-label="Login mode">
        <button
          type="button"
          :class="{ active: mode === 'login' }"
          @click="switchMode('login')"
        >
          Login
        </button>
        <button
          type="button"
          :class="{ active: mode === 'register' }"
          @click="switchMode('register')"
        >
          Create Account
        </button>
      </div>

      <form @submit.prevent="submit" class="login-form">
        <label for="username">Username</label>
        <input id="username" v-model="username" autocomplete="username" />

        <label for="password">Password</label>
        <input id="password" v-model="password" type="password" autocomplete="current-password" />

        <template v-if="mode === 'register'">
          <label for="confirmPassword">Confirm Password</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
          />
        </template>

        <button type="submit" :disabled="loading">
          {{ loading ? 'Please wait...' : mode === 'login' ? 'Login' : 'Create Account' }}
        </button>
      </form>

      <p v-if="error" class="message error">{{ error }}</p>
      <p v-if="success" class="message success">{{ success }}</p>

      <p class="hint">Default account: username <strong>admin</strong>, password <strong>1234</strong></p>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 15% 20%, #f6edff 0%, transparent 40%),
    radial-gradient(circle at 85% 85%, #e5f7f0 0%, transparent 35%),
    linear-gradient(160deg, #f5f8ff 0%, #fff8f0 100%);
}

.login-card {
  width: min(460px, 95vw);
  background: #ffffff;
  border: 1px solid #d8deeb;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(27, 39, 72, 0.14);
  padding: 28px;
}

.login-card h1 {
  margin: 0;
  color: #1f2a44;
}

.subtitle {
  color: #49587a;
  margin-bottom: 16px;
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 18px;
}

.mode-switch button {
  border: 1px solid #b8c4e6;
  background: #f8fbff;
  color: #324268;
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
}

.mode-switch button.active {
  border-color: #2d5ec4;
  background: #e7f0ff;
  color: #153067;
  font-weight: 600;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login-form label {
  color: #2b3a60;
  font-size: 0.92rem;
  margin-top: 6px;
}

.login-form input {
  border: 1px solid #bcc7df;
  border-radius: 10px;
  padding: 10px;
  font-size: 1rem;
}

.login-form button {
  margin-top: 14px;
  border: none;
  border-radius: 10px;
  padding: 11px;
  background: linear-gradient(120deg, #2056c4 0%, #2f7ab8 100%);
  color: white;
  font-weight: 600;
  cursor: pointer;
}

.login-form button:disabled {
  opacity: 0.75;
  cursor: not-allowed;
}

.message {
  margin-top: 14px;
  margin-bottom: 0;
  font-size: 0.95rem;
}

.message.error {
  color: #c1213f;
}

.message.success {
  color: #176643;
}

.hint {
  margin-top: 14px;
  color: #4f5f84;
  font-size: 0.9rem;
}
</style>
