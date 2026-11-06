<script setup>
/**
 * 主题详情（/thread/:id）
 *
 * 三类状态要分别处理，否则用户会以为「页面坏了」：
 *  1. 正常：主楼正文 + 楼层 + 回复框
 *  2. 密级不足：标题照常显示，正文替换为封存说明，并写明当前可读上限
 *  3. 回复可见未解锁：同样是封存，但明确告诉用户「回复后解锁」，且楼层可见
 *
 * 正文一律按纯文本分段渲染，绝不使用 v-html —— 帖子内容来自用户输入。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import { forumApi } from '@/api/forum'
import { fullTime, paragraphs, relativeTime } from '@/utils/forumTime'

const route = useRoute()
const router = useRouter()

const id = computed(() => String(route.params.id || ''))

const detail = ref(null)
const loading = ref(true)
const errMsg = ref('')

const thread = computed(() => detail.value?.thread || null)
const replies = computed(() => detail.value?.replies || [])
const bodyParas = computed(() => paragraphs(detail.value?.content))
const hidden = computed(() => !!detail.value?.contentHidden)

const replyText = ref('')
const submitting = ref(false)
const quote = ref(null)
const replyBox = ref(null)

async function load() {
  loading.value = true
  errMsg.value = ''
  try {
    detail.value = await forumApi.thread(id.value)
  } catch (e) {
    errMsg.value = e?.message || '主题加载失败'
    detail.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => clearTimeout(toastTimer))

/* ---------------- 点赞 ---------------- */
const liking = ref(false)
async function toggleLike() {
  if (!thread.value || liking.value) return
  liking.value = true
  try {
    const res = await forumApi.like(thread.value.id)
    thread.value.liked = !!res?.liked
    thread.value.likeCount = res?.count ?? thread.value.likeCount
    showToast(res?.liked ? '已点赞' : '已取消点赞')
  } catch (e) {
    showToast(e?.message || '操作失败')
  } finally {
    liking.value = false
  }
}

/* ---------------- 回复 / 引用 ---------------- */
function quoteReply(r) {
  quote.value = {
    id: r.id,
    floorNo: r.floorNo,
    authorName: r.authorName,
    text: r.content
  }
  focusEditor()
}

function cancelQuote() {
  quote.value = null
}

