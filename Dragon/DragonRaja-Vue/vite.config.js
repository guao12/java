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
    // 默认绑定本机局域网地址 192.168.68.1（原为 0.0.0.0）。
    // 同一局域网内的其它电脑用 http://192.168.68.1:5177 打开时，/api 由「本机」
    // 代理到同一套 Spring Boot + MySQL —— 多台电脑共用一份数据库，
    // 权限进度才会跟着账号走。
    // 反过来说：每台电脑各跑自己那套（各自装 MySQL），就等于各有各的库，
    // 换机器登录同一个账号也只能读到 0 点，跟同步代码没关系。
    // 需要同时监听所有网卡（含 127.0.0.1）时：VITE_HOST=0.0.0.0 npm run dev
    // 网卡地址变了（换了 WiFi / 重装虚拟网卡）会报 EADDRNOTAVAIL，改这里或临时传 VITE_HOST。
    host: process.env.VITE_HOST || '192.168.68.1',
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
    // 开发环境把 /api 代理到 Spring Boot（默认 192.168.68.1:8082），前端代码里直接写 /api/** 即可
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://192.168.68.1:8082',
        changeOrigin: true
      }
    }
  }
})
