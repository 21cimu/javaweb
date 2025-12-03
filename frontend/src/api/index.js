import axios from 'axios'

// Get API base URL from environment or use relative path for production
const getBaseURL = () => {
  // Use Vite's import.meta.env for environment variables
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL
  }
  // In development, Vite proxy handles /api requests
  // In production, use relative path (assumes backend and frontend on same server)
  return import.meta.env.DEV ? '/api' : '/car-rental/api'
}

// Create axios instance with base URL
const api = axios.create({
  baseURL: getBaseURL(),
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
api.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// Auth API
export const authApi = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (data) => api.post('/auth/register', data),
  logout: () => api.post('/auth/logout'),
  checkSession: () => api.get('/auth/check')
}

// User API
export const userApi = {
  getAll: () => api.get('/users'),
  getById: (id) => api.get(`/users/${id}`),
  update: (id, data) => api.put(`/users/${id}`, data),
  delete: (id) => api.delete(`/users/${id}`)
}

// Vehicle API
export const vehicleApi = {
  getAll: () => api.get('/vehicles'),
  getAvailable: () => api.get('/vehicles?available=true'),
  getById: (id) => api.get(`/vehicles/${id}`),
  search: (keyword) => api.get(`/vehicles?keyword=${keyword}`),
  getByCategory: (category) => api.get(`/vehicles?category=${category}`),
  create: (data) => api.post('/vehicles', data),
  update: (id, data) => api.put(`/vehicles/${id}`, data),
  delete: (id) => api.delete(`/vehicles/${id}`)
}

// Order API
export const orderApi = {
  getAll: () => api.get('/orders'),
  getById: (id) => api.get(`/orders/${id}`),
  getByUserId: (userId) => api.get(`/orders?userId=${userId}`),
  create: (data) => api.post('/orders', data),
  pay: (id, paymentMethod) => api.put(`/orders/${id}/pay`, { paymentMethod }),
  pickup: (id) => api.put(`/orders/${id}/pickup`),
  return: (id) => api.put(`/orders/${id}/return`),
  complete: (id) => api.put(`/orders/${id}/complete`),
  cancel: (id) => api.put(`/orders/${id}/cancel`),
  delete: (id) => api.delete(`/orders/${id}`)
}

export default api
