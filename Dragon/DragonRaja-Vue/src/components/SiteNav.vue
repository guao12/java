<script setup>
/**
 * 全院统一顶部导航栏。
 *
 * 首页、任务看板、档案室、血统鉴定、档案详情页均复用本组件，
 * 保证品牌区 / 菜单项 / 学院时钟 / 用户菜单完全一致。
 *
 * - 菜单数据来自 data/nav.js 的 SITE_NAV（单一数据源）
 * - 带 `id` 的菜单项指向首页区块：已在首页则平滑滚动；在其他页面则先回首页再定位
 * - 带 `match` 的菜单项把这些路径前缀都算作命中（高亮）
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import { authApi } from '@/api/auth'
import { clearSession, getUser } from '@/utils/auth'
import { isAdminLoggedIn } from '@/utils/adminAuth'
import { ADMIN_ENTRY, HOME_PATH, SITE_NAV } from '@/data/nav'

const props = defineProps({
  items: { type: Array, default: () => SITE_NAV },
  /** sticky 导航高度，锚点滚动时让开 */
  offset: { type: Number, default: 84 }
})

/** 点击首页区块锚点时抛出，首页可据此先退出检索视图 */
const emit = defineEmits(['anchor'])

const route = useRoute()
const router = useRouter()

/* ---------------- 用户信息 ---------------- */

const user = ref(getUser())
const codename = computed(() => user.value?.codename || '未知混血种')
const account = computed(() => user.value?.account || '--------')
const identityName = computed(() => user.value?.identityName || '')

const menuOpen = ref(false)
const userBox = ref(null)

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}
function closeMenu() {
  menuOpen.value = false
}

/** 点击空白处收起用户下拉 */
function onDocClick(e) {
  if (!menuOpen.value) return
  if (userBox.value && !userBox.value.contains(e.target)) closeMenu()
}

async function onLogout() {
  closeMenu()
  try {
    await authApi.logout()
  } catch {
    /* 令牌过期等场景直接本地清理即可 */
  }
  clearSession()
  router.push('/login')
}

/* ---------------- 学院时钟 ---------------- */

const clockText = ref('')
let clockTimer = 0
const pad2 = (n) => String(n).padStart(2, '0')

