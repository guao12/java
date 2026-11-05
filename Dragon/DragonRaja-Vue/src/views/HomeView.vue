<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import { homeApi } from '@/api/home'
import { getUser } from '@/utils/auth'
import { gradeOf, myLatestReport } from '@/utils/bloodline'
import { ORG_GALLERY, codexPath } from '@/data/codex'
import { useSpirits } from '@/data/spiritStore'
import { level as clearanceLevel, levelLabel, points } from '@/utils/clearance'
import {
  CATALOG_CATEGORIES,
  CATALOG_HOT,
  CATALOG_HOT_KEYWORDS,
  CATALOG_LATEST,
  CATALOG_SPOTLIGHT,
  CATALOG_TOTALS,
  searchCatalog
} from '@/data/catalog'
import { markIn } from '@/utils/reveal'

const route = useRoute()
const router = useRouter()

/**
 * 当前登录者
 */
const user = ref(getUser())

/* ---------------- 老锚点改道（言灵图鉴已迁至档案室） ----------------
 * 书签 / 历史记录里还留着 /home#sec-codex，那个区块已经不存在了。
 * 首次加载靠 onMounted 兜，同页内改 hash 走 watcher —— 否则点了会「什么都没发生」。
 */
function leaveLegacyCodexAnchor() {
  if (route.hash !== '#sec-codex') return false
  router.replace({ path: '/archive', hash: '#tab-spirits' })
  return true
}
watch(() => route.hash, leaveLegacyCodexAnchor)

/**
 * 顶部导航统一由 components/SiteNav.vue 渲染（与任务 / 档案室 / 血统鉴定同源）。
 * 点击首页区块锚点时先退出检索视图，让区块重新挂载，再由 SiteNav 平滑滚动定位。
 */
function onNavAnchor() {
  if (searched.value) resetAll()
}

/* ---------------- 首页概览 ---------------- */

const overview = ref(null)
const overviewError = ref(false)

/** 当前访问者的档案权限（可见密级上限） */
const access = computed(() => overview.value?.access || {})

/**
 * 可查阅密级：直接取档案室那套「权限点 → 密级」（utils/clearance.js），
 * 不再用后端按身份给的上限 —— 否则首页说「可查阅公开」，档案室却因为
 * 攒了 34 点能看绝密，同一个用户在两个页面看到两套权限。
 */
const maxLevel = computed(() => clearanceLevel.value)

/** 四卷分类：与档案室同源（data/catalog.js），条数为真实馆藏数 */
const categories = computed(() =>
  CATALOG_CATEGORIES.map((c) => ({ ...c, count: CATALOG_TOTALS[c.id] ?? 0 }))
)

const notices = computed(() => overview.value?.notices || [])
/** 热门检索词：取自卷宗目录里确实存在的关键词，点了必有结果 */
const hotKeywords = computed(() => (CATALOG_HOT_KEYWORDS.length ? CATALOG_HOT_KEYWORDS : []))

/** 首页三大书架：精选 / 热门 / 最新，全部来自同一份卷宗目录 */
const SHELVES = [
  { key: 'spotlight', title: '精选卷宗', icon: 'sparkle', list: CATALOG_SPOTLIGHT },
  { key: 'hot', title: '热门卷宗', icon: 'flame', list: CATALOG_HOT },
  { key: 'latest', title: '最新归档', icon: 'ticket', list: CATALOG_LATEST }
]

/** 首页大标题逐字入场：拆成单字交给 .char-in 按序升起（--i 控制延迟） */
const HERO_TITLE = '龙文档案馆'
const heroChars = Array.from(HERO_TITLE)

async function loadOverview() {
  const data = await homeApi.overview()
  overview.value = data
  // 卷宗内容已改为读本地卷宗目录（data/catalog.js），后端只负责通报；
  // 这里只标记「后端是否连上」，用于提示通报可能为空。
  overviewError.value = !data || data.fallback === true
}

/* ---------------- 检索 ---------------- */

const keyword = ref('')
/** 是否已显式执行过检索：false 时展示首页内容，true 时展示检索结果 */
const searched = ref(false)
const lastKeyword = ref('')

/** 筛选条件 */
const topCategory = ref(null)
const category = ref(null)
const level = ref(null)
const sort = ref('heat')
const page = ref(1)
const SIZE = 12

/** 检索结果 */
const result = ref(null)
const searchError = ref('')

/** 摘要截断：结果卡只留一行 */
function clip(text, n = 30) {
  if (!text) return ''
  return text.length > n ? `${text.slice(0, n)}…` : text
}

const list = computed(() => result.value?.page?.list || [])
const total = computed(() => result.value?.total || 0)
const pages = computed(() => result.value?.page?.pages || 0)

/* ---------------- 检索空态里的「言灵图鉴」指路 ----------------
   检索只覆盖卷宗目录（四卷），不包含言灵卡片 —— 图鉴整体搬到档案室后，
   按老印象搜「言灵图鉴」会零结果。空态里给一条明确出路，
   避免用户以为图鉴被删了。命中具体言灵名时直接送到它的档案页。 */
const spiritList = useSpirits()

const spiritHint = computed(() => {
  if (!searched.value) return null
  const kw = (lastKeyword.value || '').trim()
  if (!kw) return null
  const hit = spiritList.value.find((s) => s.key === kw || s.name === kw || s.name.includes(kw))
  if (hit) return { to: { path: `/spirit/${hit.key}` }, label: `言灵「${hit.name}」已收录，点此查阅` }
  if (/言灵|图鉴/.test(kw)) {
    return { to: { path: '/archive', hash: '#tab-spirits' }, label: '言灵图鉴已迁至档案室，点此打开' }
  }
  return null
})

/** 当前选中的一级分类对象 */
const activeTop = computed(
  () => categories.value.find((c) => c.id === topCategory.value) || null
)
/** 二级分类：跟随一级分类切换 */
const subCategories = computed(() =>
  topCategory.value ? (activeTop.value?.children || []) : []
)

/** 二级分类条数：与一级分类口径一致，检索后改用分面数据（随关键词联动） */
const subFacets = computed(() => {
  const facets = result.value?.categoryFacets
  if (!facets?.length) return subCategories.value
  const map = new Map(facets.map((f) => [f.key, f.total]))
  return subCategories.value.map((s) => ({ ...s, count: map.get(s.id) ?? 0 }))
})

/** 一级分类条数：检索过用分面数据；未检索时用馆藏真实条数 */
const topFacets = computed(() => {
  if (result.value?.topCategoryFacets?.length) {
    const map = new Map(result.value.topCategoryFacets.map((f) => [f.key, f.total]))
    return categories.value.map((c) => ({ ...c, count: map.get(c.id) ?? 0 }))
  }
  return categories.value.map((c) => ({ ...c, count: c.count ?? 0 }))
})

/** 密级选项：1 公开 / 2 内部 / 3 机密 / 4 绝密 */
const LEVELS = [
  { value: 1, label: '公开', tone: 'ok' },
  { value: 2, label: '内部', tone: 'ice' },
  { value: 3, label: '机密', tone: 'warn' },
  { value: 4, label: '绝密', tone: 'err' }
]

/** 密级条数：检索过用分面数据；未检索时后端未下发，显示「—」 */
const levelFacets = computed(() => {
  const fromApi = result.value?.levelFacets || []
  const map = new Map(fromApi.map((f) => [f.key, f.total]))
  return LEVELS.map((l) => ({
    ...l,
    total: fromApi.length ? (map.get(l.value) ?? 0) : null,
    locked: l.value > maxLevel.value
  }))
})

const SORTS = [
  { value: 'heat', label: '按热度' },
  { value: 'new', label: '按最新' },
  { value: 'view', label: '按浏览' },
  { value: 'level', label: '按密级' }
]

const hasFilter = computed(
  () => !!keyword.value.trim() || topCategory.value || category.value || level.value
)

