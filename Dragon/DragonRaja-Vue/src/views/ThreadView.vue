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
import { getUser } from '@/utils/auth'
import { fullTime, paragraphs, relativeTime } from '@/utils/forumTime'

const route = useRoute()
const router = useRouter()

/**
 * 游客态：论坛公开可读，但回复需要登录。
 * 后端会在 canReply=false 时给出 replyHint，单靠一行提示是死路 ——
 * 用户此刻的意愿最高，必须直接给一个能点的登录入口。
 */
const logged = computed(() => !!getUser())

const id = computed(() => String(route.params.id || ''))

const detail = ref(null)
const loading = ref(true)
const errMsg = ref('')

const thread = computed(() => detail.value?.thread || null)

/**
 * 楼层分页状态。
 * 详情接口只返回第 1 页（后端默认 20 层/页），翻页走 /threads/{id}/replies；
 * 楼层一多，一次性全量返回会让整页渲染明显卡顿。
 */
const replyList = ref([])
const replyPage = ref(1)
const replyPages = ref(1)
const replyTotal = ref(0)
const pageLoading = ref(false)
/** 模板里仍用 replies 这个语义名，避免改动整片楼层模板 */
const replies = computed(() => replyList.value)

/** 把详情响应里的分页字段同步到本地状态（首次加载 / 回帖后都用它） */
function syncReplyPage(d) {
  replyList.value = d?.replies || []
  replyPage.value = Number(d?.replyPage || 1)
  replyPages.value = Math.max(1, Number(d?.replyPages || 1))
  replyTotal.value = Number(d?.replyTotal || 0)
}

