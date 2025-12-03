<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="logo">
        <el-icon><Van /></el-icon>
        畅行租车管理
      </div>
      
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        
        <el-sub-menu index="vehicles">
          <template #title>
            <el-icon><Van /></el-icon>
            <span>车辆管理</span>
          </template>
          <el-menu-item index="/admin/vehicles">车辆列表</el-menu-item>
          <el-menu-item index="/admin/stores">门店管理</el-menu-item>
        </el-sub-menu>
        
        <el-menu-item index="/admin/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        
        <el-menu-item index="/admin/coupons">
          <el-icon><Ticket /></el-icon>
          <span>营销管理</span>
        </el-menu-item>
      </el-menu>
    </aside>
    
    <div class="admin-main">
      <header class="admin-header">
        <div class="breadcrumb">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="admin-user">
              <el-avatar :size="32">{{ userStore.user?.username?.charAt(0)?.toUpperCase() }}</el-avatar>
              <span>{{ userStore.user?.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">
                  <el-icon><House /></el-icon> 返回前台
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { 
  Van, DataAnalysis, Document, User, Ticket, 
  House, SwitchButton 
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const titleMap = {
  '/admin/dashboard': '仪表盘',
  '/admin/vehicles': '车辆管理',
  '/admin/orders': '订单管理',
  '/admin/users': '用户管理',
  '/admin/coupons': '营销管理',
  '/admin/stores': '门店管理'
}

const currentTitle = computed(() => titleMap[route.path] || '管理后台')

const handleCommand = (command) => {
  if (command === 'home') {
    router.push('/')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.admin-user span {
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
</style>