function focusEditor() {
  nextTick(() => {
    replyBox.value?.focus()
    replyBox.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

async function submitReply() {
  if (!thread.value || submitting.value) return
  const text = replyText.value.trim()
  if (text.length < 2) {
    showToast('批注至少写两个字')
    return
  }
  submitting.value = true
  try {
    const res = await forumApi.reply(thread.value.id, {
      content: text,
      quoteId: quote.value?.id || 0
    })
    detail.value = res
    replyText.value = ''
    quote.value = null
    showToast('批注已提交')
    nextTick(() => {
      const list = document.querySelector('.floors')
      if (list) list.lastElementChild?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  } catch (e) {
    showToast(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

/* ---------------- 跳转 ---------------- */
function goBoard() {
  if (thread.value?.boardCode) router.push(`/board/${thread.value.boardCode}`)
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
  <div class="thread">
    <RuneBackground />
    <SiteNav />

    <main class="th__body">
      <!-- 面包屑 -->
      <nav class="crumb" aria-label="位置">
        <router-link to="/forum">论坛</router-link>
        <span class="crumb__sep">/</span>
        <button type="button" class="crumb__link" @click="goBoard">
          {{ thread?.boardName || '版块' }}
        </button>
        <span class="crumb__sep">/</span>
        <span class="crumb__now">主题</span>
      </nav>

      <div v-if="loading" class="th__skeleton">
        <span class="sk sk--head" />
        <span class="sk sk--line" />
        <span class="sk sk--line" />
        <span class="sk sk--line is-short" />
      </div>

      <section v-else-if="errMsg" class="th__void">
        <AppIcon name="alert" :size="30" />
        <h2>读不到这个主题</h2>
        <p>{{ errMsg }}</p>
        <router-link to="/forum" class="th__void-link">返回论坛</router-link>
      </section>

      <template v-else-if="thread">
        <div class="th__main">
          <div class="th__col">
            <!-- ============ 主楼 ============ -->
            <article class="post">
              <header class="post__head">
                <div class="post__tags">
                  <span v-if="thread.pinned" class="chip chip--pin">镇馆</span>
                  <span v-if="thread.essence" class="chip chip--essence">入藏</span>
                  <span class="chip" :class="'chip--lv' + thread.securityLevel">
                    {{ thread.securityLabel }}
                  </span>
                  <span v-if="thread.replyVisible" class="chip">回复可见</span>
                  <span v-if="thread.mine" class="chip chip--mine">我发布的</span>
                </div>
                <h1 class="post__title">{{ thread.title }}</h1>
                <div class="post__meta">
                  <span class="post__avatar">{{ (thread.authorName || '佚').slice(0, 1) }}</span>
                  <span class="post__author">{{ thread.authorName || '佚名' }}</span>
                  <span class="dot">·</span>
                  <span>{{ fullTime(thread.createdAt) }}</span>
                  <span class="dot">·</span>
                  <span>{{ thread.viewCount }} 次阅读</span>
                  <span class="dot">·</span>
                  <span>{{ thread.replyCount }} 条批注</span>
                </div>
              </header>

              <!-- 正文 / 封存 -->
              <div v-if="hidden" class="sealed">
                <span class="sealed__icon"><AppIcon name="lock" :size="24" /></span>
                <h2 class="sealed__title">
                  {{ detail.replyUnlocked ? '本文已封存' : '正文需要授权' }}
                </h2>
                <p class="sealed__text">{{ detail.hiddenReason }}</p>
                <div class="sealed__ops">
                  <button
                    v-if="!detail.replyUnlocked && detail.canReply"
                    type="button"
                    class="sealed__btn"
                    @click="focusEditor"
                  >
                    写一条批注解锁
                  </button>
                  <button v-else type="button" class="sealed__btn" @click="goMissions">
                    去做馆务任务提升密级
                  </button>
                  <button type="button" class="sealed__link" @click="goBoard">返回版块</button>
                </div>
                <p class="sealed__hint">
                  你的当前可读上限：{{ detail.maxLevel === 4 ? '绝密' : detail.maxLevel === 3 ? '机密' : detail.maxLevel === 2 ? '内部' : '公开' }}
                </p>
              </div>

              <div v-else class="post__content">
                <p v-for="(p, i) in bodyParas" :key="i" class="post__p">{{ p }}</p>
              </div>

              <!-- 主楼操作 -->
              <footer class="post__foot">
                <button
                  type="button"
                  class="act"
                  :class="{ 'is-on': thread.liked }"
                  :disabled="liking"
                  @click="toggleLike"
                >
                  <AppIcon name="sparkle" :size="13" />
                  {{ thread.liked ? '已赞' : '点赞' }} · {{ thread.likeCount }}
                </button>
                <button
                  v-if="detail.canReply"
                  type="button"
                  class="act"
                  @click="focusEditor"
                >
                  <AppIcon name="arrow" :size="13" /> 回复本主题
                </button>
                <span class="post__floor-tag">主楼</span>
              </footer>
            </article>

            <!-- ============ 楼层 ============ -->
            <section class="floors-wrap">
              <header class="floors__head">
                <h2 class="floors__title">
                  <AppIcon name="archive" :size="14" /> 批注
                  <span class="floors__count">{{ replies.length }}</span>
                </h2>
                <span class="floors__hint">按楼层编号排列 · 引用会保留原文摘要</span>
              </header>

              <ul v-if="replies.length" class="floors">
                <li v-for="r in replies" :key="r.id" class="floor">
                  <div class="floor__side">
                    <span class="floor__avatar">{{ (r.authorName || '佚').slice(0, 1) }}</span>
                    <span class="floor__no">{{ r.floorNo }} 楼</span>
                  </div>

                  <div class="floor__main">
                    <header class="floor__head">
                      <span class="floor__author">{{ r.authorName || '佚名' }}</span>
                      <span v-if="r.mine" class="floor__me">我</span>
                      <span class="floor__time" :title="fullTime(r.createdAt)">
                        {{ relativeTime(r.createdAt) }}
                      </span>
                    </header>

                    <blockquote v-if="r.quoteFloor" class="quote">
                      <span class="quote__who">{{ r.quoteAuthor }} · {{ r.quoteFloor }} 楼</span>
                      <p class="quote__text">{{ r.quoteText }}</p>
                    </blockquote>

                    <p class="floor__content">{{ r.content }}</p>

                    <div class="floor__ops">
                      <button
                        v-if="detail.canReply"
                        type="button"
                        class="floor__op"
                        @click="quoteReply(r)"
                      >
                        引用回复
                      </button>
                    </div>
                  </div>
                </li>
              </ul>

              <p v-else class="floors__empty">
                <AppIcon name="info" :size="13" /> 还没有人批注，你可以成为第一个
              </p>
            </section>

            <!-- ============ 回复框 ============ -->
            <section v-if="detail.canReply" class="editor">
              <header class="editor__head">
                <h2 class="editor__title"><AppIcon name="arrow" :size="14" /> 写一条批注</h2>
                <span class="editor__hint">纯文本 · 换行会保留</span>
              </header>

              <div v-if="quote" class="editor__quote">
                <span>
                  引用 {{ quote.authorName }} · {{ quote.floorNo }} 楼
                </span>
                <button type="button" @click="cancelQuote"><AppIcon name="close" :size="11" /></button>
              </div>

              <textarea
                ref="replyBox"
                v-model="replyText"
                class="editor__area"
                rows="5"
                maxlength="2000"
                placeholder="写下你的批注。涉及后三部情节请先标「含剧透」。"
              />

              <footer class="editor__foot">
                <span class="editor__count">{{ replyText.length }} / 2000</span>
                <button
                  type="button"
                  class="editor__submit"
                  :disabled="submitting || replyText.trim().length < 2"
                  @click="submitReply"
                >
                  {{ submitting ? '提交中…' : '提交批注' }}
                </button>
              </footer>
            </section>

            <p v-else-if="detail.replyHint" class="editor__blocked">
              <AppIcon name="lock" :size="13" /> {{ detail.replyHint }}
            </p>
          </div>

          <!-- ============ 侧栏 ============ -->
          <aside class="th__side">
            <section class="side-card">
              <header class="side-card__head"><AppIcon name="panel" :size="14" /> 所在馆区</header>
              <button type="button" class="side-board" @click="goBoard">
                <span class="side-board__name">{{ thread.boardName }}</span>
                <AppIcon name="chevron-right" :size="12" />
              </button>
              <p class="side-note">同一馆区下的其他主题可以在版块页按「最新 / 最热 / 入藏」浏览。</p>
            </section>

            <section class="side-card">
              <header class="side-card__head"><AppIcon name="shield" :size="14" /> 阅读提示</header>
              <ul class="side-list">
                <li>密级与档案室共用一套：公开 / 内部 / 机密 / 绝密。</li>
                <li>「回复可见」的主题，写一条批注后正文立刻解锁。</li>
                <li>发现违规内容请引用该楼并标注「举报」，由教员处理。</li>
              </ul>
            </section>
          </aside>
        </div>
      </template>
    </main>

    <footer class="th__foot">
      <span>以龙档案馆 · 同人兴趣站 · 非商用</span>
      <span class="th__foot-meta">版权归原作者所有 · 仅作学习交流</span>
    </footer>

    <transition name="toast">
      <div v-if="toast" class="toast"><AppIcon name="info" :size="14" /> {{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
.th__body {
  width: 100%;
  max-width: 1080px;
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

.crumb a,
.crumb__link {
  border: 0;
  background: none;
  padding: 0;
  font: inherit;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.2s;
}

.crumb a:hover,
.crumb__link:hover {
  color: var(--gold-lt);
}

.crumb__sep {
  color: var(--gold-dp);
}

.crumb__now {
  color: var(--gold-lt);
}

/* ================= 两栏 ================= */
.th__main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 272px;
  align-items: start;
  gap: 18px;
  margin-top: 16px;
}

.th__col {
  min-width: 0;
}

/* ================= 主楼 ================= */
.post {
  padding: 22px 24px 16px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.8), rgba(14, 11, 23, 0.94));
  box-shadow: 0 0 30px -20px rgba(216, 178, 90, 0.5);
}

.post__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 9px;
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

.chip--mine {
  color: #8ee9b8;
  border-color: rgba(95, 211, 154, 0.38);
}

/* 密级徽标：公开 → 绝密 逐级加红 */
.chip--lv1 {
  color: var(--text-3);
}

.chip--lv2 {
  color: #a8d8a0;
  border-color: rgba(120, 180, 110, 0.4);
}

.chip--lv3 {
  color: #e0a86a;
  border-color: rgba(224, 168, 106, 0.45);
  background: rgba(224, 168, 106, 0.08);
}

.chip--lv4 {
  color: #f58d86;
  border-color: rgba(239, 107, 98, 0.55);
  background: rgba(179, 39, 45, 0.16);
}

.post__title {
  margin: 12px 0 0;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
  line-height: 1.45;
  letter-spacing: 0.06em;
  color: var(--text);
}

.post__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  margin-top: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line-soft);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.post__avatar {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 11px;
  color: #1a1206;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
}

.post__author {
  color: var(--text-2);
  font-size: 11px;
}

.dot {
  color: var(--gold-dp);
  opacity: 0.6;
}

.post__content {
  padding: 18px 0 6px;
}

.post__p {
  margin: 0 0 14px;
  font-size: 14px;
  line-height: 2;
  letter-spacing: 0.03em;
  color: var(--text-2);
  white-space: pre-wrap;
  word-break: break-word;
}

.post__p:first-child::first-letter {
  /* 首字略放大，读起来更像「卷宗正文」；只作用于第一段第一字 */
  font-family: var(--font-serif);
  font-size: 1.35em;
  color: var(--gold-lt);
}

.post__foot {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--line-soft);
}

.act {
  display: inline-flex;
  align-items: center;
  gap: 6px;
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

.act:hover:not(:disabled) {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.08);
}

.act.is-on {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.6);
  background: rgba(216, 178, 90, 0.16);
}

.act:disabled {
  opacity: 0.6;
  cursor: default;
}

.post__floor-tag {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.2em;
  color: var(--text-4);
}

/* ================= 封存态 ================= */
.sealed {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin: 18px 0 6px;
  padding: 34px 22px;
  border: 1px dashed rgba(216, 178, 90, 0.26);
  border-radius: var(--r-md);
  background: repeating-linear-gradient(
    135deg,
    rgba(216, 178, 90, 0.03) 0 10px,
    transparent 10px 20px
  );
  text-align: center;
}

.sealed__icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1px solid rgba(216, 178, 90, 0.34);
  background: rgba(216, 178, 90, 0.08);
  color: var(--gold);
}

.sealed__title {
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.12em;
  color: var(--text);
}

.sealed__text {
  max-width: 420px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--text-3);
}

.sealed__ops {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  flex-wrap: wrap;
  justify-content: center;
}

.sealed__btn {
  padding: 7px 16px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.12);
  color: var(--gold-lt);
  font-size: 11.5px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: background 0.2s;
}

.sealed__btn:hover {
  background: rgba(216, 178, 90, 0.24);
}

.sealed__link {
  border: 0;
  background: none;
  color: var(--text-3);
  font-size: 11.5px;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.sealed__hint {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

/* ================= 楼层 ================= */
.floors-wrap {
  margin-top: 18px;
}

.floors__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 4px 11px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.26);
}

.floors__title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--text);
}

.floors__title svg {
  color: var(--gold-dp);
}

.floors__count {
  padding: 1px 8px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.35);
  background: rgba(216, 178, 90, 0.08);
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--gold-lt);
}

