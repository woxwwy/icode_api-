<!-- 2026-03-27 修改：改为单选框组，明确展示两种模式 -->
<template>
  <div class="global-config">
    <el-popover
      placement="bottom-end"
      :width="320"
      trigger="click"
      :visible="visible"
      @hide="visible = false"
    >
      <template #reference>
        <el-button :icon="Setting" circle size="small" @click="visible = !visible" />
      </template>
      <div class="config-panel">
        <div class="config-item">
          <span class="label">限流模式</span>
          <el-radio-group v-model="mode" @change="handleModeChange">
            <el-radio label="mock">Mock 限流</el-radio>
            <el-radio label="real">真实算法</el-radio>
          </el-radio-group>
        </div>
        <div class="config-item" v-if="mode === 'real'">
          <span class="label">限流阈值 (QPS)</span>
          <el-input-number
            v-model="thresholdQps"
            :min="1"
            :max="10000"
            :step="10"
            controls-position="right"
            @change="handleThresholdChange"
          />
        </div>
        <div class="config-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>切换后立即生效，设置保存在本地</span>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Setting, InfoFilled } from '@element-plus/icons-vue'
import { ElButton, ElPopover, ElRadioGroup, ElRadio, ElInputNumber, ElIcon } from 'element-plus'

// 配置状态：'mock' 或 'real'
const mode = ref('mock')
const thresholdQps = ref(100)

const STORAGE_KEYS = {
  MODE: 'rate_limit_mode',
  THRESHOLD: 'rate_limit_threshold'
}

const loadConfig = () => {
  const savedMode = localStorage.getItem(STORAGE_KEYS.MODE)
  if (savedMode !== null) {
    mode.value = savedMode === 'real' ? 'real' : 'mock'
  }
  const savedThreshold = localStorage.getItem(STORAGE_KEYS.THRESHOLD)
  if (savedThreshold !== null) {
    thresholdQps.value = Number(savedThreshold)
  }
}

const saveConfig = () => {
  localStorage.setItem(STORAGE_KEYS.MODE, mode.value)
  localStorage.setItem(STORAGE_KEYS.THRESHOLD, String(thresholdQps.value))
  window.dispatchEvent(new CustomEvent('rate-limit-config-changed', {
    detail: {
      useRealAlgorithm: mode.value === 'real',
      thresholdQps: thresholdQps.value
    }
  }))
}

const handleModeChange = () => {
  saveConfig()
}

const handleThresholdChange = () => {
  saveConfig()
}

onMounted(() => {
  loadConfig()
})

const visible = ref(false)
</script>

<style scoped>
.global-config {
  margin-left: auto;
}
.config-panel {
  padding: 0.5rem;
}
.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.config-item .label {
  font-size: 0.9rem;
  color: #374151;
}
.config-tip {
  margin-top: 1rem;
  padding-top: 0.5rem;
  border-top: 1px solid #e5e7eb;
  font-size: 0.75rem;
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
</style>
