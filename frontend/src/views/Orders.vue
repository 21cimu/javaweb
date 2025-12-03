<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '../api'

const router = useRouter()
const orders = ref([])
const loading = ref(true)
const activeTab = ref('all')

const statusMap = {
  PENDING: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'success' },
  CONFIRMED: { text: '已确认', type: 'success' },
  PICKED_UP: { text: '已取车', type: 'primary' },
  RETURNED: { text: '已还车', type: 'info' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'info' },
  REFUNDED: { text: '已退款', type: 'danger' }
}

const tabFilters = {
  all: [],
  pending: ['PENDING'],
  active: ['PAID', 'CONFIRMED', 'PICKED_UP'],
  completed: ['RETURNED', 'COMPLETED'],
  cancelled: ['CANCELLED', 'REFUNDED']
}

onMounted(async () => {
  await loadOrders()
})

const loadOrders = async () => {
  loading.value = true
  try {
    const user = JSON.parse(localStorage.getItem('user'))
    if (!user) {
      router.push('/login')
      return
    }
    orders.value = await orderApi.getByUserId(user.id)
  } catch (e) {
    console.error('Failed to load orders:', e)
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

const filteredOrders = () => {
  const filter = tabFilters[activeTab.value]
  if (!filter || filter.length === 0) {
    return orders.value
  }
  return orders.value.filter(order => filter.includes(order.status))
}

const goToDetail = (id) => {
  router.push(`/order/${id}`)
}

const payOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认支付该订单？', '支付确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    
    const res = await orderApi.pay(order.id, 'ALIPAY')
    if (res.success) {
      ElMessage.success('支付成功')
      await loadOrders()
    } else {
      ElMessage.error(res.error || '支付失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('支付失败')
    }
  }
}

const cancelOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '取消订单', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await orderApi.cancel(order.id)
    if (res.success) {
      ElMessage.success('订单已取消')
      await loadOrders()
    } else {
      ElMessage.error(res.error || '取消失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('取消订单失败')
    }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}
</script>

<template>
  <div class="orders-page">
    <el-card>
      <template #header>
        <h2>我的订单</h2>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="全部订单" name="all" />
        <el-tab-pane label="待支付" name="pending" />
        <el-tab-pane label="进行中" name="active" />
        <el-tab-pane label="已完成" name="completed" />
        <el-tab-pane label="已取消" name="cancelled" />
      </el-tabs>
      
      <div v-loading="loading" class="order-list">
        <el-empty v-if="!loading && filteredOrders().length === 0" description="暂无订单" />
        
        <el-card 
          v-for="order in filteredOrders()" 
          :key="order.id"
          class="order-item"
          shadow="hover"
        >
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <el-tag :type="statusMap[order.status]?.type || 'info'">
              {{ statusMap[order.status]?.text || order.status }}
            </el-tag>
          </div>
          
          <div class="order-content">
            <div class="vehicle-info" v-if="order.vehicle">
              <el-image 
                :src="order.vehicle.imageUrl || 'https://via.placeholder.com/120x80?text=Car'"
                fit="cover"
                style="width: 120px; height: 80px; border-radius: 4px;"
              />
              <div class="vehicle-detail">
                <h3>{{ order.vehicle.brand }} {{ order.vehicle.model }}</h3>
                <p>取车时间：{{ formatDate(order.pickupTime) }}</p>
                <p>还车时间：{{ formatDate(order.returnTime) }}</p>
              </div>
            </div>
            
            <div class="order-price">
              <p>租赁天数：{{ order.rentalDays }}天</p>
              <p class="total-price">¥{{ order.totalPrice }}</p>
            </div>
          </div>
          
          <div class="order-actions">
            <el-button size="small" @click="goToDetail(order.id)">查看详情</el-button>
            <el-button 
              v-if="order.status === 'PENDING'"
              type="primary" 
              size="small" 
              @click="payOrder(order)"
            >
              立即支付
            </el-button>
            <el-button 
              v-if="['PENDING', 'PAID', 'CONFIRMED'].includes(order.status)"
              type="danger" 
              size="small" 
              @click="cancelOrder(order)"
            >
              取消订单
            </el-button>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.orders-page {
  max-width: 900px;
  margin: 0 auto;
}

.orders-page h2 {
  margin: 0;
}

.order-list {
  min-height: 300px;
}

.order-item {
  margin-bottom: 16px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.order-no {
  font-size: 14px;
  color: #909399;
}

.order-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.vehicle-info {
  display: flex;
  gap: 16px;
}

.vehicle-detail h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.vehicle-detail p {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
}

.order-price {
  text-align: right;
}

.order-price p {
  margin: 4px 0;
}

.total-price {
  font-size: 20px;
  font-weight: bold;
  color: #F56C6C;
}

.order-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #EBEEF5;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
