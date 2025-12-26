<template>
  <div class="admin-after-sales">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="售后单号/订单号">
          <el-input v-model="filterForm.orderNo" placeholder="输入售后单号或订单号" clearable />
        </el-form-item>
        <el-form-item label="售后状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 150px;">
            <el-option label="待审核" :value="1" />
            <el-option label="已同意" :value="2" />
            <el-option label="已拒绝" :value="3" />
            <el-option label="已完成" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
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
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>售后列表</span>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="afterNo" label="售后单号" width="180" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            {{ typeText(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="申请金额" width="120">
          <template #default="{ row }">
            <span v-if="row.type === 2">¥{{ row.refundAmount }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" link type="success" @click="openAudit(row, 'approve')">同意</el-button>
            <el-button v-if="row.status === 1" link type="danger" @click="openAudit(row, 'reject')">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadList"
        @current-change="loadList"
        class="pagination"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="售后详情" width="700px">
      <el-descriptions v-if="current" :column="2" border>
        <el-descriptions-item label="售后单号">{{ current.afterNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ current.userName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ current.userPhone }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeText(current.type) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(current.status)">{{ statusText(current.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请金额" v-if="current.type === 2">¥{{ current.refundAmount }}</el-descriptions-item>
        <el-descriptions-item label="批准金额" v-if="current.type === 2">¥{{ current.approvedRefundAmount }}</el-descriptions-item>
        <el-descriptions-item label="期望处理">{{ current.expectedSolution }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatDate(current.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="current" style="margin-top: 16px;">
        <h4>问题描述</h4>
        <p>{{ current.description }}</p>
        <h4 v-if="current.auditRemark" style="margin-top: 12px;">审核备注</h4>
        <p v-if="current.auditRemark">{{ current.auditRemark }}</p>
      </div>
    </el-dialog>

    <el-dialog v-model="auditVisible" title="售后审核" width="500px">
      <el-form :model="auditForm" label-width="100px">
        <el-form-item label="审核结果">
          <el-tag v-if="auditForm.decision === 'approve'" type="success">同意</el-tag>
          <el-tag v-else type="danger">拒绝</el-tag>
        </el-form-item>
        <el-form-item v-if="current && current.type === 2 && auditForm.decision === 'approve'" label="批准退款金额">
          <el-input-number v-model="auditForm.approvedRefundAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="submitAudit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import dayjs from 'dayjs'

const loading = ref(false)
const list = ref([])
const current = ref(null)
const detailVisible = ref(false)
const auditVisible = ref(false)
const auditLoading = ref(false)

const filterForm = reactive({
  orderNo: '',
  status: null,
  dateRange: []
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const auditForm = reactive({
  id: null,
  decision: 'approve',
  approvedRefundAmount: null,
  remark: ''
})

const statusText = (s) => {
  const map = {
    0: '已关闭',
    1: '待审核',
    2: '已同意',
    3: '已拒绝',
    4: '已完成',
    5: '退款中'
  }
  return map[s] || '未知'
}

const statusType = (s) => {
  const map = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger',
    4: 'success',
    5: 'info'
  }
  return map[s] || 'info'
}

const typeText = (t) => {
  const map = {
    1: '报修',
    2: '退款',
    3: '投诉',
    4: '其他'
  }
  return map[t] || '未知'
}

const formatDate = (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-')

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (filterForm.orderNo) params.orderNo = filterForm.orderNo
    if (filterForm.status != null) params.status = filterForm.status
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }

    const res = await api.admin.afterSales.list(params)
    if (res.code === 200) {
      list.value = res.data.list
      pagination.total = res.data.total
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.orderNo = ''
  filterForm.status = null
  filterForm.dateRange = []
  pagination.page = 1
  loadList()
}

const viewDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

const openAudit = (row, decision) => {
  current.value = row
  auditForm.id = row.id
  auditForm.decision = decision
  auditForm.approvedRefundAmount = row.refundAmount
  auditForm.remark = ''
  auditVisible.value = true
}

const submitAudit = async () => {
  if (!auditForm.id) return
  auditLoading.value = true
  try {
    const payload = {
      id: auditForm.id,
      decision: auditForm.decision,
      approvedRefundAmount: auditForm.approvedRefundAmount,
      remark: auditForm.remark
    }
    const res = await api.admin.afterSales.audit(payload)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      auditVisible.value = false
      loadList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('操作失败')
  } finally {
    auditLoading.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.admin-after-sales {
  padding: 20px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-card {
  margin-top: 12px;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>

