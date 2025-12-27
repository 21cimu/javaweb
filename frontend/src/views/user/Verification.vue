<template>
  <div class="verification-page">
    <div class="page-header">
      <h1>实名认证</h1>
      <p>完成实名认证后才能租车下单</p>
    </div>
    
    <el-card shadow="never" class="form-card">
      <el-steps :active="step" finish-status="success" align-center>
        <el-step title="填写信息" />
        <el-step title="上传证件" />
        <el-step title="提交审核" />
      </el-steps>
      
      <!-- Step 1: Basic Info -->
      <div v-show="step === 0" class="step-content">
        <el-form ref="basicFormRef" :model="form" :rules="basicRules" label-width="120px">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="身份证号" prop="idCard">
            <el-input v-model="form.idCard" placeholder="请输入18位身份证号" />
          </el-form-item>
          <el-form-item label="驾驶证号" prop="driverLicense">
            <el-input v-model="form.driverLicense" placeholder="请输入13位驾驶证号" maxlength="13" inputmode="numeric" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="nextStep">下一步</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- Step 2: Upload Documents -->
      <div v-show="step === 1" class="step-content">
        <el-form label-width="120px">
          <el-form-item label="身份证正面">
            <el-upload
              class="upload-demo"
              action="#"
              :auto-upload="false"
              :show-file-list="false"

              :on-change="(file) => handleSelect(file, 'idCardFront')"
            >
              <div v-if="form.idCardFront" class="upload-preview">
                <img :src="form.idCardFront" alt="身份证正面">
              </div>
              <div v-else class="upload-placeholder">
                <el-icon><Plus /></el-icon>
                <span>上传身份证人像面</span>
              </div>
            </el-upload>
          </el-form-item>
          
          <el-form-item label="身份证背面">
            <el-upload
              class="upload-demo"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="(file) => handleSelect(file, 'idCardBack')"
            >
              <div v-if="form.idCardBack" class="upload-preview">
                <img :src="form.idCardBack" alt="身份证背面">
              </div>
              <div v-else class="upload-placeholder">
                <el-icon><Plus /></el-icon>
                <span>上传身份证国徽面</span>
              </div>
            </el-upload>
          </el-form-item>
          
          <el-form-item label="驾驶证">
            <el-upload
              class="upload-demo"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="(file) => handleSelect(file, 'driverLicenseImage')"
            >
              <div v-if="form.driverLicenseImage" class="upload-preview">
                <img :src="form.driverLicenseImage" alt="驾驶证">
              </div>
              <div v-else class="upload-placeholder">
                <el-icon><Plus /></el-icon>
                <span>上传驾驶证照片</span>
              </div>
            </el-upload>
          </el-form-item>
          
          <el-form-item>
            <el-button @click="step = 0">上一步</el-button>
            <el-button type="primary" @click="nextStep">下一步</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- Step 3: Confirm & Submit -->
      <div v-show="step === 2" class="step-content">
        <el-descriptions title="确认信息" :column="1" border>
          <el-descriptions-item label="真实姓名">{{ form.realName }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ maskIdCard(form.idCard) }}</el-descriptions-item>
          <el-descriptions-item label="驾驶证号">{{ form.driverLicense }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="preview-images">
          <div class="preview-item" v-if="form.idCardFront">
            <img :src="form.idCardFront" alt="身份证正面">
            <span>身份证正面</span>
          </div>
          <div class="preview-item" v-if="form.idCardBack">
            <img :src="form.idCardBack" alt="身份证背面">
            <span>身份证背面</span>
          </div>
          <div class="preview-item" v-if="form.driverLicenseImage">
            <img :src="form.driverLicenseImage" alt="驾驶证">
            <span>驾驶证</span>
          </div>
        </div>
        
        <el-alert type="warning" :closable="false" show-icon style="margin: 20px 0;">
          请确保上传的证件清晰可见，信息真实有效。提交后将进入人工审核，预计1-2个工作日内完成。
        </el-alert>
        
        <div class="form-actions">
          <el-button @click="step = 1">上一步</el-button>
          <el-button type="primary" :loading="loading" @click="submitVerification">提交认证</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()

const step = ref(0)
const loading = ref(false)
const basicFormRef = ref(null)

// form stores preview/src or server path
const form = reactive({
  realName: '',
  idCard: '',
  driverLicense: '',
  idCardFront: '',
  idCardBack: '',
  driverLicenseImage: ''
})

// uploadedFiles stores selected File objects (not uploaded yet)
const uploadedFiles = reactive({
  idCardFront: null,
  idCardBack: null,
  driverLicenseImage: null
})

const basicRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, message: '姓名至少2个字符', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '请输入有效的身份证号', trigger: 'blur' }
  ],
  driverLicense: [
    { required: true, message: '请输入驾驶证号', trigger: 'blur' },
    { pattern: /^\d{13}$/, message: '驾驶证号需为13位有效数字', trigger: 'blur' }
  ]
}

