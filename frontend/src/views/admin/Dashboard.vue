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
        <div class="stat-icon" style="background: #909399;">
          <el-icon><Money /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-title">本月营收</span>
          <span class="stat-value">¥{{ monthlyRevenue || 0 }}</span>
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

        <!-- Pending After-Sales -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <span>待处理售后申请 ({{ pendingAfterSalesCount }})</span>
              <el-button type="primary" link @click="$router.push('/admin/after-sales')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="pendingAfterSales" stripe>
            <el-table-column prop="afterNo" label="售后单号" width="180" />
            <el-table-column prop="orderNo" label="订单号" width="180" />
            <el-table-column prop="userName" label="用户" width="100" />
            <el-table-column prop="type" label="类型" width="80">
              <template #default="{ row }">
                {{ afterSalesTypeText(row.type) }}
              </template>
            </el-table-column>
            <el-table-column prop="refundAmount" label="申请金额" width="110">
              <template #default="{ row }">
                <span v-if="row.type === 2">¥{{ row.refundAmount || 0 }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="pendingAfterSales.length === 0" description="暂无待处理售后申请" />
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

        <!-- Overdue Maintenance -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <span>逾期保养车辆 ({{ maintenanceOverdueCount }})</span>
              <el-button type="primary" link @click="$router.push('/admin/maintenance')">去处理</el-button>
            </div>
          </template>
          <el-table :data="maintenanceOverdue" size="small">
            <el-table-column prop="plateNumber" label="车牌" width="100" />
            <el-table-column label="车辆">
              <template #default="{ row }">
                {{ row.brand }} {{ row.model }}
              </template>
            </el-table-column>
            <el-table-column prop="lastMaintenanceDate" label="最近保养" width="110">
              <template #default="{ row }">
                {{ formatDate(row.lastMaintenanceDate) }}
              </template>
            </el-table-column>
            <el-table-column label="逾期(天)" width="90">
              <template #default="{ row }">
                <span v-if="row.daysOverdue !== null && row.daysOverdue !== undefined">{{ row.daysOverdue }}</span>
                <span v-else>未登记</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="maintenanceOverdue.length === 0" description="暂无逾期车辆" />
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

    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <span>营收与订单趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="loadTrendData">
                <el-radio-button :label="7">近7天</el-radio-button>
                <el-radio-button :label="14">近14天</el-radio-button>
                <el-radio-button :label="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-panel chart-panel-lg"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>车辆状态分布</span>
          </template>
          <div ref="vehicleChartRef" class="chart-panel chart-panel-sm"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>门店营收 TOP5</span>
          </template>
          <div ref="storeChartRef" class="chart-panel chart-panel-md"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import * as echarts from 'echarts'
import { Document, Money, Van, TrendCharts, Warning, CircleClose, InfoFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const todayStats = ref({})
const vehicleStats = ref({})
const orderStats = ref({})
const monthlyRevenue = ref(0)
const pendingOrders = ref([])
const pendingAfterSales = ref([])
const pendingAfterSalesCount = ref(0)
const alerts = ref([])
const topVehicles = ref([])
const topStores = ref([])
const maintenanceOverdue = ref([])
const maintenanceOverdueCount = ref(0)
const trendDays = ref(14)
const trendData = ref([])

const trendChartRef = ref(null)
const storeChartRef = ref(null)
const vehicleChartRef = ref(null)

let trendChart = null
let storeChart = null
let vehicleChart = null

const loadDashboard = async () => {
  try {
    const [overviewRes, orderStatsRes, alertsRes, rankingsRes, monthStatsRes] = await Promise.all([
      api.admin.dashboard.overview(),
      api.admin.orders.stats(),
      api.admin.dashboard.alerts(),
      api.admin.dashboard.rankings(),
      api.admin.dashboard.stats({ period: 'month' })
    ])
    
    if (overviewRes.code === 200) {
      todayStats.value = overviewRes.data.today || {}
      vehicleStats.value = overviewRes.data.vehicles || {}
      maintenanceOverdue.value = overviewRes.data.maintenanceOverdue || []
      maintenanceOverdueCount.value = overviewRes.data.maintenanceOverdueCount || 0
    }
    
    if (orderStatsRes.code === 200) {
      orderStats.value = orderStatsRes.data
    }
    
    if (alertsRes.code === 200) {
      alerts.value = alertsRes.data
    }
    
    if (rankingsRes.code === 200) {
      topVehicles.value = rankingsRes.data.topVehicles || []
      topStores.value = rankingsRes.data.topStores || []
    }

    if (monthStatsRes.code === 200) {
      monthlyRevenue.value = monthStatsRes.data?.revenue ?? 0
    }
    await updateCharts()
  } catch (error) {
    console.error('Failed to load dashboard:', error)
  }
}

const formatAmount = (value) => {
  const num = Number(value ?? 0)
  if (Number.isNaN(num)) return '0'
  const rounded = Math.round(num * 10) / 10
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1)
}

const initCharts = () => {
  if (trendChartRef.value && !trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  if (storeChartRef.value && !storeChart) {
    storeChart = echarts.init(storeChartRef.value)
  }
  if (vehicleChartRef.value && !vehicleChart) {
    vehicleChart = echarts.init(vehicleChartRef.value)
  }
}

const resizeCharts = () => {
  trendChart?.resize()
  storeChart?.resize()
  vehicleChart?.resize()
}

const buildTrendOption = () => {
  const rows = Array.isArray(trendData.value) ? trendData.value : []
  const dates = rows.map(item => item.date)
  const orders = rows.map(item => Number(item.orders || 0))
  const revenue = rows.map(item => Number(item.revenue || 0))
  const hasData = rows.length > 0

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params) => {
        const title = params?.[0]?.axisValue || ''
        const orderItem = params?.find(p => p.seriesName === '订单数')
        const revenueItem = params?.find(p => p.seriesName === '营收')
        const orderVal = orderItem?.data ?? 0
        const revenueVal = revenueItem?.data ?? 0
        return `${title}<br/>订单数：${orderVal}<br/>营收：¥${formatAmount(revenueVal)}`
      }
    },
    legend: {
      data: ['订单数', '营收'],
      right: 10
    },
    grid: {
      left: 40,
      right: 50,
      top: 45,
      bottom: 35
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisTick: { alignWithLabel: true },
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#606266' }
    },
    yAxis: [
      {
        type: 'value',
        name: '订单',
        axisLabel: { color: '#606266' },
        splitLine: { lineStyle: { color: '#f0f2f5' } }
      },
      {
        type: 'value',
        name: '营收(¥)',
        axisLabel: { formatter: (val) => formatAmount(val), color: '#606266' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: orders,
        barWidth: 12,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#A5D3FF' }
          ])
        }
      },
      {
        name: '营收',
        type: 'line',
        data: revenue,
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#67C23A' },
        itemStyle: { color: '#67C23A' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.35)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
          ])
        }
      }
    ],
    graphic: hasData
      ? []
      : [{
          type: 'text',
          left: 'center',
          top: 'middle',
          style: {
            text: '暂无数据',
            fill: '#909399',
            fontSize: 14
          }
        }]
  }
}

