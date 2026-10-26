import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/theme.css'

const app = createApp(App).use(router)

/**
 * 渲染崩溃自救：
 * dev 环境下 vite 热更新 / 全页刷新可能与路由跳转竞争，偶发「页面只渲染出一部分」。
 * 这时 Vue 渲染函数抛错 → 这里捕获后整页刷新一次即可恢复。
 * 同一标签页 10 秒内最多自救一次，防止真实 bug 导致无限刷新循环。
 */
app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue 渲染异常]', info, err)
  try {
    const KEY = 'dr:last-auto-reload'
    const last = Number(sessionStorage.getItem(KEY)) || 0
    if (Date.now() - last > 10000) {
      sessionStorage.setItem(KEY, String(Date.now()))
      window.location.reload()
    }
  } catch {
    /* sessionStorage 不可用时静默 */
  }
}

app.mount('#app')
