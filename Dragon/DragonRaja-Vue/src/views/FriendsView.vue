<script setup>
/**
 * 好友与私信（/friends）
 *
 * 页面结构：左侧「搜人 + 好友/申请列表」，右侧「会话」。
 *
 * 数据来源：
 *  - /api/friend/overview 一次拿齐计数、申请、好友列表（首屏只发一个请求）
 *  - 打开某个会话才拉消息，避免进页面就把所有历史拖下来
 *
 * 实时性：没有引入 WebSocket，用两条轮询兜住
 *  - 会话增量：4 秒一次，只拉「比手上最新一条更新」的消息
 *  - 概览静默刷新：15 秒一次，同步新申请与别处产生的未读
 * 页面隐藏（切到后台标签）时轮询自动让路，不做无意义的请求。
 *
 * 权限：好友与私信全部需要登录，路由 meta.requiresAuth 已挡一层，后端再挡一层。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import { friendApi } from '@/api/friend'
import { relativeTime } from '@/utils/forumTime'

/* ---------------- 基础状态 ---------------- */
const loading = ref(true)
const failMsg = ref('')
const overview = ref({})
const friends = ref([])
const requests = ref([])
const tab = ref('friends') // friends | requests

const activeId = ref(null)
const messages = ref([])
const chatLoading = ref(false)
const hasMore = ref(false)
const draft = ref('')
const sending = ref(false)
const scroller = ref(null)

const activeFriend = computed(
  () => friends.value.find((f) => f.id === activeId.value) || null
)

/* ---------------- 轻提示 ---------------- */
const toast = ref('')
let toastTimer = 0
function showToast(text) {
  if (!text) return
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => (toast.value = ''), 2800)
}

/* ---------------- 概览 ---------------- */
async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const data = await friendApi.overview()
    overview.value = data || {}
    friends.value = data?.friends || []
    requests.value = data?.requests || []
    failMsg.value = ''
    // 会话对象被对方解除后，列表里没有他了，右栏要跟着收起
    if (activeId.value && !friends.value.some((f) => f.id === activeId.value)) {
      closeChat()
    }
  } catch (e) {
    if (!silent) failMsg.value = e?.message || '好友数据加载失败'
  } finally {
    if (!silent) loading.value = false
  }
}

