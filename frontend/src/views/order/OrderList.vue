<template>
  <div class="order-list-page">
    <div class="page-header">
      <h1>我的订单</h1>
    </div>
    
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部订单" name="all" />
      <el-tab-pane label="待审核" name="1" />
      <el-tab-pane label="待支付" name="3" />
      <el-tab-pane label="待取车" name="4" />
      <el-tab-pane label="用车中" name="5" />
      <el-tab-pane label="已完成" name="8" />
      <el-tab-pane label="已取消" name="0" />
    </el-tabs>
    
    <div class="order-list" v-loading="loading">
      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="getStatusType(order.status)" size="small">
            {{ getStatusText(order.status) }}
          </el-tag>
        </div>
        
        <div class="order-content" @click="goToDetail(order.id)">
          <img :src="'/images/car-placeholder.jpg'" :alt="order.vehicleName" class="vehicle-thumb">
          <div class="order-info">
            <h3 class="vehicle-name">{{ order.vehicleName }}</h3>
            <p class="rental-time">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(order.pickupTime) }} - {{ formatDate(order.returnTime) }}
            </p>
            <p class="rental-store">
              <el-icon><Location /></el-icon>
              {{ order.pickupStoreName }}
              <span v-if="order.pickupStoreId !== order.returnStoreId">
                → {{ order.returnStoreName }}
              </span>
            </p>
          </div>
          <div class="order-price">
            <span class="amount">¥{{ order.totalAmount }}</span>
            <span class="days">{{ order.rentalDays }}天</span>
          </div>
        </div>
        
        <div class="order-footer">
          <template v-if="order.status === 1">
            <el-button size="small" @click="handleCancel(order)">取消订单</el-button>
          </template>
          <template v-else-if="order.status === 3">
            <el-button size="small" @click="handleCancel(order)">取消订单</el-button>
            <el-button type="primary" size="small" @click="handlePay(order)">立即支付</el-button>
          </template>
          <template v-else-if="order.status === 4">
            <el-button size="small">查看取车码</el-button>
            <el-button size="small" @click="handleCancel(order)">取消订单</el-button>
          </template>
          <template v-else-if="order.status === 8">
            <el-button size="small" @click="handleReorder(order)">再次预订</el-button>
            <el-button v-if="!order.rating" type="primary" size="small" @click="handleReview(order)">
              评价订单
            </el-button>
          </template>
          <el-button size="small" @click="goToDetail(order.id)">查看详情</el-button>
        </div>
      </div>
      
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </div>
    
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="page"
        :total="total"
        :page-size="pageSize"
        layout="prev, pager, next"
        @current-change="loadOrders"
      />
    </div>
    
    <!-- Review Dialog -->
    <el-dialog v-model="reviewDialogVisible" title="评价订单" width="500px">
      <el-form :model="reviewForm" label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" show-text :texts="['很差', '较差', '一般', '满意', '非常满意']" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.review"
            type="textarea"
            :rows="4"
            placeholder="请分享您的用车体验..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import dayjs from 'dayjs'
import { Calendar, Location } from '@element-plus/icons-vue'

const router = useRouter()

const orders = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref('all')

const reviewDialogVisible = ref(false)
const currentOrder = ref(null)
const reviewForm = ref({
  rating: 5,
  review: ''
})

const statusMap = {
  0: '已取消',
  1: '待审核',
  2: '审核失败',
  3: '待支付',
  4: '待取车',
  5: '用车中',
  6: '待还车',
  7: '待结算',
  8: '已完成',
  9: '退款中',
  10: '已退款'
}

const statusTypeMap = {
  0: 'info',
  1: 'warning',
  2: 'danger',
  3: 'warning',
  4: '',
  5: 'success',
  6: 'warning',
  7: 'warning',
  8: 'success',
  9: 'info',
  10: 'info'
}

const getStatusText = (status) => statusMap[status] || '未知'
const getStatusType = (status) => statusTypeMap[status] || 'info'

const formatDate = (date) => {
  return dayjs(date).format('MM-DD HH:mm')
}

const loadOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value
    }
    if (activeTab.value !== 'all') {
      params.status = parseInt(activeTab.value)
    }
    
    const res = await api.orders.list(params)
    if (res.code === 200) {
      orders.value = res.data.list
      total.value = res.data.total
    }
  } catch (error) {
    console.error('Failed to load orders:', error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  page.value = 1
  loadOrders()
}

const goToDetail = (id) => {
  router.push(`/orders/${id}`)
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确定要取消此订单吗？', '提示', {
      type: 'warning'
    })
    
    const res = await api.orders.cancel({
      orderId: order.id,
      reason: '用户主动取消'
    })
    
    if (res.code === 200) {
      ElMessage.success('订单已取消')
      loadOrders()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

const handlePay = async (order) => {
  // In production, redirect to payment page
  try {
    await ElMessageBox.confirm(`确定支付 ¥${order.totalAmount} 吗？`, '确认支付', {
      type: 'info'
    })
    
    const res = await api.orders.pay({
      orderId: order.id,
      paymentMethod: 1 // WeChat
    })
    
    if (res.code === 200) {
      ElMessage.success('支付成功')
      loadOrders()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('支付失败')
    }
  }
}

const handleReorder = (order) => {
  router.push(`/vehicles/${order.vehicleId}`)
}

const handleReview = (order) => {
  currentOrder.value = order
  reviewForm.value = { rating: 5, review: '' }
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  if (!reviewForm.value.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  
  try {
    const res = await api.orders.review({
      orderId: currentOrder.value.id,
      rating: reviewForm.value.rating,
      review: reviewForm.value.review
    })
    
    if (res.code === 200) {
      ElMessage.success('评价成功')
      reviewDialogVisible.value = false
      loadOrders()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('评价失败')
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.order-list-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #303133;
}

.order-list {
  min-height: 400px;
}

.order-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.order-no {
  font-size: 13px;
  color: #909399;
}

.order-content {
  display: flex;
  gap: 20px;
  cursor: pointer;
}

.vehicle-thumb {
  width: 140px;
  height: 90px;
  object-fit: cover;
  border-radius: 6px;
}

.order-info {
  flex: 1;
}

.vehicle-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.rental-time, .rental-store {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 5px;
}

.order-price {
  text-align: right;
}

.order-price .amount {
  display: block;
  font-size: 22px;
  font-weight: bold;
  color: #F56C6C;
}

.order-price .days {
  font-size: 12px;
  color: #909399;
}

.order-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
