<script setup>
/**
 * 发帖页（/forum/new）
 *
 * 三个刻意的限制：
 *  1. 只能选「自己有发帖权限」的馆区 —— 不让用户写完 500 字才被后端拒绝
 *  2. 密级只能选到自己的可读上限 —— 发一篇自己都看不见的帖子没有意义
 *  3. 标题 / 正文字数在前端先校验一遍，错误立刻显示在字段下方
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import { forumApi } from '@/api/forum'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const boards = ref([])
const me = ref({})

const boardCode = ref('')
const title = ref('')
const content = ref('')
const securityLevel = ref(1)
const replyVisible = ref(false)

const submitting = ref(false)
const errMsg = ref('')

const LEVELS = [
  { value: 1, label: '公开', hint: '所有在册读者可见' },
  { value: 2, label: '内部', hint: '在校生及以上可见' },
  { value: 3, label: '机密', hint: '执行部及以上可见' },
  { value: 4, label: '绝密', hint: '仅教员可见' }
]

/** 只能发布到自己能读到的密级，否则发完自己也打不开 */
const levelOptions = computed(() => {
  const max = me.value.maxLevel || 1
  return LEVELS.filter((l) => l.value <= max)
})

const currentBoard = computed(() => boards.value.find((b) => b.code === boardCode.value) || null)

const titleLen = computed(() => title.value.trim().length)
const contentLen = computed(() => content.value.trim().length)

const canSubmit = computed(
  () =>
    !!boardCode.value &&
    titleLen.value >= 4 &&
    titleLen.value <= 80 &&
    contentLen.value >= 10 &&
    contentLen.value <= 8000 &&
    !submitting.value
)

async function boot() {
  loading.value = true
  errMsg.value = ''
  try {
    const ov = await forumApi.overview()
    me.value = ov?.me || {}
    boards.value = (ov?.boards || []).filter((b) => b.canPost)
    const preset = String(route.query.board || '')
    const hit = boards.value.find((b) => b.code === preset)
    boardCode.value = hit ? hit.code : boards.value[0]?.code || ''
  } catch (e) {
    errMsg.value = e?.message || '加载版块失败'
  } finally {
    loading.value = false
  }
}

onMounted(boot)
onBeforeUnmount(() => clearTimeout(toastTimer))

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  errMsg.value = ''
  try {
    const res = await forumApi.create({
      boardCode: boardCode.value,
      title: title.value.trim(),
      content: content.value.trim(),
      securityLevel: securityLevel.value,
      replyVisible: replyVisible.value
    })
    const newId = res?.thread?.id
    if (newId) {
      router.replace(`/thread/${newId}`)
    } else {
      router.replace('/forum')
    }
  } catch (e) {
    errMsg.value = e?.message || '发布失败'
    showToast(errMsg.value)
  } finally {
    submitting.value = false
  }
}

