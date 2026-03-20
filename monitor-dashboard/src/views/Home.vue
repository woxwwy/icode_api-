<template>
  <div class="home-container">
    <!-- 查询区域 -->
    <div class="query-section">
      <el-button type="success" @click="fetchPing" :loading="loading">
        <template v-if="loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          检查中...
        </template>
        <template v-else>检查Redis连接</template>
      </el-button>
    </div>

    <!-- 结果显示 -->
    <div class="result-section" v-if="pingResult">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>Redis连接状态</span>
          </div>
        </template>
        <div class="ping-result">
          <el-tag :type="pingResult === 'PONG' ? 'success' : 'warning'">
            {{ pingResult }}
          </el-tag>
        </div>
      </el-card>
    </div>

    <!-- ========== 新增：路由表格区域 ========== -->
    <div class="route-section" style="margin-top: 30px;">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>路由列表（mock 数据）</span>
          </div>
        </template>
        <RouteTable :routes="mockRoutes" />
      </el-card>
    </div>
    <!-- ========== 新增结束 ========== -->
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getPing } from '../api/routes'
import { ElButton, ElCard, ElTag, ElIcon } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/tag/style/css'
import 'element-plus/es/components/icon/style/css'
// 新增：导入 mock 数据和表格组件
import { mockRoutes } from '@/mock/mockroutes.js'
import RouteTable from '@/components/RouteTable.vue'          //原本我的代码只显示了ping的结果，没有展示table

// 响应式数据
const pingResult = ref('')
const loading = ref(false)

// 获取Redis ping结果
const fetchPing = () => {
  loading.value = true
  
  getPing()
    .then(response => {
      pingResult.value = response
    })
    .catch(err => {
      console.warn('获取ping结果失败:', err)
      // 降级处理，与后端保持一致
      pingResult.value = 'true (降级)'
    })
    .finally(() => {
      loading.value = false
    })
}
</script>

<style scoped>
.home-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.query-section {
  margin-bottom: 20px;
}

.result-section {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ping-result {
  font-size: 18px;
  font-weight: bold;
  padding: 20px 0;
}
</style>
