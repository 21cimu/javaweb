<template>
  <div class="vehicle-list-page">
    <!-- Filters -->
    <div class="filter-section">
      <el-card shadow="never">
        <el-form :inline="true" :model="filters">
          <el-form-item label="车型">
            <el-select v-model="filters.category" placeholder="全部车型" clearable style="width: 120px;">
              <el-option label="经济型" value="economy" />
              <el-option label="紧凑型" value="compact" />
              <el-option label="中型" value="midsize" />
              <el-option label="SUV" value="suv" />
              <el-option label="豪华型" value="luxury" />
              <el-option label="MPV" value="minivan" />
            </el-select>
          </el-form-item>
          <el-form-item label="品牌">
            <el-select v-model="filters.brand" placeholder="全部品牌" clearable style="width: 120px;">
              <el-option v-for="brand in brands" :key="brand" :label="brand" :value="brand" />
            </el-select>
          </el-form-item>
          <el-form-item label="能源">
            <el-select v-model="filters.fuelType" placeholder="全部能源" clearable style="width: 100px;">
              <el-option label="汽油" value="gasoline" />
              <el-option label="柴油" value="diesel" />
              <el-option label="纯电" value="electric" />
              <el-option label="混动" value="hybrid" />
            </el-select>
          </el-form-item>
          <el-form-item label="座位数">
            <el-select v-model="filters.minSeats" placeholder="不限" clearable style="width: 100px;">
              <el-option label="2座及以上" :value="2" />
              <el-option label="5座及以上" :value="5" />
              <el-option label="7座及以上" :value="7" />
            </el-select>
          </el-form-item>
          <el-form-item label="价格区间">
            <el-select v-model="filters.priceRange" placeholder="不限" clearable style="width: 130px;">
              <el-option label="200元以下" value="0-200" />
              <el-option label="200-400元" value="200-400" />
              <el-option label="400-600元" value="400-600" />
              <el-option label="600元以上" value="600" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">筛选</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
    
    <!-- Sort Bar -->
    <div class="sort-bar">
      <div class="result-count">共找到 <strong>{{ total }}</strong> 辆车</div>
      <div class="sort-options">
        <span>排序：</span>
        <el-radio-group v-model="sortBy" @change="handleSort">
          <el-radio-button value="default">默认</el-radio-button>
          <el-radio-button value="price_asc">价格低到高</el-radio-button>
          <el-radio-button value="price_desc">价格高到低</el-radio-button>
          <el-radio-button value="rating">好评优先</el-radio-button>
          <el-radio-button value="popular">人气优先</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    
    <!-- Vehicle Grid -->
    <div class="vehicle-grid" v-loading="loading">
      <div 
        v-for="vehicle in vehicles" 
        :key="vehicle.id" 
        class="vehicle-card"
        @click="$router.push(`/vehicles/${vehicle.id}`)"
      >
        <div class="card-badges">
          <el-tag v-if="vehicle.isHot" type="danger" size="small" effect="dark">热门</el-tag>
          <el-tag v-if="vehicle.isNew" type="success" size="small" effect="dark">新车</el-tag>
          <el-tag v-if="vehicle.noDeposit" type="warning" size="small" effect="dark">免押</el-tag>
        </div>
        <img :src="getImageUrl(vehicle)" :alt="vehicle.model" class="vehicle-image">
        <div class="vehicle-info">
          <h3 class="vehicle-name">{{ vehicle.brand }} {{ vehicle.model }}</h3>
          <p class="vehicle-series">{{ vehicle.series }}</p>
          <div class="vehicle-specs">
            <span><el-icon><User /></el-icon> {{ vehicle.seats }}座</span>
            <span><el-icon><Setting /></el-icon> {{ getTransmission(vehicle.transmission) }}</span>
            <span><el-icon><Lightning /></el-icon> {{ getFuelType(vehicle.fuelType) }}</span>
          </div>
          <div class="vehicle-bottom">
            <div class="vehicle-rating">
              <el-rate v-model="vehicle.rating" disabled show-score score-template="{value}" />
            </div>
            <div class="vehicle-price">
              <span class="price">¥{{ vehicle.dailyPrice }}</span>
              <span class="unit">/天</span>
            </div>
          </div>
        </div>
      </div>
      
      <el-empty v-if="!loading && vehicles.length === 0" description="暂无符合条件的车辆" />
    </div>
    
    <!-- Pagination -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 36, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadVehicles"
        @size-change="loadVehicles"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import { User, Setting, Lightning } from '@element-plus/icons-vue'

