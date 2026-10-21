<script setup>
import { ref, onMounted, watch, computed } from "vue";
import axios from "axios";

/**
 * DbExplorer is a developer/admin-style UI for interacting with the backend DB.
 *
 * Why this view is complex:
 * - It supports multiple tables with different primary-key shapes (numeric ids vs string codes).
 * - It includes several "tools" that attempt to orchestrate API->DB workflows.
 *
 * Important contract note (for documentation clarity):
 * - This UI is written against a set of `/api/db/*` routes that may not all be implemented in the
 *   current `backend/src/main/java/com/example/demo/DbController.java`.
 * - For routes that look mismatched, we keep the UI code as-is but we document expected behavior
 *   in comments so it’s obvious where integration wiring would be needed.
 */
const tabs = [
  { key: "celebrities", label: "Celebrities" },
  { key: "aircraft", label: "Aircraft" },
  { key: "airports", label: "Airports" },
  { key: "flights", label: "Flights" },
  { key: "users", label: "Users" },
];

const columnDefs = {
  celebrities: ["id", "name", "industry"],
  aircraft: ["id", "tailNumber", "model", "ownerId"],
  airports: ["apCode", "name", "city", "country"],
  flights: [
    "id",
    "aircraftId",
    "originApCode",
    "destinationApCode",
    "departureTime",
    "arrivalTime",
    "estimatedCo2Kg",
  ],
  users: ["uId", "uname"],
};

// Field definitions for add/edit forms
const fieldDefs = {
  celebrities: [
    { key: "name", label: "Name", type: "text" },
    { key: "industry", label: "Industry", type: "text" },
  ],
  aircraft: [
    {
      key: "tailNumber",
      label: "Tail Number",
      type: "text",
      placeholder: "e.g. N628TS",
    },
    { key: "model", label: "Model", type: "text", placeholder: "e.g. G650" },
    {
      key: "ownerId",
      label: "Owner ID",
      type: "number",
      placeholder: "Celebrity ID (optional)",
    },
  ],
  airports: [
    {
      key: "apCode",
      label: "Airport Code",
      type: "text",
      placeholder: "e.g. KLAX",
    },
    { key: "name", label: "Name", type: "text" },
    { key: "city", label: "City", type: "text" },
    { key: "country", label: "Country", type: "text", placeholder: "e.g. US" },
  ],
  flights: [
    { key: "aircraftId", label: "Aircraft ID", type: "number" },
    {
      key: "originApCode",
      label: "Origin Code",
      type: "text",
      placeholder: "e.g. KLAX",
    },
    {
      key: "destinationApCode",
      label: "Destination Code",
      type: "text",
      placeholder: "e.g. KTEB",
    },
    { key: "departureTime", label: "Departure Time", type: "datetime-local" },
    { key: "arrivalTime", label: "Arrival Time", type: "datetime-local" },
    {
      key: "estimatedCo2Kg",
      label: "Est. CO₂ (kg)",
      type: "number",
      step: "0.01",
    },
  ],
  users: [{ key: "uname", label: "Username", type: "text" }],
};

const activeTab = ref("celebrities");
const items = ref([]);
const loading = ref(false);
const error = ref(null);
const status = ref(null);

// --- IMPORT LOGIC ---
const importResult = ref(null);

/**
 * Why `filteredImportResult` deletes `errors`:
 * - The UI renders a grid of per-table counts; showing raw `errors[]` in that grid would be confusing.
 * - We keep the original `importResult.errors` in state, so we can still display it in the detailed
 *   error section below (when present).
 */
const filteredImportResult = computed(() => {
  if (!importResult.value) return {};
  const result = { ...importResult.value };
  delete result.errors;
  return result;
});

const importJson = ref("");
const importError = ref(null);
const importLoading = ref(false);

const fetchIdent = ref("");
const fetchResult = ref(null);
const fetchError = ref(null);
const fetchLoading = ref(false);

