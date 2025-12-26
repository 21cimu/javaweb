<template>
  <div class="promotions-page">
    <div class="page-header">
      <h1>优惠活动</h1>
      <p>精彩活动不容错过</p>
    </div>
    
    <div class="promo-list">
      <div v-for="promo in promotions" :key="promo.id" class="promo-card">
        <div class="promo-image">
          <img :src="getPromoImage(promo)" :alt="promo.title">
        </div>
        <div class="promo-content">
          <h2>{{ promo.title }}</h2>
          <p>{{ promo.description }}</p>
          <el-button type="primary" @click="handleJoin(promo)">立即参与</el-button>
        </div>
      </div>
    </div>
    
    <el-divider />
    
    <div class="coupons-section">
      <h2>可领取的优惠券</h2>

      <div v-if="coupons.length" class="coupon-slider">
        <button
          class="slider-nav prev"
          :disabled="currentIndex === 0"
          @click="slide(-1)"
        >
          ‹
        </button>
        <div class="slider-window" ref="sliderWindowRef">
          <div
            class="slider-track"
            :style="trackVars"
          >
            <div
              v-for="coupon in coupons"
              :key="coupon.id"
              class="coupon-item"
            >
              <div class="coupon-value">
                <span class="amount" v-if="coupon.type === 1 || coupon.type === 3">¥{{ coupon.discountAmount }}</span>
                <span class="amount" v-else>{{ (coupon.discountRate * 10).toFixed(1) }}折</span>
                <span class="condition" v-if="coupon.minAmount > 0">满{{ coupon.minAmount }}可用</span>
                <span class="condition" v-else>无门槛</span>
              </div>
              <div class="coupon-info">
                <h3>{{ coupon.name }}</h3>
                <p class="desc">{{ coupon.description }}</p>
                <p class="time" v-if="coupon.endTime">
                  有效期至：{{ String(coupon.endTime).substring(0, 10) }}
                </p>
              </div>
              <div class="coupon-action">
                <el-button
                  :type="isClaimed(coupon) ? 'default' : 'danger'"
                  size="small"
                  :disabled="isClaimed(coupon) || claimingMap[coupon.id]"
                  @click="claimCoupon(coupon)"
                >
                  {{ isClaimed(coupon) ? '已领取' : (claimingMap[coupon.id] ? '领取中...' : '领取') }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
        <button
          class="slider-nav next"
          :disabled="currentIndex === maxIndex"
          @click="slide(1)"
        >
          ›
        </button>
      </div>

      <div v-else class="coupon-empty">
        <p>当前暂无可领取的优惠券</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/api'

const router = useRouter()
const userStore = useUserStore()

const promotions = ref([])
const coupons = ref([])
// Map couponId -> boolean (true means user currently holds an active unused coupon)
const claimedSet = ref(new Set())
// Map to track in-flight claims
const claimingMap = ref({})

// Slider state: show 4 items on desktop, 2 on mobile; move 1 card per click
const visibleCount = ref(typeof window !== 'undefined' && window.innerWidth <= 768 ? 2 : 4)
const currentIndex = ref(0)
const gapPx = 12
const sliderWindowRef = ref(null)

const maxIndex = computed(() => {
  const len = coupons.value?.length || 0
  const max = len - visibleCount.value
  return max > 0 ? max : 0
})
const trackVars = computed(() => ({
  '--gap': `${gapPx}px`,
  '--visible': visibleCount.value
}))

const slide = (step) => {
  if (!sliderWindowRef.value) return
  const firstCard = sliderWindowRef.value.querySelector('.coupon-item')
  const cardWidth = firstCard ? firstCard.getBoundingClientRect().width : 0
  const delta = cardWidth + gapPx
  const next = Math.min(Math.max(0, currentIndex.value + step), maxIndex.value)
  const diff = next - currentIndex.value
  if (diff !== 0) {
    sliderWindowRef.value.scrollBy({ left: diff * delta, behavior: 'smooth' })
    currentIndex.value = next
  }
}

const updateVisibleCount = () => {
  visibleCount.value = window.innerWidth <= 768 ? 2 : 4
  currentIndex.value = 0
  if (sliderWindowRef.value) {
    sliderWindowRef.value.scrollTo({ left: 0, behavior: 'auto' })
  }
}

watch([coupons, visibleCount], () => {
  if (currentIndex.value > maxIndex.value) currentIndex.value = maxIndex.value
})

const loadPromotions = async () => {
  try {
    const res = await api.marketing.promotions()
    if (res.code === 200) {
      promotions.value = res.data
    }
  } catch (error) {
    console.error('Failed to load promotions:', error)
  }
}

const loadCoupons = async () => {
  try {
    const res = await api.marketing.coupons()
    if (res.code === 200) {
      coupons.value = res.data
      if (currentIndex.value > maxIndex.value) currentIndex.value = maxIndex.value
    }
  } catch (error) {
    console.error('Failed to load coupons:', error)
  }

  // If user is logged in, load the user's coupons to determine which are already claimed
  if (userStore.isLoggedIn) {
    await loadMyCoupons()
  }
}

const loadMyCoupons = async () => {
  try {
    const res = await api.marketing.myCoupons()
    if (res.code === 200) {
      // Build set of couponIds that are active (status == 0 and endTime >= now)
      const now = new Date()
      const set = new Set()
      const list = res.data
      for (const uc of list) {
        // uc.endTime may be a string; attempt to parse
        const endTime = uc.endTime ? new Date(uc.endTime) : null
        const status = uc.status
        if (status === 0 && endTime && endTime.getTime() >= now.getTime()) {
          set.add(uc.couponId)
        }
      }
      claimedSet.value = set
    }
  } catch (error) {
    console.error('Failed to load my coupons:', error)
  }
}

const handleJoin = () => {
  router.push('/vehicles')
}

// Resolve promo image: accept absolute URLs, /images/... or a simple filename like 'weekend.png'
const getPromoImage = (promo) => {
  if (!promo) return '/images/promo-default.jpg'
  let img = promo.image || ''
  img = String(img).trim()
  if (!img) return '/images/promo-default.jpg'
  if (img.startsWith('http://') || img.startsWith('https://')) return img
  if (img.startsWith('/')) return img
  // if it's already under promo/ or images/promo
  if (img.startsWith('promo/') || img.startsWith('images/promo/') || img.startsWith('promo\\') ) {
    // normalize to /images/promo/...
    const name = img.replace(/^images\//, '').replace(/^promo\//, '')
    return '/images/promo/' + name
  }
  // plain filename -> images/promo/<filename>
  return '/images/promo/' + img
}

const isClaimed = (coupon) => {
  return claimedSet.value.has(coupon.id)
}

const claimCoupon = async (coupon) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // Prevent duplicate clicks
  if (isClaimed(coupon) || claimingMap.value[coupon.id]) return
  claimingMap.value = { ...claimingMap.value, [coupon.id]: true }

  try {
    const res = await api.marketing.claimCoupon({ couponId: coupon.id })
    if (res.code === 200) {
      ElMessage.success('领取成功')
      // mark as claimed locally
      const newSet = new Set(claimedSet.value)
      newSet.add(coupon.id)
      claimedSet.value = newSet
      // refresh my coupons to reflect server state
      await loadMyCoupons()
    } else {
      ElMessage.error(res.message || '领取失败')
    }
  } catch (error) {
    ElMessage.error('领取失败')
  } finally {
    const m = { ...claimingMap.value }
    delete m[coupon.id]
    claimingMap.value = m
  }
}

onMounted(() => {
  updateVisibleCount()
  window.addEventListener('resize', updateVisibleCount)
  loadPromotions()
  loadCoupons()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateVisibleCount)
})
</script>