function cancel() {
  if (title.value.trim() || content.value.trim()) {
    if (!window.confirm('放弃这次编辑？已填写的内容不会保存。')) return
  }
  router.back()
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
  <div class="editor-page">
    <RuneBackground />
    <SiteNav />

    <main class="ep__body">
      <nav class="crumb" aria-label="位置">
        <router-link to="/forum">论坛</router-link>
        <span class="crumb__sep">/</span>
        <span class="crumb__now">发起主题</span>
      </nav>

      <header class="ep__hero">
        <DragonEmblem class="ep__mark" />
        <div>
          <h1 class="ep__title">
            <span class="sheen-gold">发起主题</span>
            <span class="ep__title-en">NEW THREAD</span>
          </h1>
          <p class="ep__sub">
            正文按纯文本归档，空行分段；发布后可在主题页继续编辑批注。
            <template v-if="me.codename">
              当前署名：{{ me.codename }}（{{ me.identityName }}，可读至「{{ me.maxLevel === 4 ? '绝密' : me.maxLevel === 3 ? '机密' : me.maxLevel === 2 ? '内部' : '公开' }}」）
            </template>
          </p>
        </div>
      </header>

      <div v-if="loading" class="ep__loading">
        <span class="sk" />
        <span class="sk sk--short" />
      </div>

      <section v-else-if="!boards.length" class="ep__void">
        <AppIcon name="lock" :size="28" />
        <h2>暂无可发帖的馆区</h2>
        <p>你的在校身份还不满足任何馆区的发帖要求。公告区仅教员可发，执行部内网需要权限点 8。</p>
        <button type="button" class="ep__void-link" @click="router.push('/forum')">返回论坛</button>
      </section>

      <form v-else class="ep__grid" @submit.prevent="submit">
        <!-- 主编辑区 -->
        <div class="ep__col">
          <section class="field">
            <label class="field__label" for="post-title">
              标题 <span class="field__req">必填</span>
              <span class="field__count" :class="{ 'is-bad': titleLen > 80 }">{{ titleLen }} / 80</span>
            </label>
            <input
              id="post-title"
              v-model="title"
              class="input"
              type="text"
              maxlength="80"
              placeholder="一句话说清这帖在聊什么（4–80 字）"
            />
            <p v-if="titleLen > 0 && titleLen < 4" class="field__err">
              标题太短了，至少 4 个字
            </p>
          </section>

          <section class="field">
            <label class="field__label" for="post-body">
              正文 <span class="field__req">必填</span>
              <span class="field__count" :class="{ 'is-bad': contentLen > 8000 }">
                {{ contentLen }} / 8000
              </span>
            </label>
            <textarea
              id="post-body"
              v-model="content"
              class="textarea"
              rows="16"
              maxlength="8000"
              placeholder="正文至少 10 个字。空行分段，粘贴时请去掉原文全文。"
            />
            <p v-if="contentLen > 0 && contentLen < 10" class="field__err">
              再写几个字（至少 10 个字）
            </p>
          </section>

          <p v-if="errMsg" class="ep__err">
            <AppIcon name="alert" :size="14" /> {{ errMsg }}
          </p>
        </div>

        <!-- 发布设置 -->
        <aside class="ep__side">
          <section class="panel">
            <header class="panel__head"><AppIcon name="panel" :size="13" /> 馆区</header>
            <div class="board-pick">
              <label
                v-for="b in boards"
                :key="b.code"
                class="board-opt"
                :class="{ 'is-on': boardCode === b.code }"
              >
                <input v-model="boardCode" type="radio" :value="b.code" class="sr-only" />
                <span class="board-opt__icon"><AppIcon :name="b.icon" :size="13" /></span>
                <span class="board-opt__text">
                  <b>{{ b.name }}</b>
                  <em>{{ b.readOnly ? '仅教员可发' : b.threadCount + ' 主题' }}</em>
                </span>
                <AppIcon v-if="boardCode === b.code" name="check" :size="12" class="board-opt__ok" />
              </label>
            </div>
          </section>

          <section class="panel">
            <header class="panel__head"><AppIcon name="shield" :size="13" /> 阅读密级</header>
            <div class="lv-pick">
              <label
                v-for="l in levelOptions"
                :key="l.value"
                class="lv-opt"
                :class="{ 'is-on': securityLevel === l.value }"
              >
                <input v-model.number="securityLevel" type="radio" :value="l.value" class="sr-only" />
                <span class="lv-opt__dot" :data-lv="l.value" />
                <span class="lv-opt__text">
                  <b>{{ l.label }}</b>
                  <em>{{ l.hint }}</em>
                </span>
              </label>
            </div>
            <p class="panel__note">不能高于你自己的可读上限，否则发布后你本人也打不开。</p>
          </section>

          <section class="panel">
            <header class="panel__head"><AppIcon name="eye" :size="13" /> 附加限制</header>
            <label class="switch">
              <input v-model="replyVisible" type="checkbox" class="sr-only" />
              <span class="switch__track" :class="{ 'is-on': replyVisible }"><i /></span>
              <span class="switch__text">
                <b>回复后可见</b>
                <em>正文仅对写过批注的人展示，适合讨论型主题</em>
              </span>
            </label>
          </section>

          <div class="ep__ops">
            <button type="submit" class="btn-primary" :disabled="!canSubmit">
              {{ submitting ? '发布中…' : '发布主题' }}
            </button>
            <button type="button" class="btn-ghost" @click="cancel">取消</button>
          </div>

          <p class="ep__notice">
            发布即表示同意：不发布全文资源、不发布付费章节内容；转载须附授权说明。
          </p>
        </aside>
      </form>
    </main>

    <footer class="ep__foot">
      <span>以龙档案馆 · 同人兴趣站 · 非商用</span>
      <span class="ep__foot-meta">版权归原作者所有 · 仅作学习交流</span>
    </footer>

    <transition name="toast">
      <div v-if="toast" class="toast"><AppIcon name="info" :size="14" /> {{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
.ep__body {
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

/* ================= 头部 ================= */
.ep__hero {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 16px 0 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.24);
}

.ep__mark {
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  filter: drop-shadow(0 0 12px rgba(216, 178, 90, 0.45));
  animation: spin 60s linear infinite;
}

.ep__title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  font-family: var(--font-serif);
  font-size: 23px;
  letter-spacing: 0.12em;
  color: var(--text);
}

.ep__title-en {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.24em;
  color: var(--gold-dp);
}

.ep__sub {
  margin-top: 5px;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--text-3);
}

/* ================= 布局 ================= */
.ep__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  align-items: start;
  gap: 18px;
}

