<template>
  <div class="test-limit-container">
    <!-- 头部区域：标题 + 模式标签 + 实时统计面板 -->
    <div class="page-header">
      <div class="title-section">
        <h1>限流效果验证</h1>
        <p class="subtitle">API 网关 · 限流策略可视化测试</p>
      </div>
      <div class="header-right">
        <!-- 实时统计面板 (2026-04-02 新增) -->
        <div class="stats-panel">
          <div class="stat-item">
            <span class="stat-label">当前状态：</span>
            <span class="stat-value online">在线</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">最近1秒请求：</span>
            <span class="stat-value">{{ lastSecondCount }} 次</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">成功率：</span>
            <span class="stat-value" :class="{ high: lastSecondSuccessRate >= 80, low: lastSecondSuccessRate < 50 }">
              {{ lastSecondSuccessRate }}%
            </span>
            <span v-if="lastSecondTotal > 0" class="stat-detail">
              ({{ lastSecondSuccess }}成功/{{ lastSecondFail }}限流)
            </span>
          </div>
        </div>
        <div class="mode-badge">
          <el-tag type="info" effect="plain">{{ currentModeText }}</el-tag>
        </div>
      </div>
    </div>

    <!-- 控制卡片 -->
    <el-card class="control-card" shadow="hover">
      <div class="card-content">
        <div class="request-actions">
          <el-button type="primary" @click="sendSingleRequest" :icon="Promotion">
            发送请求
          </el-button>
          <el-button
            type="warning"
            :icon="autoAttackActive ? VideoPause : VideoPlay"
            @click="toggleAutoAttack"
          >
            {{ autoAttackActive ? '停止自动攻击' : '开始自动攻击' }}
          </el-button>
          <!-- 2026-04-02 新增：连续发送20次请求按钮 -->
          <el-button
            type="danger"
            :icon="Operation"
            @click="sendBatch20"
            :disabled="batchRunning"
          >
            连续发送20次请求
          </el-button>
          <el-button
            type="success"
            :icon="VideoCamera"
            @click="showScenePanel"
            :disabled="scenarioRunning"
          >
            运行演示场景
          </el-button>
        </div>

        <div class="counter-group">
          <div class="counter success">
            <el-icon><CircleCheck /></el-icon>
            <div class="counter-text">
              <span class="label">成功次数</span>
              <span class="number">{{ successCount }}</span>
            </div>
          </div>
          <div class="counter fail">
            <el-icon><Warning /></el-icon>
            <div class="counter-text">
              <span class="label">被限流次数</span>
              <span class="number">{{ failCount }}</span>
            </div>
          </div>
        </div>

        <div class="slider-group">
          <div class="slider-label">
            <el-icon><Odometer /></el-icon>
            <span>自动发送频率：{{ frequency }} req/s</span>
          </div>
          <el-slider v-model="frequency" :min="1" :max="100" :step="1" show-stops />
        </div>
      </div>
    </el-card>

    <!-- 2026-04-02 新增：20次请求结果列表区域 -->
    <div v-if="batchResults.length > 0" class="batch-results">
      <div class="batch-header">
        <span>批量请求结果（20次）</span>
        <el-button text size="small" @click="clearBatchResults">清空</el-button>
      </div>
      <div class="result-list">
        <div
          v-for="(result, idx) in batchResults"
          :key="idx"
          class="result-item"
          :class="{ success: result.success, fail: !result.success }"
        >
          {{ idx + 1 }}. {{ result.success ? '✅' : '❌' }}
        </div>
      </div>
    </div>

    <!-- 场景选择面板（内嵌在图表上方） -->
    <div v-if="showSceneSelector" class="scene-selector">
      <div class="scene-buttons">
        <el-button type="primary" plain @click="runScenario('A')">场景 A：正常用户（1 req/s，全部成功）</el-button>
        <el-button type="warning" plain @click="runScenario('B')">场景 B：突发流量（50次并发，红绿混合）</el-button>
        <el-button type="info" plain @click="runScenario('C')">场景 C：降级模式（模拟 Redis 离线，全量放行）</el-button>
        <el-button text @click="showSceneSelector = false">取消</el-button>
      </div>
    </div>

    <!-- 图表卡片 -->
    <el-card class="chart-card" shadow="hover">
      <template #header>
        <div class="chart-header">
          <span>实时监控</span>
          <el-tag size="small" type="success">最近 60 秒</el-tag>
        </div>
      </template>
      <!-- 场景运行提示（显示在图表区域内） -->
      <div v-if="scenarioHint" class="scene-hint">
        <el-alert :title="scenarioHint" type="info" :closable="true" @close="scenarioHint = ''" show-icon />
      </div>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'  // 2026-04-02 新增 nextTick