<style scoped>
.promotions-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-header h1 {
  font-size: 32px;
  color: #303133;
  margin-bottom: 10px;
}

.page-header p {
  color: #909399;
}

.promo-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30px;
  margin-bottom: 40px;
}

.promo-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.promo-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.promo-image {
  height: 200px;
}

.promo-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.promo-content {
  padding: 25px;
}

.promo-content h2 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 10px;
}

.promo-content p {
  color: #606266;
  margin-bottom: 15px;
  line-height: 1.6;
}

.coupons-section h2 {
  font-size: 22px;
  color: #303133;
  margin-bottom: 20px;
}

.coupon-slider {
  position: relative;
  width: 100%;
}

.slider-window {
  overflow: hidden;
}

.slider-track {
  display: flex;
  gap: var(--gap);
  padding: 10px 0;
}

.coupon-item {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  flex: 0 0 calc((100% - (var(--gap) * (var(--visible) - 1))) / var(--visible));
  min-width: calc((100% - (var(--gap) * (var(--visible) - 1))) / var(--visible));
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.slider-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.12);
  color: #303133;
  font-size: 22px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease, opacity 0.2s ease;
  z-index: 2;
}

.slider-nav:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.2);
}

.slider-nav:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.slider-nav.prev {
  left: -6px;
}

.slider-nav.next {
  right: -6px;
}

.coupon-value .amount {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #F56C6C;
}

.coupon-value .condition {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.coupon-info h3 {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.coupon-info .desc {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.coupon-info .time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.coupon-action {
  text-align: center;
}

.coupon-empty {
  padding: 30px 0;
  text-align: center;
  color: #909399;
}

@media screen and (max-width: 768px) {
  .promo-list {
    grid-template-columns: 1fr;
  }
}
</style>