/** 构造检索参数（口径与 data/catalog.js 的 searchCatalog 对应） */
function buildParams() {
  return {
    keyword: keyword.value.trim() || undefined,
    top: topCategory.value || undefined,
    sub: category.value || undefined,
    level: level.value || undefined,
    sort: sort.value,
    page: page.value,
    size: SIZE
  }
}

/**
 * 执行检索。
 * 数据源是 data/catalog.js（档案室四卷目录），不再是后端 dr_archive ——
 * 这样首页搜到的每一条，在档案室里都能找到同一份卷宗，点进去也是同一个详情页。
 *
 * @param {boolean} silent true = 只更新分面统计，不切换到结果视图（首屏 chips 条数用）
 */
function runSearch(silent = false) {
  searchError.value = ''
  try {
    result.value = searchCatalog(buildParams())
    if (!silent) {
      searched.value = true
      lastKeyword.value = keyword.value.trim()
    }
  } catch (e) {
    searchError.value = e?.message || '检索失败，请稍后重试'
    if (!silent) result.value = null
  }
}

/** 用户主动检索（回车 / 点按钮 / 点热词） */
function doSearch(kw) {
  if (kw !== undefined) keyword.value = kw
  page.value = 1
  runSearch(false)
}

function pickHot(word) {
  keyword.value = word
  page.value = 1
  runSearch(false)
}

/** 切换一级分类：再点一次取消 */
function toggleTop(id) {
  if (topCategory.value === id) {
    topCategory.value = null
    category.value = null
  } else {
    topCategory.value = id
    category.value = null
  }
  page.value = 1
  runSearch(false)
}

/** 切换二级分类 */
function toggleSub(id) {
  category.value = category.value === id ? null : id
  page.value = 1
  runSearch(false)
}

/** 切换密级：超出权限的不可选 */
function toggleLevel(v) {
  if (v > maxLevel.value) return
  level.value = level.value === v ? null : v
  page.value = 1
  runSearch(false)
}

function changeSort(v) {
  sort.value = v
  page.value = 1
  runSearch(false)
}

function resetAll() {
  keyword.value = ''
  topCategory.value = null
  category.value = null
  level.value = null
  sort.value = 'heat'
  page.value = 1
  searched.value = false
  searchError.value = ''
  runSearch(true)
}

