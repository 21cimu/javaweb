<template>
  <div class="vehicle-detail-page" v-loading="loading">
    <div class="detail-container" v-if="vehicle">
      <!-- Vehicle Info -->
      <div class="vehicle-main">
        <div class="vehicle-gallery">
          <el-carousel height="400px" indicator-position="outside">
            <el-carousel-item v-for="(img, index) in vehicleImages" :key="index">
              <img :src="img" :alt="vehicle.model" class="gallery-image">
            </el-carousel-item>
          </el-carousel>
        </div>
        
        <div class="vehicle-content">
          <div class="vehicle-header">
            <div class="title-area">
              <h1>{{ vehicle.brand }} {{ vehicle.model }}</h1>
              <p class="series">{{ vehicle.series }}</p>
            </div>
            <div class="tags">
              <el-tag v-if="vehicle.isHot" type="danger">热门</el-tag>
              <el-tag v-if="vehicle.isNew" type="success">新车</el-tag>
              <el-tag v-if="vehicle.noDeposit" type="warning">免押金</el-tag>
            </div>
          </div>
          
          <div class="vehicle-rating">
            <el-rate v-model="vehicle.rating" disabled show-score score-template="{value}分" />
            <span class="order-count">已租 {{ vehicle.orderCount }} 次</span>
          </div>
          
          <div class="vehicle-specs">
            <div class="spec-item">
              <span class="label">座位数</span>
              <span class="value">{{ vehicle.seats }}座</span>
            </div>
            <div class="spec-item">
              <span class="label">变速箱</span>
              <span class="value">{{ getTransmission(vehicle.transmission) }}</span>
            </div>
            <div class="spec-item">
              <span class="label">能源类型</span>
              <span class="value">{{ getFuelType(vehicle.fuelType) }}</span>
            </div>
            <div class="spec-item">
              <span class="label">车型</span>
              <span class="value">{{ getCategoryName(vehicle.category) }}</span>
            </div>
            <div class="spec-item">
              <span class="label">颜色</span>
              <span class="value">{{ vehicle.color }}</span>
            </div>
            <div class="spec-item">
              <span class="label">年份</span>
              <span class="value">{{ vehicle.year }}款</span>
            </div>
          </div>
          
          <div class="vehicle-features" v-if="features.length">
            <h3>车辆配置</h3>
            <div class="feature-tags">
              <el-tag v-for="feature in features" :key="feature" size="small" type="info">
                {{ feature }}
              </el-tag>
            </div>
          </div>
          
          <div class="vehicle-description" v-if="vehicle.description">
            <h3>车辆描述</h3>
            <p>{{ vehicle.description }}</p>
          </div>
        </div>

        <div class="vehicle-reviews">
          <div class="reviews-header">
            <h3>用户评论</h3>
            <span class="reviews-count" v-if="reviewTotal">共 {{ reviewTotal }} 条</span>
          </div>
          <div class="reviews-body" v-loading="reviewLoading">
            <div v-if="reviews.length">
              <div v-for="review in reviews" :key="review.id" class="review-item">
                <div class="review-meta">
                  <span class="review-user">{{ review.displayName }}</span>
                  <el-rate
                    :model-value="review.rating || 0"
                    disabled
                    show-score
                    score-template="{value}分"
                  />
                  <span class="review-time">{{ formatReviewTime(review.reviewTime) }}</span>
                </div>
                <p class="review-content">{{ review.review }}</p>
                <div v-if="review.imageList && review.imageList.length" class="review-images">
                  <img v-for="(img, idx) in review.imageList" :key="idx" :src="img" alt="review">
                </div>
              </div>
            </div>
            <el-empty v-else-if="!reviewLoading" description="暂无评论" />
            <div class="review-actions" v-if="hasMoreReviews">
              <el-button size="small" @click="loadMoreReviews" :loading="reviewLoading">加载更多</el-button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Booking Panel -->
      <div class="booking-panel">
        <el-card shadow="never">
          <div class="price-display">
            <span class="price">¥{{ vehicle.dailyPrice }}</span>
            <span class="unit">/天</span>
          </div>
          
          <div class="price-options">
            <div class="option">
              <span>周租价</span>
              <span class="option-price">¥{{ vehicle.weeklyPrice || (vehicle.dailyPrice * 6).toFixed(0) }}/周</span>
            </div>
            <div class="option">
              <span>月租价</span>
              <span class="option-price">¥{{ vehicle.monthlyPrice || (vehicle.dailyPrice * 25).toFixed(0) }}/月</span>
            </div>
          </div>
          
          <el-divider />
          
          <el-form :model="bookingForm" label-position="top">
            <el-form-item label="取车门店">
              <el-select v-model="bookingForm.pickupStoreId" placeholder="选择取车门店" style="width: 100%;">
                <el-option 
                  v-for="s in stores" 
                  :key="s.id" 
                  :label="s.name" 
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="取车时间">
              <el-date-picker
                v-model="bookingForm.pickupTime"
                type="datetime"
                placeholder="选择取车时间"
                style="width: 100%;"
                :disabled-date="disabledDate"
              />
            </el-form-item>
            
            <el-form-item label="还车时间">
              <el-date-picker
                v-model="bookingForm.returnTime"
                type="datetime"
                placeholder="选择还车时间"
                style="width: 100%;"
                :disabled-date="disabledDate"
              />
            </el-form-item>
            
            <el-form-item>
              <el-checkbox v-model="bookingForm.differentReturn">异地还车</el-checkbox>
            </el-form-item>
            
            <el-form-item label="还车门店" v-if="bookingForm.differentReturn">
              <el-select v-model="bookingForm.returnStoreId" placeholder="选择还车门店" style="width: 100%;">
                <el-option 
                  v-for="s in stores" 
                  :key="s.id" 
                  :label="s.name" 
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
          
          <div class="deposit-info" v-if="vehicle.deposit > 0 && !vehicle.noDeposit">
            <el-icon><Warning /></el-icon>
            押金：¥{{ vehicle.deposit }}
          </div>
          
          <el-button type="primary" size="large" block @click="handleBook">
            立即预订
          </el-button>
          
          <div class="booking-tips">
            <p><el-icon><Check /></el-icon> 免费取消（取车前24小时）</p>
            <p><el-icon><Check /></el-icon> 全险保障</p>
            <p><el-icon><Check /></el-icon> 24小时客服</p>
          </div>
        </el-card>
        
        <!-- Store Info -->
        <el-card shadow="never" class="store-card" v-if="store">
          <h3>取车门店</h3>
          <div class="store-info">
            <p class="store-name">{{ store.name }}</p>
            <p class="store-address">
              <el-icon><Location /></el-icon>
              {{ store.address }}
            </p>
            <p class="store-hours">
              <el-icon><Clock /></el-icon>
              营业时间：{{ store.businessHours }}
            </p>
            <p class="store-phone">
              <el-icon><Phone /></el-icon>
              {{ store.phone }}
            </p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import api from '@/api'
