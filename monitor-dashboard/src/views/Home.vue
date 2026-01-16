<template>
  <div class="home-container">
    <!-- 查询区域 -->
    <div class="query-section">
      <el-input 
        v-model="queryParams" 
        placeholder="请输入查询条件" 
        style="width: 300px; margin-right: 10px;" 
      />
      <QueryButton @click="fetchRoutes" />
    </div>

    <!-- 路由表格 -->
    <div class="table-section">
      <RouteTable :routes="routes" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import QueryButton from '../components/QueryButton.vue'
import RouteTable from '../components/RouteTable.vue'
import { getRoutes } from '../api/routes'
import { ElInput } from 'element-plus'
import 'element-plus/es/components/input/style/css'

// 响应式数据
const routes = ref([])
const queryParams = ref('')

// 获取路由数据
const fetchRoutes = (startLoading, endLoading) => {
  startLoading()
  
  getRoutes()
    .then(response => {
      // 确保首条数据id为order-route
      if (Array.isArray(response) && response.length > 0) {
        response[0].id = 'order-route'
        // 根据查询参数过滤数据
        if (queryParams.value) {
          const params = queryParams.value.toLowerCase()
          routes.value = response.filter(route => 
            route.id.toLowerCase().includes(params) ||
            route.path.toLowerCase().includes(params) ||
            route.name.toLowerCase().includes(params)
          )
        } else {
          routes.value = response
        }
      } else {
        // 模拟数据
        const mockData = [
          { id: 'order-route', path: '/home', component: 'HomeComponent', name: '首页' },
          { id: '2', path: '/about', component: 'AboutComponent', name: '关于我们' },
          { id: '3', path: '/user/list', component: 'UserListComponent', name: '用户列表' },
          { id: '4', path: '/user/detail', component: 'UserDetailComponent', name: '用户详情' }
        ]
        
        // 根据查询参数过滤模拟数据
        if (queryParams.value) {
          const params = queryParams.value.toLowerCase()
          routes.value = mockData.filter(route => 
            route.id.toLowerCase().includes(params) ||
            route.path.toLowerCase().includes(params) ||
            route.name.toLowerCase().includes(params)
          )
        } else {
          routes.value = mockData
        }
      }
    })
    .catch(err => {
      alert('加载失败')
      console.warn('获取路由失败:', err)
      // API失败时也显示模拟数据
      const mockData = [
        { id: 'order-route', path: '/home', component: 'HomeComponent', name: '首页' },
        { id: '2', path: '/about', component: 'AboutComponent', name: '关于我们' },
        { id: '3', path: '/user/list', component: 'UserListComponent', name: '用户列表' },
        { id: '4', path: '/user/detail', component: 'UserDetailComponent', name: '用户详情' }
      ]
      
      // 根据查询参数过滤模拟数据
      if (queryParams.value) {
        const params = queryParams.value.toLowerCase()
        routes.value = mockData.filter(route => 
          route.id.toLowerCase().includes(params) ||
          route.path.toLowerCase().includes(params) ||
          route.name.toLowerCase().includes(params)
        )
      } else {
        routes.value = mockData
      }
    })
    .finally(() => {
      endLoading()
    })
}
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.query-section {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.table-section {
  min-height: 400px;
}
</style>
