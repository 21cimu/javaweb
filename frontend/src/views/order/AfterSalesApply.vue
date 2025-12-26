<template>
  <div class="after-sales-page" v-loading="loading">
    <div class="page-header">
      <h1>售后申请</h1>
    </div>

    <el-card v-if="order" class="order-brief">
      <div class="order-brief-header">
        <span>订单号：{{ order.orderNo }}</span>
        <el-tag :type="getStatusType(order.status)" size="small">{{ getStatusText(order.status) }}</el-tag>
      </div>
      <div class="order-brief-body">
        <div class="vehicle">
          <strong>{{ order.vehicleName }}</strong>
        </div>
        <div class="time">
          {{ formatDate(order.pickupTime) }} - {{ formatDate(order.returnTime) }} （{{ order.rentalDays }}天）
        </div>
        <div class="amount">
          实付金额：¥{{ order.paidAmount ?? order.totalAmount }}
        </div>
      </div>
    </el-card>

    <el-card class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="售后类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">报修</el-radio>
            <el-radio :label="2">退款申请</el-radio>
            <el-radio :label="3">投诉</el-radio>
            <el-radio :label="4">其他</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="问题分类" prop="reasonCode">
          <el-select v-model="form.reasonCode" placeholder="请选择问题分类" clearable>
            <el-option label="车辆故障" value="vehicle_problem" />
            <el-option label="服务问题" value="service_issue" />
            <el-option label="价格/费用" value="price_issue" />
            <el-option label="订单信息错误" value="order_info" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="请尽可能详细描述您遇到的问题……"
          />
        </el-form-item>

        <el-form-item v-if="form.type === 2" label="申请退款金额" prop="refundAmount">
          <el-input-number
            v-model="form.refundAmount"
            :min="0.01"
            :max="maxRefundAmount || undefined"
            :precision="2"
            :step="10"
            controls-position="right"
          />
          <span class="hint">&nbsp;&nbsp;最多可申请 ¥{{ maxRefundAmount || 0 }}</span>
        </el-form-item>

        <el-form-item label="期望处理" prop="expectedSolution">
          <el-input
            v-model="form.expectedSolution"
            placeholder="如：希望更换车辆 / 部分退款 / 仅记录投诉等"
          />
        </el-form-item>

        <el-form-item label="联系方式" prop="contactPhone">
          <el-input v-model="form.contactPhone" maxlength="20" placeholder="请输入手机号" />
        </el-form-item>

        <!-- 预留上传图片区，先简单放一个占位说明，后续可接入已有上传接口 -->
        <el-form-item label="补充说明">
          <el-input
            v-model="form.extraRemark"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="可补充说明现场情况、工作人员姓名等信息"
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="goBack">返回</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const order = ref(null)

const formRef = ref(null)
const form = ref({
  orderId: null,
  type: 2,
  reasonCode: '',
  description: '',
  refundAmount: null,
  expectedSolution: '',
  contactPhone: '',
  extraRemark: ''
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
  const map = {
    0: 'info',
    1: 'warning',
    2: 'danger',
    3: 'warning',
    4: '',
    5: 'success',
    6: 'warning',
    7: 'warning',
    8: 'success',
    9: 'info',
    10: 'info'
  }
  return map[status] || 'info'
}

const formatDate = (v) => v ? dayjs(v).format('MM-DD HH:mm') : ''

const maxRefundAmount = computed(() => {
  if (!order.value) return null
  const paid = Number(order.value.paidAmount || order.value.totalAmount || 0)
  const refunded = Number(order.value.refundAmount || 0)
  const remain = paid - refunded
  if (remain <= 0) return null
  return Number(remain.toFixed(2))
})

const rules = {
  type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  description: [
    { required: true, message: '请填写问题描述', trigger: 'blur' },
    { min: 10, max: 1000, message: '描述需在 10-1000 字之间', trigger: 'blur' }
  ],
  refundAmount: [
    {
      validator: (_, value, callback) => {
        if (form.value.type !== 2) return callback()
        if (value == null || value === '') return callback(new Error('请输入退款金额'))
        if (value <= 0) return callback(new Error('退款金额必须大于 0'))
        if (value > maxRefundAmount.value) return callback(new Error('退款金额不能超过可退金额'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  contactPhone: [
    { required: true, message: '请输入联系方式', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (!value) return callback()
        const phoneReg = /^1\d{10}$/
        if (!phoneReg.test(value)) return callback(new Error('请输入有效的手机号'))
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const loadOrder = async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const res = await api.orders.detail(id)
    if (res.code === 200) {
      const o = res.data.order || res.data
      order.value = o
      form.value.orderId = o.id
      form.value.contactPhone = o.userPhone || ''
    } else {
      ElMessage.error(res.message || '获取订单信息失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取订单信息失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await ElMessageBox.confirm('确认提交售后申请吗？提交后将进入平台审核流程。', '提示', { type: 'warning' })
    } catch {
      return
    }

    submitting.value = true
    try {
      const payload = {
        orderId: form.value.orderId,
        type: form.value.type,
        reasonCode: form.value.reasonCode,
        description: form.value.description,
        refundAmount: form.value.type === 2 ? form.value.refundAmount : null,
        expectedSolution: form.value.expectedSolution,
        contactPhone: form.value.contactPhone,
        extraRemark: form.value.extraRemark
      }
      const res = await api.afterSales.create(payload)
      if (res.code === 200) {
        ElMessage.success('售后申请已提交')
        router.push({ name: 'OrderDetail', params: { id: form.value.orderId } })
      } else {
        ElMessage.error(res.message || '提交失败')
      }
    } catch (e) {
      console.error(e)
      ElMessage.error('提交失败')
    } finally {
      submitting.value = false
    }
  })
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadOrder()
})
</script>

<style scoped>
.after-sales-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 16px 40px;
}

.page-header {
  margin-bottom: 16px;
}

.order-brief {
  margin-bottom: 20px;
}

.order-brief-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.order-brief-body .time,
.order-brief-body .amount {
  color: #666;
  font-size: 13px;
}

.form-card {
  margin-top: 8px;
}

.hint {
  font-size: 12px;
  color: #999;
}
</style>

