/**
 * 限流测试页面的 mock 数据接口
 * 2026-03-27 创建，用于模拟后端限流行为
 * 真实后端接入后，这部分会被真实 API 替换
 */

// 模拟成功/限流的随机结果
export const sendMockRequest = () => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      // 模拟 30% 的概率被限流（429）
      const isLimited = Math.random() < 0.3
      if (isLimited) {
        reject({ status: 429, message: 'Too Many Requests' })
      } else {
        resolve({ status: 200, message: 'Success' })
      }
    }, 50) // 模拟网络延迟
  })
}