/** 翻到指定页；越界自动夹紧，避免点到不存在的页码 */
async function goPage(p) {
  if (!thread.value || pageLoading.value) return
  const target = Math.min(Math.max(1, Number(p) || 1), replyPages.value)
  if (target === replyPage.value && replyList.value.length) return
  pageLoading.value = true
  try {
    const res = await forumApi.replies(thread.value.id, target)
    replyList.value = res?.list || []
    replyPage.value = Number(res?.page || target)
    replyPages.value = Math.max(1, Number(res?.pages || 1))
    replyTotal.value = Number(res?.total || 0)
    nextTick(() => {
      document.querySelector('.floors-wrap')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  } catch (e) {
    showToast(e?.message || '楼层加载失败')
  } finally {
    pageLoading.value = false
  }
}
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
    syncReplyPage(detail.value)
  } catch (e) {
    errMsg.value = e?.message || '主题加载失败'
    detail.value = null
    syncReplyPage(null)
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

/* ---------------- 收藏 ---------------- */
const collecting = ref(false)
async function toggleCollect() {
  if (!thread.value || collecting.value) return
  if (!logged.value) {
    showToast('登录后才能收藏')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  collecting.value = true
  try {
    const res = await forumApi.collect(thread.value.id)
    thread.value.collected = !!res?.collected
    showToast(res?.collected ? '已加入收藏，可在「我的」里查看' : '已取消收藏')
  } catch (e) {
    showToast(e?.message || '操作失败')
  } finally {
    collecting.value = false
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
    showToast('回复至少写两个字')
    return
  }
  submitting.value = true
  try {
    const res = await forumApi.reply(thread.value.id, {
      content: text,
      quoteId: quote.value?.id || 0
    })
    detail.value = res
    // 后端回帖后返回的是「最后一页」，刚发的那层就在页尾
    syncReplyPage(res)
    replyText.value = ''
    quote.value = null
    showToast('回复已提交')
    nextTick(() => {
      const list = document.querySelector('.floors')
      const last = list?.lastElementChild
      if (last) last.scrollIntoView({ behavior: 'smooth', block: 'center' })
      else document.querySelector('.floors-wrap')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  } catch (e) {
    showToast(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

/* ---------------- 编辑 / 删除 ---------------- */

const editOpen = ref(false)
const editSaving = ref(false)
const editTarget = ref(null) // { type:'thread'|'reply', id }
const editTitle = ref('')
const editContent = ref('')

function openEdit(type, obj) {
  if (!thread.value) return
  editTarget.value = { type, id: type === 'thread' ? thread.value.id : obj.id }
  editTitle.value = type === 'thread' ? thread.value.title : ''
  // 用原始正文而不是 bodyParas：后者是分好段的展示数据，回填会把空行规整掉
  editContent.value = type === 'thread' ? (detail.value?.content || '') : obj.content
  editOpen.value = true
}

function closeEdit() {
  if (editSaving.value) return
  editOpen.value = false
}

async function submitEdit() {
  if (editSaving.value || !editTarget.value || !thread.value) return
  const content = editContent.value.trim()
  const t = editTarget.value
  if (t.type === 'thread' && editTitle.value.trim().length < 4) {
    showToast('标题至少 4 个字')
    return
  }
  if (t.type === 'thread' && content.length < 10) {
    showToast('正文至少 10 个字')
    return
  }
  if (t.type === 'reply' && content.length < 2) {
    showToast('回复至少 2 个字')
    return
  }

  editSaving.value = true
  try {
    if (t.type === 'thread') {
      const res = await forumApi.updateThread(t.id, {
        title: editTitle.value.trim(),
        content
      })
      detail.value = res
      syncReplyPage(res)
      showToast('修改已保存')
    } else {
      const updated = await forumApi.updateReply(thread.value.id, t.id, { content })
      // 就地替换这一层，不整页重载 —— 保住当前楼层页码和滚动位置
      replyList.value = replyList.value.map((r) =>
        r.id === t.id ? { ...r, content: updated?.content ?? content, edited: true } : r
      )
      showToast('回复已更新')
    }
    editOpen.value = false
  } catch (e) {
    showToast(e?.message || '保存失败')
  } finally {
    editSaving.value = false
  }
}

async function removeThread() {
  if (!thread.value) return
  if (!window.confirm('确认删除这条主题？删除后前台不再显示，楼层也会一并不可见。')) return
  try {
    await forumApi.deleteThread(thread.value.id)
    showToast('主题已删除，正在返回版块')
    setTimeout(() => goBoard(), 600)
  } catch (e) {
    showToast(e?.message || '删除失败')
  }
}

async function removeReply(r) {
  if (!thread.value) return
  if (!window.confirm(`确认删除 ${r.floorNo} 楼？此操作不可撤销。`)) return
  try {
    await forumApi.deleteReply(thread.value.id, r.id)
    replyList.value = replyList.value.filter((x) => x.id !== r.id)
    replyTotal.value = Math.max(0, replyTotal.value - 1)
    // 删掉最后一层后当前页可能已经空了，回退一页
    if (!replyList.value.length && replyPage.value > 1) {
      replyPage.value -= 1
      await goPage(replyPage.value)
    }
    showToast('回复已删除')
  } catch (e) {
    showToast(e?.message || '删除失败')
  }
}

/* ---------------- 举报 ---------------- */

/**
 * 举报原因与后端 ReportServiceImpl.REASON_LABELS 一一对应：
 * 前端只传 key，中文文案两边各自维护，避免把展示文案写进协议里。
 */
const REASONS = [
  { key: 'copyright', label: '侵犯版权 / 未授权转载', desc: '搬运全文、盗图、无授权翻译' },
  { key: 'spoiler', label: '剧透未标注', desc: '标题或首段直接写后几部情节' },
  { key: 'spam', label: '广告 / 引流', desc: '二维码、外链、拉群、代购' },
  { key: 'abuse', label: '人身攻击 / 引战', desc: '针对读者而非作品的攻击' },
  { key: 'other', label: '其他', desc: '请在补充说明里写清楚' }
]

const reportOpen = ref(false)
const reportTarget = ref(null)
const reportReason = ref('copyright')
const reportDetail = ref('')
const reporting = ref(false)

function openReport(type, obj) {
  if (!logged.value) {
    showToast('登录后才能举报')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!thread.value) return
  reportTarget.value = type === 'thread'
    ? { type, id: thread.value.id, title: thread.value.title }
    : { type, id: obj.id, floorNo: obj.floorNo, authorName: obj.authorName }
  reportReason.value = 'copyright'
  reportDetail.value = ''
  reportOpen.value = true
}

function closeReport() {
  if (reporting.value) return
  reportOpen.value = false
}

async function submitReport() {
  if (reporting.value || !reportTarget.value || !thread.value) return
  reporting.value = true
  try {
    await forumApi.report({
      targetType: reportTarget.value.type,
      targetId: reportTarget.value.id,
      threadId: thread.value.id,
      reason: reportReason.value,
      detail: reportDetail.value.trim()
    })
    reportOpen.value = false
    showToast('已收到举报，我们会尽快核实')
  } catch (e) {
    showToast(e?.message || '举报提交失败')
  } finally {
    reporting.value = false
  }
}

/* ---------------- 跳转 ---------------- */
function goBoard() {
  if (thread.value?.boardCode) router.push(`/board/${thread.value.boardCode}`)
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
                  <span v-if="thread.pinned" class="chip chip--pin">置顶</span>
                  <span v-if="thread.essence" class="chip chip--essence">精华</span>
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
                  <span v-if="thread.edited" class="edited-tag" title="作者修改过标题或正文">已编辑</span>
                  <span class="dot">·</span>
                  <span>{{ thread.viewCount }} 次阅读</span>
                  <span class="dot">·</span>
                  <span>{{ thread.replyCount }} 条回复</span>
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
                    写一条回复解锁
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
                  type="button"
                  class="act"
                  :class="{ 'is-on': thread.collected }"
                  :disabled="collecting"
                  @click="toggleCollect"
                >
                  <AppIcon name="archive" :size="13" />
                  {{ thread.collected ? '已收藏' : '收藏' }}
                </button>
                <button
                  v-if="detail.canReply"
                  type="button"
                  class="act"
                  @click="focusEditor"
                >
                  <AppIcon name="arrow" :size="13" /> 回复本主题
                </button>
                <!-- 只有作者本人能看到编辑 / 删除：这是权限，不是装饰 -->
                <button v-if="thread?.mine" type="button" class="act" @click="openEdit('thread')">
                  <AppIcon name="edit" :size="13" /> 编辑
                </button>
                <button
                  v-if="thread?.mine || detail.viewerStaff"
                  type="button"
                  class="act act--danger"
                  @click="removeThread"
                >
                  <AppIcon name="trash" :size="13" /> 删除
                </button>
                <button type="button" class="act act--report" @click="openReport('thread', thread)">
                  <AppIcon name="alert" :size="13" /> 举报
                </button>
                <span class="post__floor-tag">主楼</span>
              </footer>
            </article>

            <!-- ============ 楼层 ============ -->
            <section class="floors-wrap">
              <header class="floors__head">
                <h2 class="floors__title">
                  <AppIcon name="archive" :size="14" /> 回复
                  <span class="floors__count">{{ replyTotal }}</span>
                </h2>
                <span class="floors__hint">
                  <template v-if="replyPages > 1">第 {{ replyPage }} / {{ replyPages }} 页 · </template>
                  按楼层编号排列 · 引用会保留原文摘要
                </span>
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
                      <span v-if="r.edited" class="edited-tag">已编辑</span>
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
                      <button v-if="r.mine" type="button" class="floor__op" @click="openEdit('reply', r)">
                        编辑
                      </button>
                      <!-- 版务可以删别人的楼层（后端同样会校验），但改不了别人的话 -->
                      <button
                        v-if="r.mine || detail.viewerStaff"
                        type="button"
                        class="floor__op floor__op--danger"
                        @click="removeReply(r)"
                      >
                        删除
                      </button>
                      <button
                        type="button"
                        class="floor__op floor__op--report"
                        @click="openReport('reply', r)"
                      >
                        举报
                      </button>
                    </div>
                  </div>
                </li>
              </ul>

              <p v-else class="floors__empty">
                <AppIcon name="info" :size="13" /> 还没有人回复，你可以成为第一个
              </p>

              <!-- 楼层翻页：只在多页时出现，单页帖子不占位置 -->
              <nav v-if="replyPages > 1" class="pager" aria-label="楼层翻页">
                <button
                  type="button"
                  class="pager__btn"
                  :disabled="replyPage <= 1 || pageLoading"
                  @click="goPage(replyPage - 1)"
                >
                  上一页
                </button>
                <span class="pager__now">{{ replyPage }} / {{ replyPages }}</span>
                <button
                  type="button"
                  class="pager__btn"
                  :disabled="replyPage >= replyPages || pageLoading"
                  @click="goPage(replyPage + 1)"
                >
                  下一页
                </button>
                <button
                  type="button"
                  class="pager__btn pager__btn--tail"
                  :disabled="replyPage >= replyPages || pageLoading"
                  @click="goPage(replyPages)"
                >
                  看最新
                </button>
              </nav>
            </section>

            <!-- ============ 回复框 ============ -->
            <section v-if="detail.canReply" class="editor">
              <header class="editor__head">
                <h2 class="editor__title"><AppIcon name="arrow" :size="14" /> 写一条回复</h2>
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
                placeholder="写下你的回复。涉及后三部情节请先标「含剧透」。"
              />

              <footer class="editor__foot">
                <span class="editor__count">{{ replyText.length }} / 2000</span>
                <button
                  type="button"
                  class="editor__submit"
                  :disabled="submitting || replyText.trim().length < 2"
                  @click="submitReply"
                >
                  {{ submitting ? '提交中…' : '提交回复' }}
                </button>
              </footer>
            </section>

            <p v-else-if="detail.replyHint" class="editor__blocked">
              <AppIcon name="lock" :size="13" /> {{ detail.replyHint }}
              <router-link
                v-if="!logged"
                class="editor__blocked-link"
                :to="{ path: '/login', query: { redirect: route.fullPath } }"
              >
                去登录 <AppIcon name="arrow" :size="11" />
              </router-link>
            </p>
          </div>

          <!-- ============ 侧栏 ============ -->
          <aside class="th__side">
            <section class="side-card">
              <header class="side-card__head"><AppIcon name="panel" :size="14" /> 所在版块</header>
              <button type="button" class="side-board" @click="goBoard">
                <span class="side-board__name">{{ thread.boardName }}</span>
                <AppIcon name="chevron-right" :size="12" />
              </button>
              <p class="side-note">同一版块下的其他主题可以在版块页按「最新 / 最热 / 精华」浏览。</p>
            </section>

            <section class="side-card">
              <header class="side-card__head"><AppIcon name="shield" :size="14" /> 阅读提示</header>
              <ul class="side-list">
                <li>密级与档案室共用一套：公开 / 内部 / 机密 / 绝密。</li>
                <li>「回复可见」的主题，写一条回复后正文立刻解锁。</li>
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

    <!-- 编辑弹窗：主题改标题+正文，楼层只改正文 -->
    <transition name="toast">
      <div v-if="editOpen" class="rmask" role="dialog" aria-modal="true" aria-labelledby="edit-title" @click.self="closeEdit">
        <div class="rbox rbox--edit">
          <header class="rbox__head">
            <h2 id="edit-title" class="rbox__title">
              <AppIcon name="edit" :size="15" /> {{ editTarget?.type === 'thread' ? '编辑主题' : '编辑回复' }}
            </h2>
            <button type="button" class="rbox__x" aria-label="关闭" @click="closeEdit">
              <AppIcon name="close" :size="13" />
            </button>
          </header>

          <label v-if="editTarget?.type === 'thread'" class="rbox__field rbox__field--inline">
            <span>标题</span>
            <input v-model="editTitle" type="text" maxlength="80" />
          </label>

          <label class="rbox__field">
            <span>{{ editTarget?.type === 'thread' ? '正文' : '回复内容' }}</span>
            <textarea
              v-model="editContent"
              :rows="editTarget?.type === 'thread' ? 14 : 6"
              :maxlength="editTarget?.type === 'thread' ? 8000 : 2000"
              placeholder="写点什么…"
            />
          </label>

          <footer class="rbox__foot">
            <span class="rbox__note">保存后该内容会标记「已编辑」，其他人能看到改动痕迹。</span>
            <button type="button" class="rbox__btn" @click="closeEdit">取消</button>
            <button type="button" class="rbox__btn rbox__btn--primary" :disabled="editSaving" @click="submitEdit">
              {{ editSaving ? '保存中…' : '保存' }}
            </button>
          </footer>
        </div>
      </div>
    </transition>

    <!-- 举报弹窗：法律风险的正规出口，不要求用户去帖子里「手打举报」 -->
    <transition name="toast">
      <div v-if="reportOpen" class="rmask" role="dialog" aria-modal="true" aria-labelledby="report-title" @click.self="closeReport">
        <div class="rbox">
          <header class="rbox__head">
            <h2 id="report-title" class="rbox__title">
              <AppIcon name="alert" :size="15" /> 举报{{ reportTarget?.type === 'thread' ? '主题' : '回复' }}
            </h2>
            <button type="button" class="rbox__x" aria-label="关闭" @click="closeReport">
              <AppIcon name="close" :size="13" />
            </button>
          </header>

          <p class="rbox__target">
            <template v-if="reportTarget?.type === 'thread'">{{ reportTarget?.title }}</template>
            <template v-else>{{ reportTarget?.floorNo }} 楼 · {{ reportTarget?.authorName }}</template>
          </p>

          <fieldset class="rbox__reasons">
            <legend class="rbox__legend">选择原因</legend>
            <label v-for="r in REASONS" :key="r.key" class="rbox__reason" :class="{ 'is-on': reportReason === r.key }">
              <input v-model="reportReason" type="radio" :value="r.key" name="report-reason" />
              <span class="rbox__reason-label">{{ r.label }}</span>
              <span class="rbox__reason-desc">{{ r.desc }}</span>
            </label>
          </fieldset>

          <label class="rbox__field">
            <span>补充说明（选填）</span>
            <textarea
              v-model="reportDetail"
              rows="3"
              maxlength="500"
              placeholder="在哪里、具体是哪一句，写清楚能帮我们更快处理"
            />
          </label>

          <footer class="rbox__foot">
            <span class="rbox__note">恶意举报会被记录，请如实填写</span>
            <button type="button" class="rbox__btn" @click="closeReport">取消</button>
            <button type="button" class="rbox__btn rbox__btn--primary" :disabled="reporting" @click="submitReport">
              {{ reporting ? '提交中…' : '提交举报' }}
            </button>
          </footer>
        </div>
      </div>
    </transition>

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

/* ================= 楼层翻页 ================= */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}

.pager__btn {
  padding: 7px 16px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.32);
  background: transparent;
  color: var(--gold-lt);
  font-size: 11.5px;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, opacity 0.2s;
}

.pager__btn:hover:not(:disabled) {
  background: rgba(216, 178, 90, 0.1);
  border-color: rgba(216, 178, 90, 0.6);
}

.pager__btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* 「看最新」比翻页弱一档，避免和下一页抢注意力 */
.pager__btn--tail {
  border-color: rgba(255, 255, 255, 0.12);
  color: var(--text-3);
}

.pager__now {
  min-width: 52px;
  text-align: center;
  font-size: 11.5px;
  color: var(--text-4);
  font-variant-numeric: tabular-nums;
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

/* 游客态：提示右侧补一个可点的登录入口，别让用户停在死路上 */
.editor__blocked-link {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: 1px solid rgba(216, 178, 90, 0.4);
  border-radius: 999px;
  color: var(--gold-lt);
  font-size: 11.5px;
  text-decoration: none;
  transition: background 0.2s, border-color 0.2s;
}

.editor__blocked-link:hover {
  background: rgba(216, 178, 90, 0.1);
  border-color: rgba(216, 178, 90, 0.7);
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

/* ================= 举报 ================= */

/* 举报按钮比常规操作弱一档：不该比「点赞」更抢眼，但必须找得到 */
.act--report,
.floor__op--report {
  color: var(--text-4);
}

.act--report:hover,
.floor__op--report:hover {
  color: #d98a8a;
}

.rmask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(6, 5, 12, 0.72);
  backdrop-filter: blur(3px);
}

.rbox {
  width: min(520px, 100%);
  max-height: min(86dvh, 720px);
  overflow-y: auto;
  padding: 20px 22px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.98), rgba(14, 11, 23, 0.99));
  box-shadow: 0 24px 60px -20px rgba(0, 0, 0, 0.8);
}

.rbox__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.rbox__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--gold-lt);
}

.rbox__x {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 1px solid var(--line-soft);
  border-radius: 50%;
  background: transparent;
  color: var(--text-3);
  cursor: pointer;
}

.rbox__x:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
}

.rbox__target {
  margin: 10px 0 16px;
  padding: 9px 12px;
  border-left: 2px solid rgba(216, 178, 90, 0.5);
  border-radius: 0 var(--r-sm) var(--r-sm) 0;
  background: rgba(255, 255, 255, 0.03);
  font-size: 11.5px;
  color: var(--text-3);
  line-height: 1.6;
}

.rbox__reasons {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0 0 14px;
  padding: 0;
  border: 0;
}

.rbox__legend {
  padding: 0 0 6px;
  font-size: 11px;
  color: var(--text-4);
  letter-spacing: 0.04em;
}

.rbox__reason {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
  padding: 9px 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--r-sm);
  cursor: pointer;
  transition: border-color 0.18s, background 0.18s;
}

.rbox__reason:hover {
  border-color: rgba(216, 178, 90, 0.35);
}

.rbox__reason.is-on {
  border-color: rgba(216, 178, 90, 0.62);
  background: rgba(216, 178, 90, 0.08);
}

/* 原生单选框保留可聚焦能力，只缩小视觉存在感 */
.rbox__reason input {
  accent-color: var(--gold);
  margin: 0;
}

.rbox__reason-label {
  font-size: 12px;
  color: var(--text-2);
}

.rbox__reason-desc {
  font-size: 10.5px;
  color: var(--text-4);
}

.rbox__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 11px;
  color: var(--text-4);
}

.rbox__field textarea {
  width: 100%;
  resize: vertical;
  padding: 10px 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--r-sm);
  background: rgba(0, 0, 0, 0.25);
  color: var(--text-1);
  font-family: inherit;
  font-size: 12px;
  line-height: 1.7;
}

