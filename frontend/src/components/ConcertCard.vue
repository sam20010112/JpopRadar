<template>
  <div class="concert-card">
    <h3>{{ concert.artist }}</h3>
    <p class="title">{{ concert.title }}</p>
    <p class="venue">{{ concert.venue }}</p>
    <p class="date">{{ formattedDates }}</p>
    <span v-if="concert.ticket_status === 'SOLD OUT'" class="sold-out">SOLD OUT</span>
    <div class="actions">
      <RouterLink :to="`/concerts/${concert.id}`">View Details</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  concert: {
    type: Object,
    required: true,
  },
})

const dateRe = /\d{4}\.\d{2}\.\d{2}/

const formattedDates = computed(() => {
  const matched = props.concert.dates.filter((d) => dateRe.test(d))
  if (!matched.length) return props.concert.dates[0] || ''
  return matched
    .map((d) => {
      const [y, m, day] = d.split('.')
      return new Date(`${y}-${m}-${day}`).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      })
    })
    .join(' · ')
})
</script>

<style scoped>
.sold-out {
  color: red;
  font-weight: bold;
}
</style>
