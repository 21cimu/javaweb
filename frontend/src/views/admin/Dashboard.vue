<script setup>
import { ref, onMounted } from 'vue'
import { vehicleApi, orderApi, userApi } from '../../api'

const stats = ref({
  totalVehicles: 0,
  availableVehicles: 0,
  totalOrders: 0,
  pendingOrders: 0,
  totalUsers: 0
})

const recentOrders = ref([])
const loading = ref(true)

const statusMap = {
  PENDING: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'success' },
  CONFIRMED: { text: '已确认', type: 'success' },
  PICKED_UP: { text: '已取车', type: 'primary' },
  RETURNED: { text: '已还车', type: 'info' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'info' }
}

onMounted(async () => {
  await loadDashboardData()
})

const loadDashboardData = async () => {
  loading.value = true
  try {
    // Load vehicles
    const vehicles = await vehicleApi.getAll()
    stats.value.totalVehicles = vehicles.length
    stats.value.availableVehicles = vehicles.filter(v => v.status === 'AVAILABLE').length
    
    // Load orders
    const orders = await orderApi.getAll()
    stats.value.totalOrders = orders.length
    stats.value.pendingOrders = orders.filter(o => o.status === 'PENDING').length
    recentOrders.value = orders.slice(0, 5)
    
    // Load users
    const users = await userApi.getAll()
    stats.value.totalUsers = users.length
  } catch (e) {
    console.error('Failed to load dashboard data:', e)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<template>
  <div class="dashboard-page">
    <h1>管理后台</h1>
    
    <!-- Stats Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card v-loading="loading" class="stat-card">
          <div class="stat-content">
            <el-icon :size="40" color="#409EFF"><Van /></el-icon>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalVehicles }}</span>
              <span class="stat-label">总车辆数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="8" :md="4">
        <el-card v-loading="loading" class="stat-card">
          <div class="stat-content">
            <el-icon :size="40" color="#67C23A"><CircleCheck /></el-icon>
            <div class="stat-info">
              <span class="stat-value">{{ stats.availableVehicles }}</span>
              <span class="stat-label">可用车辆</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="8" :md="4">
        <el-card v-loading="loading" class="stat-card">
          <div class="stat-content">
            <el-icon :size="40" color="#E6A23C"><Document /></el-icon>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalOrders }}</span>
              <span class="stat-label">总订单数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="8" :md="4">
        <el-card v-loading="loading" class="stat-card">
          <div class="stat-content">
            <el-icon :size="40" color="#F56C6C"><Bell /></el-icon>
            <div class="stat-info">
              <span class="stat-value">{{ stats.pendingOrders }}</span>
              <span class="stat-label">待处理订单</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="8" :md="4">
        <el-card v-loading="loading" class="stat-card">
          <div class="stat-content">
            <el-icon :size="40" color="#909399"><User /></el-icon>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalUsers }}</span>
              <span class="stat-label">注册用户</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- Quick Links -->
    <el-row :gutter="20" class="links-row">
      <el-col :span="8">
        <router-link to="/admin/vehicles">
          <el-card class="link-card" shadow="hover">
            <el-icon :size="48"><Van /></el-icon>
            <h3>车辆管理</h3>
            <p>管理车辆信息、库存、定价</p>
          </el-card>
        </router-link>
      </el-col>
      <el-col :span="8">
        <router-link to="/admin/orders">
          <el-card class="link-card" shadow="hover">
            <el-icon :size="48"><Document /></el-icon>
            <h3>订单管理</h3>
            <p>处理订单、取还车管理</p>
          </el-card>
        </router-link>
      </el-col>
      <el-col :span="8">
        <el-card class="link-card" shadow="hover">
          <el-icon :size="48"><DataAnalysis /></el-icon>
          <h3>数据分析</h3>
          <p>经营数据、统计报表</p>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- Recent Orders -->
    <el-card class="recent-orders-card">
      <template #header>
        <div class="card-header">
          <h3>最近订单</h3>
          <router-link to="/admin/orders">
            <el-button type="primary" link>查看全部</el-button>
          </router-link>
        </div>
      </template>
      
      <el-table :data="recentOrders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="车辆" width="200">
          <template #default="{ row }">
            {{ row.vehicle?.brand }} {{ row.vehicle?.model }}
          </template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="金额" width="100">
          <template #default="{ row }">
            ¥{{ row.totalPrice }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard-page {
  max-width: 1400px;
  margin: 0 auto;
}

.dashboard-page h1 {
  margin-bottom: 24px;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  height: 100%;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.links-row {
  margin-bottom: 24px;
}

.links-row a {
  text-decoration: none;
}

.link-card {
  text-align: center;
  padding: 20px 0;
  cursor: pointer;
  transition: transform 0.3s;
}

.link-card:hover {
  transform: translateY(-4px);
}

.link-card h3 {
  margin: 16px 0 8px;
}

.link-card p {
  color: #909399;
  font-size: 14px;
}

.recent-orders-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.recent-orders-card h3 {
  margin: 0;
}
</style>
