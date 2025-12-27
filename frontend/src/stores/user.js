import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'
import { clearAuthStorage, getStoredAuth, setAuthStorage } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const storedAuth = getStoredAuth()
  const token = ref(storedAuth.token)
  const user = ref(storedAuth.user)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin' || user.value?.role === 'superadmin')
  const isVerified = computed(() => user.value?.verificationStatus === 2)

  async function login(credentials, options = {}) {
    const { remember = false } = options
    const res = await api.auth.login(credentials)
    if (res.code === 200) {
      token.value = res.data.token
      user.value = res.data.user
      setAuthStorage({ token: res.data.token, user: res.data.user }, remember)
    }
    return res
  }

  async function register(data) {
    const res = await api.auth.register(data)
    if (res.code === 200) {
      token.value = res.data.token
      user.value = res.data.user
      setAuthStorage({ token: res.data.token, user: res.data.user })
    }
    return res
  }

  function logout() {
    token.value = ''
    user.value = null
    clearAuthStorage()
  }

  function syncAuthFromStorage() {
    const stored = getStoredAuth()
    token.value = stored.token
    user.value = stored.user
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
    syncAuthFromStorage,
    fetchProfile,
    updateProfile
  }
})
