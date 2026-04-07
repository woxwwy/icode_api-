<template>
  <div class="home-container">
    <!-- 头部区域 -->
    <div class="page-header">
      <div class="title-section">
        <h1>路由查询系统</h1>
        <p class="subtitle">API 网关 · 路由配置管理</p >
      </div>
      <div class="connection-status" :class="{ connected: redisConnected }">
        <span class="status-dot"></span>
        <span>{{ redisConnected ? 'Redis 已连接' : 'Redis 未连接' }}</span>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="按路径、名称或ID搜索路由..."
          :prefix-icon="Search"
          clearable
        />
      </div>
      <div class="action-buttons">
        <!-- Redis连通性测试按钮 -->
        <el-button
          type="success"
          :loading="redisTestLoading"
          @click="testRedisConnectivity"
          plain
        >
          <el-icon><Monitor /></el-icon>
          Redis连通性测试
        </el-button>
        <!-- 网络延迟检测按钮 -->
        <el-button
          type="warning"
          :loading="latencyTestLoading"
          @click="testNetworkLatency"
          plain
        >
          <el-icon><Timer /></el-icon>
          网络延迟检测
        </el-button>
        <!-- 原有的刷新路由按钮 -->
        <el-button type="primary" @click="refreshRoutes" :loading="routesLoading">
          <el-icon><Refresh /></el-icon>
          {{ routesLoading ? '刷新中...' : '刷新路由' }}
        </el-button>
        <!-- 临时测试：降级接口测试（联调完成后删除） -->
        <el-button
          type="danger"
          @click="testFallback"
          plain
        >
          降级测试
        </el-button>
        <!-- 转发渠道一：外卖服务模拟 -->
        <el-button
          type="info"
          :loading="deliveryLoading"
          @click="callDeliveryService"
          plain
        >
          🍔 转发渠道一（外卖服务模拟）
        </el-button>
        <!-- 转发渠道二：快递服务模拟 -->
        <el-button
          type="info"
          :loading="expressLoading"
          @click="callExpressService"
          plain
        >
          📦 转发渠道二（快递服务模拟）
        </el-button>
      </div>
    </div>

    <!-- 路由卡片 -->
    <el-card class="route-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>路由列表</span>
          <span class="route-count" v-if="filteredRoutes.length">
            共 {{ filteredRoutes.length }} 条
          </span>
        </div>
      </template>
      <RouteTable
        :routes="filteredRoutes"
        :loading="routesLoading"
        :error="routesError"
        @retry="refreshRoutes"
        @row-click="handleRowClick"
      />
    </el-card>

    <!-- 网关日志查看区域 -->
    <el-card class="log-card" shadow="hover">
      <template #header>
        <div class="log-header">
          <span>网关日志（最近50行）</span>
          <el-button size="small" @click="loadLogs" :loading="logsLoading">
            刷新日志
          </el-button>
        </div>
      </template>
      <pre class="log-content">{{ logsContent }}</pre>
    </el-card>

    <!-- 自定义红色错误弹窗 -->
    <transition name="fade">
      <div v-if="errorPopup.visible" class="error-popup" :class="errorPopup.type">
        <div class="popup-header">
          <el-icon><WarningFilled /></el-icon>
          <span class="title">{{ errorPopup.title }}</span>
          <el-icon class="close-icon" @click="closeErrorPopup"><Close /></el-icon>
        </div>
        <div class="popup-content">
          <p class="user-message">{{ errorPopup.userMessage }}</p >
          <details class="technical-details" v-if="errorPopup.technicalDetail">
            <summary>技术详情（点击展开）</summary>
            <pre>{{ errorPopup.technicalDetail }}</pre>
          </details>
        </div>
      </div>
    </transition>

    <!-- 成功提示（轻提示，非报错） -->
    <transition name="fade">
      <div v-if="successMessage.visible" class="success-toast">
        <el-icon><SuccessFilled /></el-icon>
        <span>{{ successMessage.text }}</span>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { getPing, getRedisStatus } from '../api/routes'
import { getRoutes } from '@/mock/mockroutes.js'
import RouteTable from '@/components/RouteTable.vue'
import {
  ElButton,
  ElCard,
  ElTag,
  ElInput,
  ElMessage,
  ElIcon
} from 'element-plus'
import {
  Search,
  Refresh,
  Monitor,
  Timer,
  WarningFilled,
  Close,
  SuccessFilled
} from '@element-plus/icons-vue'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/tag/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/message/style/css'

