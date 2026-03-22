<template>
  <div class="route-table-wrapper">
    <!-- 加载状态（新增） -->
    <div v-if="loading" class="loading-wrapper">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 错误状态（新增） -->
    <div v-else-if="error" class="error-state">
      <el-empty :description="error" />
      <el-button type="primary" @click="$emit('retry')" size="small">重试</el-button>
    </div>

    <!-- 空状态（新增） -->
    <div v-else-if="!routes.length" class="empty-state">
      <el-empty description="暂无路由数据" />
    </div>

    <!-- 表格内容（修改：增加样式、动态状态标签、行点击） -->
    <el-table
      v-else
      :data="routes"
      stripe
      style="width: 100%"
      @row-click="handleRowClick"
    >
      <el-table-column prop="id" label="ID" min-width="180">
        <template #default="{ row }">
          <span class="mono-text">{{ row.id || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" min-width="200">
        <template #default="{ row }">
          <span class="path-text">{{ row.path || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="component" label="组件" min-width="180">
        <template #default="{ row }">
          {{ row.component || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="150">
        <template #default="{ row }">
          {{ row.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ row.status || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ElTable, ElTableColumn, ElTag, ElEmpty, ElButton, ElSkeleton } from 'element-plus'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/table-column/style/css'
import 'element-plus/es/components/tag/style/css'
import 'element-plus/es/components/empty/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/skeleton/style/css'

const props = defineProps({
  routes: {
    type: Array,
    default: () => []
  },
  loading: {            // 新增 prop
    type: Boolean,
    default: false
  },
  error: {              // 新增 prop
    type: String,
    default: ''
  }
})

defineEmits(['retry', 'row-click'])   // 新增事件

const getStatusType = (status) => {
  if (!status) return 'info'
  const s = status.toLowerCase()
  if (s.includes('正常') || s.includes('up')) return 'success'
  if (s.includes('异常') || s.includes('down')) return 'danger'
  return 'info'
}

const handleRowClick = (row) => {
  // 可选：点击行时触发事件
  // emit('row-click', row)
}
</script>

<style scoped>
.route-table-wrapper {
  width: 100%;
}
.mono-text {
  font-family: monospace;
  font-size: 0.8125rem;
  color: #4b5563;
}
.path-text {
  font-family: monospace;
  font-weight: 500;
  color: #2563eb;
}
.loading-wrapper {
  padding: 1rem 0;
}
.error-state, .empty-state {
  text-align: center;
  padding: 2rem 0;
}
</style>
