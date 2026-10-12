import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 用相对路径，build 之后 dist 可直接双击打开或丢到任意静态服务器
  base: './',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '127.0.0.1',
    port: 5177,
    open: false,
    // 开发环境把 /api 代理到 Spring Boot（默认 8081），前端代码里直接写 /api/** 即可
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://127.0.0.1:8081',
        changeOrigin: true
      }
    }
  }
})