import * as echarts from 'echarts'
import {
  ElButton,
  ElCard,
  ElSlider,
  ElTag,
  ElMessage,
  ElIcon,
  ElAlert
} from 'element-plus'
import {
  Promotion,
  VideoPlay,
  VideoPause,
  CircleCheck,
  Warning,
  Odometer,
  VideoCamera,
  Operation   // 2026-04-02 新增图标
} from '@element-plus/icons-vue'
import { sendMockRequest } from '@/mock/mockLimit.js'
import client from '@/api/client'

// ---------- 计数器 ----------
const successCount = ref(0)
const failCount = ref(0)

// ---------- 自动攻击相关 ----------
const frequency = ref(10)
const autoAttackActive = ref(false)
let attackInterval = null

// ---------- 演示场景相关 ----------
const showSceneSelector = ref(false)
const scenarioRunning = ref(false)
const scenarioHint = ref('')

// ---------- 2026-04-02 新增：批量请求（20次）相关 ----------
const batchRunning = ref(false)
const batchResults = ref([])  // 存储 { success: boolean }

// ---------- 2026-04-02 新增：实时统计相关 ----------
const recentRequests = ref([])   // 存储 { timestamp, success }
let statsInterval = null
const lastSecondCount = ref(0)
const lastSecondSuccess = ref(0)
const lastSecondFail = ref(0)
const lastSecondSuccessRate = ref(0)
const lastSecondTotal = ref(0)

// 当前模式文本
const currentModeText = computed(() => {
  const mode = localStorage.getItem('rate_limit_mode')
  const threshold = localStorage.getItem('rate_limit_threshold')
  if (mode === 'real') {
    return `真实算法模式 (阈值: ${threshold || 100} QPS)`
  } else {
    return 'Mock 限流模式 (30% 429)'
  }
})

// ---------- 通用请求函数（供单次、批量、自动攻击复用）----------
// 返回值: { success: boolean, is429?: boolean, error?: any }
const performRequest = async () => {
  const mode = localStorage.getItem('rate_limit_mode')
  if (mode === 'real') {
    try {
      await client.post('/test-rate-limit', {})
      return { success: true }
    } catch (error) {
      if (error.response && error.response.status === 429) {
        return { success: false, is429: true }
      } else {
        return { success: false, error }
      }
    }
  } else {
    try {
      await sendMockRequest()
      return { success: true }
    } catch (error) {
      if (error.status === 429) {
        return { success: false, is429: true }
      } else {
        return { success: false, error }
      }
    }
  }
}

// 更新计数器和统计，useAlert 参数控制是否使用 alert（自动攻击时传 false）
const handleRequestResult = (result, useAlert = true) => {
  if (result.success) {
    successCount.value++
    recordRequest(true)
    addToRecentRequests(true)
  } else if (result.is429) {
    failCount.value++
    recordRequest(false)
    addToRecentRequests(false)
    if (useAlert) {
      alert('被限流了！')
    } else {
      ElMessage.error('被限流了！')
    }
  } else {
    if (useAlert) {
      alert('请求失败：' + (result.error?.message || '未知错误'))
    } else {
      ElMessage.error('请求失败：' + (result.error?.message || '未知错误'))
    }
  }
}

// 单次请求（使用 alert）
const sendSingleRequest = async () => {
  // 清除场景提示
  scenarioHint.value = ''
  const result = await performRequest()
  handleRequestResult(result, true)
}

// 2026-04-02 新增：静默请求（用于自动攻击，不使用 alert）
const sendSingleRequestSilent = async () => {
  const result = await performRequest()
  handleRequestResult(result, false)
}

