<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '../api'

const router = useRouter()
const route = useRoute()
const order = ref(null)
const loading = ref(true)

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

const statusSteps = ['PENDING', 'PAID', 'CONFIRMED', 'PICKED_UP', 'RETURNED', 'COMPLETED']

onMounted(async () => {
  const id = route.params.id
  if (id) {
    await loadOrder(id)
  }
})

const loadOrder = async (id) => {
  loading.value = true
  try {
    order.value = await orderApi.getById(id)
  } catch (e) {
    console.error('Failed to load order:', e)
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

const getCurrentStep = () => {
  if (!order.value) return 0
  if (order.value.status === 'CANCELLED' || order.value.status === 'REFUNDED') return -1
  return statusSteps.indexOf(order.value.status)
}

const payOrder = async () => {
  try {
    await ElMessageBox.confirm('确认支付该订单？', '支付确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    
    const res = await orderApi.pay(order.value.id, 'ALIPAY')
    if (res.success) {
      ElMessage.success('支付成功')
      await loadOrder(order.value.id)
    } else {
      ElMessage.error(res.error || '支付失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('支付失败')
    }
  }
}

const cancelOrder = async () => {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '取消订单', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await orderApi.cancel(order.value.id)
    if (res.success) {
      ElMessage.success('订单已取消')
      await loadOrder(order.value.id)
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

const goBack = () => {
  router.push('/orders')
}
</script>

<template>
  <div class="order-detail-page">
    <el-button @click="goBack" icon="ArrowLeft" class="back-btn">返回订单列表</el-button>
    
    <div v-loading="loading" class="detail-content">
      <template v-if="order">
        <!-- Order Status Steps -->
        <el-card class="status-card">
          <template v-if="order.status !== 'CANCELLED' && order.status !== 'REFUNDED'">
            <el-steps :active="getCurrentStep()" finish-status="success">
              <el-step title="待支付" />
              <el-step title="已支付" />
              <el-step title="已确认" />
              <el-step title="已取车" />
              <el-step title="已还车" />
              <el-step title="已完成" />
            </el-steps>
          </template>
          <template v-else>
            <el-result 
              icon="info" 
              :title="statusMap[order.status]?.text"
              sub-title="该订单已取消或退款"
            />
          </template>
        </el-card>
        
        <!-- Order Info -->
        <el-row :gutter="20">
          <el-col :xs="24" :md="16">
            <el-card class="info-card">
              <template #header>
                <div class="card-header">
                  <h3>订单信息</h3>
                  <el-tag :type="statusMap[order.status]?.type || 'info'" size="large">
                    {{ statusMap[order.status]?.text || order.status }}
                  </el-tag>
                </div>
              </template>
              
              <el-descriptions :column="2" border>
                <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDate(order.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="取车时间">{{ formatDate(order.pickupTime) }}</el-descriptions-item>
                <el-descriptions-item label="还车时间">{{ formatDate(order.returnTime) }}</el-descriptions-item>
                <el-descriptions-item label="取车方式">
                  {{ order.pickupMethod === 'STORE' ? '到店自取' : '送车上门' }}
                </el-descriptions-item>
                <el-descriptions-item label="还车方式">
                  {{ order.returnMethod === 'STORE' ? '到店还车' : '上门取车' }}
                </el-descriptions-item>
                <el-descriptions-item v-if="order.actualPickupTime" label="实际取车时间">
                  {{ formatDate(order.actualPickupTime) }}
                </el-descriptions-item>
                <el-descriptions-item v-if="order.actualReturnTime" label="实际还车时间">
                  {{ formatDate(order.actualReturnTime) }}
                </el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">
                  {{ order.remark || '无' }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
            
            <!-- Vehicle Info -->
            <el-card v-if="order.vehicle" class="vehicle-card">
              <template #header>
                <h3>车辆信息</h3>
              </template>
              
              <div class="vehicle-content">
                <el-image 
                  :src="order.vehicle.imageUrl || 'https://via.placeholder.com/200x150?text=Car'"
                  fit="cover"
                  style="width: 200px; height: 150px; border-radius: 8px;"
                />
                <div class="vehicle-info">
                  <h2>{{ order.vehicle.brand }} {{ order.vehicle.model }}</h2>
                  <p>车牌号：{{ order.vehicle.plateNumber }}</p>
                  <p>颜色：{{ order.vehicle.color }}</p>
                  <p>座位：{{ order.vehicle.seats }}座</p>
                </div>
              </div>
            </el-card>
          </el-col>
          
          <el-col :xs="24" :md="8">
            <!-- Price Card -->
            <el-card class="price-card">
              <template #header>
                <h3>费用明细</h3>
              </template>
              
              <div class="price-item">
                <span>日租金</span>
                <span>¥{{ order.dailyPrice }}</span>
              </div>
              <div class="price-item">
                <span>租赁天数</span>
                <span>{{ order.rentalDays }}天</span>
              </div>
              <div class="price-item">
                <span>押金</span>
                <span>¥{{ order.deposit || 0 }}</span>
              </div>
              <div class="price-item">
                <span>保险费</span>
                <span>¥{{ order.insuranceFee || 0 }}</span>
              </div>
              <div class="price-item">
                <span>服务费</span>
                <span>¥{{ order.serviceFee || 0 }}</span>
              </div>
              <div v-if="order.discountAmount > 0" class="price-item discount">
                <span>优惠</span>
                <span>-¥{{ order.discountAmount }}</span>
              </div>
              <el-divider />
              <div class="price-item total">
                <span>合计租金</span>
                <span>¥{{ order.totalPrice }}</span>
              </div>
            </el-card>
            
            <!-- Actions Card -->
            <el-card class="actions-card">
              <el-button 
                v-if="order.status === 'PENDING'"
                type="primary" 
                size="large"
                @click="payOrder"
                style="width: 100%"
              >
                立即支付
              </el-button>
              <el-button 
                v-if="['PENDING', 'PAID', 'CONFIRMED'].includes(order.status)"
                type="danger" 
                size="large"
                @click="cancelOrder"
                style="width: 100%"
              >
                取消订单
              </el-button>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </div>
  </div>
</template>

<style scoped>
.order-detail-page {
  max-width: 1200px;
  margin: 0 auto;
}

.back-btn {
  margin-bottom: 20px;
}

.status-card {
  margin-bottom: 20px;
}

.info-card,
.vehicle-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}

.vehicle-content {
  display: flex;
  gap: 20px;
}

.vehicle-info h2 {
  margin: 0 0 12px 0;
}

.vehicle-info p {
  margin: 6px 0;
  color: #606266;
}

.price-card {
  margin-bottom: 20px;
}

.price-card h3 {
  margin: 0;
}

.price-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.price-item.discount span:last-child {
  color: #67C23A;
}

.price-item.total {
  font-size: 18px;
  font-weight: bold;
}

.price-item.total span:last-child {
  color: #F56C6C;
}

.actions-card .el-button + .el-button {
  margin-top: 12px;
  margin-left: 0;
}
</style>