const maskIdCard = (idCard) => {
  if (!idCard || idCard.length < 8) return idCard
  return idCard.slice(0, 4) + '**********' + idCard.slice(-4)
}

// When user selects a file: create local preview and keep File in memory.
const handleSelect = (file, field) => {
  try {
    // normalize to a File object
    let actualFile = null
    if (!file) {
      console.warn('handleSelect called with empty file for', field)
      return
    }
    if (file.raw && file.raw instanceof File) actualFile = file.raw
    else if (file.file && file.file instanceof File) actualFile = file.file
    else if (file instanceof File) actualFile = file
    else if (file.raw && file.raw.file && file.raw.file instanceof File) actualFile = file.raw.file

    if (!actualFile) {
      console.warn('Cannot find File object in upload callback:', file)
      ElMessage.error('选择的文件格式不受支持')
      return
    }

    // create object URL for immediate preview
    if (uploadedFiles[field] && uploadedFiles[field].previewUrl) {
      try { URL.revokeObjectURL(uploadedFiles[field].previewUrl) } catch (e) {}
    }
    const objectUrl = URL.createObjectURL(actualFile)
    form[field] = objectUrl

    // store File for later upload
    uploadedFiles[field] = {
      file: actualFile,
      previewUrl: objectUrl
    }

    console.log('handleSelect:', field, actualFile.name, actualFile.size, 'bytes')
  } catch (err) {
    console.error('handleSelect error', err)
    ElMessage.error('选择图片失败')
  }
}

const nextStep = async () => {
  if (step.value === 0) {
    const valid = await basicFormRef.value.validate().catch(() => false)
    if (!valid) return
    step.value = 1
  } else if (step.value === 1) {
    if (!form.idCardFront) {
      ElMessage.warning('请上传身份证正面')
      return
    }
    if (!form.idCardBack) {
      ElMessage.warning('请上传身份证背面')
      return
    }
    if (!form.driverLicenseImage) {
      ElMessage.warning('请上传驾驶证')
      return
    }
    step.value = 2
  }
}

const submitVerification = async () => {
  loading.value = true
  try {
    // First, upload any selected files (that are stored in uploadedFiles)
    const fileFields = ['idCardFront', 'idCardBack', 'driverLicenseImage']
    // upload in parallel
    const uploadPromises = fileFields.map(async (field) => {
      const entry = uploadedFiles[field]
      // If there's no local file, skip (maybe already server path in form[field])
      if (entry && entry.file) {
        const formData = new FormData()
        formData.append('file', entry.file)
        const res = await api.upload.file(formData)
        if (res && res.code === 200) {
          const data = res.data
          const url = (data && data.url) ? data.url : data
          form[field] = url
          // clear uploadedFiles entry and revoke preview
          try { URL.revokeObjectURL(entry.previewUrl) } catch (e) {}
          uploadedFiles[field] = null
        } else {
          throw new Error(res?.message || '上传文件失败')
        }
      }
    })

    await Promise.all(uploadPromises)

    // Now submit verification with server paths in form
    const res = await api.user.submitVerification({
      realName: form.realName,
      idCard: form.idCard,
      driverLicense: form.driverLicense,
      idCardFront: form.idCardFront,
      idCardBack: form.idCardBack,
      driverLicenseImage: form.driverLicenseImage
    })

    if (res.code === 200) {
      ElMessage.success('提交成功，请等待审核')
      router.push('/user')
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    console.error('submitVerification error', error)
    ElMessage.error(error?.message || '提交失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.verification-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 5px;
}

.page-header p {
  color: #909399;
}

.form-card {
  padding: 20px;
}

.step-content {
  margin-top: 40px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.upload-demo {
  width: 200px;
}

.upload-placeholder {
  width: 200px;
  height: 130px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder:hover {
  border-color: #409EFF;
}

.upload-placeholder .el-icon {
  font-size: 28px;
  color: #909399;
  margin-bottom: 8px;
}

.upload-placeholder span {
  font-size: 12px;
  color: #909399;
}

.upload-preview {
  width: 200px;
  height: 130px;
  border-radius: 8px;
  overflow: hidden;
}

.upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-images {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

.preview-item {
  text-align: center;
}

.preview-item img {
  width: 150px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
}

.preview-item span {
  display: block;
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
</style>