import { Warning, Check, Location, Clock, Phone } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const vehicle = ref(null)
const store = ref(null)
const stores = ref([])
const reviews = ref([])
const reviewLoading = ref(false)
const reviewPage = ref(1)
const reviewPageSize = ref(5)
const reviewTotal = ref(0)

const bookingForm = ref({
  pickupStoreId: null,
  returnStoreId: null,
  pickupTime: null,
  returnTime: null,
  differentReturn: false
})

const vehicleImages = computed(() => {
  if (!vehicle.value) return []
  let images = []
  if (vehicle.value.mainImage) images.push(vehicle.value.mainImage)
  if (vehicle.value.images) {
    try {
      const parsed = JSON.parse(vehicle.value.images)
      images = images.concat(parsed)
    } catch {
      // ignore
    }
  }
  return images.length ? images : ['/images/car-placeholder.jpg']
})

const features = computed(() => {
  if (!vehicle.value?.features) return []
  try {
    return JSON.parse(vehicle.value.features)
  } catch {
    return []
  }
})

const hasMoreReviews = computed(() => reviews.value.length < reviewTotal.value)

const getTransmission = (type) => type === 'auto' ? '自动挡' : '手动挡'
const getFuelType = (type) => {
  const map = { gasoline: '汽油', diesel: '柴油', electric: '纯电', hybrid: '混动' }
  return map[type] || type
}
const getCategoryName = (cat) => {
  const map = { economy: '经济型', compact: '紧凑型', midsize: '中型', suv: 'SUV', luxury: '豪华型', minivan: 'MPV' }
  return map[cat] || cat
}

const parseReviewImages = (images) => {
  if (!images) return []
  if (Array.isArray(images)) return images.filter(Boolean)
  try {
    const parsed = JSON.parse(images)
    if (Array.isArray(parsed)) return parsed.filter(Boolean)
    return parsed ? [parsed] : []
  } catch {
    return [images]
  }
}

const normalizeReview = (review) => ({
  ...review,
  displayName: review.userName || '匿名用户',
  imageList: parseReviewImages(review.reviewImages)
})

const formatReviewTime = (value) => value ? dayjs(value).format('YYYY-MM-DD HH:mm') : ''