// ---------- 原有数据 ----------
const redisConnected = ref(false)
const routes = ref([])
const routesLoading = ref(false)
const routesError = ref('')
const searchKeyword = ref('')

// 新增：日志相关数据
const logsContent = ref('点击刷新按钮加载日志...')
const logsLoading = ref(false)

// 新增：转发渠道相关
const deliveryLoading = ref(false)
const expressLoading = ref(false)

// 过滤路由
const filteredRoutes = computed(() => {
  if (!searchKeyword.value.trim()) return routes.value
  const keyword = searchKeyword.value.toLowerCase()
  return routes.value.filter(route =>
    route.path?.toLowerCase().includes(keyword) ||
    route.name?.toLowerCase().includes(keyword) ||
    route.id?.toLowerCase().includes(keyword)
  )
})

// 获取路由数据
const refreshRoutes = async () => {
  routesLoading.value = true
  routesError.value = ''
  try {
    routes.value = await getRoutes()
  } catch (err) {
    routesError.value = err.message || '加载路由列表失败'
    ElMessage.error(routesError.value)
  } finally {
    routesLoading.value = false
  }
}

// 新增：加载网关日志
const loadLogs = async () => {
  logsLoading.value = true
  try {
    const res = await fetch('http://localhost:8081/logs/recent')
    const data = await res.json()
    if (Array.isArray(data)) {
      logsContent.value = data.join('\n')
    } else {
      logsContent.value = '日志格式错误'
    }
  } catch (error) {
    console.error('加载日志失败:', error)
    logsContent.value = '加载日志失败：' + error.message
  } finally {
    logsLoading.value = false
  }
}

// ---------- 新增功能 ----------
// Redis连通性测试
const redisTestLoading = ref(false)
const testRedisConnectivity = async () => {
  redisTestLoading.value = true
  const startTime = performance.now()
  try {
    const status = await getRedisStatus()
    const latency = (performance.now() - startTime).toFixed(2)
    if (status.connected) {
      redisConnected.value = true
      showSuccessToast(`Redis 连通性测试通过，延迟: ${latency}ms`)
    } else {
      redisConnected.value = false
      showErrorPopup(
        'Redis 连通性测试失败',
        'Redis 服务未就绪，请检查配置。',
        status.error || '未知错误'
      )
    }
  } catch (error) {
    redisConnected.value = false
    showErrorPopup(
      'Redis 连通性测试错误',
      '无法获取 Redis 状态，请检查网关或网络。',
      error.message
    )
  } finally {
    redisTestLoading.value = false
  }
}

// 网络延迟检测
const latencyTestLoading = ref(false)
const testNetworkLatency = async () => {
  latencyTestLoading.value = true
  const startTime = performance.now()
  const pingUrl = '/ping'
  try {
    await axios.get(pingUrl, { timeout: 5000 })
    const latency = (performance.now() - startTime).toFixed(2)
    showSuccessToast(`网络延迟: ${latency} ms (网关响应时间)`)
  } catch (error) {
    showErrorPopup(
      '网络延迟检测失败',
      '无法连接到网关，请检查网络或服务状态。',
      `请求 URL: ${pingUrl}\n错误信息: ${error.message}`
    )
  } finally {
    latencyTestLoading.value = false
  }
}

// 临时测试：调用降级接口（联调完成后删除）
const testFallback = async () => {
  try {
    const res = await axios.get('http://192.168.226.95:8081/test-with-fallback')
    console.log('收到降级测试数据:', res.data)
    if (res.data.message && res.data.message.includes('降级')) {
      alert('注意：当前显示的是降级数据（真实服务可能挂了）')
    }
    // 临时替换路由列表为测试数据
    routes.value = [res.data]
    showSuccessToast('降级测试完成，当前显示降级数据')
  } catch (error) {
    console.error('降级测试失败:', error)
    showErrorPopup(
      '降级测试失败',
      '无法连接到网关降级测试接口',
      error.message
    )
  }
}

// 转发渠道一：外卖服务模拟
const callDeliveryService = async () => {
  deliveryLoading.value = true
  try {
    const res = await axios.get('http://localhost:8081/api/demo/delivery')
    console.log('外卖服务响应:', res.data)
    showSuccessToast(`转发成功 - ${res.data.channel}，${res.data.deliveryTime}`)
  } catch (error) {
    console.error('调用失败:', error)
    showErrorPopup('转发失败', '外卖服务调用失败', error.message)
  } finally {
    deliveryLoading.value = false
  }
}

