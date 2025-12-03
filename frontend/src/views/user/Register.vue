<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <h1>注册</h1>
        <p>创建畅行租车账号</p>
      </div>
      
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" size="large">
            <template #prefix>
              <el-icon><Phone /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请设置密码" size="large" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" size="large" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="邀请码（选填）" prop="inviteCode">
          <el-input v-model="form.inviteCode" placeholder="输入邀请码可获得积分奖励" size="large">
            <template #prefix>
              <el-icon><Ticket /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-checkbox v-model="form.agree">
            我已阅读并同意
            <a href="#">《用户协议》</a>
            和
            <a href="#">《隐私政策》</a>
          </el-checkbox>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" size="large" block :loading="loading" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { User, Phone, Lock, Ticket } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  inviteCode: '',
  agree: false
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名3-20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  if (!form.agree) {
    ElMessage.warning('请阅读并同意用户协议')
    return
  }
  
  loading.value = true
  try {
    const res = await userStore.register({
      username: form.username,
      phone: form.phone,
      password: form.password,
      inviteCode: form.inviteCode
    })
    
    if (res.code === 200) {
      ElMessage.success('注册成功')
      router.push('/')
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.register-container {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 8px;
}

.register-header p {
  color: #909399;
}

.el-checkbox {
  font-size: 13px;
}

.el-checkbox a {
  color: #409EFF;
  text-decoration: none;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #606266;
}

.register-footer a {
  color: #409EFF;
  text-decoration: none;
  margin-left: 5px;
}
</style>
