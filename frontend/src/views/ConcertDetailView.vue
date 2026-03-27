<template>
  <div class="concert-detail">
    <RouterLink class="back-link" to="/concerts">← Back to Concerts</RouterLink>

    <p v-if="store.loading">Loading...</p>
    <p v-else-if="store.error" class="error">{{ store.error }}</p>

    <div v-else-if="concert" class="detail-wrapper">

      <!-- Header -->
      <div class="detail-header">
        <h1>{{ concert.artist }}</h1>
        <p class="concert-title">{{ concert.title }}</p>
        <span v-if="concert.ticket_status === 'SOLD OUT'" class="sold-out">SOLD OUT</span>
      </div>

      <div class="detail-body">

      <!-- Venue & Dates -->
      <div class="detail-section">
        <h2 class="section-heading">Venue &amp; Dates</h2>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">Venue</span>
            <span class="info-value">{{ concert.venue }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Date(s)</span>
            <span class="info-value">{{ formattedDates }}</span>
          </div>
          <div v-if="concert.open_time" class="info-item">
            <span class="info-label">Doors Open</span>
            <span class="info-value">{{ concert.open_time }}</span>
          </div>
          <div v-if="concert.start_time" class="info-item">
            <span class="info-label">Show Start</span>
            <span class="info-value">{{ concert.start_time }}</span>
          </div>
        </div>
      </div>

      <!-- Tickets -->
      <div class="detail-section">
        <h2 class="section-heading">Tickets</h2>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">Sale Date</span>
            <span class="info-value">{{ concert.ticket_sale_date || '-' }}</span>
          </div>
        </div>
        <div class="price-list">
          <span class="info-label">Prices</span>
          <ul v-if="concert.prices?.length">
            <li v-for="(price, i) in concert.prices" :key="i">{{ price }}</li>
          </ul>
          <span v-else class="info-value"><br>-</span>
        </div>
        <div class="vendor-list">
          <span class="info-label">Where to Buy</span>
          <div v-if="concert.ticket_vendors?.length" class="vendor-links">
            <a
              v-for="vendor in concert.ticket_vendors"
              :key="vendor.name"
              :href="vendor.url"
              target="_blank"
              rel="noopener noreferrer"
              class="vendor-btn"
            >{{ vendor.name }}</a>
          </div>
          <span v-else class="info-value"><br>-</span>
        </div>
      </div>

      <!-- Links -->
      <div class="detail-section" v-if="concert.official_site || concert.detail_url">
        <h2 class="section-heading">Links</h2>
        <div class="link-list">
          <a v-if="concert.official_site" :href="concert.official_site" target="_blank" rel="noopener noreferrer" class="ext-link">
            Official Site ↗
          </a>
          <a v-if="concert.detail_url" :href="concert.detail_url" target="_blank" rel="noopener noreferrer" class="ext-link">
            Source Page ↗
          </a>
        </div>
      </div>

      <!-- Contact -->
      <div class="detail-section" v-if="concert.contact">
        <h2 class="section-heading">Contact</h2>
        <p class="info-value">{{ concert.contact }}</p>
      </div>

      </div><!-- end detail-body -->
    </div>
    <p v-else>Concert not found.</p>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { useConcertStore } from '../stores/concertStore'

const route = useRoute()
const store = useConcertStore()

onMounted(async () => {
  if (store.scanConcerts.length === 0) {
    await store.fetchScan()
  }
})

const concert = computed(() =>
  store.scanConcerts.find((c) => String(c.id) === String(route.params.id))
)

const dateRe = /\d{4}\.\d{2}\.\d{2}/

const formattedDates = computed(() => {
  if (!concert.value) return ''
  const matched = concert.value.dates.filter((d) => dateRe.test(d))
  if (!matched.length) return concert.value.dates.join(', ')
  return matched
    .map((d) => {
      const [y, m, day] = d.split('.')
      return new Date(Number(y), Number(m) - 1, Number(day)).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      })
    })
    .join(' · ')
})
</script>

<style scoped>
.back-link {
  display: inline-block;
  margin-bottom: 1.5rem;
  color: #9999b3;
  text-decoration: none;
  font-size: 0.875rem;
  transition: color 0.15s;
}
.back-link:hover {
  color: #e8e8f0;
}

.detail-wrapper {
  display: grid;
  grid-template-columns: 260px 1fr;
  grid-template-rows: auto;
  gap: 2rem;
  align-items: start;
  max-width: 900px;
}

/* Header */
.detail-header {
  position: sticky;
  top: 1.5rem;
  padding: 1.5rem;
  background: #1c1c28;
  border: 1px solid #2a2a3a;
  border-radius: 12px;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
.detail-header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin: 0 0 0.25rem;
  color: #e8e8f0;
}
.concert-title {
  color: #9999b3;
  font-size: 1rem;
  margin: 0 0 0.75rem;
}
.sold-out {
  display: inline-block;
  background: #3a0f0f;
  color: #f87171;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
  border: 1px solid #7f1d1d;
}

/* Sections */
.detail-section {
  background: #1c1c28;
  border: 1px solid #2a2a3a;
  border-radius: 12px;
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.section-heading {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #7c3aed;
  margin: 0;
}

/* Info grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 0.75rem 1.5rem;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.info-label {
  font-size: 0.75rem;
  color: #9999b3;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.info-value {
  color: #e8e8f0;
  font-size: 0.95rem;
}

/* Prices */
.price-list ul {
  margin: 0.4rem 0 0;
  padding-left: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.price-list li {
  color: #e8e8f0;
  font-size: 0.95rem;
}

/* Vendors */
.vendor-links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.4rem;
}
.vendor-btn {
  display: inline-block;
  padding: 0.4rem 0.9rem;
  background: #2a2a3a;
  border: 1px solid #3a3a50;
  border-radius: 8px;
  color: #c4b5fd;
  font-size: 0.875rem;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s;
}
.vendor-btn:hover {
  background: #7c3aed;
  border-color: #7c3aed;
  color: #fff;
}

/* Links */
.link-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}
.ext-link {
  color: #c4b5fd;
  font-size: 0.9rem;
  text-decoration: none;
  transition: color 0.15s;
}
.ext-link:hover {
  color: #fff;
  text-decoration: underline;
}

.error {
  color: #f87171;
}
</style>
