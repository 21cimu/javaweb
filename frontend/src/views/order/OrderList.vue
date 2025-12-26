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
          <img
            :src="getOrderImage(order)"
            :alt="order.vehicleName"
            class="vehicle-thumb"
            @error="(e) => { e.target.src = '/images/car-placeholder.jpg' }"
          >
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
            <span class="amount">¥{{ formatAmount(getOrderRentalAmount(order)) }}</span>
            <span class="days">{{ order.rentalDays }}天</span>
            <!-- 如果有售后申请退款金额，则额外展示一行提示 -->
            <div
              v-if="order.afterSalesRefundAmount && Number(order.afterSalesRefundAmount) > 0"
              class="after-sales-refund"
            >
              申请退款 ¥{{ formatAmount(order.afterSalesRefundAmount) }}
            </div>
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
            <el-button size="small" @click.stop="showPickupCode(order)">查看取车码</el-button>
            <el-button size="small" @click="handleCancel(order)">取消订单</el-button>
          </template>
          <template v-else-if="order.status === 8">
            <el-button size="small" @click="handleReorder(order)">再次预订</el-button>
            <el-button v-if="!order.rating" type="primary" size="small" @click="handleReview(order)">
              评价订单
            </el-button>
          </template>
          <el-button
            v-if="canApplyAfterSales(order)"
            size="small"
            type="danger"
            @click.stop="goToAfterSales(order)"
          >
            售后申请
          </el-button>
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

    <!-- Pickup Code Dialog -->
    <el-dialog v-model="pickupDialogVisible" title="取车码" width="300px" center>
      <div class="pickup-code">
        <span class="code">{{ pickupCodeValue }}</span>
        <p>请在取车时向工作人员出示此取车码</p>
        <div style="margin-top:12px;text-align:center">
          <el-button type="primary" @click="copyPickupCode">复制取车码</el-button>
          <el-button @click="pickupDialogVisible = false" style="margin-left:10px">关闭</el-button>
        </div>
      </div>
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

const pickupDialogVisible = ref(false)
const pickupCodeValue = ref('')

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

const getOrderRentalAmount = (order) => {
  const total = Number(order?.totalAmount ?? 0)
  const deposit = Number(order?.deposit ?? 0)
  const rent = total - deposit
  if (Number.isNaN(rent)) {
    const fallback = Number(order?.rentalAmount ?? 0)
    return Number.isNaN(fallback) ? 0 : fallback
  }
  return rent < 0 ? 0 : rent
}

