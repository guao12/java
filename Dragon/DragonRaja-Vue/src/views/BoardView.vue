<script setup>
/**
 * 版块页（/board/:code）
 *
 * 一页只做一件事：把这个馆区的主题按「最新 / 最热 / 入藏」列出来。
 * 版块元信息来自 /api/forum/overview（本来就有 3 秒缓存，从首页点进来不会重复请求）。
 *
 * 权限：进入时先看 overview 里的 locked 标记，锁住就不发列表请求 ——
 * 否则每次都会先吃一个 403 再显示提示，体验很差。
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import { forumApi } from '@/api/forum'
import { relativeTime } from '@/utils/forumTime'

const route = useRoute()
const router = useRouter()

const code = computed(() => String(route.params.code || ''))

const board = ref(null)
const rows = ref([])
const total = ref(0)
const pages = ref(0)
const page = ref(1)
const size = 20
const sort = ref('latest')
const kw = ref('')
const kwInput = ref('')

const loading = ref(true)
const errMsg = ref('')

const SORTS = [
  { id: 'latest', label: '最新批注' },
  { id: 'hot', label: '最热' },
  { id: 'essence', label: '仅入藏' }
]

const locked = computed(() => !!board.value?.locked)

async function loadBoard() {
  try {
    const ov = await forumApi.overview()
    board.value = (ov.boards || []).find((b) => b.code === code.value) || null
  } catch {
    board.value = null
  }
}

async function load() {
  if (locked.value) {
    loading.value = false
    return
  }
  loading.value = true
  errMsg.value = ''
  try {
    const res = await forumApi.threads({
      board: code.value,
      sort: sort.value,
      kw: kw.value,
      page: page.value,
      size
    })
    rows.value = res?.list || []
    total.value = res?.total || 0
    pages.value = res?.pages || 0
  } catch (e) {
    errMsg.value = e?.message || '列表加载失败'
    rows.value = []
    total.value = 0
    pages.value = 0
  } finally {
    loading.value = false
  }
}

async function boot() {
  loading.value = true
  await loadBoard()
  await load()
}

onMounted(boot)

watch(code, () => {
  page.value = 1
  sort.value = 'latest'
  kw.value = ''
  kwInput.value = ''
  rows.value = []
  boot()
})

onBeforeUnmount(() => clearTimeout(toastTimer))

/* ---------------- 交互 ---------------- */
function switchSort(id) {
  if (sort.value === id) return
  sort.value = id
  page.value = 1
  load()
}

function doSearch() {
  const q = kwInput.value.trim()
  if (q === kw.value) return
  kw.value = q
  page.value = 1
  load()
}

function clearSearch() {
  kwInput.value = ''
  if (!kw.value) return
  kw.value = ''
  page.value = 1
  load()
}

function goPage(p) {
  const next = Math.min(Math.max(1, p), Math.max(1, pages.value))
  if (next === page.value) return
  page.value = next
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goNew() {
  router.push({ path: '/forum/new', query: { board: code.value } })
}

function openThread(t) {
  if (t.locked) {
    showToast('该主题密级高于你的可读上限')
    return
  }
  router.push(`/thread/${t.id}`)
}

function goMissions() {
  router.push('/missions')
}

/* ---------------- 轻提示 ---------------- */
const toast = ref('')
let toastTimer = 0
function showToast(text) {
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => (toast.value = ''), 2800)
}
</script>