/* ---------------- 会话 ---------------- */
function scrollBottom() {
  nextTick(() => {
    const el = scroller.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function nearBottom() {
  const el = scroller.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 120
}

async function openChat(f) {
  if (activeId.value === f.id) return
  activeId.value = f.id
  messages.value = []
  hasMore.value = false
  tab.value = 'friends'
  await loadMessages()
  f.unread = 0
}

function closeChat() {
  activeId.value = null
  messages.value = []
  hasMore.value = false
}

async function loadMessages() {
  if (!activeId.value) return
  chatLoading.value = true
  try {
    const rows = await friendApi.messages({ friendId: activeId.value, size: 30 })
    messages.value = rows || []
    hasMore.value = (rows || []).length >= 30
    scrollBottom()
  } catch (e) {
    showToast(e?.message || '会话加载失败')
  } finally {
    chatLoading.value = false
  }
}

/** 往上翻：用游标拿更早的一页，并保持当前滚动位置不跳 */
async function loadEarlier() {
  const first = messages.value[0]
  const el = scroller.value
  if (!first || !activeId.value) return
  const prevHeight = el ? el.scrollHeight : 0
  try {
    const rows = await friendApi.messages({
      friendId: activeId.value,
      beforeId: first.id,
      size: 30
    })
    if (!rows || !rows.length) {
      hasMore.value = false
      return
    }
    messages.value = [...rows, ...messages.value]
    hasMore.value = rows.length >= 30
    await nextTick()
    if (el) el.scrollTop = el.scrollHeight - prevHeight
  } catch (e) {
    showToast(e?.message || '更早的消息加载失败')
  }
}

async function onSend() {
  const text = draft.value.trim()
  if (!text || sending.value || !activeId.value) return
  sending.value = true
  try {
    const msg = await friendApi.send({ toUserId: activeId.value, content: text })
    messages.value.push(msg)
    draft.value = ''
    const f = activeFriend.value
    if (f) {
      f.lastMessage = msg.content
      f.lastAt = msg.createdAt
    }
    scrollBottom()
  } catch (e) {
    showToast(e?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

/** 轮询新消息：只拉增量，避免整页替换把滚动位置冲掉 */
async function tick() {
  if (document.hidden) return
  if (!activeId.value || !messages.value.length) return
  const last = messages.value[messages.value.length - 1]
  try {
    const rows = await friendApi.newMessages(activeId.value, last.id)
    if (!rows || !rows.length) return
    const keep = nearBottom()
    messages.value.push(...rows)
    const f = activeFriend.value
    const tail = rows[rows.length - 1]
    if (f) {
      f.lastMessage = tail.content
      f.lastAt = tail.createdAt
      f.unread = 0
    }
    if (keep) scrollBottom()
  } catch {
    /* 轮询失败静默：下一轮会补上，不打扰用户 */
  }
}

/* ---------------- 好友申请 ---------------- */
async function onAccept(r) {
  try {
    const text = await friendApi.accept(r.id)
    showToast(text || '已添加为好友')
    await load(true)
  } catch (e) {
    showToast(e?.message || '操作失败')
  }
}

async function onReject(r) {
  try {
    const text = await friendApi.reject(r.id)
    showToast(text || '已拒绝')
    await load(true)
  } catch (e) {
    showToast(e?.message || '操作失败')
  }
}

async function onRemove() {
  const f = activeFriend.value
  if (!f) return
  const ok = window.confirm(
    `解除与「${f.codename}」的好友关系？\n\n历史密信会保留，重新加为好友后仍可查看。`
  )
  if (!ok) return
  try {
    const text = await friendApi.remove(f.id)
    showToast(text || '已解除好友关系')
    closeChat()
    await load(true)
  } catch (e) {
    showToast(e?.message || '操作失败')
  }
}

/* ---------------- 搜人 / 加好友 ---------------- */
const kw = ref('')
const searching = ref(false)
const results = ref([])
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
      results.value = await friendApi.search(q)
      searchOpen.value = true
    } catch (e) {
      results.value = []
      showToast(e?.message || '检索失败')
    } finally {
      searching.value = false
    }
  }, 280)
})

function onSearchBlur() {
  // 留一点时间给点击结果，别被 blur 抢先关掉面板
  clearTimeout(blurTimer)
  blurTimer = window.setTimeout(() => (searchOpen.value = false), 180)
}

function onSearchFocus() {
  clearTimeout(blurTimer)
  if (kw.value.trim() && results.value.length) searchOpen.value = true
}

async function addFriend(u) {
  if (u.relation === 'friend') return
  try {
    // pending_in（对方已申请我）在这里也会直接成为好友，后端已处理
    const text = await friendApi.sendRequest({ toUserId: u.id, message: '' })
    showToast(text || '申请已送达')
    if (u.relation === 'pending_in') {
      u.relation = 'friend'
      u.relationLabel = '已是好友'
      await load(true)
    } else {
      u.relation = 'pending_out'
      u.relationLabel = '已申请'
    }
  } catch (e) {
    showToast(e?.message || '操作失败')
  }
}

/* ---------------- 时间 ---------------- */
function hhmm(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

/* ---------------- 生命周期 ---------------- */
let chatTimer = 0
let overviewTimer = 0

onMounted(() => {
  load()
  chatTimer = window.setInterval(tick, 4000)
  overviewTimer = window.setInterval(() => {
    if (!document.hidden) load(true)
  }, 15000)
})

onBeforeUnmount(() => {
  clearInterval(chatTimer)
  clearInterval(overviewTimer)
  clearTimeout(toastTimer)
  clearTimeout(searchTimer)
  clearTimeout(blurTimer)
})
</script>

<template>
  <div class="fr">
    <RuneBackground />
    <SiteNav />

    <main class="fr__body">
      <!-- ============ 头部 ============ -->
      <section class="fr__hero">
        <DragonEmblem class="fr__mark" />
        <div class="fr__hero-text">
          <h1 class="fr__title">
            <span class="sheen-gold">好友与私信</span>
            <span class="fr__title-en">CASSELL · CONTACTS &amp; DISPATCH</span>
          </h1>
          <p class="fr__sub">
            按学号或行动代号查找同窗，互加好友后即可一对一传递密信
          </p>
          <ul class="fr__stats">
            <li>
              <b>{{ overview.friendCount ?? 0 }}</b><span>好友</span>
            </li>
            <li>
              <b>{{ overview.pendingCount ?? 0 }}</b><span>待处理申请</span>
            </li>
            <li>
              <b>{{ overview.unreadCount ?? 0 }}</b><span>未读密信</span>
            </li>
          </ul>
        </div>
      </section>

      <div v-if="failMsg" class="fr__error">
        <AppIcon name="alert" :size="18" />
        <p>{{ failMsg }}</p>
        <button type="button" class="fr__retry" @click="load()">重新加载</button>
      </div>

      <!-- ============ 主体：列表 + 会话 ============ -->
      <div class="fr__grid">
        <aside class="fr__side">
          <!-- 搜人 -->
          <div class="fsearch">
            <AppIcon name="search" :size="15" class="fsearch__icon" />
            <input
              v-model="kw"
              class="fsearch__input"
              type="search"
              placeholder="搜学号 / 行动代号…"
              aria-label="搜索可添加的同窗"
              @focus="onSearchFocus"
              @blur="onSearchBlur"
            />
            <span v-if="searching" class="fsearch__hint">…</span>

            <transition name="drop">
              <div v-if="searchOpen" class="fsearch__panel">
                <div v-for="u in results" :key="u.id" class="hit">
                  <span class="avatar avatar--sm">{{ (u.codename || '?').slice(0, 1) }}</span>
                  <span class="hit__body">
                    <span class="hit__name">
                      {{ u.codename }}
                      <em>{{ u.account }}</em>
                    </span>
                    <span class="hit__meta">
                      {{ u.identityName || '身份未定' }}
                      <template v-if="u.spiritName"> · {{ u.spiritName }}</template>
                    </span>
                  </span>
                  <button
                    v-if="u.relation === 'none'"
                    type="button"
                    class="hit__btn"
                    @mousedown.prevent="addFriend(u)"
                  >
                    加好友
                  </button>
                  <button
                    v-else-if="u.relation === 'pending_in'"
                    type="button"
                    class="hit__btn hit__btn--ok"
                    @mousedown.prevent="addFriend(u)"
                  >
                    同意
                  </button>
                  <span v-else class="hit__state">{{ u.relationLabel }}</span>
                </div>
                <p v-if="!results.length && !searching" class="fsearch__empty">
                  没有匹配的同窗。试试完整学号，或对方的行动代号。
                </p>
              </div>
            </transition>
          </div>

          <!-- 切换：好友 / 申请 -->
          <div class="tabs" role="tablist">
            <button
              type="button"
              class="tab"
              :class="{ 'is-on': tab === 'friends' }"
              role="tab"
              :aria-selected="tab === 'friends'"
              @click="tab = 'friends'"
            >
              好友 <em>{{ friends.length }}</em>
            </button>
            <button
              type="button"
              class="tab"
              :class="{ 'is-on': tab === 'requests' }"
              role="tab"
              :aria-selected="tab === 'requests'"
              @click="tab = 'requests'"
            >
              申请
              <em v-if="requests.length" class="tab__badge">{{ requests.length }}</em>
            </button>
          </div>

          <!-- 骨架 -->
          <div v-if="loading" class="sk-list">
            <span v-for="i in 4" :key="i" class="sk-row" />
          </div>

          <!-- 好友列表 -->
          <ul v-else-if="tab === 'friends'" class="flist">
            <li
              v-for="f in friends"
              :key="f.id"
              class="fitem"
              :class="{ 'is-active': f.id === activeId }"
              @click="openChat(f)"
            >
              <span class="avatar">{{ (f.codename || '?').slice(0, 1) }}</span>
              <span class="fitem__body">
                <span class="fitem__top">
                  <span class="fitem__name">{{ f.codename }}</span>
                  <span class="fitem__time">{{ f.lastAt ? relativeTime(f.lastAt) : '' }}</span>
                </span>
                <span class="fitem__last">
                  {{ f.lastMessage || '还没聊过，先去打个招呼' }}
                </span>
              </span>
              <span v-if="f.unread" class="fitem__badge">{{ f.unread > 99 ? '99+' : f.unread }}</span>
            </li>
            <li v-if="!friends.length" class="empty">
              <AppIcon name="user" :size="22" />
              <p>还没有好友。用上面的搜索框找一位同窗吧。</p>
            </li>
          </ul>

          <!-- 申请列表 -->
          <ul v-else class="rlist">
            <li v-for="r in requests" :key="r.id" class="ritem">
              <span class="avatar avatar--sm">{{ (r.codename || '?').slice(0, 1) }}</span>
              <span class="ritem__body">
                <span class="ritem__name">
                  {{ r.codename }}
                  <em>{{ r.account }}</em>
                </span>
                <span class="ritem__meta">
                  {{ r.identityName || '身份未定' }} · {{ relativeTime(r.createdAt) }}
                </span>
                <span v-if="r.message" class="ritem__msg">「{{ r.message }}」</span>
              </span>
              <span class="ritem__ops">
                <button type="button" class="mini mini--ok" @click="onAccept(r)">同意</button>
                <button type="button" class="mini" @click="onReject(r)">拒绝</button>
              </span>
            </li>
            <li v-if="!requests.length" class="empty">
              <AppIcon name="mail" :size="22" />
              <p>没有待处理的申请。</p>
            </li>
          </ul>
        </aside>

        <!-- ============ 会话窗口 ============ -->
        <section class="fr__chat">
          <template v-if="activeFriend">
            <header class="chat__head">
              <span class="avatar">{{ (activeFriend.codename || '?').slice(0, 1) }}</span>
              <div class="chat__who">
                <p class="chat__name">{{ activeFriend.codename }}</p>
                <p class="chat__meta">
                  {{ activeFriend.account }}
                  <template v-if="activeFriend.identityName"> · {{ activeFriend.identityName }}</template>
                  <template v-if="activeFriend.spiritName"> · {{ activeFriend.spiritName }}</template>
                </p>
              </div>
              <button type="button" class="chat__del" title="解除好友关系" @click="onRemove">
                <AppIcon name="trash" :size="14" />
                <span>解除好友</span>
              </button>
            </header>

            <div ref="scroller" class="chat__body">
              <button v-if="hasMore" type="button" class="chat__more" @click="loadEarlier">
                查看更早的密信
              </button>

              <p v-if="chatLoading" class="chat__hint">正在调阅密信…</p>
              <p v-else-if="!messages.length" class="chat__hint">
                还没有消息往来。第一句话，通常最难也最有用。
              </p>

              <div
                v-for="m in messages"
                :key="m.id"
                class="msg"
                :class="{ 'msg--mine': m.mine }"
              >
                <div class="msg__bubble">{{ m.content }}</div>
                <span class="msg__time">{{ hhmm(m.createdAt) }}</span>
              </div>
            </div>

            <footer class="chat__input">
              <textarea
                v-model="draft"
                class="chat__ta"
                rows="2"
                maxlength="500"
                placeholder="写点什么…（Enter 发送，Shift + Enter 换行）"
                @keydown.enter.exact.prevent="onSend"
              />
              <div class="chat__send-row">
                <span class="chat__count">{{ draft.length }} / 500</span>
                <button
                  type="button"
                  class="chat__send"
                  :disabled="sending || !draft.trim()"
                  @click="onSend"
                >
                  <AppIcon name="send" :size="14" />
                  {{ sending ? '发送中' : '发送' }}
                </button>
              </div>
            </footer>
          </template>

          <div v-else class="chat__placeholder">
            <AppIcon name="chat" :size="36" :stroke="1.2" />
            <p>从左侧选一位好友，开始密信往来</p>
            <span v-if="!friends.length">还没有好友时，先用左上角的搜索框找人</span>
          </div>
        </section>
      </div>
    </main>

    <transition name="drop">
      <div v-if="toast" class="fr__toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
/* ============================================================
   页面骨架
   ============================================================ */
.fr {
  position: relative;
  min-height: 100%;
  padding-bottom: 80px;
}

.fr__body {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: 0 auto;
  padding: 96px 24px 0;
}

/* ---------------- 头部 ---------------- */
.fr__hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 26px 30px;
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  background: linear-gradient(140deg, rgba(20, 28, 46, 0.82), rgba(8, 12, 22, 0.72));
  box-shadow: var(--shadow-card);
}

.fr__mark {
  width: 62px;
  height: 62px;
  flex: 0 0 auto;
  opacity: 0.92;
}

.fr__hero-text {
  min-width: 0;
}

.fr__title {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
  font-family: var(--font-serif);
  font-size: 26px;
  letter-spacing: 0.04em;
}

.fr__title-en {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.22em;
  color: var(--text-4);
}

.fr__sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-3);
}

