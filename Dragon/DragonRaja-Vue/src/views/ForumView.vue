<script setup>
/**
 * 论坛首页（/forum）
 *
 * 与首页的分工：首页是「学院门面」，这里是「讨论区入口」。
 * 一次 /api/forum/overview 拿全部数据（版块 / 最新 / 最热 / 统计 / 我的权限），
 * 首屏只发一个请求。
 *
 * 权限说明：版块卡片即使无权进入也会展示，并写明「还差什么」——
 * 藏起来的版块不会有人去争取权限，论坛的等级体系也就失去了意义。
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import { forumApi } from '@/api/forum'
import { relativeTime } from '@/utils/forumTime'

const router = useRouter()

/* ---------------- 概览数据 ---------------- */
const loading = ref(true)
const failMsg = ref('')
const data = ref(null)
const feed = ref('latest')

const boards = computed(() => data.value?.boards || [])
const stats = computed(() => data.value?.stats || {})
const me = computed(() => data.value?.me || {})
const list = computed(() => (feed.value === 'hot' ? data.value?.hot : data.value?.latest) || [])

/** 距下一档密级还差多少权限点（阈值与后端一致：3 / 8 / 15） */
const nextLevel = computed(() => {
  const p = me.value.points || 0
  const steps = [
    { at: 3, label: '内部' },
    { at: 8, label: '机密' },
    { at: 15, label: '绝密' }
  ]
  const target = steps.find((s) => p < s.at)
  if (!target) return null
  return { ...target, remain: target.at - p }
})

const levelPct = computed(() => {
  const p = me.value.points || 0
  const next = nextLevel.value
  return next ? Math.min(100, Math.round((p / next.at) * 100)) : 100
})