const formatAmount = (value) => {
  const num = Number(value ?? 0)
  if (Number.isNaN(num)) return '0'
  const rounded = Math.round(num * 10) / 10
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1)
}

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
      try { console.log('orders.list sample:', res.data.list && res.data.list[0]) } catch (e) {}
      orders.value = res.data.list
      total.value = res.data.total

      // For orders that don't have an image, fetch vehicle detail to get mainImage
      const needsFetch = orders.value.filter(o => {
        const hasImage = o.vehicleMainImage || o.vehicleImage || o.mainImage || (o.vehicle && (o.vehicle.mainImage || o.vehicle.images))
        return !hasImage && o.vehicleId
      })

      if (needsFetch.length > 0) {
        try {
          await Promise.all(needsFetch.map(async (o) => {
            try {
              const vr = await api.vehicles.detail(o.vehicleId)
              if (vr && vr.code === 200) {
                // vehicle might be in vr.data or vr.data.vehicle
                const v = vr.data && (vr.data.vehicle || vr.data)
                if (v) {
                  // prefer mainImage or images
                  o.vehicleMainImage = v.mainImage || v.main_image || (Array.isArray(v.images) ? v.images[0] : v.images)
                }
              }
            } catch (err) {
              console.warn('Failed fetch vehicle detail for', o.vehicleId, err)
            }
          }))
          // force UI update
          orders.value = [...orders.value]
        } catch (err) {
          console.warn('vehicle detail batch fetch failed', err)
        }
      }
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
  try {
    await ElMessageBox.confirm(`确定支付 ¥${order.totalAmount} 吗？`, '确认支付', {
      type: 'info'
    })
    
    const res = await api.pay.alipayCreate({ orderId: order.id })
    console.log('[alipay.create] response:', res)

    if (res.code !== 200) {
      ElMessage.error(res.message)
      return
    }

    const { htmlForm } = res.data || {}
    if (!htmlForm) {
      ElMessage.error('未获取到支付宝支付表单')
      return
    }

    // Render and submit in current page (most reliable; avoids popup/CSP issues).
    const wrap = document.createElement('div')
    wrap.style.display = 'none'
    wrap.innerHTML = htmlForm
    document.body.appendChild(wrap)
    const form = wrap.querySelector('form')
    if (form) {
      form.setAttribute('target', '_self')
      form.submit()
    } else {
      ElMessage.error('支付表单解析失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('支付发起失败')
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

const showPickupCode = async (order) => {
  // If pickupCode already present on order object, use it; otherwise fetch detail
  if (order.pickupCode) {
    pickupCodeValue.value = order.pickupCode
    pickupDialogVisible.value = true
    return
  }
  try {
    const res = await api.orders.detail(order.id)
    if (res.code === 200) {
      const o = res.data.order || res.data
      pickupCodeValue.value = o.pickupCode || ''
      pickupDialogVisible.value = true
    } else {
      ElMessage.error(res.message || '无法获取取车码')
    }
  } catch (err) {
    ElMessage.error('获取取车码失败')
  }
}

const copyPickupCode = async () => {
  try {
    await navigator.clipboard.writeText(pickupCodeValue.value)
    ElMessage.success('已复制取车码')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const getOrderImage = (order) => {
  // potential fields that may contain image paths
  const candidates = [
    order.vehicleMainImage,
    order.vehicleImage,
    order.mainImage,
    order.vehicle?.mainImage,
    order.vehicle?.main_image,
    order.vehicle?.images,
    order.vehicleImages,
    order.reviewImages,
    order.pickupImages
  ]

  for (let v of candidates) {
    if (!v) continue
    // if v is JSON array string, try parse
    if (typeof v === 'string') {
      const s = v.trim()
      // JSON array
      if ((s.startsWith('[') && s.endsWith(']')) || s.startsWith('{')) {
        try {
          const parsed = JSON.parse(s)
          if (Array.isArray(parsed) && parsed.length > 0) return normalizeImgUrl(parsed[0])
          // if object has url field
          if (parsed && parsed.url) return normalizeImgUrl(parsed.url)
        } catch (_) {
          // not json
        }
      }
      // comma-separated list
      if (s.includes(',')) {
        const first = s.split(',').map(x => x.trim()).find(x => x.length > 0)
        if (first) return normalizeImgUrl(first)
      }
      // plain string
      return normalizeImgUrl(s)
    }
    // if it's an array
    if (Array.isArray(v) && v.length > 0) return normalizeImgUrl(v[0])
  }
  return '/images/car-placeholder.jpg'
}

const normalizeImgUrl = (url) => {
  if (!url) return '/images/car-placeholder.jpg'
  // absolute URLs or already-rooted paths
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/')) return url

  // common backend might return 'vehicles/xxx.jpg' or 'message/xxx.jpg' without /images prefix
  if (url.startsWith('vehicles/') || url.startsWith('message/') || url.startsWith('images/')) {
    return '/images/' + url.replace(/^images\//, '')
  }

  if (url.startsWith('uploads/')) return '/' + url // if uploads mapped at root

  // default: prefix with /images/ to match frontend public folder
  return '/images/' + url
}

const canApplyAfterSales = (order) => {
  // 已完成、用车中、待还车、待结算均允许发起售后
  const allowedStatus = [5, 6, 7, 8, 9, 10]
  // 需有支付金额（防止未支付/零元单）
  const paid = Number(order.paidAmount || order.totalAmount || 0)
  return allowedStatus.includes(order.status) && paid > 0
}

const goToAfterSales = (order) => {
  router.push({ name: 'OrderAfterSalesApply', params: { id: order.id } })
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

.after-sales-refund {
  margin-top: 4px;
  font-size: 12px;
  color: #E6A23C;
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

/* Pickup code styles to match OrderDetail.vue */
.pickup-code { text-align: center; padding: 8px 0; }
.pickup-code .code {
  display: block;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 6px;
  margin-bottom: 8px;
}
</style>
