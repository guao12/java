<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthCard from '@/components/AuthCard.vue'
import FormField from '@/components/FormField.vue'
import GlowButton from '@/components/GlowButton.vue'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import { authApi } from '@/api/auth'
import {
  clearHistoryAccounts,
  currentScopeName,
  historyAccounts,
  lastAccount,
  pushHistoryAccount,
  rememberAccount,
  removeHistoryAccount,
  saveSession
} from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const form = reactive({ account: '', password: '', remember: true })

/** 后端连通状态：null 检测中，true 已连接，false 未连接 */
const backendOnline = ref(null)
const checking = ref(false)

const stageEl = ref(null)
const formEl = ref(null)
const errors = reactive({ account: '', password: '' })
const touched = reactive({ account: false, password: false })
const loading = ref(false)
const notice = ref(null) // { type: 'err' | 'ok' | 'info', text }

/* ---- 互动状态 ---- */
const shaking = ref(false) // 认证失败抖动
const scanning = ref(false) // 认证通过扫描过场
const capsOn = ref(false) // 大写锁定
const welcome = ref('') // 认证通过后的打字机欢迎语

/* ---- 连续失败保护：5 次失败锁定 30 秒 ---- */
const MAX_ATTEMPTS = 5
const LOCK_SECONDS = 30
const attempts = ref(0)
const cooldown = ref(0)
let cooldownTimer = null

/* ---- 本机缓存 ---- */
const lastLoginAt = ref(Number(localStorage.getItem('dr:last-login') || 0))
const history = ref(historyAccounts())

/**
 * 当前身份槽。默认槽不提示；一旦本窗口被分到独立槽（第二个账号登录时自动分配），
 * 就显示出来，让用户知道这个窗口和另一个窗口的账号互不影响。
 */
const scopeName = ref(currentScopeName())

/* ---- 卡片 3D 跟随（尊重「减少动态效果」与触屏） ---- */
const tilt = reactive({ rx: 0, ry: 0, mx: 50, my: 0, on: false })
const reduceMotion =
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

const stageStyle = computed(() => ({
  '--rx': `${tilt.rx}deg`,
  '--ry': `${tilt.ry}deg`,
  '--mx': `${tilt.mx}%`,
  '--my': `${tilt.my}%`
}))

function onMove(e) {
  if (reduceMotion || e.pointerType === 'touch' || !stageEl.value) return
  const r = stageEl.value.getBoundingClientRect()
  const px = (e.clientX - r.left) / r.width
  const py = (e.clientY - r.top) / r.height
  tilt.ry = (px - 0.5) * 9 // 左右倾斜
  tilt.rx = (0.5 - py) * 7 // 上下倾斜
  tilt.mx = px * 100
  tilt.my = py * 100
  tilt.on = true
}

function onLeave() {
  tilt.rx = 0
  tilt.ry = 0
  tilt.on = false
}

/* ---- 大写锁定检测 ---- */
function syncCaps(e) {
  if (typeof e.getModifierState === 'function') capsOn.value = e.getModifierState('CapsLock')
}

/* ---- 编号自动规整：去空格 + 自动大写（学号适用；中文姓名不受影响） ---- */
watch(
  () => form.account,
  (v) => {
    const clean = v.replace(/\s+/g, '').toUpperCase()
    if (clean !== v) form.account = clean
  }
)

function validateAccount() {
  const v = form.account.trim()
  if (!v) return '请输入学号或姓名'
  if (v.length > 20) return '长度不超过 20 个字符'
  return ''
}

function validatePassword() {
  if (!form.password) return '请输入言灵密钥'
  if (form.password.length < 6) return '密钥至少 6 位'
  return ''
}

function check(field) {
  touched[field] = true
  errors[field] = field === 'account' ? validateAccount() : validatePassword()
}

const accountValid = computed(() => touched.account && !errors.account && !!form.account)
const passwordValid = computed(() => touched.password && !errors.password && !!form.password)

const canSubmit = computed(() => !!form.account.trim() && !!form.password)
const locked = computed(() => cooldown.value > 0)

/** 密钥「强度」已移除：登录时评估强度没有决策意义（该在注册 / 改密时做），
 *  6 格能量条 + 进度条 + 文字标签占了约 60px 却不改变任何行为。 */