async function load() {
  loading.value = true
  failMsg.value = ''
  try {
    data.value = await forumApi.overview()
  } catch (e) {
    failMsg.value = e?.message || '论坛数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

/* ---------------- 轻提示 ---------------- */
const toast = ref('')
let toastTimer = 0
function showToast(text) {
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => (toast.value = ''), 2800)
}

/* ---------------- 跳转 ---------------- */
function openBoard(b) {
  if (b.locked) {
    // 游客被门槛拦住时，引导登录而不是干看着 —— 这是论坛拉新的关键时刻
    if (!me.value.logged) {
      showToast('该版块需要登录后浏览')
      router.push({ path: '/login', query: { redirect: `/board/${b.code}` } })
      return
    }
    showToast(`「${b.name}」尚未开放 · ${b.lockHint}`)
    return
  }
  router.push(`/board/${b.code}`)
}

function openThread(t) {
  if (t.locked) {
    showToast('该主题仅对更高权限开放，多发帖互动可以提升')
    return
  }
  router.push(`/thread/${t.id}`)
}

function goNew() {
  // 未登录时由路由守卫转登录页，并带上回跳地址
  router.push('/forum/new')
}

/* ---------------- 论坛内检索 ---------------- */
const kw = ref('')
const results = ref([])
const searching = ref(false)
const searchOpen = ref(false)
let searchTimer = 0
let blurTimer = 0

watch(kw, (v) => {
  clearTimeout(searchTimer)
  const q = v.trim()
  if (!q) {
    results.value = []
    searchOpen.value = false
    return
  }
  searchTimer = window.setTimeout(async () => {
    searching.value = true
    try {
      results.value = await forumApi.search(q, 8)
      searchOpen.value = true
    } catch {
      results.value = []
    } finally {
      searching.value = false
    }
  }, 280)
})

function onSearchFocus() {
  clearTimeout(blurTimer)
  if (kw.value.trim() && results.value.length) searchOpen.value = true
}

function onSearchBlur() {
  // 留 160ms：让点击结果先触发，不会被 blur 抢先关掉
  clearTimeout(blurTimer)
  blurTimer = window.setTimeout(() => (searchOpen.value = false), 160)
}

function pickResult(t) {
  searchOpen.value = false
  kw.value = ''
  openThread(t)
}

onBeforeUnmount(() => {
  clearTimeout(toastTimer)
  clearTimeout(searchTimer)
  clearTimeout(blurTimer)
})
</script>

<template>
  <div class="forum">
    <RuneBackground />

    <SiteNav />

    <main class="fm__body">
      <!-- ============ 头部 ============ -->
      <section class="fm__hero">
        <DragonEmblem class="fm__hero-mark" />
        <div class="fm__hero-text">
          <h1 class="fm__title">
            <span class="sheen-gold">龙族读者论坛</span>
            <span class="fm__title-en">DRAGON RAJA · READERS' FORUM</span>
          </h1>
          <p class="fm__sub">
            原著考据 · 人物讨论 · 言灵设定 · 同人创作 ——
            按版块归档，回复按楼层编号，游客可直接阅读
          </p>

          <ul class="fm__stats">
            <li><b>{{ stats.threadCount ?? 0 }}</b><span>主题</span></li>
            <li><b>{{ stats.replyCount ?? 0 }}</b><span>回复</span></li>
            <li><b>{{ stats.todayCount ?? 0 }}</b><span>今日新增</span></li>
            <li><b>{{ stats.memberCount ?? 0 }}</b><span>在册读者</span></li>
          </ul>
        </div>

        <button type="button" class="fm__new" @click="goNew">
          <AppIcon name="plus" :size="14" :stroke="2" />
          发起主题
        </button>
      </section>

      <!-- ============ 全论坛检索 ============ -->
      <section class="fm__search">
        <div class="fsearch" :class="{ 'is-open': searchOpen }">
          <AppIcon name="search" :size="16" class="fsearch__icon" />
          <input
            v-model="kw"
            class="fsearch__input"
            type="search"
            placeholder="全论坛检索：标题、正文、作者…"
            aria-label="全论坛检索"
            @focus="onSearchFocus"
            @blur="onSearchBlur"
          />
          <span v-if="searching" class="fsearch__hint">检索中…</span>
          <span v-else-if="kw.trim()" class="fsearch__hint">{{ results.length }} 条命中</span>

          <transition name="drop">
            <div v-if="searchOpen" class="fsearch__panel">
              <button
                v-for="r in results"
                :key="r.id"
                type="button"
                class="fsearch__row"
                @mousedown.prevent="pickResult(r)"
              >
                <span class="fsearch__row-title">{{ r.title }}</span>
                <span class="fsearch__row-meta">
                  {{ r.boardName }} · {{ r.authorName }} · {{ r.replyCount }} 回复
                  <template v-if="r.locked"> · 密级不足</template>
                </span>
              </button>
              <p v-if="!results.length" class="fsearch__empty">
                没有命中的主题。换个词，或者去对应的版块翻一翻。
              </p>
            </div>
          </transition>
        </div>
      </section>

      <!-- ============ 主体：两栏 ============ -->
      <div class="fm__main">
        <div class="fm__col">
          <!-- 版块 -->
          <section class="fm__section">
            <header class="fm__head">
              <h2 class="fm__h2"><AppIcon name="panel" :size="15" /> 版块</h2>
              <span class="fm__head-meta">{{ boards.length }} 个讨论区</span>
            </header>

            <div v-if="loading" class="fm__skeleton">
              <span v-for="i in 6" :key="i" class="sk-card" />
            </div>

            <div v-else-if="failMsg" class="fm__error">
              <AppIcon name="alert" :size="18" />
              <p>{{ failMsg }}</p>
              <button type="button" class="fm__retry" @click="load">重新加载</button>
            </div>

            <div v-else class="boards">
              <button
                v-for="b in boards"
                :key="b.code"
                type="button"
                class="bcard"
                :class="{ 'is-locked': b.locked }"
                @click="openBoard(b)"
              >
                <span class="bcard__icon"><AppIcon :name="b.icon" :size="18" /></span>
                <span class="bcard__body">
                  <span class="bcard__title">
                    {{ b.name }}
                    <em>{{ b.subtitle }}</em>
                  </span>
                  <span class="bcard__desc">{{ b.description }}</span>
                  <span class="bcard__meta">
                    <span>{{ b.threadCount }} 主题</span>
                    <span v-if="b.readOnly">只读公告</span>
                    <span v-if="b.locked" class="bcard__lock">
                      <AppIcon name="lock" :size="10" /> {{ b.lockHint }}
                    </span>
                  </span>
                </span>
                <AppIcon name="chevron-right" class="bcard__go" :size="13" />
              </button>
            </div>
          </section>

          <!-- 讨论 -->
          <section class="fm__section">
            <header class="fm__head">
              <h2 class="fm__h2"><AppIcon name="flame" :size="15" /> 讨论</h2>
              <div class="tabs" role="tablist">
                <button
                  type="button"
                  class="tab"
                  :class="{ 'is-on': feed === 'latest' }"
                  :aria-selected="feed === 'latest'"
                  role="tab"
                  @click="feed = 'latest'"
                >
                  最新回复
                </button>
                <button
                  type="button"
                  class="tab"
                  :class="{ 'is-on': feed === 'hot' }"
                  :aria-selected="feed === 'hot'"
                  role="tab"
                  @click="feed = 'hot'"
                >
                  热门
                </button>
              </div>
            </header>

            <ul v-if="list.length" class="feed">
              <li v-for="t in list" :key="t.id" class="frow" @click="openThread(t)">
                <div class="frow__main">
                  <div class="frow__tags">
                    <span v-if="t.pinned" class="chip chip--pin">置顶</span>
                    <span v-if="t.essence" class="chip chip--essence">精华</span>
                    <span v-if="t.replyVisible" class="chip">回复可见</span>
                    <span v-if="t.locked" class="chip chip--lock">
                      <AppIcon name="lock" :size="9" /> {{ t.securityLabel }}
                    </span>
                  </div>
                  <h3 class="frow__title">{{ t.title }}</h3>
                  <p class="frow__excerpt">
                    {{ t.excerpt || (t.locked ? '该主题仅对更高权限开放，仅显示标题' : '回复后可见正文') }}
                  </p>
                  <div class="frow__meta">
                    <span class="frow__board">{{ t.boardName }}</span>
                    <span>{{ t.authorName || '佚名' }}</span>
                    <span>{{ t.replyCount }} 回复</span>
                    <span>{{ t.viewCount }} 阅</span>
                    <span class="frow__time">{{ relativeTime(t.lastReplyAt || t.createdAt) }}</span>
                  </div>
                </div>
                <div class="frow__right">
                  <span class="frow__likes"><AppIcon name="sparkle" :size="11" /> {{ t.likeCount }}</span>
                  <AppIcon name="chevron-right" :size="13" class="frow__go" />
                </div>
              </li>
            </ul>

            <p v-else-if="!loading" class="fm__empty">
              <AppIcon name="info" :size="14" /> 暂无主题 · 点右上角「发起主题」开第一帖
            </p>
          </section>
        </div>

        <!-- 侧栏 -->
        <aside class="fm__side">
          <section class="side-card">
            <header class="side-card__head">
              <AppIcon name="id" :size="14" /> {{ me.logged ? '我的账号' : '游客浏览' }}
            </header>
            <div class="me">
              <span class="me__avatar">{{ (me.codename || '游').slice(0, 1) }}</span>
              <div class="me__text">
                <b>{{ me.codename || '未登录访客' }}</b>
                <span v-if="me.logged">{{ me.identityName || '读者' }} · 可读至「{{ me.maxLevelLabel }}」</span>
                <span v-else>登录后即可发帖、回复与收藏</span>
              </div>
            </div>

            <div v-if="me.logged" class="me__points">
              <div class="me__points-row">
                <span>积分</span>
                <b>{{ me.points ?? 0 }}</b>
              </div>
              <div class="me__bar"><i :style="{ width: levelPct + '%' }" /></div>
              <p class="me__hint">
                <template v-if="nextLevel">
                  当前「{{ me.levelLabel }}」· 距「{{ nextLevel.label }}」还差 {{ nextLevel.remain }} 点
                </template>
                <template v-else>已达最高密级「绝密」</template>
              </p>
            </div>

            <ul v-if="me.logged" class="me__nums">
              <li><b>{{ me.threadCount ?? 0 }}</b><span>我的主题</span></li>
              <li><b>{{ me.replyCount ?? 0 }}</b><span>我的回复</span></li>
            </ul>

            <router-link v-if="!me.logged" to="/login" class="side-link">
              登录 / 注册 <AppIcon name="arrow" :size="12" />
            </router-link>
            <router-link v-else to="/missions" class="side-link">
              每日任务 · 赚积分 <AppIcon name="arrow" :size="12" />
            </router-link>
          </section>

          <section class="side-card side-card--rule">
            <header class="side-card__head"><AppIcon name="shield" :size="14" /> 版规</header>
            <ol class="rules">
              <li>转载必须附原作者与授权方式，无授权一律下沉。</li>
              <li>涉及后几部情节的标题必须标「含剧透」，正文首段留空。</li>
              <li>本站为非官方、非商业兴趣站，不提供全文资源。</li>
            </ol>
          </section>
        </aside>
      </div>
    </main>

    <footer class="fm__foot">
      <span>龙族读者论坛 · 非官方 · 非商业</span>
      <span class="fm__foot-meta">版权归原作者所有 · 仅作学习交流</span>
    </footer>

    <transition name="toast">
      <div v-if="toast" class="toast"><AppIcon name="info" :size="14" /> {{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
/* ================= 布局 ================= */
.fm__body {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 28px 24px 56px;
}

.fm__hero {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.24);
}

.fm__hero-mark {
  width: 54px;
  height: 54px;
  flex: 0 0 auto;
  margin-top: 4px;
  filter: drop-shadow(0 0 14px rgba(216, 178, 90, 0.45));
  animation: spin 60s linear infinite;
}

.fm__hero-text {
  min-width: 0;
  flex: 1;
}

.fm__title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  font-family: var(--font-serif);
  font-size: 27px;
  letter-spacing: 0.12em;
  color: var(--text);
}

.fm__title-en {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.26em;
  color: var(--gold-dp);
  text-transform: uppercase;
}

.fm__sub {
  margin-top: 7px;
  font-size: 12.5px;
  line-height: 1.75;
  letter-spacing: 0.04em;
  color: var(--text-3);
}

.fm__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 26px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.fm__stats li {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.fm__stats b {
  font-family: var(--font-mono);
  font-size: 19px;
  font-weight: 500;
  color: var(--gold-lt);
  letter-spacing: 0.04em;
}

.fm__stats span {
  font-size: 10.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
}

.fm__new {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 18px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.55);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.24), rgba(216, 178, 90, 0.08));
  box-shadow: 0 0 20px -12px var(--gold-glow), inset 0 0 0 1px rgba(242, 220, 155, 0.12);
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.1em;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.24s, border-color 0.24s, transform 0.2s;
}