function goPage(p) {
  if (p < 1 || p > pages.value || p === page.value) return
  page.value = p
  runSearch(false)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ---------------- 卷宗详情 ---------------- */

/**
 * 所有卷宗卡片（精选 / 热门 / 最新 / 检索结果）都直接进各自的详情页：
 *   龙王 / 部门人物 / 龙族武器 → /archive/:kind/:key
 *   任务执行记录             → /log/:code/:person
 * 密级不足时不在这里拦——详情页会给「已封存 + 还差多少权限点」，
 * 比首页弹一句「权限不足」更有下文（原来点高级别卡片是纯假反馈）。
 */
function openDossier(item) {
  if (!item?.path) return
  router.push(item.path)
}

/* ---------------- 机构图鉴 ----------------
   言灵图鉴已搬到档案室（第五个分卷），首页只剩「学院机构」这一个图鉴区块。 */

/** 机构卡片点击 → /org/:key 机构档案 */
function goCodex(kind, key) {
  router.push(codexPath(kind, key))
}

/* ---------------- 顶部公告横幅 ---------------- */

/** 横幅收折状态：纯组件内，刷新即重置为展开（不记忆关闭状态，保证每次进来都有预警压迫感） */
const bannerCollapsed = ref(false)

/** 优先展示置顶公告，否则取最新一条 */
const bannerNotice = computed(() => {
  const list = notices.value
  if (!list.length) return null
  return list.find((v) => v.pinned) || list[0]
})

/** 横幅以下保留的通报列表（不与横幅重复） */
const restNotices = computed(() =>
  notices.value.filter((n) => !(bannerNotice.value && n.id === bannerNotice.value.id))
)

/** 收起为细线 / 点击细线再展开 */
function dismissBanner() {
  bannerCollapsed.value = true
}
function expandBanner() {
  bannerCollapsed.value = false
}

/* ---------------- 时间格式化（学院时钟已随导航栏移入 SiteNav） ---------------- */

const pad2 = (n) => String(n).padStart(2, '0')

/** 通报时间美化：2025-09-01 10:00 → 2025.09.01 10:00 */
function formatDate(v) {
  if (!v) return ''
  const d = new Date(String(v).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return v
  return `${d.getFullYear()}.${pad2(d.getMonth() + 1)}.${pad2(d.getDate())} ${pad2(
    d.getHours()
  )}:${pad2(d.getMinutes())}`
}

/* ---------------- 血统鉴定（英雄区） ---------------- */

/**
 * 与 /appraisal 血统鉴定页共用同一份档案与评级标准（utils/bloodline.js）：
 * - 有本人鉴定报告 → 直接展示最新报告的评级与龙血占比；
 * - 尚未送检 → 回退为「密级上限推导」的估算值，并标注为待检测。
 */
const BLOOD_RANKS = { 1: ['C', 12.5], 2: ['B', 25], 3: ['A', 50], 4: ['S', 87.5] }
const bloodTick = ref(0)
const myBloodReport = computed(() => {
  void bloodTick.value // 进入页面时刷新一次
  return myLatestReport(user.value?.account)
})
const bloodRank = computed(() => {
  const r = myBloodReport.value
  if (r) return r.grade || gradeOf(r.pct).grade
  return (BLOOD_RANKS[maxLevel.value] || BLOOD_RANKS[1])[0]
})
const bloodPct = computed(() => {
  const r = myBloodReport.value
  if (r) return Number(r.pct) || 0
  return (BLOOD_RANKS[maxLevel.value] || BLOOD_RANKS[1])[1]
})
/** true = 来自血统鉴定科的实际检测报告；false = 按密级估算的待检测值 */
const bloodTested = computed(() => !!myBloodReport.value)
const bloodNote = computed(() =>
  bloodTested.value
    ? `血统鉴定科 · ${myBloodReport.value?.code || ''} 出具`
    : '未送检 · 血统鉴定科估算值'
)

/* ---------------- 渐进显现（滚动进场，平滑加载） ---------------- */

let revealObs = null

function observeReveals() {
  if (typeof window === 'undefined' || !('IntersectionObserver' in window)) {
    // 不支持时直接全部显示
    document.querySelectorAll('.reveal').forEach((el) => markIn(el))
    return
  }
  if (!revealObs) {
    revealObs = new IntersectionObserver(
      (entries) => {
        for (const en of entries) {
          if (en.isIntersecting) {
            markIn(en.target)
            revealObs.unobserve(en.target)
          }
        }
      },
      { rootMargin: '0px 0px -6% 0px', threshold: 0.05 }
    )
  }
  document.querySelectorAll('.reveal:not([data-rise])').forEach((el) => revealObs.observe(el))
}

/* ---------------- 光标跟随高光（事件委托，不给每张卡单独绑监听） ---------------- */

/**
 * 把指针在元素内的相对坐标写入 --mx / --my，供 .dcard::before 的 radial-gradient 定位。
 * 用 pointermove 而非 mousemove：触屏与手写笔也能正常工作。
 */
function onPointerMove(e) {
  const card = e.target.closest?.('.dcard, .lift')
  if (!card) return
  const rect = card.getBoundingClientRect()
  card.style.setProperty('--mx', `${e.clientX - rect.left}px`)
  card.style.setProperty('--my', `${e.clientY - rect.top}px`)
}

/** 图片加载失败兜底：隐藏 img，露出渐变底 */
function onImgError(e) {
  const img = e.target
  img.style.display = 'none'
  const wrap = img.closest('.rcard__img')
  if (wrap) wrap.classList.add('is-broken')
}

/* ---------------- 快捷入口 ---------------- */

const QUICK = [
  { label: '我的身份卡', desc: '查看血统档案', icon: 'id', to: '/console' },
  { label: '密钥重置', desc: '重铸言灵密钥', icon: 'key', to: '/forgot' },
  { label: '新生登记', desc: '签发学院编号', icon: 'ticket', to: '/register' }
]

onMounted(async () => {
  // 老链接兼容：言灵图鉴区块已搬到档案室，旧的 /home#sec-codex 直接改道，
  // 否则从收藏夹/历史记录点进来会停在首页且毫无反应（锚点已经不存在）。
  if (leaveLegacyCodexAnchor()) return
  document.documentElement.style.colorScheme = 'dark'
  // 每次进入首页都重读一次血统鉴定档案，确保与 /appraisal 的最新报告一致
  bloodTick.value++
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  await loadOverview()
  // 首屏：静默跑一次检索，只为拿到各筛选项的真实条数（不切到结果视图）
  runSearch(true)
  // 渐进显现：等首屏区块渲染完成后再挂观察器
  await nextTick()
  observeReveals()
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove)
  revealObs?.disconnect()
  revealObs = null
})
</script>

<template>
  <div class="home">
    <RuneBackground />

    <!-- ============ 导航栏：全院统一组件（任务 / 档案室 / 血统鉴定同源） ============ -->
    <SiteNav @anchor="onNavAnchor" />

    <!-- ============ 主体 ============ -->
    <main class="home__body">
      <!-- 顶部预警横幅：可收折为细线，刷新恢复展开（不记忆关闭状态） -->
      <transition name="banner">
        <div
          v-if="bannerNotice && !bannerCollapsed"
          key="banner"
          class="banner"
          :data-level="bannerNotice.level || 1"
        >
          <i class="stamp stamp--lg">龍</i>
          <div class="banner__body">
            <p class="banner__title">
              <em v-if="bannerNotice.pinned" class="banner__pin">置顶</em>
              {{ bannerNotice.title }}
            </p>
            <p class="banner__text">{{ bannerNotice.content }}</p>
          </div>
          <span class="banner__meta">
            {{ bannerNotice.levelLabel || '通告' }} · {{ formatDate(bannerNotice.publishAt) }}
          </span>
          <button type="button" class="banner__close" aria-label="收起预警" @click="dismissBanner">
            <AppIcon name="close" :size="14" />
          </button>
        </div>
        <button
          v-else-if="bannerNotice && bannerCollapsed"
          key="collapsed"
          type="button"
          class="banner__collapsed"
          @click="expandBanner"
        >
          <span class="banner__dot" />⚠ 龙王级预警已收起，点击展开
        </button>
      </transition>

      <!-- 权限提示条：与档案室同一套口径（权限点 → 密级） -->
      <div class="access">
        <AppIcon name="shield" :size="14" />
        <span>
          当前身份
          <b>{{ access.identityName || '未认证访客' }}</b>
          · 档案室密级
          <b class="access__lv">「{{ levelLabel }}」</b>
          （{{ points }} 点）· 可查阅该密级及以下卷宗
        </span>
        <router-link v-if="maxLevel < 4" class="access__hint" to="/missions">
          更高密级需完成任务累积权限点
          <AppIcon name="lock" :size="12" />
        </router-link>
      </div>

      <!-- 英雄区 + 搜索栏 -->
      <section id="sec-top" class="hero">
        <p class="hero__eyebrow"><i class="breathe-dot" /> Secret Archive · 龙文档案馆</p>
        <h1 class="hero__title sheen-gold">
          <span v-for="(ch, i) in heroChars" :key="i" class="char-in" :style="{ '--i': i }">{{ ch }}</span>
        </h1>
        <p class="hero__sub">检索混血种名册、言灵图鉴与执行部机密卷宗</p>

        <!-- 血统鉴定：评级 + 龙血占比 -->
        <div class="bloodline">
          <span class="bloodline__rank" :data-rank="bloodRank">
            <b>{{ bloodRank }}</b>级血统
          </span>
          <div class="bloodline__bar" title="龙血占比">
            <i :style="{ width: bloodPct + '%' }" />
          </div>
          <span class="bloodline__pct">龙血占比 <b>{{ bloodPct }}%</b></span>
          <span class="bloodline__note">{{ bloodNote }}</span>
          <router-link v-if="!bloodTested" class="bloodline__link" to="/appraisal">
            <AppIcon name="chevron" :size="11" />前往血统鉴定
          </router-link>
        </div>

        <form class="search" role="search" @submit.prevent="doSearch()">
          <span class="search__icon"><AppIcon name="search" :size="18" /></span>
          <input
            v-model="keyword"
            class="search__input"
            type="search"
            placeholder="输入关键词：卷宗编号 / 标题 / 标签…"
            maxlength="40"
            autocomplete="off"
          />
          <button class="search__btn" type="submit">
            <span>检索</span>
          </button>
        </form>

        <div class="hot">
          <span class="hot__label">热门检索</span>
          <button
            v-for="word in hotKeywords"
            :key="word"
            type="button"
            class="hot__tag"
            @click="pickHot(word)"
          >
            {{ word }}
          </button>
        </div>
      </section>

      <!-- ============ 分级 · 分类筛选器 ============ -->
      <section class="filters">
        <!-- 一级分类 -->
        <div class="filters__row">
          <span class="filters__label">分类</span>
          <div class="chips">
            <button
              type="button"
              class="chip"
              :class="{ 'is-on': !topCategory }"
              @click="toggleTop(null)"
            >
              全部
            </button>
            <button
              v-for="c in topFacets"
              :key="c.id"
              type="button"
              class="chip"
              :class="{ 'is-on': topCategory === c.id }"
              @click="toggleTop(c.id)"
            >
              <AppIcon :name="c.icon || 'archive'" :size="13" />
              {{ c.name }}
              <i class="chip__n">{{ String(c.count).padStart(2, '0') }}</i>
            </button>
          </div>
        </div>

        <!-- 二级分类：选中一级后出现 -->
        <transition name="grow">
          <div v-if="subCategories.length" class="filters__row">
            <span class="filters__label">细目</span>
            <div class="chips">
              <button
                type="button"
                class="chip chip--sm"
                :class="{ 'is-on': !category }"
                @click="toggleSub(null)"
              >
                全部
              </button>
              <button
                v-for="s in subFacets"
                :key="s.id"
                type="button"
                class="chip chip--sm"
                :class="{ 'is-on': category === s.id }"
                @click="toggleSub(s.id)"
              >
                {{ s.name }}
                <i class="chip__n">{{ s.count }}</i>
              </button>
            </div>
          </div>
        </transition>

        <!-- 密级分级 -->
        <div class="filters__row">
          <span class="filters__label">密级</span>
          <div class="chips">
            <button
              type="button"
              class="chip chip--sm"
              :class="{ 'is-on': !level }"
              @click="toggleLevel(null)"
            >
              全部
            </button>
            <button
              v-for="l in levelFacets"
              :key="l.value"
              type="button"
              class="chip chip--sm"
              :class="[`chip--${l.tone}`, { 'is-on': level === l.value }]"
              :disabled="l.locked"
              :title="l.locked ? '权限不足：当前身份不可查阅该密级' : ''"
              @click="toggleLevel(l.value)"
            >
              <AppIcon v-if="l.locked" name="lock" :size="11" />
              {{ l.label }}
              <i v-if="l.total !== null" class="chip__n">{{ l.total }}</i>
            </button>
          </div>
        </div>

        <!-- 排序与重置 -->
        <div class="filters__row filters__row--end">
          <div class="sorts">
            <button
              v-for="s in SORTS"
              :key="s.value"
              type="button"
              class="chip chip--sm"
              :class="{ 'is-on': sort === s.value }"
              @click="changeSort(s.value)"
            >
              {{ s.label }}
            </button>
          </div>
          <button v-if="hasFilter" type="button" class="reset" @click="resetAll">
            <AppIcon name="refresh" :size="12" /> 清除筛选
          </button>
        </div>
      </section>

      <p v-if="searchError" class="tip tip--err">
        <AppIcon name="alert" :size="14" /> {{ searchError }}
      </p>

      <!-- ============ 检索结果 ============ -->
      <section v-if="searched" class="results">
        <header class="results__head">
          <h2 class="results__title">
            检索结果
            <span v-if="lastKeyword" class="results__kw">「{{ lastKeyword }}」</span>
          </h2>
          <span class="results__count">共 {{ total }} 条卷宗</span>
        </header>

        <div v-if="list.length" class="results__grid">
          <article
            v-for="item in list"
            :key="item.uid"
            class="dcard"
            :data-lv="item.level"
            role="button"
            tabindex="0"
            :aria-label="`查阅卷宗：${item.topName} ${item.title}`"
            @click="openDossier(item)"
            @keydown.enter.prevent="openDossier(item)"
            @keydown.space.prevent="openDossier(item)"
          >
            <div class="dcard__top">
              <span class="dcard__code">{{ item.topName }} · {{ item.code }}</span>
              <span class="dcard__lvl">
                <span v-if="item.rankLabel" class="dcard__rank">{{ item.rankLabel }}</span>
                <span class="dcard__sec" :data-lv="item.level">[{{ item.levelLabel }}]</span>
              </span>
            </div>
            <h3 class="dcard__title">{{ item.title }}</h3>
            <p class="dcard__desc">{{ clip(item.summary, 30) }}</p>
            <div class="dcard__foot">
              <span class="dcard__dept">{{ item.subName || item.source }}</span>
              <span class="dcard__dot">·</span>
              <span class="dcard__time">{{ formatDate(item.savedAt) }}</span>
            </div>
            <span class="dcard__arrow"><AppIcon name="arrow" :size="13" /> 查阅→</span>
          </article>
        </div>

        <div v-else class="results__empty">
          <AppIcon name="archive" :size="34" :stroke="1.2" />
          <p>档案馆中未检索到相关卷宗</p>
          <p class="results__empty-sub">换个关键词，或放宽密级 / 分类筛选试试</p>
          <router-link v-if="spiritHint" :to="spiritHint.to" class="results__empty-go">
            <AppIcon name="flame" :size="13" /> {{ spiritHint.label }}
          </router-link>
        </div>

        <!-- 分页 -->
        <nav v-if="pages > 1" class="pager">
          <button class="pager__btn" :disabled="page <= 1" @click="goPage(page - 1)">
            上一页
          </button>
          <span class="pager__info">{{ page }} / {{ pages }}</span>
          <button class="pager__btn" :disabled="page >= pages" @click="goPage(page + 1)">
            下一页
          </button>
        </nav>
      </section>

      <!-- ============ 首页展示（未检索时） ============ -->
      <template v-else>
        <!-- 学院通报（置顶 / 最新一条已提升为顶部横幅） -->
        <section v-if="restNotices.length" class="notice reveal">
          <header class="shelf__head">
            <h2 class="shelf__title"><AppIcon name="alert" :size="15" /> 学院通报</h2>
          </header>
          <div class="notice__list">
            <article
              v-for="n in restNotices"
              :key="n.id"
              class="notice__item"
              :data-level="n.level"
            >
              <span class="notice__badge">
                <i v-if="n.pinned" class="notice__pin">置顶</i>
                {{ n.levelLabel }}
              </span>
              <div class="notice__body">
                <h3 class="notice__title">{{ n.title }}</h3>
                <p class="notice__text">{{ n.content }}</p>
                <p class="notice__meta">
                  <i class="stamp stamp--sm">龍</i>
                  {{ n.publisher }} · {{ formatDate(n.publishAt) }}
                </p>
              </div>
            </article>
          </div>
        </section>

        <!-- 精选 / 热门 / 最新：同源于档案室四卷目录 -->
        <section v-for="shelf in SHELVES" :key="shelf.key" class="shelf reveal">
          <header class="shelf__head">
            <h2 class="shelf__title">
              <AppIcon :name="shelf.icon" :size="15" /> {{ shelf.title }}
            </h2>
            <span class="shelf__more">共 {{ shelf.list.length }} 条 · 点击卡片查看详情</span>
          </header>
          <div v-if="shelf.list.length" class="results__grid">
            <article
              v-for="item in shelf.list"
              :key="item.uid"
              class="card"
              role="button"
              tabindex="0"
              :aria-label="`查阅卷宗：${item.topName} ${item.title}`"
              @click="openDossier(item)"
              @keydown.enter.prevent="openDossier(item)"
              @keydown.space.prevent="openDossier(item)"
            >
              <div class="card__top">
                <span class="card__type">{{ item.topName }}</span>
                <span class="card__rank" :data-lv="item.level">{{ item.levelLabel }}</span>
              </div>
              <h3 class="card__title">{{ item.title }}</h3>
              <p class="card__desc">{{ item.summary }}</p>
              <div class="card__tags">
                <span v-if="item.rankLabel" class="card__tag card__tag--rank">{{ item.rankLabel }}</span>
                <span v-for="tag in item.tags.slice(0, 3)" :key="tag" class="card__tag">
                  {{ tag }}
                </span>
              </div>
              <p class="card__foot">
                <span>{{ item.code }} · {{ item.subName }}</span>
                <span>{{ formatDate(item.savedAt) }}</span>
              </p>
            </article>
          </div>
          <p v-else class="shelf__empty">暂无内容</p>
        </section>

        <!-- 言灵图鉴：已整体搬入档案室（/archive#tab-spirits），首页不再展示 -->

        <!-- 学院机构 -->
        <section id="sec-org" class="gallery reveal">
          <header class="shelf__head">
            <h2 class="shelf__title"><AppIcon name="shield" :size="15" /> 学院机构</h2>
            <span class="shelf__more">点击卡片查看机构职责与密级</span>
          </header>
          <div class="roster__grid">
            <article
              v-for="o in ORG_GALLERY"
              :key="o.key"
              class="rcard rcard--org"
              role="button"
              tabindex="0"
              @click="goCodex('org', o.key)"
              @keydown.enter.prevent="goCodex('org', o.key)"
              @keydown.space.prevent="goCodex('org', o.key)"
            >
              <div class="rcard__img">
                <img
                  :src="`/img/org/${o.key}.webp`"
                  :alt="`机构 · ${o.name}`"
                  loading="lazy"
                  @error="onImgError"
                />
                <span class="rcard__level">{{ o.code }}</span>
                <span class="gcard__hint">查看详情</span>
              </div>
              <div class="rcard__body">
                <h3 class="rcard__name">{{ o.name }}</h3>
                <p class="rcard__desc">{{ o.duty }}</p>
                <div class="rcard__perks">
                  <span v-for="k in o.tags" :key="k" class="rcard__perk">{{ k }}</span>
                </div>
              </div>
            </article>
          </div>
        </section>

        <!-- 快捷入口 -->
        <section class="quick reveal">
          <router-link v-for="q in QUICK" :key="q.label" :to="q.to" class="quick__item">
            <span class="quick__icon"><AppIcon :name="q.icon" :size="20" /></span>
            <span class="quick__text">
              <b>{{ q.label }}</b>
              <i>{{ q.desc }}</i>
            </span>
            <AppIcon name="arrow" :size="15" class="quick__arrow" />
          </router-link>
        </section>

        <p v-if="overviewError" class="tip tip--warn">
          <AppIcon name="alert" :size="14" />
          通报服务未连接（后端 /api/home/overview 不可用）：学院通报暂缺，卷宗内容不受影响
        </p>
      </template>
    </main>

    <!-- ============ 页脚 ============ -->
    <footer class="home__foot">
      <span>以龙档案馆 · 同人兴趣站 · 非商用</span>
      <span class="home__foot-meta">版权归原作者所有 · 仅作学习交流</span>
    </footer>

  </div>
</template>

<style scoped>
/* ================= 布局骨架 ================= */
.home {
  position: relative;
  z-index: 1;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.home__body {
  flex: 1;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  padding: 26px 24px 30px;
}

.home__foot {
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 22px;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 26px;
  border-top: 1px solid var(--line-soft);
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

/* ================= 权限条 ================= */
.access {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(216, 178, 90, 0.05);
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--text-3);
  animation: fade-down 0.6s both;
}

.access b {
  color: var(--text);
  font-weight: 600;
}

.access__lv {
  color: var(--gold-lt) !important;
}

.access__hint {
  margin-left: auto;
  font-style: normal;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-4);
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
}

/* ================= 英雄区 ================= */
.hero {
  text-align: center;
  padding: 40px 0 26px;
  animation: fade-up 0.8s 0.12s both;
}

.hero__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.34em;
  text-transform: uppercase;
  color: var(--gold-dp);
  padding: 5px 14px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(216, 178, 90, 0.06);
}