.ep__col {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ================= 字段 ================= */
.field {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.field__label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.12em;
  color: var(--text-2);
}

.field__req {
  padding: 1px 6px;
  border-radius: 3px;
  border: 1px solid rgba(216, 178, 90, 0.32);
  font-family: var(--font-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--gold-dp);
}

.field__count {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.field__count.is-bad {
  color: var(--err);
}

.field__err {
  font-size: 11px;
  color: var(--err);
}

.input,
.textarea {
  width: 100%;
  padding: 11px 13px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: rgba(0, 0, 0, 0.3);
  color: var(--text);
  font-family: var(--font-sans);
  font-size: 13.5px;
  line-height: 1.85;
  outline: none;
  transition: border-color 0.22s, box-shadow 0.22s;
}

.input::placeholder,
.textarea::placeholder {
  color: var(--text-4);
}

.input:focus,
.textarea:focus {
  border-color: rgba(216, 178, 90, 0.5);
  box-shadow: 0 0 22px -14px var(--gold-glow);
}

.textarea {
  resize: vertical;
  min-height: 260px;
}

/* ================= 侧栏 ================= */
.ep__side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: 92px;
}

.panel {
  padding: 13px 14px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.78), rgba(14, 11, 23, 0.92));
}

.panel__head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-bottom: 9px;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.24);
  font-family: var(--font-serif);
  font-size: 12px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
}

.panel__note {
  margin-top: 9px;
  font-size: 10px;
  line-height: 1.65;
  color: var(--text-4);
}

