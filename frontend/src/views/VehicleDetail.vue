<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { vehicleApi, orderApi } from '../api'

const router = useRouter()
const route = useRoute()

const vehicle = ref(null)
const loading = ref(true)
const orderLoading = ref(false)
const showOrderDialog = ref(false)

const orderForm = ref({
  pickupDate: '',
  returnDate: '',
  pickupMethod: 'STORE',
  returnMethod: 'STORE',
  remark: ''
})

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

const categories = {
  ECONOMY: '经济型',
  COMFORT: '舒适型',
  SUV: 'SUV',
  BUSINESS: '商务型',
  LUXURY: '豪华型',
  NEW_ENERGY: '新能源',
  SEVEN_SEATS: '七座'
}

// Calculate rental days and total price
const rentalDays = computed(() => {
  if (!orderForm.value.pickupDate || !orderForm.value.returnDate) return 0
  const pickup = new Date(orderForm.value.pickupDate)
  const returnD = new Date(orderForm.value.returnDate)
  const diff = Math.ceil((returnD - pickup) / (1000 * 60 * 60 * 24))
  return diff > 0 ? diff : 0
})

const totalPrice = computed(() => {
  if (!vehicle.value || rentalDays.value === 0) return 0
  return vehicle.value.dailyPrice * rentalDays.value
})

onMounted(async () => {
  const id = route.params.id
  if (id) {
    await loadVehicle(id)
  }
})

const loadVehicle = async (id) => {
  loading.value = true
  try {
    vehicle.value = await vehicleApi.getById(id)
  } catch (e) {
    console.error('Failed to load vehicle:', e)
    ElMessage.error('加载车辆信息失败')
  } finally {
    loading.value = false
  }
}

const openOrderDialog = () => {
  const user = localStorage.getItem('user')
  if (!user) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  showOrderDialog.value = true
}

const submitOrder = async () => {
  if (!orderForm.value.pickupDate || !orderForm.value.returnDate) {
    ElMessage.warning('请选择取车和还车日期')
    return
  }
  
  if (rentalDays.value <= 0) {
    ElMessage.warning('还车日期必须晚于取车日期')
    return
  }
  
  const user = JSON.parse(localStorage.getItem('user'))
  
  orderLoading.value = true
  try {
    const res = await orderApi.create({
      vehicleId: vehicle.value.id,
      userId: user.id,
      pickupTime: new Date(orderForm.value.pickupDate).toISOString().replace('Z', ''),
      returnTime: new Date(orderForm.value.returnDate).toISOString().replace('Z', ''),
      pickupMethod: orderForm.value.pickupMethod,
      returnMethod: orderForm.value.returnMethod,
      rentalDays: rentalDays.value,
      remark: orderForm.value.remark
    })
    
    if (res.success) {
      ElMessage.success('订单创建成功')
      showOrderDialog.value = false
      router.push(`/order/${res.order.id}`)
    } else {
      ElMessage.error(res.error || '创建订单失败')
    }
  } catch (e) {
    console.error('Failed to create order:', e)
    ElMessage.error('创建订单失败，请稍后重试')
  } finally {
    orderLoading.value = false
  }
}

const goBack = () => {
  router.push('/vehicles')
}
</script>

