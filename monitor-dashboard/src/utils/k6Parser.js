/**
 * k6 压测结果解析工具
 * 2026-03-27 创建，用于解析 summary.json 并提取关键指标
 */

/**
 * 解析 k6 summary.json 文件内容
 * @param {Object} data - 解析后的 JSON 对象
 * @returns {Object} 包含总请求数、平均延迟、p99、错误率等指标
 */
export function parseK6Summary(data) {
  // 检查必要的 metrics 字段
  const metrics = data.metrics || {}

  // 总请求数：http_reqs 的 count
  const totalRequests = metrics.http_reqs?.values?.count || 0

  // 平均延迟：http_req_duration 的 avg
  const avgLatency = metrics.http_req_duration?.values?.avg || 0

  // p99 延迟：http_req_duration 的 p(99)
  const p99Latency = metrics.http_req_duration?.values?.['p(99)'] || 0

  // 错误率：http_req_failed 的 rate（或从 failed requests 计算）
  let errorRate = 0
  if (metrics.http_req_failed?.values?.rate !== undefined) {
    errorRate = metrics.http_req_failed.values.rate * 100 // 转为百分比
  } else if (metrics.errors?.values?.rate !== undefined) {
    errorRate = metrics.errors.values.rate * 100
  } else if (metrics.http_reqs?.values?.rate && metrics.http_req_failed?.values?.rate) {
    // 如果存在失败率，直接使用
    errorRate = metrics.http_req_failed.values.rate * 100
  }

  // 平均 QPS（从 http_reqs 的 rate 获取）
  const avgQPS = metrics.http_reqs?.values?.rate || 0

  // 测试持续时间（秒）
  const duration = metrics.iteration_duration?.values?.avg || 0

  return {
    totalRequests,
    avgLatency,
    p99Latency,
    errorRate,
    avgQPS,
    duration
  }
}

/**
 * 生成模拟的 QPS 时间序列数据（用于图表展示）
 * 如果 summary.json 中没有时间序列，基于总请求数和平均 QPS 构造一个模拟曲线
 * @param {number} totalRequests - 总请求数
 * @param {number} avgQPS - 平均 QPS
 * @param {number} duration - 持续时间（秒）
 * @returns {Array} 时间序列点 [{ time: number, qps: number }]
 */
export function generateMockTimeSeries(totalRequests, avgQPS, duration) {
  if (!duration || duration <= 0) {
    // 默认生成 60 秒的模拟数据
    duration = 60
    avgQPS = totalRequests / duration || 10
  }

  const points = []
  const step = 1 // 每秒一个点
  for (let t = 0; t <= duration; t += step) {
    // 模拟正弦波动，峰值在 1.5 倍平均 QPS，谷值在 0.5 倍平均 QPS
    const factor = 1 + 0.5 * Math.sin(t * 0.2) // 周期约 31.4 秒
    const qps = Math.max(0, Math.round(avgQPS * factor))
    points.push({ time: t, qps })
  }
  return points
}