.floors__hint {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.floors {
  margin: 0;
  padding: 0;
  list-style: none;
}

.floor {
  display: flex;
  gap: 12px;
  padding: 16px 4px;
  border-bottom: 1px solid var(--line-soft);
}

.floor__side {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.floor__avatar {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: rgba(216, 178, 90, 0.06);
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--gold-lt);
}

.floor__no {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  color: var(--text-4);
  white-space: nowrap;
}

.floor__main {
  flex: 1;
  min-width: 0;
}

.floor__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.floor__author {
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: var(--text);
}

.floor__me {
  padding: 0 6px;
  border-radius: 3px;
  border: 1px solid rgba(95, 211, 154, 0.36);
  font-family: var(--font-mono);
  font-size: 8.5px;
  color: #8ee9b8;
}

.floor__time {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.06em;
  color: var(--text-4);
}

.quote {
  margin: 9px 0 0;
  padding: 8px 12px;
  border-left: 2px solid rgba(216, 178, 90, 0.45);
  border-radius: 0 6px 6px 0;
  background: rgba(216, 178, 90, 0.05);
}

.quote__who {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--gold-dp);
}

.quote__text {
  margin-top: 3px;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--text-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.floor__content {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.9;
  letter-spacing: 0.03em;
  color: var(--text-2);
  white-space: pre-wrap;
  word-break: break-word;
}

.floor__ops {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.floor:hover .floor__ops,
.floor:focus-within .floor__ops {
  opacity: 1;
}

.floor__op {
  padding: 0;
  border: 0;
  background: none;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
  cursor: pointer;
  transition: color 0.2s;
}

.floor__op:hover {
  color: var(--gold-lt);
}

.floors__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 30px 16px;
  border: 1px dashed rgba(255, 255, 255, 0.09);
  border-radius: var(--r-md);
  margin-top: 14px;
  font-size: 11.5px;
  color: var(--text-4);
}

/* ================= 编辑器 ================= */
.editor {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.78), rgba(14, 11, 23, 0.92));
}

