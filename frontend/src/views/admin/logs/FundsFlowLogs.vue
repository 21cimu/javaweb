<template>
  <div class="admin-funds-flow-logs">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="关键字">
          <el-input v-model="filterForm.keyword" placeholder="流水号/订单号" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterForm.type" placeholder="全部类型" clearable style="width: 140px;">
            <el-option label="收入" value="income" />
            <el-option label="支出" value="expense" />
            <el-option label="退款" value="refund" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="filterForm.channel" placeholder="全部渠道" clearable style="width: 140px;">
            <el-option label="支付宝" value="alipay" />
            <el-option label="微信支付" value="wechat" />
            <el-option label="余额" value="balance" />
            <el-option label="线下" value="offline" />
            <el-option label="银行卡" value="bank" />
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
          <span>资金流水日志</span>
        </div>
      </template>

      <el-table :data="logs" v-loading="loading" stripe empty-text="暂无数据">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="flowNo" label="流水号" width="180" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            {{ typeText(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            ¥{{ row.amount != null ? row.amount : 0 }}
          </template>
        </el-table-column>
        <el-table-column label="渠道" width="120">
          <template #default="{ row }">
            {{ channelText(row.channel) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column label="备注" min-width="200">
          <template #default="{ row }">
            {{ remarkText(row.remark) }}
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
  keyword: '',
  type: '',
  channel: '',
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
    if (filterForm.keyword) params.keyword = filterForm.keyword
    if (filterForm.type) params.type = filterForm.type
    if (filterForm.channel) params.channel = filterForm.channel
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }
    const res = await api.admin.logs.fundsFlow(params)
    if (res.code === 200) {
      logs.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load funds flow logs:', error)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.keyword = ''
  filterForm.type = ''
  filterForm.channel = ''
  filterForm.dateRange = []
  pagination.page = 1
  loadLogs()
}

onMounted(() => {
  loadLogs()
})

const typeText = (value) => {
  const map = {
    income: '收入',
    expense: '支出',
    refund: '退款'
  }
  return map[value] || value || '-'
}

const channelText = (value) => {
  const map = {
    alipay: '支付宝',
    wechat: '微信支付',
    balance: '余额',
    offline: '线下',
    bank: '银行卡'
  }
  return map[value] || value || '-'
}

const remarkText = (value) => {
  const map = {
    order_payment: '订单支付',
    alipay_payment: '支付宝支付',
    deposit_refund: '押金退还',
    manual_refund: '人工退款',
    after_sales_refund: '售后退款'
  }
  return map[value] || value || '-'
}
</script>

<style scoped>
.admin-funds-flow-logs {
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
