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
            <el-tag :type="row.role === 'admin' ? 'danger' : ''">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="余额" width="100">
          <template #default="{ row }">
            ¥{{ row.balance || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="80" />
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewUser(row)">详情</el-button>
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
          <el-descriptions-item label="账户余额">¥{{ currentUser.balance || 0 }}</el-descriptions-item>
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

const filterForm = reactive({
  keyword: '',
  verificationStatus: null,
  role: ''
})

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

const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'

const maskIdCard = (idCard) => {
  if (!idCard || idCard.length < 8) return idCard
  return idCard.substring(0, 4) + '**********' + idCard.substring(idCard.length - 4)
}

const loadUsers = async () => {
  loading.value = true
  try {
    // Note: This would call an admin users API endpoint
    // For now, we'll simulate with mock data
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (filterForm.keyword) params.keyword = filterForm.keyword

    // Mock data for demonstration
    users.value = [
      { id: 1, username: 'admin', phone: '13800000000', email: 'admin@example.com', realName: '管理员', verificationStatus: 2, role: 'admin', balance: 0, points: 0, status: 1, createdAt: '2024-01-01 10:00:00' },
      { id: 2, username: 'testuser', phone: '13900000001', email: 'test@example.com', realName: '测试用户', verificationStatus: 2, role: 'user', balance: 1000, points: 500, status: 1, createdAt: '2024-01-02 10:00:00' }
    ]
    pagination.total = 2
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

const approveVerification = async (user) => {
  try {
    await ElMessageBox.confirm('确认通过该用户的实名认证?', '确认')
    // Call API to approve verification
    ElMessage.success('认证已通过')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to approve:', error)
  }
}

const rejectVerification = async (user) => {
  try {
    await ElMessageBox.prompt('请输入拒绝原因', '拒绝认证')
    // Call API to reject verification
    ElMessage.success('已拒绝')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to reject:', error)
  }
}

const disableUser = async (user) => {
  try {
    await ElMessageBox.confirm('确认禁用该用户?', '确认')
    // Call API to disable user
    ElMessage.success('用户已禁用')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to disable:', error)
  }
}

const enableUser = async (user) => {
  try {
    await ElMessageBox.confirm('确认启用该用户?', '确认')
    // Call API to enable user
    ElMessage.success('用户已启用')
    loadUsers()
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

.table-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info .username {
  font-weight: 600;
}

.user-info .phone {
  font-size: 12px;
  color: #909399;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.user-detail {
  padding: 10px 0;
}

.verification-images {
  margin-top: 20px;
}

.verification-images h4 {
  margin-bottom: 15px;
}

.image-list {
  display: flex;
  gap: 20px;
}

.image-item {
  text-align: center;
}

.image-item p {
  margin-bottom: 8px;
  color: #606266;
}

.image-item .el-image {
  width: 150px;
  height: 100px;
  border-radius: 4px;
}
</style>