.editor__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.editor__title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--text);
}

.editor__title svg {
  color: var(--gold-dp);
}

.editor__hint {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.editor__quote {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  padding: 6px 10px;
  border-left: 2px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.06);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--gold-dp);
}

.editor__quote button {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.07);
  color: var(--text-3);
  cursor: pointer;
}

.editor__area {
  width: 100%;
  padding: 11px 13px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: rgba(0, 0, 0, 0.3);
  color: var(--text);
  font-family: var(--font-sans);
  font-size: 13px;
  line-height: 1.85;
  resize: vertical;
  outline: none;
  transition: border-color 0.22s, box-shadow 0.22s;
}

.editor__area::placeholder {
  color: var(--text-4);
}

.editor__area:focus {
  border-color: rgba(216, 178, 90, 0.5);
  box-shadow: 0 0 20px -12px var(--gold-glow);
}

.editor__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.editor__count {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.editor__submit {
  padding: 7px 20px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.55);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.24), rgba(216, 178, 90, 0.08));
  color: var(--gold-lt);
  font-size: 12px;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: background 0.2s, transform 0.18s;
}

.editor__submit:hover:not(:disabled) {
  background: rgba(216, 178, 90, 0.34);
  transform: translateY(-1px);
}

.editor__submit:disabled {
  opacity: 0.5;
  cursor: default;
}

