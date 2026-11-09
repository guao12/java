<script setup>
/**
 * 全院统一顶部导航栏。
 *
 * 论坛、档案馆、好友、血统鉴定、档案详情页均复用本组件，
 * 保证品牌区 / 菜单项 / 学院时钟 / 用户菜单完全一致。
 *
 * - 菜单数据来自 data/nav.js 的 SITE_NAV（单一数据源）
 * - 带 `id` 的菜单项指向首页区块：已在首页则平滑滚动；在其他页面则先回首页再定位
 * - 带 `match` 的菜单项把这些路径前缀都算作命中（高亮）
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import { authApi } from '@/api/auth'
import { friendApi } from '@/api/friend'
import { request } from '@/api/http'
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
/** 游客态：论坛公开可读，未登录时导航右侧要给出登录 / 注册入口，而不是一个空的账号菜单 */
const logged = computed(() => !!user.value)
const codename = computed(() => user.value?.codename || '读者')
const account = computed(() => user.value?.account || '')
const identityName = computed(() => user.value?.identityName || '')

// 登录 / 退出后路由会变，导航随之刷新账号态（SiteNav 常驻，不会重新挂载）
watch(
  () => route.fullPath,
  () => {
    user.value = getUser()
    // 换了账号就要换一份角标数字：退出时清零，登录时立刻取一次
    pollBadge()
  }
)

const menuOpen = ref(false)
const userBox = ref(null)

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}
function closeMenu() {
  menuOpen.value = false
}

/* ---------------- 移动端抽屉菜单（≤860px 时代替 .nav__links） ---------------- */

const drawerOpen = ref(false)

/** 抽屉展开时锁住页面滚动，收起 / 卸载时一定要还原 */
function syncBodyLock() {
  if (typeof document === 'undefined') return
  document.body.style.overflow = drawerOpen.value ? 'hidden' : ''
}

function toggleDrawer() {
  drawerOpen.value = !drawerOpen.value
  syncBodyLock()
}

function closeDrawer() {
  if (!drawerOpen.value) return
  drawerOpen.value = false
  syncBodyLock()
}

/** Esc 收起抽屉与用户下拉 */
function onKeydown(e) {
  if (e.key !== 'Escape') return
  closeDrawer()
  closeMenu()
}

/** 视口变宽（旋转平板等）时抽屉要自动收起，否则会和新露出的导航打架 */
function onResize() {
  if (window.innerWidth > 860) closeDrawer()
}

// 路由变化后收起抽屉
watch(
  () => route.fullPath,
  () => closeDrawer()
)

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
  request.clearCache() // 清空 GET 缓存，避免下一位登录者读到本账号的缓存
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

/** 菜单里所有「页面内分卷」目标（如 言灵图鉴 → /archive#tab-spirits） */
const hashClaims = computed(() =>
  props.items.filter((i) => i.hash).map((i) => ({ path: i.to, hash: `#${i.hash}` }))
)

/** 该项命中的最长路径前缀长度；未命中返回 -1 */
function matchLen(item) {
  const list = item.match?.length ? item.match : [item.to]
  let len = -1
  for (const p of list) {
    if (route.path === p || route.path.startsWith(`${p}/`)) len = Math.max(len, p.length)
  }
  return len
}

/**
 * 页面型菜单项的唯一归属项：取「命中前缀最长」的那一项。
 *
 * 为什么需要：`/board/fanwork` 同时命中「讨论区」（/board）和「同人」（/board/fanwork），
 * 不做仲裁会两个标签一起点亮。用最长前缀让更具体的菜单项胜出，
 * 以后再加 /board/xxx 的子入口都不用改这里。
 */
const navOwner = computed(() => {
  let owner = null
  let best = -1
  for (const item of props.items) {
    if (item.id || item.hash) continue // 锚点项 / 分卷项走各自判定
    const len = matchLen(item)
    if (len > best) {
      best = len
      owner = item
    }
  }
  return owner
})

function isActive(item) {
  if (item.id) return onHome.value && activeId.value === item.id
  // 带 hash 的分卷项：路径 + hash 同时命中才算
  if (item.hash) return route.path === item.to && route.hash === `#${item.hash}`
  // 父级页面项让位：该页已有分卷项被选中时不再双重高亮（否则「档案室」「言灵图鉴」同时亮）
  if (hashClaims.value.some((c) => c.path === item.to && route.hash === c.hash)) return false
  // 页面型项只由最具体的那个命中项点亮
  return item === navOwner.value
}

