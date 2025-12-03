<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '../api'

const user = ref(null)
const loading = ref(true)
const editDialogVisible = ref(false)
const editLoading = ref(false)

const editForm = reactive({
  phone: '',
  email: '',
  realName: '',
  idCard: '',
  drivingLicense: ''
})

const memberLevelMap = {
  NORMAL: { text: '普通会员', color: '#909399' },
  GOLD: { text: '黄金会员', color: '#E6A23C' },
  DIAMOND: { text: '钻石会员', color: '#409EFF' }
}

onMounted(async () => {
  await loadUser()
})

const loadUser = async () => {
  loading.value = true
  try {
    const savedUser = JSON.parse(localStorage.getItem('user'))
    if (savedUser && savedUser.id) {
      user.value = await userApi.getById(savedUser.id)
    }
  } catch (e) {
    console.error('Failed to load user:', e)
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

const openEditDialog = () => {
  if (user.value) {
    editForm.phone = user.value.phone || ''
    editForm.email = user.value.email || ''
    editForm.realName = user.value.realName || ''
    editForm.idCard = user.value.idCard || ''
    editForm.drivingLicense = user.value.drivingLicense || ''
  }
  editDialogVisible.value = true
}

const saveUserInfo = async () => {
  editLoading.value = true
  try {
    const res = await userApi.update(user.value.id, editForm)
    if (res.success) {
      ElMessage.success('保存成功')
      editDialogVisible.value = false
      await loadUser()
      // Update localStorage
      localStorage.setItem('user', JSON.stringify(res.user))
    } else {
      ElMessage.error(res.error || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    editLoading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="user-center-page">
    <el-row :gutter="20">
      <el-col :xs="24" :md="8">
        <!-- User Profile Card -->
        <el-card v-loading="loading" class="profile-card">
          <div class="profile-header">
            <el-avatar :size="80" icon="User" />
            <h2>{{ user?.username || '用户' }}</h2>
            <el-tag 
              :color="memberLevelMap[user?.memberLevel]?.color"
              effect="dark"
            >
              {{ memberLevelMap[user?.memberLevel]?.text || '普通会员' }}
            </el-tag>
          </div>
          
          <el-divider />
          
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">{{ user?.points || 0 }}</span>
              <span class="stat-label">积分</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">¥{{ user?.balance || 0 }}</span>
              <span class="stat-label">余额</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :md="16">
        <!-- User Info Card -->
        <el-card v-loading="loading" class="info-card">
          <template #header>
            <div class="card-header">
              <h3>个人信息</h3>
              <el-button type="primary" @click="openEditDialog">编辑资料</el-button>
            </div>
          </template>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户名">{{ user?.username }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ user?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ user?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formatDate(user?.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        
        <!-- Verification Card -->
        <el-card v-loading="loading" class="verify-card">
          <template #header>
            <h3>实名认证</h3>
          </template>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="真实姓名">
              <span v-if="user?.realName">{{ user.realName }}</span>
              <el-tag v-else type="warning" size="small">未认证</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="身份证号">
              <span v-if="user?.idCard">{{ user.idCard.replace(/(\d{4})\d+(\d{4})/, '$1****$2') }}</span>
              <el-tag v-else type="warning" size="small">未认证</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="驾照号">
              <span v-if="user?.drivingLicense">{{ user.drivingLicense }}</span>
              <el-tag v-else type="warning" size="small">未认证</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="认证状态">
              <el-tag v-if="user?.realName && user?.idCard && user?.drivingLicense" type="success">
                已完成认证
              </el-tag>
              <el-tag v-else type="warning">
                待完善信息
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
          
          <div class="verify-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>完成实名认证和驾照认证后，方可进行车辆预订</span>
          </div>
        </el-card>
        
        <!-- Quick Links -->
        <el-card class="links-card">
          <template #header>
            <h3>快捷入口</h3>
          </template>
          
          <el-row :gutter="16">
            <el-col :span="8">
              <router-link to="/orders" class="link-item">
                <el-icon :size="32"><Document /></el-icon>
                <span>我的订单</span>
              </router-link>
            </el-col>
            <el-col :span="8">
              <router-link to="/vehicles" class="link-item">
                <el-icon :size="32"><Van /></el-icon>
                <span>浏览车辆</span>
              </router-link>
            </el-col>
            <el-col :span="8">
              <div class="link-item" @click="openEditDialog">
                <el-icon :size="32"><Setting /></el-icon>
                <span>账户设置</span>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- Edit Dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑个人资料" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="editForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="editForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="驾照号">
          <el-input v-model="editForm.drivingLicense" placeholder="请输入驾照号" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveUserInfo">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-center-page {
  max-width: 1200px;
  margin: 0 auto;
}

.profile-card {
  text-align: center;
  margin-bottom: 20px;
}

.profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.profile-header h2 {
  margin: 0;
}

.profile-stats {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.info-card,
.verify-card,
.links-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}

.verify-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px;
  background: #FDF6EC;
  border-radius: 4px;
  color: #E6A23C;
  font-size: 14px;
}

.link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  transition: background 0.3s;
}

.link-item:hover {
  background: #ecf5ff;
}

.link-item span {
  font-size: 14px;
}
</style>