.fm__new:hover {
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.38), rgba(216, 178, 90, 0.14));
  border-color: rgba(216, 178, 90, 0.9);
  transform: translateY(-1px);
}

/* ================= 检索 ================= */
.fm__search {
  position: relative;
  z-index: 5;
  margin: 18px 0 22px;
}

.fsearch {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  height: 46px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.6);
  transition: border-color 0.24s, box-shadow 0.24s;
}

.fsearch:focus-within,
.fsearch.is-open {
  border-color: rgba(216, 178, 90, 0.5);
  box-shadow: 0 0 24px -12px var(--gold-glow);
}

.fsearch__icon {
  flex: 0 0 auto;
  color: var(--gold-dp);
}

.fsearch__input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: 0;
  background: transparent;
  font-size: 13px;
  letter-spacing: 0.04em;
  color: var(--text);
  outline: none;
}

.fsearch__input::placeholder {
  color: var(--text-4);
}

/* 去掉 type=search 的原生清除按钮，避免与深色主题冲突 */
.fsearch__input::-webkit-search-cancel-button {
  -webkit-appearance: none;
}

.fsearch__hint {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.fsearch__panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 8px);
  padding: 6px;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  background: linear-gradient(165deg, rgba(20, 28, 46, 0.98), rgba(8, 12, 22, 0.99));
  box-shadow: 0 30px 60px -30px #000;
  max-height: 360px;
  overflow-y: auto;
}

