<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthCard from '@/components/AuthCard.vue'
import GlowButton from '@/components/GlowButton.vue'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import { authApi } from '@/api/auth'
import { CODE_UNAUTHORIZED, request } from '@/api/http'
import { forumApi } from '@/api/forum'
import { relativeTime } from '@/utils/forumTime'
import { clearSession, getUser } from '@/utils/auth'

const router = useRouter()

/** 先用本地缓存渲染，避免白屏；随后用令牌拉取最新数据 */
const profile = ref(getUser())
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    profile.value = await authApi.profile()
  } catch (e) {
    if (e.code === CODE_UNAUTHORIZED) {
      router.replace({ path: '/login', query: { redirect: '/console' } })
      return
    }
    error.value = e.message
  } finally {
    loading.value = false
  }
}

/** 回到论坛首页 */
function goHome() {
  router.push('/forum')
}

/* ---------------- 我的收藏 ---------------- */

const favorites = ref([])
const favTotal = ref(0)
const favLoading = ref(true)

async function loadFavorites() {
  favLoading.value = true
  try {
    const res = await forumApi.collected({ page: 1, size: 10 })
    favorites.value = res?.list || []
    favTotal.value = Number(res?.total || favorites.value.length)
  } catch {
    // 收藏夹失败不该挡住身份卡：静默降级为空列表
    favorites.value = []
    favTotal.value = 0
  } finally {
    favLoading.value = false
  }
}

function openThread(t) {
  router.push(`/thread/${t.id}`)
}

async function uncollect(t) {
  const prev = favorites.value
  // 先摘掉再请求，失败时整体回滚 —— 收藏是个高频轻操作，等一次往返太慢
  favorites.value = favorites.value.filter((x) => x.id !== t.id)
  favTotal.value = Math.max(0, favTotal.value - 1)
  try {
    await forumApi.collect(t.id)
  } catch {
    favorites.value = prev
    favTotal.value = prev.length
  }
}

async function logout() {
  try {
    await authApi.logout()
  } catch {
    /* 令牌可能已过期，本地清理即可 */
  }
  clearSession()
  request.clearCache() // 清空 GET 缓存，避免下一位登录者读到本账号的缓存
  router.replace('/login')
}

onMounted(() => {
  load()
  loadFavorites()
})
</script>

<template>
  <AuthCard
    wide
    badge="Cassell Intranet"
    :title="profile ? `${profile.codename} 的身份卡` : '学院内网'"
    subtitle="令牌鉴权通过，以下信息来自 MySQL · dr_user"
  >
    <div v-if="loading && !profile" class="state">
      <span class="state__spinner" />
      正在调取身份档案…
    </div>

    <template v-else>
      <div class="id-card">
        <DragonEmblem class="id-card__mark" />
        <div class="id-card__main">
          <p class="id-card__name">{{ profile?.codename }}</p>
          <p class="id-card__account">
            <AppIcon name="id" :size="13" />
            {{ profile?.account }}
          </p>
        </div>
        <span class="id-card__status"><i class="dot-live" />在册</span>
      </div>

      <dl class="info-list">
        <div class="info-row">
          <dt>验证方式</dt>
          <dd>系统验证码核验</dd>
        </div>
        <div class="info-row">
          <dt>在校身份</dt>
          <dd>{{ profile?.identityName }}</dd>
        </div>
        <div class="info-row">
          <dt>言灵</dt>
          <dd>{{ profile?.spiritName }}</dd>
        </div>
        <div class="info-row">
          <dt>累计登录</dt>
          <dd>{{ profile?.loginCount ?? 0 }} 次</dd>
        </div>
        <div class="info-row">
          <dt>最近登录</dt>
          <dd>{{ profile?.lastLoginAt || '——' }}</dd>
        </div>
        <div class="info-row">
          <dt>登记时间</dt>
          <dd>{{ profile?.createdAt || '——' }}</dd>
        </div>
      </dl>

      <div v-if="error" class="alert alert--err">
        <AppIcon name="alert" :size="15" />
        <span>{{ error }}</span>
      </div>

      <!-- 我的收藏：收藏夹总得有个地方能翻，否则「收藏」只是心理安慰 -->
      <section class="fav">
        <header class="fav__head">
          <h2 class="fav__title"><AppIcon name="archive" :size="14" /> 我的收藏</h2>
          <span class="fav__count">{{ favTotal }} 条</span>
        </header>

        <p v-if="favLoading" class="fav__state">正在读取收藏夹…</p>
        <p v-else-if="!favorites.length" class="fav__state">
          还没有收藏。在主题页点「收藏」，帖子会出现在这里。
        </p>
        <ul v-else class="fav__list">
          <li v-for="t in favorites" :key="t.id" class="fav__item">
            <button type="button" class="fav__link" @click="openThread(t)">
              <span class="fav__board">{{ t.boardName }}</span>
              <span class="fav__name">{{ t.title }}</span>
              <span class="fav__meta">{{ t.replyCount }} 回复 · {{ relativeTime(t.lastReplyAt || t.createdAt) }}</span>
            </button>
            <button
              type="button"
              class="fav__x"
              :aria-label="`取消收藏 ${t.title}`"
              title="取消收藏"
              @click="uncollect(t)"
            >
              <AppIcon name="close" :size="12" />
            </button>
          </li>
        </ul>
      </section>

      <div class="actions">
        <GlowButton variant="ghost" type="button" :disabled="loading" @click="load">
          <AppIcon name="refresh" :size="16" />
          {{ loading ? '刷新中…' : '刷新档案' }}
        </GlowButton>
        <GlowButton variant="ghost" type="button" @click="goHome">
          <AppIcon name="home" :size="16" />
          返回首页
        </GlowButton>
        <GlowButton type="button" @click="logout">
          <AppIcon name="logout" :size="16" />
          退出登录
        </GlowButton>
      </div>
    </template>

    <template #foot>
      需要换一张身份卡？<router-link class="link-gold" to="/login">返回登录页</router-link>
    </template>
  </AuthCard>
