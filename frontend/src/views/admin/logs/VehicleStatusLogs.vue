<template>
  <div class="admin-vehicle-status-logs">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="车辆">
          <el-input v-model="filterForm.vehicle" placeholder="车牌/车型/编号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 140px;">
            <el-option label="可租" :value="1" />
            <el-option label="预订中" :value="2" />
            <el-option label="出租中" :value="3" />
            <el-option label="维修中" :value="4" />
            <el-option label="清洁中" :value="5" />
            <el-option label="事故" :value="6" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>车辆状态变更日志</span>
        </div>
      </template>

      <el-table :data="logs" v-loading="loading" stripe empty-text="暂无数据">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="vehicleName" label="车辆" width="180" />
        <el-table-column prop="plateNumber" label="车牌" width="120" />
        <el-table-column label="原状态" width="120">
          <template #default="{ row }">
            {{ statusText(row.fromStatus) }}
          </template>
        </el-table-column>
        <el-table-column label="新状态" width="120">
          <template #default="{ row }">
            {{ statusText(row.toStatus) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column label="备注" min-width="200">
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadLogs"
        @current-change="loadLogs"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import api from '@/api'

const loading = ref(false)
const logs = ref([])

const filterForm = reactive({
  vehicle: '',
  status: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (filterForm.vehicle) params.vehicle = filterForm.vehicle
    if (filterForm.status !== '' && filterForm.status !== null) params.status = filterForm.status
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }
    const res = await api.admin.logs.vehicleStatus(params)
    if (res.code === 200) {
      logs.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load vehicle status logs:', error)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.vehicle = ''
  filterForm.status = ''
  filterForm.dateRange = []
  pagination.page = 1
  loadLogs()
}

onMounted(() => {
  loadLogs()
})

const statusText = (value) => {
  const map = {
    0: '下架',
    1: '可租',
    2: '预订中',
    3: '出租中',
    4: '维修中',
    5: '清洁中',
    6: '事故'
  }
  return map[value] || value || '-'
}
</script>

<style scoped>
.admin-vehicle-status-logs {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
