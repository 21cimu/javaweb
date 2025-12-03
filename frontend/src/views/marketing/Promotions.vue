<template>
  <div class="promotions-page">
    <div class="page-header">
      <h1>优惠活动</h1>
      <p>精彩活动不容错过</p>
    </div>
    
    <div class="promo-list">
      <div v-for="promo in promotions" :key="promo.id" class="promo-card">
        <div class="promo-image">
          <img :src="promo.image || '/images/promo-default.jpg'" :alt="promo.title">
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
      <div class="coupon-grid">
        <div v-for="coupon in coupons" :key="coupon.id" class="coupon-item">
          <div class="coupon-value">
            <span class="amount" v-if="coupon.type === 1 || coupon.type === 3">¥{{ coupon.discountAmount }}</span>
            <span class="amount" v-else>{{ (coupon.discountRate * 10).toFixed(1) }}折</span>
            <span class="condition" v-if="coupon.minAmount > 0">满{{ coupon.minAmount }}可用</span>
          </div>
          <div class="coupon-info">
            <h3>{{ coupon.name }}</h3>
            <p>{{ coupon.description }}</p>
          </div>
          <el-button type="danger" size="small" @click="claimCoupon(coupon)">领取</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/api'

const router = useRouter()
const userStore = useUserStore()

const promotions = ref([])
const coupons = ref([])

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
    }
  } catch (error) {
    console.error('Failed to load coupons:', error)
  }
}

const handleJoin = (promo) => {
  router.push('/vehicles')
}

const claimCoupon = async (coupon) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    const res = await api.marketing.claimCoupon({ couponId: coupon.id })
    if (res.code === 200) {
      ElMessage.success('领取成功')
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('领取失败')
  }
}

onMounted(() => {
  loadPromotions()
  loadCoupons()
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.coupon-item {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.coupon-value .amount {
  display: block;
  font-size: 32px;
  font-weight: bold;
  color: #F56C6C;
}

.coupon-value .condition {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.coupon-info h3 {
  font-size: 14px;
  color: #303133;
  margin-bottom: 5px;
}

.coupon-info p {
  font-size: 12px;
  color: #909399;
  margin-bottom: 15px;
}

@media screen and (max-width: 768px) {
  .promo-list {
    grid-template-columns: 1fr;
  }
  
  .coupon-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
