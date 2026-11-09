<script setup>
/**
 * 档案室（独立页面 /archive）
 * - 不在首页展示，入口在顶部导航栏「档案馆」
 * - 任务系统已下线，档案全量开放：密级只作卷宗分级标注，不再拦截查阅
 * - 五卷：龙王卷宗（资料+情报）/ 部门人物 / 龙族武器 / 任务执行记录 / 言灵图鉴
 * - 言灵图鉴原本在首页，现已整体并入档案室（功能与详情页跳转不变）
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import { LEVELS } from '@/utils/clearance'
import {
  ARCHIVE_ARMS,
  ARCHIVE_DEPTS,
  ARCHIVE_KINGS,
  ARCHIVE_MISSION_LOGS,
  ARCHIVE_PEOPLE
} from '@/data/archive'
import { codexPath } from '@/data/codex'
import { logPath } from '@/data/logDetail'
import { ensureSpirits, useSpirits } from '@/data/spiritStore'
import { isIn, markIn, revealInView } from '@/utils/reveal'

const router = useRouter()
const route = useRoute()

/** 跨页跳转：顶部导航由 SiteNav 统一渲染，这里给正文里的跳转按钮复用 */
function go(to) {
  if (to === route.path) {
    showToast('已在当前页面')
    return
  }
  router.push(to)
}

/** 轻提示：停留在当前页时给出明确反馈（避免"点了没反应"） */
const toast = ref('')
let toastTimer = 0
function showToast(text) {
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => (toast.value = ''), 4000)
}
onBeforeUnmount(() => clearTimeout(toastTimer))

/**
 * 卡片即入口：整张卡片点进各自的卷宗详情页（/archive/:kind/:key）。
 * 任务系统已下线，所有卡片都可直接查阅，不存在封存态。
 */
function openKing(k) {
  go(`/archive/king/${k.code}`)
}
function openPerson(p) {
  go(`/archive/person/${p.key}`)
}
function openArm(a) {
  go(`/archive/arm/${a.code}`)
}

/* ---------------- 分卷切换 ---------------- */
const TABS = [
  { id: 'kings', label: '龙王卷宗', icon: 'flame' },
  { id: 'people', label: '部门人物', icon: 'user' },
  { id: 'arms', label: '龙族武器', icon: 'sparkle' },
  { id: 'logs', label: '任务执行记录', icon: 'archive' },
  { id: 'spirits', label: '言灵图鉴', icon: 'flame' }
]
const tab = ref('kings')

/**
 * 支持 hash 直达某个卷宗：#tab-kings / #tab-people / #tab-arms / #tab-logs
 * 任务记录详情页的「返回」会带 #tab-logs 回来，方便用户接着看列表。
 * 同时兼容查询串写法（/archive?tab=people）：早期分享出去的链接长这样，
 * 不认 query 的话点进去会停在「龙王卷宗」，看起来像跳错了页。
 */
function tabFromHash() {
  const q = String(route.query?.tab || '')
  if (TABS.some((t) => t.id === q)) {
    tab.value = q
    return
  }
  const h = String(route.hash || '').replace(/^#/, '')
  const m = h.match(/^tab-(\w+)$/)
  if (m && TABS.some((t) => t.id === m[1])) tab.value = m[1]
}
tabFromHash()
watch([() => route.hash, () => route.query?.tab], tabFromHash)

/** 切换分卷时同步 hash（用 replace 避免污染浏览器历史） */
function selectTab(id) {
  if (tab.value === id) return
  tab.value = id
  router.replace({ path: route.path, hash: `#tab-${id}` })
}

/**
 * 分卷栏的键盘操作（WAI-ARIA tabs · roving tabindex）：
 * - Tab 键整体进出分卷栏（当前卷 tabindex=0，其余 -1）
 * - ←/→ 循环切换，Home/End 跳到首/末卷，切换后焦点跟到新卷上
 * 选中的分卷由 aria-selected 暴露，读屏不会再把它当成一排普通按钮。
 */
function onTabsKeydown(e) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(e.key)) return
  e.preventDefault()
  const idx = Math.max(0, TABS.findIndex((t) => t.id === tab.value))
  const next =
    e.key === 'Home'
      ? 0
      : e.key === 'End'
        ? TABS.length - 1
        : (idx + (e.key === 'ArrowRight' ? 1 : -1) + TABS.length) % TABS.length
  const id = TABS[next].id
  selectTab(id)
  nextTick(() => document.getElementById(`tab-${id}`)?.focus())
}

