<template>
  <div class="admin-users">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="用户名/手机号/姓名" clearable />
        </el-form-item>
        <el-form-item label="认证状态">
          <el-select v-model="filterForm.verificationStatus" placeholder="全部" clearable style="width: 120px;">
            <el-option label="未认证" :value="0" />
            <el-option label="待审核" :value="1" />
            <el-option label="已认证" :value="2" />
            <el-option label="认证失败" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户角色">
          <el-select v-model="filterForm.role" placeholder="全部" clearable style="width: 120px;">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
            <el-option label="超级管理员" value="superadmin" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <div>
            <el-statistic title="总用户数" :value="pagination.total" />
          </div>
        </div>
      </template>

      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="40" :src="row.avatar">
                {{ row.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <div class="user-info">
                <div class="username">{{ row.username }}</div>
                <div class="phone">{{ row.phone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" width="100" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column label="认证状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getVerificationTag(row.verificationStatus)">
              {{ getVerificationText(row.verificationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleTag(row.role)">
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="余额" width="100">
          <template #default="{ row }">
            ¥{{ row.balance || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="80" />
        <el-table-column label="会员等级" width="120">
          <template #default="{ row }">
            <el-tag v-if="getVipText(row.vipLevel) === '普通'" type="info">{{ getVipText(row.vipLevel) }}</el-tag>
            <el-tag v-else-if="getVipText(row.vipLevel) === '黄金'" type="warning">{{ getVipText(row.vipLevel) }}</el-tag>
            <el-tag v-else type="success">{{ getVipText(row.vipLevel) }}</el-tag>
            <div style="font-size:12px;color:#909399;margin-top:4px">¥{{ row.cumulativeSpending || 0 }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewUser(row)">详情</el-button>
            <el-button link type="warning" @click="openRoleDialog(row)">权限</el-button>
            <el-button v-if="row.verificationStatus === 1" link type="success" @click="approveVerification(row)">通过认证</el-button>
            <el-button v-if="row.verificationStatus === 1" link type="danger" @click="rejectVerification(row)">拒绝</el-button>
            <el-button v-if="row.status === 1" link type="danger" @click="disableUser(row)">禁用</el-button>
            <el-button v-else link type="success" @click="enableUser(row)">启用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadUsers"
        @current-change="loadUsers"
        class="pagination"
      />
    </el-card>

    <!-- User Detail Dialog -->
    <el-dialog v-model="detailVisible" title="用户详情" width="700px">
      <div v-if="currentUser" class="user-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentUser.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ currentUser.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ maskIdCard(currentUser.idCard) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="驾驶证号">{{ currentUser.driverLicense || '-' }}</el-descriptions-item>
          <el-descriptions-item label="认证状态">
            <el-tag :type="getVerificationTag(currentUser.verificationStatus)">
              {{ getVerificationText(currentUser.verificationStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户权限">{{ getRoleText(currentUser.role) }}</el-descriptions-item>
          <el-descriptions-item label="账户余额">¥{{ currentUser.balance || 0 }}</el-descriptions-item>
          <el-descriptions-item label="会员等级">{{ getVipText(currentUser.vipLevel) }}</el-descriptions-item>
          <el-descriptions-item label="累计消费">¥{{ currentUser.cumulativeSpending || 0 }}</el-descriptions-item>
          <el-descriptions-item label="积分">{{ currentUser.points || 0 }}</el-descriptions-item>
          <el-descriptions-item label="邀请码">{{ currentUser.inviteCode }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDate(currentUser.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentUser.verificationStatus === 1" class="verification-images">
          <h4>认证资料</h4>
          <div class="image-list">
            <div v-if="currentUser.idCardFront" class="image-item">
              <p>身份证正面</p>
              <el-image :src="currentUser.idCardFront" fit="cover" />
            </div>
            <div v-if="currentUser.idCardBack" class="image-item">
              <p>身份证背面</p>
              <el-image :src="currentUser.idCardBack" fit="cover" />
            </div>
            <div v-if="currentUser.driverLicenseImage" class="image-item">
              <p>驾驶证</p>
              <el-image :src="currentUser.driverLicenseImage" fit="cover" />
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- Role Update Dialog -->
    <el-dialog v-model="roleVisible" title="修改用户权限" width="420px" @closed="resetRoleForm">
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="用户">
          <span>{{ roleTarget?.username || '-' }}</span>
        </el-form-item>
        <el-form-item label="权限">
          <el-select v-model="roleForm.role" placeholder="请选择权限" style="width: 100%;">
            <el-option
              v-for="option in roleOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '@/api'
import dayjs from 'dayjs'

const loading = ref(false)
const users = ref([])
const currentUser = ref(null)
const detailVisible = ref(false)
const roleVisible = ref(false)
const roleSubmitting = ref(false)
const roleTarget = ref(null)

const filterForm = reactive({
  keyword: '',
  verificationStatus: null,
  role: ''
})

const roleForm = reactive({
  userId: null,
  role: ''
})

const roleOptions = [
  { label: '普通用户', value: 'user' },
  { label: '管理员', value: 'admin' },
  { label: '超级管理员', value: 'superadmin' }
]

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const getVerificationText = (status) => {
  const map = { 0: '未认证', 1: '待审核', 2: '已认证', 3: '认证失败' }
  return map[status] || '未知'
}

const getVerificationTag = (status) => {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || ''
}

const getRoleText = (role) => {
  const map = { user: '普通用户', admin: '管理员', superadmin: '超级管理员' }
  return map[role] || '未知'
}

const getRoleTag = (role) => {
  const map = { user: '', admin: 'danger', superadmin: 'warning' }
  return map[role] || ''
}

const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'

const maskIdCard = (idCard) => {
  if (!idCard || idCard.length < 8) return idCard
  return idCard.substring(0, 4) + '**********' + idCard.substring(idCard.length - 4)
}

// VIP helper
const getVipText = (level) => {
  if (level === 2) return '钻石'
  if (level === 1) return '黄金'
  return '普通'
}

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (filterForm.keyword) params.keyword = filterForm.keyword
    if (filterForm.verificationStatus !== null && filterForm.verificationStatus !== '') params.verificationStatus = filterForm.verificationStatus
    if (filterForm.role) params.role = filterForm.role

    const res = await api.admin.users.list(params)
    if (res && res.code === 200) {
      const data = res.data
      users.value = data.list || []
      pagination.page = data.page || pagination.page
      pagination.pageSize = data.pageSize || pagination.pageSize
      pagination.total = data.total || 0
    } else {
      console.error('Failed to load users:', res)
    }
  } catch (error) {
    console.error('Failed to load users:', error)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.keyword = ''
  filterForm.verificationStatus = null
  filterForm.role = ''
  pagination.page = 1
  loadUsers()
}

const viewUser = (user) => {
  currentUser.value = user
  detailVisible.value = true
}

const openRoleDialog = (user) => {
  roleTarget.value = user
  roleForm.userId = user.id
  roleForm.role = user.role || 'user'
  roleVisible.value = true
}

const resetRoleForm = () => {
  roleTarget.value = null
  roleForm.userId = null
  roleForm.role = ''
}

const submitRole = async () => {
  if (!roleForm.userId) {
    ElMessage.error('用户不存在')
    return
  }
  if (!roleForm.role) {
    ElMessage.warning('请选择权限')
    return
  }
  roleSubmitting.value = true
  try {
    const res = await api.admin.users.updateRole({
      userId: roleForm.userId,
      role: roleForm.role
    })
    if (res && res.code === 200) {
      ElMessage.success('权限已更新')
      roleVisible.value = false
      loadUsers()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('Failed to update role:', error)
  } finally {
    roleSubmitting.value = false
  }
}

const approveVerification = async (user) => {
  try {
    await ElMessageBox.confirm('确认通过该用户的实名认证?', '确认')
    const res = await api.admin.users.approve({ userId: user.id })
    if (res && res.code === 200) {
      ElMessage.success('认证已通过')
      loadUsers()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to approve:', error)
  }
}

const rejectVerification = async (user) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝认证')
    const res = await api.admin.users.reject({ userId: user.id, reason: value })
    if (res && res.code === 200) {
      ElMessage.success('已拒绝')
      loadUsers()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to reject:', error)
  }
}

const disableUser = async (user) => {
  try {
    await ElMessageBox.confirm('确认禁用该用户?', '确认')
    const res = await api.admin.users.disable({ userId: user.id })
    if (res && res.code === 200) {
      ElMessage.success('用户已禁用')
      loadUsers()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to disable:', error)
  }
}

const enableUser = async (user) => {
  try {
    await ElMessageBox.confirm('确认启用该用户?', '确认')
    const res = await api.admin.users.enable({ userId: user.id })
    if (res && res.code === 200) {
      ElMessage.success('用户已启用')
      loadUsers()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to enable:', error)
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.admin-users {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  background: #f9f9f9;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: #fff;
  border-radius: 4px 4px 0 0;
}

.user-cell {
  display: flex;
  align-items: center;
}

.user-info {
  margin-left: 10px;
}

.username {
  font-weight: 500;
}

.phone {
  font-size: 12px;
  color: #666;
}

.verification-images {
  margin-top: 20px;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
}

.image-item {
  width: 120px;
  margin-right: 10px;
  margin-bottom: 10px;
}

.image-item p {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #333;
}
</style>
