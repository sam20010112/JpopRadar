<template>
  <div class="concerts-view">
    <h1>All Concerts</h1>

    <div class="filters">
      <div class="search-wrap">
        <svg class="search-icon" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="8.5" cy="8.5" r="5.5" stroke="currentColor" stroke-width="1.6"/>
          <path d="M13 13L17 17" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
        </svg>
        <input
          v-model="query"
          type="text"
          placeholder="Search artist, title, or venue…"
          class="search-input"
        />
      </div>

      <!-- Month dropdown -->
      <select v-if="availableMonths.length" v-model="selectedMonth" class="month-select">
        <option :value="null">All Months</option>
        <option v-for="m in availableMonths" :key="m.value" :value="m.value">{{ m.label }}</option>
      </select>

      <!-- Ticket status dropdown -->
      <select v-model="statusFilter" class="month-select">
        <option value="">All Tickets</option>
        <option value="available">Available</option>
        <option value="soldout">Sold Out</option>
      </select>
    </div>

    <div class="filter-meta">
      <span class="result-count">{{ sortedConcerts.length }} concert{{ sortedConcerts.length !== 1 ? 's' : '' }} found</span>
      <button v-if="hasFilters" class="clear-btn" @click="clearAll">Clear filters</button>
    </div>

    <div v-if="dateQuery" class="date-chip">
      Filtering by date: {{ dateQuery }}
      <button class="chip-clear" @click="clearDateFilter">×</button>
    </div>

    <p v-if="store.loading">Loading... <br>Backend may take a few minutes to cold start... <br>Please refresh later...</p>
    <p v-else-if="store.error" class="error">{{ store.error }}</p>
    <div v-else-if="sortedConcerts.length === 0">No concerts found.</div>
    <div v-else class="concert-list">
      <ConcertCard v-for="concert in sortedConcerts" :key="concert.id" :concert="concert" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConcertStore } from '../stores/concertStore'
import ConcertCard from '../components/ConcertCard.vue'

const store = useConcertStore()
const route = useRoute()
const router = useRouter()
const query = ref('')
const selectedMonth = ref(null)
const statusFilter = ref('')
const dateQuery = ref(route.query.date ?? '')

function clearDateFilter() {
  dateQuery.value = ''
  router.replace({ query: { ...route.query, date: undefined } })
}

function clearAll() {
  query.value = ''
  selectedMonth.value = null
  statusFilter.value = ''
  clearDateFilter()
}

const hasFilters = computed(() =>
  query.value !== '' || selectedMonth.value !== null || statusFilter.value !== '' || dateQuery.value !== ''
)

const dateRe = /\d{4}\.\d{2}\.\d{2}/

function parseDate(concert) {
  const match = concert.dates.find((d) => dateRe.test(d))
  return match ? match.replace(/\./g, '-') : ''
}

function concertMonth(concert) {
  const match = concert.dates.find((d) => dateRe.test(d))
  if (!match) return null
  const [y, m] = match.split('.')
  return `${y}-${m}`
}

onMounted(() => {
  store.fetchScan()
})

const availableMonths = computed(() => {
  const seen = new Set()
  const months = []
  const sorted = [...store.scanConcerts].sort((a, b) => parseDate(a).localeCompare(parseDate(b)))
  for (const c of sorted) {
    const key = concertMonth(c)
    if (key && !seen.has(key)) {
      seen.add(key)
      const [y, m] = key.split('-')
      const label = new Date(`${y}-${m}-01`).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
      months.push({ value: key, label })
    }
  }
  return months
})

const sortedConcerts = computed(() => {
  const q = query.value.trim().toLowerCase()
  let list = store.scanConcerts
  if (q) {
    list = list.filter((c) =>
      c.artist.toLowerCase().includes(q) ||
      (c.title ?? '').toLowerCase().includes(q) ||
      (c.venue ?? '').toLowerCase().includes(q)
    )
  }
  if (selectedMonth.value) {
    list = list.filter((c) => concertMonth(c) === selectedMonth.value)
  }
  if (statusFilter.value === 'available') {
    list = list.filter((c) => c.ticket_status !== 'SOLD OUT')
  } else if (statusFilter.value === 'soldout') {
    list = list.filter((c) => c.ticket_status === 'SOLD OUT')
  }
  if (dateQuery.value) {
    const dateKey = dateQuery.value.replaceAll('-', '.')
    list = list.filter((c) => c.dates.some((d) => d.startsWith(dateKey)))
  }
  return [...list].sort((a, b) => parseDate(a).localeCompare(parseDate(b)))
})
</script>

<style scoped>
.filters {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.search-wrap {
  position: relative;
  flex: 1;
  min-width: 200px;
  max-width: 340px;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: #9999b3;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 0.5rem 0.875rem 0.5rem 2.25rem;
  background: #1c1c28;
  border: 1px solid #2a2a3a;
  border-radius: 8px;
  color: #e8e8f0;
  font-size: 0.875rem;
  transition: border-color 0.15s;
  outline: none;
}

.search-input::placeholder {
  color: #9999b3;
}

.search-input:focus {
  border-color: #7c3aed;
}

.month-select {
  padding: 0.5rem 2rem 0.5rem 0.875rem;
  background: #1c1c28;
  border: 1px solid #2a2a3a;
  border-radius: 8px;
  color: #e8e8f0;
  font-size: 0.875rem;
  cursor: pointer;
  outline: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%239999b3' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  transition: border-color 0.15s;
}

.month-select:focus {
  border-color: #7c3aed;
}

.filter-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.25rem;
}

.result-count {
  font-size: 0.8rem;
  color: #9999b3;
}

.clear-btn {
  background: none;
  border: 1px solid #2a2a3a;
  border-radius: 6px;
  color: #a78bfa;
  font-size: 0.8rem;
  padding: 0.25rem 0.625rem;
  cursor: pointer;
  transition: border-color 0.15s;
}

.clear-btn:hover {
  border-color: #a78bfa;
}

.date-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #2a1a4a;
  border: 1px solid #a78bfa66;
  color: #e2c0ff;
  font-size: 0.875rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  margin-bottom: 1.25rem;
}

.chip-clear {
  background: none;
  border: none;
  color: #a78bfa;
  font-size: 1rem;
  cursor: pointer;
  line-height: 1;
  padding: 0;
}
</style>
