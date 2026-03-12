<template>
  <div class="calendar">
    <div class="cal-header">
      <button class="nav-btn" @click="prevMonth">&#8249;</button>
      <span class="month-label">{{ monthLabel }}</span>
      <button class="nav-btn" @click="nextMonth">&#8250;</button>
    </div>

    <div class="cal-grid">
      <div v-for="day in WEEKDAYS" :key="day" class="weekday-header">{{ day }}</div>

      <div
        v-for="(cell, i) in calendarCells"
        :key="i"
        class="day-cell"
        :class="{
          empty: !cell.dayNum,
          'has-concert': cell.hasConcert,
          today: cell.isToday,
        }"
        @click="cell.hasConcert && emit('date-click', cell.dateKey)"
      >
        <span v-if="cell.dayNum" class="day-num">{{ cell.dayNum }}</span>
        <span v-if="cell.hasConcert" class="tooltip">{{ cell.artists.join(', ') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  concerts: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['date-click'])

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth()) // 0-indexed

const monthLabel = computed(() => `${MONTHS[currentMonth.value]} ${currentYear.value}`)

function prevMonth() {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

const concertsByDate = computed(() => {
  const map = new Map()
  const re = /(\d{4})\.(\d{2})\.(\d{2})/
  for (const concert of props.concerts) {
    for (const dateStr of concert.dates ?? []) {
      const m = re.exec(dateStr)
      if (!m) continue
      const key = `${m[1]}-${m[2]}-${m[3]}`
      if (!map.has(key)) map.set(key, [])
      map.get(key).push(concert.artist)
    }
  }
  return map
})

const calendarCells = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month, 1).getDay() // 0=Sun
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  const cells = []

  // Leading empty cells
  for (let i = 0; i < firstDay; i++) {
    cells.push({ dayNum: null, hasConcert: false, artists: [], isToday: false })
  }

  // Day cells
  for (let d = 1; d <= daysInMonth; d++) {
    const key = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const artists = concertsByDate.value.get(key) ?? []
    cells.push({
      dayNum: d,
      hasConcert: artists.length > 0,
      artists,
      isToday: key === todayStr,
      dateKey: key,
    })
  }

  // Trailing empty cells to complete the grid (always 6 rows = 42 cells)
  while (cells.length < 42) {
    cells.push({ dayNum: null, hasConcert: false, artists: [], isToday: false })
  }

  return cells
})
</script>

<style scoped>
.calendar {
  background: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 12px;
  padding: 1.25rem;
  max-width: 480px;
}

.cal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.month-label {
  font-weight: 600;
  font-size: 1rem;
  color: #e2e2f0;
}

.nav-btn {
  background: none;
  border: 1px solid #2a2a4a;
  color: #9999b3;
  border-radius: 6px;
  width: 2rem;
  height: 2rem;
  cursor: pointer;
  font-size: 1.2rem;
  line-height: 1;
  transition: border-color 0.15s, color 0.15s;
}

.nav-btn:hover {
  border-color: #a78bfa;
  color: #a78bfa;
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 3px;
}

.weekday-header {
  text-align: center;
  font-size: 0.7rem;
  color: #6666aa;
  font-weight: 600;
  padding-bottom: 0.4rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.day-cell {
  position: relative;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: default;
  transition: background 0.15s;
}

.day-cell.empty {
  pointer-events: none;
}

.day-num {
  font-size: 0.8rem;
  color: #c0c0dd;
  z-index: 1;
  pointer-events: none;
}

.day-cell.today .day-num {
  color: #a78bfa;
  font-weight: 700;
}

.day-cell.today {
  border: 1px solid #a78bfa44;
}

.day-cell.has-concert {
  background: linear-gradient(135deg, #3d1f6e55, #6b1f5544);
  cursor: pointer;
}

.day-cell.has-concert .day-num {
  color: #f0b0e0;
  font-weight: 600;
}

.day-cell.has-concert::before {
  content: '';
  position: absolute;
  bottom: 4px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #f472b6;
}

/* Tooltip */
.tooltip {
  display: none;
  position: absolute;
  bottom: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  background: #2a1a4a;
  border: 1px solid #a78bfa66;
  color: #e2c0ff;
  font-size: 0.68rem;
  white-space: nowrap;
  padding: 4px 8px;
  border-radius: 6px;
  z-index: 10;
  pointer-events: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.day-cell.has-concert:hover .tooltip {
  display: block;
}
</style>