/** 后端信号格已移除（细节见模板 card-note）。探测逻辑保留：仅用于「连不上」时兜底提示。 */

/** 上次登录的相对时间 */
const lastLoginText = computed(() => {
  if (!lastLoginAt.value) return ''
  const diff = Date.now() - lastLoginAt.value
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m} 分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} 小时前`
  const d = Math.floor(h / 24)
  return d < 30 ? `${d} 天前` : new Date(lastLoginAt.value).toLocaleDateString()
})

function shake() {
  shaking.value = false
  window.requestAnimationFrame(() => {
    shaking.value = true
    window.setTimeout(() => (shaking.value = false), 560)
  })
}

/* ---- 打字机欢迎语 ---- */
let typeTimer = null
function typeWriter(text) {
  window.clearInterval(typeTimer)
  welcome.value = ''
  let i = 0
  typeTimer = window.setInterval(() => {
    i += 1
    welcome.value = text.slice(0, i)
    if (i >= text.length) window.clearInterval(typeTimer)
  }, 42)
}

function startCooldown() {
  cooldown.value = LOCK_SECONDS
  stopCooldown()
  cooldownTimer = window.setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) {
      stopCooldown()
      attempts.value = 0
    }
  }, 1000)
}

function stopCooldown() {
  if (cooldownTimer) {
    window.clearInterval(cooldownTimer)
    cooldownTimer = null
  }
}

/* ---- 本机历史身份卡 ---- */
function useHistory(acc) {
  form.account = acc
  touched.account = true
  errors.account = validateAccount()
  notice.value = null
  focusPassword()
}

function dropHistory(acc) {
  history.value = removeHistoryAccount(acc)
  if (form.account === acc) {
    form.account = ''
    touched.account = false
    errors.account = ''
  }
}

function focusPassword() {
  nextTick(() => {
    const el = formEl.value?.querySelector('input[type="password"]')
    el?.focus()
  })
}

/**
 * 跳转目标带上身份槽：?as=<scope>
 * 默认槽不写，保持 URL 干净；独立槽写进去，用户把这个网址存成书签，
 * 下次直接打开还是这个账号 —— 两个账号各有一个固定网址，互不干扰。
 */
function withScope(target, scope) {
  if (!scope || scope === 'default') return target
  const cut = target.indexOf('?')
  const path = cut >= 0 ? target.slice(0, cut) : target
  const params = new URLSearchParams(cut >= 0 ? target.slice(cut + 1) : '')
  params.set('as', scope)
  return `${path}?${params.toString()}`
}

function forget() {
  localStorage.removeItem('dr:last-account')
  localStorage.removeItem('dr:last-login')
  history.value = clearHistoryAccounts()
  lastLoginAt.value = 0
  form.account = ''
  touched.account = false
  errors.account = ''
  notice.value = { type: 'info', text: '已清除本机缓存的身份卡与登录记录。' }
}

/** 只判断「连得上 / 连不上」，不再计时长（信号格已移除，耗时无处展示） */
async function probeBackend() {
  checking.value = true
  backendOnline.value = null
  try {
    await authApi.dictionaries()
    backendOnline.value = true
  } catch {
    backendOnline.value = false
  } finally {
    checking.value = false
  }
}

async function onSubmit() {
  if (loading.value || locked.value) return
  check('account')
  check('password')
  notice.value = null
  welcome.value = ''
  if (errors.account || errors.password) {
    notice.value = { type: 'err', text: '认证信息不完整，请检查被标记的字段。' }
    shake()
    return
  }

  loading.value = true
  try {
    // 调用后端 /api/auth/login，由 Spring Boot + MyBatis-Plus 校验并签发 JWT
    const data = await authApi.login({
      account: form.account.trim(),
      password: form.password,
      remember: form.remember
    })
    // 若是本窗口的第二个账号，saveSession 会自动换到新的身份槽，不覆盖原账号
    const scope = saveSession({ token: data.token, user: data.user }, form.remember)
    scopeName.value = scope
    rememberAccount(form.remember ? form.account.trim() : '')
    if (form.remember) history.value = pushHistoryAccount(form.account.trim())
    localStorage.setItem('dr:last-login', String(Date.now()))
    attempts.value = 0
    stopCooldown()
    scanning.value = true
    notice.value = null
    typeWriter(`认证通过 · 欢迎归来，${data.user.codename}。密钥校验通过，正在接入学院内网…`)
    window.setTimeout(() => {
      router.push(withScope(String(route.query.redirect || '/forum'), scope))
    }, 1600)
  } catch (e) {
    loading.value = false
    attempts.value += 1
    shake()
    if (attempts.value >= MAX_ATTEMPTS) {
      startCooldown()
      notice.value = {
        type: 'err',
        text: `连续 ${MAX_ATTEMPTS} 次认证失败，龙文安全锁已启动，请 ${LOCK_SECONDS} 秒后再试。`
      }
    } else {
      notice.value = {
        type: 'err',
        text: `${e.message}（剩余尝试 ${MAX_ATTEMPTS - attempts.value} 次）`
      }
    }
  }
}

/** Ctrl / ⌘ + Enter 快捷提交 */
function onShortcut(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') onSubmit()
}

onMounted(async () => {
  form.account = lastAccount()
  if (form.account) touched.account = true
  probeBackend()
  window.addEventListener('keydown', syncCaps)
  window.addEventListener('keyup', syncCaps)
  window.addEventListener('keydown', onShortcut)
  // 自动聚焦第一个还没填的字段
  await nextTick()
  const inputs = [...(formEl.value?.querySelectorAll('input') || [])].filter(
    (i) => i.type !== 'checkbox'
  )
  inputs.find((i) => !i.value)?.focus()
})

onBeforeUnmount(() => {
  stopCooldown()
  window.clearInterval(typeTimer)
  window.removeEventListener('keydown', syncCaps)
  window.removeEventListener('keyup', syncCaps)
  window.removeEventListener('keydown', onShortcut)
})
</script>

<template>
  <div
    ref="stageEl"
    class="login-stage"
    :class="{ 'is-hover': tilt.on }"
    :style="stageStyle"
    @pointermove="onMove"
    @pointerleave="onLeave"
  >
    <div class="login-shell" :class="{ 'is-shake': shaking }">
      <!-- 返回门户：从 /portal 进来后必须有回路，否则是死胡同 -->
      <router-link to="/portal" class="back-link">
        <AppIcon name="chevron-left" :size="14" />
        返回门户
      </router-link>

      <!--
        引子区放在卡片外。这是本次改版最决定性的一步：
        原先把「徽记 + 标题 + 副题」塞在 AuthCard 里，卡片被撑到 800px+，
        1440×900 / 1366×768 下整页必然滚动。移出后卡片只剩表单本身，整页不再滚动。
      -->
      <header class="intro">
        <span class="intro__emblem" aria-hidden="true"><DragonEmblem /></span>
        <h1 class="intro__title">身份认证</h1>
        <p class="intro__sub">出示你的身份卡与言灵密钥，进入学院内部网络</p>
      </header>

      <div class="login-tilt">
        <AuthCard>
          <template #tabs>
            <router-link to="/login" active-class="active">登录</router-link>
            <router-link to="/register" active-class="active">注册</router-link>
          </template>

          <form ref="formEl" class="form" novalidate @submit.prevent="onSubmit">
            <FormField
              v-model="form.account"
              label="学号 / 姓名"
              icon="id"
              placeholder="例：CC202600004 或 孤傲"
              autocomplete="username"
              :error="touched.account ? errors.account : ''"
              :valid="accountValid"
              @blur="check('account')"
              @enter="focusPassword"
            />

            <!-- 本机历史身份卡：一个是「填入」按钮，删除是另一个独立按钮（不再嵌套） -->
            <div v-if="history.length" class="chips">
              <span class="chips__label">本机</span>
              <div class="chips__list">
                <span
                  v-for="acc in history"
                  :key="acc"
                  class="chip"
                  :class="{ 'is-active': acc === form.account }"
                >
                  <button type="button" class="chip__pick" @click="useHistory(acc)">
                    {{ acc }}
                  </button>
                  <button
                    type="button"
                    class="chip__x"
                    :aria-label="`删除 ${acc}`"
                    @click="dropHistory(acc)"
                  >
                    <AppIcon name="close" :size="10" />
                  </button>
                </span>
              </div>
            </div>

            <FormField
              v-model="form.password"
              label="言灵密钥"
              icon="lock"
              type="password"
              placeholder="请输入登录密钥"
              autocomplete="current-password"
              :error="touched.password ? errors.password : ''"
              :valid="passwordValid"
              @blur="check('password')"
              @enter="onSubmit"
            />

            <!-- 大写锁定提示 -->
            <p v-if="capsOn" class="caps-hint">
              <AppIcon name="alert" :size="13" />大写锁定已开启，密钥区分大小写
            </p>

            <div class="form-row">
              <label class="check">
                <input v-model="form.remember" type="checkbox" />
                <span class="check__box" />
                <span>记住身份卡</span>
              </label>
              <button v-if="form.account || history.length" class="link-tiny" type="button" @click="forget">
                清除记录
              </button>
              <router-link class="link-gold" to="/forgot">忘记密钥？</router-link>
            </div>

            <!-- 认证通过：打字机欢迎语 -->
            <div v-if="welcome" class="welcome">
              <AppIcon name="check" :size="15" />
              <span>{{ welcome }}<b class="caret" /></span>
            </div>

            <div v-if="notice" class="alert" :class="`alert--${notice.type}`">
              <AppIcon
                :name="notice.type === 'ok' ? 'check' : notice.type === 'err' ? 'alert' : 'info'"
                :size="15"
              />
              <span>{{ notice.text }}</span>
            </div>

            <GlowButton
              :loading="loading"
              :disabled="!canSubmit || locked"
              :loading-text="scanning ? '正在接入…' : '校验密钥中'"
            >
              <template v-if="locked">安全锁定 {{ cooldown }}s</template>
              <template v-else>进入学院</template>
            </GlowButton>

            <p class="shortcut-hint">
              上次登录：<b v-if="lastLoginText">{{ lastLoginText }}</b>
              <span v-else>首次登录</span>
              <span class="shortcut-hint__sep">·</span>
              <kbd>Ctrl</kbd>/<kbd>⌘</kbd> + <kbd>Enter</kbd> 快速认证
            </p>

            <!--
              只在「后端不可达」时出现。原先是常驻一行「已连接（192.168.68.1:8082）· 12ms」
              + 三格信号强度：内网地址属于拓扑信息不该摆在登录页，而 27 个用户里没有一个
              会因为「后端 12ms」而改变登录动作。连不上才是需要提示的状态。
            -->
            <p v-if="backendOnline === false" class="card-note is-offline">
              <AppIcon name="alert" :size="13" />
              后端通道未连接，请先启动服务
              <button class="link-tiny" type="button" :disabled="checking" @click="probeBackend">
                {{ checking ? '检测中…' : '重新检测' }}
              </button>
            </p>

            <!-- 独立身份槽提示：只在第二个账号登录后出现 -->
            <p v-if="scopeName !== 'default'" class="slot-note">
              <AppIcon name="shield" :size="12" />
              <span>
                本窗口使用独立身份槽 <b>{{ scopeName }}</b>，与另一个窗口的账号互不影响
              </span>
            </p>
          </form>

          <template #foot>
            还没有身份卡？<router-link class="link-gold" to="/register">前往新生登记</router-link>
          </template>
        </AuthCard>
      </div>

      <!-- 认证通过：扫描过场 -->
      <div v-if="scanning" class="scan" aria-hidden="true">
        <span class="scan__line" />
        <span class="scan__grid" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ------------------------------------------------ 返回门户 */
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin: 0 0 14px -6px;
  padding: 4px 8px 4px 2px;
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--text-4);
  transition: color 0.24s, transform 0.24s;
}
.back-link:hover {
  color: var(--ice);
  transform: translateX(-2px);
}
.back-link:hover svg {
  color: var(--ice);
}

/* ------------------------------------------------ 引子区（卡片外）
   结构色统一走冷银蓝，与落地页 / 门户保持同一条视觉链路；
   金色只保留在悬停、焦点、成功态，作为「龙族 = 鎏金」的识别锚点。 */
.intro {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 20px;
}
.intro__emblem {
  display: block;
  width: 56px;
  height: 56px;
  margin-bottom: 14px;
  filter: drop-shadow(0 0 18px rgba(184, 194, 212, 0.32));
  animation: intro-spin 46s linear infinite;
}
@keyframes intro-spin {
  to {
    transform: rotate(360deg);
  }
}
.intro__title {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.22em;
  /* 字间距会在末尾多留一格，左移半格让它视觉居中 */
  text-indent: 0.22em;
  color: var(--silver-bright, #dce2ee);
  text-shadow: 0 0 28px rgba(184, 194, 212, 0.26);
  margin: 0 0 8px;
}
.intro__sub {
  margin: 0;
  font-size: 12.5px;
  letter-spacing: 0.05em;
  color: var(--text-3);
  max-width: 34em;
}

/* ------------------------------------------------ 舞台：光晕跟随 + 3D 倾斜 */
.login-stage {
  position: relative;
  width: 100%;
  max-width: 452px;
  margin: 0 auto;
}
.login-stage::before {
  content: '';
  position: absolute;
  inset: -26px;
  border-radius: calc(var(--r-lg) + 14px);
  background: radial-gradient(
    300px circle at var(--mx, 50%) var(--my, 50%),
    rgba(184, 194, 212, 0.13),
    transparent 62%
  );
  opacity: 0;
  transition: opacity 0.35s ease;
  pointer-events: none;
  mix-blend-mode: screen;
  z-index: 3;
}
.login-stage.is-hover::before {
  opacity: 1;
}

.login-shell {
  position: relative;
  perspective: 1100px;
}
.login-shell.is-shake {
  animation: login-shake 0.52s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}

.login-tilt {
  transform: rotateX(var(--rx, 0deg)) rotateY(var(--ry, 0deg));
  transform-style: preserve-3d;
  transition: transform 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
  will-change: transform;
}

@keyframes login-shake {
  10%,
  90% {
    transform: translateX(-3px);
  }
  20%,
  80% {
    transform: translateX(5px);
  }
  30%,
  50%,
  70% {
    transform: translateX(-9px);
  }
  40%,
  60% {
    transform: translateX(9px);
  }
}

/* ------------------------------------------------ 认证通过扫描过场 */
.scan {
  position: absolute;
  inset: 0;
  border-radius: var(--r-lg);
  overflow: hidden;
  pointer-events: none;
  z-index: 4;
}
.scan__line {
  position: absolute;
  left: 0;
  right: 0;
  height: 96px;
  background: linear-gradient(180deg, transparent, rgba(216, 178, 90, 0.28), transparent);
  animation: scan-sweep 1.6s ease-in-out forwards;
}
.scan__grid {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    rgba(216, 178, 90, 0.07) 0 1px,
    transparent 1px 5px
  );
  animation: scan-grid 1.6s ease-in-out forwards;
}
@keyframes scan-sweep {
  0% {
    transform: translateY(-110%);
    opacity: 0;
  }
  25% {
    opacity: 1;
  }
  100% {
    transform: translateY(420%);
    opacity: 0;
  }
}
@keyframes scan-grid {
  0% {
    opacity: 0;
  }
  40% {
    opacity: 1;
  }
  100% {
    opacity: 0;
  }
}

/* ------------------------------------------------ 本机历史身份卡
   每个 chip 现在是「一个 span 容器 + 两个并列 button」，
   不再像原先那样把 span[role=button] 嵌在 button 里（HTML 不合法，读屏会念成两个按钮）。 */
.chips {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: -4px 0 -2px;
}
.chips__label {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}
.chips__list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}
.chip {
  display: inline-flex;
  align-items: stretch;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.6);
  overflow: hidden;
  transition: border-color 0.22s, background 0.22s;
}
.chip:hover {
  border-color: rgba(184, 194, 212, 0.45);
}
.chip.is-active {
  border-color: rgba(184, 194, 212, 0.62);
  background: rgba(184, 194, 212, 0.1);
}
.chip__pick {
  padding: 4px 2px 4px 10px;
  border: 0;
  background: none;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.22s;
}
.chip:hover .chip__pick,
.chip.is-active .chip__pick {
  color: var(--silver-bright, #dce2ee);
}
.chip__x {
  display: grid;
  place-items: center;
  width: 22px;
  border: 0;
  border-left: 1px solid var(--line-soft);
  background: none;
  color: var(--text-4);
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}
.chip__x:hover {
  color: var(--err);
  background: rgba(179, 39, 45, 0.18);
}

/* ------------------------------------------------ 页脚小链接 */
.link-tiny {
  border: 0;
  background: none;
  padding: 0;
  font: inherit;
  font-size: 11.5px;
  color: var(--text-3);
  cursor: pointer;
  text-decoration: underline dotted;
  text-underline-offset: 3px;
  transition: color 0.2s ease;
}
.link-tiny:hover:not(:disabled) {
  color: var(--silver-bright, #dce2ee);
}
.link-tiny:disabled {
  cursor: default;
  opacity: 0.6;
}

/* ------------------------------------------------ 大写锁定 */
.caps-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--warn);
  animation: caps-in 0.28s ease both;
}
@keyframes caps-in {
  from {
    opacity: 0;
    transform: translateY(-3px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ------------------------------------------------ 打字机欢迎语 */
.welcome {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 11px 14px;
  border-radius: var(--r-sm);
  font-size: 12.5px;
  line-height: 1.6;
  color: #d6ffe9;
  background: rgba(95, 211, 154, 0.13);
  border: 1px solid rgba(95, 211, 154, 0.4);
}
.welcome svg {
  flex: 0 0 auto;
  margin-top: 3px;
}
.caret {
  display: inline-block;
  width: 6px;
  height: 13px;
  margin-left: 2px;
  vertical-align: -2px;
  background: var(--ok);
  animation: caret-blink 0.9s steps(2, start) infinite;
}
@keyframes caret-blink {
  50% {
    opacity: 0;
  }
}

/* ------------------------------------------------ 快捷键提示 */
.shortcut-hint {
  margin: 10px 0 0;
  text-align: center;
  font-size: 11px;
  color: var(--text-4);
}
.shortcut-hint b {
  color: var(--text-2);
  font-weight: 600;
}
.shortcut-hint__sep {
  margin: 0 6px;
  opacity: 0.55;
}
.shortcut-hint kbd {
  display: inline-block;
  min-width: 18px;
  padding: 1px 5px;
  margin: 0 1px;
  border: 1px solid var(--line-soft);
  border-bottom-width: 2px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.04);
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: var(--text-2);
}

/* ------------------------------------------------ 独立身份槽提示 */
.slot-note {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 8px 0 0;
  padding: 8px 10px;
  border-radius: var(--r-sm);
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--text-2);
  background: rgba(127, 212, 255, 0.06);
  border: 1px solid rgba(127, 212, 255, 0.2);
}
.slot-note svg {
  flex: 0 0 auto;
  margin-top: 2px;
  color: var(--ice);
}
.slot-note b {
  font-family: var(--font-mono);
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--ice);
}

/* ------------------------------------------------ 后端不可达提示 */
.card-note {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  padding: 8px 11px;
  border-radius: var(--r-sm);
  font-size: 11.5px;
  letter-spacing: 0.02em;
  color: var(--err);
  background: rgba(179, 39, 45, 0.1);
  border: 1px solid rgba(239, 107, 98, 0.28);
}
.card-note svg {
  flex: 0 0 auto;
}

/* 动效敏感用户：关掉位移类动画 */
@media (prefers-reduced-motion: reduce) {
  .login-tilt {
    transform: none !important;
  }
  .login-shell.is-shake {
    animation: none;
  }
  .scan__line,
  .scan__grid {
    animation-duration: 0.01s;
  }
  .caret {
    animation: none;
  }
  .intro__emblem {
    animation: none;
  }
}

@media (max-width: 560px) {
  .intro {
    margin-bottom: 16px;
  }
  .intro__emblem {
    width: 46px;
    height: 46px;
    margin-bottom: 11px;
  }
  .intro__title {
    font-size: 22px;
    letter-spacing: 0.16em;
    text-indent: 0.16em;
  }
  .intro__sub {
    font-size: 12px;
  }
  .chips {
    flex-wrap: wrap;
  }
}
</style>
