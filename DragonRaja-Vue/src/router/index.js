import { createRouter, createWebHashHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ForgotView from '@/views/ForgotView.vue'
import ConsoleView from '@/views/ConsoleView.vue'
import { isLoggedIn } from '@/utils/auth'

const routes = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { title: '身份认证 · 登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: { title: '新生登记 · 注册' }
  },
  {
    path: '/forgot',
    name: 'forgot',
    component: ForgotView,
    meta: { title: '密钥重置 · 找回' }
  },
  {
    path: '/console',
    name: 'console',
    component: ConsoleView,
    meta: { title: '学院内网 · 我的身份卡', requiresAuth: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  // hash 模式：部署到任意静态目录 / 直接打开 dist 都能正常切换
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 需要登录的页面统一走这里（令牌失效由 api 层清理会话）
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta?.title
    ? `${to.meta.title} | 卡塞尔学院`
    : '卡塞尔学院 · 龙族身份认证'
})

export default router
