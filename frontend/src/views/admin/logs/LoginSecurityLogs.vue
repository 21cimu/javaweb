<template>
  <div class="admin-login-security-logs">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="账号">
          <el-input v-model="filterForm.account" placeholder="用户名/手机号/邮箱" clearable />
        </el-form-item>
        <el-form-item label="结果">
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
          <span>登录安全日志</span>
        </div>
      </template>

      <el-table :data="logs" v-loading="loading" stripe empty-text="暂无数据">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="account" label="账号" width="140" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column label="地区" width="120">
          <template #default="{ row }">
            {{ row.location || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="device" label="设备/浏览器" min-width="200" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === 'success' ? 'success' : 'danger'">
              {{ resultText(row.result) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="200">
          <template #default="{ row }">
            {{ messageText(row.message) }}
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
  account: '',
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
    if (filterForm.account) params.account = filterForm.account
    if (filterForm.result) params.result = filterForm.result
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }
    const res = await api.admin.logs.loginSecurity(params)
    if (res.code === 200) {
      logs.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load login security logs:', error)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.account = ''
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

const messageText = (value) => {
  const map = {
    login_success: '登录成功',
    invalid_password: '密码错误',
    user_not_found: '用户不存在',
    account_disabled: '账号已禁用',
    missing_credentials: '缺少账号或密码'
  }
  return map[value] || value || '-'
}
</script>

<style scoped>
.admin-login-security-logs {
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