.fsearch__row {
  display: flex;
  flex-direction: column;
  gap: 3px;
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-radius: 8px;
  background: none;
  text-align: left;
  cursor: pointer;
  transition: background 0.18s;
}

.fsearch__row:hover {
  background: rgba(216, 178, 90, 0.08);
}

.fsearch__row-title {
  font-size: 13px;
  letter-spacing: 0.04em;
  color: var(--text);
}

.fsearch__row-meta {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.fsearch__empty {
  padding: 16px 12px;
  font-size: 12px;
  color: var(--text-4);
  text-align: center;
}

/* ================= 两栏 ================= */
.fm__main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 296px;
  align-items: start;
  gap: 20px;
}

.fm__col {
  min-width: 0;
}

.fm__section + .fm__section {
  margin-top: 26px;
}

.fm__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px 12px;
  margin-bottom: 14px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.26);
}

.fm__h2 {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding-left: 14px;
  font-family: var(--font-serif);
  font-size: 16.5px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--text);
}

.fm__h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 14px;
  border-radius: 2px;
  transform: translateY(-50%);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold) 40%, var(--gold-antique));
  box-shadow: 0 0 8px -2px var(--gold-glow);
}

.fm__h2 svg {
  color: var(--gold-dp);
}

.fm__head-meta {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

/* ================= 版块卡片 ================= */
.boards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.bcard {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 14px 13px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.72), rgba(14, 11, 23, 0.9));
  text-align: left;
  cursor: pointer;
  transition: border-color 0.26s, transform 0.26s, box-shadow 0.26s;
}

