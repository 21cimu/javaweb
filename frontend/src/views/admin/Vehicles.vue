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
        <!-- Thumbnail column -->
        <el-table-column label="图片" width="120">
          <template #default="{ row }">
            <el-image
              :src="getImageUrl(row)"
              fit="cover"
              style="width: 100px; height: 60px; border-radius: 4px;"
            />
          </template>
        </el-table-column>

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
        <el-table-column prop="lastMaintenanceDate" label="最近保养日期" width="140">
          <template #default="{ row }">
            {{ row.lastMaintenanceDate || '-' }}
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

        <!-- Images upload -->
        <el-form-item label="车辆图片">
          <div class="images-upload-wrapper">
            <div class="preview-list">
              <div v-for="(item, idx) in uploadedImages" :key="idx" class="preview-item">
                <img :src="item.previewUrl" alt="vehicle" />
                <div class="preview-actions">
                  <el-button type="text" size="small" @click="removeImage(idx)">移除</el-button>
                </div>
              </div>
            </div>

            <el-upload
              class="upload-demo"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :multiple="true"
              :on-change="handleSelectImage"
            >
              <el-button size="small" type="primary"><el-icon><Plus /></el-icon> 选择图片</el-button>
            </el-upload>
            <div class="hint">建议上传多张车辆照片（长宽比例 16:9 或 4:3），最多 8 张。</div>
          </div>
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
  lastMaintenanceDate: null,
  isHot: false,
  isNew: false,
  noDeposit: false,
  description: '',
  images: [] // store server URLs
})

// uploadedImages stores preview objects: { file: File|null, previewUrl: string, isServer?: boolean }
const uploadedImages = ref([])

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

// image resolver for admin list (prefer mainImage, then images[0], else placeholder)
const getImageUrl = (row) => {
  if (!row) return '/images/car-placeholder.svg'
  if (row.mainImage) return normalizeImgUrl(row.mainImage)
  if (row.main_image) return normalizeImgUrl(row.main_image)
  if (Array.isArray(row.images) && row.images.length > 0) return normalizeImgUrl(row.images[0])
  if (row.images && typeof row.images === 'string') {
    try {
      const parsed = JSON.parse(row.images)
      if (Array.isArray(parsed) && parsed.length > 0) return normalizeImgUrl(parsed[0])
    } catch (e) {}
  }
  return '/images/car-placeholder.svg'
}

const normalizeImgUrl = (url) => {
  if (!url) return '/images/car-placeholder.svg'
  const s = String(url).trim()
  if (!s) return '/images/car-placeholder.svg'
  if (s.startsWith('http://') || s.startsWith('https://') || s.startsWith('/')) return s
  if (s.startsWith('images/') || s.startsWith('vehicles/') || s.startsWith('message/')) return '/images/' + s.replace(/^images\//, '')
  if (s.startsWith('uploads/')) return '/' + s
  return '/images/' + s
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
    dailyPrice: 0, weeklyPrice: 0, deposit: 0, lastMaintenanceDate: null,
    isHot: false, isNew: false, noDeposit: false, description: '', images: []
  })
  // clear previews
  uploadedImages.value.forEach(item => { if (item && item.previewUrl && item.file) try { URL.revokeObjectURL(item.previewUrl) } catch (e) {} })
  uploadedImages.value = []
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  // prepare uploadedImages previews: keep server images as is (file=null, previewUrl=url, isServer=true)
  uploadedImages.value.forEach(item => { if (item && item.previewUrl && item.file) try { URL.revokeObjectURL(item.previewUrl) } catch (e) {} })
  uploadedImages.value = []
  if (Array.isArray(row.images)) {
    uploadedImages.value = row.images.map(url => ({ file: null, previewUrl: url, isServer: true }))
  }
  dialogVisible.value = true
}