<template>
  <div class="board">
    <RuneBackground />
    <SiteNav />

    <main class="bd__body">
      <!-- 面包屑 -->
      <nav class="crumb" aria-label="位置">
        <router-link to="/forum">论坛</router-link>
        <span class="crumb__sep">/</span>
        <span class="crumb__now">{{ board?.name || code }}</span>
      </nav>

      <!-- 版块头 -->
      <section v-if="board" class="bd__hero" :class="{ 'is-locked': locked }">
        <span class="bd__icon"><AppIcon :name="board.icon" :size="22" /></span>
        <div class="bd__hero-text">
          <h1 class="bd__title">
            {{ board.name }}
            <em>{{ board.subtitle }}</em>
          </h1>
          <p class="bd__desc">{{ board.description }}</p>
          <div class="bd__meta">
            <span>{{ board.threadCount }} 主题</span>
            <span v-if="board.readOnly">只读公告 · 仅教员可发</span>
            <span v-if="locked" class="bd__lock">
              <AppIcon name="lock" :size="10" /> {{ board.lockHint }}
            </span>
          </div>
        </div>
        <button v-if="board.canPost" type="button" class="bd__new" @click="goNew">
          <AppIcon name="plus" :size="13" :stroke="2" /> 在本馆区发帖
        </button>
      </section>

      <!-- 锁态：给出明确的达标路径 -->
      <section v-else-if="!loading" class="bd__void">
        <DragonEmblem class="bd__void-mark" />
        <h2>馆区不存在</h2>
        <p>这个标识没有对应的讨论区，可能已经合并或改名。</p>
        <router-link to="/forum" class="bd__void-link">返回论坛首页</router-link>
      </section>

      <section v-if="locked" class="bd__void">
        <AppIcon name="lock" :size="30" />
        <h2>该馆区尚未对你开放</h2>
        <p>{{ board?.lockHint }}。馆务积分由执行部任务累积，完成一次即可看到进度变化。</p>
        <button type="button" class="bd__void-link" @click="goMissions">前往任务看板</button>
      </section>

      <!-- 工具条 + 列表 -->
      <template v-if="!locked">
        <section class="bd__toolbar">
          <div class="btabs" role="tablist">
            <button
              v-for="s in SORTS"
              :key="s.id"
              type="button"
              class="btab"
              :class="{ 'is-on': sort === s.id }"
              role="tab"
              :aria-selected="sort === s.id"
              @click="switchSort(s.id)"
            >
              {{ s.label }}
            </button>
          </div>

          <div class="bsearch">
            <AppIcon name="search" :size="14" class="bsearch__icon" />
            <input
              v-model="kwInput"
              class="bsearch__input"
              type="search"
              placeholder="在本馆区检索…"
              aria-label="在本馆区检索"
              @keyup.enter="doSearch"
            />
            <button v-if="kw" type="button" class="bsearch__clear" @click="clearSearch">
              <AppIcon name="close" :size="11" />
            </button>
          </div>

          <span class="bd__count">共 {{ total }} 条</span>
        </section>

        <p v-if="kw" class="bd__filter">
          正在检索「{{ kw }}」
          <button type="button" @click="clearSearch">清除</button>
        </p>

        <div v-if="loading" class="bd__skeleton">
          <span v-for="i in 6" :key="i" class="sk-row" />
        </div>

        <div v-else-if="errMsg" class="bd__error">
          <AppIcon name="alert" :size="18" />
          <p>{{ errMsg }}</p>
          <button type="button" class="bd__retry" @click="load">重试</button>
        </div>

        <ul v-else-if="rows.length" class="tlist">
          <li v-for="t in rows" :key="t.id" class="trow" @click="openThread(t)">
            <div class="trow__main">
              <div class="trow__tags">
                <span v-if="t.pinned" class="chip chip--pin">镇馆</span>
                <span v-if="t.essence" class="chip chip--essence">入藏</span>
                <span v-if="t.replyVisible" class="chip">回复可见</span>
                <span v-if="t.locked" class="chip chip--lock">
                  <AppIcon name="lock" :size="9" /> {{ t.securityLabel }}
                </span>
                <span v-if="t.mine" class="chip chip--mine">我发的</span>
              </div>
              <h3 class="trow__title">{{ t.title }}</h3>
              <p class="trow__excerpt">
                {{ t.excerpt || (t.locked ? '本文高于你的可读密级，仅显示标题' : '回复后可见正文') }}
              </p>
              <div class="trow__meta">
                <span>{{ t.authorName || '佚名' }}</span>
                <span class="dot">·</span>
                <span>{{ relativeTime(t.createdAt) }}</span>
                <span v-if="t.lastReplyAt" class="dot">·</span>
                <span v-if="t.lastReplyAt">最后批注 {{ relativeTime(t.lastReplyAt) }}</span>
              </div>
            </div>
            <div class="trow__nums">
              <span class="tnum"><b>{{ t.replyCount }}</b>批注</span>
              <span class="tnum"><b>{{ t.viewCount }}</b>阅读</span>
              <span class="tnum"><b>{{ t.likeCount }}</b>点赞</span>
              <AppIcon name="chevron-right" :size="13" class="trow__go" />
            </div>
          </li>
        </ul>

        <p v-else class="bd__empty">
          <AppIcon name="info" :size="14" />
          <span>
            {{ kw ? '没有命中的主题，换个关键词试试' : '本馆区还没有主题' }}
            <button v-if="!kw && board?.canPost" type="button" class="bd__empty-link" @click="goNew">
              发第一帖
            </button>
          </span>
        </p>

        <!-- 分页 -->
        <nav v-if="pages > 1" class="pager" aria-label="分页">
          <button type="button" class="pager__btn" :disabled="page <= 1" @click="goPage(page - 1)">
            <AppIcon name="chevron-left" :size="12" /> 上一页
          </button>
          <span class="pager__now">{{ page }} / {{ pages }}</span>
          <button
            type="button"
            class="pager__btn"
            :disabled="page >= pages"
            @click="goPage(page + 1)"
          >
            下一页 <AppIcon name="chevron-right" :size="12" />
          </button>
        </nav>
      </template>
    </main>

    <footer class="bd__foot">
      <span>以龙档案馆 · 同人兴趣站 · 非商用</span>
      <span class="bd__foot-meta">版权归原作者所有 · 仅作学习交流</span>
    </footer>

    <transition name="toast">
      <div v-if="toast" class="toast"><AppIcon name="info" :size="14" /> {{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
.bd__body {
  width: 100%;
  max-width: 1040px;
  margin: 0 auto;
  padding: 22px 24px 56px;
}

/* ================= 面包屑 ================= */
.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

.crumb a {
  color: var(--text-3);
  transition: color 0.2s;
}

.crumb a:hover {
  color: var(--gold-lt);
}

.crumb__sep {
  color: var(--gold-dp);
}

.crumb__now {
  color: var(--gold-lt);
}

/* ================= 版块头 ================= */
.bd__hero {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin: 16px 0 20px;
  padding: 18px 20px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.8), rgba(14, 11, 23, 0.92));
  box-shadow: 0 0 26px -18px rgba(216, 178, 90, 0.5);
}