</template>

<style scoped>
.state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 34px 0;
  font-size: 13px;
  color: var(--text-3);
}
.state__spinner {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  border: 2px solid rgba(216, 178, 90, 0.25);
  border-top-color: var(--gold);
  animation: spin 0.7s linear infinite;
}

.id-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  background: linear-gradient(140deg, rgba(216, 178, 90, 0.09), rgba(8, 12, 22, 0.5));
}
.id-card__mark {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
}
.id-card__main {
  flex: 1;
  min-width: 0;
}
.id-card__name {
  font-family: var(--font-serif);
  font-size: 16px;
  letter-spacing: 0.1em;
  color: var(--text);
}
.id-card__account {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
  font-family: var(--font-mono);
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: var(--gold-dp);
}
.id-card__status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.16em;
  color: var(--ok);
}

.info-list {
  margin: 18px 0 0;
  display: flex;
  flex-direction: column;
}
.info-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 2px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.07);
}
.info-row:last-child {
  border-bottom: 0;
}
.info-row dt {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--text-4);
  white-space: nowrap;
}
.info-row dd {
  margin: 0;
  font-size: 13px;
  color: var(--text);
  text-align: right;
  word-break: break-all;
}

/* 三个按钮并排时每列只有约 118px，而 .btn 的 22px 内边距 + 0.2em 字距
   会让「刷新档案」这类四字标签必定折行。改成 grid 定宽列 + 收紧内边距与字距。 */
.actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 20px;
}
.actions > * {
  min-width: 0;
}
.actions .btn {
  padding: 12px 8px;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

/* ================= 我的收藏 ================= */
.fav {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.fav__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.fav__title {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--gold-lt);
  letter-spacing: 0.04em;
}

.fav__count {
  font-size: 11px;
  color: var(--text-4);
  font-variant-numeric: tabular-nums;
}

.fav__state {
  margin: 0;
  padding: 16px 14px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--text-4);
}

.fav__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 整行是按钮：收藏夹里的主要动作是「点进去看」，不是「管理」 */
.fav__item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.fav__link {
  flex: 1;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2px 10px;
  padding: 9px 12px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.02);
  color: inherit;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s, background 0.18s;
}

.fav__link:hover {
  border-color: rgba(216, 178, 90, 0.3);
  background: rgba(216, 178, 90, 0.06);
}

.fav__board {
  grid-row: span 2;
  align-self: center;
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.28);
  font-size: 10px;
  color: var(--gold);
  white-space: nowrap;
}

.fav__name {
  font-size: 12.5px;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fav__meta {
  font-size: 10.5px;
  color: var(--text-4);
}

.fav__x {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 1px solid transparent;
  border-radius: 50%;
  background: transparent;
  color: var(--text-4);
  cursor: pointer;
  transition: color 0.18s, border-color 0.18s;
}

.fav__x:hover {
  color: #d98a8a;
  border-color: rgba(217, 138, 138, 0.4);
}

/* 极窄屏不硬塞三列，直接竖排 */
@media (max-width: 420px) {
  .actions {
    grid-template-columns: minmax(0, 1fr);
  }
  .actions .btn {
    padding: 13px 16px;
    letter-spacing: 0.1em;
  }
}
</style>
