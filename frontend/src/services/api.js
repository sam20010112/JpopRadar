import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
})

export const concertApi = {
  getAll: () => http.get('/concerts'),
  getById: (id) => http.get(`/concerts/${id}`),
  create: (concert) => http.post('/concerts', concert),
  update: (id, concert) => http.put(`/concerts/${id}`, concert),
  delete: (id) => http.delete(`/concerts/${id}`),
  getUpcoming: () => http.get('/concerts/upcoming'),
  getByCity: (city) => http.get('/concerts/by-city', { params: { city } }),
  getByArtist: (artist) => http.get('/concerts/by-artist', { params: { artist } }),
  getScan: () => http.get('/concerts/scan'),
}
