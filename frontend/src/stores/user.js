import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin' || user.value?.role === 'superadmin')
  const isVerified = computed(() => user.value?.verificationStatus === 2)

  async function login(credentials) {
    const res = await api.auth.login(credentials)
    if (res.code === 200) {
      token.value = res.data.token
      user.value = res.data.user
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.user))
    }
    return res
  }

  async function register(data) {
    const res = await api.auth.register(data)
    if (res.code === 200) {
      token.value = res.data.token
      user.value = res.data.user
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.user))
    }
    return res
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function fetchProfile() {
    if (!token.value) return
    const res = await api.user.getProfile()
    if (res.code === 200) {
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
    }
  }

  async function updateProfile(data) {
    const res = await api.user.updateProfile(data)
    if (res.code === 200) {
      Object.assign(user.value, data)
      localStorage.setItem('user', JSON.stringify(user.value))
    }
    return res
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    isVerified,
    login,
    register,
    logout,
    fetchProfile,
    updateProfile
  }
})
