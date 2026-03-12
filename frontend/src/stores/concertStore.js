import { defineStore } from 'pinia'
import { concertApi } from '../services/api'

export const useConcertStore = defineStore('concerts', {
  state: () => ({
    concerts: [],
    scanConcerts: [],
    currentConcert: null,
    loading: false,
    error: null,
  }),

  actions: {
    async fetchAll() {
      this.loading = true
      this.error = null
      try {
        const { data } = await concertApi.getAll()
        this.concerts = data
      } catch (err) {
        this.error = err.message
      } finally {
        this.loading = false
      }
    },

    async fetchById(id) {
      this.loading = true
      this.error = null
      try {
        const { data } = await concertApi.getById(id)
        this.currentConcert = data
      } catch (err) {
        this.error = err.message
      } finally {
        this.loading = false
      }
    },

    async fetchScan() {
      this.loading = true
      this.error = null
      try {
        const { data } = await concertApi.getScan()
        this.scanConcerts = data
        this.concerts = data
      } catch (err) {
        this.error = err.message
      } finally {
        this.loading = false
      }
    },

    async fetchUpcoming() {
      await this.fetchScan()
      const today = new Date().toISOString().slice(0, 10)
      const dateRe = /\d{4}\.\d{2}\.\d{2}/
      this.concerts = this.scanConcerts
        .filter((c) => {
          const match = c.dates.find((d) => dateRe.test(d))
          if (!match) return false
          return match.replace(/\./g, '-') >= today
        })
        .sort((a, b) => {
          const da = a.dates.find((d) => dateRe.test(d)) || ''
          const db = b.dates.find((d) => dateRe.test(d)) || ''
          return da.localeCompare(db)
        })
    },

    async fetchByCity(city) {
      this.loading = true
      this.error = null
      try {
        const { data } = await concertApi.getByCity(city)
        this.concerts = data
      } catch (err) {
        this.error = err.message
      } finally {
        this.loading = false
      }
    },

    async fetchByArtist(artist) {
      this.loading = true
      this.error = null
      try {
        const { data } = await concertApi.getByArtist(artist)
        this.concerts = data
      } catch (err) {
        this.error = err.message
      } finally {
        this.loading = false
      }
    },

    async create(concert) {
      const { data } = await concertApi.create(concert)
      this.concerts.push(data)
      return data
    },

    async update(id, concert) {
      const { data } = await concertApi.update(id, concert)
      const index = this.concerts.findIndex((c) => c.id === id)
      if (index !== -1) this.concerts[index] = data
      return data
    },

    async remove(id) {
      await concertApi.delete(id)
      this.concerts = this.concerts.filter((c) => c.id !== id)
    },
  },
})
