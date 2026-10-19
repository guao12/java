<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthCard from '@/components/AuthCard.vue'
import FormField from '@/components/FormField.vue'
import GlowButton from '@/components/GlowButton.vue'
import AppIcon from '@/components/AppIcon.vue'
import { authApi } from '@/api/auth'
import { rememberAccount } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const STEPS = ['验证身份', '重置密钥', '完成']
// 支持通过 ?step=2 直达某一步（便于分享 / 演示）
const preset = Number(route.query.step)
const step = ref(preset >= 1 && preset <= 3 ? Math.floor(preset) : 1)

const form = reactive({ account: '', code: '', password: '', confirm: '' })
const errors = reactive({ account: '', code: '', password: '', confirm: '' })
const touched = reactive({ account: false, code: false, password: false, confirm: false })
const loading = ref(false)
const notice = ref(null)
/** 后端直接回显的验证龙文（演示环境）：系统给出、用户照填 */
const resetCode = ref('')
/** 验证龙文有效期（秒），用于倒计时提示 */
const expireSeconds = ref(0)
/** 重新发送倒计时（秒），与后端 60 秒冷却保持一致 */
const cooldown = ref(0)
let cooldownTimer = 0

const EMAIL_RE = /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/
const CODE_RE = /^[A-Za-z0-9_-]{4,20}$/
const STRENGTH_LABEL = ['——', '脆弱', '一般', '良好', '坚不可摧']

function validateAccount() {
  const v = form.account.trim()
  if (!v) return '请输入账号 / 邮箱'
  if (v.includes('@')) return EMAIL_RE.test(v) ? '' : '邮箱格式不正确'
  return CODE_RE.test(v) ? '' : '账号应为 4–20 位字母、数字或下划线'
}

const RULES = {
  account() {
    return validateAccount()
  },
  code() {
    if (!form.code) return '请输入 6 位验证龙文'
    if (!/^\d{6}$/.test(form.code)) return '验证龙文应为 6 位数字'
    return ''
  },
  password() {
    if (!form.password) return '请设置新的言灵密钥'
    if (form.password.length < 8) return '密钥至少 8 位'
    if (!/[A-Za-z]/.test(form.password) || !/\d/.test(form.password))
      return '密钥需同时包含字母与数字'
    return ''
  },
  confirm() {
    if (!form.confirm) return '请再次输入新密钥'
    return form.confirm === form.password ? '' : '两次输入的密钥不一致'
  }
}

function check(field) {
  touched[field] = true
  errors[field] = RULES[field]()
  if (field === 'password' && touched.confirm) errors.confirm = RULES.confirm()
}

const strength = computed(() => {
  const p = form.password
  if (!p) return 0
  let s = 0
  if (p.length >= 6) s++
  if (p.length >= 10) s++
  if (/[A-Za-z]/.test(p) && /\d/.test(p)) s++
  if (/[^A-Za-z0-9]/.test(p)) s++
  return Math.min(s, 4)
})

const passwordValid = computed(() => touched.password && !errors.password && !!form.password)
const confirmValid = computed(() => touched.confirm && !errors.confirm && !!form.confirm)

/** 失焦时探测账号是否已登记，提前给出提示（后端 /api/auth/check-account） */
async function checkAccountExists() {
  const value = form.account.trim()
  if (!value || validateAccount()) return
  try {
    const data = await authApi.checkAccount(value)
    if (!data.registered) {
      errors.account = '该身份卡尚未登记，请先完成新生登记'
    }
  } catch {
    /* 网络异常留给提交时统一提示 */
  }
}

function startCooldown(seconds) {
  cooldown.value = seconds
  window.clearInterval(cooldownTimer)
  cooldownTimer = window.setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) window.clearInterval(cooldownTimer)
  }, 1000)
}

onBeforeUnmount(() => window.clearInterval(cooldownTimer))