/* ---------------- 部门人物 ---------------- */
const deptFilter = ref('')
const filteredPeople = computed(() =>
  deptFilter.value ? ARCHIVE_PEOPLE.filter((p) => p.dept === deptFilter.value) : ARCHIVE_PEOPLE
)
function countDept(id) {
  return ARCHIVE_PEOPLE.filter((p) => p.dept === id).length
}

/* ---------------- 任务执行记录 ---------------- */

/**
 * 任务记录同样是「整张卡片 = 入口」：一律进详情页。
 *
 * 注意签名只收一条记录：模板里是 @click="openLog(m)"，
 * 早先多写了一个 $event 形参（openLog(e, m)），m 恒为 undefined →
 * logPath(undefined) 抛错被 Vue 吞进 console.error，表现就是「点卡片没反应」。
 */
function openLog(m) {
  if (!m?.code) return
  go(logPath(m))
}

/* ---------------- 言灵图鉴（原首页区块整体搬入） ----------------
   数据源、交互、跳转目标与搬迁前完全一致：
   - 列表来自 data/spiritStore（后台维护，静态兜底，进页面即拉取最新）
   - 点击卡片 → /spirit/:key 言灵档案（codexPath('spirit', key)，与详情页路由一致）
   - 图片加载失败显示「图像暂未收录」占位 */
const spirits = useSpirits()

function openSpirit(s) {
  router.push(codexPath('spirit', s.key))
}

function onImgError(e) {
  const img = e.target
  img.style.display = 'none'
  const wrap = img.closest('.gcard__img')
  if (wrap) wrap.classList.add('is-broken')
}

/* ---------------- 卷宗入架动效 ----------------
   档案馆的隐喻是「一卷一卷从架上抽出」。切卷时让卡片依次入场，
   而不是整块同时淡入。观察器只盯当前这一卷，切入时重建，
   避免跨卷残留的旧观察目标。

   入场态一律走 utils/reveal 的 markIn（= data-rise 属性），不要用
   classList.add('is-in')：卡片模板上还有动态 class / 属性绑定，Vue 重渲染
   会把整串 class 重写，手上的 is-in 被冲掉，.rise 的 opacity: 0 立刻生效，
   表现就是「部门人物只剩一两张卡，其余全空」。 */
const shelfEl = ref(null)
let shelfIO = null

function bindShelfObserver() {
  shelfIO?.disconnect()
  const root = shelfEl.value
  if (!root) return

  // 不支持 IntersectionObserver 的环境：直接全部入场，别让整页停在 opacity: 0
  if (typeof IntersectionObserver === 'undefined') {
    for (const el of root.querySelectorAll('.rise')) markIn(el)
    return
  }

  shelfIO = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        markIn(entry.target)
        shelfIO.unobserve(entry.target)
      }
    },
    { rootMargin: '0px 0px -8% 0px', threshold: 0.06 }
  )

  for (const el of root.querySelectorAll('.rise')) {
    // 已经入场的跳过，避免重挂观察器时重复播放入场动画
    if (isIn(el)) continue
    shelfIO.observe(el)
  }
}

/** 视口内的卡片立即入场（筛选 / 切卷重建出来的新节点没有进过观察器） */
function revealShelf() {
  revealInView(shelfEl.value)
}

/** 列表结构变化后：等 DOM 落地 → 重挂观察器 → 补首屏入场 */
function refreshShelf() {
  nextTick(() => {
    bindShelfObserver()
    requestAnimationFrame(revealShelf)
  })
}

// 切卷时 DOM 换了一整批卡片
watch(tab, refreshShelf)
// 按部门筛选会重建卡片（不补入场就是「点一次筛选，卡片全消失」）
watch(filteredPeople, refreshShelf)
// 言灵图鉴：后台数据拉回来后整表替换，新卡片同样要补入场
watch(spirits, refreshShelf)

onMounted(() => {
  // 言灵图鉴：拉取后台管理系统维护的最新数据（失败时保持静态兜底）
  ensureSpirits()
  nextTick(() => {
    bindShelfObserver()
    revealShelf()
  })
})
onBeforeUnmount(() => shelfIO?.disconnect())
</script>

