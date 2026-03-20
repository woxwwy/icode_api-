import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'  // 新增

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5170,
    open: true
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')  // 新增：配置 @ 别名指向 src
    }
  }
})
