<template>
  <div class="app-layout">
    <header class="app-header">
      <div class="logo" @click="$router.push('/')">
        <el-icon :size="28"><Van /></el-icon>
        <span>畅行租车</span>
      </div>
      
      <nav class="nav-menu">
        <router-link to="/" :class="{ active: $route.path === '/' }">首页</router-link>
        <router-link to="/vehicles" :class="{ active: $route.path.startsWith('/vehicles') }">选车</router-link>
        <router-link to="/promotions" :class="{ active: $route.path === '/promotions' }">优惠活动</router-link>
        <router-link to="/readme" :class="{ active: $route.path === '/readme' }">README</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/orders" :class="{ active: $route.path.startsWith('/orders') }">我的订单</router-link>
      </nav>
      
      <div class="user-area">
        <template v-if="userStore.isLoggedIn">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.user?.avatar">
                {{ userStore.user?.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.user?.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="user">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="orders">
                  <el-icon><List /></el-icon> 我的订单
                </el-dropdown-item>
                <el-dropdown-item command="coupons">
                  <el-icon><Ticket /></el-icon> 我的优惠券
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>
                  <el-icon><Setting /></el-icon> 管理后台
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
          <el-button @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </header>
    
    <main class="app-main">
      <router-view />
    </main>
    
    <footer class="app-footer">
      <div class="footer-links">
        <a href="#">关于我们</a>
        <a href="#">帮助中心</a>
        <router-link to="/readme">README</router-link>
        <a href="#">联系客服</a>
        <a href="#">服务条款</a>
        <a href="#">隐私政策</a>
      </div>
      <div class="copyright">
        © 2024 畅行租车 版权所有
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { Van, User, List, Ticket, Setting, SwitchButton } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

const handleCommand = (command) => {
  switch (command) {
    case 'user':
      router.push('/user')
      break
    case 'orders':
      router.push('/orders')
      break
    case 'coupons':
      router.push('/coupons')
      break
    case 'admin':
      router.push('/admin')
      break
    case 'logout':
      userStore.logout()
      router.push('/')
      break
  }
}
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #606266;
}
</style>
