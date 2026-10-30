import { createRouter, createWebHashHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import { isAdminLoggedIn } from '@/utils/adminAuth'

/**
 * 路由级代码分割：视图全部用动态 import，由 Vite 自动切成独立 chunk。
 * 这样首屏只需下载登录页那一个 chunk，后台管理 / 血统鉴定 / 档案室
 * 等页面在真正访问时才加载，首屏体积大幅下降。
 */
const LoginView = () => import('@/views/LoginView.vue')
const RegisterView = () => import('@/views/RegisterView.vue')
const ForgotView = () => import('@/views/ForgotView.vue')
const ConsoleView = () => import('@/views/ConsoleView.vue')
const HomeView = () => import('@/views/HomeView.vue')
const MissionsView = () => import('@/views/MissionsView.vue')
const ArchiveView = () => import('@/views/ArchiveView.vue')
const CharacterView = () => import('@/views/CharacterView.vue')
const AppraisalView = () => import('@/views/AppraisalView.vue')
const CodexDetailView = () => import('@/views/CodexDetailView.vue')
const AdminLoginView = () => import('@/views/AdminLoginView.vue')
const AdminView = () => import('@/views/AdminView.vue')

const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/home',
    name: 'home',
    component: HomeView,
    meta: { title: '学院首页 · 龙文档案馆', requiresAuth: true, standalone: true }
  },
  {
    path: '/missions',
    name: 'missions',
    component: MissionsView,
    meta: { title: '执行部任务 · 任务看板', requiresAuth: true, standalone: true }
  },
  {
    path: '/archive',
    name: 'archive',
    component: ArchiveView,
    meta: { title: '档案室 · 龙王与人物卷宗', requiresAuth: true, standalone: true }
  },
  {
    path: '/codex/:key',
    name: 'codex',
    component: CharacterView,
    meta: { title: '人物档案 · 人物志', requiresAuth: true, standalone: true }
  },
  {
    path: '/appraisal',
    name: 'appraisal',
    component: AppraisalView,
    meta: { title: '血统鉴定 · 采样受理', requiresAuth: true, standalone: true }
  },
  {
    path: '/spirit/:key',
    name: 'spirit',
    component: CodexDetailView,
    props: { kind: 'spirit' },
    meta: { title: '言灵档案 · 言灵谱', requiresAuth: true, standalone: true }
  },
  {
    path: '/org/:key',
    name: 'org',
    component: CodexDetailView,
    props: { kind: 'org' },
    meta: { title: '机构档案 · 机构志', requiresAuth: true, standalone: true }
  },
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
  {
    // 后台管理系统 · 登录页（只有登录，没有注册）
    path: '/admin/login',
    name: 'admin-login',
    component: AdminLoginView,
    // standalone：不套学员端 AuthLayout 外壳，使用后台专属登录界面
    meta: { title: '后台管理系统 · 登录', standalone: true }
  },
  {
    // 后台管理系统 · 言灵图鉴数据管理（meta.adminAuth 由下方守卫校验）
    path: '/admin',
    name: 'admin',
    component: AdminView,
    // standalone：脱离学员端「执行部档案系统」外壳，独立后台身份
    meta: { title: '后台管理系统', adminAuth: true, standalone: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  // hash 模式：部署到任意静态目录 / 直接打开 dist 都能正常切换
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 需要登录的页面统一走这里（令牌失效由 api 层清理会话）
router.beforeEach((to) => {
  // 后台管理系统：管理员令牌独立于学员令牌
  if (to.meta.adminAuth && !isAdminLoggedIn()) {
    return { path: '/admin/login' }
  }
  // 已登录管理员不再停留登录页，直接进后台
  if (to.path === '/admin/login' && isAdminLoggedIn()) {
    return { path: '/admin' }
  }
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  // 后台路由使用独立的「后台管理」后缀，不再套用学员端「执行部档案系统」品牌
  const suffix = to.path.startsWith('/admin')
    ? '卡塞尔学院・后台管理'
    : '卡塞尔学院・执行部档案系统'
  document.title = to.meta?.title ? `${to.meta.title} | ${suffix}` : suffix
})

export default router