const searchTailNumber = ref("");
const searchResults = ref(null);
const searchLoading = ref(false);
const searchError = ref(null);

const directEndpoint = ref("reg");
const directIdent = ref("");
const directResult = ref(null);
const directError = ref(null);
const directLoading = ref(false);

const saveLiveIdent = ref("");
const saveLiveResult = ref(null);
const saveLiveError = ref(null);
const saveLiveLoading = ref(false);

const saveAllError = ref(null);
const saveAllResult = ref(null);
const saveAllLoading = ref(false);

const showAddModal = ref(false);
const showEditModal = ref(false);
const formItem = ref({});

async function fetchStatus() {
  try {
    const res = await axios.get("/api/db/status");
    status.value = res.data;
  } catch (e) {
    console.error("Status fetch failed", e);
  }
}

async function loadTab(tabKey) {
  loading.value = true;
  error.value = null;
  try {
    const res = await axios.get(`/api/db/${tabKey}`);
    items.value = Array.isArray(res.data) ? res.data : [];
  } catch (e) {
    error.value = `Failed to load ${tabKey}: ${e.message}`;
  } finally {
    loading.value = false;
  }
}

watch(activeTab, (newTab) => {
  loadTab(newTab);
});

function openAdd() {
  formItem.value = {};
  showAddModal.value = true;
}

function openEdit(item) {
  formItem.value = { ...item };
  showEditModal.value = true;
}

async function handleAdd() {
  try {
    await axios.post(`/api/db/${activeTab.value}`, formItem.value);
    showAddModal.value = false;
    await Promise.all([loadTab(activeTab.value), fetchStatus()]);
  } catch (e) {
    alert("Save failed: " + e.message);
  }
}

async function handleEdit() {
  try {
    await axios.post(`/api/db/${activeTab.value}`, formItem.value);
    showEditModal.value = false;
    await Promise.all([loadTab(activeTab.value), fetchStatus()]);
  } catch (e) {
    alert("Update failed: " + e.message);
  }
}

async function handleDelete(id) {
  if (!confirm("Are you sure?")) return;
  try {
    await axios.delete(`/api/db/${activeTab.value}/${id}`);
    await Promise.all([loadTab(activeTab.value), fetchStatus()]);
  } catch (e) {
    alert("Delete failed: " + e.message);
  }
}

async function runImport() {
  importError.value = null;
  importResult.value = null;
  importLoading.value = true;
  try {
    const payload = JSON.parse(importJson.value);
    const res = await axios.post("/api/db/import", payload);
    importResult.value = res.data;
    importJson.value = "";
    await Promise.all([loadTab(activeTab.value), fetchStatus()]);
  } catch (e) {
    importError.value =
      e.name === "SyntaxError" ? "Invalid JSON format" : e.message;
  } finally {
    importLoading.value = false;
  }
}

async function runFetch() {
  /**
   * Fetch & Save tool (intended):
   * - The UI expects an endpoint that takes an `ident` and performs an external fetch + DB persistence.
   *
   * Backend mismatch to be aware of:
   * - Current backend `DbController` does not define `/api/db/fetch-save`.
   * - A related DB-first chain exists as a helper `DbService.fetchAndSaveFlight(...)` but is not currently routed.
   */
  fetchError.value = null;
  fetchResult.value = null;
  fetchLoading.value = true;
  try {
    const res = await axios.get(`/api/db/fetch-save?ident=${fetchIdent.value}`);
    fetchResult.value = res.data;
    await Promise.all([loadTab(activeTab.value), fetchStatus()]);
  } catch (e) {
    fetchError.value = e.response
      ? e.response.data.error || e.message
      : e.message;
  } finally {
    fetchLoading.value = false;
  }
}

async function runSearch() {
  searchError.value = null;
  searchResults.value = null;
  searchLoading.value = true;
  try {
    const res = await axios.get(
      `/api/db/search/flights?tailNumber=${searchTailNumber.value}`,
    );
    searchResults.value = res.data;
  } catch (e) {
    searchError.value = e.message;
  } finally {
    searchLoading.value = false;
  }
}