<template>
  <div class="arc">
    <RuneBackground />

    <!-- ============ 顶部导航：与首页同源 ============ -->
    <SiteNav />

    <!-- ============ 主体 ============ -->
    <main ref="shelfEl" class="arc__body">
      <section class="arc__hero">
        <div class="arc__hero-row">
          <DragonEmblem class="arc__hero-mark" />
          <div class="arc__hero-text">
            <h1 class="arc__title">
              <span class="sheen-gold">档案室</span>
              <span class="arc__title-en">ARCHIVE · DRAGON DOSSIER</span>
            </h1>
            <p class="arc__sub">
              龙王资料与情报 · 各部门人物性格 / 武器 / 言灵 · 任务执行过程与结果 · 荣誉与身世 · 言灵图鉴
            </p>
          </div>
        </div>
      </section>

      <!-- 任务系统已下线：本馆卷宗全部开放查阅，密级仅作分级标注 -->
      <!-- ============ 分卷（WAI-ARIA tabs：Tab 键进出，左右键切换，读屏能听到「第几卷 / 共几卷」） -->
      <div
        class="tabs"
        role="tablist"
        aria-label="档案室分卷"
        @keydown="onTabsKeydown"
      >
        <button
          v-for="(t, i) in TABS"
          :id="`tab-${t.id}`"
          :key="t.id"
          type="button"
          role="tab"
          class="tab"
          :class="{ 'is-on': tab === t.id }"
          :aria-selected="tab === t.id"
          :aria-controls="`panel-${t.id}`"
          :aria-posinset="i + 1"
          :aria-setsize="TABS.length"
          :tabindex="tab === t.id ? 0 : -1"
          @click="selectTab(t.id)"
        >
          <AppIcon :name="t.icon" :size="14" /> {{ t.label }}
        </button>
      </div>

      <!-- ===== 卷一 · 龙王卷宗 ===== -->
      <section
        v-if="tab === 'kings'"
        id="panel-kings"
        class="shelf rise"
        role="tabpanel"
        aria-labelledby="tab-kings"
        tabindex="-1"
      >
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="flame" :size="15" /> 龙王卷宗 · 资料与情报</h2>
          <span class="shelf__more">
            共 {{ ARCHIVE_KINGS.length }} 位 · 点击卡片查看完整卷宗
          </span>
        </header>

        <div class="kgrid">
          <article
            v-for="(k, i) in ARCHIVE_KINGS"
            :key="k.code"
            class="kcard rise kcard--entry"
            :style="{ '--d': `${(i % 6) * 55}ms` }"
            role="button"
            tabindex="0"
            :aria-label="`查看龙王卷宗：${k.code} ${k.name}`"
            @click="openKing(k)"
            @keydown.enter.prevent="openKing(k)"
            @keydown.space.prevent="openKing(k)"
          >
            <div class="kcard__top">
              <div class="kcard__id">
                <span class="kcard__code">{{ k.code }}</span>
                <span class="kcard__threat" :data-t="k.threat">{{ k.threat }}</span>
              </div>
              <div class="kcard__name">
                <h3>{{ k.name }}</h3>
                <p>{{ k.alias }} · {{ k.seat }}</p>
              </div>
              <span class="kcard__lv" :data-lv="k.req">{{ LEVELS[k.req - 1]?.label }}</span>
              <span class="kcard__go"><AppIcon name="arrow" :size="14" /></span>
            </div>

            <!-- 基础资料：公开可见 -->
            <div class="kcard__body">
              <p class="kcard__desc">{{ k.desc }}</p>
              <div class="kcard__meta">
                <span><i>王座</i>{{ k.seats }}</span>
                <span><i>状态</i>{{ k.status }}</span>
                <span><i>调阅</i>{{ k.notes }}</span>
              </div>
              <div class="kcard__tags">
                <span v-for="t in k.tags" :key="t">{{ t }}</span>
              </div>

              <!-- 情报条目标题不剧透，只交代条数 -->
              <p class="kcard__foot">
                <AppIcon name="info" :size="12" />
                情报条目 {{ k.intel.length }} 条 · 点击进入详情页
              </p>
            </div>
          </article>
        </div>
      </section>

      <!-- ===== 卷二 · 部门人物 ===== -->
      <section
        v-else-if="tab === 'people'"
        id="panel-people"
        class="shelf rise"
        role="tabpanel"
        aria-labelledby="tab-people"
        tabindex="-1"
      >
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="user" :size="15" /> 部门人物档案</h2>
          <span class="shelf__more">
            共 {{ ARCHIVE_PEOPLE.length }} 人 · 点击卡片展开完整档案
          </span>
        </header>

        <div class="chips pchips">
          <button
            type="button"
            class="chip chip--sm"
            :class="{ 'is-on': !deptFilter }"
            @click="deptFilter = ''"
          >
            全部 <i class="chip__n">{{ ARCHIVE_PEOPLE.length }}</i>
          </button>
          <button
            v-for="d in ARCHIVE_DEPTS"
            :key="d.id"
            type="button"
            class="chip chip--sm"
            :class="{ 'is-on': deptFilter === d.id }"
            @click="deptFilter = d.id"
          >
            <AppIcon :name="d.icon" :size="12" /> {{ d.label }}
            <i class="chip__n">{{ countDept(d.id) }}</i>
          </button>
        </div>

        <div class="pgrid">
          <article
            v-for="(p, i) in filteredPeople"
            :key="p.key"
            class="person rise person--entry"
            :data-tone="p.tone"
            :style="{ '--d': `${(i % 8) * 50}ms` }"
            role="button"
            tabindex="0"
            :aria-label="`查看人物卷宗：${p.name}`"
            @click="openPerson(p)"
            @keydown.enter.prevent="openPerson(p)"
            @keydown.space.prevent="openPerson(p)"
          >
            <div class="person__top">
              <div class="person__avatar"><span>{{ p.name.slice(0, 1) }}</span></div>
              <div class="person__id">
                <h3>{{ p.name }}</h3>
                <p>{{ p.alias }}</p>
                <span class="person__dept">{{ p.deptLabel }}</span>
              </div>
              <div class="person__right">
                <span class="person__blood" :data-rank="p.blood">{{ p.blood }} 级 · {{ p.bloodPct }}%</span>
                <span class="person__lv" :data-lv="p.req">{{ LEVELS[p.req - 1]?.label }}</span>
                <span class="kcard__go"><AppIcon name="arrow" :size="14" /></span>
              </div>
            </div>

            <!-- 基础档案：公开可见 -->
            <div class="person__body">
              <div class="person__brief">
                <p>{{ p.desc }}</p>
                <p class="person__quote">{{ p.quote }}</p>
                <p class="person__note">{{ p.bloodNote }}</p>
              </div>

              <!-- 完整档案（性格/武器/言灵/任务/荣誉/身世）在详情页 -->
              <p class="kcard__foot">
                <AppIcon name="id" :size="12" />
                完整档案 · 性格 / 武器 / 言灵 / 任务 / 荣誉 / 身世 · 点击进入详情页
              </p>
            </div>
          </article>
        </div>
      </section>

      <!-- ===== 卷三 · 龙族武器 ===== -->
      <section
        v-else-if="tab === 'arms'"
        id="panel-arms"
        class="shelf rise"
        role="tabpanel"
        aria-labelledby="tab-arms"
        tabindex="-1"
      >
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="sparkle" :size="15" /> 龙族武器情报</h2>
          <span class="shelf__more">
            共 {{ ARCHIVE_ARMS.length }} 件 · 点击卡片查看器物档案
          </span>
        </header>

        <div class="agrid">
          <article
            v-for="(a, i) in ARCHIVE_ARMS"
            :key="a.code"
            class="acard rise acard--entry"
            :style="{ '--d': `${(i % 8) * 50}ms` }"
            role="button"
            tabindex="0"
            :aria-label="`查看武器档案：${a.code} ${a.name}`"
            @click="openArm(a)"
            @keydown.enter.prevent="openArm(a)"
            @keydown.space.prevent="openArm(a)"
          >
            <div class="acard__top">
              <span class="acard__code">{{ a.code }}</span>
              <span class="acard__rank">{{ a.rank }}</span>
            </div>
            <h3>{{ a.name }}</h3>
            <p class="acard__type">{{ a.type }} · {{ a.origin }}</p>

            <!-- 器物详情 -->
            <p class="acard__desc">{{ a.desc }}</p>
            <p class="acard__perk">{{ a.perk }}</p>
            <div class="acard__tags">
              <span v-for="t in a.tags" :key="t">{{ t }}</span>
            </div>
            <p class="acard__status">状态：{{ a.status }}</p>
            <p class="kcard__foot">
              <AppIcon name="info" :size="12" /> 点击查看器物档案
            </p>
          </article>
        </div>
      </section>

      <!-- ===== 卷四 · 任务执行记录 ===== -->
      <section
        v-else-if="tab === 'logs'"
        id="panel-logs"
        class="shelf rise"
        role="tabpanel"
        aria-labelledby="tab-logs"
        tabindex="-1"
      >
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="archive" :size="15" /> 任务执行记录</h2>
          <span class="shelf__more">
            共 {{ ARCHIVE_MISSION_LOGS.length }} 条 · 点击卡片查看详情
          </span>
        </header>

        <div class="lgrid">
          <article
            v-for="(m, i) in ARCHIVE_MISSION_LOGS"
            :key="m.code + i"
            class="log rise log--open"
            :style="{ '--d': `${(i % 8) * 50}ms` }"
            role="button"
            tabindex="0"
            :aria-label="`查看任务详情：${m.code} ${m.name}`"
            @click="openLog(m)"
            @keydown.enter.prevent="openLog(m)"
            @keydown.space.prevent="openLog(m)"
          >
            <div class="log__head">
              <span class="log__code">{{ m.code }}</span>
              <b>{{ m.name }}</b>
              <span class="log__who">{{ m.person }} · {{ m.deptLabel }}</span>
            </div>

            <p class="log__p"><i>执行过程</i>{{ m.process }}</p>
            <p class="log__r"><i>完成结果</i>{{ m.result }}</p>
            <span class="log__more">查看详情 <AppIcon name="arrow" :size="12" /></span>
          </article>
        </div>
      </section>

      <!-- ===== 卷五 · 言灵图鉴（原首页区块，内容与交互不变） ===== -->
      <section
        v-else-if="tab === 'spirits'"
        id="panel-spirits"
        class="shelf rise"
        role="tabpanel"
        aria-labelledby="tab-spirits"
        tabindex="-1"
      >
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="flame" :size="15" /> 言灵图鉴</h2>
          <span class="shelf__more">
            共 {{ spirits.length }} 条 · 点击卡片查看言灵档案
          </span>
        </header>

        <div class="ggrid">
          <article
            v-for="s in spirits"
            :key="s.key"
            class="gcard rise"
            :data-element="s.element"
            role="button"
            tabindex="0"
            :aria-label="`查阅言灵：${s.name}`"
            @click="openSpirit(s)"
            @keydown.enter.prevent="openSpirit(s)"
            @keydown.space.prevent="openSpirit(s)"
          >
            <div class="gcard__img">
              <img
                :src="s.imageUrl || `/img/spirit/${s.key}.webp`"
                :alt="`言灵 · ${s.name}`"
                loading="lazy"
                @error="onImgError"
              />
              <span class="gcard__element">{{ s.element }}</span>
              <span class="gcard__hint">查看详情</span>
            </div>
            <div class="gcard__body">
              <div class="gcard__row">
                <h3 class="gcard__name">{{ s.name }}</h3>
                <span class="gcard__level" :data-level="s.level">{{ s.level }}</span>
              </div>
              <p class="gcard__effect">{{ s.effect }}</p>
            </div>
          </article>
        </div>

        <p v-if="!spirits.length" class="shelf__empty">言灵谱暂未收录任何条目</p>
      </section>
    </main>

    <!-- 轻提示：点击封存内容时给出反馈 -->
    <transition name="toast">
      <div v-if="toast" class="toast"><AppIcon name="lock" :size="14" /> {{ toast }}</div>
    </transition>

    <!-- ============ 页脚 ============ -->
    <footer class="arc__foot">
      <span>以龙档案馆 · 同人兴趣站 · 非商用</span>
      <span class="arc__foot-meta">版权归原作者所有 · 仅作学习交流</span>
    </footer>
  </div>
