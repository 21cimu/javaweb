<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from './api'

const router = useRouter()
const user = ref(null)
const isMenuCollapsed = ref(false)

onMounted(async () => {
  const savedUser = localStorage.getItem('user')
  if (savedUser) {
    user.value = JSON.parse(savedUser)
    // Verify session
    try {
      const res = await authApi.checkSession()
      if (!res.loggedIn) {
        localStorage.removeItem('user')
        user.value = null
      }
    } catch (e) {
      console.log('Session check failed')
    }
  }
})

const handleLogout = async () => {
  try {
    await authApi.logout()
  } catch (e) {
    console.log('Logout error')
  }
  localStorage.removeItem('user')
  user.value = null
  router.push('/')
}

const goToLogin = () => {
  router.push('/login')
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<template>
  <el-container class="layout-container">
    <!-- Header -->
    <el-header class="app-header">
      <div class="header-content">
        <div class="logo" @click="router.push('/')">
          <el-icon :size="28"><Van /></el-icon>
          <span class="logo-text">租车平台</span>
        </div>
        
        <el-menu
          mode="horizontal"
          :ellipsis="false"
          class="nav-menu"
          router
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/vehicles">选车</el-menu-item>
          <el-menu-item v-if="user" index="/orders">我的订单</el-menu-item>
          <el-menu-item v-if="user" index="/user">个人中心</el-menu-item>
          <el-sub-menu v-if="user" index="admin">
            <template #title>管理后台</template>
            <el-menu-item index="/admin">仪表盘</el-menu-item>
            <el-menu-item index="/admin/vehicles">车辆管理</el-menu-item>
            <el-menu-item index="/admin/orders">订单管理</el-menu-item>
          </el-sub-menu>
        </el-menu>
        
        <div class="user-area">
          <template v-if="user">
            <el-dropdown>
              <span class="user-dropdown">
                <el-avatar :size="32" icon="User" />
                <span class="username">{{ user.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/user')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="goToLogin">登录</el-button>
            <el-button @click="goToRegister">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    
    <!-- Main Content -->
    <el-main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
    
    <!-- Footer -->
    <el-footer class="app-footer">
      <div class="footer-content">
        <p>© 2024 租车平台 - 安全可靠的在线租车服务</p>
        <p>服务热线：400-123-4567</p>
      </div>
    </el-footer>
  </el-container>
</template>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.app-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 60px;
  line-height: 60px;
  padding: 0 20px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  margin-right: 40px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  margin-left: 8px;
  color: #409EFF;
}

.nav-menu {
  flex: 1;
  border-bottom: none;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin-left: 8px;
  font-size: 14px;
}

.app-main {
  margin-top: 60px;
  min-height: calc(100vh - 160px);
  padding: 20px;
  background: #f5f7fa;
}

.app-footer {
  background: #303133;
  color: #fff;
  text-align: center;
  height: auto;
  padding: 20px;
}

.footer-content p {
  margin: 5px 0;
  font-size: 14px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