// ---------- 2026-04-02 新增：批量发送20次请求（使用 alert）----------
const sendBatch20 = async () => {
  if (batchRunning.value) {
    ElMessage.warning('批量请求正在进行中，请稍后')
    return
  }
  if (autoAttackActive.value) {
    ElMessage.warning('请先停止自动攻击')
    return
  }
  batchRunning.value = true
  batchResults.value = []  // 清空上次结果
  let success = 0
  let fail = 0

  for (let i = 0; i < 20; i++) {
    const result = await performRequest()
    if (result.success) {
      success++
      batchResults.value.push({ success: true })
      // 更新全局计数和统计（复用函数）
      successCount.value++
      recordRequest(true)
      addToRecentRequests(true)
    } else if (result.is429) {
      fail++
      batchResults.value.push({ success: false })
      failCount.value++
      recordRequest(false)
      addToRecentRequests(false)
      alert('被限流了！')
    } else {
      // 其他错误，也记录为失败
      fail++
      batchResults.value.push({ success: false })
      alert('请求失败：' + (result.error?.message || '未知错误'))
    }
    // 间隔 100ms
    await new Promise(r => setTimeout(r, 100))
  }

  batchRunning.value = false
  // 最终统计弹窗
  alert(`测试完成：成功${success}次，被限流${fail}次`)
}

// 清空批量结果列表
const clearBatchResults = () => {
  batchResults.value = []
}

// ---------- 实时统计相关函数 ----------
const addToRecentRequests = (success) => {
  const now = Date.now()
  recentRequests.value.push({ timestamp: now, success })
  // 保留最近20条足够用于每秒统计，但为了精确，保留所有最近1秒的请求即可，不过我们会在每秒定时清理
  // 为了性能，限制数组长度不超过200
  if (recentRequests.value.length > 200) {
    recentRequests.value = recentRequests.value.slice(-100)
  }
}

// 更新统计面板（每秒执行）
const updateStats = () => {
  const now = Date.now()
  const oneSecondAgo = now - 1000
  // 过滤出最近1秒内的请求
  const recent = recentRequests.value.filter(r => r.timestamp >= oneSecondAgo)
  const total = recent.length
  const success = recent.filter(r => r.success).length
  const fail = total - success
  lastSecondCount.value = total
  lastSecondSuccess.value = success
  lastSecondFail.value = fail
  lastSecondTotal.value = total
  if (total === 0) {
    lastSecondSuccessRate.value = 0
  } else {
    lastSecondSuccessRate.value = Math.round((success / total) * 100)
  }
  // 清理超过1秒的旧记录（可选，避免数组无限增长）
  recentRequests.value = recentRequests.value.filter(r => r.timestamp >= oneSecondAgo)
}

// 启动统计定时器
const startStatsTimer = () => {
  if (statsInterval) clearInterval(statsInterval)
  statsInterval = setInterval(() => {
    updateStats()
  }, 1000)
}

const stopStatsTimer = () => {
  if (statsInterval) {
    clearInterval(statsInterval)
    statsInterval = null
  }
}

// 自动攻击（使用静默请求，避免弹窗阻塞）
const startAutoAttack = () => {
  if (attackInterval) clearInterval(attackInterval)
  const intervalMs = 1000 / frequency.value
  attackInterval = setInterval(() => {
    sendSingleRequestSilent()
  }, intervalMs)
}

const stopAutoAttack = () => {
  if (attackInterval) {
    clearInterval(attackInterval)
    attackInterval = null
  }
}

const toggleAutoAttack = () => {
  scenarioHint.value = ''
  autoAttackActive.value = !autoAttackActive.value
  if (autoAttackActive.value) {
    startAutoAttack()
  } else {
    stopAutoAttack()
  }
}

watch(frequency, () => {
  if (autoAttackActive.value) {
    startAutoAttack()
  }
})

// ---------- 演示场景函数 ----------
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

// 2026-04-02 新增辅助函数：强制更新场景提示
const setScenarioHint = async (hint) => {
  scenarioHint.value = ''
  await nextTick()
  scenarioHint.value = hint
}