.hero__eyebrow i {
  width: 4px;
  height: 4px;
  background: var(--gold);
  border-radius: 50%;
}

.hero__title {
  margin-top: 22px;
  font-family: var(--font-serif);
  font-size: clamp(30px, 5.4vw, 44px);
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--text);
  text-shadow: 0 0 34px rgba(216, 178, 90, 0.32);
}

.hero__sub {
  margin-top: 12px;
  font-size: 13.5px;
  letter-spacing: 0.12em;
  color: var(--text-3);
}

.search {
  position: relative;
  display: flex;
  align-items: center;
  max-width: 640px;
  margin: 28px auto 0;
  border-radius: 999px;
  background: rgba(4, 7, 13, 0.72);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-card);
  transition: border-color 0.3s, box-shadow 0.3s;
}

.search:focus-within {
  border-color: var(--gold);
  box-shadow: 0 0 0 4px rgba(216, 178, 90, 0.13), 0 18px 44px -18px var(--gold-glow);
}

.search__icon {
  flex: 0 0 52px;
  display: grid;
  place-items: center;
  color: var(--text-4);
  transition: color 0.3s;
}

.search:focus-within .search__icon {
  color: var(--gold);
}

.search__input {
  flex: 1;
  min-width: 0;
  padding: 16px 0;
  background: none;
  border: 0;
  outline: none;
  font-size: 14.5px;
  letter-spacing: 0.03em;
  color: var(--text);
}