// Called when user selects image(s) via el-upload
const handleSelectImage = (file) => {
  try {
    // el-upload passes file and sometimes fileList; the actual File is at file.raw or file.file
    let actualFile = null
    if (!file) return
    if (file.raw && file.raw instanceof File) actualFile = file.raw
    else if (file.file && file.file instanceof File) actualFile = file.file
    else if (file instanceof File) actualFile = file
    else if (file.raw && file.raw.file && file.raw.file instanceof File) actualFile = file.raw.file

    if (!actualFile) {
      ElMessage.error('选择的文件格式不受支持')
      return
    }

    if (uploadedImages.value.length >= 8) {
      ElMessage.warning('最多上传 8 张图片')
      return
    }

    const objectUrl = URL.createObjectURL(actualFile)
    uploadedImages.value.push({ file: actualFile, previewUrl: objectUrl })
  } catch (err) {
    console.error('handleSelectImage error', err)
    ElMessage.error('选择图片失败')
  }
}

const removeImage = (index) => {
  const entry = uploadedImages.value[index]
  if (!entry) return
  try {
    if (entry.file && entry.previewUrl) URL.revokeObjectURL(entry.previewUrl)
  } catch (e) {}
  uploadedImages.value.splice(index, 1)
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    // First upload any local files in uploadedImages (those with entry.file != null)
    const uploadEntries = uploadedImages.value
    const uploadPromises = uploadEntries.map(async (entry) => {
      if (entry.file) {
        const formData = new FormData()
        formData.append('file', entry.file)
        // tell the backend to save into images/vehicles
        formData.append('folder', 'vehicles')
        // include plate number so backend can use it as filename base
        if (form.plateNumber) formData.append('plateNumber', form.plateNumber)
        const res = await api.upload.file(formData)
        if (res && res.code === 200) {
          const data = res.data
          const url = (data && data.url) ? data.url : data
          return url
        } else {
          throw new Error(res?.message || '上传文件失败')
        }
      }
      // entry from server (isServer) already has previewUrl as server url
      if (entry.isServer && entry.previewUrl) return entry.previewUrl
      return null
    })

    const uploadedUrls = (await Promise.all(uploadPromises)).filter(u => !!u)
    // Set form.images to uploadedUrls (this includes prior server URLs and newly uploaded ones)
    form.images = uploadedUrls

    const apiMethod = isEdit.value ? api.admin.vehicles.update : api.admin.vehicles.create
    const payload = { ...form }
    // ensure mainImage is set to the first image if available
    if (!payload.mainImage) {
      if (Array.isArray(payload.images) && payload.images.length > 0) {
        payload.mainImage = payload.images[0]
      } else {
        payload.mainImage = null
      }
    }
    const res = await apiMethod(payload)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
      dialogVisible.value = false
      loadVehicles()
      loadStats()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    console.error('save vehicle error', error)
    ElMessage.error(error?.message || '保存失败')
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
        // remove the deleted vehicle from local list to reflect the change immediately
        vehicles.value = vehicles.value.filter(v => v.id !== row.id)
        total.value = Math.max(0, total.value - 1)
        loadStats()
      } else {
        ElMessage.error(res.message)
      }
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('删除失败')
    }
  } else {
    const statusMap = { available: 1, maintenance: 4, offline: 0 }
    const status = statusMap[command]
    if (typeof status === 'undefined') return

    try {
      const res = await api.admin.vehicles.updateStatus({ id: row.id, status })
      if (res.code === 200) {
        ElMessage.success('状态更新成功')
        loadVehicles()
        loadStats()
      } else {
        ElMessage.error(res.message)
      }
    } catch (err) {
      console.error('update status failed', err)
      ElMessage.error('操作失败')
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

.stats-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.filter-card {
  margin-bottom: 20px;
}

.images-upload-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}
.preview-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.preview-item {
  width: 120px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
  border: 1px solid #e6e6e6;
}
.preview-item img { width: 100%; height: 100%; object-fit: cover; }
.preview-actions { position: absolute; top: 4px; right: 4px; }
.hint { color: #909399; font-size: 12px; margin-top: 6px; }

.upload-demo {
  display: inline-block;
}
</style>