const runScenarioA = async () => {
  scenarioRunning.value = true
  await setScenarioHint('场景演示：正常用户（1 req/s，全部成功）')
  ElMessage.info('场景 A 开始：正常用户，每秒1次请求，全部成功')
  for (let i = 0; i < 5; i++) {
    successCount.value++
    recordRequest(true)
    addToRecentRequests(true)
    await sleep(1000)
  }
  ElMessage.success('场景 A 执行完毕')
  scenarioRunning.value = false
}

const runScenarioB = async () => {
  scenarioRunning.value = true
  await setScenarioHint('场景演示：突发流量（50次并发，红绿混合）')
  ElMessage.info('场景 B 开始：突发流量，50次并发请求，部分成功部分限流')
  const promises = []
  for (let i = 0; i < 50; i++) {
    promises.push(
      sendMockRequest()
        .then(() => {
          successCount.value++
          recordRequest(true)
          addToRecentRequests(true)
        })
        .catch(err => {
          if (err.status === 429) {
            failCount.value++
            recordRequest(false)
            addToRecentRequests(false)
            alert('被限流了！')
          } else {
            console.warn('意外错误', err)
          }
        })
    )
  }
  await Promise.all(promises)
  ElMessage.success('场景 B 执行完毕')
  scenarioRunning.value = false
}

const runScenarioC = async () => {
  scenarioRunning.value = true
  await setScenarioHint('场景演示：降级模式（模拟 Redis 离线，全量放行）')
  ElMessage.warning({
    message: '降级模式：限流器离线，全量放行',
    duration: 3000,
    showClose: true
  })
  for (let i = 0; i < 10; i++) {
    successCount.value++
    recordRequest(true)
    addToRecentRequests(true)
    await sleep(200)
  }
  ElMessage.success('场景 C 执行完毕（模拟降级，所有请求成功）')
  scenarioRunning.value = false
}

const showScenePanel = () => {
  if (autoAttackActive.value) {
    ElMessage.warning('请先停止自动攻击')
    return
  }
  if (scenarioRunning.value) {
    ElMessage.warning('场景正在运行，请稍后')
    return
  }
  showSceneSelector.value = true
}

const runScenario = async (scene) => {
  showSceneSelector.value = false
  // 2026-04-02 修复：先清空提示，再运行场景
  scenarioHint.value = ''
  await nextTick()
  if (scene === 'A') await runScenarioA()
  else if (scene === 'B') await runScenarioB()
  else if (scene === 'C') await runScenarioC()
}

// ---------- ECharts 图表 ----------
const chartRef = ref(null)
let chart = null
const requestHistory = ref([])

const recordRequest = (success) => {
  const now = Date.now()
  requestHistory.value.push({ timestamp: now, success })
  const cutoff = now - 60 * 1000
  requestHistory.value = requestHistory.value.filter(r => r.timestamp >= cutoff)
  updateChart()
}

const updateChart = () => {
  if (!chart) return
  const now = Date.now()
  const successRates = []
  const qpsData = []
  for (let i = 59; i >= 0; i--) {
    const secondStart = now - i * 1000
    const secondEnd = secondStart + 1000
    const requestsInSecond = requestHistory.value.filter(r => r.timestamp >= secondStart && r.timestamp < secondEnd)
    const total = requestsInSecond.length
    const success = requestsInSecond.filter(r => r.success).length
    successRates.push(total === 0 ? 0 : (success / total) * 100)
    qpsData.push(total)
  }
  chart.setOption({
    series: [
      {
        name: '成功率 (%)',
        type: 'line',
        data: successRates,
        yAxisIndex: 0,
        smooth: true,
        lineStyle: { color: '#67c23a', width: 2 },
        symbol: 'none',
        areaStyle: { opacity: 0.1, color: '#67c23a' }
      },
      {
        name: 'QPS',
        type: 'bar',
        data: qpsData,
        yAxisIndex: 1,
        barWidth: '60%',
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] }
      }
    ]
  })
}