</template>

<style scoped>
.arc {
  position: relative;
  z-index: 1;
  min-height: 100%;
}

/* ================= 主体 ================= */
.arc__body {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 30px 24px 64px;
}

.arc__hero {
  margin-bottom: 22px;
}

.arc__hero-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.arc__hero-mark {
  width: 52px;
  height: 52px;
  filter: drop-shadow(0 0 14px rgba(216, 178, 90, 0.45));
  animation: spin 60s linear infinite;
}

.arc__title {
  font-family: var(--font-serif);
  font-size: 26px;
  letter-spacing: 0.12em;
  color: var(--text);
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
}

.arc__title-en {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.28em;
  color: var(--gold-dp);
  text-transform: uppercase;
}

.arc__sub {
  margin-top: 6px;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--text-3);
}

/* ================= 分卷 ================= */
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(14, 20, 34, 0.5);
  color: var(--text-2);
  font-size: 12.5px;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: color 0.22s, border-color 0.22s, background 0.22s;
}

.tab:hover {
  color: var(--text);
  border-color: rgba(216, 178, 90, 0.45);
}

.tab.is-on {
  color: #1a1206;
  font-weight: 600;
  border-color: var(--gold);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
}

/* 键盘焦点：分卷栏是 roving tabindex，焦点环必须看得见（鼠标点击不显示） */
.tab:focus-visible {
  outline: 2px solid var(--gold-lt);
  outline-offset: 2px;
}

