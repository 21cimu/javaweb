<template>
  <div class="admin-operation-audit-logs">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="操作者">
          <el-input v-model="filterForm.operator" placeholder="姓名/账号" clearable />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="filterForm.module" placeholder="全部模块" clearable style="width: 140px;">
            <el-option label="订单管理" value="orders" />
            <el-option label="车辆管理" value="vehicles" />
            <el-option label="用户管理" value="users" />
            <el-option label="营销管理" value="marketing" />
            <el-option label="售后管理" value="after-sales" />
            <el-option label="门店管理" value="stores" />
            <el-option label="评论管理" value="reviews" />
            <el-option label="仪表盘" value="dashboard" />
            <el-option label="汽车保养" value="maintenance" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作结果">
          <el-select v-model="filterForm.result" placeholder="全部结果" clearable style="width: 140px;">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="fail" />
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
          <span>后台操作审计日志</span>
        </div>
      </template>

      <el-table :data="logs" v-loading="loading" stripe empty-text="暂无数据">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="operatorName" label="操作者" width="120" />
        <el-table-column label="模块" width="120">
          <template #default="{ row }">
            {{ moduleText(row.module) }}
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="180" />
        <el-table-column label="对象" min-width="180">
          <template #default="{ row }">
            {{ row.target || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === 'success' ? 'success' : 'danger'">
              {{ resultText(row.result) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="140" />
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
  operator: '',
  module: '',
  result: '',
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
    if (filterForm.operator) params.operator = filterForm.operator
    if (filterForm.module) params.module = filterForm.module
    if (filterForm.result) params.result = filterForm.result
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }
    const res = await api.admin.logs.operationAudit(params)
    if (res.code === 200) {
      logs.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load operation audit logs:', error)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.operator = ''
  filterForm.module = ''
  filterForm.result = ''
  filterForm.dateRange = []
  pagination.page = 1
  loadLogs()
}

onMounted(() => {
  loadLogs()
})

const resultText = (value) => {
  if (value === 'success') return '成功'
  if (value === 'fail') return '失败'
  return value || '-'
}

const moduleText = (value) => {
  const map = {
    orders: '订单管理',
    vehicles: '车辆管理',
    users: '用户管理',
    marketing: '营销管理',
    'after-sales': '售后管理',
    stores: '门店管理',
    reviews: '评论管理',
    dashboard: '仪表盘',
    maintenance: '汽车保养',
    admin: '后台管理'
  }
  return map[value] || value || '-'
}
</script>

<style scoped>
.admin-operation-audit-logs {
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