.fr__stats {
  display: flex;
  gap: 26px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.fr__stats li {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.fr__stats b {
  font-family: var(--font-serif);
  font-size: 20px;
  color: var(--gold-lt);
}

.fr__stats span {
  font-size: 12px;
  color: var(--text-4);
  letter-spacing: 0.08em;
}

.fr__error {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
  padding: 14px 18px;
  border: 1px solid rgba(239, 107, 98, 0.35);
  border-radius: var(--r-md);
  background: rgba(179, 39, 45, 0.12);
  color: var(--err);
  font-size: 13px;
}

.fr__retry {
  margin-left: auto;
  padding: 6px 14px;
  border: 1px solid rgba(239, 107, 98, 0.5);
  border-radius: 999px;
  background: transparent;
  color: var(--err);
  cursor: pointer;
}

/* ---------------- 两栏 ---------------- */
.fr__grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  margin-top: 18px;
  align-items: stretch;
}

.fr__side,
.fr__chat {
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  background: var(--surface);
  box-shadow: var(--shadow-card);
}

.fr__side {
  display: flex;
  flex-direction: column;
  padding: 14px;
  min-height: 560px;
}

/* ============================================================
   搜人
   ============================================================ */
.fsearch {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 40px;
  border: 1px solid var(--line-soft);
  border-radius: var(--r-md);
  background: rgba(5, 7, 13, 0.5);
  transition: border-color 0.2s;
}

.fsearch:focus-within {
  border-color: rgba(216, 178, 90, 0.5);
}

.fsearch__icon {
  color: var(--text-4);
  flex: 0 0 auto;
}

.fsearch__input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: var(--text);
}

