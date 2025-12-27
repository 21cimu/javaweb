<template>
  <div class="admin-stores">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="城市">
          <el-select v-model="filterForm.city" placeholder="全部城市" clearable style="width: 150px;">
            <el-option v-for="city in cities" :key="city" :label="city" :value="city" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="营业中" :value="1" />
            <el-option label="关闭" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadStores">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>门店管理</span>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon> 新增门店
          </el-button>
        </div>
      </template>

      <el-table :data="stores" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="门店名称" width="180" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="district" label="区县" width="100" />
        <el-table-column prop="address" label="详细地址" min-width="200" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="businessHours" label="营业时间" width="120" />
        <el-table-column label="车辆数" width="120">
          <template #default="{ row }">
            <span>{{ row.availableCount }}/{{ row.vehicleCount }}</span>
            <span class="count-tip">(可用/总数)</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '营业中' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="editStore(row)">编辑</el-button>
            <el-button v-if="row.status === 1" link type="warning" @click="closeStore(row)">关闭</el-button>
            <el-button v-else link type="success" @click="openStore(row)">开启</el-button>
            <el-button link type="danger" @click="deleteStore(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadStores"
        @current-change="loadStores"
        class="pagination"
      />
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑门店' : '新增门店'" width="600px">
      <el-form :model="storeForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="门店名称" prop="name">
          <el-input v-model="storeForm.name" placeholder="输入门店名称" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-select v-model="storeForm.city" placeholder="选择城市" style="width: 100%;">
            <el-option v-for="city in cities" :key="city" :label="city" :value="city" />
          </el-select>
        </el-form-item>
        <el-form-item label="区县">
          <el-input v-model="storeForm.district" placeholder="输入区县" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="storeForm.address" placeholder="输入详细地址" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="storeForm.phone" placeholder="输入联系电话" />
        </el-form-item>
        <el-form-item label="营业时间">
          <el-input v-model="storeForm.businessHours" placeholder="如: 08:00-22:00" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="storeForm.longitude" :precision="7" :step="0.0001" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="storeForm.latitude" :precision="7" :step="0.0001" />
        </el-form-item>
        <el-form-item label="地图选点">
          <div class="map-wrapper">
            <div ref="mapRef" class="store-map"></div>
            <div class="map-tip">点击地图选择门店位置</div>
          </div>
        </el-form-item>
        <el-form-item label="门店图片">
          <el-input v-model="storeForm.image" placeholder="输入图片URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStore">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const stores = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const mapRef = ref(null)

const amapKey = 'f7db0589e08b284eeef6a67627f220e0'
const amapSecurityJsCode = 'b13984a389ab1a2635a44ab6a71eb45f'
let mapInstance = null
let mapMarker = null
let geocoder = null

const cities = ref(['北京', '上海', '广州', '深圳', '成都', '杭州', '南京', '武汉'])

