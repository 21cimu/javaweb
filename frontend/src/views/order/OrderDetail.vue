<template>
  <div class="order-detail-page" v-loading="loading">
    <div class="page-header">
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h1>订单详情</h1>
    </div>
    
    <div class="detail-container" v-if="order">
      <!-- Order Status -->
      <el-card shadow="never" class="status-card">
        <div class="status-content">
          <el-tag :type="getStatusType(order.status)" size="large">
            {{ getStatusText(order.status) }}
          </el-tag>
          <p class="status-desc">{{ getStatusDesc(order.status) }}</p>
        </div>
        <div class="status-actions">
          <template v-if="order.status === 3">
            <el-button type="primary" @click="handlePay">立即支付</el-button>
          </template>
          <template v-if="order.status === 4">
            <el-button type="primary" @click="showPickupCode">查看取车码</el-button>
          </template>
          <template v-if="order.status < 5">
            <el-button @click="handleCancel">取消订单</el-button>
          </template>
        </div>
      </el-card>
      
      <!-- Pickup Code Dialog -->
      <el-dialog v-model="pickupCodeVisible" title="取车码" width="300px" center>
        <div class="pickup-code">
          <span class="code">{{ order.pickupCode }}</span>
          <p>请在取车时向工作人员出示此取车码</p>
        </div>
      </el-dialog>
      
      <!-- Vehicle Info -->
      <el-card shadow="never">
        <template #header>车辆信息</template>
        <div class="vehicle-info">
          <img :src="vehicle?.mainImage || '/images/car-placeholder.jpg'" class="vehicle-image">
          <div class="vehicle-details">
            <h3>{{ order.vehicleName }}</h3>
            <p>车牌号：{{ order.vehiclePlate }}</p>
          </div>
        </div>
      </el-card>
      
      <!-- Rental Info -->
      <el-card shadow="never">
        <template #header>租车信息</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="租期">{{ order.rentalDays }} 天</el-descriptions-item>
          <el-descriptions-item label="取车门店">{{ order.pickupStoreName }}</el-descriptions-item>
          <el-descriptions-item label="还车门店">{{ order.returnStoreName }}</el-descriptions-item>
          <el-descriptions-item label="预计取车时间">{{ formatDate(order.pickupTime) }}</el-descriptions-item>
          <el-descriptions-item label="预计还车时间">{{ formatDate(order.returnTime) }}</el-descriptions-item>
          <el-descriptions-item label="实际取车时间" v-if="order.actualPickupTime">
            {{ formatDate(order.actualPickupTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="实际还车时间" v-if="order.actualReturnTime">
            {{ formatDate(order.actualReturnTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
      
      <!-- Price Info -->
      <el-card shadow="never">
        <template #header>费用明细</template>
        <div class="price-details">
          <div class="price-item">
            <span>租金 ({{ order.rentalDays }}天 × ¥{{ order.dailyPrice }})</span>
            <span>¥{{ order.rentalAmount }}</span>
          </div>
          <div class="price-item" v-if="order.deposit > 0">
            <span>押金</span>
            <span>¥{{ order.deposit }}</span>
          </div>
          <div class="price-item" v-if="order.insuranceAmount > 0">
            <span>保险费</span>
            <span>¥{{ order.insuranceAmount }}</span>
          </div>
          <div class="price-item" v-if="order.serviceAmount > 0">
            <span>服务费</span>
            <span>¥{{ order.serviceAmount }}</span>
          </div>
          <div class="price-item" v-if="order.extraAmount > 0">
            <span>额外费用</span>
            <span>¥{{ order.extraAmount }}</span>
          </div>
          <div class="price-item discount" v-if="order.discountAmount > 0">
            <span>优惠</span>
            <span>-¥{{ order.discountAmount }}</span>
          </div>
          <el-divider />
          <div class="price-item total">
            <span>应付总额</span>
            <span class="total-amount">¥{{ order.totalAmount }}</span>
          </div>
          <div class="price-item paid" v-if="order.paidAmount > 0">
            <span>已支付</span>
            <span>¥{{ order.paidAmount }}</span>
          </div>
        </div>
      </el-card>
      
      <!-- Contact Info -->
      <el-card shadow="never">
        <template #header>联系人信息</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ order.userName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ order.userPhone }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      
      <!-- Review (if completed) -->
      <el-card shadow="never" v-if="order.status === 8">
        <template #header>评价</template>
        <div v-if="order.rating">
          <el-rate v-model="order.rating" disabled show-score />
          <p class="review-content">{{ order.review }}</p>
          <p class="review-time">{{ formatDate(order.reviewTime) }}</p>
        </div>
        <div v-else>
          <p>您还没有评价此订单</p>
          <el-button type="primary" @click="reviewDialogVisible = true">立即评价</el-button>
        </div>
      </el-card>
    </div>
    
    <!-- Review Dialog -->
    <el-dialog v-model="reviewDialogVisible" title="评价订单" width="500px">
      <el-form :model="reviewForm" label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" show-text />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="reviewForm.review" type="textarea" :rows="4" placeholder="请分享您的用车体验..." />
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import dayjs from 'dayjs'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref(null)
const vehicle = ref(null)
const pickupCodeVisible = ref(false)
const reviewDialogVisible = ref(false)
const reviewForm = ref({ rating: 5, review: '' })

const statusMap = {
  0: '已取消', 1: '待审核', 2: '审核失败', 3: '待支付',
  4: '待取车', 5: '用车中', 6: '待还车', 7: '待结算',
  8: '已完成', 9: '退款中', 10: '已退款'
}

const statusTypeMap = {
  0: 'info', 1: 'warning', 2: 'danger', 3: 'warning',
  4: '', 5: 'success', 6: 'warning', 7: 'warning',
  8: 'success', 9: 'info', 10: 'info'
}

const statusDescMap = {
  1: '您的订单正在审核中，请耐心等待',
  3: '订单已审核通过，请尽快完成支付',
  4: '支付成功，请在预约时间前往门店取车',
  5: '祝您用车愉快，注意安全驾驶',
  8: '感谢您的使用，期待下次光临'
}

const getStatusText = (status) => statusMap[status] || '未知'
const getStatusType = (status) => statusTypeMap[status] || 'info'
const getStatusDesc = (status) => statusDescMap[status] || ''
const formatDate = (date) => dayjs(date).format('YYYY-MM-DD HH:mm')

const loadOrder = async () => {
  loading.value = true
  try {
    const res = await api.orders.detail(route.params.id)
    if (res.code === 200) {
      order.value = res.data.order
      vehicle.value = res.data.vehicle
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

const showPickupCode = () => {
  pickupCodeVisible.value = true
}

const handlePay = async () => {
  try {
    await ElMessageBox.confirm(`确定支付 ¥${order.value.totalAmount} 吗？`, '确认支付')
    const res = await api.orders.pay({ orderId: order.value.id, paymentMethod: 1 })
    if (res.code === 200) {
      ElMessage.success('支付成功')
      loadOrder()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('支付失败')
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消此订单吗？', '提示', { type: 'warning' })
    const res = await api.orders.cancel({ orderId: order.value.id, reason: '用户主动取消' })
    if (res.code === 200) {
      ElMessage.success('订单已取消')
      loadOrder()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('取消失败')
  }
}

const submitReview = async () => {
  try {
    const res = await api.orders.review({
      orderId: order.value.id,
      rating: reviewForm.value.rating,
      review: reviewForm.value.review
    })
    if (res.code === 200) {
      ElMessage.success('评价成功')
      reviewDialogVisible.value = false
      loadOrder()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('评价失败')
  }
}

onMounted(() => {
  loadOrder()
})
</script>

<style scoped>
.order-detail-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #303133;
}

.el-card {
  margin-bottom: 20px;
}

.status-card :deep(.el-card__body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.status-desc {
  color: #606266;
}

.pickup-code {
  text-align: center;
  padding: 20px;
}

.pickup-code .code {
  font-size: 36px;
  font-weight: bold;
  color: #409EFF;
  letter-spacing: 5px;
}

.pickup-code p {
  margin-top: 15px;
  color: #909399;
  font-size: 13px;
}

.vehicle-info {
  display: flex;
  gap: 20px;
}

.vehicle-image {
  width: 200px;
  height: 130px;
  object-fit: cover;
  border-radius: 8px;
}

.vehicle-details h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 10px;
}

.vehicle-details p {
  color: #606266;
}

.price-details {
  max-width: 400px;
}

.price-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
  color: #606266;
}

.price-item.discount {
  color: #67C23A;
}

.price-item.total {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.total-amount {
  font-size: 24px;
  color: #F56C6C;
}

.price-item.paid {
  color: #67C23A;
}

.review-content {
  margin: 15px 0;
  color: #606266;
  line-height: 1.6;
}

.review-time {
  color: #909399;
  font-size: 12px;
}
</style>