<template>
  <div class="vehicle-detail-page">
    <el-button @click="goBack" icon="ArrowLeft" class="back-btn">返回列表</el-button>
    
    <div v-loading="loading" class="detail-content">
      <template v-if="vehicle">
        <el-row :gutter="30">
          <!-- Vehicle Image -->
          <el-col :xs="24" :md="12">
            <el-card class="image-card">
              <el-image 
                :src="vehicle.imageUrl || 'https://via.placeholder.com/600x400?text=Car'"
                fit="cover"
                style="width: 100%; height: 400px; border-radius: 8px;"
              />
            </el-card>
          </el-col>
          
          <!-- Vehicle Info -->
          <el-col :xs="24" :md="12">
            <el-card class="info-card">
              <template #header>
                <div class="card-header">
                  <h1>{{ vehicle.brand }} {{ vehicle.model }}</h1>
                  <div class="tags">
                    <el-tag v-if="vehicle.depositFree" type="success">免押金</el-tag>
                    <el-tag type="info">{{ categories[vehicle.category] || '其他' }}</el-tag>
                  </div>
                </div>
              </template>
              
              <div class="price-section">
                <span class="price">¥{{ vehicle.dailyPrice }}</span>
                <span class="unit">/天</span>
                <span v-if="!vehicle.depositFree" class="deposit">
                  押金：¥{{ vehicle.deposit }}
                </span>
              </div>
              
              <el-divider />
              
              <div class="specs-section">
                <h3>车辆规格</h3>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="车牌号">{{ vehicle.plateNumber }}</el-descriptions-item>
                  <el-descriptions-item label="颜色">{{ vehicle.color }}</el-descriptions-item>
                  <el-descriptions-item label="座位数">{{ vehicle.seats }}座</el-descriptions-item>
                  <el-descriptions-item label="变速箱">{{ transmissions[vehicle.transmission] }}</el-descriptions-item>
                  <el-descriptions-item label="能源类型">{{ fuelTypes[vehicle.fuelType] }}</el-descriptions-item>
                  <el-descriptions-item label="里程">{{ vehicle.mileage }}公里</el-descriptions-item>
                </el-descriptions>
              </div>
              
              <div v-if="vehicle.description" class="description-section">
                <h3>车辆描述</h3>
                <p>{{ vehicle.description }}</p>
              </div>
              
              <el-divider />
              
              <div class="action-section">
                <el-button 
                  type="primary" 
                  size="large"
                  @click="openOrderDialog"
                  :disabled="vehicle.status !== 'AVAILABLE'"
                >
                  <el-icon><ShoppingCart /></el-icon>
                  立即预订
                </el-button>
                <el-tag v-if="vehicle.status !== 'AVAILABLE'" type="danger">
                  该车辆暂不可租
                </el-tag>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </div>
    
    <!-- Order Dialog -->
    <el-dialog 
      v-model="showOrderDialog" 
      title="预订车辆"
      width="500px"
    >
      <el-form :model="orderForm" label-width="100px">
        <el-form-item label="取车日期" required>
          <el-date-picker
            v-model="orderForm.pickupDate"
            type="datetime"
            placeholder="选择取车日期"
            :disabled-date="(date) => date < new Date()"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="还车日期" required>
          <el-date-picker
            v-model="orderForm.returnDate"
            type="datetime"
            placeholder="选择还车日期"
            :disabled-date="(date) => date < new Date(orderForm.pickupDate || Date.now())"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="取车方式">
          <el-radio-group v-model="orderForm.pickupMethod">
            <el-radio value="STORE">到店自取</el-radio>
            <el-radio value="DELIVERY">送车上门</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="还车方式">
          <el-radio-group v-model="orderForm.returnMethod">
            <el-radio value="STORE">到店还车</el-radio>
            <el-radio value="PICKUP">上门取车</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="备注">
          <el-input 
            v-model="orderForm.remark" 
            type="textarea" 
            placeholder="特殊需求或备注信息"
          />
        </el-form-item>
        
        <el-divider />
        
        <div class="order-summary">
          <p>租赁天数：<strong>{{ rentalDays }}</strong> 天</p>
          <p>日租金：<strong>¥{{ vehicle?.dailyPrice || 0 }}</strong></p>
          <p v-if="!vehicle?.depositFree">押金：<strong>¥{{ vehicle?.deposit || 0 }}</strong></p>
          <p class="total">合计租金：<strong>¥{{ totalPrice }}</strong></p>
        </div>
      </el-form>
      
      <template #footer>
        <el-button @click="showOrderDialog = false">取消</el-button>
        <el-button type="primary" :loading="orderLoading" @click="submitOrder">
          确认预订
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.vehicle-detail-page {
  max-width: 1200px;
  margin: 0 auto;
}

.back-btn {
  margin-bottom: 20px;
}

.image-card .el-card__body {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h1 {
  font-size: 24px;
  margin: 0;
}

.tags {
  display: flex;
  gap: 8px;
}

.price-section {
  margin-bottom: 20px;
}

.price-section .price {
  font-size: 32px;
  font-weight: bold;
  color: #F56C6C;
}

.price-section .unit {
  font-size: 16px;
  color: #909399;
}

.price-section .deposit {
  margin-left: 20px;
  font-size: 14px;
  color: #909399;
}

.specs-section h3,
.description-section h3 {
  font-size: 16px;
  margin-bottom: 12px;
}

.description-section p {
  color: #606266;
  line-height: 1.6;
}

.action-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-summary {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
}

.order-summary p {
  margin: 8px 0;
}

.order-summary .total {
  font-size: 18px;
  color: #F56C6C;
}
</style>
