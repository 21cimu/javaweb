<template>
  <div class="admin-orders">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="订单号">
          <el-input v-model="filterForm.orderNo" placeholder="输入订单号" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 150px;">
            <el-option label="待审核" :value="1" />
            <el-option label="审核失败" :value="2" />
            <el-option label="待支付" :value="3" />
            <el-option label="待取车" :value="4" />
            <el-option label="用车中" :value="5" />
            <el-option label="待还车" :value="6" />
            <el-option label="待结算" :value="7" />
            <el-option label="已完成" :value="8" />
            <el-option label="已取消" :value="0" />
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
          <el-button type="primary" @click="loadOrders">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>订单列表</span>
          <el-button type="primary" @click="exportOrders">
            <el-icon><Download /></el-icon> 导出
          </el-button>
        </div>
      </template>

      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userName" label="用户" width="100" />
        <el-table-column prop="vehicleName" label="车辆" width="150" />
        <el-table-column prop="vehiclePlate" label="车牌" width="100" />
        <el-table-column label="取还车时间" width="200">
          <template #default="{ row }">
            <div class="time-info">
              <div>取: {{ formatDate(row.pickupTime) }}</div>
              <div>还: {{ formatDate(row.returnTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewOrder(row)">详情</el-button>
            <el-button v-if="row.status === 1" link type="success" @click="approveOrder(row)">通过</el-button>
            <el-button v-if="row.status === 1" link type="danger" @click="rejectOrder(row)">拒绝</el-button>
            <el-button v-if="row.status === 4" link type="primary" @click="handlePickup(row)">取车</el-button>
            <el-button v-if="row.status === 5 || row.status === 6" link type="primary" @click="handleReturn(row)">还车</el-button>
            <el-button v-if="row.status === 7" link type="success" @click="completeOrder(row)">完成</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadOrders"
        @current-change="loadOrders"
        class="pagination"
      />
    </el-card>

    <!-- Order Detail Dialog -->
    <el-dialog v-model="detailVisible" title="订单详情" width="800px">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentOrder.status)">{{ getStatusText(currentOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentOrder.userName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentOrder.userPhone }}</el-descriptions-item>
          <el-descriptions-item label="车辆">{{ currentOrder.vehicleName }}</el-descriptions-item>
          <el-descriptions-item label="车牌">{{ currentOrder.vehiclePlate }}</el-descriptions-item>
          <el-descriptions-item label="取车门店">{{ currentOrder.pickupStoreName }}</el-descriptions-item>
          <el-descriptions-item label="还车门店">{{ currentOrder.returnStoreName }}</el-descriptions-item>
          <el-descriptions-item label="取车时间">{{ formatDate(currentOrder.pickupTime) }}</el-descriptions-item>
          <el-descriptions-item label="还车时间">{{ formatDate(currentOrder.returnTime) }}</el-descriptions-item>
          <el-descriptions-item label="租期天数">{{ currentOrder.rentalDays }}天</el-descriptions-item>
          <el-descriptions-item label="日租金">¥{{ currentOrder.dailyPrice }}</el-descriptions-item>
          <el-descriptions-item label="租金金额">¥{{ currentOrder.rentalAmount }}</el-descriptions-item>
          <el-descriptions-item label="押金">¥{{ currentOrder.deposit }}</el-descriptions-item>
          <el-descriptions-item label="保险费">¥{{ currentOrder.insuranceAmount }}</el-descriptions-item>
          <el-descriptions-item label="服务费">¥{{ currentOrder.serviceAmount }}</el-descriptions-item>
          <el-descriptions-item label="优惠金额">-¥{{ currentOrder.discountAmount }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="取车码">{{ currentOrder.pickupCode }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(currentOrder.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- Pickup Dialog -->
    <el-dialog v-model="pickupVisible" title="取车验车" width="500px">
      <el-form :model="pickupForm" label-width="100px">
        <el-form-item label="当前里程">
          <el-input-number v-model="pickupForm.mileage" :min="0" />
        </el-form-item>
        <el-form-item label="油量/电量">
          <el-slider v-model="pickupForm.fuel" :max="100" show-input />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="pickupForm.note" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pickupVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPickup">确认取车</el-button>
      </template>
    </el-dialog>

    <!-- Return Dialog -->
    <el-dialog v-model="returnVisible" title="还车验车" width="500px">
      <el-form :model="returnForm" label-width="100px">
        <el-form-item label="当前里程">
          <el-input-number v-model="returnForm.mileage" :min="0" />
        </el-form-item>
        <el-form-item label="油量/电量">
          <el-slider v-model="returnForm.fuel" :max="100" show-input />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="returnForm.note" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReturn">确认还车</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import api from '@/api'
import dayjs from 'dayjs'

const loading = ref(false)
const orders = ref([])
const currentOrder = ref(null)
const detailVisible = ref(false)
const pickupVisible = ref(false)
const returnVisible = ref(false)

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

const pickupForm = reactive({
  orderId: null,
  mileage: 0,
  fuel: 100,
  note: ''
})

const returnForm = reactive({
  orderId: null,
  mileage: 0,
  fuel: 100,
  note: ''
})

const statusMap = {
  0: '已取消',
  1: '待审核',
  2: '审核失败',
  3: '待支付',
  4: '待取车',
  5: '用车中',
  6: '待还车',
  7: '待结算',
  8: '已完成',
  9: '退款中',
  10: '已退款'
}

const getStatusText = (status) => statusMap[status] || '未知'

const getStatusType = (status) => {
  const typeMap = {
    0: 'info', 1: 'warning', 2: 'danger', 3: 'warning',
    4: '', 5: 'primary', 6: 'primary', 7: 'warning', 8: 'success'
  }
  return typeMap[status] || ''
}

const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'

const loadOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (filterForm.orderNo) params.orderNo = filterForm.orderNo
    if (filterForm.status !== null) params.status = filterForm.status
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }

    const res = await api.admin.orders.list(params)
    if (res.code === 200) {
      orders.value = res.data.list
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error('Failed to load orders:', error)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.orderNo = ''
  filterForm.status = null
  filterForm.dateRange = []
  pagination.page = 1
  loadOrders()
}

const viewOrder = async (order) => {
  try {
    const res = await api.admin.orders.detail(order.id)
    if (res.code === 200) {
      currentOrder.value = res.data.order
      detailVisible.value = true
    }
  } catch (error) {
    console.error('Failed to load order detail:', error)
  }
}

const approveOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认通过该订单审核?', '确认')
    const res = await api.admin.orders.approve({ orderId: order.id })
    if (res.code === 200) {
      ElMessage.success('审核通过')
      loadOrders()
    }
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to approve order:', error)
  }
}

const rejectOrder = async (order) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝订单')
    const res = await api.admin.orders.reject({ orderId: order.id, reason: value })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      loadOrders()
    }
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to reject order:', error)
  }
}