// 转发渠道二：快递服务模拟
const callExpressService = async () => {
  expressLoading.value = true
  try {
    const res = await axios.get('http://localhost:8081/api/demo/express')
    console.log('快递服务响应:', res.data)
    showSuccessToast(`转发成功 - ${res.data.channel}，${res.data.estimatedArrival}`)
  } catch (error) {
    console.error('调用失败:', error)
    showErrorPopup('转发失败', '快递服务调用失败', error.message)
  } finally {
    expressLoading.value = false
  }
}

// ---------- 错误弹窗与成功提示 ----------
const errorPopup = ref({
  visible: false,
  type: 'error',
  title: '',
  userMessage: '',
  technicalDetail: ''
})

const successMessage = ref({
  visible: false,
  text: ''
})

let successTimeout = null
let errorTimeout = null

// 显示红色报错弹窗
const showErrorPopup = (title, userMsg, technicalMsg) => {
  console.error(`[Error Popup] ${title}:`, {
    userMessage: userMsg,
    technicalDetail: technicalMsg,
    timestamp: new Date().toISOString()
  })

  if (errorTimeout) clearTimeout(errorTimeout)
  errorPopup.value = {
    visible: true,
    type: 'error',
    title: title || '错误',
    userMessage: userMsg || '操作失败，请稍后重试。',
    technicalDetail: technicalMsg || '无详细技术信息'
  }
  errorTimeout = setTimeout(() => {
    errorPopup.value.visible = false
  }, 5000)
}

// 显示成功提示
const showSuccessToast = (text) => {
  if (successTimeout) clearTimeout(successTimeout)
  successMessage.value = {
    visible: true,
    text: text
  }
  successTimeout = setTimeout(() => {
    successMessage.value.visible = false
  }, 3000)
}

const closeErrorPopup = () => {
  errorPopup.value.visible = false
  if (errorTimeout) clearTimeout(errorTimeout)
}

const handleRowClick = (row) => {
  // 可选：点击行时触发事件
}

// 组件挂载时获取路由数据
onMounted(() => {
  refreshRoutes()
})
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
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
.connection-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #ffffff;
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  font-size: 0.875rem;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ef4444;
}
.connection-status.connected .status-dot {
  background-color: #10b981;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}
.search-box {
  flex: 1;
  min-width: 260px;
}
.action-buttons {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.route-card {
  margin-top: 0.5rem;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.route-count {
  font-size: 0.75rem;
  color: #6b7280;
  background: #f3f4f6;
  padding: 0.25rem 0.5rem;
  border-radius: 9999px;
}

/* 网关日志卡片样式 */
.log-card {
  margin-top: 20px;
}
.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.log-content {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  height: 300px;
  overflow: auto;
  font-family: 'Consolas', monospace;
  font-size: 12px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}

/* 红色错误弹窗 */
.error-popup {
  position: fixed;
  top: 80px;
  right: 24px;
  width: 380px;
  background: #fff2f0;
  border-left: 4px solid #f56c6c;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
  z-index: 2000;
  backdrop-filter: blur(2px);
  transition: all 0.3s ease;
}
.error-popup.error {
  background: #fff2f0;
  border-left-color: #f56c6c;
}
.popup-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 8px 16px;
  border-bottom: 1px solid #ffccc7;
}
.popup-header .title {
  flex: 1;
  font-weight: 600;
  color: #f56c6c;
  font-size: 16px;
}
.close-icon {
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
}
.close-icon:hover {
  color: #f56c6c;
}
.popup-content {
  padding: 12px 16px 16px 16px;
}
.user-message {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}
.technical-details {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  background: #fafafa;
  border-radius: 6px;
  padding: 8px;
}
.technical-details summary {
  cursor: pointer;
  color: #f56c6c;
  font-weight: 500;
}
.technical-details pre {
  margin: 8px 0 0 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: monospace;
  font-size: 11px;
  color: #d14;
}

/* 成功提示 */
.success-toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #67c23a;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 2000;
  backdrop-filter: blur(4px);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

/* 响应式 */
@media (max-width: 768px) {
  .home-container {
    padding: 1rem;
  }
  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .action-buttons {
    justify-content: flex-start;
  }
  .error-popup {
    width: calc(100% - 32px);
    top: 16px;
    right: 16px;
  }
}
</style>
