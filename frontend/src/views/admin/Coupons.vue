<template>
  <div class="admin-coupons">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="优惠券名称">
          <el-input v-model="filterForm.name" placeholder="输入名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadCoupons">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>优惠券管理</span>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon> 新增优惠券
          </el-button>
        </div>
      </template>

      <el-table :data="coupons" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="优惠码" width="120" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ getCouponType(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠" width="120">
          <template #default="{ row }">
            <span v-if="row.type === 1 || row.type === 3">¥{{ row.discountAmount }}</span>
            <span v-else-if="row.type === 2">{{ (row.discountRate * 100).toFixed(0) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="minAmount" label="满额条件" width="100">
          <template #default="{ row }">
            ¥{{ row.minAmount }}
          </template>
        </el-table-column>
        <el-table-column label="领取/使用" width="120">
          <template #default="{ row }">
            {{ row.usedCount || 0 }}/{{ row.totalCount || '无限' }}
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="200">
          <template #default="{ row }">
            <div class="date-range">
              <div>{{ formatDate(row.startTime) }}</div>
              <div>至 {{ formatDate(row.endTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="editCoupon(row)">编辑</el-button>
            <el-button v-if="row.status === 1" link type="warning" @click="disableCoupon(row)">禁用</el-button>
            <el-button v-else link type="success" @click="enableCoupon(row)">启用</el-button>
            <el-button link type="danger" @click="deleteCoupon(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadCoupons"
        @current-change="loadCoupons"
        class="pagination"
      />
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑优惠券' : '新增优惠券'" width="600px">
      <el-form :model="couponForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="优惠码" prop="code">
          <el-input v-model="couponForm.code" placeholder="输入优惠码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="couponForm.name" placeholder="输入优惠券名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="couponForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="couponForm.type">
            <el-radio :value="1">满减</el-radio>
            <el-radio :value="2">折扣</el-radio>
            <el-radio :value="3">固定金额</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="满额条件" prop="minAmount">
          <el-input-number v-model="couponForm.minAmount" :min="0" :precision="2" />
          <span class="form-tip">满此金额可用</span>
        </el-form-item>
        <el-form-item v-if="couponForm.type === 1 || couponForm.type === 3" label="减免金额" prop="discountAmount">
          <el-input-number v-model="couponForm.discountAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item v-if="couponForm.type === 2" label="折扣比例" prop="discountRate">
          <el-slider v-model="couponForm.discountRatePercent" :min="1" :max="99" show-input />
          <span class="form-tip">{{ couponForm.discountRatePercent }}% 折扣</span>
        </el-form-item>
        <el-form-item v-if="couponForm.type === 2" label="最大优惠">
          <el-input-number v-model="couponForm.maxDiscount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="发放数量">
          <el-input-number v-model="couponForm.totalCount" :min="0" />
          <span class="form-tip">0表示不限量</span>
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="couponForm.perUserLimit" :min="1" />
        </el-form-item>
        <el-form-item label="有效期" prop="dateRange">
          <el-date-picker
            v-model="couponForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCoupon">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const loading = ref(false)
const coupons = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const filterForm = reactive({
  name: '',
  status: null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const couponForm = reactive({
  id: null,
  code: '',
  name: '',
  description: '',
  type: 1,
  minAmount: 0,
  discountAmount: 0,
  discountRatePercent: 80,
  maxDiscount: 0,
  totalCount: 0,
  perUserLimit: 1,
  dateRange: []
})

const rules = {
  code: [{ required: true, message: '请输入优惠码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择有效期', trigger: 'change' }]
}

const getCouponType = (type) => {
  const map = { 1: '满减', 2: '折扣', 3: '固定金额' }
  return map[type] || '未知'
}

const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'

const loadCoupons = () => {
  loading.value = true
  // Mock data
  coupons.value = [
    { id: 1, code: 'NEW50', name: '新用户专享50元券', description: '新用户首单立减50元', type: 1, minAmount: 200, discountAmount: 50, totalCount: 10000, usedCount: 100, perUserLimit: 1, startTime: '2024-01-01 00:00:00', endTime: '2024-12-31 23:59:59', status: 1 },
    { id: 2, code: 'WEEKEND20', name: '周末8折券', description: '周末租车享8折优惠', type: 2, minAmount: 100, discountRate: 0.8, maxDiscount: 200, totalCount: 5000, usedCount: 50, perUserLimit: 2, startTime: '2024-01-01 00:00:00', endTime: '2024-12-31 23:59:59', status: 1 },
    { id: 3, code: 'VIP100', name: 'VIP专享100元券', type: 3, minAmount: 500, discountAmount: 100, totalCount: 1000, usedCount: 10, perUserLimit: 1, startTime: '2024-01-01 00:00:00', endTime: '2024-12-31 23:59:59', status: 1 }
  ]
  pagination.total = 3
  loading.value = false
}

const resetFilter = () => {
  filterForm.name = ''
  filterForm.status = null
  pagination.page = 1
  loadCoupons()
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(couponForm, {
    id: null,
    code: '',
    name: '',
    description: '',
    type: 1,
    minAmount: 0,
    discountAmount: 0,
    discountRatePercent: 80,
    maxDiscount: 0,
    totalCount: 0,
    perUserLimit: 1,
    dateRange: []
  })
  dialogVisible.value = true
}

const editCoupon = (coupon) => {
  isEdit.value = true
  Object.assign(couponForm, {
    id: coupon.id,
    code: coupon.code,
    name: coupon.name,
    description: coupon.description,
    type: coupon.type,
    minAmount: coupon.minAmount,
    discountAmount: coupon.discountAmount || 0,
    discountRatePercent: coupon.discountRate ? coupon.discountRate * 100 : 80,
    maxDiscount: coupon.maxDiscount || 0,
    totalCount: coupon.totalCount || 0,
    perUserLimit: coupon.perUserLimit || 1,
    dateRange: [coupon.startTime, coupon.endTime]
  })
  dialogVisible.value = true
}

const saveCoupon = async () => {
  try {
    await formRef.value.validate()
    // Call API to save
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadCoupons()
  } catch (error) {
    console.error('Validation failed:', error)
  }
}

const disableCoupon = async (coupon) => {
  try {
    await ElMessageBox.confirm('确认禁用该优惠券?', '确认')
    ElMessage.success('已禁用')
    loadCoupons()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed:', error)
  }
}

const enableCoupon = async (coupon) => {
  try {
    await ElMessageBox.confirm('确认启用该优惠券?', '确认')
    ElMessage.success('已启用')
    loadCoupons()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed:', error)
  }
}

const deleteCoupon = async (coupon) => {
  try {
    await ElMessageBox.confirm('确认删除该优惠券?', '确认')
    ElMessage.success('已删除')
    loadCoupons()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed:', error)
  }
}

onMounted(() => {
  loadCoupons()
})
</script>

<style scoped>
.admin-coupons {
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

.date-range {
  font-size: 12px;
  line-height: 1.6;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