/* 版块单选 */
.board-pick {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.board-opt {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.board-opt:hover {
  background: rgba(255, 255, 255, 0.03);
}

.board-opt.is-on {
  border-color: rgba(216, 178, 90, 0.42);
  background: rgba(216, 178, 90, 0.1);
}

.board-opt__icon {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid rgba(216, 178, 90, 0.28);
  color: var(--gold-lt);
}

.board-opt__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.board-opt__text b {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: var(--text);
}

.board-opt__text em {
  font-family: var(--font-mono);
  font-style: normal;
  font-size: 9px;
  letter-spacing: 0.06em;
  color: var(--text-4);
}

.board-opt__ok {
  flex: 0 0 auto;
  color: var(--gold-lt);
}

/* 密级单选 */
.lv-pick {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.lv-opt {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 9px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.lv-opt:hover {
  background: rgba(255, 255, 255, 0.03);
}

.lv-opt.is-on {
  border-color: rgba(216, 178, 90, 0.4);
  background: rgba(216, 178, 90, 0.08);
}

.lv-opt__dot {
  flex: 0 0 auto;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--text-4);
}

.lv-opt__dot[data-lv='1'] {
  background: #9aa2b6;
}
.lv-opt__dot[data-lv='2'] {
  background: #7ec27a;
  box-shadow: 0 0 8px rgba(126, 194, 122, 0.7);
}
.lv-opt__dot[data-lv='3'] {
  background: #e0a86a;
  box-shadow: 0 0 8px rgba(224, 168, 106, 0.7);
}
.lv-opt__dot[data-lv='4'] {
  background: #ef6b62;
  box-shadow: 0 0 8px rgba(239, 107, 98, 0.8);
}

.lv-opt__text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.lv-opt__text b {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: var(--text);
}

.lv-opt__text em {
  font-style: normal;
  font-size: 9.5px;
  color: var(--text-4);
}

/* 开关 */
.switch {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
}

.switch__track {
  position: relative;
  flex: 0 0 auto;
  width: 34px;
  height: 18px;
  margin-top: 2px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(0, 0, 0, 0.35);
  transition: background 0.22s, border-color 0.22s;
}

.switch__track i {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--text-4);
  transition: transform 0.22s, background 0.22s;
}

.switch__track.is-on {
  border-color: rgba(216, 178, 90, 0.55);
  background: rgba(216, 178, 90, 0.22);
}

.switch__track.is-on i {
  transform: translateX(16px);
  background: var(--gold-lt);
}

.switch__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.switch__text b {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: var(--text);
}

.switch__text em {
  font-style: normal;
  font-size: 9.5px;
  line-height: 1.6;
  color: var(--text-4);
}

/* ================= 操作 ================= */
.ep__ops {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-primary {
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.6);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.3), rgba(216, 178, 90, 0.1));
  box-shadow: 0 0 22px -14px var(--gold-glow);
  color: var(--gold-lt);
  font-size: 13px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: background 0.22s, transform 0.18s;
}

.btn-primary:hover:not(:disabled) {
  background: rgba(216, 178, 90, 0.4);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.45;
  cursor: default;
}

.btn-ghost {
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: none;
  color: var(--text-3);
  font-size: 12px;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.btn-ghost:hover {
  color: var(--text);
  border-color: rgba(255, 255, 255, 0.18);
}

.ep__notice {
  padding: 0 4px;
  font-size: 9.5px;
  line-height: 1.7;
  color: var(--text-4);
  text-align: center;
}

.ep__err {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 13px;
  border-radius: var(--r-md);
  border: 1px solid rgba(239, 107, 98, 0.4);
  background: rgba(179, 39, 45, 0.12);
  font-size: 11.5px;
  color: #f0a39c;
}

/* ================= 空态 / 加载 ================= */
.ep__loading {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
}

.sk {
  display: block;
  height: 42px;
  border-radius: 8px;
  background: linear-gradient(
    100deg,
    rgba(255, 255, 255, 0.03) 20%,
    rgba(216, 178, 90, 0.08) 42%,
    rgba(255, 255, 255, 0.03) 64%
  );
  background-size: 220% 100%;
  animation: sk 1.4s ease-in-out infinite;
}

.sk--short {
  width: 60%;
}

@keyframes sk {
  from {
    background-position: 120% 0;
  }
  to {
    background-position: -40% 0;
  }
}

.ep__void {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 52px 24px;
  border: 1px dashed rgba(216, 178, 90, 0.24);
  border-radius: var(--r-lg);
  text-align: center;
  color: var(--gold-dp);
}

.ep__void h2 {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0.12em;
  color: var(--text);
}

.ep__void p {
  max-width: 440px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--text-3);
}

.ep__void-link {
  padding: 7px 18px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.1);
  color: var(--gold-lt);
  font-size: 11.5px;
  cursor: pointer;
}

/* ================= 页脚 / 提示 ================= */
.ep__foot {
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

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
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
@media (max-width: 940px) {
  .ep__grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .ep__side {
    position: static;
  }
}

@media (max-width: 620px) {
  .ep__body {
    padding: 18px 16px 40px;
  }

  .ep__title {
    font-size: 20px;
  }

  .textarea {
    min-height: 200px;
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