const buildStoreOption = () => {
  const rows = Array.isArray(topStores.value) ? topStores.value : []
  const names = rows.map(item => item.name)
  const values = rows.map(item => Number(item.revenue || 0))
  const hasData = rows.length > 0

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = params?.[0]
        if (!item) return ''
        return `${item.axisValue}<br/>营收：¥${formatAmount(item.data)}`
      }
    },
    grid: {
      left: 100,
      right: 30,
      top: 20,
      bottom: 25
    },
    xAxis: {
      type: 'value',
      axisLabel: { formatter: (val) => formatAmount(val), color: '#606266' },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    yAxis: {
      type: 'category',
      data: names,
      inverse: true,
      axisTick: { show: false },
      axisLabel: { color: '#606266' }
    },
    series: [
      {
        type: 'bar',
        data: values,
        barWidth: 16,
        itemStyle: {
          borderRadius: 6,
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#F56C6C' },
            { offset: 1, color: '#FAD0C4' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          formatter: (p) => `¥${formatAmount(p.value)}`,
          color: '#303133'
        }
      }
    ],
    graphic: hasData
      ? []
      : [{
          type: 'text',
          left: 'center',
          top: 'middle',
          style: {
            text: '暂无数据',
            fill: '#909399',
            fontSize: 14
          }
        }]
  }
}

const buildVehicleOption = () => {
  const total = Number(vehicleStats.value?.total || 0)
  const available = Number(vehicleStats.value?.available || 0)
  const rented = Number(vehicleStats.value?.rented || 0)
  const maintenance = Number(vehicleStats.value?.maintenance || 0)
  const bookedRaw = vehicleStats.value?.booked
  const bookedFallback = Math.max(total - available - rented - maintenance, 0)
  const booked = Number.isFinite(Number(bookedRaw)) ? Number(bookedRaw) : bookedFallback
  const data = [
    { value: available, name: '可租' },
    { value: booked, name: '预订中' },
    { value: rented, name: '出租中' },
    { value: maintenance, name: '维修中' }
  ].filter(item => item.value > 0)
  const hasData = data.length > 0

  return {
    title: {
      text: total ? String(total) : '0',
      subtext: '总车辆',
      left: 'center',
      top: '36%',
      textStyle: { fontSize: 22, fontWeight: 600, color: '#303133' },
      subtextStyle: { fontSize: 12, color: '#909399' }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params) => `${params.name}：${params.value} (${params.percent}%)`
    },
    legend: {
      bottom: 0,
      left: 'center'
    },
    series: [
      {
        name: '车辆状态',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        data: hasData ? data : [{
          value: 1,
          name: '暂无数据',
          itemStyle: { color: '#ebeef5' },
          label: { show: false },
          tooltip: { show: false }
        }],
        label: { color: '#606266', formatter: '{b} {c}' },
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }
      }
    ]
  }
}

const updateCharts = async () => {
  await nextTick()
  initCharts()
  if (trendChart) trendChart.setOption(buildTrendOption(), true)
  if (storeChart) storeChart.setOption(buildStoreOption(), true)
  if (vehicleChart) vehicleChart.setOption(buildVehicleOption(), true)
}

const loadTrendData = async () => {
  try {
    const res = await api.admin.dashboard.trends({ days: trendDays.value })
    if (res.code === 200) {
      trendData.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    console.error('Failed to load trend data:', error)
  }
  await updateCharts()
}

const formatDate = (value) => {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD')
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

const afterSalesTypeText = (type) => {
  const map = {
    1: '报修',
    2: '退款',
    3: '投诉',
    4: '其他'
  }
  return map[type] || '未知'
}

const loadPendingAfterSales = async () => {
  try {
    const res = await api.admin.afterSales.list({ status: 1, page: 1, pageSize: 5 })
    if (res.code === 200) {
      pendingAfterSales.value = res.data?.list || []
      pendingAfterSalesCount.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('Failed to load pending after-sales:', error)
  }
}

onMounted(() => {
  loadDashboard()
  loadPendingOrders()
  loadPendingAfterSales()
  loadTrendData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  storeChart?.dispose()
  vehicleChart?.dispose()
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

.charts-row {
  margin-top: 20px;
}

.chart-panel {
  width: 100%;
  height: 320px;
}

.chart-panel-lg { height: 360px; }
.chart-panel-md { height: 300px; }
.chart-panel-sm { height: 280px; }

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