function onNav(item) {
  closeMenu()
  closeDrawer()
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
  // 目标页内的分卷：跨页直达 / 同页切换都走同一个 hash（目标页监听 route.hash 自行切卷）
  if (item.hash) {
    const hash = `#${item.hash}`
    if (route.path === item.to && route.hash === hash) {
      window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' })
      return
    }
    router.push({ path: item.to, hash })
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

/* ---------------- 未读私信角标 ---------------- */

/**
 * 只轮询一个极小的接口（/api/friend/badge，返回 unread / pending）。
 *
 * 导航是全站常驻组件，轮询策略直接影响整站空转开销，所以：
 *  - 未登录不轮询（游客没有私信）
 *  - 页面切到后台标签页时跳过本轮
 *  - 失败静默，下一轮自己恢复，不弹提示打扰阅读
 *
 * 登录 / 退出会改变路由，上面的 watch 已负责刷新 user，这里跟着重算一次。
 */
const unread = ref(0)
let badgeTimer = 0

async function pollBadge() {
  if (!logged.value) {
    unread.value = 0
    return
  }
  if (typeof document !== 'undefined' && document.hidden) return
  try {
    const data = await friendApi.badge()
    unread.value = data?.unread || 0
  } catch {
    /* 静默 */
  }
}

/* ---------------- 滚动高亮（仅首页区块） ---------------- */

/** 阅读进度（0~1）：导航底部那条鎏金线，随页面滚过的高度生长 */
const progress = ref(0)

let spyRaf = 0
function updateSpy() {
  if (spyRaf) return
  spyRaf = requestAnimationFrame(() => {
    spyRaf = 0

    // 进度线：复用同一次 rAF，不为它单独挂 scroll 监听
    const scrollable = document.documentElement.scrollHeight - window.innerHeight
    progress.value = scrollable > 0 ? Math.min(1, Math.max(0, window.scrollY / scrollable)) : 0

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
  document.addEventListener('keydown', onKeydown)
  window.addEventListener('resize', onResize)
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  window.addEventListener('scroll', updateSpy, { passive: true })
  window.addEventListener('resize', updateSpy)
  updateSpy()
  // 未读私信角标：先取一次，再每 20 秒刷新
  pollBadge()
  badgeTimer = window.setInterval(pollBadge, 20000)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKeydown)
  window.removeEventListener('resize', onResize)
  window.clearInterval(clockTimer)
  window.clearInterval(badgeTimer)
  window.removeEventListener('scroll', updateSpy)
  window.removeEventListener('resize', updateSpy)
  if (spyRaf) cancelAnimationFrame(spyRaf)
  // 组件卸载时还原滚动锁，避免整页卡住
  if (typeof document !== 'undefined') document.body.style.overflow = ''
})
</script>

<template>
  <header class="nav">
    <!-- 阅读进度：贴近导航底边的一根鎏金线，滚到底时正好铺满 -->
    <span class="nav__progress" aria-hidden="true" :style="{ transform: `scaleX(${progress})` }" />

    <div class="nav__inner" :class="{ 'nav__inner--actioned': !!$slots.actions }">
      <a class="nav__brand" href="#" @click.prevent="onNav({ to: HOME_PATH })">
        <DragonEmblem class="nav__mark" />
        <div class="nav__brand-text">
          <span class="nav__brand-cn">龙族读者论坛</span>
          <span class="nav__brand-en">Dragon Raja Forum</span>
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
          <!-- 未读私信角标：只有数据里声明了 badge:'unread' 的菜单项才显示 -->
          <i v-if="item.badge === 'unread' && unread > 0" class="nav__badge">
            {{ unread > 99 ? '99+' : unread }}
          </i>
        </button>
      </nav>

      <!-- 移动端（≤860px）菜单按钮：桌面隐藏，导航链接收起后由它打开抽屉 -->
      <button
        type="button"
        class="nav__burger"
        :aria-expanded="drawerOpen"
        aria-controls="nav-drawer"
        :aria-label="drawerOpen ? '收起主菜单' : '展开主菜单'"
        @click.stop="toggleDrawer"
      >
        <span class="burger" :class="{ 'is-open': drawerOpen }" aria-hidden="true">
          <i /><i /><i />
        </span>
      </button>

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

      <!-- 本站标准时间（右上角 · 邮戳标识） -->
      <div class="nav__clock" title="本站 · 标准时间">
        <i class="stamp">龍</i>
        <span>{{ clockText }}</span>
      </div>

      <!-- 游客区：论坛公开可读，未登录时直接给登录 / 注册入口 -->
      <div v-if="!logged" class="nav__guest">
        <router-link to="/login" class="nav__login">登录</router-link>
        <router-link to="/register" class="nav__reg">注册</router-link>
      </div>

      <!-- 用户区 -->
      <div v-else ref="userBox" class="nav__user" @click="toggleMenu">
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
              <AppIcon name="id" :size="15" /> 我的账号
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

    <!-- 移动端抽屉：与桌面导航同源（items / ADMIN_ENTRY），不额外维护菜单数据 -->
    <transition name="drawer">
      <div v-if="drawerOpen" id="nav-drawer" class="nav__drawer">
        <button
          v-for="item in items"
          :key="item.label"
          type="button"
          class="nav__drawer-item"
          :class="{ 'is-current': isActive(item) }"
          :aria-current="isActive(item) ? 'page' : undefined"
          @click="onNav(item)"
        >
          <span>
            {{ item.label }}
            <i v-if="item.badge === 'unread' && unread > 0" class="nav__badge">
              {{ unread > 99 ? '99+' : unread }}
            </i>
          </span>
          <AppIcon v-if="isActive(item)" name="chevron" :size="12" :stroke="2" />
        </button>

        <span class="nav__drawer-sep" aria-hidden="true" />

        <button
          type="button"
          class="nav__drawer-item nav__drawer-item--admin"
          :class="{ 'is-current': onAdmin }"
          @click="goAdmin"
        >
          <span><AppIcon name="shield" :size="13" :stroke="1.8" /> {{ ADMIN_ENTRY.label }}</span>
          <i v-if="adminReady" class="nav__admin-dot" />
        </button>
      </div>
    </transition>
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

/* 阅读进度线：压在底边上，用 scaleX 而非 width —— 只走合成层，不触发重排 */
.nav__progress {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  z-index: 1;
  transform: scaleX(0);
  transform-origin: 0 50%;
  background: linear-gradient(90deg, rgba(216, 178, 90, 0.3), var(--gold), #fffdf3);
  box-shadow: 0 0 10px rgba(216, 178, 90, 0.7);
  /* 只给 transform 加过渡，滚轮高速滚动时线也不会「追不上」而抖动 */
  transition: transform 0.1s linear;
  pointer-events: none;
}

.nav__inner {
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 26px;
  padding: 13px 24px;
}

/* 详情页会通过 actions 插槽在右侧追加「返回」按钮，
   此时导航里共 5 组内容，1080px 装不下：.nav__links 是 flex:1 + min-width:0，
   会被压到比按钮自身还窄，按钮向两侧溢出、压住品牌区和后台入口的文字。
   这里为带 actions 的导航单独放宽宽度、收紧间距，并隐藏纯装饰的学院时钟。 */
.nav__inner--actioned {
  max-width: 1160px;
  gap: 14px;
}

.nav__inner--actioned .nav__links {
  flex: 0 0 auto;
}

.nav__inner--actioned .nav__link {
  padding: 8px 12px;
}

.nav__inner--actioned .nav__clock {
  display: none;
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
  /* 不能用 min-width:0：菜单项是 nowrap 的固定宽度按钮，
     容器一旦被压窄，按钮会从两侧溢出并压住品牌区 / 后台入口的文字。
     max-content 让它以按钮实际总宽为下限，宁可整行更宽也不要叠字。 */
  min-width: max-content;
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
  transition:
    color var(--t-base) var(--ease-soft),
    background var(--t-base) var(--ease-soft),
    box-shadow var(--t-base) var(--ease-soft);
}

/* 未选中项的 hover：从中心向两侧长出一条极细金线，
   与「已选中」那条静态短线区分开 —— 一个是当下，一个是意图 */
.nav__link::before {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 3px;
  width: 0;
  height: 1px;
  transform: translateX(-50%);
  background: linear-gradient(90deg, transparent, rgba(216, 178, 90, 0.85), transparent);
  transition: width var(--t-base) var(--ease-out);
  pointer-events: none;
}

.nav__link:hover::before,
.nav__link:focus-visible::before {
  width: 62%;
}

/* 当前项已有高亮胶囊 + 呼吸金线，不再叠一层生长线 */
.nav__link.is-current::before {
  display: none;
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

/* 未读私信角标：挂在菜单文字右上角，不改变导航高度 */
.nav__badge {
  display: inline-block;
  min-width: 15px;
  margin-left: 4px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--blood);
  color: #fff;
  font-size: 9.5px;
  font-style: normal;
  font-weight: 600;
  line-height: 15px;
  letter-spacing: 0;
  text-align: center;
  vertical-align: 1px;
  box-shadow: 0 0 10px -2px rgba(179, 39, 45, 0.9);
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

/* ===== 移动端菜单按钮 + 抽屉 ===== */
.nav__burger {
  display: none;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: rgba(4, 7, 13, 0.55);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: border-color 0.25s, box-shadow 0.25s;
}

.nav__burger:hover {
  border-color: rgba(216, 178, 90, 0.5);
  box-shadow: 0 0 18px -6px var(--gold-glow);
}

/* 三条线用 CSS 画，不依赖图标库新增 path */
.burger {
  position: relative;
  display: block;
  width: 16px;
  height: 12px;
}

.burger i {
  position: absolute;
  left: 0;
  width: 100%;
  height: 1.5px;
  border-radius: 2px;
  background: var(--gold-lt);
  transition: transform 0.26s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.2s;
}

.burger i:nth-child(1) {
  top: 0;
}
.burger i:nth-child(2) {
  top: 5px;
}
.burger i:nth-child(3) {
  top: 10px;
}

.burger.is-open i:nth-child(1) {
  transform: translateY(5px) rotate(45deg);
}
.burger.is-open i:nth-child(2) {
  opacity: 0;
}
.burger.is-open i:nth-child(3) {
  transform: translateY(-5px) rotate(-45deg);
}

.nav__drawer {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 32;
  display: grid;
  gap: 2px;
  padding: 8px 16px 14px;
  background: rgba(6, 9, 16, 0.98);
  border-bottom: 1px solid var(--line);
  box-shadow: 0 30px 60px -32px #000;
  backdrop-filter: blur(18px) saturate(140%);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
}

.nav__drawer-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  padding: 13px 10px;
  border: 0;
  border-radius: 10px;
  background: none;
  font: inherit;
  font-size: 14px;
  letter-spacing: 0.1em;
  color: var(--text-2);
  text-align: left;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.2s, color 0.2s;
}

.nav__drawer-item:active,
.nav__drawer-item:hover {
  background: rgba(216, 178, 90, 0.08);
  color: var(--gold-lt);
}

.nav__drawer-item.is-current {
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.1);
}

.nav__drawer-item--admin {
  color: var(--gold-lt);
}

.nav__drawer-item--admin span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.nav__drawer-sep {
  height: 1px;
  margin: 6px 10px;
  background: var(--line-soft);
}

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.2s, transform 0.24s cubic-bezier(0.16, 1, 0.3, 1);
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ===== 后台管理系统入口按钮 ===== */
/* 游客区：与管理员按钮同为描边胶囊，视觉权重略低 */
.nav__guest {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.nav__login {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12.5px;
  letter-spacing: 0.08em;
  color: var(--text-3);
  white-space: nowrap;
  transition: color 0.24s;
}

.nav__login:hover {
  color: var(--gold-lt);
}

.nav__reg {
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.5);
  background: linear-gradient(135deg, rgba(216, 178, 90, 0.18), rgba(216, 178, 90, 0.04));
  font-size: 12.5px;
  letter-spacing: 0.08em;
  color: var(--gold-lt);
  white-space: nowrap;
  transition: color 0.24s, border-color 0.24s, box-shadow 0.24s;
}

.nav__reg:hover {
  color: #f6e7bd;
  border-color: rgba(216, 178, 90, 0.85);
  box-shadow: 0 0 20px -8px var(--gold-glow);
}

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
/* 菜单项现在以 max-content 为下限，导航不再叠字，但整行需要约 1240px 才装得下：
   往下逐级让出装饰性内容，避免出现横向滚动条。 */
@media (max-width: 1320px) {
  .nav__clock {
    display: none;
  }
}

@media (max-width: 1080px) {
  .nav__inner {
    gap: 16px;
  }

  .nav__link {
    padding: 8px 12px;
  }
}

@media (max-width: 1100px) {
  .nav__inner {
    gap: 12px;
  }

  .nav__link {
    padding: 8px 10px;
  }

  /* 只留盾牌图标；按钮有 title 提示用途 */
  .nav__admin-text {
    display: none;
  }

  .nav__admin {
    padding: 7px 10px;
  }

  /* 账号名收进头像，展开菜单里仍能看到完整信息 */
  .nav__user-name {
    display: none;
  }
}

@media (max-width: 860px) {
  /* 导航链接收进抽屉，不再让主流程断掉：
     下方 .nav__burger 负责打开，.nav__admin 也移入抽屉，
     避免手机上「菜单 + 盾牌 + 头像」三个圆形按钮挤在一行。 */
  .nav__links {
    display: none;
  }

  .nav__inner {
    justify-content: flex-start;
    padding: 11px 16px;
  }

  .nav__burger {
    display: grid;
    margin-left: auto;
  }

  .nav__admin {
    display: none;
  }

  .nav__clock {
    display: none;
  }

  .nav__mark {
    width: 34px;
    height: 34px;
  }

  .nav__brand-cn {
    font-size: 15px;
    letter-spacing: 0.16em;
  }

  .nav__brand-en {
    font-size: 8px;
    letter-spacing: 0.24em;
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
