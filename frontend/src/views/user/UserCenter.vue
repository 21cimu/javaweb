<template>
  <div class="user-center-page">
    <div class="user-header">
      <div class="user-info">
        <el-upload
          class="avatar-uploader"
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleAvatarSelect"
          accept="image/*"
          :disabled="avatarUploading"
        >
          <div class="avatar-wrapper" :class="{ 'is-loading': avatarUploading }">
            <el-avatar :size="80" :src="userStore.user?.avatar">
              {{ userStore.user?.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <div class="avatar-mask">
              {{ avatarUploading ? '上传中...' : '点击更换' }}
            </div>
          </div>
        </el-upload>
        <div class="user-details">
          <h2>{{ userStore.user?.username }}</h2>
          <p>{{ userStore.user?.phone }}</p>
          <div class="vip-info">
            <el-tag v-if="getVipText(userStore.user?.vipLevel) === '普通'" type="info">{{ getVipText(userStore.user?.vipLevel) }}</el-tag>
            <el-tag v-else-if="getVipText(userStore.user?.vipLevel) === '黄金'" type="warning">{{ getVipText(userStore.user?.vipLevel) }}</el-tag>
            <el-tag v-else type="success">{{ getVipText(userStore.user?.vipLevel) }}</el-tag>
            <span class="vip-spend">累计消费: ¥{{ userStore.user?.cumulativeSpending || 0 }}</span>
          </div>
          <el-tag v-if="userStore.isVerified" type="success" size="small">已认证</el-tag>
          <el-tag v-else type="warning" size="small">未认证</el-tag>
        </div>
      </div>
      <div class="user-stats">
        <div class="stat-item">
          <span class="value">{{ userStore.user?.balance || 0 }}</span>
          <span class="label">余额(元)</span>
        </div>
        <div class="stat-item">
          <span class="value">{{ userStore.user?.points || 0 }}</span>
          <span class="label">积分</span>
        </div>
        <div class="stat-item">
          <span class="value">{{ orderCount }}</span>
          <span class="label">订单数</span>
        </div>
      </div>
    </div>
    
    <div class="user-content">
      <el-menu :default-active="activeMenu" mode="horizontal" @select="handleMenuSelect">
        <el-menu-item index="profile">个人信息</el-menu-item>
        <el-menu-item index="verification">实名认证</el-menu-item>
        <el-menu-item index="security">账号安全</el-menu-item>
        <el-menu-item index="invite">邀请有礼</el-menu-item>
      </el-menu>
      
      <!-- Profile Tab -->
      <div v-if="activeMenu === 'profile'" class="tab-content">
        <el-card shadow="never">
          <el-form :model="profileForm" label-width="100px">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="profileForm.realName" :disabled="userStore.isVerified" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profileForm.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
                <el-radio :value="0">保密</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updateProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
      
      <!-- Verification Tab -->
      <div v-if="activeMenu === 'verification'" class="tab-content">
        <el-card shadow="never">
          <div v-if="userStore.isVerified" class="verified-info">
            <el-result icon="success" title="已完成实名认证">
              <template #sub-title>
                <p>您已通过实名认证，可以正常下单租车</p>
              </template>
            </el-result>
          </div>
          <div v-else>
            <el-alert type="info" :closable="false" show-icon style="margin-bottom: 20px;">
              完成实名认证后才能租车下单，请确保信息真实有效
            </el-alert>
            <router-link to="/user/verification">
              <el-button type="primary">去认证</el-button>
            </router-link>
          </div>
        </el-card>
      </div>
      
      <!-- Security Tab -->
      <div v-if="activeMenu === 'security'" class="tab-content">
        <el-card shadow="never">
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="图片验证码" prop="captcha">
              <div class="captcha-row">
                <el-input
                  v-model="passwordForm.captcha"
                  maxlength="4"
                  placeholder="请输入验证码"
                />
                <img
                  class="captcha-image"
                  :class="{ 'is-loading': passwordCaptchaLoading }"
                  :src="passwordCaptcha"
                  alt="验证码"
                  @click="refreshPasswordCaptcha"
                />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
      
      <!-- Invite Tab -->
      <div v-if="activeMenu === 'invite'" class="tab-content">
        <el-card shadow="never">
          <div class="invite-section">
            <h3>我的邀请码</h3>
            <div class="invite-code">
              <span>{{ userStore.user?.inviteCode }}</span>
              <el-button size="small" @click="copyInviteCode">复制</el-button>
            </div>
            <p class="invite-tip">邀请好友注册，双方各得100积分奖励</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/api'

const userStore = useUserStore()

const activeMenu = ref('profile')
const orderCount = ref(0)
const passwordFormRef = ref(null)
const avatarUploading = ref(false)
const passwordCaptcha = ref('')
const passwordCaptchaLoading = ref(false)

const profileForm = reactive({
  username: '',
  phone: '',
  email: '',
  realName: '',
  gender: 0
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  captcha: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 4, message: '验证码为4位', trigger: 'blur' }
  ]
}

const handleAvatarSelect = async (file) => {
  if (avatarUploading.value) return
  let actualFile = null
  if (file?.raw && file.raw instanceof File) actualFile = file.raw
  else if (file?.file && file.file instanceof File) actualFile = file.file
  else if (file instanceof File) actualFile = file
  else if (file?.raw?.file && file.raw.file instanceof File) actualFile = file.raw.file

  if (!actualFile) {
    ElMessage.error('请选择图片文件')
    return
  }
  if (!actualFile.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return
  }
  const maxSizeMb = 5
  if (actualFile.size > maxSizeMb * 1024 * 1024) {
    ElMessage.warning(`图片大小不能超过${maxSizeMb}MB`)
    return
  }

  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', actualFile)
    formData.append('folder', 'header')
    if (userStore.user?.id) {
      formData.append('baseName', `user_${userStore.user.id}`)
    }
    const uploadRes = await api.upload.file(formData)
    if (!uploadRes || uploadRes.code !== 200) {
      throw new Error(uploadRes?.message || '上传失败')
    }
    const data = uploadRes.data
    const url = data && data.url ? data.url : data
    if (!url) {
      throw new Error('上传失败')
    }
    const res = await userStore.updateProfile({ avatar: url })
    if (res.code === 200) {
      ElMessage.success('头像更新成功')
    } else {
      ElMessage.error(res.message || '头像更新失败')
    }
  } catch (error) {
    console.error('avatar upload error', error)
    ElMessage.error(error?.message || '头像更新失败')
  } finally {
    avatarUploading.value = false
  }
}