.bd__icon {
  flex: 0 0 auto;
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid rgba(216, 178, 90, 0.36);
  background: rgba(216, 178, 90, 0.08);
  color: var(--gold-lt);
}

.bd__hero-text {
  flex: 1;
  min-width: 0;
}

.bd__title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
  font-family: var(--font-serif);
  font-size: 22px;
  letter-spacing: 0.12em;
  color: var(--text);
}

.bd__title em {
  font-family: var(--font-mono);
  font-style: normal;
  font-size: 9.5px;
  letter-spacing: 0.22em;
  color: var(--gold-dp);
}

.bd__desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  letter-spacing: 0.04em;
  color: var(--text-3);
}

.bd__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.bd__lock {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #e0a86a;
}

.bd__new {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.55);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.24), rgba(216, 178, 90, 0.08));
  color: var(--gold-lt);
  font-size: 12px;
  letter-spacing: 0.08em;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.22s, transform 0.2s;
}

.bd__new:hover {
  background: rgba(216, 178, 90, 0.34);
  transform: translateY(-1px);
}

/* ================= 锁态 / 空馆区 ================= */
.bd__void {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 54px 24px;
  margin-top: 10px;
  border: 1px dashed rgba(216, 178, 90, 0.22);
  border-radius: var(--r-lg);
  background: rgba(4, 7, 13, 0.4);
  text-align: center;
  color: var(--gold-dp);
}

.bd__void-mark {
  width: 48px;
  height: 48px;
  opacity: 0.7;
}

.bd__void h2 {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0.12em;
  color: var(--text);
}

.bd__void p {
  max-width: 460px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--text-3);
}