.search__input::placeholder {
  color: var(--text-4);
  letter-spacing: 0.05em;
}

.search__input::-webkit-search-cancel-button {
  -webkit-appearance: none;
}

.search__input:-webkit-autofill {
  -webkit-text-fill-color: var(--text);
  -webkit-box-shadow: 0 0 0 1000px #0a0f1a inset;
}

.search__btn {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 6px;
  padding: 11px 26px;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: #1a1206;
  background: linear-gradient(180deg, var(--gold-lt) 0%, var(--gold) 46%, #b98f38 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55), 0 10px 26px -12px rgba(216, 178, 90, 0.8);
  transition: transform 0.2s, filter 0.25s, box-shadow 0.3s;
}

.search__btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
}

.hot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 9px;
  margin-top: 20px;
}

.hot__label {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: var(--text-4);
  margin-right: 3px;
}

.hot__tag {
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(14, 20, 34, 0.5);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.22s, border-color 0.22s, background 0.22s, transform 0.22s;
}

.hot__tag:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.08);
  transform: translateY(-1px);
}

/* ================= 筛选器 ================= */
.filters {
  margin-top: 30px;
  padding: 18px 20px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: rgba(9, 13, 23, 0.55);
  animation: fade-up 0.6s 0.2s both;
}

.filters__row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 7px 0;
}

.filters__row + .filters__row {
  border-top: 1px dashed rgba(255, 255, 255, 0.05);
}

.filters__row--end {
  align-items: center;
  flex-wrap: wrap;
}

.filters__label {
  flex: 0 0 34px;
  padding-top: 6px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
}

.chips {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(14, 20, 34, 0.5);
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s, transform 0.2s;
}

.chip:hover:not(:disabled) {
  color: var(--text);
  border-color: rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.07);
  transform: translateY(-1px);
}

.chip:disabled {
  cursor: not-allowed;
  opacity: 0.36;
  filter: grayscale(0.5);
}

.chip.is-on {
  color: #1a1206;
  border-color: var(--gold);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
  font-weight: 600;
}

.chip--sm {
  padding: 5px 12px;
  font-size: 12px;
}

.chip__n {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-4);
}

.chip.is-on .chip__n {
  background: rgba(0, 0, 0, 0.22);
  color: rgba(26, 18, 6, 0.75);
}

/* 密级配色 */
.chip--ok.is-on {
  background: linear-gradient(180deg, #7ee2b0, #3fae7b);
  border-color: #3fae7b;
}

.chip--ice.is-on {
  background: linear-gradient(180deg, #a5e4ff, #4aa8d4);
  border-color: #4aa8d4;
}

.chip--warn.is-on {
  background: linear-gradient(180deg, #f4cd83, #cf9436);
  border-color: #cf9436;
}

.chip--err.is-on {
  background: linear-gradient(180deg, #f58d86, #c9483f);
  border-color: #c9483f;
  color: #2a0b0b;
}

.sorts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reset {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(226, 86, 79, 0.32);
  background: rgba(179, 39, 45, 0.1);
  color: var(--blood-lt);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.reset:hover {
  background: rgba(179, 39, 45, 0.2);
  color: #fff;
}

.grow-enter-active,
.grow-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}

.grow-enter-from,
.grow-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ================= 提示条 ================= */
.tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 10px 16px;
  border-radius: var(--r-md);
  font-size: 12.5px;
  letter-spacing: 0.05em;
}

.tip--err {
  border: 1px solid rgba(239, 107, 98, 0.3);
  background: rgba(179, 39, 45, 0.1);
  color: var(--err);
}

.tip--warn {
  border: 1px solid rgba(232, 180, 90, 0.3);
  background: rgba(216, 178, 90, 0.07);
  color: var(--warn);
}

/* ================= 卡片网格（结果 / 书架共用） ================= */
.results__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  grid-auto-rows: 1fr; /* 行内等高，网格对齐 */
  gap: 16px;
}

/* ================= 检索结果卷宗卡（dcard） ================= */
.dcard {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 15px 17px 14px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  box-shadow: 0 0 0 1px rgba(216, 178, 90, 0.05), 0 0 22px -14px rgba(216, 178, 90, 0.3);
  cursor: pointer;
  overflow: hidden;
  transition:
    border-color var(--t-base) var(--ease-soft),
    transform var(--t-base) var(--ease-out),
    box-shadow var(--t-base) var(--ease-soft);
}

/* 光标跟随的暖光：鼠标移到哪，卡片就在哪亮起一小片金辉 */
.dcard::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(
    180px circle at var(--mx, 50%) var(--my, 0%),
    rgba(216, 178, 90, 0.16),
    transparent 70%
  );
  opacity: 0;
  transition: opacity var(--t-base) var(--ease-soft);
  pointer-events: none;
}

.dcard:hover::before,
.dcard:focus-visible::before {
  opacity: 1;
}

.dcard:hover {
  border-color: rgba(216, 178, 90, 0.5);
  transform: translateY(-4px);
  box-shadow:
    0 0 0 1px rgba(216, 178, 90, 0.18),
    0 22px 44px -22px rgba(0, 0, 0, 0.9);
}

/* 键盘聚焦也必须有可见反馈，而不是只有鼠标用户能感知 */
.dcard:focus-visible {
  outline: none;
  border-color: var(--gold);
  box-shadow:
    0 0 0 2px rgba(216, 178, 90, 0.28),
    0 22px 44px -22px rgba(0, 0, 0, 0.9);
}

.dcard__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 9px;
}

