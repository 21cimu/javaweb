import axios from 'axios'

// 独立使用一个 axios 实例，保持与全局拦截器行为一致可按需调整
const instance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

const afterSalesApi = {
  create(data) {
    return instance.post('/after-sales', data)
  }
}

export default afterSalesApi