/* ================= 区块标题 ================= */
.shelf__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  /* 窄屏时右侧说明整行下移到标题下方，而不是把标题挤成两行 */
  flex-wrap: wrap;
  gap: 6px 12px;
  padding: 0 4px 13px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.26);
  margin-bottom: 16px;
}

.shelf__title {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  white-space: nowrap;
  gap: 8px;
  position: relative;
  padding-left: 15px;
  font-family: var(--font-serif);
  font-size: 17px;
  letter-spacing: 0.14em;
  color: var(--text);
}

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
  flex: 1 1 auto;
  min-width: 0;
  text-align: right;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

/* ================= 通用 chips ================= */
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pchips {
  margin-bottom: 14px;
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
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}

.chip:hover {
  color: var(--text);
  border-color: rgba(216, 178, 90, 0.45);
}

.chip.is-on {
  color: #1a1206;
  font-weight: 600;
  border-color: var(--gold);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
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

/* ================= 言灵图鉴（自首页搬入，样式随卡片一起搬过来） ================= */
.ggrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  grid-auto-rows: 1fr; /* 行内等高，网格对齐 */
  gap: 16px;
}

.gcard {
  /* --el：元素系辉光色（默认鎏金），由 :data-element 覆盖 */
  --el: 216, 178, 90;
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: var(--r-lg);
  overflow: hidden;
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  box-shadow: 0 0 0 1px rgba(216, 178, 90, 0.05), 0 0 22px -14px rgba(216, 178, 90, 0.3);
  cursor: pointer;
  transition: border-color 0.28s, transform 0.42s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.28s;
}

