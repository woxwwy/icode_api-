<template>
  <div class="test-limit-container">
    <!-- 头部区域 -->
    <div class="page-header">
      <div class="title-section">
        <h1>限流效果验证</h1>
        <p class="subtitle">API 网关 · 限流策略可视化测试</p>
      </div>
      <div class="mode-badge">
        <el-tag type="info" effect="plain">{{ currentModeText }}</el-tag>
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
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
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
  VideoCamera
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
const showSceneSelector = ref(false)  // 控制场景选择面板显示
const scenarioRunning = ref(false)
const scenarioHint = ref('')          // 场景运行提示

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

// 单次请求
const sendSingleRequest = async () => {
  // 2026-03-27 修复：手动发送请求时清除场景提示
  scenarioHint.value = ''
  const mode = localStorage.getItem('rate_limit_mode')
  if (mode === 'real') {
    try {
      await client.post('/test-rate-limit', {})
      successCount.value++
      recordRequest(true)
    } catch (error) {
      if (error.response && error.response.status === 429) {
        failCount.value++
        recordRequest(false)
      } else {
        ElMessage.error('请求失败：' + (error.message || '未知错误'))
      }
    }
  } else {
    try {
      await sendMockRequest()
      successCount.value++
      recordRequest(true)
    } catch (error) {
      if (error.status === 429) {
        failCount.value++
        recordRequest(false)
      } else {
        ElMessage.error('请求失败：' + error.message)
      }
    }
  }
}

// 自动攻击
const startAutoAttack = () => {
  if (attackInterval) clearInterval(attackInterval)
  const intervalMs = 1000 / frequency.value
  attackInterval = setInterval(() => {
    sendSingleRequest()
  }, intervalMs)
}

const stopAutoAttack = () => {
  if (attackInterval) {
    clearInterval(attackInterval)
    attackInterval = null
  }
}

const toggleAutoAttack = () => {
  // 2026-03-27 修复：点击自动攻击按钮时清除场景提示
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

// 场景 A：正常用户
const runScenarioA = async () => {
  scenarioRunning.value = true
  scenarioHint.value = '场景演示：正常用户（1 req/s，全部成功）'
  ElMessage.info('场景 A 开始：正常用户，每秒1次请求，全部成功')
  for (let i = 0; i < 5; i++) {
    successCount.value++
    recordRequest(true)
    await sleep(1000)
  }
  ElMessage.success('场景 A 执行完毕')
  scenarioRunning.value = false
}

// 场景 B：突发流量
const runScenarioB = async () => {
  scenarioRunning.value = true
  scenarioHint.value = '场景演示：突发流量（50次并发，红绿混合）'
  ElMessage.info('场景 B 开始：突发流量，50次并发请求，部分成功部分限流')
  const promises = []
  for (let i = 0; i < 50; i++) {
    promises.push(
      sendMockRequest()
        .then(() => {
          successCount.value++
          recordRequest(true)
        })
        .catch(err => {
          if (err.status === 429) {
            failCount.value++
            recordRequest(false)
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

// 场景 C：降级模式
const runScenarioC = async () => {
  scenarioRunning.value = true
  scenarioHint.value = '场景演示：降级模式（模拟 Redis 离线，全量放行）'
  ElMessage.warning({
    message: '降级模式：限流器离线，全量放行',
    duration: 3000,
    showClose: true
  })
  for (let i = 0; i < 10; i++) {
    successCount.value++
    recordRequest(true)
    await sleep(200)
  }
  ElMessage.success('场景 C 执行完毕（模拟降级，所有请求成功）')
  scenarioRunning.value = false
}

// 显示场景选择面板
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

// 统一场景运行入口
const runScenario = async (scene) => {
  showSceneSelector.value = false  // 隐藏选择面板
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
})

onUnmounted(() => {
  if (attackInterval) clearInterval(attackInterval)
  if (chart) chart.dispose()
})
</script>

<style scoped>
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

/* 场景选择面板 */
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
}
</style>