.fsearch__input::placeholder {
  color: var(--text-4);
}

.fsearch__hint {
  font-size: 12px;
  color: var(--text-4);
}

.fsearch__panel {
  position: absolute;
  z-index: 30;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  max-height: 360px;
  overflow-y: auto;
  padding: 6px;
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  background: rgba(10, 15, 26, 0.98);
  box-shadow: 0 24px 60px -20px rgba(0, 0, 0, 0.9);
}

.fsearch__empty {
  padding: 10px 8px;
  font-size: 12px;
  color: var(--text-4);
}

.hit {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 8px;
  border-radius: var(--r-sm);
}

.hit:hover {
  background: rgba(216, 178, 90, 0.06);
}

.hit__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.hit__name {
  font-size: 13px;
  color: var(--text);
}

.hit__name em {
  margin-left: 6px;
  font-family: var(--font-mono);
  font-style: normal;
  font-size: 11px;
  color: var(--text-4);
}

.hit__meta {
  font-size: 11px;
  color: var(--text-4);
}

.hit__btn {
  flex: 0 0 auto;
  padding: 5px 12px;
  border: 1px solid rgba(216, 178, 90, 0.4);
  border-radius: 999px;
  background: transparent;
  color: var(--gold-lt);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s;
}

.hit__btn:hover {
  background: rgba(216, 178, 90, 0.14);
}

