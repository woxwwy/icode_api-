<template>
  <div class="route-table-container">
    <el-table :data="routes" style="width: 100%">
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="path" label="路径" />
      <el-table-column prop="component" label="组件" />
      <el-table-column prop="name" label="名称" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ElTable, ElTableColumn, ElTag } from 'element-plus'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/table-column/style/css'
import 'element-plus/es/components/tag/style/css'

const props = defineProps({
  routes: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.route-table-container {
  margin-top: 20px;
}
</style>
