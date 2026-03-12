import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
  },
  {
    path: '/concerts',
    name: 'concerts',
    component: () => import('../views/ConcertsView.vue'),
  },
  {
    path: '/concerts/:id',
    name: 'concert-detail',
    component: () => import('../views/ConcertDetailView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