.hit__btn--ok {
  border-color: rgba(95, 211, 154, 0.45);
  color: var(--ok);
}

.hit__state {
  flex: 0 0 auto;
  font-size: 11px;
  color: var(--text-4);
}

/* ============================================================
   切换 / 列表
   ============================================================ */
.tabs {
  display: flex;
  gap: 6px;
  margin: 14px 0 8px;
  padding: 3px;
  border-radius: 999px;
  background: rgba(5, 7, 13, 0.5);
  border: 1px solid var(--line-soft);
}

.tab {
  flex: 1;
  padding: 7px 0;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--text-3);
  font-size: 12.5px;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all 0.2s;
}

.tab em {
  margin-left: 4px;
  font-style: normal;
  color: var(--text-4);
}

.tab.is-on {
  background: rgba(216, 178, 90, 0.14);
  color: var(--gold-lt);
  box-shadow: inset 0 0 0 1px rgba(216, 178, 90, 0.28);
}

.tab__badge {
  display: inline-block;
  min-width: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--blood);
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}

.flist,
.rlist {
  flex: 1;
  margin: 0;
  padding: 0;
  list-style: none;
  overflow-y: auto;
  max-height: 460px;
}

.fitem {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 9px;
  border-radius: var(--r-md);
  cursor: pointer;
  transition: background 0.18s;
}

