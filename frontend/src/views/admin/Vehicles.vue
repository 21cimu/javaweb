<template>
  <div class="admin-vehicles-page">
    <div class="page-header">
      <h1>车辆管理</h1>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 添加车辆
      </el-button>
    </div>
    
    <!-- Stats -->
    <div class="stats-bar">
      <div class="stat-item">
        <span class="label">总车辆</span>
        <span class="value">{{ stats.total || 0 }}</span>
      </div>
      <div class="stat-item">
        <span class="label">可租</span>
        <span class="value success">{{ stats.available || 0 }}</span>
      </div>
      <div class="stat-item">
        <span class="label">预订中</span>
        <span class="value warning">{{ stats.booked || 0 }}</span>
      </div>
      <div class="stat-item">
        <span class="label">出租中</span>
        <span class="value primary">{{ stats.rented || 0 }}</span>
      </div>
      <div class="stat-item">
        <span class="label">维修中</span>
        <span class="value danger">{{ stats.maintenance || 0 }}</span>
      </div>
    </div>
    
    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 120px;">
            <el-option label="可租" :value="1" />
            <el-option label="预订中" :value="2" />
            <el-option label="出租中" :value="3" />
            <el-option label="维修中" :value="4" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadVehicles">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="vehicles" v-loading="loading" stripe>
        <el-table-column prop="plateNumber" label="车牌号" width="120" />
        <el-table-column label="车型">
          <template #default="{ row }">
            {{ row.brand }} {{ row.model }}
          </template>
        </el-table-column>
        <el-table-column prop="category" label="类型" width="100">
          <template #default="{ row }">
            {{ getCategoryName(row.category) }}
          </template>
        </el-table-column>
        <el-table-column prop="dailyPrice" label="日租价" width="100">
          <template #default="{ row }">¥{{ row.dailyPrice }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="80" />
        <el-table-column prop="rating" label="评分" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button type="primary" link size="small">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="available" v-if="row.status !== 1">设为可租</el-dropdown-item>
                  <el-dropdown-item command="maintenance" v-if="row.status !== 4">设为维修</el-dropdown-item>
                  <el-dropdown-item command="offline" v-if="row.status !== 0">下架</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadVehicles"
          @size-change="loadVehicles"
        />
      </div>
    </el-card>
    
    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑车辆' : '添加车辆'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="车牌号" prop="plateNumber">
              <el-input v-model="form.plateNumber" placeholder="如：京A12345" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="VIN码" prop="vin">
              <el-input v-model="form.vin" placeholder="车架号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="form.brand" placeholder="如：大众" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号" prop="model">
              <el-input v-model="form.model" placeholder="如：朗逸" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="车型" prop="category">
              <el-select v-model="form.category" placeholder="选择车型" style="width: 100%;">
                <el-option label="经济型" value="economy" />
                <el-option label="紧凑型" value="compact" />
                <el-option label="中型" value="midsize" />
                <el-option label="SUV" value="suv" />
                <el-option label="豪华型" value="luxury" />
                <el-option label="MPV" value="minivan" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="座位数" prop="seats">
              <el-input-number v-model="form.seats" :min="2" :max="9" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="变速箱" prop="transmission">
              <el-select v-model="form.transmission" style="width: 100%;">
                <el-option label="自动挡" value="auto" />
                <el-option label="手动挡" value="manual" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="能源类型" prop="fuelType">
              <el-select v-model="form.fuelType" style="width: 100%;">
                <el-option label="汽油" value="gasoline" />
                <el-option label="柴油" value="diesel" />
                <el-option label="纯电" value="electric" />
                <el-option label="混动" value="hybrid" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="日租价" prop="dailyPrice">
              <el-input-number v-model="form.dailyPrice" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="周租价">
              <el-input-number v-model="form.weeklyPrice" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="押金">
              <el-input-number v-model="form.deposit" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签">
          <el-checkbox v-model="form.isHot">热门</el-checkbox>
          <el-checkbox v-model="form.isNew">新车</el-checkbox>
          <el-checkbox v-model="form.noDeposit">免押金</el-checkbox>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { Plus } from '@element-plus/icons-vue'

const vehicles = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const stats = ref({})

const filters = ref({
  status: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  plateNumber: '',
  vin: '',
  brand: '',
  model: '',
  category: '',
  seats: 5,
  transmission: 'auto',
  fuelType: 'gasoline',
  dailyPrice: 0,
  weeklyPrice: 0,
  deposit: 0,
  isHot: false,
  isNew: false,
  noDeposit: false,
  description: ''
})

const rules = {
  plateNumber: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  category: [{ required: true, message: '请选择车型', trigger: 'change' }],
  dailyPrice: [{ required: true, message: '请输入日租价', trigger: 'blur' }]
}

const getCategoryName = (cat) => {
  const map = { economy: '经济型', compact: '紧凑型', midsize: '中型', suv: 'SUV', luxury: '豪华型', minivan: 'MPV' }
  return map[cat] || cat
}

const getStatusName = (status) => {
  const map = { 0: '下架', 1: '可租', 2: '预订中', 3: '出租中', 4: '维修中', 5: '清洁中' }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'warning', 3: '', 4: 'danger', 5: 'info' }
  return map[status] || 'info'
}

const loadVehicles = async () => {
  loading.value = true
  try {
    const res = await api.admin.vehicles.list({
      page: page.value,
      pageSize: pageSize.value,
      ...filters.value
    })
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

const loadStats = async () => {
  try {
    const res = await api.admin.vehicles.stats()
    if (res.code === 200) {
      stats.value = res.data
    }
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null, plateNumber: '', vin: '', brand: '', model: '',
    category: '', seats: 5, transmission: 'auto', fuelType: 'gasoline',
    dailyPrice: 0, weeklyPrice: 0, deposit: 0,
    isHot: false, isNew: false, noDeposit: false, description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  saving.value = true
  try {
    const apiMethod = isEdit.value ? api.admin.vehicles.update : api.admin.vehicles.create
    const res = await apiMethod(form)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
      dialogVisible.value = false
      loadVehicles()
      loadStats()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleCommand = async (command, row) => {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定要删除此车辆吗？', '提示', { type: 'warning' })
      const res = await api.admin.vehicles.delete(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadVehicles()
        loadStats()
      }
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('删除失败')
    }
  } else {
    const statusMap = { available: 1, maintenance: 4, offline: 0 }
    const res = await api.admin.vehicles.updateStatus({ id: row.id, status: statusMap[command] })
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      loadVehicles()
      loadStats()
    } else {
      ElMessage.error(res.message)
    }
  }
}

onMounted(() => {
  loadVehicles()
  loadStats()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #303133;
}

.stats-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.stats-bar .stat-item {
  text-align: center;
  padding: 0 20px;
  border-right: 1px solid #ebeef5;
}

.stats-bar .stat-item:last-child {
  border-right: none;
}

.stats-bar .label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 5px;
}

.stats-bar .value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stats-bar .success { color: #67C23A; }
.stats-bar .warning { color: #E6A23C; }
.stats-bar .primary { color: #409EFF; }
.stats-bar .danger { color: #F56C6C; }

.filter-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
