<template>
  <div class="benchmark-container">
    <div class="page-header">
      <div class="title-section">
        <h1>压测数据看板</h1>
        <p class="subtitle">上传 k6 生成的 summary.json，可视化展示性能指标</p>
      </div>
    </div>

    <el-card class="upload-card" shadow="hover">
      <el-upload
        class="upload-area"
        drag
        :before-upload="handleBeforeUpload"
        :show-file-list="false"
        accept=".json"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">点击或拖拽上传 summary.json 文件</div>
        <div class="upload-hint">仅支持 k6 生成的 summary.json 格式</div>
      </el-upload>
    </el-card>

    <!-- 指标卡片 -->
    <div v-if="metrics" class="metrics-grid">
      <el-card class="metric-card" shadow="hover">
        <div class="metric-icon total">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">总请求数</div>
          <div class="metric-value">{{ formatNumber(metrics.totalRequests) }}</div>
        </div>
      </el-card>

      <el-card class="metric-card" shadow="hover">
        <div class="metric-icon latency">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">平均延迟</div>
          <div class="metric-value">{{ formatLatency(metrics.avgLatency) }}</div>
        </div>
      </el-card>

      <el-card class="metric-card" shadow="hover">
        <div class="metric-icon p99">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">P99 延迟</div>
          <div class="metric-value">{{ formatLatency(metrics.p99Latency) }}</div>
        </div>
      </el-card>

      <el-card class="metric-card" shadow="hover">
        <div class="metric-icon error">
          <el-icon><CircleClose /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">错误率</div>
          <div class="metric-value" :class="{ 'error-value': metrics.errorRate > 0 }">
            {{ formatPercent(metrics.errorRate) }}
          </div>
        </div>
      </el-card>
    </div>

    <!-- QPS 曲线图 -->
    <el-card v-if="chartData" class="chart-card" shadow="hover">
      <template #header>
        <div class="chart-header">
          <span>QPS 变化趋势</span>
          <el-tag size="small" type="info">模拟数据（基于测试结果估算）</el-tag>
        </div>
      </template>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>

    <!-- 原始 JSON 预览（可选） -->
    <el-card v-if="rawJson" class="json-card" shadow="hover">
      <template #header>
        <div class="json-header">
          <span>原始数据（部分）</span>
          <el-button type="text" size="small" @click="copyJson">复制 JSON</el-button>
        </div>
      </template>
      <pre class="json-preview">{{ formatJsonPreview(rawJson) }}</pre>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { ElCard, ElUpload, ElTag, ElButton, ElMessage, ElIcon } from 'element-plus'
import {
  UploadFilled,
  DataLine,
  Timer,
  TrendCharts,
  CircleClose
} from '@element-plus/icons-vue'
import { parseK6Summary, generateMockTimeSeries } from '@/utils/k6Parser'

// 数据状态
const metrics = ref(null)
const chartData = ref(null)
const rawJson = ref(null)
let chart = null
const chartRef = ref(null)

// 上传前处理文件
const handleBeforeUpload = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const jsonData = JSON.parse(e.target.result)
      rawJson.value = jsonData
      // 解析指标
      const parsed = parseK6Summary(jsonData)
      metrics.value = parsed

      // 生成模拟 QPS 时间序列
      const timeSeries = generateMockTimeSeries(parsed.totalRequests, parsed.avgQPS, parsed.duration)
      chartData.value = timeSeries

      // 渲染图表
      renderChart(timeSeries)

      ElMessage.success('解析成功！')
    } catch (err) {
      console.error(err)
      ElMessage.error('文件解析失败，请确保是有效的 JSON 格式')
    }
  }
  reader.readAsText(file)
  return false // 阻止自动上传
}

// 格式化数字
const formatNumber = (num) => {
  if (num >= 1e6) return (num / 1e6).toFixed(2) + 'M'
  if (num >= 1e3) return (num / 1e3).toFixed(2) + 'K'
  return num.toString()
}

// 格式化延迟（毫秒）
const formatLatency = (ms) => {
  if (ms >= 1000) return (ms / 1000).toFixed(2) + ' s'
  return ms.toFixed(2) + ' ms'
}

// 格式化百分比
const formatPercent = (rate) => {
  return rate.toFixed(2) + '%'
}

// 渲染图表
const renderChart = (timeSeries) => {
  if (!chartRef.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)

  const times = timeSeries.map(point => `${point.time}s`)
  const qpsValues = timeSeries.map(point => point.qps)

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '8%', right: '5%', top: '10%', bottom: '5%', containLabel: true },
    xAxis: {
      type: 'category',
      data: times,
      name: '时间',
      axisLabel: { rotate: 45, interval: Math.floor(times.length / 10) }
    },
    yAxis: {
      type: 'value',
      name: 'QPS'
    },
    series: [
      {
        name: 'QPS',
        type: 'line',
        data: qpsValues,
        smooth: true,
        lineStyle: { color: '#409eff', width: 2 },
        areaStyle: { opacity: 0.2, color: '#409eff' },
        symbol: 'none'
      }
    ]
  })
}

// 预览 JSON（限制长度）
const formatJsonPreview = (json) => {
  const str = JSON.stringify(json, null, 2)
  if (str.length > 1000) return str.slice(0, 1000) + '... (已截断)'
  return str
}

// 复制 JSON
const copyJson = () => {
  const text = JSON.stringify(rawJson.value, null, 2)
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 组件销毁时清理图表
onUnmounted(() => {
  if (chart) chart.dispose()
})
</script>

<style scoped>
.benchmark-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
  background-color: #f9fafb;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 2rem;
}
.title-section h1 {
  font-size: 1.875rem;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #1e3a8a);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin: 0;
}
.subtitle {
  color: #6b7280;
  margin-top: 0.25rem;
  font-size: 0.875rem;
}

.upload-card {
  margin-bottom: 2rem;
  border-radius: 16px;
}
.upload-area {
  text-align: center;
  padding: 2rem;
}
.upload-icon {
  font-size: 3rem;
  color: #409eff;
  margin-bottom: 1rem;
}
.upload-text {
  font-size: 1rem;
  color: #374151;
  margin-bottom: 0.5rem;
}
.upload-hint {
  font-size: 0.75rem;
  color: #9ca3af;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}
.metric-card {
  border-radius: 16px;
  display: flex;
  align-items: center;
  padding: 1rem;
  transition: transform 0.2s;
}
.metric-card:hover {
  transform: translateY(-4px);
}
.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 1rem;
  font-size: 1.5rem;
}
.metric-icon.total {
  background-color: #e6f7ff;
  color: #409eff;
}
.metric-icon.latency {
  background-color: #fff7e6;
  color: #e6a23c;
}
.metric-icon.p99 {
  background-color: #f4e6ff;
  color: #9c6ade;
}
.metric-icon.error {
  background-color: #ffe6e6;
  color: #f56c6c;
}
.metric-content {
  flex: 1;
}
.metric-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.25rem;
}
.metric-value {
  font-size: 1.5rem;
  font-weight: 600;
}
.error-value {
  color: #f56c6c;
}

.chart-card {
  margin-bottom: 2rem;
  border-radius: 16px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.chart-container {
  height: 400px;
  width: 100%;
}

.json-card {
  border-radius: 16px;
}
.json-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.json-preview {
  background-color: #f5f7fa;
  padding: 1rem;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 0.75rem;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
}
</style>