/* 言灵卡按元素系着色：辉光、描边、徽标同色 */
.gcard[data-element='火'] {
  --el: 255, 138, 76;
}
.gcard[data-element='冰'] {
  --el: 127, 212, 255;
}
.gcard[data-element='风'] {
  --el: 138, 232, 199;
}
.gcard[data-element='时间'] {
  --el: 196, 181, 253;
}
.gcard[data-element='重力'] {
  --el: 206, 162, 112;
}
.gcard[data-element='威压'] {
  --el: 240, 122, 90;
}
.gcard[data-element='精神'] {
  --el: 176, 196, 255;
}
.gcard[data-element='暗影'] {
  --el: 158, 132, 208;
}
.gcard[data-element='雷'] {
  --el: 150, 175, 255;
}

/* 底部元素辉光层（hover 点亮） */
.gcard::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background: radial-gradient(118% 56% at 50% 116%, rgba(var(--el), 0.26), transparent 64%);
  opacity: 0;
  transition: opacity 0.38s;
}

/* 悬停扫光 */
.gcard::before {
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

.gcard:hover {
  border-color: rgba(216, 178, 90, 0.5);
  transform: translateY(-5px) scale(1.012);
  animation: card-glow 2.4s ease-in-out infinite;
}

.gcard:hover::before {
  animation: card-sheen 0.9s cubic-bezier(0.5, 0, 0.2, 1) both;
}

.gcard__img {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background:
    radial-gradient(80% 80% at 50% 30%, rgba(216, 178, 90, 0.12), transparent 70%),
    linear-gradient(160deg, var(--bg-3), var(--bg-1));
}

.gcard__img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1), filter 0.5s;
}

.gcard:hover .gcard__img img {
  transform: scale(1.06);
  filter: saturate(1.12) brightness(1.06);
}

/* 图片加载失败兜底：显示文字占位 */
.gcard__img.is-broken::after {
  content: '图像暂未收录';
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  font-size: 11px;
  letter-spacing: 0.2em;
  color: var(--text-4);
}

.gcard__element {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 12px;
  color: var(--gold-lt);
  background: rgba(4, 7, 13, 0.72);
  border: 1px solid rgba(216, 178, 90, 0.5);
  backdrop-filter: blur(4px);
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

/* 「查看详情」提示：悬停 / 键盘聚焦时浮出 */
.gcard:hover .gcard__hint,
.gcard:focus-visible .gcard__hint {
  opacity: 1;
  transform: none;
}

.gcard__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 13px 15px 14px;
}

.gcard__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.gcard__name {
  font-family: var(--font-serif);
  font-size: 16px;
  letter-spacing: 0.1em;
  color: var(--text);
}

/* 言灵危级徽标：重点等级做金属质感 */
.gcard__level {
  position: relative;
  display: inline-flex;
  align-items: center;
  overflow: hidden;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid;
}

.gcard__level[data-level='高危'] {
  color: var(--warn);
  border-color: rgba(232, 180, 90, 0.45);
  background: rgba(232, 180, 90, 0.1);
}

/* 机密：古铜金属 */
.gcard__level[data-level='机密'] {
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
.gcard__level[data-level='绝密'] {
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

.gcard__level[data-level='绝密']::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 45%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  animation: metal-sheen 3.6s ease-in-out infinite;
}

/* 「内部」档徽标 */
.gcard__level[data-level='内部'] {
  color: var(--text-3);
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.04);
}

.gcard__effect {
  margin-top: 7px;
  font-size: 11.5px;
  letter-spacing: 0.05em;
  color: var(--text-4);
}