.fitem:hover {
  background: rgba(216, 178, 90, 0.05);
}

.fitem.is-active {
  background: rgba(216, 178, 90, 0.1);
  box-shadow: inset 2px 0 0 var(--gold);
}

.fitem__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.fitem__top {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.fitem__name {
  font-size: 13.5px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fitem__time {
  margin-left: auto;
  flex: 0 0 auto;
  font-size: 10.5px;
  color: var(--text-4);
}

.fitem__last {
  font-size: 11.5px;
  color: var(--text-4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fitem__badge {
  flex: 0 0 auto;
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--blood);
  color: #fff;
  font-size: 10px;
  line-height: 17px;
  text-align: center;
}

.ritem {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 9px;
  border-bottom: 1px solid var(--line-soft);
}

.ritem:last-child {
  border-bottom: none;
}

.ritem__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.ritem__name {
  font-size: 13px;
  color: var(--text);
}

.ritem__name em {
  margin-left: 6px;
  font-family: var(--font-mono);
  font-style: normal;
  font-size: 10.5px;
  color: var(--text-4);
}

.ritem__meta {
  font-size: 11px;
  color: var(--text-4);
}

.ritem__msg {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--text-2);
  word-break: break-word;
}

.ritem__ops {
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 0 0 auto;
}

.mini {
  padding: 3px 10px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: transparent;
  color: var(--text-3);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.18s;
}

.mini:hover {
  border-color: rgba(216, 178, 90, 0.45);
  color: var(--gold-lt);
}

.mini--ok {
  border-color: rgba(95, 211, 154, 0.4);
  color: var(--ok);
}

.mini--ok:hover {
  background: rgba(95, 211, 154, 0.12);
  border-color: rgba(95, 211, 154, 0.6);
  color: var(--ok);
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 16px;
  color: var(--text-4);
  font-size: 12.5px;
  text-align: center;
  line-height: 1.7;
}

/* 头像 */
.avatar {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(216, 178, 90, 0.32);
  background: radial-gradient(circle at 30% 25%, rgba(216, 178, 90, 0.28), rgba(10, 15, 26, 0.9));
  color: var(--gold-lt);
  font-family: var(--font-serif);
  font-size: 15px;
}

.avatar--sm {
  width: 28px;
  height: 28px;
  font-size: 13px;
}

/* 骨架 */
.sk-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 6px;
}

.sk-row {
  height: 46px;
  border-radius: var(--r-md);
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.03),
    rgba(255, 255, 255, 0.07),
    rgba(255, 255, 255, 0.03)
  );
  background-size: 200% 100%;
  animation: sk 1.4s infinite linear;
}

@keyframes sk {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}

/* ============================================================
   会话窗口
   ============================================================ */
.fr__chat {
  display: flex;
  flex-direction: column;
  min-height: 560px;
  max-height: 72vh;
  overflow: hidden;
}

.chat__head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
  background: rgba(8, 12, 22, 0.4);
}

.chat__who {
  min-width: 0;
}