const refreshPasswordCaptcha = async () => {
  if (passwordCaptchaLoading.value) return
  passwordCaptchaLoading.value = true
  try {
    const res = await api.user.passwordCaptcha()
    if (res.code === 200 && res.data?.image) {
      passwordCaptcha.value = res.data.image
    } else {
      passwordCaptcha.value = ''
      ElMessage.error(res.message || '验证码获取失败')
    }
  } catch (error) {
    passwordCaptcha.value = ''
    ElMessage.error('验证码获取失败')
  } finally {
    passwordCaptchaLoading.value = false
  }
}

const handleMenuSelect = (index) => {
  activeMenu.value = index
}

watch(activeMenu, (value) => {
  if (value === 'security') {
    refreshPasswordCaptcha()
  }
})

const loadProfile = () => {
  if (userStore.user) {
    profileForm.username = userStore.user.username
    profileForm.phone = userStore.user.phone
    profileForm.email = userStore.user.email
    profileForm.realName = userStore.user.realName
    profileForm.gender = userStore.user.gender || 0
  }
}

onMounted(() => {
  // Ensure we have freshest profile including VIP info
  userStore.fetchProfile()
  loadProfile()
  if (activeMenu.value === 'security') {
    refreshPasswordCaptcha()
  }
})

// VIP helper
const getVipText = (level) => {
  if (level === 2) return '钻石'
  if (level === 1) return '黄金'
  return '普通'
}

const updateProfile = async () => {
  try {
    const res = await userStore.updateProfile({
      phone: profileForm.phone,
      email: profileForm.email,
      realName: profileForm.realName,
      gender: profileForm.gender
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const changePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    const res = await api.user.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      captcha: passwordForm.captcha
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    ElMessage.error('修改失败')
  } finally {
    passwordForm.captcha = ''
    refreshPasswordCaptcha()
  }
}

const copyInviteCode = () => {
  navigator.clipboard.writeText(userStore.user?.inviteCode || '')
  ElMessage.success('邀请码已复制')
}

</script>

<style scoped>
.user-center-page {
  max-width: 900px;
  margin: 0 auto;
}

.user-header {
  background: linear-gradient(135deg, #ffffff 0%, #ffffff 100%);
  border-radius: 12px;
  padding: 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-uploader :deep(.el-upload) {
  display: inline-block;
}

.avatar-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.avatar-wrapper.is-loading {
  cursor: default;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.avatar-wrapper.is-loading .avatar-mask {
  opacity: 1;
}

.user-details h2 {
  font-size: 24px;
  margin-bottom: 5px;
  color:#606266;
}

.user-details p {
  opacity: 0.9;
  margin-bottom: 8px;
  color:#606266;
}

.vip-info { margin-top: 8px; display:flex; gap:10px; align-items:center }
.vip-spend { color: #606266; font-size: 13px }

.user-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  text-align: center;
  color:#606266;
}

.stat-item .value {
  display: block;
  font-size: 28px;
  font-weight: bold;
}

.stat-item .label {
  font-size: 13px;
  opacity: 0.9;
}

.user-content {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.tab-content {
  padding: 20px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.captcha-row :deep(.el-input) {
  flex: 1;
}

.captcha-image {
  width: 110px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  object-fit: cover;
}

.captcha-image.is-loading {
  opacity: 0.6;
  pointer-events: none;
}

.verified-info {
  text-align: center;
}

.invite-section {
  text-align: center;
  padding: 30px;
}

.invite-section h3 {
  margin-bottom: 20px;
  color: #303133;
}

.invite-code {
  display: inline-flex;
  align-items: center;
  gap: 15px;
  background: #f5f7fa;
  padding: 15px 25px;
  border-radius: 8px;
  margin-bottom: 15px;
}

.invite-code span {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  letter-spacing: 3px;
}

.invite-tip {
  color: #909399;
  font-size: 13px;
}
</style>