const handlePickup = (order) => {
  pickupForm.orderId = order.id
  pickupForm.mileage = 0
  pickupForm.fuel = 100
  pickupForm.note = ''
  pickupVisible.value = true
}

const confirmPickup = async () => {
  try {
    const res = await api.admin.orders.pickup(pickupForm)
    if (res.code === 200) {
      ElMessage.success('取车成功')
      pickupVisible.value = false
      loadOrders()
    }
  } catch (error) {
    console.error('Failed to pickup:', error)
  }
}

const handleReturn = (order) => {
  returnForm.orderId = order.id
  returnForm.mileage = 0
  returnForm.fuel = 100
  returnForm.note = ''
  returnVisible.value = true
}

const confirmReturn = async () => {
  try {
    const res = await api.admin.orders.return(returnForm)
    if (res.code === 200) {
      ElMessage.success('还车成功')
      returnVisible.value = false
      loadOrders()
    }
  } catch (error) {
    console.error('Failed to return:', error)
  }
}

const completeOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确认完成该订单?', '确认')
    const res = await api.admin.orders.complete({ orderId: order.id })
    if (res.code === 200) {
      ElMessage.success('订单已完成')
      loadOrders()
    }
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to complete order:', error)
  }
}

const exportOrders = async () => {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.admin-orders {
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

.time-info {
  font-size: 12px;
  line-height: 1.6;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.order-detail {
  padding: 10px 0;
}
</style>