.dcard__code {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--gold-dp);
  padding: 3px 9px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(216, 178, 90, 0.06);
}

.dcard__lvl {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.dcard__rank {
  font-family: var(--font-serif);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--gold-lt);
}

/* 密级四色：公开=灰 / 内部=蓝 / 机密=金 / 绝密=暗红 */
.dcard__sec {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--line);
}
.dcard__sec[data-lv='1'] {
  color: var(--text-3);
  border-color: rgba(154, 163, 178, 0.4);
  background: rgba(154, 163, 178, 0.1);
}
.dcard__sec[data-lv='2'] {
  color: #8fc4f0;
  border-color: rgba(74, 168, 212, 0.5);
  background: rgba(74, 168, 212, 0.12);
}
.dcard__sec[data-lv='3'] {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.55);
  background: rgba(216, 178, 90, 0.1);
}
.dcard__sec[data-lv='4'] {
  color: #f0a39c;
  border-color: rgba(239, 107, 98, 0.5);
  background: rgba(179, 39, 45, 0.14);
}

.dcard__title {
  font-family: var(--font-mono);
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dcard__desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-3);
}

.dcard__foot {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 11px;
  padding-top: 9px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.dcard__dot {
  color: var(--line);
}

/* hover 右侧滑出的“查阅→” */
.dcard__arrow {
  position: absolute;
  top: 50%;
  right: 14px;
  transform: translate(16px, -50%);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(216, 178, 90, 0.14);
  border: 1px solid rgba(216, 178, 90, 0.5);
  color: var(--gold-lt);
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.26s, transform 0.26s;
}
.dcard:hover .dcard__arrow {
  opacity: 1;
  transform: translate(0, -50%);
}

/* 悬浮呼吸发光：--el 为卡片主色（言灵卡按元素系着色，其余默认鎏金） */
@keyframes card-glow {
  0%,
  100% {
    box-shadow: 0 22px 50px -24px rgba(0, 0, 0, 0.9), 0 0 22px -12px rgba(var(--el, 216, 178, 90), 0.38);
  }
  50% {
    box-shadow:
      0 26px 60px -24px rgba(0, 0, 0, 0.95),
      0 0 0 1px rgba(var(--el, 216, 178, 90), 0.26),
      0 0 48px -8px rgba(var(--el, 216, 178, 90), 0.66);
  }
}

/* 扫光掠过 */
@keyframes card-sheen {
  from {
    transform: translateX(-130%) skewX(-16deg);
  }
  to {
    transform: translateX(340%) skewX(-16deg);
  }
}

/* 金属徽标上的往复高光 */
@keyframes metal-sheen {
  0% {
    transform: translateX(-140%) skewX(-18deg);
  }
  45%,
  100% {
    transform: translateX(420%) skewX(-18deg);
  }
}

.card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 20px 22px 18px;
  border-radius: var(--r-lg);
  /* 深灰 + 暗紫的细微渐变，避免纯黑的死板 */
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  border: 1px solid var(--line-soft);
  /* 常驻微弱外发光：机密档案待机的通电感 */
  box-shadow: 0 0 0 1px rgba(216, 178, 90, 0.05), 0 0 22px -14px rgba(216, 178, 90, 0.3);
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.28s, transform 0.28s, box-shadow 0.28s;
}

/* 等高卡片：末块沉底，下边缘对齐 */
.card > :last-child {
  margin-top: auto;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 14%;
  right: 14%;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    var(--gold-lt),
    var(--gold-antique),
    transparent
  );
  opacity: 0;
  transition: opacity 0.3s;
}

/* 悬停扫光 */
.card::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 42%;
  background: linear-gradient(100deg, transparent, rgba(242, 220, 155, 0.1), transparent);
  pointer-events: none;
  transform: translateX(-130%) skewX(-16deg);
}

.card:hover {
  border-color: rgba(216, 178, 90, 0.42);
  transform: translateY(-5px) scale(1.012);
  animation: card-glow 2.4s ease-in-out infinite;
}

.card:hover::after {
  animation: card-sheen 0.9s cubic-bezier(0.5, 0, 0.2, 1) both;
}

.card:hover::before {
  opacity: 0.9;
}

.card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card__type {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--gold-dp);
  padding: 3px 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(216, 178, 90, 0.06);
}

/* 密级徽标：按 data-lv 着色；重点等级（机密 / 绝密）做金属质感 */
.card__rank {
  position: relative;
  display: inline-flex;
  align-items: center;
  overflow: hidden;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-3);
}

.card__rank[data-lv='1'] {
  color: #7ee2b0;
  border-color: rgba(95, 211, 154, 0.4);
  background: rgba(95, 211, 154, 0.1);
}

.card__rank[data-lv='2'] {
  color: var(--ice);
  border-color: rgba(127, 212, 255, 0.4);
  background: rgba(127, 212, 255, 0.1);
}

/* 机密：古铜金属 */
.card__rank[data-lv='3'] {
  color: #f8ecc4;
  font-weight: 600;
  border-color: rgba(138, 106, 40, 0.85);
  background: linear-gradient(
    180deg,
    var(--gold-metal-hi) -40%,
    var(--gold-antique) 30%,
    var(--gold-metal-lo) 62%,
    #9c742c 100%
  );
  box-shadow:
    inset 0 1px 0 rgba(246, 230, 176, 0.35),
    inset 0 -1px 0 rgba(0, 0, 0, 0.5);
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.45);
}

/* 绝密：鎏金金属 + 往复扫光 */
.card__rank[data-lv='4'] {
  color: #241a05;
  font-weight: 600;
  border-color: rgba(122, 90, 30, 0.9);
  background: linear-gradient(
    180deg,
    var(--gold-metal-hi) 0%,
    var(--gold-metal-md) 42%,
    var(--gold-dp) 70%,
    #b98f38 100%
  );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.6),
    inset 0 -1px 0 rgba(0, 0, 0, 0.35),
    0 2px 12px -4px var(--gold-glow);
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.35);
}

.card__rank[data-lv='4']::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 45%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  animation: metal-sheen 3.6s ease-in-out infinite;
}

.card__title {
  font-family: var(--font-serif);
  font-size: 19px;
  letter-spacing: 0.1em;
  color: var(--text);
  margin-bottom: 8px;
}

.card__desc {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-2);
  margin-bottom: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.card__tag {
  font-size: 11px;
  letter-spacing: 0.06em;
  color: var(--text-3);
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(4, 7, 13, 0.55);
  border: 1px solid var(--line-soft);
}

.card__tag--rank {
  color: var(--blood-lt);
  border-color: rgba(226, 86, 79, 0.3);
  background: rgba(179, 39, 45, 0.1);
}

.card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 13px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.card__meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}


/* ================= 结果区 ================= */
.results {
  margin-top: 22px;
  animation: fade-up 0.6s both;
}

.results__head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 4px 16px;
}

.results__title {
  font-family: var(--font-serif);
  font-size: 19px;
  letter-spacing: 0.14em;
  color: var(--text);
}

.results__kw {
  color: var(--gold);
}

.results__count {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--text-4);
}

.results__empty {
  text-align: center;
  padding: 56px 20px;
  border-radius: var(--r-lg);
  border: 1px dashed var(--line-soft);
  color: var(--text-3);
  font-size: 13.5px;
  letter-spacing: 0.06em;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.results__empty-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-4);
}

/* 空态里指向「档案室 · 言灵图鉴」的出路按钮 */
.results__empty-go {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
  padding: 7px 15px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.08);
  color: var(--gold-lt);
  font-size: 12px;
  letter-spacing: 0.08em;
  transition: background 0.22s, border-color 0.22s, transform 0.2s;
}

.results__empty-go:hover {
  background: rgba(216, 178, 90, 0.18);
  border-color: var(--gold);
  transform: translateY(-1px);
}

/* ================= 分页 ================= */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 26px;
}

.pager__btn {
  padding: 8px 20px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(14, 20, 34, 0.6);
  color: var(--text-2);
  font-size: 12.5px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}

