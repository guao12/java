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
  build: {
    // 静态资源加 hash 后缀，可放心交给浏览器 / CDN 长缓存
    assetsInlineLimit: 4096,
    cssCodeSplit: true,
    // 只对体积大的库做拆分，业务 chunk 仍由路由动态 import 切分
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('vue-router')) return 'vendor-router'
            if (id.includes('/vue/') || id.includes('@vue/')) return 'vendor-vue'
            return 'vendor'
          }
        }
      }
    },
    chunkSizeWarningLimit: 700
  },
  server: {
    host: '127.0.0.1',
    port: 5177,
    open: false,
    // 预构建常改的入口依赖，减少 dev 首屏的按需编译卡顿
    warmup: {
      clientFiles: [
        './src/main.js',
        './src/App.vue',
        './src/router/index.js',
        './src/views/AdminView.vue',
        './src/components/AdminSidebar.vue'
      ]
    },
    // 开发环境把 /api 代理到 Spring Boot（默认 8082），前端代码里直接写 /api/** 即可
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://127.0.0.1:8082',
        changeOrigin: true
      }
    }
  }
})
