import client from './client'

// 获取路由列表
export const getRoutes = () => {
  return client.get('/routes')
}