.chat__name {
  font-family: var(--font-serif);
  font-size: 15.5px;
  letter-spacing: 0.04em;
  color: var(--gold-lt);
}

.chat__meta {
  font-size: 11.5px;
  color: var(--text-4);
}

.chat__del {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-left: auto;
  padding: 5px 12px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: transparent;
  color: var(--text-4);
  font-size: 11.5px;
  cursor: pointer;
  transition: all 0.18s;
}

.chat__del:hover {
  border-color: rgba(239, 107, 98, 0.45);
  color: var(--err);
}

.chat__body {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat__more {
  align-self: center;
  margin-bottom: 4px;
  padding: 5px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: transparent;
  color: var(--text-4);
  font-size: 11.5px;
  cursor: pointer;
}

.chat__more:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.4);
}

.chat__hint {
  margin: auto;
  padding: 20px;
  color: var(--text-4);
  font-size: 12.5px;
  text-align: center;
  line-height: 1.8;
}

.msg {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  max-width: 76%;
}

.msg--mine {
  align-self: flex-end;
  align-items: flex-end;
}

.msg__bubble {
  padding: 9px 13px;
  border: 1px solid var(--line-soft);
  border-radius: 14px 14px 14px 4px;
  background: rgba(20, 28, 46, 0.85);
  color: var(--text);
  font-size: 13.5px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg--mine .msg__bubble {
  border-color: rgba(216, 178, 90, 0.32);
  border-radius: 14px 14px 4px 14px;
  background: linear-gradient(140deg, rgba(216, 178, 90, 0.2), rgba(140, 106, 40, 0.16));
  color: var(--gold-lt);
}

.msg__time {
  padding: 0 3px;
  font-size: 10.5px;
  color: var(--text-4);
}

.chat__input {
  border-top: 1px solid var(--line);
  padding: 12px 16px 14px;
  background: rgba(8, 12, 22, 0.44);
}

.chat__ta {
  width: 100%;
  resize: none;
  padding: 10px 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--r-md);
  background: rgba(5, 7, 13, 0.55);
  color: var(--text);
  font-size: 13.5px;
  line-height: 1.6;
  outline: none;
  transition: border-color 0.2s;
}

.chat__ta:focus {
  border-color: rgba(216, 178, 90, 0.45);
}

.chat__ta::placeholder {
  color: var(--text-4);
}

.chat__send-row {
  display: flex;
  align-items: center;
  margin-top: 8px;
}

.chat__count {
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: var(--text-4);
}

.chat__send {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  padding: 7px 18px;
  border: 1px solid rgba(216, 178, 90, 0.5);
  border-radius: 999px;
  background: linear-gradient(140deg, rgba(216, 178, 90, 0.26), rgba(156, 116, 44, 0.2));
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: all 0.2s;
}

.chat__send:hover:not(:disabled) {
  box-shadow: var(--shadow-gold);
}

.chat__send:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.chat__placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-4);
  font-size: 13px;
  text-align: center;
  line-height: 1.8;
}

.chat__placeholder span {
  font-size: 11.5px;
  color: var(--text-4);
  opacity: 0.8;
}

/* ============================================================
   提示条
   ============================================================ */
.fr__toast {
  position: fixed;
  z-index: 60;
  left: 50%;
  bottom: 40px;
  transform: translateX(-50%);
  padding: 10px 22px;
  border: 1px solid rgba(216, 178, 90, 0.4);
  border-radius: 999px;
  background: rgba(10, 15, 26, 0.94);
  color: var(--gold-lt);
  font-size: 13px;
  box-shadow: var(--shadow-gold);
}

/* ============================================================
   响应式
   ============================================================ */
@media (max-width: 900px) {
  .fr__body {
    padding: 88px 14px 0;
  }

  .fr__hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px 18px;
  }

  .fr__grid {
    grid-template-columns: 1fr;
  }

  .fr__side {
    min-height: 0;
  }

  .flist,
  .rlist {
    max-height: 320px;
  }

  .fr__chat {
    min-height: 460px;
    max-height: 68vh;
  }

  .msg {
    max-width: 86%;
  }
}
</style>
