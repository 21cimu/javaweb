<template>
  <div class="coupons-page">
    <div class="page-header">
      <h1>我的优惠券</h1>
    </div>
    
    <el-tabs v-model="activeTab" @tab-change="loadCoupons">
      <el-tab-pane label="可使用" name="0" />
      <el-tab-pane label="已使用" name="1" />
      <el-tab-pane label="已过期" name="2" />
    </el-tabs>
    
    <div class="coupon-list" v-loading="loading">
      <div v-for="coupon in coupons" :key="coupon.id" class="coupon-card" :class="{ disabled: activeTab !== '0' }">
        <div class="coupon-left">
          <span class="amount" v-if="coupon.type === 1 || coupon.type === 3">
            ¥{{ coupon.discountAmount }}
          </span>
          <span class="amount" v-else>
            {{ (coupon.discountRate * 10).toFixed(1) }}折
          </span>
          <span class="condition" v-if="coupon.minAmount > 0">满{{ coupon.minAmount }}可用</span>
          <span class="condition" v-else>无门槛</span>
        </div>
        <div class="coupon-right">
          <h3>{{ coupon.name }}</h3>
          <p class="desc">{{ coupon.description }}</p>
          <p class="expire">有效期至 {{ formatDate(coupon.endTime) }}</p>
        </div>
        <div class="coupon-action" v-if="activeTab === '0'">
          <el-button type="primary" size="small" @click="goUse">去使用</el-button>
        </div>
      </div>
      
      <el-empty v-if="!loading && coupons.length === 0" description="暂无优惠券" />
    </div>
    
    <!-- Available Coupons to Claim -->
    <div class="claim-section" v-if="activeTab === '0'">
      <h2>领取更多优惠券</h2>
      <div class="available-coupons">
        <div v-for="coupon in availableCoupons" :key="coupon.id" class="coupon-card claimable">
          <div class="coupon-left">
            <span class="amount" v-if="coupon.type === 1 || coupon.type === 3">
              ¥{{ coupon.discountAmount }}
            </span>
            <span class="amount" v-else>
              {{ (coupon.discountRate * 10).toFixed(1) }}折
            </span>
            <span class="condition" v-if="coupon.minAmount > 0">满{{ coupon.minAmount }}可用</span>
          </div>
          <div class="coupon-right">
            <h3>{{ coupon.name }}</h3>
            <p class="expire">有效期至 {{ formatDate(coupon.endTime) }}</p>
          </div>
          <div class="coupon-action">
            <el-button type="danger" size="small" @click="claimCoupon(coupon)">立即领取</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'
import dayjs from 'dayjs'

const router = useRouter()

const activeTab = ref('0')
const loading = ref(false)
const coupons = ref([])
const availableCoupons = ref([])

const formatDate = (date) => dayjs(date).format('YYYY-MM-DD')

const loadCoupons = async () => {
  loading.value = true
  try {
    const res = await api.marketing.myCoupons({ status: parseInt(activeTab.value) })
    if (res.code === 200) {
      coupons.value = res.data
    }
  } catch (error) {
    console.error('Failed to load coupons:', error)
  } finally {
    loading.value = false
  }
}

const loadAvailableCoupons = async () => {
  try {
    const res = await api.marketing.coupons()
    if (res.code === 200) {
      availableCoupons.value = res.data
    }
  } catch (error) {
    console.error('Failed to load available coupons:', error)
  }
}

const claimCoupon = async (coupon) => {
  try {
    const res = await api.marketing.claimCoupon({ couponId: coupon.id })
    if (res.code === 200) {
      ElMessage.success('领取成功')
      loadCoupons()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('领取失败')
  }
}

const goUse = () => {
  router.push('/vehicles')
}

onMounted(() => {
  loadCoupons()
  loadAvailableCoupons()
})
</script>

<style scoped>
.coupons-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #303133;
}

.coupon-list {
  min-height: 300px;
}

.coupon-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 15px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.coupon-card.disabled {
  opacity: 0.6;
}

.coupon-left {
  width: 120px;
  background: linear-gradient(135deg, #F56C6C 0%, #E6A23C 100%);
  color: #fff;
  padding: 20px;
  text-align: center;
}

.coupon-left .amount {
  display: block;
  font-size: 28px;
  font-weight: bold;
}

.coupon-left .condition {
  font-size: 11px;
  opacity: 0.9;
}

.coupon-right {
  flex: 1;
  padding: 15px 20px;
}

.coupon-right h3 {
  font-size: 16px;
  color: #303133;
  margin-bottom: 5px;
}

.coupon-right .desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 5px;
}

.coupon-right .expire {
  font-size: 12px;
  color: #909399;
}

.coupon-action {
  padding: 0 20px;
}

.claim-section {
  margin-top: 40px;
}

.claim-section h2 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 20px;
}

.coupon-card.claimable .coupon-left {
  background: linear-gradient(135deg, #409EFF 0%, #67C23A 100%);
}
</style>