.pager__btn:hover:not(:disabled) {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.08);
}

.pager__btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.pager__info {
  font-family: var(--font-mono);
  font-size: 11.5px;
  letter-spacing: 0.14em;
  color: var(--text-4);
}

/* ================= 学院通报 ================= */
.notice {
  margin-top: 30px;
  animation: fade-up 0.6s 0.24s both;
}

.shelf__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px 13px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.26); /* 暗金发丝线 */
  margin-bottom: 16px;
}

.shelf__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  position: relative;
  padding-left: 15px;
  font-family: var(--font-serif);
  font-size: 17px;
  letter-spacing: 0.14em;
  color: var(--text);
}

/* 暗金装饰立柱 */
.shelf__title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 15px;
  border-radius: 2px;
  transform: translateY(-50%);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold) 40%, var(--gold-antique));
  box-shadow: 0 0 8px -2px var(--gold-glow);
}

.shelf__more {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

.notice__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice__item {
  display: flex;
  gap: 14px;
  padding: 14px 18px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(120deg, rgba(32, 27, 45, 0.62), rgba(13, 11, 22, 0.78));
  border-left: 3px solid var(--text-4);
  transition: border-color 0.25s, transform 0.25s;
}

.notice__item:hover {
  transform: translateX(3px);
}

.notice__item[data-level='2'] {
  border-left-color: var(--warn);
}

.notice__item[data-level='3'] {
  border-left-color: var(--blood-lt);
  background: linear-gradient(120deg, rgba(179, 39, 45, 0.14), rgba(13, 11, 22, 0.78));
}

.notice__badge {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--text-3);
  padding-top: 2px;
}

.notice__pin {
  font-style: normal;
  padding: 1px 6px;
  border-radius: 3px;
  background: var(--gold);
  color: #1a1206;
  font-weight: 700;
  font-size: 9px;
}

.notice__body {
  min-width: 0;
}

.notice__title {
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--text);
  margin-bottom: 5px;
}

.notice__text {
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-2);
}

.notice__meta {
  margin-top: 6px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

/* ================= 书架区块 ================= */
.shelf {
  margin-top: 34px;
}

.shelf__empty {
  padding: 28px;
  text-align: center;
  border: 1px dashed var(--line-soft);
  border-radius: var(--r-md);
  font-size: 12.5px;
  color: var(--text-4);
}

/* ================= 快捷入口 ================= */
.quick {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-top: 34px;
  animation: fade-up 0.8s 0.28s both;
}

.quick__item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  border-radius: var(--r-lg);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.8), rgba(13, 11, 22, 0.92));
  border: 1px solid var(--line-soft);
  box-shadow: 0 0 0 1px rgba(216, 178, 90, 0.05), 0 0 22px -16px rgba(216, 178, 90, 0.3);
  transition: border-color 0.28s, transform 0.28s, box-shadow 0.28s;
}

.quick__item:hover {
  border-color: rgba(216, 178, 90, 0.42);
  transform: translateY(-3px);
  box-shadow: 0 22px 50px -24px rgba(0, 0, 0, 0.9), 0 0 30px -18px var(--gold-glow);
}

.quick__icon {
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  display: grid;
  place-items: center;
  border-radius: var(--r-md);
  color: var(--gold-lt);
  border: 1px solid var(--line);
  background: rgba(216, 178, 90, 0.07);
}

.quick__text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.quick__text b {
  font-size: 14.5px;
  letter-spacing: 0.08em;
  color: var(--text);
}

.quick__text i {
  font-style: normal;
  font-size: 11.5px;
  letter-spacing: 0.05em;
  color: var(--text-4);
}

.quick__arrow {
  color: var(--text-4);
  transition: transform 0.25s, color 0.25s;
}

.quick__item:hover .quick__arrow {
  transform: translateX(4px);
  color: var(--gold);
}

/* ================= 响应式 ================= */
@media (max-width: 560px) {
  .home__body {
    padding: 20px 16px 22px;
  }

  .hero {
    padding: 26px 0 20px;
  }

  /* 移动端检索按钮：原来只隐藏了文字、按钮里又没有图标，
     结果被 flex 压成 36px 的空金块。这里保留文字并禁止收缩。 */
  .search__btn {
    flex: 0 0 auto;
    min-width: 0;
    margin: 5px;
    padding: 10px 16px;
    font-size: 13px;
    letter-spacing: 0.1em;
    white-space: nowrap;
  }

  .search {
    padding-right: 2px;
  }

  .search__icon {
    flex: 0 0 40px;
  }

  /* 16px 是 iOS 的分界线：低于它，聚焦输入框会放大整页 */
  .search__input {
    padding: 14px 0;
    font-size: 16px;
  }

  .results__grid {
    grid-template-columns: 1fr;
  }

  .banner {
    flex-wrap: wrap;
  }

  .banner__meta {
    text-align: left;
  }

  .filters {
    padding: 14px 14px;
  }

  .filters__row {
    flex-direction: column;
    gap: 8px;
  }

  .filters__label {
    padding-top: 0;
  }

  .access__hint {
    margin-left: 0;
  }
}

/* ================= 言灵图鉴 / 学院编制 ================= */
.gallery {
  margin-top: 34px;
  animation: fade-up 0.8s 0.24s both;
}

/* —— 言灵卡片的样式已随区块搬到 ArchiveView.vue（.ggrid / .gcard） —— */

/* 悬停扫光 */
.rcard::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 2;
  width: 42%;
  background: linear-gradient(100deg, transparent, rgba(242, 220, 155, 0.12), transparent);
  pointer-events: none;
  transform: translateX(-130%) skewX(-16deg);
}

.rcard:hover::before {
  animation: card-sheen 0.9s cubic-bezier(0.5, 0, 0.2, 1) both;
}

/* 图片加载失败兜底：显示文字占位 */
.rcard__img.is-broken::after {
  content: '图像暂未收录';
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  font-size: 11px;
  letter-spacing: 0.2em;
  color: var(--text-4);
}

.gcard__hint {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 10.5px;
  letter-spacing: 0.1em;
  color: var(--gold-lt);
  background: rgba(4, 7, 13, 0.78);
  border: 1px solid rgba(216, 178, 90, 0.4);
  backdrop-filter: blur(4px);
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.28s, transform 0.28s;
}

/* 卡片右下角的「查看详情」提示：悬停 / 键盘聚焦时浮出。
   言灵卡片已搬到档案室，首页只剩 .rcard（学院机构）在用这个类名。 */
.rcard:hover .gcard__hint,
.rcard:focus-visible .gcard__hint {
  opacity: 1;
  transform: none;
}

/* —— 学院编制卡片 —— */
.roster__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  grid-auto-rows: 1fr; /* 行内等高，网格对齐 */
  gap: 16px;
}

.rcard {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: var(--r-lg);
  overflow: hidden;
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  box-shadow: 0 0 0 1px rgba(216, 178, 90, 0.05), 0 0 22px -14px rgba(216, 178, 90, 0.3);
  cursor: pointer;
  transition: border-color 0.28s, transform 0.28s, box-shadow 0.28s;
}

.rcard:hover {
  border-color: rgba(216, 178, 90, 0.42);
  transform: translateY(-5px) scale(1.012);
  animation: card-glow 2.4s ease-in-out infinite;
}

/* 机构卡片：改用冰蓝描边，与人物卡片区分 */
.rcard--org:hover {
  border-color: rgba(127, 212, 255, 0.45);
  box-shadow: 0 22px 50px -24px rgba(0, 0, 0, 0.9), 0 0 30px -18px rgba(127, 212, 255, 0.5);
}

.rcard--org .rcard__level {
  color: var(--ice);
  border-color: rgba(127, 212, 255, 0.45);
}

/* .rcard__hint 的悬停规则原本与 .gcard__hint 重复，
   机构卡片实际用的是 .gcard__hint（见上方规则），这里只留真正生效的那一份 */

