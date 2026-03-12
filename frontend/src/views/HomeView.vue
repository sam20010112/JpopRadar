<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <h1>JpopRadar</h1>
      <p class="tagline">Track live J-pop concerts in Japan</p>
      <div class="hero-actions">
        <RouterLink to="/concerts" class="btn btn-primary">See Concerts</RouterLink>
      </div>
    </section>

    <!-- Calendar + Upcoming side by side -->
    <div class="main-row">
      <section class="calendar-section">
        <h2>Concert Calendar</h2>
        <ConcertCalendar :concerts="concertStore.scanConcerts" @date-click="onDateClick" />
      </section>

      <section class="preview-section">
        <div class="section-header">
          <h2>Upcoming Concerts</h2>
          <RouterLink to="/concerts">See all concerts &rarr;</RouterLink>
        </div>
        <p v-if="concertStore.loading" class="loading">Loading concerts...</p>
        <p v-else-if="concertStore.error" class="error">{{ concertStore.error }}</p>
        <div v-else class="card-grid">
          <ConcertCard
            v-for="concert in upcomingPreview"
            :key="concert.id"
            :concert="concert"
          />
          <p v-if="upcomingPreview.length === 0" class="loading">No upcoming concerts.</p>
        </div>
      </section>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useConcertStore } from '../stores/concertStore'
import ConcertCard from '../components/ConcertCard.vue'
import ConcertCalendar from '../components/ConcertCalendar.vue'

const concertStore = useConcertStore()
const router = useRouter()

function onDateClick(date) {
  router.push({ name: 'concerts', query: { date } })
}

const upcomingPreview = computed(() => concertStore.concerts.slice(0, 7))

onMounted(() => {
  concertStore.fetchUpcoming()
})
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.hero {
  text-align: center;
  padding: 4rem 1rem 2rem;
}

.hero h1 {
  font-size: 3rem;
  font-weight: 700;
  letter-spacing: -0.04em;
  background: linear-gradient(135deg, #a78bfa, #f472b6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 0.75rem;
}

.tagline {
  font-size: 1.2rem;
  color: #9999b3;
  margin-bottom: 2rem;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.main-row {
  display: flex;
  gap: 2rem;
  align-items: flex-start;
}

.calendar-section {
  flex-shrink: 0;
}

.calendar-section h2 {
  font-size: 1.3rem;
  font-weight: 600;
  margin-bottom: 1.25rem;
}

.preview-section {
  flex: 1;
  min-width: 0;
}

@media (max-width: 768px) {
  .main-row {
    flex-direction: column;
  }
}

.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.section-header h2 {
  font-size: 1.3rem;
  font-weight: 600;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

</style>
