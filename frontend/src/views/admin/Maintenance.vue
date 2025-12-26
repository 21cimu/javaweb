<template>
  <div class="admin-maintenance-page">
    <div class="page-header">
      <h1>汽车保养</h1>
      <div class="header-actions">
        <el-button type="primary" @click="loadVehicles">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="车辆状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 120px;">
            <el-option label="可租" :value="1" />
            <el-option label="预订中" :value="2" />
            <el-option label="出租中" :value="3" />
            <el-option label="维修中" :value="4" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="保养状态">
          <el-select v-model="filters.maintenanceState" placeholder="全部" clearable style="width: 120px;">
            <el-option label="正常" value="normal" />
            <el-option label="临近" value="due" />
            <el-option label="逾期" value="overdue" />
            <el-option label="未登记" value="none" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="车牌/品牌/车型"
            clearable
            style="width: 180px;"
          />
        </el-form-item>
        <el-form-item label="保养周期">
          <el-input-number v-model="cycleDays" :min="30" :max="365" />
          <span class="unit">天</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="stats-bar">
      <div class="stat-item">
        <span class="label">车辆数</span>
        <span class="value">{{ stats.total }}</span>
      </div>
      <div class="stat-item">
        <span class="label">临近保养</span>
        <span class="value warning">{{ stats.due }}</span>
      </div>
      <div class="stat-item">
        <span class="label">已逾期</span>
        <span class="value danger">{{ stats.overdue }}</span>
      </div>
      <div class="stat-item">
        <span class="label">未登记</span>
        <span class="value info">{{ stats.none }}</span>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="pagedVehicles" v-loading="loading" stripe row-key="id">
        <el-table-column prop="plateNumber" label="车牌号" width="120" />
        <el-table-column label="车辆">
          <template #default="{ row }">
            {{ row.brand }} {{ row.model }}
          </template>
        </el-table-column>
        <el-table-column prop="mileage" label="里程" width="100">
          <template #default="{ row }">
            {{ row.mileage ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="lastMaintenanceDate" label="最近保养日期" width="140">
          <template #default="{ row }">
            {{ formatDate(row.lastMaintenanceDate) }}
          </template>
        </el-table-column>
        <el-table-column label="距今(天)" width="100">
          <template #default="{ row }">
            {{ getDaysSince(row.lastMaintenanceDate) }}
          </template>
        </el-table-column>
        <el-table-column label="下次保养" width="140">
          <template #default="{ row }">
            {{ getNextMaintenanceDate(row.lastMaintenanceDate) }}
          </template>
        </el-table-column>
        <el-table-column label="保养状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getMaintenanceTagType(row)" size="small">
              {{ getMaintenanceLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openRecord(row)">
              登记保养
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="登记保养" width="420px">
      <el-form label-width="100px">
        <el-form-item label="车辆">
          <span>{{ currentVehicleLabel }}</span>
        </el-form-item>
        <el-form-item label="保养日期" required>
          <el-date-picker
            v-model="maintenanceDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMaintenance">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import api from '@/api'

const vehicles = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = ref(10)
const cycleDays = ref(180)
const warnDays = 30

const filters = reactive({
  status: null,
  maintenanceState: '',
  dateRange: [],
  keyword: ''
})

const dialogVisible = ref(false)
const currentVehicle = ref(null)
const maintenanceDate = ref('')

const currentVehicleLabel = computed(() => {
  if (!currentVehicle.value) return '-'
  const v = currentVehicle.value
  return `${v.brand || ''} ${v.model || ''} (${v.plateNumber || '-'})`
})

const formatDate = (value) => {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD')
}

const getDaysSince = (value) => {
  if (!value) return '-'
  const days = dayjs().startOf('day').diff(dayjs(value), 'day')
  return days < 0 ? 0 : days
}

const getNextMaintenanceDate = (value) => {
  if (!value) return '-'
  return dayjs(value).add(cycleDays.value, 'day').format('YYYY-MM-DD')
}

const getMaintenanceState = (row) => {
  if (!row || !row.lastMaintenanceDate) return 'none'
  const days = dayjs().startOf('day').diff(dayjs(row.lastMaintenanceDate), 'day')
  if (days >= cycleDays.value) return 'overdue'
  const warnLine = Math.max(0, cycleDays.value - warnDays)
  if (days >= warnLine) return 'due'
  return 'normal'
}

const getMaintenanceLabel = (row) => {
  const map = {
    normal: '正常',
    due: '临近',
    overdue: '逾期',
    none: '未登记'
  }
  return map[getMaintenanceState(row)] || '未知'
}

const getMaintenanceTagType = (row) => {
  const map = {
    normal: 'success',
    due: 'warning',
    overdue: 'danger',
    none: 'info'
  }
  return map[getMaintenanceState(row)] || ''
}

const filteredVehicles = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  const hasRange = Array.isArray(filters.dateRange) && filters.dateRange.length === 2
  const start = hasRange ? dayjs(filters.dateRange[0]) : null
  const end = hasRange ? dayjs(filters.dateRange[1]) : null

  return vehicles.value.filter((v) => {
    if (keyword) {
      const target = `${v.plateNumber || ''} ${v.brand || ''} ${v.model || ''} ${v.series || ''}`.toLowerCase()
      if (!target.includes(keyword)) return false
    }

    if (filters.maintenanceState) {
      if (getMaintenanceState(v) !== filters.maintenanceState) return false
    }

    if (hasRange) {
      if (!v.lastMaintenanceDate) return false
      const date = dayjs(v.lastMaintenanceDate)
      if (date.isBefore(start, 'day') || date.isAfter(end, 'day')) return false
    }

    return true
  })
})

const total = computed(() => filteredVehicles.value.length)

const pagedVehicles = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredVehicles.value.slice(start, start + pageSize.value)
})

