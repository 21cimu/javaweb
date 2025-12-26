<template>
  <div class="admin-order-event-logs">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="订单号">
          <el-input v-model="filterForm.orderNo" placeholder="输入订单号" clearable />
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="filterForm.eventType" placeholder="全部事件" clearable style="width: 160px;">
            <el-option label="创建" value="created" />
            <el-option label="审核" value="reviewed" />
            <el-option label="支付" value="paid" />
            <el-option label="取车" value="pickup" />
            <el-option label="还车" value="return" />
            <el-option label="取消" value="cancel" />
            <el-option label="退款" value="refund" />
            <el-option label="完成" value="complete" />
            <el-option label="评价" value="review" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发人">
          <el-input v-model="filterForm.operator" placeholder="用户/管理员" clearable />
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
          <span>订单事件流</span>
        </div>
      </template>

      <el-table :data="logs" v-loading="loading" stripe empty-text="暂无数据">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="事件" width="160">
          <template #default="{ row }">
            {{ eventText(row.eventType) }}
          </template>
        </el-table-column>
        <el-table-column label="环节" width="120">
          <template #default="{ row }">
            {{ stageText(row.stage) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="触发人" width="120" />
        <el-table-column label="说明" min-width="220">
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
  orderNo: '',
  eventType: '',
  operator: '',
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
    if (filterForm.orderNo) params.orderNo = filterForm.orderNo
    if (filterForm.eventType) params.eventType = filterForm.eventType
    if (filterForm.operator) params.operator = filterForm.operator
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }
    const res = await api.admin.logs.orderEvents(params)
    if (res.code === 200) {
      logs.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load order event logs:', error)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.orderNo = ''
  filterForm.eventType = ''
  filterForm.operator = ''
  filterForm.dateRange = []
  pagination.page = 1
  loadLogs()
}

onMounted(() => {
  loadLogs()
})

const eventText = (value) => {
  const map = {
    created: '创建',
    reviewed: '审核',
    paid: '支付',
    pickup: '取车',
    return: '还车',
    cancel: '取消',
    refund: '退款',
    complete: '完成',
    review: '评价'
  }
  return map[value] || value || '-'
}

const stageText = (value) => {
  const map = {
    order: '下单',
    review: '审核',
    payment: '支付',
    fulfillment: '履约',
    settlement: '结算',
    service: '评价',
    'after-sales': '售后'
  }
  return map[value] || value || '-'
}

const messageText = (value) => {
  const map = {
    order_created: '订单创建',
    payment_success: '支付成功',
    alipay_paid: '支付宝支付',
    order_cancelled: '订单取消',
    order_reviewed: '完成评价',
    pickup_confirmed: '取车确认',
    return_confirmed: '还车确认',
    order_completed: '订单完成',
    order_refunded: '订单退款',
    after_sales_refund: '售后退款',
    approved: '审核通过',
    rejected: '审核拒绝'
  }
  if (!value) return '-'
  if (value.startsWith('rejected:')) {
    return `审核拒绝: ${value.slice('rejected:'.length)}`
  }
  return map[value] || value
}
</script>

<style scoped>
.admin-order-event-logs {
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
