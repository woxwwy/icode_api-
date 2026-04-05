<template>
  <!-- 2026-03-27 修改：添加顶部栏和全局配置组件，保留路由视图 -->
  <div class="app-container">
    <div class="app-header">
      <div class="app-title">API 网关管理控制台</div>
      <GlobalConfig />
    </div>
    <router-view />
  </div>
</template>

<script setup>
// 2026-03-27 新增：导入全局配置组件
import GlobalConfig from './components/GlobalConfig.vue'
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f9fafb;
}
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 2rem;
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 100;
}
.app-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}
</style>
