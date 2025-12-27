const TOKEN_KEY = 'token'
const USER_KEY = 'user'
const EXPIRY_KEY = 'token_expires_at'

const ONE_DAY_MS = 24 * 60 * 60 * 1000
const THIRTY_MIN_MS = 30 * 60 * 1000

const getExpiryDurationMs = (remember) => (remember ? ONE_DAY_MS : THIRTY_MIN_MS)

const clearAuthStorage = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(EXPIRY_KEY)
}

const getStoredToken = () => {
  const token = localStorage.getItem(TOKEN_KEY) || ''
  if (!token) {
    return ''
  }

  const expiresAtRaw = localStorage.getItem(EXPIRY_KEY)
  const expiresAt = expiresAtRaw ? Number(expiresAtRaw) : null

  if (expiresAt && Date.now() > expiresAt) {
    clearAuthStorage()
    return ''
  }

  return token
}

const getStoredAuth = () => {
  const token = getStoredToken()
  if (!token) {
    return { token: '', user: null }
  }

  let user = null
  const userRaw = localStorage.getItem(USER_KEY)
  if (userRaw) {
    try {
      user = JSON.parse(userRaw)
    } catch (error) {
      user = null
    }
  }

  return { token, user }
}

const setAuthStorage = ({ token, user }, remember = false) => {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))

  const expiresAt = Date.now() + getExpiryDurationMs(remember)
  localStorage.setItem(EXPIRY_KEY, String(expiresAt))

  return expiresAt
}

export {
  clearAuthStorage,
  getStoredAuth,
  getStoredToken,
  setAuthStorage
}