const route = useRoute()

const vehicles = ref([])
const brands = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const sortBy = ref('default')

const filters = ref({
  category: '',
  brand: '',
  fuelType: '',
  minSeats: null,
  priceRange: ''
})

const getTransmission = (type) => type === 'auto' ? '自动挡' : '手动挡'
const getFuelType = (type) => {
  const map = { gasoline: '汽油', diesel: '柴油', electric: '纯电', hybrid: '混动' }
  return map[type] || type
}

// resolve vehicle image: prefer mainImage, then first of images array, else placeholder
const getImageUrl = (vehicle) => {
  if (!vehicle) return '/images/car-placeholder.svg'
  if (vehicle.mainImage) return vehicle.mainImage
  if (Array.isArray(vehicle.images) && vehicle.images.length > 0) return vehicle.images[0]
  // sometimes API returns main_image or other keys
  if (vehicle.main_image) return vehicle.main_image
  if (vehicle.images && typeof vehicle.images === 'string') {
    try {
      const parsed = JSON.parse(vehicle.images)
      if (Array.isArray(parsed) && parsed.length > 0) return parsed[0]
    } catch (e) {}
  }
  return '/images/car-placeholder.svg'
}

const loadVehicles = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      sortBy: sortBy.value !== 'default' ? sortBy.value : undefined,
      ...filters.value
    }
    
    // Parse price range
    if (filters.value.priceRange) {
      const parts = filters.value.priceRange.split('-')
      const min = parts[0]
      const max = parts.length > 1 ? parts[1] : undefined
      if (min) params.minPrice = min
      if (max) params.maxPrice = max
    }
    
    delete params.priceRange
    
    const res = await api.vehicles.list(params)
    if (res.code === 200) {
      vehicles.value = res.data.list
      total.value = res.data.total
    }
  } catch (error) {
    console.error('Failed to load vehicles:', error)
  } finally {
    loading.value = false
  }
}

const loadBrands = async () => {
  try {
    const res = await api.vehicles.brands()
    if (res.code === 200) {
      brands.value = res.data
    }
  } catch (error) {
    console.error('Failed to load brands:', error)
  }
}

const handleSearch = () => {
  page.value = 1
  loadVehicles()
}

const handleReset = () => {
  filters.value = {
    category: '',
    brand: '',
    fuelType: '',
    minSeats: null,
    priceRange: ''
  }
  page.value = 1
  sortBy.value = 'default'
  loadVehicles()
}

const handleSort = () => {
  page.value = 1
  loadVehicles()
}

// Watch route query for initial filter values
watch(() => route.query, (query) => {
  if (query.category) filters.value.category = query.category
  if (query.brand) filters.value.brand = query.brand
  loadVehicles()
}, { immediate: true })

onMounted(() => {
  loadBrands()
})
</script>

<style scoped>
.vehicle-list-page {
  max-width: 1400px;
  margin: 0 auto;
}

.filter-section {
  margin-bottom: 20px;
}

.sort-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px 20px;
  background: #fff;
  border-radius: 8px;
}

.result-count {
  color: #606266;
}

.result-count strong {
  color: #409EFF;
}

.sort-options {
  display: flex;
  align-items: center;
  gap: 10px;
}

.vehicle-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 400px;
}

.vehicle-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.vehicle-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}

.card-badges {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 1;
  display: flex;
  gap: 5px;
}

.vehicle-image {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.vehicle-info {
  padding: 15px;
}

.vehicle-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.vehicle-series {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.vehicle-specs {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #606266;
  margin-bottom: 15px;
}

.vehicle-specs span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.vehicle-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.vehicle-price {
  display: flex;
  align-items: baseline;
}

.vehicle-price .price {
  font-size: 22px;
  font-weight: bold;
  color: #F56C6C;
}

.vehicle-price .unit {
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px 0;
}

@media screen and (max-width: 1200px) {
  .vehicle-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media screen and (max-width: 768px) {
  .vehicle-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .sort-bar {
    flex-direction: column;
    gap: 10px;
  }
}

@media screen and (max-width: 480px) {
  .vehicle-grid {
    grid-template-columns: 1fr;
  }
}
</style>
