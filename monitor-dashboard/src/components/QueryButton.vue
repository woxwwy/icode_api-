<template>
  <el-button 
    type="success" 
    @click="handleClick"
    :loading="loading"
  >
    <template v-if="loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      查询中...
    </template>
    <template v-else>
      查询路由
    </template>
  </el-button>
</template>

<script setup>
import { ref } from 'vue'
import { ElButton, ElIcon } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/icon/style/css'

const emit = defineEmits(['click'])

const loading = ref(false)

const handleClick = () => {
  emit('click', () => {
    loading.value = true
  }, () => {
    loading.value = false
  })
}
</script>

<style scoped>
/* 组件样式 */
</style>
