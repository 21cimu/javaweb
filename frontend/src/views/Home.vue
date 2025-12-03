<template>
  <div class="home-page">
    <!-- Banner Section -->
    <section class="banner-section">
      <el-carousel height="400px" :interval="5000">
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div class="banner-item" :style="{ backgroundImage: `url(${banner.image})` }">
            <div class="banner-content">
              <h2>{{ banner.title }}</h2>
              <el-button type="primary" size="large" @click="$router.push(banner.link)">
                立即查看
              </el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
      
      <!-- Search Box -->
      <div class="search-box">
        <el-card shadow="always">
          <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
            <el-form-item label="取车城市">
              <el-select v-model="searchForm.city" placeholder="选择城市" style="width: 150px;">
                <el-option v-for="city in cities" :key="city" :label="city" :value="city" />
              </el-select>
            </el-form-item>
            <el-form-item label="取车时间">
              <el-date-picker
                v-model="searchForm.pickupTime"
                type="datetime"
                placeholder="选择取车时间"
                style="width: 200px;"
              />
            </el-form-item>
            <el-form-item label="还车时间">
              <el-date-picker
                v-model="searchForm.returnTime"
                type="datetime"
                placeholder="选择还车时间"
                style="width: 200px;"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">
                <el-icon><Search /></el-icon>
                搜索车辆
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </section>
    
    <!-- Categories Section -->
    <section class="categories-section">
      <div class="section-header">
        <h2>车型分类</h2>
        <router-link to="/vehicles">查看全部 <el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <div class="category-grid">
        <div 
          v-for="cat in categories" 
          :key="cat.code" 
          class="category-item"
          @click="$router.push(`/vehicles?category=${cat.code}`)"
        >
          <span class="icon">{{ cat.icon }}</span>
          <span class="name">{{ cat.name }}</span>
          <span class="count">{{ cat.count }}款</span>
        </div>
      </div>
    </section>
    
    <!-- Hot Vehicles Section -->
    <section class="hot-section">
      <div class="section-header">
        <h2>🔥 热门车型</h2>
        <router-link to="/vehicles?tag=hot">查看更多 <el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <div class="vehicle-grid">
        <div 
          v-for="vehicle in hotVehicles" 
          :key="vehicle.id" 
          class="vehicle-card"
          @click="$router.push(`/vehicles/${vehicle.id}`)"
        >
          <img :src="vehicle.mainImage || '/images/car-placeholder.jpg'" :alt="vehicle.model" class="vehicle-image">
          <div class="vehicle-info">
            <h3 class="vehicle-name">{{ vehicle.brand }} {{ vehicle.model }}</h3>
            <div class="vehicle-tags">
              <el-tag v-if="vehicle.isHot" type="danger" size="small">热门</el-tag>
              <el-tag v-if="vehicle.isNew" type="success" size="small">新车</el-tag>
              <el-tag v-if="vehicle.noDeposit" type="warning" size="small">免押</el-tag>
              <el-tag size="small">{{ vehicle.seats }}座</el-tag>
              <el-tag size="small">{{ getTransmission(vehicle.transmission) }}</el-tag>
            </div>
            <div class="vehicle-price">
              <span class="price">¥{{ vehicle.dailyPrice }}</span>
              <span class="unit">/天起</span>
            </div>
          </div>
        </div>
      </div>
    </section>
    
    <!-- New Vehicles Section -->
    <section class="new-section">
      <div class="section-header">
        <h2>✨ 新车上线</h2>
        <router-link to="/vehicles?tag=new">查看更多 <el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <div class="vehicle-grid">
        <div 
          v-for="vehicle in newVehicles" 
          :key="vehicle.id" 
          class="vehicle-card"
          @click="$router.push(`/vehicles/${vehicle.id}`)"
        >
          <img :src="vehicle.mainImage || '/images/car-placeholder.jpg'" :alt="vehicle.model" class="vehicle-image">
          <div class="vehicle-info">
            <h3 class="vehicle-name">{{ vehicle.brand }} {{ vehicle.model }}</h3>
            <div class="vehicle-tags">
              <el-tag v-if="vehicle.isNew" type="success" size="small">新车</el-tag>
              <el-tag size="small">{{ vehicle.seats }}座</el-tag>
              <el-tag size="small">{{ getFuelType(vehicle.fuelType) }}</el-tag>
            </div>
            <div class="vehicle-price">
              <span class="price">¥{{ vehicle.dailyPrice }}</span>
              <span class="unit">/天起</span>
            </div>
          </div>
        </div>
      </div>
    </section>
    
    <!-- Promotions Section -->
    <section class="promo-section">
      <div class="section-header">
        <h2>🎁 优惠活动</h2>
        <router-link to="/promotions">查看全部 <el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <div class="promo-grid">
        <div v-for="promo in promotions" :key="promo.id" class="promo-card">
          <div class="promo-icon">🎉</div>
          <div class="promo-info">
            <h3>{{ promo.title }}</h3>
            <p>{{ promo.description }}</p>
          </div>
          <el-button type="primary" size="small">立即参与</el-button>
        </div>
      </div>
    </section>
    
    <!-- Features Section -->
    <section class="features-section">
      <h2>为什么选择畅行租车</h2>
      <div class="feature-grid">
        <div class="feature-item">
          <el-icon :size="48" color="#409EFF"><Timer /></el-icon>
          <h3>便捷取还</h3>
          <p>全国200+门店，24小时自助取还车</p>
        </div>
        <div class="feature-item">
          <el-icon :size="48" color="#67C23A"><Lock /></el-icon>
          <h3>安全保障</h3>
          <p>全险覆盖，道路救援，安心出行</p>
        </div>
        <div class="feature-item">
          <el-icon :size="48" color="#E6A23C"><Money /></el-icon>
          <h3>透明定价</h3>
          <p>无隐藏费用，一口价更省心</p>
        </div>
        <div class="feature-item">
          <el-icon :size="48" color="#F56C6C"><Service /></el-icon>
          <h3>贴心服务</h3>
          <p>专业客服，全程陪伴</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { Search, ArrowRight, Timer, Lock, Money, Service } from '@element-plus/icons-vue'

