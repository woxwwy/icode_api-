import axios from 'axios'

// 创建axios实例
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081',
  timeout: 10000
})

// 请求拦截器
client.interceptors.request.use(
  config => {
    return config
  },
  error => {
    console.warn('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
client.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.warn('响应错误:', error)
    return Promise.reject(error)
  }
)

export default client
