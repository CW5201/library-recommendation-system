import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'library_token'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 90000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.satoken = token
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload.code !== 'undefined') {
      if (payload.code !== 200) {
        ElMessage.error(payload.message || '请求失败')
        return Promise.reject(new Error(payload.message || '请求失败'))
      }
      return payload.data
    }
    return payload
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || (status === 401 ? '登录状态已失效，请重新登录' : '网络异常，请稍后重试')
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      const current = window.location.pathname + window.location.search
      if (!current.startsWith('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(current)}`
      }
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
