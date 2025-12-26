<template>
  <div class="order-confirm-page" v-loading="loading">
    <div class="page-header">
      <h1>确认订单</h1>
    </div>
    
    <div class="confirm-container" v-if="bookingInfo">
      <div class="main-content">
        <!-- Vehicle Info -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>车辆信息</span>
          </template>
          <div class="vehicle-info">
            <img :src="getImageUrl(bookingInfo.vehicle?.mainImage)" :alt="bookingInfo.vehicle?.model || 'vehicle'" class="vehicle-image">
            <div class="vehicle-details">
              <h3>{{ bookingInfo.vehicle?.brand }} {{ bookingInfo.vehicle?.model }}</h3>
              <p>{{ bookingInfo.vehicle?.series }}</p>
              <div class="specs">
                <span>{{ bookingInfo.vehicle?.seats }}座</span>
                <span>{{ getTransmission(bookingInfo.vehicle?.transmission) }}</span>
                <span>{{ getFuelType(bookingInfo.vehicle?.fuelType) }}</span>
              </div>
            </div>
          </div>
        </el-card>
        
        <!-- Rental Info -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>租车信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="取车门店">
              {{ getStoreName(bookingInfo.pickupStoreId) }}
            </el-descriptions-item>
            <el-descriptions-item label="还车门店">
              {{ getStoreName(bookingInfo.returnStoreId) }}
            </el-descriptions-item>
            <el-descriptions-item label="取车时间">
              {{ formatDate(bookingInfo.pickupTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="还车时间">
              {{ formatDate(bookingInfo.returnTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="租期">
              {{ priceInfo.rentalDays }} 天
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
        
        <!-- Insurance -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>保险服务</span>
          </template>
          <el-radio-group v-model="insuranceType" @change="calculatePrice">
            <div class="insurance-option">
              <el-radio value="none">
                <div class="option-content">
                  <span class="option-title">不购买保险</span>
                  <span class="option-price">¥0</span>
                </div>
                <p class="option-desc">不含任何保险保障</p>
              </el-radio>
            </div>
            <div class="insurance-option">
              <el-radio value="basic">
                <div class="option-content">
                  <span class="option-title">基础保障</span>
                  <span class="option-price">¥30/天</span>
                </div>
                <p class="option-desc">车辆损失险、第三者责任险</p>
              </el-radio>
            </div>
            <div class="insurance-option recommended">
              <el-radio value="premium">
                <div class="option-content">
                  <span class="option-title">尊享保障 <el-tag size="small" type="danger">推荐</el-tag></span>
                  <span class="option-price">¥60/天</span>
                </div>
                <p class="option-desc">全险保障，0免赔，道路救援</p>
              </el-radio>
            </div>
          </el-radio-group>
        </el-card>
        
        <!-- Coupon -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>优惠券</span>
          </template>
          <el-select v-model="selectedCouponId" placeholder="选择优惠券" style="width: 100%;" @change="calculatePrice">
            <el-option label="不使用优惠券" :value="null" />
            <el-option 
              v-for="coupon in coupons" 
              :key="coupon.couponId" 
              :label="`${coupon.name} (-¥${coupon.discountAmount || ''})`"
              :value="coupon.couponId"
            />
          </el-select>
        </el-card>
        
        <!-- Contact Info -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <span>联系人信息</span>
          </template>
          <el-form :model="contactForm" label-width="80px">
            <el-form-item label="姓名">
              <el-input v-model="contactForm.name" disabled />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="contactForm.phone" disabled />
            </el-form-item>
          </el-form>
          <el-alert 
            v-if="!userStore.isVerified" 
            type="warning" 
            :closable="false"
            show-icon
          >
            您尚未完成实名认证，请先
            <router-link to="/user/verification">完成认证</router-link>
            后再下单
          </el-alert>
        </el-card>
      </div>
      
      <!-- Price Summary -->
      <div class="price-summary">
        <el-card shadow="never">
          <h3>费用明细</h3>
          <div class="price-items">
            <div class="price-item">
              <span>租金 ({{ priceInfo.rentalDays }}天 × ¥{{ priceInfo.dailyPrice }})</span>
              <span>¥{{ priceInfo.rentalAmount }}</span>
            </div>
            <div class="price-item" v-if="priceInfo.deposit > 0">
              <span>押金</span>
              <span>¥{{ priceInfo.deposit }}</span>
            </div>
            <div class="price-item" v-if="priceInfo.insuranceAmount > 0">
              <span>保险费</span>
              <span>¥{{ priceInfo.insuranceAmount }}</span>
            </div>
            <div class="price-item" v-if="priceInfo.serviceAmount > 0">
              <span>服务费（异地还车）</span>
              <span>¥{{ priceInfo.serviceAmount }}</span>
            </div>
            <div class="price-item discount" v-if="priceInfo.discountAmount > 0">
              <span>优惠</span>
              <span>-¥{{ priceInfo.discountAmount }}</span>
            </div>
          </div>
          
          <el-divider />
          
          <div class="total-price">
            <span>应付总额</span>
            <span class="total">¥{{ priceInfo.totalAmount }}</span>
          </div>
          
          <el-button 
            type="primary" 
            size="large" 
            block 
            :disabled="!userStore.isVerified"
            @click="submitOrder"
          >
            提交订单
          </el-button>
          
          <p class="agreement">
            提交订单即表示同意
            <a href="#">《租车服务协议》</a>
          </p>
        </el-card>
      </div>
    </div>
    
    <el-empty v-else description="暂无订单信息，请重新选择车辆" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/api'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const bookingInfo = ref(null)
const stores = ref([])
const coupons = ref([])
const insuranceType = ref('basic')
const selectedCouponId = ref(null)

const contactForm = ref({
  name: '',
  phone: ''
})

const priceInfo = ref({
  rentalDays: 0,
  dailyPrice: 0,
  rentalAmount: 0,
  deposit: 0,
  insuranceAmount: 0,
  serviceAmount: 0,
  discountAmount: 0,
  totalAmount: 0
})

const getTransmission = (type) => type === 'auto' ? '自动挡' : '手动挡'
const getFuelType = (type) => {
  const map = { gasoline: '汽油', diesel: '柴油', electric: '纯电', hybrid: '混动' }
  return map[type] || type
}

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

// Resolve image input which can be: a vehicle object, an image path string, or an images array
const getImageUrl = (input) => {
  if (!input) return '/images/car-placeholder.jpg'

  // If passed a vehicle object
  if (typeof input === 'object' && !Array.isArray(input)) {
    const v = input
    if (v.mainImage) return normalizeImgUrl(v.mainImage)
    if (v.main_image) return normalizeImgUrl(v.main_image)
    if (Array.isArray(v.images) && v.images.length > 0) return normalizeImgUrl(v.images[0])
    if (v.images && typeof v.images === 'string') {
      try {
        const parsed = JSON.parse(v.images)
        if (Array.isArray(parsed) && parsed.length > 0) return normalizeImgUrl(parsed[0])
      } catch (e) {
        // fall through
      }
    }
    return '/images/car-placeholder.jpg'
  }

  // Otherwise treat input as a path or string
  return normalizeImgUrl(String(input))
}

const normalizeImgUrl = (url) => {
  if (!url) return '/images/car-placeholder.jpg'
  const s = url.trim()
  if (!s) return '/images/car-placeholder.jpg'

  // absolute URLs or already-rooted paths
  if (s.startsWith('http://') || s.startsWith('https://') || s.startsWith('/')) return s

  // JSON array or object string
  if ((s.startsWith('[') && s.endsWith(']')) || s.startsWith('{')) {
    try {
      const parsed = JSON.parse(s)
      if (Array.isArray(parsed) && parsed.length > 0) return normalizeImgUrl(parsed[0])
      if (parsed && parsed.url) return normalizeImgUrl(parsed.url)
    } catch (e) {}
  }

  // comma-separated
  if (s.includes(',')) {
    const first = s.split(',').map(x => x.trim()).find(x => x.length > 0)
    if (first) return normalizeImgUrl(first)
  }

  // common backend returns 'vehicles/...' or 'message/...' without leading /images/
  if (s.startsWith('vehicles/') || s.startsWith('message/') || s.startsWith('images/')) {
    return '/images/' + s.replace(/^images\//, '')
  }

  if (s.startsWith('uploads/')) return '/' + s

  return '/images/' + s
}

const getStoreName = (storeId) => {
  const store = stores.value.find(s => s.id === storeId)
  return store?.name || '未知门店'
}

const loadStores = async () => {
  try {
    const res = await api.stores.list()
    if (res.code === 200) {
      stores.value = res.data
    }
  } catch (error) {
    console.error('Failed to load stores:', error)
  }
}

const loadCoupons = async () => {
  try {
    const res = await api.marketing.myCoupons({ status: 0 })
    if (res.code === 200) {
      coupons.value = res.data
    }
  } catch (error) {
    console.error('Failed to load coupons:', error)
  }
}

const calculatePrice = async () => {
  if (!bookingInfo.value) return
  
  try {
    const res = await api.orders.calculate({
      vehicleId: bookingInfo.value.vehicleId,
      pickupStoreId: bookingInfo.value.pickupStoreId,
      returnStoreId: bookingInfo.value.returnStoreId,
      pickupTime: formatDate(bookingInfo.value.pickupTime),
      returnTime: formatDate(bookingInfo.value.returnTime),
      insuranceType: insuranceType.value,
      couponId: selectedCouponId.value
    })
    
    if (res.code === 200) {
      priceInfo.value = res.data
    }
  } catch (error) {
    console.error('Failed to calculate price:', error)
  }
}

const submitOrder = async () => {
  if (!userStore.isVerified) {
    ElMessage.warning('请先完成实名认证')
    return
  }
  
  loading.value = true
  try {
    const res = await api.orders.create({
      vehicleId: bookingInfo.value.vehicleId,
      pickupStoreId: bookingInfo.value.pickupStoreId,
      returnStoreId: bookingInfo.value.returnStoreId,
      pickupTime: formatDate(bookingInfo.value.pickupTime),
      returnTime: formatDate(bookingInfo.value.returnTime),
      insuranceType: insuranceType.value,
      couponId: selectedCouponId.value
    })
    
    if (res.code === 200) {
      ElMessage.success('订单创建成功')
      sessionStorage.removeItem('bookingInfo')
      router.push(`/orders/${res.data.id}`)
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('创建订单失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // Load booking info from session storage
  const stored = sessionStorage.getItem('bookingInfo')
  if (stored) {
    bookingInfo.value = JSON.parse(stored)
    calculatePrice()
  }
  
  // Load user info
  if (userStore.user) {
    contactForm.value.name = userStore.user.realName || userStore.user.username
    contactForm.value.phone = userStore.user.phone
  }
  
  loadStores()
  loadCoupons()
})
</script>

<style scoped>
.order-confirm-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #303133;
}

.confirm-container {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 20px;
}

.section-card {
  margin-bottom: 20px;
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
  margin-bottom: 5px;
}

.vehicle-details p {
  color: #909399;
  font-size: 14px;
  margin-bottom: 10px;
}

.specs {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #606266;
}

.insurance-option {
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 10px;
  transition: all 0.3s;
}

.insurance-option:hover, .insurance-option.recommended {
  border-color: #409EFF;
}

.option-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.option-title {
  font-weight: 600;
}

.option-price {
  color: #F56C6C;
  font-weight: 600;
}

.option-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  margin-left: 24px;
}

.price-summary {
  position: sticky;
  top: 80px;
}

.price-summary h3 {
  font-size: 16px;
  margin-bottom: 20px;
  color: #303133;
}

.price-items {
  font-size: 14px;
}

.price-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  color: #606266;
}

.price-item.discount {
  color: #67C23A;
}

.total-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.total-price .total {
  font-size: 28px;
  font-weight: bold;
  color: #F56C6C;
}

.agreement {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin-top: 15px;
}

.agreement a {
  color: #409EFF;
}

@media screen and (max-width: 992px) {
  .confirm-container {
    grid-template-columns: 1fr;
  }
  
  .price-summary {
    position: static;
  }
}
</style>