.rcard:focus-visible {
  outline: 2px solid var(--gold);
  outline-offset: 2px;
}

.rcard__img {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background:
    radial-gradient(80% 80% at 50% 30%, rgba(216, 178, 90, 0.12), transparent 70%),
    linear-gradient(160deg, var(--bg-3), var(--bg-1));
}

.rcard__img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.rcard:hover .rcard__img img {
  transform: scale(1.05);
}

.rcard__level {
  position: absolute;
  left: 10px;
  bottom: 10px;
  padding: 3px 10px;
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--gold-lt);
  background: rgba(4, 7, 13, 0.78);
  border: 1px solid rgba(216, 178, 90, 0.4);
  backdrop-filter: blur(4px);
}

.rcard__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 14px 16px 16px;
}

.rcard__name {
  font-family: var(--font-serif);
  font-size: 16.5px;
  letter-spacing: 0.12em;
  color: var(--gold-lt);
}

.rcard__desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-2);
}

.rcard__perks {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto; /* 等高卡片下权限标签沉底对齐 */
  padding-top: 11px;
}

.rcard__perk {
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--text-3);
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(4, 7, 13, 0.55);
  border: 1px solid var(--line-soft);
}

@media (max-width: 560px) {
  .roster__grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 400px) {
  .roster__grid {
    grid-template-columns: 1fr;
  }
}

/* ================= 顶部公告横幅 ================= */
.banner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px 12px 16px;
  margin-bottom: 16px;
  border-radius: var(--r-md);
  border: 1px solid rgba(216, 178, 90, 0.3);
  border-left: 3px solid var(--gold-dp);
  background: linear-gradient(
    100deg,
    rgba(216, 178, 90, 0.13),
    rgba(32, 27, 45, 0.78) 55%,
    rgba(13, 11, 22, 0.9)
  );
  box-shadow:
    0 16px 36px -22px rgba(0, 0, 0, 0.85),
    0 0 26px -16px var(--gold-glow);
  animation: fade-down 0.5s both;
}

/* 紧急公告：暗红点缀 */
.banner[data-level='3'] {
  border-color: rgba(226, 86, 79, 0.38);
  border-left-color: var(--blood-lt);
  background: linear-gradient(
    100deg,
    rgba(179, 39, 45, 0.18),
    rgba(32, 27, 45, 0.78) 55%,
    rgba(13, 11, 22, 0.9)
  );
  box-shadow:
    0 16px 36px -22px rgba(0, 0, 0, 0.85),
    0 0 26px -16px rgba(226, 86, 79, 0.45);
}

.banner__body {
  flex: 1;
  min-width: 0;
}

.banner__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-size: 14.5px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--text);
}

.banner__pin {
  font-style: normal;
  flex: 0 0 auto;
  padding: 1px 7px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  color: #241a05;
  background: linear-gradient(180deg, var(--gold-metal-hi), #a87f2f 70%, #b98f38);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.banner__text {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-2);
}

.banner__meta {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--text-4);
  text-align: right;
}

.banner__close {
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--line-soft);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, transform 0.2s;
}

.banner__close:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  transform: rotate(90deg);
}

.banner__close :deep(svg) {
  transform: rotate(90deg);
}

.banner-enter-active {
  transition: opacity 0.4s, transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.banner-leave-active {
  transition: opacity 0.25s, transform 0.3s ease;
}

.banner-enter-from,
.banner-leave-to {
  opacity: 0;
  transform: translateY(-14px);
}

/* 收折后的预警细线（40px 高） */
.banner__collapsed {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  height: 40px;
  padding: 0 16px;
  margin-bottom: 16px;
  border-radius: var(--r-md);
  border: 1px solid rgba(239, 107, 98, 0.3);
  border-left: 3px solid var(--blood-lt);
  background: linear-gradient(100deg, rgba(179, 39, 45, 0.14), rgba(13, 11, 22, 0.92));
  color: #f0a39c;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: background 0.22s, border-color 0.22s;
}
.banner__collapsed:hover {
  background: linear-gradient(100deg, rgba(179, 39, 45, 0.24), rgba(13, 11, 22, 0.96));
  border-color: rgba(239, 107, 98, 0.55);
}
.banner__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: 0 0 auto;
  background: #ef6b62;
  box-shadow: 0 0 8px 2px rgba(239, 107, 98, 0.6);
  animation: dot-pulse 1.8s ease-in-out infinite;
}
@keyframes dot-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.45;
    transform: scale(0.78);
  }
}

/* ================= 血统鉴定（英雄区） ================= */
.bloodline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
  margin-top: 18px;
  padding: 10px 16px;
  width: fit-content;
  max-width: 100%;
  border-radius: var(--r-md);
  border: 1px solid rgba(138, 106, 40, 0.4);
  background: linear-gradient(120deg, rgba(216, 178, 90, 0.07), rgba(32, 27, 45, 0.55));
}

.bloodline__rank {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 13px;
  border-radius: 999px;
  overflow: hidden;
  font-family: var(--font-serif);
  font-size: 13px;
  font-weight: 700;
  color: #241a05;
  border: 1px solid rgba(122, 90, 30, 0.9);
  background: linear-gradient(
    180deg,
    var(--gold-metal-hi),
    var(--gold-metal-md) 45%,
    var(--gold-dp)
  );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.55),
    0 2px 12px -4px var(--gold-glow);
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.35);
}

.bloodline__rank b {
  font-size: 15px;
}

/* S 级：鎏金偏暗红，贵重感 */
.bloodline__rank[data-rank='S'] {
  background: linear-gradient(180deg, #f8ecc4, var(--gold-metal-md) 40%, #9c5b2a);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.6),
    0 2px 14px -4px rgba(226, 86, 79, 0.5);
}

.bloodline__rank::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 45%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  animation: metal-sheen 3.6s ease-in-out infinite;
}

.bloodline__bar {
  position: relative;
  flex: 1 1 170px;
  max-width: 300px;
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  border: 1px solid rgba(138, 106, 40, 0.55);
  background: rgba(4, 7, 13, 0.6);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.6);
}

.bloodline__bar i {
  position: absolute;
  inset: 1px auto 1px 1px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    #6e5218,
    var(--gold-dp) 35%,
    var(--gold-metal-md) 70%,
    var(--gold-metal-hi)
  );
  box-shadow:
    0 0 14px -3px var(--gold-glow),
    inset 0 1px 0 rgba(255, 255, 255, 0.35);
  animation: blood-fill 1.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes blood-fill {
  from {
    width: 0;
  }
}

.bloodline__pct {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--text-3);
}

.bloodline__pct b {
  color: var(--gold-lt);
  font-size: 13px;
  text-shadow: 0 0 12px var(--gold-glow);
}

.bloodline__note {
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--text-4);
}

.bloodline__link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
  padding: 3px 11px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.34);
  background: rgba(216, 178, 90, 0.08);
  text-decoration: none;
  transition: background 0.24s, border-color 0.24s, color 0.24s;
}

.bloodline__link:hover {
  background: rgba(216, 178, 90, 0.2);
  border-color: var(--gold);
  color: #fff;
}

.bloodline__link svg {
  transform: rotate(-90deg);
}


/* ================= 渐进显现（滚动进场） =================
   基础规则在 styles/theme.css 的 .reveal（带 --d 错峰变量）；
   这里只给同一屏内的区块排出先后次序，视觉上像档案被逐份放到桌上。 */
.reveal:nth-of-type(2) {
  --d: 70ms;
}
.reveal:nth-of-type(3) {
  --d: 140ms;
}
.reveal:nth-of-type(4) {
  --d: 210ms;
}
.reveal:nth-of-type(5) {
  --d: 280ms;
}
.reveal:nth-of-type(n + 6) {
  --d: 340ms;
}

/* ================= 尊重系统的「减少动态效果」设置 ================= */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
