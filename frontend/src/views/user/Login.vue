<template>
  <div class="login-page">
    <div class="box">
      <div class="square" style="--i:0;"></div>
      <div class="square" style="--i:1;"></div>
      <div class="square" style="--i:2;"></div>
      <div class="square" style="--i:3;"></div>
      <div class="square" style="--i:4;"></div>
      <div class="square" style="--i:5;"></div>

      <div class="container">
        <div class="form">
          <h2>登录</h2>

          <el-form ref="formRef" :model="form" :rules="rules">
            <el-form-item prop="username">
              <div class="inputBx">
                <el-input v-model="form.username" placeholder="" size="large" clearable></el-input>
                <span>账号</span>
                <i class="fas fa-user-circle"></i>
              </div>
            </el-form-item>

            <el-form-item prop="password">
              <div class="inputBx password">
                <el-input v-model="form.password" type="password" placeholder="" size="large" show-password></el-input>
                <span>密码</span>
                <i class="fas fa-key"></i>
              </div>
            </el-form-item>

            <el-form-item>
              <div class="form-options">
                <el-checkbox v-model="form.remember">记住我</el-checkbox>
                <a href="#" class="forgot-link">忘记密码？</a>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" size="large" block :loading="loading" @click="handleLogin">登录</el-button>
            </el-form-item>
          </el-form>

          <div class="login-footer">
            <span>还没有账号？</span>
            <router-link to="/register">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    const res = await userStore.login(
      {
        username: form.username,
        password: form.password
      },
      { remember: form.remember }
    )
    
    if (res.code === 200) {
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/'
      router.push(redirect)
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=El+Messiri:wght@700&display=swap');

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(-45deg, #ee7752, #e73c7e, #23a6d5, #23d5ab);
  background-size: 400% 400%;
  animation: gradient 10s ease infinite;
  font-family: 'El Messiri', sans-serif;
}

@keyframes gradient {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.box {
  position: relative;
}

.square {
  position: absolute;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
  box-shadow: 0 25px 45px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 15px;
  animation: square 10s linear infinite;
  animation-delay: calc(-1s * var(--i));
}

@keyframes square {
  0%,100% { transform: translateY(-20px); }
  50% { transform: translateY(20px); }
}

.square:nth-child(1) { width: 100px; height: 100px; top: -15px; right: -45px; }
.square:nth-child(2) { width: 150px; height: 150px; top: 105px; left: -125px; z-index: 2; }
.square:nth-child(3) { width: 60px; height: 60px; bottom: 85px; right: -45px; z-index: 2; }
.square:nth-child(4) { width: 50px; height: 50px; bottom: 35px; left: -95px; }
.square:nth-child(5) { width: 50px; height: 50px; top: -15px; left: -25px; }
.square:nth-child(6) { width: 85px; height: 85px; top: 165px; right: -155px; z-index: 2; }

.container {
  position: relative;
  padding: 40px;
  width: 420px;
  min-height: 420px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(6px);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.container::after {
  content: '';
  position: absolute;
  top: 6px;
  right: 6px;
  bottom: 6px;
  left: 6px;
  border-radius: 8px;
  pointer-events: none;
  background: linear-gradient(to bottom, rgba(255,255,255,0.04), rgba(255,255,255,0.02));
}

.form {
  position: relative;
  width: 100%;
}

.form h2 {
  color: #fff;
  letter-spacing: 2px;
  margin-bottom: 22px;
  text-align: center;
}

.inputBx {
  position: relative;
  width: 100%;
  margin-bottom: 18px;
}

.inputBx .el-input__inner {
  width: 100%;
  outline: none;
  border: 1px solid rgba(255,255,255,0.18);
  background: rgba(255,255,255,0.06);
  padding: 10px 12px 10px 44px;
  border-radius: 12px;
  color: #fff;
  font-size: 15px;
  box-shadow: 0 5px 15px rgba(0,0,0,0.05);
}

.inputBx .el-input__inner::placeholder { color: rgba(255,255,255,0.6); }

.inputBx .password .el-input__inner { padding-right: 44px; }

.inputBx .password .el-input__suffix { right: 10px; }

.inputBx span {
  position: absolute;
  left: 34px;
  top: 12px;
  padding: 2px 6px;
  display: inline-block;
  color: #44c4c4;
  transition: .35s;
  pointer-events: none;
}

/* when input is focused move the label */
.inputBx:focus-within span {
  transform: translateX(-18px) translateY(-26px);
  font-size: 12px;
  opacity: 0.95;
}

.inputBx .fas {
  position: absolute;
  top: 12px;
  left: 10px;
  color: rgba(255,255,255,0.8);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-top: 6px;
  margin-bottom: 8px;
}

.forgot-link {
  color: #bfe4ff;
  text-decoration: none;
  font-size: 13px;
}

.login-footer {
  text-align: center;
  margin-top: 18px;
  font-size: 14px;
  color: rgba(255,255,255,0.85);
}

.login-footer a {
  color: #bfe4ff;
  text-decoration: none;
  margin-left: 6px;
}

/* style Element Plus button to fit */
.el-button--primary {
  background: linear-gradient(115deg, rgba(255,255,255,0.12), rgba(255,255,255,0.02));
  border-color: rgba(255,255,255,0.1);
  color: #fff;
}

.el-button--primary.is-disabled { opacity: 0.6; }
</style>
