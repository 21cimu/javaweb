<template>
  <div class="admin-coupons">
    <el-row justify="space-between" align="middle" class="mb-4">
      <el-col>
        <div class="search-bar">
          <div class="search-keyword">
            <el-input v-model="keyword" placeholder="搜索优惠券名称或编码" clearable @clear="fetchList"/>
          </div>
          <el-select v-model="status" placeholder="状态" clearable class="search-status">
            <el-option :label="'全部'" :value="null"/>
            <el-option label="启用" :value="1"/>
            <el-option label="禁用" :value="0"/>
          </el-select>
          <el-button type="primary" @click="fetchList" class="search-action">搜索</el-button>
        </div>
      </el-col>
      <el-col>
        <el-button type="primary" @click="openCreate">新建优惠券</el-button>
      </el-col>
    </el-row>

    <el-table :data="items" stripe style="width:100%">
      <el-table-column prop="id" label="#" width="80"/>
      <el-table-column prop="code" label="编码" width="140"/>
      <el-table-column prop="name" label="名称"/>
      <el-table-column prop="type" label="类型" width="100" :formatter="formatType"/>
      <el-table-column prop="minAmount" label="门槛" width="120"/>
      <el-table-column prop="discountAmount" label="减免" width="120"/>
      <el-table-column prop="vipLevelRequired" label="会员等级门槛" width="140" :formatter="formatVipLevel"/>
      <el-table-column prop="startTime" label="开始日期" width="120" :formatter="formatDate"/>
      <el-table-column prop="endTime" label="结束日期" width="120" :formatter="formatDate"/>
      <el-table-column prop="status" label="状态" width="100" :formatter="formatStatus"/>
      <el-table-column label="操作" width="260">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="openIssue(row)">发放</el-button>
          <el-button size="small" type="danger" @click="disable(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination :current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="onPageChange"/>

    <!-- 原有 coupon-form 单独挂载改为放在对话框内 -->
    <el-dialog v-model="couponDialogVisible" title="优惠券" width="600px">
      <coupon-form ref="couponFormRef" :coupon="editingCoupon" @saved="onSaved" />
      <template #footer>
        <el-button @click="couponDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleSaveCoupon">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog title="发放优惠券" v-model="issueVisible">
      <el-form :model="issueForm">
        <el-form-item label="用户ID(逗号分隔)">
          <el-input v-model="issueForm.userIds" placeholder="例如: 1,2,3"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="issueForm.note"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" @click="doIssue">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import api from '@/api'
import CouponForm from '@/components/admin/CouponForm.vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'AdminCoupons',
  components: { CouponForm },
  data() {
    return {
      items: [],
      total: 0,
      page: 1,
      pageSize: 20,
      keyword: '',
      status: null,
      editingCoupon: null,
      issueVisible: false,
      issueForm: {
        couponId: null,
        userIds: '',
        note: ''
      },
      // 新增：控制优惠券表单对话框显隐
      couponDialogVisible: false
    }
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      const params = { page: this.page, pageSize: this.pageSize }
      if (this.keyword) params.keyword = this.keyword
      if (this.status !== null) params.status = this.status
      api.admin.coupons.list(params).then(res => {
        this.total = res.data.total
        this.items = res.data.items
      }).catch(() => {})
    },
    onPageChange(p) {
      this.page = p
      this.fetchList()
    },
    formatType(row, column, cell) {
      if (cell === 1) return '满减'
      if (cell === 2) return '折扣'
      if (cell === 3) return '固定'
      return ''
    },
    formatStatus(row, column, cell) {
      return cell === 1 ? '启用' : '禁用'
    },
    // 调整：会员等级门槛展示，仅保留 不限 / 黄金 / 钻石
    formatVipLevel(row, column, cell) {
      const level = cell
      if (level === null || level === undefined || level === 0) return '不限（所有用户）'
      if (level === 1) return '仅黄金会员'
      if (level === 2) return '仅钻石会员'
      return `未知等级(${level})`
    },
    formatDate(row, column, cellValue) {
      if (!cellValue) return ''
      return String(cellValue).substring(0, 10)
    },
    // 新建：打开弹窗并重置表单
    openCreate() {
      this.editingCoupon = null
      this.couponDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.couponFormRef && this.$refs.couponFormRef.resetForCreate) {
          this.$refs.couponFormRef.resetForCreate()
        }
      })
    },
    // 编辑：打开弹窗并填充表单
    openEdit(row) {
      this.editingCoupon = row
      this.couponDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.couponFormRef && this.$refs.couponFormRef.fillForEdit) {
          this.$refs.couponFormRef.fillForEdit(row)
        }
      })
    },
    // 点击弹窗“保存”按钮时，委托子组件执行保存
    handleSaveCoupon() {
      if (this.$refs.couponFormRef && this.$refs.couponFormRef.save) {
        this.$refs.couponFormRef.save()
      }
    },
    // 子组件保存成功后关闭弹窗并刷新列表
    onSaved() {
      this.couponDialogVisible = false
      this.fetchList()
    },
    openIssue(row) {
      this.issueForm.couponId = row.id
      this.issueVisible = true
    },
    doIssue() {
      const userIds = this.issueForm.userIds
        .split(',')
        .map(s => parseInt(s.trim()))
        .filter(Boolean)
      if (!userIds.length) {
        ElMessage.error('请至少填写一个用户ID')
        return
      }
      api.admin.coupons.issue({
        couponId: this.issueForm.couponId,
        userIds,
        note: this.issueForm.note
      })
        .then(() => {
          ElMessage.success('发放完成')
          this.issueVisible = false
        })
        .catch(() => {})
    },
    disable(row) {
      api.admin.coupons.delete(row.id)
        .then(() => {
          ElMessage.success('已删除')
          this.fetchList()
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.mb-4 { margin-bottom: 16px }

.search-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.search-keyword {
  flex: 1 1 260px;
  max-width: 420px;
  min-width: 220px;
}

.search-status {
  margin-left: 12px;
  width: 120px;
}

.search-action {
  margin-left: 8px;
}
</style>