.bd__void-link {
  margin-top: 4px;
  padding: 7px 18px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.48);
  background: rgba(216, 178, 90, 0.1);
  color: var(--gold-lt);
  font-size: 11.5px;
  letter-spacing: 0.08em;
  cursor: pointer;
}

/* ================= 工具条 ================= */
.bd__toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 2px 12px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.26);
}

.btabs {
  display: flex;
  gap: 4px;
}

.btab {
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: none;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}

.btab:hover {
  color: var(--text);
  background: rgba(255, 255, 255, 0.04);
}

.btab.is-on {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.36);
  background: rgba(216, 178, 90, 0.12);
}

.bsearch {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.5);
  transition: border-color 0.22s;
}

.bsearch:focus-within {
  border-color: rgba(216, 178, 90, 0.45);
}

.bsearch__icon {
  flex: 0 0 auto;
  color: var(--gold-dp);
}

.bsearch__input {
  flex: 1;
  min-width: 0;
  border: 0;
  background: transparent;
  font-size: 12px;
  color: var(--text);
  outline: none;
}

.bsearch__input::placeholder {
  color: var(--text-4);
}

.bsearch__input::-webkit-search-cancel-button {
  -webkit-appearance: none;
}

.bsearch__clear {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-3);
  cursor: pointer;
}

.bd__count {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.bd__filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 10px 2px 0;
  font-size: 11.5px;
  color: var(--text-3);
}

.bd__filter button {
  border: 0;
  background: none;
  color: var(--gold-dp);
  font-size: 11px;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

/* ================= 列表 ================= */
.tlist {
  margin: 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid var(--line-soft);
}

.trow {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 8px;
  border-bottom: 1px solid var(--line-soft);
  cursor: pointer;
  transition: background 0.22s, padding-left 0.22s;
}

.trow:hover {
  background: rgba(216, 178, 90, 0.04);
  padding-left: 14px;
}

.trow__main {
  flex: 1;
  min-width: 0;
}

.trow__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 5px;
}

.trow__tags:empty {
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

.chip--mine {
  color: #8ee9b8;
  border-color: rgba(95, 211, 154, 0.38);
}

.trow__title {
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trow__excerpt {
  margin-top: 4px;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--text-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.trow__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 7px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.dot {
  color: var(--gold-dp);
  opacity: 0.6;
}

.trow__nums {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 14px;
  padding-top: 4px;
}

.tnum {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  color: var(--text-4);
  min-width: 34px;
}

.tnum b {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--text-2);
}

.trow:hover .tnum b {
  color: var(--gold-lt);
}

.trow__go {
  color: var(--text-4);
}

/* ================= 骨架 / 错误 / 空 ================= */
.bd__skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
}

.sk-row {
  height: 76px;
  border-radius: var(--r-md);
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

.bd__error,
.bd__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 34px 20px;
  font-size: 12.5px;
  color: var(--text-4);
  text-align: center;
}

.bd__retry,
.bd__empty-link {
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.1);
  color: var(--gold-lt);
  font-size: 11px;
  cursor: pointer;
}

/* ================= 分页 ================= */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 20px;
}

.pager__btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(255, 255, 255, 0.02);
  color: var(--text-3);
  font-size: 11.5px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}

.pager__btn:hover:not(:disabled) {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.08);
}

.pager__btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.pager__now {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

/* ================= 页脚 / 提示 ================= */
.bd__foot {
  width: 100%;
  max-width: 1040px;
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

/* ================= 响应式 ================= */
@media (max-width: 760px) {
  .bd__body {
    padding: 18px 16px 40px;
  }

  .bd__hero {
    flex-wrap: wrap;
  }

  .bd__new {
    width: 100%;
    justify-content: center;
  }

  .bd__toolbar {
    flex-wrap: wrap;
  }

  .bsearch {
    order: 3;
    flex-basis: 100%;
  }

  .bd__count {
    margin-left: auto;
  }

  .trow__nums {
    gap: 8px;
  }

  .tnum:nth-child(2) {
    display: none;
  }
}

@media (max-width: 520px) {
  .tnum {
    display: none;
  }

  .bd__title {
    font-size: 19px;
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
