import axios from 'axios'

// 网络延迟检测（网关健康检查）
export const getPing = () => {
  console.log('调用ping接口...')
  return axios.get('http://localhost:8081/ping')
    .then(res => {
      console.log('成功收到:', res.data)
      return res.data
    })
    .catch(error => {
      console.error('请求失败:', error.message)
      if (error.response) {
        console.error('响应状态:', error.response.status)
      }
      return '请求失败'
    })
}

// Redis 连通性测试（调用新的结构化接口）
export const getRedisStatus = () => {
  console.log('获取 Redis 状态...')
  return axios.get('http://localhost:8081/api/gateway/redis/status')
    .then(res => {
      console.log('Redis状态:', res.data)
      return res.data
    })
    .catch(error => {
      console.error('Redis状态请求失败:', error)
      return { connected: false, error: error.message }
    })
}

// 原有获取路由列表的函数（根据实际调整）
export const getRoutes = () => {
  // 如果使用模拟数据
  return import('@/mock/mockroutes.js').then(module => module.getRoutes())
  // 如果使用真实后端接口，改为 axios.get(...)
}