onMounted(() => {
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['成功率 (%)', 'QPS'], left: 'left' },
    grid: { left: '8%', right: '8%', top: '15%', bottom: '5%', containLabel: true },
    xAxis: {
      type: 'category',
      data: Array.from({ length: 60 }, (_, i) => `-${59 - i}s`),
      axisLabel: { rotate: 30, interval: 'auto', margin: 8 }
    },
    yAxis: [
      { type: 'value', name: '成功率 (%)', min: 0, max: 100, nameStyle: { color: '#67c23a' } },
      { type: 'value', name: 'QPS', nameStyle: { color: '#409eff' } }
    ],
    series: []
  })
  // 启动统计定时器
  startStatsTimer()
})

onUnmounted(() => {
  if (attackInterval) clearInterval(attackInterval)
  if (chart) chart.dispose()
  stopStatsTimer()
})
</script>

<style scoped>
/* 原有样式保持不变，新增以下样式 */
.test-limit-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 1.5rem;
  background-color: #f9fafb;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}
.header-right {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
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
.mode-badge {
  margin-top: 0.5rem;
}

/* 实时统计面板样式 */
.stats-panel {
  background: #ffffff;
  border-radius: 12px;
  padding: 0.5rem 1rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  font-size: 0.85rem;
}
.stat-item {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}
.stat-label {
  color: #6b7280;
}
.stat-value {
  font-weight: 600;
  color: #1f2937;
}
.stat-value.online {
  color: #10b981;
}
.stat-value.high {
  color: #10b981;
}
.stat-value.low {
  color: #f56c6c;
}
.stat-detail {
  font-size: 0.7rem;
  color: #9ca3af;
  margin-left: 0.25rem;
}

/* 批量结果列表样式 */
.batch-results {
  margin-bottom: 1rem;
  background: #ffffff;
  border-radius: 12px;
  padding: 0.75rem;
  border: 1px solid #eef2f6;
}
.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  font-weight: 500;
  font-size: 0.9rem;
}
.result-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  max-height: 120px;
  overflow-y: auto;
}
.result-item {
  width: 40px;
  text-align: center;
  font-size: 1.1rem;
  padding: 0.2rem;
  border-radius: 6px;
}
.result-item.success {
  background-color: #e8f5e9;
}
.result-item.fail {
  background-color: #ffebee;
}

/* 其他原有样式保持不变 */
.control-card {
  margin-bottom: 1.5rem;
  border-radius: 16px;
  transition: transform 0.2s;
}
.control-card:hover {
  transform: translateY(-2px);
}
.card-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.request-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.counter-group {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
}
.counter {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #ffffff;
  padding: 0.75rem 1.25rem;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  flex: 1;
  min-width: 140px;
}
.counter.success {
  border-left: 4px solid #67c23a;
}
.counter.fail {
  border-left: 4px solid #f56c6c;
}
.counter .el-icon {
  font-size: 1.75rem;
}
.counter.success .el-icon {
  color: #67c23a;
}
.counter.fail .el-icon {
  color: #f56c6c;
}
.counter-text {
  display: flex;
  flex-direction: column;
}
.label {
  font-size: 0.75rem;
  color: #6b7280;
}
.number {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.2;
}
.counter.success .number {
  color: #67c23a;
}
.counter.fail .number {
  color: #f56c6c;
}
.slider-group {
  width: 100%;
  max-width: 400px;
}
.slider-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
  font-size: 0.85rem;
  color: #374151;
}
.slider-label .el-icon {
  color: #3b82f6;
}

.scene-selector {
  margin-bottom: 1rem;
  padding: 1rem;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #eef2f6;
}
.scene-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.chart-card {
  border-radius: 16px;
  overflow: hidden;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.scene-hint {
  margin-bottom: 0.75rem;
}
.chart-container {
  height: 350px;
  width: 100%;
}

@media (max-width: 768px) {
  .test-limit-container {
    padding: 1rem;
  }
  .counter-group {
    flex-direction: column;
  }
  .counter {
    min-width: auto;
  }
  .slider-group {
    max-width: 100%;
  }
  .chart-container {
    height: 280px;
  }
  .stats-panel {
    font-size: 0.7rem;
    padding: 0.3rem 0.6rem;
  }
}
</style>