const stats = computed(() => {
  const summary = { total: 0, due: 0, overdue: 0, none: 0 }
  summary.total = filteredVehicles.value.length
  filteredVehicles.value.forEach((v) => {
    const state = getMaintenanceState(v)
    if (state === 'due') summary.due += 1
    else if (state === 'overdue') summary.overdue += 1
    else if (state === 'none') summary.none += 1
  })
  return summary
})

const loadVehicles = async () => {
  loading.value = true
  try {
    const pageSizeFetch = 200
    let currentPage = 1
    let totalCount = 0
    const all = []
    while (true) {
      const params = { page: currentPage, pageSize: pageSizeFetch }
      if (filters.status !== null && filters.status !== '') params.status = filters.status
      const res = await api.admin.vehicles.list(params)
      if (!res || res.code !== 200) break
      const list = res.data.list || []
      all.push(...list)
      totalCount = res.data.total || all.length
      if (all.length >= totalCount || list.length === 0) break
      currentPage += 1
      if (currentPage > 50) break
    }
    vehicles.value = all
  } catch (error) {
    console.error('Failed to load vehicles:', error)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.status = null
  filters.maintenanceState = ''
  filters.dateRange = []
  filters.keyword = ''
  page.value = 1
  loadVehicles()
}

const handlePageChange = (p) => {
  page.value = p
}

const handlePageSizeChange = () => {
  page.value = 1
}

const openRecord = (row) => {
  currentVehicle.value = row
  maintenanceDate.value = row.lastMaintenanceDate || dayjs().format('YYYY-MM-DD')
  dialogVisible.value = true
}

const saveMaintenance = async () => {
  if (!currentVehicle.value) return
  if (!maintenanceDate.value) {
    ElMessage.warning('请选择保养日期')
    return
  }

  saving.value = true
  try {
    const res = await api.admin.vehicles.update({
      id: currentVehicle.value.id,
      lastMaintenanceDate: maintenanceDate.value
    })
    if (res && res.code === 200) {
      const target = vehicles.value.find(v => v.id === currentVehicle.value.id)
      if (target) target.lastMaintenanceDate = maintenanceDate.value
      dialogVisible.value = false
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res?.message || '保存失败')
    }
  } catch (error) {
    console.error('save maintenance error', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

watch(() => filters.status, () => {
  page.value = 1
  loadVehicles()
})

watch([() => filters.keyword, () => filters.maintenanceState, () => filters.dateRange, cycleDays], () => {
  page.value = 1
})

watch(total, (value) => {
  const maxPage = Math.max(1, Math.ceil(value / pageSize.value))
  if (page.value > maxPage) page.value = maxPage
})

onMounted(() => {
  loadVehicles()
})
</script>

<style scoped>
.admin-maintenance-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 16px;
}

.stats-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-item .label {
  color: #909399;
  font-size: 12px;
}

.stat-item .value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.stat-item .value.warning {
  color: #e6a23c;
}

.stat-item .value.danger {
  color: #f56c6c;
}

.stat-item .value.info {
  color: #909399;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.unit {
  margin-left: 6px;
  color: #909399;
}
</style>