.rbox__field textarea:focus {
  outline: none;
  border-color: rgba(216, 178, 90, 0.55);
}

.rbox__foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.rbox__note {
  flex: 1;
  font-size: 10.5px;
  color: var(--text-4);
  line-height: 1.5;
}

.rbox__btn {
  padding: 8px 16px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: transparent;
  color: var(--text-3);
  font-family: inherit;
  font-size: 11.5px;
  cursor: pointer;
}

.rbox__btn:hover {
  color: var(--text-1);
  border-color: rgba(255, 255, 255, 0.24);
}

.rbox__btn--primary {
  border-color: rgba(216, 178, 90, 0.55);
  background: rgba(216, 178, 90, 0.12);
  color: var(--gold-lt);
}

.rbox__btn--primary:hover:not(:disabled) {
  background: rgba(216, 178, 90, 0.2);
  border-color: rgba(216, 178, 90, 0.8);
}

.rbox__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ================= 编辑 ================= */

/* 编辑框比举报框宽：正文编辑需要行宽，太窄会不断折行 */
.rbox--edit {
  width: min(660px, 100%);
}

.rbox__field--inline {
  margin-bottom: 12px;
}

.rbox__field--inline input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--r-sm);
  background: rgba(0, 0, 0, 0.25);
  color: var(--text-1);
  font-family: var(--font-serif);
  font-size: 13px;
}

.rbox__field--inline input:focus {
  outline: none;
  border-color: rgba(216, 178, 90, 0.55);
}

/* 已编辑：低对比度，是记录而不是警告 */
.edited-tag {
  padding: 1px 6px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  font-size: 10px;
  color: var(--text-4);
  white-space: nowrap;
}

.act--danger,
.floor__op--danger {
  color: var(--text-4);
}

.act--danger:hover,
.floor__op--danger:hover {
  color: #d98a8a;
}

</style>