const disabledDate = (date) => {
  return date.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

const loadVehicle = async () => {
  loading.value = true
  try {
    const res = await api.vehicles.detail(route.params.id)
    if (res.code === 200) {
      vehicle.value = res.data.vehicle
      store.value = res.data.store
      if (store.value) {
        bookingForm.value.pickupStoreId = store.value.id
      }
    } else {
      ElMessage.error(res.message || '获取车辆信息失败')
    }
  } catch (error) {
    ElMessage.error('获取车辆信息失败')
  } finally {
    loading.value = false
  }
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

const loadReviews = async ({ reset = false, page } = {}) => {
  if (!route.params.id || reviewLoading.value) return
  reviewLoading.value = true
  const targetPage = page || (reset ? 1 : reviewPage.value)
  try {
    const res = await api.reviews.list({
      vehicleId: route.params.id,
      page: targetPage,
      pageSize: reviewPageSize.value
    })
    if (res.code === 200) {
      const payload = res.data || {}
      const list = Array.isArray(payload.list) ? payload.list : []
      const normalized = list.map(normalizeReview)
      if (reset) {
        reviews.value = normalized
      } else {
        reviews.value = reviews.value.concat(normalized)
      }
      reviewTotal.value = payload.total || 0
      reviewPage.value = payload.page || targetPage
    } else {
      ElMessage.error(res.message || '获取评论失败')
    }
  } catch (error) {
    console.error('Failed to load reviews:', error)
    ElMessage.error('获取评论失败')
  } finally {
    reviewLoading.value = false
  }
}

const loadMoreReviews = () => {
  if (reviewLoading.value || !hasMoreReviews.value) return
  loadReviews({ page: reviewPage.value + 1 })
}

const resetReviews = () => {
  reviews.value = []
  reviewTotal.value = 0
  reviewPage.value = 1
  loadReviews({ reset: true })
}

const handleBook = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  
  if (!bookingForm.value.pickupStoreId) {
    ElMessage.warning('请选择取车门店')
    return
  }
  if (!bookingForm.value.pickupTime) {
    ElMessage.warning('请选择取车时间')
    return
  }
  if (!bookingForm.value.returnTime) {
    ElMessage.warning('请选择还车时间')
    return
  }
  if (bookingForm.value.returnTime <= bookingForm.value.pickupTime) {
    ElMessage.warning('还车时间必须晚于取车时间')
    return
  }
  
  // Store booking info and navigate to confirm page
  sessionStorage.setItem('bookingInfo', JSON.stringify({
    vehicleId: vehicle.value.id,
    vehicle: vehicle.value,
    pickupStoreId: bookingForm.value.pickupStoreId,
    returnStoreId: bookingForm.value.differentReturn ? bookingForm.value.returnStoreId : bookingForm.value.pickupStoreId,
    pickupTime: bookingForm.value.pickupTime,
    returnTime: bookingForm.value.returnTime
  }))
  
  router.push('/order/confirm')
}

onMounted(() => {
  loadVehicle()
  loadStores()
  resetReviews()
})

watch(() => route.params.id, () => {
  loadVehicle()
  resetReviews()
})
</script>

<style scoped>
.vehicle-detail-page {
  max-width: 1400px;
  margin: 0 auto;
}

.detail-container {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 30px;
}

.vehicle-gallery {
  margin-bottom: 30px;
}

.gallery-image {
  width: 100%;
  height: 400px;
  object-fit: cover;
  border-radius: 8px;
}

.vehicle-content {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
}

.vehicle-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.vehicle-header h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 5px;
}

.series {
  color: #909399;
  font-size: 14px;
}

.tags {
  display: flex;
  gap: 8px;
}

.vehicle-rating {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 25px;
}

.order-count {
  font-size: 13px;
  color: #909399;
}

.vehicle-specs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 25px;
}

.spec-item {
  text-align: center;
}

.spec-item .label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.spec-item .value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.vehicle-features h3,
.vehicle-description h3 {
  font-size: 16px;
  color: #303133;
  margin-bottom: 15px;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.vehicle-description {
  margin-top: 25px;
}

.vehicle-description p {
  color: #606266;
  line-height: 1.8;
}

.vehicle-reviews {
  background: #fff;
  border-radius: 8px;
  padding: 25px 30px;
  margin-top: 20px;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.reviews-header h3 {
  font-size: 16px;
  color: #303133;
  margin: 0;
}

.reviews-count {
  font-size: 12px;
  color: #909399;
}

.review-item {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.review-item:last-child {
  border-bottom: none;
}

.review-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

.review-user {
  font-weight: 600;
  color: #303133;
}

.review-time {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.review-content {
  color: #606266;
  line-height: 1.7;
  margin: 0;
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.review-images img {
  width: 90px;
  height: 70px;
  object-fit: cover;
  border-radius: 6px;
}

.review-actions {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

/* Booking Panel */
.booking-panel {
  position: sticky;
  top: 80px;
}

.booking-panel .el-card {
  margin-bottom: 20px;
}

.price-display {
  text-align: center;
  margin-bottom: 15px;
}

.price-display .price {
  font-size: 36px;
  font-weight: bold;
  color: #F56C6C;
}

.price-display .unit {
  font-size: 14px;
  color: #909399;
}

.price-options {
  display: flex;
  justify-content: space-around;
  padding: 15px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.option {
  text-align: center;
}

.option span:first-child {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.option-price {
  font-size: 14px;
  font-weight: 600;
  color: #E6A23C;
}

.deposit-info {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px;
  background: #FEF0E6;
  color: #E6A23C;
  border-radius: 4px;
  font-size: 13px;
  margin-bottom: 15px;
}

.booking-tips {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px dashed #ebeef5;
}

.booking-tips p {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #67C23A;
  margin-bottom: 8px;
}

.store-card h3 {
  font-size: 16px;
  margin-bottom: 15px;
  color: #303133;
}

.store-info p {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
}

.store-name {
  font-weight: 600;
  font-size: 15px !important;
}

@media screen and (max-width: 992px) {
  .detail-container {
    grid-template-columns: 1fr;
  }
  
  .booking-panel {
    position: static;
  }
}
</style>
