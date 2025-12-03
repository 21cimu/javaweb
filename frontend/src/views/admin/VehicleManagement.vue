<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { vehicleApi } from '../../api'

const vehicles = ref([])
const loading = ref(true)
const dialogVisible = ref(false)
const dialogTitle = ref('添加车辆')
const formLoading = ref(false)

const vehicleForm = reactive({
  id: null,
  plateNumber: '',
  brand: '',
  model: '',
  category: 'ECONOMY',
  color: '',
  seats: 5,
  transmission: 'AUTO',
  fuelType: 'GASOLINE',
  dailyPrice: 0,
  deposit: 0,
  depositFree: false,
  imageUrl: '',
  description: ''
})

const categories = [
  { label: '经济型', value: 'ECONOMY' },
  { label: '舒适型', value: 'COMFORT' },
  { label: 'SUV', value: 'SUV' },
  { label: '商务型', value: 'BUSINESS' },
  { label: '豪华型', value: 'LUXURY' },
  { label: '新能源', value: 'NEW_ENERGY' },
  { label: '七座', value: 'SEVEN_SEATS' }
]

const statusMap = {
  AVAILABLE: { text: '可用', type: 'success' },
  RENTED: { text: '已租', type: 'primary' },
  MAINTENANCE: { text: '维护中', type: 'warning' },
  CLEANING: { text: '清洁中', type: 'info' },
  OFFLINE: { text: '下架', type: 'danger' }
}

onMounted(async () => {
  await loadVehicles()
})

const loadVehicles = async () => {
  loading.value = true
  try {
    vehicles.value = await vehicleApi.getAll()
  } catch (e) {
    console.error('Failed to load vehicles:', e)
    ElMessage.error('加载车辆列表失败')
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  dialogTitle.value = '添加车辆'
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (vehicle) => {
  dialogTitle.value = '编辑车辆'
  Object.assign(vehicleForm, vehicle)
  dialogVisible.value = true
}

const resetForm = () => {
  vehicleForm.id = null
  vehicleForm.plateNumber = ''
  vehicleForm.brand = ''
  vehicleForm.model = ''
  vehicleForm.category = 'ECONOMY'
  vehicleForm.color = ''
  vehicleForm.seats = 5
  vehicleForm.transmission = 'AUTO'
  vehicleForm.fuelType = 'GASOLINE'
  vehicleForm.dailyPrice = 0
  vehicleForm.deposit = 0
  vehicleForm.depositFree = false
  vehicleForm.imageUrl = ''
  vehicleForm.description = ''
}

const saveVehicle = async () => {
  if (!vehicleForm.plateNumber || !vehicleForm.brand || !vehicleForm.model) {
    ElMessage.warning('请填写必要信息')
    return
  }
  
  formLoading.value = true
  try {
    let res
    if (vehicleForm.id) {
      res = await vehicleApi.update(vehicleForm.id, vehicleForm)
    } else {
      res = await vehicleApi.create(vehicleForm)
    }
    
    if (res.success) {
      ElMessage.success(vehicleForm.id ? '更新成功' : '添加成功')
      dialogVisible.value = false
      await loadVehicles()
    } else {
      ElMessage.error(res.error || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    formLoading.value = false
  }
}

const deleteVehicle = async (vehicle) => {
  try {
    await ElMessageBox.confirm(`确认删除车辆 ${vehicle.brand} ${vehicle.model}?`, '删除确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await vehicleApi.delete(vehicle.id)
    if (res.success) {
      ElMessage.success('删除成功')
      await loadVehicles()
    } else {
      ElMessage.error(res.error || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<template>
  <div class="vehicle-management-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>车辆管理</h2>
          <el-button type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon>
            添加车辆
          </el-button>
        </div>
      </template>
      
      <el-table :data="vehicles" v-loading="loading" stripe>
        <el-table-column prop="plateNumber" label="车牌号" width="120" />
        <el-table-column label="品牌型号" width="180">
          <template #default="{ row }">
            {{ row.brand }} {{ row.model }}
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            {{ categories.find(c => c.value === row.category)?.label || row.category }}
          </template>
        </el-table-column>
        <el-table-column prop="seats" label="座位" width="80" />
        <el-table-column prop="dailyPrice" label="日租金" width="100">
          <template #default="{ row }">
            ¥{{ row.dailyPrice }}
          </template>
        </el-table-column>
        <el-table-column prop="deposit" label="押金" width="100">
          <template #default="{ row }">
            <span v-if="row.depositFree" style="color: #67C23A">免押</span>
            <span v-else>¥{{ row.deposit }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteVehicle(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="vehicleForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="车牌号" required>
              <el-input v-model="vehicleForm.plateNumber" placeholder="请输入车牌号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌" required>
              <el-input v-model="vehicleForm.brand" placeholder="请输入品牌" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="型号" required>
              <el-input v-model="vehicleForm.model" placeholder="请输入型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="vehicleForm.category" style="width: 100%">
                <el-option
                  v-for="cat in categories"
                  :key="cat.value"
                  :label="cat.label"
                  :value="cat.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="颜色">
              <el-input v-model="vehicleForm.color" placeholder="请输入颜色" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="座位数">
              <el-input-number v-model="vehicleForm.seats" :min="2" :max="12" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="变速箱">
              <el-radio-group v-model="vehicleForm.transmission">
                <el-radio value="AUTO">自动</el-radio>
                <el-radio value="MANUAL">手动</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="能源类型">
              <el-select v-model="vehicleForm.fuelType" style="width: 100%">
                <el-option label="汽油" value="GASOLINE" />
                <el-option label="柴油" value="DIESEL" />
                <el-option label="纯电" value="ELECTRIC" />
                <el-option label="混动" value="HYBRID" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="日租金">
              <el-input-number v-model="vehicleForm.dailyPrice" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="押金">
              <el-input-number v-model="vehicleForm.deposit" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="免押金">
          <el-switch v-model="vehicleForm.depositFree" />
        </el-form-item>
        
        <el-form-item label="图片URL">
          <el-input v-model="vehicleForm.imageUrl" placeholder="请输入图片URL" />
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input 
            v-model="vehicleForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入车辆描述"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="saveVehicle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.vehicle-management-page {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
}
</style>