.shelf__empty {
  padding: 28px;
  text-align: center;
  border: 1px dashed var(--line-soft);
  border-radius: var(--r-md);
  font-size: 12.5px;
  color: var(--text-4);
}

/* scoped 样式会把 @keyframes 名字一起加后缀，所以关键帧必须搬进本文件，
   否则 .gcard 上的 card-glow / card-sheen / metal-sheen 全部失效（无动画但不报错） */
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

/* 窄屏：与搬迁前一致，两列 → 单列 */
@media (max-width: 560px) {
  .ggrid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 400px) {
  .ggrid {
    grid-template-columns: 1fr;
  }
}

/* ================= 龙王卷宗 ================= */
.kgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 16px;
  align-items: start;
}

.kcard {
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  overflow: hidden;
  transition: border-color 0.26s, box-shadow 0.26s;
}

.kcard:hover {
  border-color: rgba(216, 178, 90, 0.45);
  box-shadow: 0 0 26px -14px rgba(216, 178, 90, 0.6);
}

.kcard__top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  cursor: pointer;
}

.kcard__id {
  display: flex;
  flex-direction: column;
  gap: 5px;
  align-items: flex-start;
}

.kcard__code {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--gold-dp);
  padding: 3px 9px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(216, 178, 90, 0.06);
}

.kcard__threat {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--blood-lt);
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(226, 86, 79, 0.45);
  background: rgba(179, 39, 45, 0.12);
}

.kcard__name {
  flex: 1;
  min-width: 0;
}

.kcard__name h3 {
  font-family: var(--font-serif);
  font-size: 16px;
  letter-spacing: 0.08em;
  color: var(--text);
}

.kcard__name p {
  margin-top: 3px;
  font-size: 11.5px;
  letter-spacing: 0.05em;
  color: var(--text-4);
}

.kcard__lv {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-3);
}

.kcard__lv[data-lv='3'] {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.08);
}

.kcard__lv[data-lv='4'] {
  color: #f0a39c;
  border-color: rgba(239, 107, 98, 0.5);
  background: rgba(179, 39, 45, 0.14);
}

/* 卡片右下角的进入箭头（卡片整张可点，指针悬停时亮起） */
.kcard__go {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid rgba(216, 178, 90, 0.34);
  color: var(--gold-lt);
  opacity: 0.65;
  transition: opacity 0.24s, background 0.24s, transform 0.24s;
}

.kcard:hover .kcard__go,
.kcard:focus-visible .kcard__go,
.person:hover .kcard__go,
.person:focus-visible .kcard__go {
  opacity: 1;
  background: rgba(216, 178, 90, 0.16);
  transform: translateX(2px);
}

.kcard__body {
  padding: 0 18px 18px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
}

.kcard__desc {
  margin-top: 12px;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--text-3);
}

.kcard__meta {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 11.5px;
  color: var(--text-3);
}

.kcard__meta i {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  margin-right: 8px;
}

.kcard__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.kcard__tags span {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  font-size: 11px;
  color: var(--text-3);
  background: rgba(216, 178, 90, 0.05);
}

/* ================= 卡片底部：进入详情页的提示 ================= */
.kcard__foot {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 13px;
  padding-top: 10px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: var(--gold-lt);
}

/* 入口卡：整张可点，悬停给出「能进去」的信号 */
.kcard--entry,
.person--entry,
.acard--entry {
  cursor: pointer;
}

.kcard--entry:focus-visible,
.person--entry:focus-visible,
.acard--entry:focus-visible {
  outline: none;
  border-color: var(--gold-dp);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.18);
}

.kcard--entry:hover {
  transform: translateY(-2px);
  box-shadow: 0 22px 44px -26px rgba(0, 0, 0, 0.95), 0 0 24px -16px var(--gold-glow);
}

.person__note {
  margin-top: 6px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--text-4);
}

/* ================= 轻提示 ================= */
.toast {
  position: fixed;
  left: 50%;
  bottom: 34px;
  z-index: 60;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 22px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.96), rgba(14, 11, 23, 0.98));
  box-shadow: 0 16px 40px -20px rgba(0, 0, 0, 0.9), 0 0 24px -12px var(--gold-glow);
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--gold-lt);
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.24s, transform 0.24s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}

/* ================= 部门人物 ================= */
.pgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
  align-items: start;
}

.person {
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  overflow: hidden;
  transition: border-color 0.26s, box-shadow 0.26s, transform 0.26s;
}

.person:hover {
  border-color: rgba(216, 178, 90, 0.4);
  transform: translateY(-2px);
}

.person__top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  cursor: pointer;
}

