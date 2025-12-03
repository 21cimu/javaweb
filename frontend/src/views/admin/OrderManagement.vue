<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, vehicleApi } from '../../api'

const orders = ref([])
const loading = ref(true)
const statusFilter = ref('')

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

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING' },
  { label: '已支付', value: 'PAID' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已取车', value: 'PICKED_UP' },
  { label: '已还车', value: 'RETURNED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

onMounted(async () => {
  await loadOrders()
})

const loadOrders = async () => {
  loading.value = true
  try {
    orders.value = await orderApi.getAll()
  } catch (e) {
    console.error('Failed to load orders:', e)
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

const filteredOrders = () => {
  if (!statusFilter.value) {
    return orders.value
  }
  return orders.value.filter(order => order.status === statusFilter.value)
}

const confirmOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认该订单?', '确认操作', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    
    const res = await orderApi.pay(order.id, order.paymentMethod || 'ALIPAY')
    if (res.success) {
      ElMessage.success('订单已确认')
      await loadOrders()
    } else {
      ElMessage.error(res.error || '操作失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const pickupOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认用户已取车?', '取车确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    
    const res = await orderApi.pickup(order.id)
    if (res.success) {
      ElMessage.success('已确认取车')
      await loadOrders()
    } else {
      ElMessage.error(res.error || '操作失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const returnOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认用户已还车?', '还车确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    
    const res = await orderApi.return(order.id)
    if (res.success) {
      ElMessage.success('已确认还车')
      await loadOrders()
    } else {
      ElMessage.error(res.error || '操作失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const completeOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认完成该订单?', '完成确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    
    const res = await orderApi.complete(order.id)
    if (res.success) {
      ElMessage.success('订单已完成')
      await loadOrders()
    } else {
      ElMessage.error(res.error || '操作失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const cancelOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认取消该订单?', '取消订单', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await orderApi.cancel(order.id)
    if (res.success) {
      ElMessage.success('订单已取消')
      await loadOrders()
    } else {
      ElMessage.error(res.error || '操作失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const deleteOrder = async (order) => {
  try {
    await ElMessageBox.confirm(`确认删除订单 ${order.orderNo}?`, '删除确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await orderApi.delete(order.id)
    if (res.success) {
      ElMessage.success('删除成功')
      await loadOrders()
    } else {
      ElMessage.error(res.error || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<template>
  <div class="order-management-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>订单管理</h2>
          <el-select v-model="statusFilter" placeholder="筛选状态" style="width: 150px">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
      </template>
      
      <el-table :data="filteredOrders()" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="车辆" width="160">
          <template #default="{ row }">
            {{ row.vehicle?.brand }} {{ row.vehicle?.model }}
          </template>
        </el-table-column>
        <el-table-column prop="rentalDays" label="天数" width="80" />
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
        <el-table-column prop="paymentStatus" label="支付" width="80">
          <template #default="{ row }">
            <el-tag :type="row.paymentStatus === 'PAID' ? 'success' : 'warning'" size="small">
              {{ row.paymentStatus === 'PAID' ? '已付' : '未付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pickupTime" label="取车时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.pickupTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 'PAID'"
              type="primary" 
              size="small" 
              @click="pickupOrder(row)"
            >
              确认取车
            </el-button>
            <el-button 
              v-if="row.status === 'PICKED_UP'"
              type="success" 
              size="small" 
              @click="returnOrder(row)"
            >
              确认还车
            </el-button>
            <el-button 
              v-if="row.status === 'RETURNED'"
              type="primary" 
              size="small" 
              @click="completeOrder(row)"
            >
              完成订单
            </el-button>
            <el-button 
              v-if="['PENDING', 'PAID', 'CONFIRMED'].includes(row.status)"
              type="warning" 
              size="small" 
              @click="cancelOrder(row)"
            >
              取消
            </el-button>
            <el-button 
              v-if="['COMPLETED', 'CANCELLED'].includes(row.status)"
              type="danger" 
              size="small" 
              @click="deleteOrder(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.order-management-page {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
}
</style>