async function sendCode() {
  check('account')
  if (errors.account) return
  loading.value = true
  notice.value = null
  try {
    // 调用后端下发验证龙文：演示环境由后端直接回显验证码（你给验证码）
    const data = await authApi.sendResetCode(form.account.trim())
    resetCode.value = data.demoCode
    expireSeconds.value = data.expireSeconds
    startCooldown(60)
    notice.value = {
      type: 'info',
      text: `系统已生成验证龙文，请在下方抄录输入（${Math.round(
        data.expireSeconds / 60
      )} 分钟内有效）。`
    }
    step.value = 2
  } catch (e) {
    notice.value = { type: 'err', text: e.message }
  } finally {
    loading.value = false
  }
}

async function resetKey() {
  notice.value = null
  ;['code', 'password', 'confirm'].forEach(check)
  if (errors.code || errors.password || errors.confirm) {
    notice.value = { type: 'err', text: '重置信息未通过校验，请检查被标记的字段。' }
    return
  }
  loading.value = true
  try {
    // 校验验证龙文并重置密钥（后端会把旧码标记为已使用）
    await authApi.confirmReset({
      account: form.account.trim(),
      code: form.code,
      password: form.password
    })
    rememberAccount(form.account.trim())
    step.value = 3
  } catch (e) {
    notice.value = { type: 'err', text: e.message }
  } finally {
    loading.value = false
  }
}