function tickClock() {
  const d = new Date()
  clockText.value = `${d.getFullYear()}.${pad2(d.getMonth() + 1)}.${pad2(d.getDate())} · ${pad2(
    d.getHours()
  )}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

/* ---------------- 区块锚点定位 ---------------- */

const onHome = computed(() => route.path === HOME_PATH)
/** 当前高亮的区块 id；非首页或区块未渲染（检索视图）时为空 */
const activeId = ref('')

const reduceMotion =
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

const RetryLimit = 120 // 约 2s：跨页回首页时等区块渲染完成

/** 元素可能晚一帧才出现（跨页跳转 / 退出检索视图），用 rAF 轮询等待 */
function scrollToId(id, tries = 0) {
  const el = document.getElementById(id)
  if (!el) {
    if (tries < RetryLimit) requestAnimationFrame(() => scrollToId(id, tries + 1))
    return
  }
  const top = el.getBoundingClientRect().top + window.scrollY - props.offset
  window.scrollTo({ top: Math.max(top, 0), behavior: reduceMotion ? 'auto' : 'smooth' })
}

function isActive(item) {
  if (item.id) return onHome.value && activeId.value === item.id
  const list = item.match?.length ? item.match : [item.to]
  return list.some((p) => route.path === p || route.path.startsWith(`${p}/`))
}

function onNav(item) {
  closeMenu()
  // 首页区块锚点
  if (item.id) {
    activeId.value = item.id
    // 先让首页退出检索视图，再定位
    emit('anchor', item.id)
    if (onHome.value) {
      scrollToId(item.id)
    } else {
      router.push(item.to || HOME_PATH).then(() => scrollToId(item.id))
    }
    return
  }
  if (!item.to) return
  if (route.path === item.to) {
    // 已在当前页：回到顶部
    window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' })
    return
  }
  router.push(item.to)
}

/* ---------------- 后台管理系统入口 ---------------- */

/** 已登录管理员时按钮文案带上「已登录」，进后台不再被守卫拦 */
const adminReady = computed(() => isAdminLoggedIn())
const onAdmin = computed(() => route.path === '/admin' || route.path.startsWith('/admin/'))

/**
 * 跳后台：统一 push /admin。
 * 路由守卫会兜底 —— 无管理员令牌时自动转 /admin/login，已登录则直接进。
 */
function goAdmin() {
  closeMenu()
  router.push(ADMIN_ENTRY.to)
}

/* ---------------- 滚动高亮（仅首页区块） ---------------- */

let spyRaf = 0
function updateSpy() {
  if (spyRaf) return
  spyRaf = requestAnimationFrame(() => {
    spyRaf = 0
    if (!onHome.value) {
      activeId.value = ''
      return
    }
    let cur = ''
    for (const item of props.items) {
      if (!item.id) continue
      const el = document.getElementById(item.id)
      if (el && el.getBoundingClientRect().top - props.offset <= 12) cur = item.id
    }
    // 触底时直接高亮最后一个区块
    if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 8) {
      const last = props.items.filter((i) => i.id).pop()
      if (last) cur = last.id
    }
    activeId.value = cur
  })
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  window.addEventListener('scroll', updateSpy, { passive: true })
  window.addEventListener('resize', updateSpy)
  updateSpy()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  window.clearInterval(clockTimer)
  window.removeEventListener('scroll', updateSpy)
  window.removeEventListener('resize', updateSpy)
  if (spyRaf) cancelAnimationFrame(spyRaf)
})
</script>

<template>
  <header class="nav">
    <div class="nav__inner">
      <a class="nav__brand" href="#" @click.prevent="onNav({ to: HOME_PATH })">
        <DragonEmblem class="nav__mark" />
        <div class="nav__brand-text">
          <span class="nav__brand-cn">卡塞尔学院</span>
          <span class="nav__brand-en">Cassell College</span>
        </div>
      </a>

      <nav class="nav__links">
        <button
          v-for="item in items"
          :key="item.label"
          type="button"
          class="nav__link"
          :class="{ 'is-current': isActive(item) }"
          :aria-current="isActive(item) ? 'page' : undefined"
          @click="onNav(item)"
        >
          {{ item.label }}
        </button>
      </nav>

      <!-- 后台管理系统入口：独立描边按钮，未登录时由路由守卫转 /admin/login -->
      <button
        type="button"
        class="nav__admin"
        :class="{ 'is-current': onAdmin }"
        :aria-current="onAdmin ? 'page' : undefined"
        :title="adminReady ? '进入后台管理系统' : '前往后台管理系统登录页（仅限授权管理员）'"
        @click="goAdmin"
      >
        <AppIcon name="shield" :size="14" :stroke="1.8" />
        <span class="nav__admin-text">{{ ADMIN_ENTRY.label }}</span>
        <i v-if="adminReady" class="nav__admin-dot" title="管理员已登录" />
      </button>

      <!-- 学院标准时间（右上角 · 邮戳标识） -->
      <div class="nav__clock" title="卡塞尔学院 · 标准时间">
        <i class="stamp">龍</i>
        <span>{{ clockText }}</span>
      </div>

      <!-- 用户区 -->
      <div ref="userBox" class="nav__user" @click="toggleMenu">
        <span class="nav__user-avatar">{{ codename.slice(0, 1) }}</span>
        <span class="nav__user-name">{{ codename }}</span>
        <AppIcon name="chevron" :size="13" class="nav__user-caret" :stroke="2" />

        <transition name="drop">
          <div v-if="menuOpen" class="nav__menu" @click.stop>
            <div class="nav__menu-head">
              <p class="nav__menu-name">{{ codename }}</p>
              <p class="nav__menu-meta">
                {{ account }}
                <template v-if="identityName"> · {{ identityName }}</template>
              </p>
            </div>
            <router-link to="/console" class="nav__menu-item" @click="menuOpen = false">
              <AppIcon name="id" :size="15" /> 我的身份卡
            </router-link>
            <button type="button" class="nav__menu-item is-danger" @click="onLogout">
              <AppIcon name="logout" :size="15" /> 退出登录
            </button>
          </div>
        </transition>
      </div>

      <!-- 额外操作区：详情页的「返回」按钮等 -->
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(5, 7, 13, 0.78);
  backdrop-filter: blur(16px) saturate(140%);
  -webkit-backdrop-filter: blur(16px) saturate(140%);
  border-bottom: 1px solid var(--line);
  animation: fade-down 0.7s both;
}

.nav::after {
  content: '';
  position: absolute;
  left: 20%;
  right: 20%;
  bottom: -1px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
  opacity: 0.55;
}

.nav__inner {
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 26px;
  padding: 13px 24px;
}

.nav__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
  cursor: pointer;
}

.nav__mark {
  width: 38px;
  height: 38px;
  filter: drop-shadow(0 0 10px rgba(216, 178, 90, 0.5));
  animation: spin 46s linear infinite;
}

.nav__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.nav__brand-cn {
  font-family: var(--font-serif);
  font-size: 16px;
  letter-spacing: 0.24em;
  color: var(--gold-lt);
}

.nav__brand-en {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.3em;
  color: var(--text-4);
  text-transform: uppercase;
}

.nav__links {
  flex: 1;
  display: flex;
  justify-content: center;
  gap: 4px;
  min-width: 0;
}

.nav__link {
  position: relative;
  padding: 8px 15px;
  border: 0;
  background: none;
  font: inherit;
  font-size: 13px;
  border-radius: 999px;
  letter-spacing: 0.1em;
  color: var(--text-2);
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.25s, background 0.25s, box-shadow 0.25s;
}

.nav__link:hover {
  color: var(--text);
  background: rgba(216, 178, 90, 0.08);
}

/* 当前区块 / 当前页面高亮 */
.nav__link.is-current {
  color: var(--gold-lt);
  font-weight: 600;
  background: linear-gradient(135deg, rgba(216, 178, 90, 0.16), rgba(216, 178, 90, 0.04));
  box-shadow: inset 0 0 0 1px rgba(216, 178, 90, 0.26), 0 0 20px -10px var(--gold-glow);
}

.nav__link.is-current::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 1px;
  width: 16px;
  height: 2px;
  transform: translateX(-50%);
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, var(--gold-lt), transparent);
  box-shadow: 0 0 10px rgba(216, 178, 90, 0.9);
  animation: nav-glow 2.6s ease-in-out infinite;
}

@keyframes nav-glow {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

/* ===== 后台管理系统入口按钮 ===== */
.nav__admin {
  flex: 0 0 auto;
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.42);
  background: linear-gradient(135deg, rgba(216, 178, 90, 0.13), rgba(216, 178, 90, 0.03));
  font: inherit;
  font-size: 12.5px;
  letter-spacing: 0.08em;
  color: var(--gold-lt);
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.25s, border-color 0.25s, box-shadow 0.25s, transform 0.25s;
}

.nav__admin:hover {
  color: #f6e7bd;
  border-color: rgba(216, 178, 90, 0.75);
  box-shadow: 0 0 20px -8px var(--gold-glow);
  transform: translateY(-1px);
}

.nav__admin:active {
  transform: translateY(0);
}

/* 已在后台时高亮 */
.nav__admin.is-current {
  color: #fff6df;
  border-color: var(--gold);
  background: linear-gradient(135deg, rgba(216, 178, 90, 0.3), rgba(216, 178, 90, 0.1));
  box-shadow: inset 0 0 0 1px rgba(216, 178, 90, 0.35), 0 0 22px -10px var(--gold-glow);
}

/* 管理员已登录标记 */
.nav__admin-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--gold-lt);
  box-shadow: 0 0 8px var(--gold-glow);
}

.nav__clock {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--text-4);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.nav__user {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 12px 6px 7px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.55);
  cursor: pointer;
  user-select: none;
  transition: border-color 0.25s, box-shadow 0.25s;
}

.nav__user:hover {
  border-color: rgba(216, 178, 90, 0.45);
  box-shadow: 0 0 18px -6px var(--gold-glow);
}

.nav__user-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-family: var(--font-serif);
  font-size: 13px;
  color: #1a1206;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.nav__user-name {
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: var(--text);
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav__user-caret {
  color: var(--text-4);
  transition: transform 0.25s;
}

.nav__user:hover .nav__user-caret {
  color: var(--gold);
}

.nav__menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 208px;
  border-radius: var(--r-md);
  background: linear-gradient(165deg, rgba(20, 28, 46, 0.96), rgba(8, 12, 22, 0.98));
  border: 1px solid var(--line);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.nav__menu-head {
  padding: 13px 16px 11px;
  border-bottom: 1px solid var(--line-soft);
}

.nav__menu-name {
  font-family: var(--font-serif);
  font-size: 15px;
  letter-spacing: 0.1em;
  color: var(--gold-lt);
}

.nav__menu-meta {
  margin-top: 3px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.nav__menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 16px;
  font-size: 12.5px;
  letter-spacing: 0.05em;
  color: var(--text-2);
  background: none;
  border: 0;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.nav__menu-item:hover {
  background: rgba(216, 178, 90, 0.08);
  color: var(--text);
}

.nav__menu-item.is-danger:hover {
  background: rgba(179, 39, 45, 0.14);
  color: var(--blood-lt);
}

.drop-enter-active,
.drop-leave-active {
  transition: opacity 0.22s, transform 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

.drop-enter-from,
.drop-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ================= 响应式 ================= */
@media (max-width: 1080px) {
  .nav__inner {
    gap: 16px;
  }

  .nav__link {
    padding: 8px 12px;
  }
}

@media (max-width: 860px) {
  .nav__links {
    display: none;
  }

  .nav__inner {
    justify-content: space-between;
  }

  .nav__clock {
    display: none;
  }
}

@media (max-width: 560px) {
  .nav__user-name {
    display: none;
  }

  /* 窄屏只留盾牌图标，标题仍说明用途 */
  .nav__admin-text {
    display: none;
  }

  .nav__admin {
    padding: 7px 10px;
  }
}
</style>