async function runDirect() {
  /**
   * Direct live proxy tool (intended):
   * - The UI expects an `/api/airplanes-live/...` style route.
   *
   * Backend mismatch:
   * - Current backend exposes airplanes.live proxies at `/api/live/*` (see `AirplanesLiveController`),
   *   with paths like `/api/live/reg/{reg}` and `/api/live/hex/{hex}`.
   *
   * This comment is here so testers understand why calls may 404 unless the route is wired.
   */
  directError.value = null;
  directResult.value = null;
  directLoading.value = true;
  try {
    const res = await axios.get(
      `/api/airplanes-live/${directEndpoint.value}/${directIdent.value}`,
    );
    directResult.value = res.data;
  } catch (e) {
    directError.value = e.message;
  } finally {
    directLoading.value = false;
  }
}

async function runSaveLive() {
  /**
   * Save live position tool (intended):
   * - Should call an endpoint that snapshots airplanes.live data into `aircraft` live columns.
   *
   * Backend mismatch:
   * - Current backend `DbController` does not define `/api/db/save-live` or `/api/db/save-live-all`.
   * - The underlying implementation lives in `DbService.saveLivePosition(...)` (not wired to routes).
   */
  saveLiveError.value = null;
  saveLiveResult.value = null;
  saveLiveLoading.value = true;
  try {
    const res = await axios.get(
      `/api/db/save-live?tailNumber=${saveLiveIdent.value}`,
    );
    saveLiveResult.value = res.data;
    await Promise.all([loadTab(activeTab.value), fetchStatus()]);
  } catch (e) {
    saveLiveError.value = e.response
      ? `${e.response.status} — ${JSON.stringify(e.response.data)}`
      : e.message;
  } finally {
    saveLiveLoading.value = false;
  }
}

async function runSaveAll() {
  /**
   * Save all live positions tool (intended):
   * - Should iterate tracked aircraft and persist their latest live snapshot.
   *
   * Backend mismatch:
   * - Current backend `DbController` does not define `/api/db/save-live-all`.
   * - Integration would need to add/route an endpoint to call the service-side implementation.
   */
  saveAllError.value = null;
  saveAllResult.value = null;
  saveAllLoading.value = true;
  try {
    const res = await axios.get("/api/db/save-live-all", { timeout: 120000 });
    saveAllResult.value = Array.isArray(res.data) ? res.data : [];
    await Promise.all([loadTab(activeTab.value), fetchStatus()]);
  } catch (e) {
    saveAllError.value = e.response
      ? `${e.response.status} — ${JSON.stringify(e.response.data)}`
      : e.message;
  } finally {
    saveAllLoading.value = false;
  }
}

function formatCellValue(val) {
  if (val === null || val === undefined) return "—";
  return String(val);
}

onMounted(async () => {
  await fetchStatus();
  await loadTab(activeTab.value);
});
</script>