.editor__blocked {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 14px 16px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
  border-radius: var(--r-md);
  font-size: 11.5px;
  color: var(--text-4);
}

/* ================= 侧栏 ================= */
.th__side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 92px;
}

.side-card {
  padding: 14px 16px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.76), rgba(14, 11, 23, 0.9));
}

.side-card__head {
  display: flex;
  align-items: center;
  gap: 7px;
  padding-bottom: 9px;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.24);
  font-family: var(--font-serif);
  font-size: 12.5px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
}

.side-board {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 11px;
  border-radius: 8px;
  border: 1px solid rgba(216, 178, 90, 0.28);
  background: rgba(216, 178, 90, 0.06);
  color: var(--gold-lt);
  font-size: 12px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: background 0.2s;
}

.side-board:hover {
  background: rgba(216, 178, 90, 0.14);
}

.side-note {
  margin-top: 9px;
  font-size: 10.5px;
  line-height: 1.7;
  color: var(--text-4);
}

.side-list {
  margin: 0;
  padding-left: 15px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  font-size: 10.5px;
  line-height: 1.7;
  color: var(--text-3);
}

.side-list li::marker {
  color: var(--gold-dp);
  font-size: 9px;
}

/* ================= 骨架 / 空态 ================= */
.th__skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
  padding: 22px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
}

.sk {
  display: block;
  height: 14px;
  border-radius: 6px;
  background: linear-gradient(
    100deg,
    rgba(255, 255, 255, 0.03) 20%,
    rgba(216, 178, 90, 0.08) 42%,
    rgba(255, 255, 255, 0.03) 64%
  );
  background-size: 220% 100%;
  animation: sk 1.4s ease-in-out infinite;
}

.sk--head {
  height: 28px;
  width: 62%;
  margin-bottom: 10px;
}

.sk--line.is-short {
  width: 48%;
}

@keyframes sk {
  from {
    background-position: 120% 0;
  }
  to {
    background-position: -40% 0;
  }
}

.th__void {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 22px;
  padding: 56px 24px;
  border: 1px dashed rgba(216, 178, 90, 0.24);
  border-radius: var(--r-lg);
  text-align: center;
  color: var(--gold-dp);
}

.th__void h2 {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0.12em;
  color: var(--text);
}

.th__void p {
  font-size: 12px;
  color: var(--text-3);
}

.th__void-link {
  margin-top: 4px;
  padding: 7px 18px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.1);
  color: var(--gold-lt);
  font-size: 11.5px;
}

/* ================= 页脚 / 提示 ================= */
.th__foot {
  width: 100%;
  max-width: 1080px;
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
@media (max-width: 960px) {
  .th__main {
    grid-template-columns: minmax(0, 1fr);
  }

  .th__side {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .th__body {
    padding: 18px 16px 40px;
  }

  .post {
    padding: 18px 16px 14px;
  }

  .post__title {
    font-size: 20px;
  }

  .th__side {
    grid-template-columns: minmax(0, 1fr);
  }

  .floor__ops {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