function prev() {
  step.value = 1
  notice.value = null
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <AuthCard
    badge="Key Recovery"
    title="密钥重置"
    subtitle="验证身份后重新铸造你的言灵密钥"
  >
    <div class="stepper">
      <template v-for="(label, i) in STEPS" :key="label">
        <div
          class="stepper__item"
          :class="{ 'is-active': step === i + 1, 'is-done': step > i + 1 }"
        >
          <span class="stepper__dot">
            <AppIcon v-if="step > i + 1" name="check" :size="12" />
            <template v-else>{{ i + 1 }}</template>
          </span>
          <span class="stepper__label">{{ label }}</span>
        </div>
        <span v-if="i < STEPS.length - 1" class="stepper__line" />
      </template>
    </div>

    <!-- 第 1 步：验证身份 -->
    <form v-if="step === 1" class="form" novalidate @submit.prevent="sendCode">
      <FormField
        v-model="form.account"
        label="账号 / 邮箱"
        icon="id"
        placeholder="例：Luffy.Cassell 或 name@cassell.edu"
        autocomplete="username"
        :error="touched.account ? errors.account : ''"
        @blur="check('account'); checkAccountExists()"
        @enter="sendCode"
      />

      <div v-if="notice" class="alert" :class="`alert--${notice.type}`">
        <AppIcon :name="notice.type === 'err' ? 'alert' : 'info'" :size="15" />
        <span>{{ notice.text }}</span>
      </div>

      <GlowButton :loading="loading" :disabled="!form.account.trim()" loading-text="下发指令中">
        发送重置龙文
      </GlowButton>

      <p class="card-note">
        校验由后端 <b>Spring Boot + MySQL</b> 完成，验证龙文 5 分钟内有效<br />
        演示环境由系统直接显示验证码，你照着输入即可
      </p>
    </form>

    <!-- 第 2 步：重置密钥 -->
    <form v-else-if="step === 2" class="form" novalidate @submit.prevent="resetKey">
      <!-- 系统直接给出的验证龙文：你抄录、我输入 -->
      <div v-if="resetCode" class="code-reveal">
        <span class="code-reveal__label">系统验证龙文 · 请照此输入</span>
        <span class="code-reveal__value">{{ resetCode }}</span>
        <span class="code-reveal__meta">有效期 {{ Math.round(expireSeconds / 60) }} 分钟 · 重新获取请点右侧</span>
      </div>

      <FormField
        v-model="form.code"
        class="field--center"
        label="验证龙文"
        icon="key"
        placeholder="······"
        maxlength="6"
        autocomplete="one-time-code"
        :error="touched.code ? errors.code : ''"
        @blur="check('code')"
        @enter="resetKey"
      />

      <div class="form-row">
        <span class="resend-hint">验证龙文 5 分钟内有效</span>
        <button type="button" class="as-link" :disabled="cooldown > 0 || loading" @click="sendCode">
          {{ cooldown > 0 ? `重新发送（${cooldown}s）` : '重新发送验证龙文' }}
        </button>
      </div>

      <FormField
        v-model="form.password"
        label="新的言灵密钥"
        icon="lock"
        type="password"
        placeholder="至少 8 位，含字母与数字"
        autocomplete="new-password"
        :error="touched.password ? errors.password : ''"
        :valid="passwordValid"
        @blur="check('password')"
      />

      <div v-if="form.password" class="strength">
        <div class="strength__bars">
          <span
            v-for="i in 4"
            :key="i"
            class="strength__bar"
            :class="i <= strength ? `on-${strength}` : ''"
          />
        </div>
        <span class="strength__text">{{ STRENGTH_LABEL[strength] }}</span>
      </div>

      <FormField
        v-model="form.confirm"
        label="确认新密钥"
        icon="shield"
        type="password"
        placeholder="请再次输入新密钥"
        autocomplete="new-password"
        :error="touched.confirm ? errors.confirm : ''"
        :valid="confirmValid"
        @blur="check('confirm')"
        @enter="resetKey"
      />

      <div v-if="notice" class="alert" :class="`alert--${notice.type}`">
        <AppIcon name="alert" :size="15" />
        <span>{{ notice.text }}</span>
      </div>

      <div class="btn-row">
        <GlowButton variant="ghost" type="button" :disabled="loading" @click="prev">
          上一步
        </GlowButton>
        <GlowButton :loading="loading" loading-text="重铸密钥中">重置密钥</GlowButton>
      </div>
    </form>

    <!-- 第 3 步：完成 -->
    <div v-else class="done">
      <div class="done__icon"><AppIcon name="check" :size="30" :stroke="2" /></div>
      <h2 class="done__title">密钥重铸完成</h2>
      <p class="done__text">
        你的言灵密钥已更新，旧密钥即刻失效。<br />
        请使用新密钥重新进行身份认证。
      </p>
      <GlowButton type="button" @click="goLogin">
        返回身份认证
        <AppIcon name="arrow" :size="16" />
      </GlowButton>
    </div>

    <template #foot>
      <router-link class="link-gold" to="/login">想起来了？返回登录</router-link>
    </template>
  </AuthCard>
</template>

<style scoped>
.btn-row {
  display: flex;
  gap: 10px;
}
.btn-row > * {
  flex: 1;
}

.resend-hint {
  font-size: 12px;
  color: var(--text-4);
  letter-spacing: 0.03em;
}

.code-reveal {
  border: 1px dashed var(--gold);
  background: rgba(212, 175, 55, 0.08);
  border-radius: 12px;
  padding: 14px 16px 13px;
  text-align: center;
  margin-bottom: 16px;
  animation: card-in 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.code-reveal__label {
  display: block;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
  margin-bottom: 7px;
}
.code-reveal__value {
  display: block;
  font-family: var(--font-mono, ui-monospace, "SFMono-Regular", Menlo, Consolas, monospace);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 0.34em;
  text-indent: 0.34em;
  color: var(--gold);
  text-shadow: 0 0 18px rgba(212, 175, 55, 0.4);
}
.code-reveal__meta {
  display: block;
  font-size: 11px;
  color: var(--text-4);
  margin-top: 9px;
}

.as-link {
  background: none;
  border: 0;
  padding: 0;
  cursor: pointer;
  font-size: 12.5px;
  color: var(--gold);
  border-bottom: 1px solid transparent;
  transition: color 0.25s, border-color 0.25s;
}
.as-link:hover:not(:disabled) {
  color: var(--gold-lt);
  border-color: currentColor;
}
.as-link:disabled {
  color: var(--text-4);
  cursor: not-allowed;
}

.done {
  text-align: center;
  padding: 6px 0 2px;
}
.done__icon {
  width: 62px;
  height: 62px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: var(--ok);
  border: 1px solid rgba(95, 211, 154, 0.42);
  background: rgba(95, 211, 154, 0.09);
  box-shadow: 0 0 34px -10px rgba(95, 211, 154, 0.65);
  animation: card-in 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.done__title {
  font-family: var(--font-serif);
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--text);
  margin-bottom: 10px;
}
.done__text {
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-3);
  margin-bottom: 22px;
}
</style>
