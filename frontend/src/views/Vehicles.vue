<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { vehicleApi } from '../api'

const router = useRouter()
const route = useRoute()

const vehicles = ref([])
const loading = ref(true)
const searchKeyword = ref('')
const selectedCategory = ref('')
const priceRange = ref([0, 2000])

const categories = [
  { label: '全部', value: '' },
  { label: '经济型', value: 'ECONOMY' },
  { label: '舒适型', value: 'COMFORT' },
  { label: 'SUV', value: 'SUV' },
  { label: '商务型', value: 'BUSINESS' },
  { label: '豪华型', value: 'LUXURY' },
  { label: '新能源', value: 'NEW_ENERGY' },
  { label: '七座', value: 'SEVEN_SEATS' }
]

const fuelTypes = {
  GASOLINE: '汽油',
  DIESEL: '柴油',
  ELECTRIC: '纯电',
  HYBRID: '混动'
}

const transmissions = {
  AUTO: '自动挡',
  MANUAL: '手动挡'
}

const filteredVehicles = computed(() => {
  return vehicles.value.filter(v => {
    const matchCategory = !selectedCategory.value || v.category === selectedCategory.value
    const matchPrice = v.dailyPrice >= priceRange.value[0] && v.dailyPrice <= priceRange.value[1]
    const matchKeyword = !searchKeyword.value || 
      v.brand?.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      v.model?.toLowerCase().includes(searchKeyword.value.toLowerCase())
    return matchCategory && matchPrice && matchKeyword
  })
})

onMounted(async () => {
  // Check for category in query params
  if (route.query.category) {
    selectedCategory.value = route.query.category
  }
  
  await loadVehicles()
})

const loadVehicles = async () => {
  loading.value = true
  try {
    const data = await vehicleApi.getAvailable()
    vehicles.value = data
  } catch (e) {
    console.error('Failed to load vehicles:', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  // Filter is computed, no need to do anything
}

const goToDetail = (id) => {
  router.push(`/vehicles/${id}`)
}

const formatPrice = (price) => {
  return price ? `¥${price}` : '¥0'
}
</script>

<template>
  <div class="vehicles-page">
    <el-card class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索品牌或车型"
          prefix-icon="Search"
          clearable
          style="width: 300px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />
        
        <el-select v-model="selectedCategory" placeholder="车型分类" style="width: 150px">
          <el-option
            v-for="cat in categories"
            :key="cat.value"
            :label="cat.label"
            :value="cat.value"
          />
        </el-select>
        
        <div class="price-filter">
          <span>价格区间：</span>
          <el-slider
            v-model="priceRange"
            range
            :min="0"
            :max="2000"
            :step="50"
            :format-tooltip="formatPrice"
            style="width: 200px"
          />
          <span>{{ formatPrice(priceRange[0]) }} - {{ formatPrice(priceRange[1]) }}</span>
        </div>
      </div>
    </el-card>
    
    <div v-loading="loading" class="vehicle-list">
      <el-empty v-if="!loading && filteredVehicles.length === 0" description="暂无可用车辆" />
      
      <el-row :gutter="20">
        <el-col
          v-for="vehicle in filteredVehicles"
          :key="vehicle.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <el-card 
            class="vehicle-card" 
            shadow="hover"
            @click="goToDetail(vehicle.id)"
          >
            <div class="vehicle-image">
              <el-image 
                :src="vehicle.imageUrl || 'https://via.placeholder.com/300x200?text=Car'"
                fit="cover"
              />
              <div class="vehicle-badges">
                <el-tag v-if="vehicle.depositFree" type="success" size="small">免押金</el-tag>
                <el-tag v-if="vehicle.fuelType === 'ELECTRIC'" type="warning" size="small">新能源</el-tag>
              </div>
            </div>
            
            <div class="vehicle-info">
              <h3>{{ vehicle.brand }} {{ vehicle.model }}</h3>
              
              <div class="vehicle-specs">
                <span><el-icon><SetUp /></el-icon> {{ transmissions[vehicle.transmission] || '自动' }}</span>
                <span><el-icon><User /></el-icon> {{ vehicle.seats }}座</span>
                <span><el-icon><Oil /></el-icon> {{ fuelTypes[vehicle.fuelType] || '汽油' }}</span>
              </div>
              
              <div class="vehicle-footer">
                <div class="price">
                  <span class="amount">¥{{ vehicle.dailyPrice }}</span>
                  <span class="unit">/天</span>
                </div>
                <el-button type="primary" size="small">立即预订</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.vehicles-page {
  max-width: 1400px;
  margin: 0 auto;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.price-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}

.vehicle-list {
  min-height: 400px;
}

.vehicle-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s;
}

.vehicle-card:hover {
  transform: translateY(-4px);
}

.vehicle-image {
  position: relative;
  height: 180px;
  margin: -20px -20px 16px;
  overflow: hidden;
}

.vehicle-image .el-image {
  width: 100%;
  height: 100%;
}

.vehicle-badges {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.vehicle-info h3 {
  font-size: 16px;
  margin-bottom: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.vehicle-specs {
  display: flex;
  gap: 16px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}

.vehicle-specs span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.vehicle-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price .amount {
  font-size: 20px;
  font-weight: bold;
  color: #F56C6C;
}

.price .unit {
  font-size: 13px;
  color: #909399;
}
</style>
