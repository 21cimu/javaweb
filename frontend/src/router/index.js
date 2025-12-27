import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// Layouts
const MainLayout = () => import('@/views/layouts/MainLayout.vue')
const AdminLayout = () => import('@/views/layouts/AdminLayout.vue')

// Front pages
const Home = () => import('@/views/Home.vue')
const VehicleList = () => import('@/views/vehicle/VehicleList.vue')
const VehicleDetail = () => import('@/views/vehicle/VehicleDetail.vue')
const OrderConfirm = () => import('@/views/order/OrderConfirm.vue')
const OrderList = () => import('@/views/order/OrderList.vue')
const OrderDetail = () => import('@/views/order/OrderDetail.vue')
const OrderAfterSalesApply = () => import('@/views/order/AfterSalesApply.vue')
const UserCenter = () => import('@/views/user/UserCenter.vue')
const Verification = () => import('@/views/user/Verification.vue')
const MyCoupons = () => import('@/views/marketing/MyCoupons.vue')
const Promotions = () => import('@/views/marketing/Promotions.vue')
const Login = () => import('@/views/user/Login.vue')
const Register = () => import('@/views/user/Register.vue')

// Admin pages
const Dashboard = () => import('@/views/admin/Dashboard.vue')
const AdminVehicles = () => import('@/views/admin/Vehicles.vue')
const AdminOrders = () => import('@/views/admin/Orders.vue')
const AdminUsers = () => import('@/views/admin/Users.vue')
const AdminCoupons = () => import('@/views/admin/Coupons.vue')
const AdminStores = () => import('@/views/admin/Stores.vue')
const AdminUserCoupons = () => import('@/views/admin/UserCoupons.vue')
const AdminReviews = () => import('@/views/admin/Reviews.vue')
const AdminAfterSales = () => import('@/views/admin/AfterSales.vue')
const AdminMaintenance = () => import('@/views/admin/Maintenance.vue')
const AdminLoginSecurityLogs = () => import('@/views/admin/logs/LoginSecurityLogs.vue')
const AdminOperationAuditLogs = () => import('@/views/admin/logs/AdminOperationAuditLogs.vue')
const AdminOrderEventLogs = () => import('@/views/admin/logs/OrderEventLogs.vue')
const AdminFundsFlowLogs = () => import('@/views/admin/logs/FundsFlowLogs.vue')
const AdminVehicleStatusLogs = () => import('@/views/admin/logs/VehicleStatusLogs.vue')

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', name: 'Home', component: Home },
      { path: 'vehicles', name: 'VehicleList', component: VehicleList },
      { path: 'vehicles/:id', name: 'VehicleDetail', component: VehicleDetail },
      { path: 'order/confirm', name: 'OrderConfirm', component: OrderConfirm, meta: { requiresAuth: true } },
      { path: 'orders', name: 'OrderList', component: OrderList, meta: { requiresAuth: true } },
      { path: 'orders/:id', name: 'OrderDetail', component: OrderDetail, meta: { requiresAuth: true } },
      { path: 'orders/:id/after-sales', name: 'OrderAfterSalesApply', component: OrderAfterSalesApply, meta: { requiresAuth: true } },
      { path: 'user', name: 'UserCenter', component: UserCenter, meta: { requiresAuth: true } },
      { path: 'user/verification', name: 'Verification', component: Verification, meta: { requiresAuth: true } },
      { path: 'coupons', name: 'MyCoupons', component: MyCoupons, meta: { requiresAuth: true } },
      { path: 'promotions', name: 'Promotions', component: Promotions }
    ]
  },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: Dashboard },
      { path: 'vehicles', name: 'AdminVehicles', component: AdminVehicles },
      { path: 'orders', name: 'AdminOrders', component: AdminOrders },
      { path: 'users', name: 'AdminUsers', component: AdminUsers },
      { path: 'coupons', name: 'AdminCoupons', component: AdminCoupons },
      { path: 'user-coupons', name: 'AdminUserCoupons', component: AdminUserCoupons },
      { path: 'stores', name: 'AdminStores', component: AdminStores },
      { path: 'reviews', name: 'AdminReviews', component: AdminReviews },
      { path: 'after-sales', name: 'AdminAfterSales', component: AdminAfterSales },
      { path: 'maintenance', name: 'AdminMaintenance', component: AdminMaintenance },
      { path: 'logs/login-security', name: 'AdminLoginSecurityLogs', component: AdminLoginSecurityLogs },
      { path: 'logs/operation-audit', name: 'AdminOperationAuditLogs', component: AdminOperationAuditLogs },
      { path: 'logs/order-events', name: 'AdminOrderEventLogs', component: AdminOrderEventLogs },
      { path: 'logs/funds-flow', name: 'AdminFundsFlowLogs', component: AdminFundsFlowLogs },
      { path: 'logs/vehicle-status', name: 'AdminVehicleStatusLogs', component: AdminVehicleStatusLogs }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  userStore.syncAuthFromStorage()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
