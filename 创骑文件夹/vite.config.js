import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 假面骑士创骑 · 骑士系统前端
// 开发环境把 /api 开头的请求代理到 Spring Boot 后端（默认 8081，见 application-dev.properties）
const BACKEND = 'http://localhost:8081'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: false,
    // 代理后前端只需要请求 /api/xxx，不存在跨域问题，也不用关心后端端口
    proxy: {
      '/api': {
        target: BACKEND,
        changeOrigin: true,
        // 后端本身就是 /api 前缀，所以不需要 rewrite
      }
    }
  }
})