.person__avatar {
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 17px;
  color: #1a1206;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5), 0 0 16px -6px var(--gold-glow);
}

.person[data-tone='ice'] .person__avatar {
  background: linear-gradient(180deg, #a5e4ff, #4aa8d4);
}

.person[data-tone='fire'] .person__avatar {
  background: linear-gradient(180deg, #f6c07a, #cf6a36);
}

.person[data-tone='rose'] .person__avatar {
  background: linear-gradient(180deg, #f7b6c2, #c05a72);
}

.person__id {
  flex: 1;
  min-width: 0;
}

.person__id h3 {
  font-family: var(--font-serif);
  font-size: 16px;
  letter-spacing: 0.08em;
  color: var(--text);
}

.person__id p {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--text-4);
}

.person__dept {
  display: inline-block;
  margin-top: 5px;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: var(--text-3);
}

.person__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.person__blood {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--gold-lt);
}

.person__lv {
  font-family: var(--font-mono);
  font-size: 10px;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-3);
}

.person__lv[data-lv='3'] {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.08);
}

.person__lv[data-lv='4'] {
  color: #f0a39c;
  border-color: rgba(239, 107, 98, 0.5);
  background: rgba(179, 39, 45, 0.14);
}

.person__body {
  padding: 0 18px 18px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
}

.person__brief {
  margin-top: 12px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-3);
}

.person__quote {
  margin-top: 8px;
  font-family: var(--font-serif);
  font-size: 12.5px;
  color: var(--gold-dp);
}

/* ================= 龙族武器 ================= */
.agrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  grid-auto-rows: 1fr;
  gap: 16px;
}

.acard {
  display: flex;
  flex-direction: column;
  padding: 16px 18px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  transition: border-color 0.26s, transform 0.26s;
}

.acard:hover {
  border-color: rgba(216, 178, 90, 0.45);
  transform: translateY(-3px);
}

.acard__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.acard__code {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--gold-dp);
}

.acard__rank {
  font-family: var(--font-serif);
  font-size: 12px;
  color: var(--gold-lt);
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.08);
}

.acard h3 {
  font-family: var(--font-serif);
  font-size: 15.5px;
  letter-spacing: 0.08em;
  color: var(--text);
}

.acard__type {
  margin-top: 4px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.acard__desc {
  margin-top: 10px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-3);
}

.acard__perk {
  margin-top: 10px;
  font-size: 12px;
  color: var(--gold-dp);
}

.acard__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.acard__tags span {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  font-size: 11px;
  color: var(--text-3);
}

.acard__status {
  margin-top: auto;
  padding-top: 12px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

/* ================= 任务执行记录 ================= */
.lgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 16px;
}

.log {
  padding: 14px 16px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  /* 整张卡片都是入口 */
  cursor: pointer;
  transition: border-color 0.24s ease, background 0.24s ease, box-shadow 0.24s ease;
}

/* 已解锁：悬停给出「可以进去」的信号。
   这里刻意不用 transform 抬升 —— 卡片带 .rise 入场动画，
   animation 的前景填充会压住 :hover 的 transform，抬升会失效。 */
.log--open:hover {
  border-color: rgba(216, 178, 90, 0.42);
  background: linear-gradient(165deg, rgba(41, 35, 58, 0.9), rgba(16, 13, 26, 0.96));
  box-shadow: 0 20px 44px -26px rgba(0, 0, 0, 0.95), 0 0 22px -16px var(--gold-glow);
}

.log:focus-visible {
  outline: none;
  border-color: var(--gold-dp);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.18);
}

.log__head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.log__code {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.18em;
  color: var(--gold-dp);
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
}

.log__head b {
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 0.06em;
  color: var(--text);
}

.log__who {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-4);
}

.log__p,
.log__r {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--text-3);
}

.log__p i,
.log__r i {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  margin-right: 7px;
}

.log__r i {
  color: var(--gold-dp);
}

.log--locked {
  opacity: 0.75;
}

/* 「查看详情」：卡片右下角的细边框指示片。
   真正的点击热区是整张卡片，所以这里是 span 而不是 button —— 避免按钮套按钮。 */
.log__more {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  padding: 5px 11px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-3);
  transition: color 0.22s ease, border-color 0.22s ease, background 0.22s ease;
}

.log--open:hover .log__more,
.log:focus-visible .log__more {
  color: var(--gold-lt);
  border-color: var(--gold-dp);
  background: rgba(216, 178, 90, 0.08);
}

/* ================= 页脚 ================= */
.arc__foot {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 22px 24px 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid var(--line-soft);
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

/* ================= 响应式 ================= */
@media (max-width: 720px) {
  .arc__title {
    font-size: 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
