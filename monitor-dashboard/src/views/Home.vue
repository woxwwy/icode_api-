<template>
  <div class="home-container">
    <!-- 头部区域（新增） -->
    <div class="page-header">
      <div class="title-section">
        <h1>路由查询系统</h1>
        <p class="subtitle">API 网关 · 路由配置管理</p>
      </div>
      <div class="connection-status" :class="{ connected: redisConnected }">
        <span class="status-dot"></span>
        <span>{{ redisConnected ? 'Redis 已连接' : 'Redis 未连接' }}</span>
      </div>
    </div>

    <!-- 操作栏（新增） -->
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
        <el-button @click="fetchPing" :loading="loading">
          {{ loading ? '检查中...' : '检查 Redis 连接' }}
        </el-button>
        <el-button type="primary" @click="refreshRoutes" :loading="routesLoading">
          {{ routesLoading ? '刷新中...' : '刷新路由' }}
        </el-button>
      </div>
    </div>

    <!-- Redis 连接状态卡片（修改：样式优化，增加时间戳） -->
    <el-card v-if="pingResult" class="result-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>Redis 连接状态</span>
          <span class="check-time" v-if="lastCheckTime">{{ lastCheckTime }}</span>
        </div>
      </template>
      <div class="ping-result">
        <el-tag :type="pingResult === 'PONG' ? 'success' : 'danger'">
          {{ pingResult === 'PONG' ? '连接成功' : pingResult }}
        </el-tag>
      </div>
    </el-card>

    <!-- 路由卡片（修改：增加加载状态、错误提示、搜索过滤） -->
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
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getPing } from '../api/routes'
import { getRoutes } from '@/mock/mockroutes.js'
import RouteTable from '@/components/RouteTable.vue'
import { ElButton, ElCard, ElTag, ElInput, ElMessage } from 'element-plus'
import { Search, Loading } from '@element-plus/icons-vue'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/tag/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/message/style/css'

// 响应式数据
const pingResult = ref('')
const loading = ref(false)
const redisConnected = ref(false) // 用于头部状态显示
const lastCheckTime = ref('')
const routes = ref([])
const routesLoading = ref(false)
const routesError = ref('')
const searchKeyword = ref('')

// 过滤路由（新增）
const filteredRoutes = computed(() => {
  if (!searchKeyword.value.trim()) return routes.value
  const keyword = searchKeyword.value.toLowerCase()
  return routes.value.filter(route =>
    route.path?.toLowerCase().includes(keyword) ||
    route.name?.toLowerCase().includes(keyword) ||
    route.id?.toLowerCase().includes(keyword)
  )
})

// 获取路由数据（修改：增加 loading 和错误处理）
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

// 获取Redis ping结果（修改：增加连接状态和时间戳）
const fetchPing = async () => {
  loading.value = true
  try {
    const response = await getPing()
    pingResult.value = response
    redisConnected.value = response === 'PONG'
    lastCheckTime.value = new Date().toLocaleString()
    if (redisConnected.value) {
      ElMessage.success('Redis 连接正常')
    } else {
      ElMessage.warning(`Redis 连接异常：${response}`)
    }
  } catch (err) {
    console.warn('获取ping结果失败:', err)
    pingResult.value = '连接失败'
    redisConnected.value = false
    lastCheckTime.value = new Date().toLocaleString()
    ElMessage.error('Redis 连接检查失败')
  } finally {
    loading.value = false
  }
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

/* 头部 */
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

/* 操作栏 */
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
}

/* Redis 结果卡片 */
.result-card {
  margin-bottom: 1.5rem;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.check-time {
  font-size: 0.75rem;
  color: #9ca3af;
}
.ping-result {
  padding: 0.5rem 0;
}

/* 路由卡片 */
.route-card {
  margin-top: 0.5rem;
}
.route-count {
  font-size: 0.75rem;
  color: #6b7280;
  background: #f3f4f6;
  padding: 0.25rem 0.5rem;
  border-radius: 9999px;
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
}
</style>
