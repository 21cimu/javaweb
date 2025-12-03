<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { vehicleApi } from '../api'

const router = useRouter()
const featuredVehicles = ref([])
const loading = ref(true)

const categories = [
  { name: '经济型', value: 'ECONOMY', icon: 'Bicycle' },
  { name: '舒适型', value: 'COMFORT', icon: 'Van' },
  { name: 'SUV', value: 'SUV', icon: 'Truck' },
  { name: '商务型', value: 'BUSINESS', icon: 'OfficeBuilding' },
  { name: '豪华型', value: 'LUXURY', icon: 'Trophy' },
  { name: '新能源', value: 'NEW_ENERGY', icon: 'Lightning' }
]

onMounted(async () => {
  try {
    const vehicles = await vehicleApi.getAvailable()
    featuredVehicles.value = vehicles.slice(0, 4)
  } catch (e) {
    console.error('Failed to load vehicles:', e)
  } finally {
    loading.value = false
  }
})

const goToVehicles = (category = '') => {
  if (category) {
    router.push({ path: '/vehicles', query: { category } })
  } else {
    router.push('/vehicles')
  }
}

const goToVehicleDetail = (id) => {
  router.push(`/vehicles/${id}`)
}
</script>

<template>
  <div class="home-page">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-content">
        <h1>轻松租车 · 畅享出行</h1>
        <p>专业的租车平台，为您提供安全、便捷、实惠的租车服务</p>
        <el-button type="primary" size="large" @click="goToVehicles()">
          立即选车
        </el-button>
      </div>
    </div>
    
    <!-- Category Section -->
    <div class="section">
      <h2 class="section-title">车型分类</h2>
      <div class="category-grid">
        <el-card 
          v-for="cat in categories" 
          :key="cat.value"
          class="category-card"
          shadow="hover"
          @click="goToVehicles(cat.value)"
        >
          <div class="category-content">
            <el-icon :size="40"><component :is="cat.icon" /></el-icon>
            <span>{{ cat.name }}</span>
          </div>
        </el-card>
      </div>
    </div>
    
    <!-- Featured Vehicles -->
    <div class="section">
      <h2 class="section-title">热门车型</h2>
      <div v-loading="loading" class="vehicle-grid">
        <el-card 
          v-for="vehicle in featuredVehicles" 
          :key="vehicle.id"
          class="vehicle-card"
          shadow="hover"
          @click="goToVehicleDetail(vehicle.id)"
        >
          <div class="vehicle-image">
            <el-image 
              :src="vehicle.imageUrl || 'https://via.placeholder.com/300x200?text=Car'"
              fit="cover"
            />
            <el-tag v-if="vehicle.depositFree" type="success" class="deposit-free-tag">免押金</el-tag>
          </div>
          <div class="vehicle-info">
            <h3>{{ vehicle.brand }} {{ vehicle.model }}</h3>
            <div class="vehicle-tags">
              <el-tag size="small">{{ vehicle.transmission === 'AUTO' ? '自动挡' : '手动挡' }}</el-tag>
              <el-tag size="small" type="info">{{ vehicle.seats }}座</el-tag>
              <el-tag size="small" type="warning">{{ vehicle.fuelType === 'ELECTRIC' ? '电动' : '燃油' }}</el-tag>
            </div>
            <div class="vehicle-price">
              <span class="price">¥{{ vehicle.dailyPrice }}</span>
              <span class="unit">/天</span>
            </div>
          </div>
        </el-card>
      </div>
      <div class="view-more">
        <el-button type="primary" plain @click="goToVehicles()">查看更多车型</el-button>
      </div>
    </div>
    
    <!-- Features Section -->
    <div class="section features-section">
      <h2 class="section-title">为什么选择我们</h2>
      <div class="features-grid">
        <div class="feature-item">
          <el-icon :size="48" color="#409EFF"><Shield /></el-icon>
          <h3>安全保障</h3>
          <p>全面的保险覆盖，让您出行无忧</p>
        </div>
        <div class="feature-item">
          <el-icon :size="48" color="#67C23A"><Timer /></el-icon>
          <h3>便捷高效</h3>
          <p>线上预订，快速取还车</p>
        </div>
        <div class="feature-item">
          <el-icon :size="48" color="#E6A23C"><Money /></el-icon>
          <h3>价格透明</h3>
          <p>无隐藏费用，价格公开透明</p>
        </div>
        <div class="feature-item">
          <el-icon :size="48" color="#F56C6C"><Service /></el-icon>
          <h3>贴心服务</h3>
          <p>24小时客服，全程为您服务</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  background: linear-gradient(135deg, #409EFF 0%, #67C23A 100%);
  border-radius: 12px;
  padding: 60px 40px;
  margin-bottom: 40px;
  color: white;
  text-align: center;
}

.hero-content h1 {
  font-size: 36px;
  margin-bottom: 16px;
}

.hero-content p {
  font-size: 18px;
  margin-bottom: 24px;
  opacity: 0.9;
}

.section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 24px;
  margin-bottom: 24px;
  text-align: center;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.category-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.category-card:hover {
  transform: translateY(-4px);
}

.category-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
}

.vehicle-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.vehicle-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.vehicle-card:hover {
  transform: translateY(-4px);
}

.vehicle-image {
  position: relative;
  height: 160px;
  overflow: hidden;
  margin: -20px -20px 16px;
  border-radius: 4px 4px 0 0;
}

.vehicle-image .el-image {
  width: 100%;
  height: 100%;
}

.deposit-free-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.vehicle-info h3 {
  font-size: 16px;
  margin-bottom: 8px;
}

.vehicle-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.vehicle-price {
  color: #F56C6C;
}

.vehicle-price .price {
  font-size: 20px;
  font-weight: bold;
}

.vehicle-price .unit {
  font-size: 14px;
}

.view-more {
  text-align: center;
  margin-top: 24px;
}

.features-section {
  background: #f5f7fa;
  padding: 40px;
  border-radius: 12px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.feature-item {
  text-align: center;
  padding: 24px;
  background: white;
  border-radius: 8px;
}

.feature-item h3 {
  margin: 16px 0 8px;
}

.feature-item p {
  color: #909399;
  font-size: 14px;
}

@media (max-width: 1024px) {
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .vehicle-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .vehicle-grid {
    grid-template-columns: 1fr;
  }
  .features-grid {
    grid-template-columns: 1fr;
  }
}
</style>
