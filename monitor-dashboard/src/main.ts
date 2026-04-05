import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Home from './views/Home.vue'
import TestLimit from './views/TestLimit.vue'
// 2026-03-27 新增：导入压测数据展示页面组件
import Benchmark from './views/Benchmark.vue'
//import './styles/main.css'

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: Home
    },
    {
      path: '/test-limit',
      name: 'TestLimit',
      component: TestLimit
    },
    // 2026-03-27 新增：压测数据展示页面路由
    {
      path: '/benchmark',
      name: 'Benchmark',
      component: Benchmark
    }
  ]
})

// 创建 Vue 应用实例
const app = createApp(App)

// 使用路由插件
app.use(router)

// 挂载应用（只挂载一次）
app.mount('#app')
