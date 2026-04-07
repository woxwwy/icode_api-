import axios from 'axios'

// 创建axios实例
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081',    
  timeout: 10000        // 请求超时时间
})

// 请求拦截器
client.interceptors.request.use(
  config => {
    // 2026-03-27 新增：将限流配置添加到请求头
    const mode = localStorage.getItem('rate_limit_mode')
    const threshold = localStorage.getItem('rate_limit_threshold')
    if (mode) {
      config.headers['X-RateLimit-Mode'] = mode === 'real' ? 'real' : 'mock'
    }
    if (threshold) {
      config.headers['X-RateLimit-Threshold'] = threshold
    }
    return config
  },
  error => {
    console.warn('请求错误:', error)
    return Promise.reject(error)         // 返回被拒绝的Promise，捕获错误
  }
)

// 响应拦截器
client.interceptors.response.use(
  response => {
    return response.data           //只return了response.data，调用时直接拿到后端数据，不需要再res.data了
  },
  error => {
    console.warn('响应错误:', error)
    return Promise.reject(error)
  }
)

export default client         //暴露实例，后续调用
