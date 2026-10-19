<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthCard from '@/components/AuthCard.vue'
import FormField from '@/components/FormField.vue'
import GlowButton from '@/components/GlowButton.vue'
import AppIcon from '@/components/AppIcon.vue'
import IdCardPreview from '@/components/IdCardPreview.vue'
import { authApi } from '@/api/auth'
import {
  clearHistoryAccounts,
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
/** 后端响应耗时（ms），用于信号强度显示 */
const ping = ref(0)

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
/** 编号 → 上次登录成功的行动代号（仅本机展示用） */
const knownName = ref('')

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

const CODE_RE = /^[A-Za-z0-9_-]{4,20}$/

/* ---- 编号自动规整：去空格 + 自动大写 ---- */
watch(
  () => form.account,
  (v) => {
    const clean = v.replace(/\s+/g, '').toUpperCase()
    if (clean !== v) form.account = clean
    knownName.value = clean ? localStorage.getItem(`dr:name-of-${clean}`) || '' : ''
  }
)

function validateAccount() {
  const v = form.account.trim()
  if (!v) return '请输入账号'
  return CODE_RE.test(v) ? '' : '账号应为 4–20 位字母、数字或下划线'
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

/** 密钥「血统共鸣度」：长度 + 字符种类，纯前端趣味反馈 */
const resonance = computed(() => {
  const v = form.password
  if (!v) return { pct: 0, level: 'idle', text: '等待注入密钥' }
  let score = 0
  if (v.length >= 6) score += 30
  if (v.length >= 10) score += 15
  if (v.length >= 14) score += 10
  if (/[a-z]/.test(v) && /[A-Z]/.test(v)) score += 15
  if (/\d/.test(v)) score += 15
  if (/[^A-Za-z0-9]/.test(v)) score += 15
  const pct = Math.min(100, score)
  const level = pct >= 85 ? 'high' : pct >= 60 ? 'mid' : pct >= 30 ? 'low' : 'weak'
  const text = { high: '龙文共振', mid: '共鸣稳定', low: '共鸣微弱', weak: '几乎无共鸣' }[level]
  return { pct, level, text }
})

/** 密钥能量格：6 格随长度点亮 */
const slots = computed(() => Array.from({ length: 6 }, (_, i) => i < Math.min(6, form.password.length)))

/** 身份卡状态 */
const cardState = computed(() => (scanning.value ? 'ok' : form.account ? 'scan' : 'idle'))
const cardIdentity = computed(() => (scanning.value ? '认证通过 · 权限已解锁' : '身份待验证'))
const cardGrade = computed(() => {
  const pct = resonance.value.pct
  if (!pct) return '—'
  if (pct >= 85) return 'S'
  if (pct >= 60) return 'A'
  if (pct >= 30) return 'B'
  return 'C'
})
const cardStatus = computed(() => {
  if (scanning.value) return '认证通过 · 正在接入'
  if (loading.value) return '正在校验龙文密钥…'
  if (form.account && form.password) return '密钥已注入，可提交认证'
  if (form.account) return '已刷卡，等待密钥'
  return '等待刷卡'
})

/** 信号强度：按 ping 折算为 0–3 格 */
const signal = computed(() => {
  if (backendOnline.value !== true) return 0
  if (ping.value <= 120) return 3
  if (ping.value <= 400) return 2
  return 1
})

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

function fillDemo() {
  form.account = 'CC20260001'
  form.password = 'Dragon@2026'
  errors.account = ''
  errors.password = ''
  touched.account = true
  touched.password = true
  notice.value = { type: 'info', text: '已载入演示身份卡（密钥 Dragon@2026），点击「进入学院」即可登录。' }
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

async function probeBackend() {
  checking.value = true
  backendOnline.value = null
  const t0 = performance.now()
  try {
    await authApi.dictionaries()
    backendOnline.value = true
    ping.value = Math.round(performance.now() - t0)
  } catch {
    backendOnline.value = false
    ping.value = 0
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
    saveSession({ token: data.token, user: data.user }, form.remember)
    rememberAccount(form.remember ? form.account.trim() : '')
    if (form.remember) history.value = pushHistoryAccount(form.account.trim())
    if (data.user?.codename) {
      localStorage.setItem(`dr:name-of-${form.account.trim()}`, data.user.codename)
    }
    localStorage.setItem('dr:last-login', String(Date.now()))
    attempts.value = 0
    stopCooldown()
    scanning.value = true
    notice.value = null
    typeWriter(`认证通过 · 欢迎归来，${data.user.codename}。血统扫描正常，正在接入学院内网…`)
    window.setTimeout(() => {
      router.push(String(route.query.redirect || '/home'))
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
  if (form.account) {
    touched.account = true
    knownName.value = localStorage.getItem(`dr:name-of-${form.account}`) || ''
  }
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
      <div class="login-tilt">
        <AuthCard
          badge="Cassell College · Access"
          title="身份认证"
          subtitle="出示你的身份卡与言灵密钥，进入学院内部网络"
        >
          <template #tabs>
            <router-link to="/login" active-class="active">登录</router-link>
            <router-link to="/register" active-class="active">注册</router-link>
          </template>

          <form ref="formEl" class="form" novalidate @submit.prevent="onSubmit">
            <!-- 身份卡实时预览 -->
            <IdCardPreview
              :name="knownName"
              :account="form.account"
              :identity="cardIdentity"
              :spirit="form.password ? '密钥已注入' : '言灵待激活'"
              :grade="cardGrade"
              :state="cardState"
              :status="cardStatus"
              :serial="form.password.length ? String(form.password.length) : ''"
            />

            <FormField
              v-model="form.account"
              label="账号"
              icon="id"
              placeholder="例：CC20260001"
              autocomplete="username"
              :error="touched.account ? errors.account : ''"
              :valid="accountValid"
              @blur="check('account')"
              @enter="focusPassword"
            />

            <!-- 本机历史身份卡 -->
            <div v-if="history.length" class="chips">
              <span class="chips__label">本机身份卡</span>
              <div class="chips__list">
                <button
                  v-for="acc in history"
                  :key="acc"
                  type="button"
                  class="chip"
                  :class="{ 'is-active': acc === form.account }"
                  @click="useHistory(acc)"
                >
                  {{ acc }}
                  <span
                    class="chip__x"
                    role="button"
                    tabindex="0"
                    :aria-label="`删除 ${acc}`"
                    @click.stop="dropHistory(acc)"
                    @keydown.enter.stop.prevent="dropHistory(acc)"
                  >
                    <AppIcon name="alert" :size="9" />
                  </span>
                </button>
              </div>
            </div>

            <div class="remember-line">
              <p v-if="lastLoginText" class="remember-line__txt">
                <AppIcon name="shield" :size="13" />
                上次登录：<b>{{ lastLoginText }}</b>
              </p>
              <button v-if="form.account || history.length" class="link-tiny" type="button" @click="forget">
                清除本机记录
              </button>
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

            <!-- 密钥能量格 + 共鸣度 -->
            <div class="meter" :class="`meter--${resonance.level}`">
              <div class="meter__slots">
                <i v-for="(on, i) in slots" :key="i" :class="{ 'is-on': on }" />
              </div>
              <span class="meter__bar"><i :style="{ width: resonance.pct + '%' }" /></span>
              <span class="meter__txt">血统共鸣 · {{ resonance.text }}</span>
            </div>

            <div class="form-row">
              <label class="check">
                <input v-model="form.remember" type="checkbox" />
                <span class="check__box" />
                <span>记住身份卡</span>
              </label>
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

            <p class="shortcut-hint">提示：按 <kbd>Ctrl</kbd> / <kbd>⌘</kbd> + <kbd>Enter</kbd> 快速认证</p>

            <div class="divider">或</div>

            <GlowButton variant="ghost" type="button" :disabled="loading" @click="fillDemo">
              <AppIcon name="ticket" :size="16" />
              使用演示身份卡
            </GlowButton>

            <p class="card-note">
              后端通道 ·
              <b v-if="backendOnline === true" class="is-online">
                已连接（127.0.0.1:8082）· {{ ping }}ms
              </b>
              <template v-else-if="backendOnline === false">
                <b class="is-offline">未连接，请先启动 Spring Boot 服务</b>
                <button class="link-tiny link-tiny--btn" type="button" :disabled="checking" @click="probeBackend">
                  {{ checking ? '检测中…' : '重新检测' }}
                </button>
              </template>
              <b v-else>检测中…</b>
              <span class="signal" :class="`signal--${signal}`" :title="`信号强度 ${signal}/3`">
                <i /><i /><i />
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
    rgba(216, 178, 90, 0.14),
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

/* ------------------------------------------------ 本机历史身份卡 */
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
  align-items: center;
  gap: 5px;
  padding: 3px 7px 3px 9px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.6);
  color: var(--text-3);
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: color 0.22s, border-color 0.22s, background 0.22s, transform 0.22s;
}
.chip:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.45);
  transform: translateY(-1px);
}
.chip.is-active {
  color: var(--gold);
  border-color: rgba(216, 178, 90, 0.6);
  background: rgba(216, 178, 90, 0.1);
}
.chip__x {
  display: inline-grid;
  place-items: center;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  color: var(--text-4);
  transition: color 0.2s, background 0.2s;
}
.chip__x:hover {
  color: var(--err);
  background: rgba(179, 39, 45, 0.18);
}

/* ------------------------------------------------ 本机记录行 */
.remember-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: -6px 0 2px;
  min-height: 16px;
}
.remember-line__txt {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin: 0;
  font-size: 11.5px;
  color: var(--text-3);
  letter-spacing: 0.02em;
}
.remember-line__txt b {
  color: var(--text-2);
  font-weight: 600;
}

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
  color: var(--gold-lt);
}
.link-tiny:disabled {
  cursor: default;
  opacity: 0.6;
}
.link-tiny--btn {
  margin-left: 6px;
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

/* ------------------------------------------------ 密钥共鸣度 + 能量格 */
.meter {
  margin: 10px 0 2px;
}
.meter__slots {
  display: flex;
  gap: 5px;
  margin-bottom: 7px;
}
.meter__slots i {
  flex: 1;
  height: 4px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.07);
  transition: background 0.3s ease, box-shadow 0.3s ease;
}
.meter__slots i.is-on {
  background: linear-gradient(90deg, var(--ice-dp), var(--ice));
}
.meter--mid .meter__slots i.is-on {
  background: linear-gradient(90deg, var(--gold-dp), var(--gold));
}
.meter--high .meter__slots i.is-on {
  background: linear-gradient(90deg, var(--gold), var(--gold-lt));
  box-shadow: 0 0 10px var(--gold-glow);
}
.meter--weak .meter__slots i.is-on {
  background: linear-gradient(90deg, var(--blood), var(--blood-lt));
}

.meter__bar {
  display: block;
  height: 3px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
}
.meter__bar i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: linear-gradient(90deg, var(--ice-dp), var(--ice));
  transition: width 0.45s cubic-bezier(0.16, 1, 0.3, 1), background 0.35s ease;
}
.meter--mid .meter__bar i {
  background: linear-gradient(90deg, var(--gold-dp), var(--gold));
}
.meter--high .meter__bar i {
  background: linear-gradient(90deg, var(--gold), var(--gold-lt));
  box-shadow: 0 0 12px var(--gold-glow);
}
.meter--weak .meter__bar i {
  background: linear-gradient(90deg, var(--blood), var(--blood-lt));
}
.meter__txt {
  display: block;
  margin-top: 6px;
  font-size: 11.5px;
  letter-spacing: 0.04em;
  color: var(--text-3);
}
.meter--high .meter__txt {
  color: var(--gold-lt);
}
.meter--weak .meter__txt {
  color: var(--err);
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

/* ------------------------------------------------ 后端状态 + 信号格 */
.card-note b.is-online {
  color: var(--ok);
}
.card-note b.is-offline {
  color: var(--err);
}
.signal {
  display: inline-flex;
  align-items: flex-end;
  gap: 2px;
  height: 10px;
  margin-left: 8px;
  vertical-align: -1px;
}
.signal i {
  width: 3px;
  background: var(--text-4);
  border-radius: 1px;
  transition: background 0.3s;
}
.signal i:nth-child(1) {
  height: 4px;
}
.signal i:nth-child(2) {
  height: 7px;
}
.signal i:nth-child(3) {
  height: 10px;
}
.signal--1 i:nth-child(1) {
  background: var(--warn);
}
.signal--2 i:nth-child(-n + 2) {
  background: var(--gold);
}
.signal--3 i {
  background: var(--ok);
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
}

@media (max-width: 480px) {
  .chips {
    flex-wrap: wrap;
  }
}
</style>
