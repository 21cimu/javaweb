import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuthStorage, getStoredToken } from '@/utils/auth'

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// Request interceptor
instance.interceptors.request.use(
  config => {
    const token = getStoredToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
instance.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          clearAuthStorage()
          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
          break
        case 403:
          ElMessage.error('没有权限执行此操作')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误，请稍后重试')
          break
        default:
          ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

// API endpoints
const api = {
  // Auth
  auth: {
    login: (data) => instance.post('/auth/login', data),
    register: (data) => instance.post('/auth/register', data),
    forgotPassword: (data) => instance.post('/auth/forgot-password', data)
  },

  // User
  user: {
    getProfile: () => instance.get('/user/profile'),
    updateProfile: (data) => instance.post('/user/profile', data),
    getVerification: () => instance.get('/user/verification'),
    submitVerification: (data) => instance.post('/user/verification', data),
    changePassword: (data) => instance.post('/user/password', data),
    passwordCaptcha: () => instance.get('/user/password-captcha')
  },

  // Vehicles
  vehicles: {
    list: (params) => instance.get('/vehicles/list', { params }),
    search: (params) => instance.get('/vehicles/search', { params }),
    detail: (id) => instance.get(`/vehicles/detail/${id}`),
    categories: () => instance.get('/vehicles/categories'),
    brands: () => instance.get('/vehicles/brands'),
    hot: (limit = 6) => instance.get('/vehicles/hot', { params: { limit } }),
    new: (limit = 6) => instance.get('/vehicles/new', { params: { limit } })
  },

  // Stores
  stores: {
    list: (params) => instance.get('/stores/list', { params }),
    cities: () => instance.get('/stores/cities'),
    detail: (id) => instance.get(`/stores/detail/${id}`)
  },

  // Orders
  orders: {
    list: (params) => instance.get('/orders/list', { params }),
    detail: (id) => instance.get(`/orders/detail/${id}`),
    create: (data) => instance.post('/orders/create', data),
    pay: (data) => instance.post('/orders/pay', data),
    cancel: (data) => instance.post('/orders/cancel', data),
    review: (data) => instance.post('/orders/review', data),
    calculate: (data) => instance.post('/orders/calculate', data)
  },

  // Reviews (public)
  reviews: {
    list: (params) => instance.get('/reviews/list', { params })
  },

  // Payment
  pay: {
    alipayCreate: (data) => instance.post('/pay/alipay/create', data)
  },

  // Marketing
  marketing: {
    promotions: () => instance.get('/marketing/promotions'),
    banners: () => instance.get('/marketing/banners'),
    coupons: () => instance.get('/marketing/coupons'),
    myCoupons: (params) => instance.get('/marketing/my-coupons', { params }),
    claimCoupon: (data) => instance.post('/marketing/claim-coupon', data),
    points: () => instance.get('/marketing/points'),
    exchangePoints: (data) => instance.post('/marketing/exchange-points', data)
  },

  // Admin APIs
  admin: {
    // Reviews (admin)
    reviews: {
      list: (params) => instance.get('/admin/reviews', { params }),
      delete: (id) => instance.delete(`/admin/reviews/${id}`)
    },

    // Dashboard
    dashboard: {
      overview: () => instance.get('/admin/dashboard/overview'),
      stats: (params) => instance.get('/admin/dashboard/stats', { params }),
      trends: (params) => instance.get('/admin/dashboard/trends', { params }),
      alerts: () => instance.get('/admin/dashboard/alerts'),
      rankings: () => instance.get('/admin/dashboard/rankings')
    },

    // Users
    users: {
      list: (params) => instance.get('/admin/users/list', { params }),
      detail: (id) => instance.get(`/admin/users/detail/${id}`),
      approve: (data) => instance.post('/admin/users/approve', data),
      reject: (data) => instance.post('/admin/users/reject', data),
      enable: (data) => instance.post('/admin/users/enable', data),
      disable: (data) => instance.post('/admin/users/disable', data),
      updateRole: (data) => instance.post('/admin/users/role', data)
    },

    // Vehicles
    vehicles: {
      list: (params) => instance.get('/admin/vehicles/list', { params }),
      detail: (id) => instance.get(`/admin/vehicles/detail/${id}`),
      create: (data) => instance.post('/admin/vehicles/create', data),
      update: (data) => instance.post('/admin/vehicles/update', data),
      updateStatus: (data) => instance.post('/admin/vehicles/status', data),
      delete: (id) => instance.post('/admin/vehicles/delete', { id }),
      stats: () => instance.get('/admin/vehicles/stats')
    },

    // Orders
    orders: {
      list: (params) => instance.get('/admin/orders/list', { params }),
      detail: (id) => instance.get(`/admin/orders/detail/${id}`),
      pending: () => instance.get('/admin/orders/pending'),
      stats: () => instance.get('/admin/orders/stats'),
      approve: (data) => instance.post('/admin/orders/approve', data),
      reject: (data) => instance.post('/admin/orders/reject', data),
      pickup: (data) => instance.post('/admin/orders/pickup', data),
      return: (data) => instance.post('/admin/orders/return', data),
      complete: (data) => instance.post('/admin/orders/complete', data),
      refund: (data) => instance.post('/admin/orders/refund', data),
      delete: (id) => instance.post('/admin/orders/delete', { orderId: id }),
      export: (params) => instance.get('/admin/orders/export', { params, responseType: 'blob' })
    },

    // Coupons (admin)
    coupons: {
      list: (params) => instance.get('/admin/coupons', { params }),
      detail: (id) => instance.get(`/admin/coupons/${id}`),
      create: (data) => instance.post('/admin/coupons', data),
      update: (id, data) => instance.put(`/admin/coupons/${id}`, data),
      delete: (id) => instance.delete(`/admin/coupons/${id}`),
      issue: (data) => instance.post('/admin/coupons/issue', data)
    },

    // User Coupons (admin) - 管理用户已持有优惠券
    userCoupons: {
      list: (params) => instance.get('/admin/user-coupons', { params }),
      updateStatus: (id, data) => instance.put(`/admin/user-coupons/${id}`, data),
      delete: (id) => instance.delete(`/admin/user-coupons/${id}`)
    },

    // Logs (admin)
    logs: {
      loginSecurity: (params) => instance.get('/admin/logs/login-security', { params }),
      operationAudit: (params) => instance.get('/admin/logs/operation-audit', { params }),
      orderEvents: (params) => instance.get('/admin/logs/order-events', { params }),
      fundsFlow: (params) => instance.get('/admin/logs/funds-flow', { params }),
      vehicleStatus: (params) => instance.get('/admin/logs/vehicle-status', { params })
    },

    // 售后（admin）
    afterSales: {
      list: (params) => instance.get('/admin/after-sales', { params }),
      audit: (data) => instance.post('/admin/after-sales/audit', data)
    }
  },

  // Upload
  upload: {
    file: (formData) => instance.post('/upload', formData)
  },

  // 售后（用户）
  afterSales: {
    create: (data) => instance.post('/after-sales', data)
  }
}

export default api

// Note: for JSON requests axios will set Content-Type automatically; for FormData uploads
// we must not force the Content-Type here so the browser can add the proper multipart boundary.