const filterForm = reactive({
  city: '',
  status: null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const storeForm = reactive({
  id: null,
  name: '',
  city: '',
  district: '',
  address: '',
  phone: '',
  businessHours: '08:00-22:00',
  longitude: 116.4074,
  latitude: 39.9042,
  image: ''
})

const rules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  city: [{ required: true, message: '请选择城市', trigger: 'change' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

// 本地 mock 数据源
const allStores = ref([])

const buildMockStores = () => {
  allStores.value = [
    { id: 1, name: '北京朝阳门店', city: '北京', district: '朝阳区', address: '北京市朝阳区建国路88号', phone: '010-12345678', businessHours: '08:00-22:00', vehicleCount: 10, availableCount: 8, status: 1 },
    { id: 2, name: '北京海淀门店', city: '北京', district: '海淀区', address: '北京市海淀区中关村大街1号', phone: '010-87654321', businessHours: '08:00-22:00', vehicleCount: 8, availableCount: 6, status: 1 },
    { id: 3, name: '上海浦东门店', city: '上海', district: '浦东新区', address: '上海市浦东新区陆家嘴环路1000号', phone: '021-12345678', businessHours: '08:00-22:00', vehicleCount: 12, availableCount: 10, status: 1 },
    { id: 4, name: '广州天河门店', city: '广州', district: '天河区', address: '广州市天河区天河路385号', phone: '020-12345678', businessHours: '08:00-22:00', vehicleCount: 10, availableCount: 8, status: 0 },
    { id: 5, name: '深圳福田门店', city: '深圳', district: '福田区', address: '深圳市福田区深南大道6008号', phone: '0755-12345678', businessHours: '08:00-22:00', vehicleCount: 8, availableCount: 6, status: 1 }
  ]
}

// 基于本地数据做筛选 + 分页
const applyFilterAndPaging = () => {
  let data = allStores.value
  if (filterForm.city) {
    data = data.filter(s => s.city === filterForm.city)
  }
  if (filterForm.status !== null && filterForm.status !== undefined) {
    data = data.filter(s => s.status === filterForm.status)
  }

  pagination.total = data.length

  const start = (pagination.page - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  stores.value = data.slice(start, end)
}

const loadStores = () => {
  loading.value = true
  try {
    if (!allStores.value.length) {
      buildMockStores()
    }
    applyFilterAndPaging()
  } catch (e) {
    console.error('加载门店失败', e)
    ElMessage.error('加载门店列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.city = ''
  filterForm.status = null
  pagination.page = 1
  loadStores()
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(storeForm, {
    id: null,
    name: '',
    city: '',
    district: '',
    address: '',
    phone: '',
    businessHours: '08:00-22:00',
    longitude: 116.4074,
    latitude: 39.9042,
    image: ''
  })
  dialogVisible.value = true
}

const editStore = (store) => {
  isEdit.value = true
  Object.assign(storeForm, {
    id: store.id,
    name: store.name,
    city: store.city,
    district: store.district,
    address: store.address,
    phone: store.phone,
    businessHours: store.businessHours,
    longitude: store.longitude || 116.4074,
    latitude: store.latitude || 39.9042,
    image: store.image || ''
  })
  dialogVisible.value = true
}

const loadAmapScript = () => new Promise((resolve, reject) => {
  if (window.AMap) {
    resolve(window.AMap)
    return
  }
  const existing = document.getElementById('amap-js')
  if (existing) {
    existing.addEventListener('load', () => resolve(window.AMap))
    existing.addEventListener('error', reject)
    return
  }
  window._AMapSecurityConfig = { securityJsCode: amapSecurityJsCode }
  const script = document.createElement('script')
  script.id = 'amap-js'
  script.src = `https://webapi.amap.com/maps?v=2.0&key=${amapKey}`
  script.async = true
  script.onload = () => resolve(window.AMap)
  script.onerror = reject
  document.head.appendChild(script)
})

const normalizeCityName = (value) => (value ? value.replace(/市$/, '') : '')

const resolveCityName = (city, province) => {
  let value = ''
  if (Array.isArray(city)) {
    value = city.length ? city[0] : ''
  } else if (city) {
    value = city
  } else if (province && /市$/.test(province)) {
    value = province
  }
  return normalizeCityName(value)
}

const ensureGeocoder = () => new Promise((resolve) => {
  if (geocoder) {
    resolve(geocoder)
    return
  }
  window.AMap.plugin('AMap.Geocoder', () => {
    geocoder = new window.AMap.Geocoder({ radius: 1000, extensions: 'all' })
    resolve(geocoder)
  })
})

const fillAddressFromLocation = async (lng, lat) => {
  try {
    const geo = await ensureGeocoder()
    geo.getAddress([lng, lat], (status, result) => {
      if (status === 'complete' && result.regeocode) {
        const { addressComponent, formattedAddress } = result.regeocode
        if (formattedAddress) {
          storeForm.address = formattedAddress
        }
        if (addressComponent) {
          const cityName = resolveCityName(addressComponent.city, addressComponent.province)
          if (cityName) {
            storeForm.city = cityName
            if (!cities.value.includes(cityName)) {
              cities.value.push(cityName)
            }
          }
          if (addressComponent.district) {
            storeForm.district = addressComponent.district
          }
        }
      } else {
        ElMessage.warning('未能解析该位置地址')
      }
    })
  } catch (error) {
    console.error('解析位置失败', error)
    ElMessage.warning('未能解析该位置地址')
  }
}

const setMapMarker = (lng, lat, syncForm = true) => {
  if (!mapInstance || lng === null || lat === null || lng === undefined || lat === undefined) return
  const position = new window.AMap.LngLat(lng, lat)
  if (!mapMarker) {
    mapMarker = new window.AMap.Marker({ position })
    mapInstance.add(mapMarker)
  } else {
    mapMarker.setPosition(position)
  }
  if (syncForm) {
    storeForm.longitude = Number(Number(lng).toFixed(7))
    storeForm.latitude = Number(Number(lat).toFixed(7))
  }
}

const initMap = async () => {
  if (!mapRef.value) return
  try {
    await loadAmapScript()
    if (!mapInstance) {
      mapInstance = new window.AMap.Map(mapRef.value, {
        zoom: 13,
        center: [storeForm.longitude, storeForm.latitude]
      })
      mapInstance.on('click', (e) => {
        const { lng, lat } = e.lnglat
        setMapMarker(lng, lat, true)
        fillAddressFromLocation(lng, lat)
      })
    }
    setMapMarker(storeForm.longitude, storeForm.latitude, false)
    mapInstance.setCenter([storeForm.longitude, storeForm.latitude])
    mapInstance.resize()
  } catch (error) {
    console.error('加载高德地图失败', error)
    ElMessage.error('地图加载失败，请稍后重试')
  }
}

const saveStore = async () => {
  try {
    await formRef.value.validate()
    // Call API to save
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadStores()
  } catch (error) {
    console.error('Validation failed:', error)
  }
}

const closeStore = async (store) => {
  try {
    await ElMessageBox.confirm('确认关闭该门店?', '确认')
    ElMessage.success('已关闭')
    loadStores()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed:', error)
  }
}

const openStore = async (store) => {
  try {
    await ElMessageBox.confirm('确认开启该门店?', '确认')
    ElMessage.success('已开启')
    loadStores()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed:', error)
  }
}

const deleteStore = async (store) => {
  try {
    await ElMessageBox.confirm('确认删除该门店?', '确认')
    ElMessage.success('已删除')
    loadStores()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed:', error)
  }
}

watch(() => dialogVisible.value, async (visible) => {
  if (visible) {
    await nextTick()
    await initMap()
  }
})

watch(() => [storeForm.longitude, storeForm.latitude], ([lng, lat]) => {
  if (!dialogVisible.value || !mapInstance) return
  setMapMarker(lng, lat, false)
})

onMounted(() => {
  loadStores()
})

onBeforeUnmount(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
    mapMarker = null
  }
})
</script>

<style scoped>
.admin-stores {
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

.count-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 5px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.map-wrapper {
  width: 100%;
}

.store-map {
  width: 100%;
  height: 260px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.map-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
</style>
