<template>
  <el-form :model="form">
    <el-form-item label="编码">
      <el-input v-model="form.code" />
    </el-form-item>
    <el-form-item label="名称">
      <el-input v-model="form.name" />
    </el-form-item>
    <el-form-item label="类型">
      <el-select v-model="form.type">
        <el-option label="满减" :value="1" />
        <el-option label="折扣" :value="2" />
        <el-option label="固定" :value="3" />
      </el-select>
    </el-form-item>
    <el-form-item label="门槛">
      <el-input v-model="form.minAmount" />
    </el-form-item>
    <!-- 调整：最低会员等级门槛，仅保留 不限 / 黄金 / 钻石 -->
    <el-form-item label="最低会员等级">
      <el-select v-model="form.vipLevelRequired" placeholder="请选择领取所需会员等级">
        <el-option :label="'不限（所有用户）'" :value="0" />
        <el-option label="仅黄金会员" :value="1" />
        <el-option label="仅钻石会员" :value="2" />
      </el-select>
    </el-form-item>
    <el-form-item label="减免/折扣率">
      <el-input v-model="form.discountAmount" placeholder="减免金额或折扣率" />
    </el-form-item>
    <el-form-item label="开始日期">
      <el-date-picker
        v-model="form.startTime"
        type="date"
        value-format="YYYY-MM-DD"
        format="YYYY-MM-DD"
        placeholder="选择开始日期"
      />
    </el-form-item>
    <el-form-item label="结束日期">
      <el-date-picker
        v-model="form.endTime"
        type="date"
        value-format="YYYY-MM-DD"
        format="YYYY-MM-DD"
        placeholder="选择结束日期"
      />
    </el-form-item>
    <el-form-item label="总量">
      <el-input v-model="form.totalCount" />
    </el-form-item>
    <el-form-item label="每人限领">
      <el-input v-model="form.perUserLimit" />
    </el-form-item>
    <el-form-item label="状态">
      <el-select v-model="form.status">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
    </el-form-item>
  </el-form>
</template>

<script>
import api from '@/api'
import { ElMessage } from 'element-plus'

export default {
  name: 'CouponForm',
  props: {
    coupon: Object
  },
  data() {
    return {
      form: {
        id: null,
        code: '',
        name: '',
        type: 1,
        minAmount: null,
        discountAmount: null,
        discountRate: null,
        maxDiscount: null,
        totalCount: null,
        perUserLimit: 1,
        startTime: '',
        endTime: '',
        applicableCategories: '',
        applicableVehicles: '',
        // 新增：最低会员等级门槛，0 表示不限
        vipLevelRequired: 0,
        status: 1
      }
    }
  },
  methods: {
    // 新建时重置表单
    resetForCreate() {
      this.form = {
        id: null,
        code: '',
        name: '',
        type: 1,
        minAmount: null,
        discountAmount: null,
        discountRate: null,
        maxDiscount: null,
        totalCount: null,
        perUserLimit: 1,
        startTime: '',
        endTime: '',
        applicableCategories: '',
        applicableVehicles: '',
        // 重置为不限
        vipLevelRequired: 0,
        status: 1
      }
    },
    // 编辑时填充表单
    fillForEdit(coupon) {
      const copy = Object.assign({}, this.form, coupon)
      // 只保留日期部分
      copy.startTime = copy.startTime ? String(copy.startTime).substring(0, 10) : ''
      copy.endTime = copy.endTime ? String(copy.endTime).substring(0, 10) : ''
      // 兼容后端未返回或为 null 的情况，统一视为不限
      copy.vipLevelRequired = (copy.vipLevelRequired === null || copy.vipLevelRequired === undefined) ? 0 : copy.vipLevelRequired
      this.form = copy
    },
    save() {
      // 基础校验
      if (!this.form.code || !this.form.name) {
        ElMessage.error('编码和名称为必填')
        return
      }

      // 确保提交前 vipLevelRequired 有值
      if (this.form.vipLevelRequired === null || this.form.vipLevelRequired === undefined) {
        this.form.vipLevelRequired = 0
      }

      if (this.form.id) {
        api.admin.coupons.update(this.form.id, this.form).then(() => {
          ElMessage.success('保存成功')
          this.$emit('saved')
        }).catch(() => {})
      } else {
        api.admin.coupons.create(this.form).then(() => {
          ElMessage.success('创建成功')
          this.$emit('saved')
        }).catch(() => {})
      }
    }
  }
}
</script>
