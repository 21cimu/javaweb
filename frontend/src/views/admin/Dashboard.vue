<template>
  <div class="dashboard-page">
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: #409EFF;">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-title">今日订单</span>
          <span class="stat-value">{{ todayStats.orderCount || 0 }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #67C23A;">
          <el-icon><Money /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-title">今日营收</span>
          <span class="stat-value">¥{{ todayStats.gmv || 0 }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #E6A23C;">
          <el-icon><Van /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-title">可租车辆</span>
          <span class="stat-value">{{ vehicleStats.available || 0 }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #F56C6C;">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-title">出租率</span>
          <span class="stat-value">{{ todayStats.rentalRate || '0%' }}</span>
        </div>
      </div>
    </div>
    
    <el-row :gutter="20">
      <el-col :span="16">
        <!-- Pending Orders -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <span>待处理订单</span>
              <el-button type="primary" link @click="$router.push('/admin/orders')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="pendingOrders" stripe>
            <el-table-column prop="orderNo" label="订单号" width="180" />
            <el-table-column prop="vehicleName" label="车辆" />
            <el-table-column prop="userName" label="用户" width="100" />
            <el-table-column prop="totalAmount" label="金额" width="100">
              <template #default="{ row }">¥{{ row.totalAmount }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'warning' : 'primary'" size="small">
                  {{ row.status === 1 ? '待审核' : '待支付' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="row.status === 1" type="primary" size="small" @click="approveOrder(row)">
                  审核
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="pendingOrders.length === 0" description="暂无待处理订单" />
        </el-card>
        
        <!-- Order Stats -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>订单统计</span>
          </template>
          <div class="order-stats">
            <div class="stat-item">
              <span class="label">待审核</span>
              <span class="value">{{ orderStats.pending || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="label">待支付</span>
              <span class="value">{{ orderStats.waitingPayment || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="label">待取车</span>
              <span class="value">{{ orderStats.waitingPickup || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="label">用车中</span>
              <span class="value">{{ orderStats.inUse || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="label">已完成</span>
              <span class="value">{{ orderStats.completed || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <!-- Alerts -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>系统预警</span>
          </template>
          <div class="alert-list">
            <div v-for="(alert, index) in alerts" :key="index" class="alert-item" :class="alert.level">
              <el-icon v-if="alert.level === 'warning'"><Warning /></el-icon>
              <el-icon v-else-if="alert.level === 'danger'"><CircleClose /></el-icon>
              <el-icon v-else><InfoFilled /></el-icon>
              <span>{{ alert.message }}</span>
            </div>
            <el-empty v-if="alerts.length === 0" description="暂无预警" />
          </div>
        </el-card>
        
        <!-- Vehicle Stats -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>车辆状态</span>
          </template>
          <div class="vehicle-stats">
            <div class="stat-row">
              <span>总车辆</span>
              <span>{{ vehicleStats.total || 0 }}</span>
            </div>
            <div class="stat-row">
              <span>可租</span>
              <span class="success">{{ vehicleStats.available || 0 }}</span>
            </div>
            <div class="stat-row">
              <span>预订中</span>
              <span class="warning">{{ vehicleStats.booked || 0 }}</span>
            </div>
            <div class="stat-row">
              <span>出租中</span>
              <span class="primary">{{ vehicleStats.rented || 0 }}</span>
            </div>
            <div class="stat-row">
              <span>维修中</span>
              <span class="danger">{{ vehicleStats.maintenance || 0 }}</span>
            </div>
          </div>
        </el-card>
        
        <!-- Top Vehicles -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>热门车型</span>
          </template>
          <div class="ranking-list">
            <div v-for="(item, index) in topVehicles" :key="item.id" class="ranking-item">
              <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="name">{{ item.name }}</span>
              <span class="count">{{ item.orderCount }}单</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import { Document, Money, Van, TrendCharts, Warning, CircleClose, InfoFilled } from '@element-plus/icons-vue'

const todayStats = ref({})
const vehicleStats = ref({})
const orderStats = ref({})
const pendingOrders = ref([])
const alerts = ref([])
const topVehicles = ref([])

const loadDashboard = async () => {
  try {
    const [overviewRes, orderStatsRes, alertsRes, rankingsRes] = await Promise.all([
      api.admin.dashboard.overview(),
      api.admin.orders.stats(),
      api.admin.dashboard.alerts(),
      api.admin.dashboard.rankings()
    ])
    
    if (overviewRes.code === 200) {
      todayStats.value = overviewRes.data.today || {}
      vehicleStats.value = overviewRes.data.vehicles || {}
    }
    
    if (orderStatsRes.code === 200) {
      orderStats.value = orderStatsRes.data
    }
    
    if (alertsRes.code === 200) {
      alerts.value = alertsRes.data
    }
    
    if (rankingsRes.code === 200) {
      topVehicles.value = rankingsRes.data.topVehicles || []
    }
  } catch (error) {
    console.error('Failed to load dashboard:', error)
  }
}

const loadPendingOrders = async () => {
  try {
    const res = await api.admin.orders.pending()
    if (res.code === 200) {
      pendingOrders.value = res.data.slice(0, 5)
    }
  } catch (error) {
    console.error('Failed to load pending orders:', error)
  }
}

const approveOrder = async (order) => {
  try {
    const res = await api.admin.orders.approve({ orderId: order.id })
    if (res.code === 200) {
      ElMessage.success('审核通过')
      loadPendingOrders()
      loadDashboard()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadDashboard()
  loadPendingOrders()
})
</script>

<style scoped>
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
}

.stat-info {
  flex: 1;
}

.stat-title {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.section-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-stats {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.order-stats .stat-item .label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 5px;
}

.order-stats .stat-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.alert-list {
  max-height: 200px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  margin-bottom: 10px;
  border-radius: 6px;
  font-size: 13px;
}

.alert-item.warning {
  background: #FEF0E6;
  color: #E6A23C;
}

.alert-item.danger {
  background: #FDECEA;
  color: #F56C6C;
}

.alert-item.info {
  background: #E8F4FD;
  color: #409EFF;
}

.vehicle-stats .stat-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.vehicle-stats .stat-row:last-child {
  border-bottom: none;
}

.vehicle-stats .success { color: #67C23A; }
.vehicle-stats .warning { color: #E6A23C; }
.vehicle-stats .primary { color: #409EFF; }
.vehicle-stats .danger { color: #F56C6C; }

.ranking-list .ranking-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.ranking-list .ranking-item:last-child {
  border-bottom: none;
}

.ranking-item .rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ebeef5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  margin-right: 10px;
}

.ranking-item .rank.top {
  background: #F56C6C;
  color: #fff;
}

.ranking-item .name {
  flex: 1;
  font-size: 13px;
}

.ranking-item .count {
  font-size: 13px;
  color: #909399;
}
</style>