<template>
  <div class="db-explorer">
    <header class="explorer-header">
      <h1>Database Explorer</h1>
      <div v-if="status" class="db-stats">
        <span v-for="(count, key) in status" :key="key" class="stat-badge">
          <strong>{{ count }}</strong> {{ key }}
        </span>
      </div>
    </header>

    <div class="explorer-grid">
      <div class="explorer-main">
        <nav class="tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            :class="['tab-btn', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </nav>

        <div class="table-actions">
          <button class="btn-primary" @click="openAdd">
            Add {{ activeTab }}
          </button>
          <button class="btn-secondary" @click="loadTab(activeTab)">
            Refresh
          </button>
        </div>

        <div class="table-container">
          <div v-if="loading" class="state-msg">Loading data...</div>
          <div v-else-if="error" class="state-msg error">{{ error }}</div>
          <table v-else>
            <thead>
              <tr>
                <th v-for="col in columnDefs[activeTab]" :key="col">
                  {{ col }}
                </th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in items"
                :key="item.id || item.apCode || item.uId"
              >
                <td v-for="col in columnDefs[activeTab]" :key="col">
                  {{ formatCellValue(item[col]) }}
                </td>
                <td>
                  <div class="row-btns">
                    <button
                      class="btn-icon"
                      @click="openEdit(item)"
                      title="Edit"
                    >
                      ✎
                    </button>
                    <button
                      class="btn-icon delete"
                      @click="handleDelete(item.id || item.apCode || item.uId)"
                      title="Delete"
                    >
                      🗑
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="items.length === 0">
                <td
                  :colspan="columnDefs[activeTab].length + 1"
                  class="empty-row"
                >
                  No records found.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <aside class="explorer-tools">
        <section class="tool-card">
          <h3>Bulk JSON Import</h3>
          <textarea
            v-model="importJson"
            class="json-textarea"
            placeholder='{"celebrities": [...], "aircraft": [...], "airports": [...], "flights": [...]}'
            rows="8"
          ></textarea>
          <div class="import-actions">
            <button
              class="btn-primary"
              @click="runImport"
              :disabled="importLoading || !importJson.trim()"
            >
              {{ importLoading ? "Importing..." : "Import JSON" }}
            </button>
            <button
              class="btn-ghost"
              @click="
                importJson = '';
                importResult = null;
                importError = null;
              "
            >
              Clear
            </button>
          </div>
          <div v-if="importError" class="state-msg error">
            {{ importError }}
          </div>
          <div v-if="importResult" class="import-result">
            <div class="result-header">Import complete</div>
            <div class="result-grid">
              <div
                class="result-item"
                v-for="(count, key) in filteredImportResult"
                :key="key"
              >
                <span class="result-key">{{ key }}</span>
                <span class="result-val">{{ count }} saved</span>
              </div>
            </div>
            <div
              v-if="importResult.errors && importResult.errors.length > 0"
              class="import-errors"
            >
              <div class="result-header error">
                Errors ({{ importResult.errors.length }})
              </div>
              <ul>
                <li v-for="(err, i) in importResult.errors" :key="i">
                  {{ err }}
                </li>
              </ul>
            </div>
          </div>
        </section>

        <section class="tool-card">
          <h3>Fetch & Save (AeroAPI)</h3>
          <div class="tool-input-group">
            <input
              v-model="fetchIdent"
              placeholder="Ident or Reg (e.g. DAL123)"
              @keyup.enter="runFetch"
            />
            <button @click="runFetch" :disabled="fetchLoading || !fetchIdent">
              Go
            </button>
          </div>
          <div v-if="fetchError" class="state-msg error">{{ fetchError }}</div>
          <div v-if="fetchResult" class="fetch-result">
            <p><strong>Source:</strong> {{ fetchResult.source }}</p>
            <p v-if="fetchResult.aircraft">
              <strong>Aircraft:</strong>
              {{ fetchResult.aircraft.tailNumber }} ({{
                fetchResult.aircraft.model
              }})
            </p>
            <p v-if="fetchResult.flights">
              <strong>Flights saved:</strong> {{ fetchResult.flights.length }}
            </p>
            <p v-if="fetchResult.message" class="info-msg">
              {{ fetchResult.message }}
            </p>
          </div>
        </section>

        <section class="tool-card">
          <h3>Save Live Position</h3>
          <p class="tool-desc">
            Fetch current position from airplanes.live and write to aircraft
            table.
          </p>
          <div class="tool-input-group">
            <input
              v-model="saveLiveIdent"
              placeholder="Tail Number (e.g. N628TS)"
              @keyup.enter="runSaveLive"
            />
            <button
              @click="runSaveLive"
              :disabled="saveLiveLoading || !saveLiveIdent"
            >
              Save
            </button>
          </div>
          <div v-if="saveLiveError" class="state-msg error">
            {{ saveLiveError }}
          </div>
          <div v-if="saveLiveResult" class="fetch-result">
            <p v-if="saveLiveResult.saved">
              <strong>Success!</strong> {{ saveLiveResult.tailNumber }} is
              {{ saveLiveResult.airborne ? "airborne" : "on ground" }}.
            </p>
            <p v-if="saveLiveResult.message" class="info-msg">
              {{ saveLiveResult.message }}
            </p>
          </div>

          <div class="divider">OR</div>

          <button
            class="btn-full"
            @click="runSaveAll"
            :disabled="saveAllLoading"
          >
            {{ saveAllLoading ? "Processing..." : "Sync All Live Aircraft" }}
          </button>
          <div v-if="saveAllError" class="state-msg error">
            {{ saveAllError }}
          </div>
          <div v-if="saveAllResult" class="fetch-result">
            <p>
              <strong>Processed:</strong> {{ saveAllResult.length }} aircraft
              synced.
            </p>
          </div>
        </section>

        <section class="tool-card">
          <h3>Search Saved Flights</h3>
          <div class="tool-input-group">
            <input
              v-model="searchTailNumber"
              placeholder="Tail Number"
              @keyup.enter="runSearch"
            />
            <button
              @click="runSearch"
              :disabled="searchLoading || !searchTailNumber"
            >
              Search
            </button>
          </div>
          <div v-if="searchError" class="state-msg error">
            {{ searchError }}
          </div>
          <div v-if="searchResults" class="search-results">
            <div v-for="f in searchResults" :key="f.id" class="search-item">
              #{{ f.id }}: {{ f.originApCode }} ➔ {{ f.destinationApCode }}
            </div>
            <p v-if="searchResults.length === 0">
              No flights found for this aircraft.
            </p>
          </div>
        </section>

        <section class="tool-card">
          <h3>Airplanes.live Proxy</h3>
          <div class="tool-input-row">
            <select v-model="directEndpoint">
              <option value="reg">Reg</option>
              <option value="hex">Hex</option>
              <option value="callsign">Callsign</option>
            </select>
            <input
              v-model="directIdent"
              placeholder="Ident"
              @keyup.enter="runDirect"
            />
            <button
              @click="runDirect"
              :disabled="directLoading || !directIdent"
            >
              Go
            </button>
          </div>
          <pre v-if="directResult" class="raw-json">{{
            JSON.stringify(directResult, null, 2)
          }}</pre>
          <div v-if="directError" class="state-msg error">
            {{ directError }}
          </div>
        </section>
      </aside>
    </div>

    <div
      v-if="showAddModal || showEditModal"
      class="modal-overlay"
      @click.self="showAddModal = showEditModal = false"
    >
      <div class="modal-content">
        <h2>{{ showAddModal ? "Add" : "Edit" }} {{ activeTab }}</h2>
        <form @submit.prevent="showAddModal ? handleAdd() : handleEdit()">
          <div
            v-for="field in fieldDefs[activeTab]"
            :key="field.key"
            class="form-group"
          >
            <label>{{ field.label }}</label>
            <input
              v-model="formItem[field.key]"
              :type="field.type"
              :step="field.step"
              :placeholder="field.placeholder"
              required
            />
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn-ghost"
              @click="showAddModal = showEditModal = false"
            >
              Cancel
            </button>
            <button type="submit" class="btn-primary">Save Changes</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* (All previous styles remain identical) */
.db-explorer {
  padding: 2rem;
  max-width: 1600px;
  margin: 0 auto;
}
.explorer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}
.db-stats {
  display: flex;
  gap: 1rem;
}
.stat-badge {
  background: #f1f5f9;
  padding: 0.5rem 1rem;
  border-radius: 2rem;
  font-size: 0.9rem;
}
.explorer-grid {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 2rem;
  align-items: start;
}
.tabs {
  display: flex;
  border-bottom: 2px solid #e2e8f0;
  margin-bottom: 1.5rem;
}
.tab-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  background: none;
  cursor: pointer;
  font-weight: 600;
  color: #64748b;
  transition: all 0.2s;
}
.tab-btn:hover {
  color: #3b82f6;
}
.tab-btn.active {
  color: #3b82f6;
  border-bottom: 2px solid #3b82f6;
  margin-bottom: -2px;
}
.table-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.table-container {
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow-x: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
th {
  background: #f8fafc;
  padding: 1rem;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
}
td {
  padding: 1rem;
  border-bottom: 1px solid #f1f5f9;
}
.empty-row {
  text-align: center;
  padding: 3rem;
  color: #94a3b8;
}
.row-btns {
  display: flex;
  gap: 0.5rem;
}
.btn-icon {
  background: #f1f5f9;
  border: none;
  border-radius: 4px;
  width: 32px;
  height: 32px;
  cursor: pointer;
}
.btn-icon:hover {
  background: #e2e8f0;
}
.btn-icon.delete:hover {
  background: #fee2e2;
  color: #ef4444;
}
.tool-card {
  background: white;
  padding: 1.5rem;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 1.5rem;
}
.tool-card h3 {
  margin-top: 0;
  margin-bottom: 1rem;
  font-size: 1.1rem;
  color: #1e293b;
}
.tool-desc {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 1rem;
}
.tool-input-group {
  display: flex;
  gap: 0.5rem;
}
.tool-input-group input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}
.tool-input-row {
  display: flex;
  gap: 0.5rem;
}
.tool-input-row select {
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}
.json-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.85rem;
  margin-bottom: 1rem;
  resize: vertical;
}
.import-actions {
  display: flex;
  justify-content: space-between;
}
.import-result {
  margin-top: 1rem;
  background: #f0fdf4;
  padding: 1rem;
  border-radius: 4px;
}
.result-header {
  font-weight: 700;
  color: #166534;
  margin-bottom: 0.5rem;
}
.result-header.error {
  color: #991b1b;
}
.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}
.result-item {
  display: flex;
  flex-direction: column;
}
.result-key {
  font-size: 0.75rem;
  text-transform: uppercase;
  color: #64748b;
}
.result-val {
  font-weight: 600;
}
.import-errors {
  margin-top: 1rem;
  border-top: 1px solid #dcfce7;
  padding-top: 1rem;
}
.import-errors ul {
  padding-left: 1.25rem;
  margin: 0.5rem 0 0;
  font-size: 0.85rem;
  color: #991b1b;
}
.raw-json {
  background: #1e293b;
  color: #38bdf8;
  padding: 1rem;
  border-radius: 4px;
  font-size: 0.75rem;
  overflow: auto;
  max-height: 300px;
  margin-top: 1rem;
}
.state-msg {
  padding: 1rem;
  text-align: center;
  color: #64748b;
}
.state-msg.error {
  color: #ef4444;
  background: #fef2f2;
}
.divider {
  text-align: center;
  margin: 1rem 0;
  font-size: 0.75rem;
  color: #94a3b8;
  position: relative;
}
.divider::before,
.divider::after {
  content: "";
  position: absolute;
  top: 50%;
  width: 40%;
  height: 1px;
  background: #e2e8f0;
}
.divider::before {
  left: 0;
}
.divider::after {
  right: 0;
}
.btn-primary {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-secondary {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
}
.btn-ghost {
  background: none;
  border: none;
  color: #64748b;
  padding: 0.5rem;
  cursor: pointer;
}
.btn-full {
  width: 100%;
  padding: 0.75rem;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
}
.btn-full:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #3b82f6;
  color: #3b82f6;
}
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 0.5rem;
  width: 100%;
  max-width: 500px;
}
.form-group {
  margin-bottom: 1rem;
}
.form-group label {
  display: block;
  margin-bottom: 0.4rem;
  font-size: 0.9rem;
  font-weight: 600;
}
.form-group input {
  width: 100%;
  padding: 0.6rem;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
}
</style>