.bcard:hover {
  border-color: rgba(216, 178, 90, 0.42);
  transform: translateY(-3px);
  box-shadow: 0 20px 40px -28px #000, 0 0 24px -14px var(--gold-glow);
}

.bcard:focus-visible {
  outline: 2px solid var(--gold);
  outline-offset: 2px;
}

.bcard.is-locked {
  opacity: 0.78;
}

.bcard__icon {
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid rgba(216, 178, 90, 0.32);
  background: rgba(216, 178, 90, 0.08);
  color: var(--gold-lt);
}

.bcard__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bcard__title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-family: var(--font-serif);
  font-size: 14.5px;
  letter-spacing: 0.1em;
  color: var(--text);
}

.bcard__title em {
  font-family: var(--font-mono);
  font-style: normal;
  font-size: 8.5px;
  letter-spacing: 0.18em;
  color: var(--gold-dp);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bcard__desc {
  font-size: 11.5px;
  line-height: 1.65;
  letter-spacing: 0.03em;
  color: var(--text-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bcard__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 2px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.bcard__lock {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #e0a86a;
}

.bcard__go {
  flex: 0 0 auto;
  margin-top: 10px;
  color: var(--text-4);
  transition: color 0.2s, transform 0.2s;
}

.bcard:hover .bcard__go {
  color: var(--gold-lt);
  transform: translateX(2px);
}

/* ================= 骨架 / 错误 / 空态 ================= */
.fm__skeleton {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sk-card {
  height: 104px;
  border-radius: var(--r-lg);
  background: linear-gradient(
    100deg,
    rgba(255, 255, 255, 0.03) 20%,
    rgba(216, 178, 90, 0.08) 42%,
    rgba(255, 255, 255, 0.03) 64%
  );
  background-size: 220% 100%;
  animation: sk 1.4s ease-in-out infinite;
}

@keyframes sk {
  from {
    background-position: 120% 0;
  }
  to {
    background-position: -40% 0;
  }
}

.fm__error,
.fm__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 30px 20px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
  border-radius: var(--r-md);
  font-size: 12.5px;
  color: var(--text-4);
  text-align: center;
}

.fm__empty {
  flex-direction: row;
  justify-content: center;
}

.fm__retry {
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.1);
  color: var(--gold-lt);
  font-size: 11.5px;
  cursor: pointer;
}

/* ================= Tab ================= */
.tabs {
  display: flex;
  gap: 4px;
  padding: 3px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.5);
}

.tab {
  padding: 5px 14px;
  border: 0;
  border-radius: 999px;
  background: none;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.tab:hover {
  color: var(--text);
}

.tab.is-on {
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.14);
  box-shadow: inset 0 0 0 1px rgba(216, 178, 90, 0.3);
}

/* ================= 讨论列表 ================= */
.feed {
  margin: 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid var(--line-soft);
}

.frow {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px 8px;
  border-bottom: 1px solid var(--line-soft);
  cursor: pointer;
  transition: background 0.22s, padding-left 0.22s;
}

.frow:hover {
  background: rgba(216, 178, 90, 0.04);
  padding-left: 14px;
}

.frow__main {
  flex: 1;
  min-width: 0;
}

.frow__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 5px;
}