const router = useRouter()

const banners = ref([
  { id: 1, title: '新用户专享 首单立减50元', image: '/images/banner1.jpg', link: '/promotions' },
  { id: 2, title: '周末特惠 低至8折', image: '/images/banner2.jpg', link: '/vehicles' },
  { id: 3, title: '豪华车型 品质之选', image: '/images/banner3.jpg', link: '/vehicles?category=luxury' }
])

const cities = ref(['北京', '上海', '广州', '深圳', '成都', '杭州'])
const categories = ref([])
const hotVehicles = ref([])
const newVehicles = ref([])
const promotions = ref([])

const searchForm = ref({
  city: '北京',
  pickupTime: null,
  returnTime: null
})

const getTransmission = (type) => type === 'auto' ? '自动挡' : '手动挡'
const getFuelType = (type) => {
  const map = { gasoline: '汽油', diesel: '柴油', electric: '纯电', hybrid: '混动' }
  return map[type] || type
}

const handleSearch = () => {
  const query = {}
  if (searchForm.value.city) query.city = searchForm.value.city
  router.push({ path: '/vehicles', query })
}

const loadData = async () => {
  try {
    const [catRes, hotRes, newRes, promoRes] = await Promise.all([
      api.vehicles.categories(),
      api.vehicles.hot(6),
      api.vehicles.new(6),
      api.marketing.promotions()
    ])
    
    if (catRes.code === 200) categories.value = catRes.data
    if (hotRes.code === 200) hotVehicles.value = hotRes.data
    if (newRes.code === 200) newVehicles.value = newRes.data
    if (promoRes.code === 200) promotions.value = promoRes.data
  } catch (error) {
    console.error('Failed to load data:', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.home-page {
  max-width: 1400px;
  margin: 0 auto;
}

.banner-section {
  position: relative;
  margin-bottom: 60px;
}

.banner-item {
  height: 400px;
  background-size: cover;
  background-position: center;
  background-color: #1a1a2e;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-content {
  text-align: center;
  color: #fff;
}

.banner-content h2 {
  font-size: 36px;
  margin-bottom: 20px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.search-box {
  position: absolute;
  bottom: -40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  width: 90%;
  max-width: 900px;
}

.search-box :deep(.el-card__body) {
  padding: 20px 30px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 10px;
}

.section-header h2 {
  font-size: 22px;
  color: #303133;
}

.section-header a {
  color: #409EFF;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
}

.categories-section {
  margin-bottom: 40px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 15px;
}

.category-item {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.category-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
}

.category-item .icon {
  font-size: 36px;
  display: block;
  margin-bottom: 10px;
}

.category-item .name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  display: block;
}

.category-item .count {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 5px;
}

.hot-section, .new-section {
  margin-bottom: 40px;
}

.vehicle-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.promo-section {
  margin-bottom: 40px;
}

.promo-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.promo-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 25px;
  display: flex;
  align-items: center;
  gap: 20px;
  color: #fff;
}

.promo-icon {
  font-size: 48px;
}

.promo-info {
  flex: 1;
}

.promo-info h3 {
  font-size: 18px;
  margin-bottom: 5px;
}

.promo-info p {
  font-size: 13px;
  opacity: 0.9;
}

.features-section {
  background: #f5f7fa;
  padding: 60px 20px;
  margin: 0 -20px;
  text-align: center;
}

.features-section h2 {
  font-size: 28px;
  margin-bottom: 40px;
  color: #303133;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30px;
  max-width: 1000px;
  margin: 0 auto;
}

.feature-item {
  padding: 20px;
}

.feature-item h3 {
  font-size: 16px;
  margin: 15px 0 10px;
  color: #303133;
}

.feature-item p {
  font-size: 13px;
  color: #909399;
}

@media screen and (max-width: 768px) {
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .vehicle-grid {
    grid-template-columns: 1fr;
  }
  
  .promo-grid {
    grid-template-columns: 1fr;
  }
  
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
