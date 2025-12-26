<template>
  <div class="admin-user-coupons">
    <el-row class="mb-4" justify="space-between">
      <el-col>
        <el-input v-model="keyword" placeholder="搜索用户名/手机号/券码" clearable style="width:260px;"/>
        <el-input v-model="filterUserId" placeholder="用户ID" clearable style="width:120px; margin-left:8px;"/>
        <el-select v-model="status" placeholder="状态" clearable style="width:120px; margin-left:8px;">
          <el-option :label="'全部'" :value="null"/>
          <el-option label="未使用" :value="0"/>
          <el-option label="已使用" :value="1"/>
          <el-option label="已过期/回收" :value="2"/>
        </el-select>
        <el-button type="primary" @click="fetchList" style="margin-left:8px;">搜索</el-button>
      </el-col>
      <el-col>
        <!-- future: bulk actions -->
      </el-col>
    </el-row>

    <el-table :data="items" stripe style="width:100%">
      <el-table-column prop="id" label="#" width="80"/>
      <el-table-column prop="userId" label="用户ID" width="100"/>
      <el-table-column prop="username" label="用户名" width="140"/>
      <el-table-column prop="phone" label="手机号" width="140"/>
      <el-table-column prop="couponId" label="券ID" width="100"/>
      <el-table-column prop="code" label="券编码" width="160"/>
      <el-table-column prop="name" label="券名称"/>
      <el-table-column prop="couponEndTime" label="到期日期" width="120" :formatter="formatDate"/>
      <el-table-column prop="status" label="状态" width="140" :formatter="formatStatus"/>
      <el-table-column prop="createdAt" label="领取时间" width="180"/>
      <el-table-column label="操作" width="260">
        <template #default="{row}">
          <el-select v-model="row._editingStatus" placeholder="修改状态" size="small" style="width:120px;">
            <el-option label="未使用" :value="0"/>
            <el-option label="已使用" :value="1"/>
            <el-option label="已过期/回收" :value="2"/>
          </el-select>
          <el-button size="small" type="primary" @click="updateStatus(row)">保存</el-button>
          <el-button size="small" type="danger" @click="revoke(row)">回收</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination :current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="onPageChange"/>
  </div>
</template>

<script>
import api from '@/api'
import { ElMessage } from 'element-plus'

export default {
  name: 'AdminUserCoupons',
  data() {
    return {
      items: [],
      total: 0,
      page: 1,
      pageSize: 20,
      keyword: '',
      filterUserId: '',
      status: null
    }
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      const params = { page: this.page, pageSize: this.pageSize }
      if (this.keyword) params.keyword = this.keyword
      if (this.filterUserId) params.userId = this.filterUserId
      if (this.status !== null) params.status = this.status
      api.admin.userCoupons.list(params).then(res => {
        this.total = res.data.total
        this.items = res.data.items.map(i => ({ ...i, _editingStatus: i.status }))
      }).catch(() => {})
    },
    onPageChange(p) {
      this.page = p
      this.fetchList()
    },
    formatStatus(row, column, cell) {
      if (cell === 0) return '未使用'
      if (cell === 1) return '已使用'
      if (cell === 2) return '已过期/回收'
      return ''
    },
    formatDate(row, column, cellValue) {
      if (!cellValue) return ''
      return String(cellValue).substring(0, 10)
    },
    updateStatus(row) {
      const newStatus = row._editingStatus
      api.admin.userCoupons.updateStatus(row.id, { status: newStatus }).then(() => {
        ElMessage.success('状态更新成功')
        this.fetchList()
      }).catch(() => {})
    },
    revoke(row) {
      this.$confirm('确认回收该优惠券？', '提示', { type: 'warning' }).then(() => {
        api.admin.userCoupons.delete(row.id).then(() => {
          ElMessage.success('已回收')
          this.fetchList()
        }).catch(() => {})
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.mb-4 { margin-bottom: 16px }
</style>