.frow__tags:empty {
  display: none;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 8px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.chip--pin {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.48);
  background: rgba(216, 178, 90, 0.1);
}

.chip--essence {
  color: #a5e4ff;
  border-color: rgba(74, 168, 212, 0.45);
  background: rgba(74, 168, 212, 0.1);
}

.chip--lock {
  color: #e0a86a;
  border-color: rgba(224, 168, 106, 0.4);
}

.frow__title {
  font-family: var(--font-serif);
  font-size: 14.5px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.frow__excerpt {
  margin-top: 4px;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--text-3);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.frow__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.frow__board {
  padding: 1px 7px;
  border-radius: 3px;
  border: 1px solid rgba(138, 106, 40, 0.32);
  color: var(--gold-dp);
}

.frow__time {
  margin-left: auto;
}

.frow__right {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
}

.frow__likes {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-4);
}

.frow__go {
  color: var(--text-4);
}

.frow:hover .frow__go {
  color: var(--gold-lt);
}

/* ================= 侧栏 ================= */
.fm__side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 92px;
}

.side-card {
  padding: 15px 16px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.78), rgba(14, 11, 23, 0.92));
  box-shadow: 0 0 24px -16px rgba(216, 178, 90, 0.4);
}

.side-card__head {
  display: flex;
  align-items: center;
  gap: 7px;
  padding-bottom: 10px;
  margin-bottom: 12px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.24);
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
}

.me {
  display: flex;
  align-items: center;
  gap: 10px;
}

.me__avatar {
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 16px;
  color: #1a1206;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.me__text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.me__text b {
  font-size: 13.5px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.me__text span {
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--text-4);
}

.me__points {
  margin-top: 14px;
}

.me__points-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: var(--text-3);
}

.me__points-row b {
  font-family: var(--font-mono);
  font-size: 15px;
  color: var(--gold-lt);
}

.me__bar {
  margin-top: 7px;
  height: 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
}

.me__bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--gold-antique), var(--gold-lt));
  box-shadow: 0 0 10px rgba(216, 178, 90, 0.6);
  transition: width 0.5s ease;
}

.me__hint {
  margin-top: 7px;
  font-size: 10.5px;
  line-height: 1.6;
  letter-spacing: 0.04em;
  color: var(--text-4);
}

.me__nums {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin: 14px 0 0;
  padding: 12px 0 0;
  border-top: 1px dashed var(--line-soft);
  list-style: none;
}

.me__nums li {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: center;
}

.me__nums b {
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 500;
  color: var(--text);
}

.me__nums span {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

.side-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 13px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.42);
  background: rgba(216, 178, 90, 0.08);
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: var(--gold-lt);
  transition: background 0.22s, transform 0.2s;
}

.side-link:hover {
  background: rgba(216, 178, 90, 0.18);
  transform: translateY(-1px);
}

.rules {
  margin: 0;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 11.5px;
  line-height: 1.7;
  letter-spacing: 0.03em;
  color: var(--text-3);
}

.rules li::marker {
  color: var(--gold-dp);
  font-family: var(--font-mono);
  font-size: 10px;
}

/* ================= 页脚 / 提示 ================= */
.fm__foot {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 20px 24px 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid var(--line-soft);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

.toast {
  position: fixed;
  left: 50%;
  bottom: 34px;
  z-index: 60;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.96), rgba(14, 11, 23, 0.98));
  box-shadow: 0 16px 40px -20px rgba(0, 0, 0, 0.9), 0 0 24px -12px var(--gold-glow);
  font-size: 12.5px;
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

.drop-enter-active,
.drop-leave-active {
  transition: opacity 0.2s, transform 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

.drop-enter-from,
.drop-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ================= 响应式 ================= */
@media (max-width: 980px) {
  .fm__main {
    grid-template-columns: minmax(0, 1fr);
  }

  .fm__side {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .fm__body {
    padding: 20px 16px 40px;
  }

  .fm__hero {
    flex-wrap: wrap;
  }

  .fm__new {
    width: 100%;
    justify-content: center;
  }

  .boards,
  .fm__skeleton {
    grid-template-columns: minmax(0, 1fr);
  }

  .fm__side {
    grid-template-columns: minmax(0, 1fr);
  }

  .fm__title {
    font-size: 22px;
  }

  .fm__stats {
    gap: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
