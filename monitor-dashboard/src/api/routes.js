import axios from 'axios'

export const getPing = () => {
  console.log('🔄 调用ping接口...')
  
  return axios.get('http://localhost:8081/ping')
    .then(res => {
      console.log('✅ 成功收到:', res.data)
      return res.data  // 直接返回后端的数据
    })
    .catch(error => {
      console.error('❌ 请求失败:', error.message)
      if (error.response) {
        console.error('响应状态:', error.response.status)
      }
      return '请求失败'
    })
}
