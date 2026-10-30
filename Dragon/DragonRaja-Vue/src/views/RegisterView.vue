<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AuthCard from '@/components/AuthCard.vue'
import FormField from '@/components/FormField.vue'
import GlowButton from '@/components/GlowButton.vue'
import AppIcon from '@/components/AppIcon.vue'
import OtpInput from '@/components/OtpInput.vue'
import IdCardPreview from '@/components/IdCardPreview.vue'
import { authApi } from '@/api/auth'
import { rememberAccount } from '@/utils/auth'

const router = useRouter()

const IDENTITIES = [
  { value: 'freshman', label: '新生 · 2026 级' },
  { value: 'student', label: '在校生' },
  { value: 'executor', label: '执行部预备成员' },
  { value: 'teacher', label: '教员 / 研究员' },
  { value: 'guest', label: '访客 · 临时通行' }
]

/** 各身份的通行权限说明（选完即时提示，纯前端文案） */
const IDENTITY_NOTE = {
  freshman: '权限：公开档案 + 内部档案（密级 ≤ 2）',
  student: '权限：公开档案 + 内部档案（密级 ≤ 2）',
  executor: '权限：追加机密级档案（密级 ≤ 3）',
  teacher: '权限：全密级档案，含绝密卷宗（密级 ≤ 4）',
  guest: '权限：仅公开档案（密级 ≤ 1），30 天后自动失效'
}

/** 言灵卡片：value 与后端字典 code 一致，desc 为前端展示文案 */
const SPIRIT_CARDS = [
  { value: 'none', label: '未觉醒', desc: '血统尚未显现，待入学后统一鉴定', icon: 'shield' },
  { value: 'junyan', label: '言灵 · 君焰', desc: '高阶火系，领域内绝对压制', icon: 'flame' },
  { value: 'bronze', label: '言灵 · 青铜御座', desc: '重力操控，可抬升万钧之物', icon: 'shield' },
  { value: 'shiling', label: '言灵 · 时零', desc: '时间迟滞，令领域内万物减速', icon: 'sparkle' },
  { value: 'fengwang', label: '言灵 · 风王之瞳', desc: '风刃凝聚，洞穿一切实体', icon: 'sparkle' },
  { value: 'zhulong', label: '言灵 · 烛龙', desc: '极寒吐息，冻结千里之地', icon: 'flame' },
  { value: 'wangquan', label: '言灵 · 王权', desc: '血统威压，令低阶混血种臣服', icon: 'key' }
]

// 下拉项 / 卡片优先取后端字典（前后端共用一套 code）；后端未启动时回退到本地默认值
const identities = ref(IDENTITIES)
const spirits = ref(SPIRIT_CARDS)

onMounted(async () => {
  try {
    const dict = await authApi.dictionaries()
    if (dict?.identities?.length) {
      identities.value = dict.identities.map((item) => ({ value: item.code, label: item.name }))
    }
    if (dict?.spirits?.length) {
      const nameMap = new Map(dict.spirits.map((item) => [item.code, item.name]))
      spirits.value = SPIRIT_CARDS.map((c) => ({ ...c, label: nameMap.get(c.value) || c.label }))
    }
  } catch {
    /* 后端未启动：继续使用本地字典，提交时会给出明确错误提示 */
  }
  // 预加载图形验证码（注册下发前置）
  loadCaptcha()
})

const form = reactive({
  codename: '',
  code: '',
  identity: '',
  spirit: 'none',
  password: '',
  confirm: '',
  agree: false
})

const errors = reactive({
  codename: '',
  code: '',
  identity: '',
  spirit: '',
  password: '',
  confirm: '',
  agree: ''
})
const touched = reactive({ codename: false, code: false, identity: false, password: false, confirm: false })
const loading = ref(false)
const done = ref(false)
const notice = ref(null)
/** 协议摘要展开状态：'' / 'rules'（学院守则）/ 'agreement'（保密协议） */
const legalOpen = ref('')

/* ---------------- 分步向导 ---------------- */
const STEPS = [
  { key: 'identity', label: '身份核验' },
  { key: 'blood', label: '血统鉴定' },
  { key: 'key', label: '密钥设定' }
]
const step = ref(0)
/** 血统鉴定步骤要求手动点选一次（含「未觉醒」），避免一路回车跳过 */
const spiritPicked = ref(false)

const progress = computed(() => (step.value / (STEPS.length - 1)) * 100)
const isLast = computed(() => step.value === STEPS.length - 1)

/** 浏览器标识：用于把验证码绑定到本次登记，刷新页面即失效 */
const clientKey = (() => {
  const bytes = new Uint8Array(8)
  if (window.crypto?.getRandomValues) {
    window.crypto.getRandomValues(bytes)
    return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  }
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-6)
})()

/** 系统直接给出的验证码（演示环境不发邮件，改由后端回显） */
const demoCode = ref('')
/** 验证码有效期（秒） */
const expireSeconds = ref(0)
/** 重新获取的冷却（秒），与后端 60 秒一致 */
const cooldown = ref(0)
let cooldownTimer = 0

/** 图形验证码：注册下发前置，防脚本批量刷取 */
const captcha = reactive({ token: '', image: '', input: '', error: '' })

/** 拉取一张新的图形验证码（token 由后端签发，答案仅存服务端） */
async function loadCaptcha() {
  try {
    const data = await authApi.captcha()
    captcha.token = data.token
    captcha.image = data.image
    captcha.input = ''
    captcha.error = ''
  } catch {
    captcha.error = '验证码加载失败，请点击刷新后重试'
  }
}

const otpRef = ref(null)

const STRENGTH_LABEL = ['——', '脆弱', '一般', '良好', '坚不可摧']

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

const checks = computed(() => ({
  len: form.password.length >= 8,
  mix: /[A-Za-z]/.test(form.password) && /\d/.test(form.password),
  symbol: /[^A-Za-z0-9]/.test(form.password)
}))

/** 身份卡预览用：身份 / 言灵文案 */
const identityLabel = computed(
  () => identities.value.find((i) => i.value === form.identity)?.label || ''
)
const spiritLabel = computed(() => spirits.value.find((s) => s.value === form.spirit)?.label || '')
const identityNote = computed(() => (form.identity ? IDENTITY_NOTE[form.identity] || '' : ''))

const RULES = {
  codename() {
    const v = form.codename.trim()
    if (!v) return '请输入姓名或行动代号'
    if (v.length < 2) return '长度至少 2 个字符'
    if (v.length > 16) return '长度不超过 16 个字符'
    return ''
  },
  code() {
    const v = form.code.trim()
    if (!v) return '请输入验证码'
    return /^\d{6}$/.test(v) ? '' : '验证码应为 6 位数字'
  },
  identity() {
    return form.identity ? '' : '请选择在校身份'
  },
  password() {
    if (!form.password) return '请设置言灵密钥'
    if (form.password.length < 8) return '密钥至少 8 位'
    if (!/[A-Za-z]/.test(form.password) || !/\d/.test(form.password))
      return '密钥需同时包含字母与数字'
    return ''
  },
  confirm() {
    if (!form.confirm) return '请再次输入密钥以确认'
    return form.confirm === form.password ? '' : '两次输入的密钥不一致'
  }
}

/** 每一步需要校验的字段 */
const STEP_FIELDS = [['codename', 'identity', 'code'], ['spirit'], ['password', 'confirm']]

function check(field) {
  if (field === 'spirit') {
    errors.spirit = ''
    return
  }
  touched[field] = true
  errors[field] = RULES[field]()
  if (field === 'password' && touched.confirm) errors.confirm = RULES.confirm()
}

const passwordValid = computed(
  () => touched.password && !errors.password && !!form.password
)
const confirmValid = computed(() => touched.confirm && !errors.confirm && !!form.confirm)

function startCooldown(seconds) {
  cooldown.value = seconds
  window.clearInterval(cooldownTimer)
  cooldownTimer = window.setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) window.clearInterval(cooldownTimer)
  }, 1000)
}

onBeforeUnmount(() => window.clearInterval(cooldownTimer))

/** 获取验证码：先过图形验证码，后端生成后直接回显给页面（系统给验证码，不发邮件） */
async function sendCode() {
  notice.value = null
  // 前端基础校验：图形验证码必填
  if (!captcha.input.trim()) {
    captcha.error = '请输入图形验证码'
    return
  }
  loading.value = true
  try {
    const data = await authApi.sendRegisterCode({
      clientKey,
      captchaToken: captcha.token,
      captchaCode: captcha.input.trim()
    })
    demoCode.value = data.demoCode
    expireSeconds.value = data.expireSeconds || 300
    startCooldown(data.cooldownSeconds || 60)
    notice.value = {
      type: 'ok',
      text: `系统已生成验证码，可直接点上方金色数字一键填入（${Math.round(expireSeconds.value / 60)} 分钟内有效）。`
    }
    // 本次验证码已消费，刷新一张新的备用
    await loadCaptcha()
  } catch (e) {
    notice.value = { type: 'err', text: e.message }
    // 验证码错误或已消费：刷新一张新的，避免用户卡死
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

/** 点击系统给出的验证码 → 自动填入六格 */
function fillCode() {
  if (!demoCode.value) return
  form.code = demoCode.value
  touched.code = true
  errors.code = RULES.code()
  nextTick(() => otpRef.value?.focus(6))
}

function onCodeComplete() {
  touched.code = true
  errors.code = RULES.code()
}

/* ---------------- 密钥生成器 ---------------- */
const UPPER = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
const LOWER = 'abcdefghijkmnopqrstuvwxyz'
const DIGIT = '23456789'
const SYMBOL = '!@#$%&*'

/** 取一个密码学安全的随机下标 */
function randIndex(max) {
  const arr = new Uint32Array(1)
  if (window.crypto?.getRandomValues) window.crypto.getRandomValues(arr)
  else arr[0] = Math.floor(Math.random() * 0xffffffff)
  return arr[0] % max
}

function rnd(set) {
  return set[randIndex(set.length)]
}

function generatePassword() {
  const pool = []
  pool.push(rnd(UPPER), rnd(LOWER), rnd(DIGIT), rnd(SYMBOL))
  const all = UPPER + LOWER + DIGIT
  while (pool.length < 12) pool.push(rnd(all))
  // 洗牌，避免固定位置规律
  for (let i = pool.length - 1; i > 0; i -= 1) {
    const j = randIndex(i + 1)
    const tmp = pool[i]
    pool[i] = pool[j]
    pool[j] = tmp
  }
  const pwd = pool.join('')
  form.password = pwd
  form.confirm = pwd
  touched.password = true
  touched.confirm = true
  errors.password = RULES.password()
  errors.confirm = RULES.confirm()
  notice.value = { type: 'info', text: '已按学院规范生成 12 位密钥并自动填入两栏，请自行抄录保存。' }
}

async function copyPassword() {
  if (!form.password) return
  try {
    await navigator.clipboard.writeText(form.password)
    notice.value = { type: 'ok', text: '密钥已复制到剪贴板。' }
  } catch {
    notice.value = { type: 'info', text: '当前浏览器禁止自动复制，请手动选中密钥复制。' }
  }
}

/* ---------------- 步骤流转 ---------------- */
function validateStep(index) {
  const fields = STEP_FIELDS[index] || []
  let ok = true
  fields.forEach((f) => {
    check(f)
    if (errors[f]) ok = false
  })
  if (index === 1 && !spiritPicked.value) {
    ok = false
    notice.value = { type: 'err', text: '请点选你的言灵鉴定结果（尚未觉醒可选「未觉醒」）。' }
  }
  return ok
}

function goNext() {
  notice.value = null
  if (!validateStep(step.value)) {
    if (step.value !== 1) {
      notice.value = { type: 'err', text: '本步信息存在未通过校验的项目，请检查被标记的字段。' }
    }
    return
  }
  if (isLast.value) {
    onSubmit()
    return
  }
  step.value += 1
}

function goPrev() {
  notice.value = null
  if (step.value > 0) step.value -= 1
}

function pickSpirit(value) {
  form.spirit = value
  spiritPicked.value = true
  notice.value = null
}

watch(step, () => {
  notice.value = null
})

async function onSubmit() {
  notice.value = null
  Object.keys(RULES).forEach(check)
  errors.agree = form.agree ? '' : '请先阅读并同意《卡塞尔学院守则》'

  if (Object.values(errors).some(Boolean)) {
    // 若错误发生在前面的步骤，自动回退到对应步骤
    if (errors.codename || errors.code || errors.identity) step.value = 0
    notice.value = { type: 'err', text: '登记信息存在未通过校验的项目，请检查被标记的字段。' }
    return
  }

  loading.value = true
  try {
    // 调用后端 /api/auth/register，由后端校验验证码并签发账号落库
    const data = await authApi.register({
      codename: form.codename.trim(),
      identity: form.identity,
      spirit: form.spirit,
      password: form.password,
      code: form.code.trim(),
      clientKey
    })
    done.value = true
    // 把新账号写入"上次账号"，跳回登录页后自动预填
    rememberAccount(data.account)
    notice.value = {
      type: 'ok',
      text: `登记完成 · ${data.codename}，账号 ${data.account} 已签发（${data.identityName} / ${data.spiritName}），即将跳转登录…`
    }
    window.setTimeout(() => router.push('/login'), 2600)
  } catch (e) {
    notice.value = { type: 'err', text: e.message }
  } finally {
    loading.value = false
  }
}

/** Ctrl / ⌘ + Enter 快捷提交 */
function onShortcut(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    if (isLast.value) onSubmit()
    else goNext()
  }
}

onMounted(() => window.addEventListener('keydown', onShortcut))
onBeforeUnmount(() => window.removeEventListener('keydown', onShortcut))
</script>

<template>
  <AuthCard
    wide
    badge="Freshman Registration"
    title="新生登记"
    subtitle="登记你的混血种身份，领取卡塞尔学院身份卡"
  >
    <template #tabs>
      <router-link to="/login" active-class="active">登录</router-link>
      <router-link to="/register" active-class="active">注册</router-link>
    </template>

    <!-- 步骤条 -->
    <div class="stepper">
      <template v-for="(s, i) in STEPS" :key="s.key">
        <div
          class="stepper__item"
          :class="{ 'is-active': i === step, 'is-done': i < step }"
        >
          <span class="stepper__dot">
            <AppIcon v-if="i < step" name="check" :size="11" />
            <template v-else>{{ i + 1 }}</template>
          </span>
          <span class="stepper__label">{{ s.label }}</span>
        </div>
        <span v-if="i < STEPS.length - 1" class="stepper__line" />
      </template>
    </div>
    <div class="progress"><i :style="{ width: progress + '%' }" /></div>

    <form class="form" novalidate @submit.prevent="goNext">
      <!-- ============ 第 1 步：身份核验 ============ -->
      <template v-if="step === 0">
        <div class="grid-2">
          <FormField
            v-model="form.codename"
            label="姓名 / 行动代号"
            icon="user"
            placeholder="例：路明非"
            :maxlength="16"
            :error="touched.codename ? errors.codename : ''"
            @blur="check('codename')"
          />
          <FormField
            v-model="form.identity"
            label="在校身份"
            icon="id"
            type="select"
            placeholder="请选择身份"
            :options="identities"
            :error="touched.identity ? errors.identity : ''"
            @blur="check('identity')"
          />
        </div>

        <p v-if="identityNote" class="note-line">
          <AppIcon name="shield" :size="12" />{{ identityNote }}
        </p>

        <!-- 验证码：系统直接给出（原「学院邮箱」验证已改为系统给验证码） -->
        <div v-if="demoCode" class="code-reveal" role="button" @click="fillCode">
          <span class="code-reveal__label">系统验证码 · 点击一键填入</span>
          <span class="code-reveal__value">{{ demoCode }}</span>
          <span class="code-reveal__meta">
            有效期 {{ Math.round(expireSeconds / 60) }} 分钟 · 由系统直接给出，不发邮件
          </span>
        </div>

        <!-- 图形验证码：注册下发前置，防脚本批量刷取 -->
        <div class="captcha-row">
          <input
            v-model="captcha.input"
            class="captcha-input"
            type="text"
            maxlength="4"
            placeholder="图形验证码"
            :disabled="cooldown > 0 || loading"
            @input="captcha.error = ''"
          />
          <img
            v-if="captcha.image"
            :src="captcha.image"
            class="captcha-img"
            title="点击刷新"
            alt="图形验证码"
            @click="loadCaptcha"
          />
          <button type="button" class="captcha-refresh" :disabled="loading" @click="loadCaptcha">
            换一张
          </button>
        </div>
        <p v-if="captcha.error" class="field__msg">
          <AppIcon name="alert" :size="13" />{{ captcha.error }}
        </p>

        <div class="code-block">
          <label class="code-block__label">验证码</label>
          <div class="code-row">
            <OtpInput
              ref="otpRef"
              v-model="form.code"
              :length="6"
              :invalid="!!(touched.code && errors.code)"
              @complete="onCodeComplete"
            />
            <button
              type="button"
              class="code-btn"
              :disabled="cooldown > 0 || loading"
              @click="sendCode"
            >
              <span v-if="cooldown > 0">{{ cooldown }}s</span>
              <span v-else-if="demoCode">重发</span>
              <span v-else>获取</span>
            </button>
          </div>
          <p v-if="touched.code && errors.code" class="field__msg">
            <AppIcon name="alert" :size="13" />{{ errors.code }}
          </p>
          <p v-else class="code-meta">
            <AppIcon name="info" :size="12" />
            {{ demoCode ? '照上方金色数字填入，或点击数字自动填入' : '点击右侧「获取」，由系统直接给出 6 位验证码' }}
          </p>
        </div>
      </template>

      <!-- ============ 第 2 步：血统鉴定 ============ -->
      <template v-else-if="step === 1">
        <p class="step-title">选择你的言灵鉴定结果</p>
        <div class="spirits">
          <button
            v-for="s in spirits"
            :key="s.value"
            type="button"
            class="spirit"
            :class="{ 'is-on': form.spirit === s.value }"
            @click="pickSpirit(s.value)"
          >
            <span class="spirit__icon"><AppIcon :name="s.icon" :size="17" /></span>
            <span class="spirit__text">
              <b>{{ s.label }}</b>
              <em>{{ s.desc }}</em>
            </span>
            <span class="spirit__tick"><AppIcon name="check" :size="12" /></span>
          </button>
        </div>
        <p class="note-line">
          <AppIcon name="info" :size="12" />入学后可到执行部重新鉴定，此处仅用于生成身份卡底档。
        </p>
        <IdCardPreview
          :name="form.codename"
          account="待 签 发"
          :identity="identityLabel"
          :spirit="spiritLabel"
          :grade="form.spirit === 'none' ? '—' : 'A'"
          state="scan"
          status="血统鉴定完成 · 等待签发账号"
          :serial="form.spirit"
        />
      </template>

      <!-- ============ 第 3 步：密钥设定 ============ -->
      <template v-else>
        <FormField
          v-model="form.password"
          label="设置言灵密钥"
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

        <div class="gen-row">
          <button type="button" class="as-link" @click="generatePassword">
            <AppIcon name="sparkle" :size="12" />让学院为我生成密钥
          </button>
          <button v-if="form.password" type="button" class="as-link" @click="copyPassword">
            <AppIcon name="key" :size="12" />复制密钥
          </button>
        </div>

        <FormField
          v-model="form.confirm"
          label="确认密钥"
          icon="shield"
          type="password"
          placeholder="请再次输入密钥"
          autocomplete="new-password"
          :error="touched.confirm ? errors.confirm : ''"
          :valid="confirmValid"
          @blur="check('confirm')"
        />

        <div class="rule-hints">
          <span :class="checks.len ? 'ok' : ''">
            <AppIcon :name="checks.len ? 'check' : 'alert'" :size="12" />8 位以上
          </span>
          <span :class="checks.mix ? 'ok' : ''">
            <AppIcon :name="checks.mix ? 'check' : 'alert'" :size="12" />字母 + 数字
          </span>
          <span :class="checks.symbol ? 'ok' : ''">
            <AppIcon :name="checks.symbol ? 'check' : 'alert'" :size="12" />含符号更佳
          </span>
        </div>

        <label class="check">
          <input v-model="form.agree" type="checkbox" />
          <span class="check__box" />
          <span>
            我已阅读并同意
            <button
              type="button"
              class="link-gold legal-link"
              @click="legalOpen = legalOpen === 'rules' ? '' : 'rules'"
            >
              《卡塞尔学院守则》
            </button>
            与
            <button
              type="button"
              class="link-gold legal-link"
              @click="legalOpen = legalOpen === 'agreement' ? '' : 'agreement'"
            >
              《血统保密协议》
            </button>
          </span>
        </label>
        <div v-if="legalOpen" class="legal-panel">
          <strong>{{ legalOpen === 'rules' ? '卡塞尔学院守则（摘要）' : '血统保密协议（摘要）' }}</strong>
          <p>
            {{
              legalOpen === 'rules'
                ? '本站为《龙族》同人兴趣作品，非商用，版权归原著作者及出版方所有。注册即视为以虚构身份参与互动，所填代号、言灵与密钥仅用于本站演示，请勿使用真实敏感信息。'
                : '你的血统数据与身份档案仅存储于本站演示数据库，不对外共享。本站不会以任何形式索要真实姓名、证件号或支付信息；如遇此类请求，均为诈骗。'
            }}
          </p>
        </div>
        <p v-if="errors.agree" class="field__msg">
          <AppIcon name="alert" :size="13" />{{ errors.agree }}
        </p>
      </template>

      <div v-if="notice" class="alert" :class="`alert--${notice.type}`">
        <AppIcon :name="notice.type === 'ok' ? 'check' : notice.type === 'err' ? 'alert' : 'info'" :size="15" />
        <span>{{ notice.text }}</span>
      </div>

      <!-- 步骤按钮 -->
      <div class="actions">
        <button v-if="step > 0" type="button" class="btn btn--ghost actions__back" @click="goPrev">
          上一步
        </button>
        <GlowButton
          :loading="loading && isLast"
          :disabled="done || loading"
          :loading-text="'签发身份卡中'"
        >
          <template v-if="isLast">提交登记</template>
          <template v-else>下一步 · {{ STEPS[step + 1].label }}</template>
        </GlowButton>
      </div>

      <p class="card-note">
        第 {{ step + 1 }} / {{ STEPS.length }} 步 ·
        按 <kbd>Ctrl</kbd> / <kbd>⌘</kbd> + <kbd>Enter</kbd> 可继续<br />
        本页面为 <b>龙族</b> 同人风格作品，与原著版权方无关
      </p>
    </form>

    <template #foot>
      已持有身份卡？<router-link class="link-gold" to="/login">返回登录</router-link>
    </template>

    <!-- 签发成功过场 -->
    <div v-if="done" class="issued" aria-hidden="true">
      <span class="issued__ring" />
      <span class="issued__seal">已签发</span>
    </div>
  </AuthCard>
</template>

<style scoped>
/* ---------------- 步骤条 + 进度 ---------------- */
.stepper {
  margin-bottom: 10px;
}
.progress {
  height: 2px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
  margin-bottom: 18px;
}
.progress i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: linear-gradient(90deg, var(--gold-dp), var(--gold-lt));
  box-shadow: 0 0 12px var(--gold-glow);
  transition: width 0.45s cubic-bezier(0.16, 1, 0.3, 1);
}

.step-title {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: var(--text-3);
  text-transform: uppercase;
  margin-bottom: 4px;
}

.note-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: -6px 0 0;
  font-size: 11.5px;
  letter-spacing: 0.03em;
  color: var(--text-4);
  animation: fade-in 0.3s both;
}
.note-line svg {
  flex: 0 0 auto;
  color: var(--gold-dp);
}

/* ---------------- 表单栅格 ---------------- */
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

/* ---------------- 验证码 ---------------- */
.code-reveal {
  border: 1px dashed var(--gold);
  background: rgba(212, 175, 55, 0.08);
  border-radius: 12px;
  padding: 12px 16px 11px;
  text-align: center;
  cursor: pointer;
  animation: card-in 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
  transition: background 0.25s, transform 0.25s, box-shadow 0.25s;
}
.code-reveal:hover {
  background: rgba(212, 175, 55, 0.14);
  transform: translateY(-1px);
  box-shadow: 0 10px 30px -18px var(--gold-glow);
}
.code-reveal__label {
  display: block;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
  margin-bottom: 6px;
}
.code-reveal__value {
  display: block;
  font-family: var(--font-mono, ui-monospace, "SFMono-Regular", Menlo, Consolas, monospace);
  font-size: 27px;
  font-weight: 700;
  letter-spacing: 0.32em;
  text-indent: 0.32em;
  color: var(--gold);
  text-shadow: 0 0 18px rgba(212, 175, 55, 0.4);
}
.code-reveal__meta {
  display: block;
  font-size: 11px;
  color: var(--text-4);
  margin-top: 7px;
}

.code-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.code-block__label {
  font-family: var(--font-mono);
  font-size: 11.5px;
  letter-spacing: 0.16em;
  color: var(--text-3);
  text-transform: uppercase;
}
.code-row {
  display: flex;
  align-items: stretch;
  gap: 10px;
}
.code-row :deep(.otp) {
  flex: 1;
  min-width: 0;
}
.code-btn {
  flex: 0 0 78px;
  border-radius: 10px;
  border: 1px solid rgba(216, 178, 90, 0.34);
  background: rgba(216, 178, 90, 0.08);
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: background 0.22s, border-color 0.22s, color 0.22s, transform 0.22s;
}
.code-btn:hover:not(:disabled) {
  background: rgba(216, 178, 90, 0.16);
  border-color: var(--gold);
  transform: translateY(-1px);
}
.code-btn:disabled {
  color: var(--text-4);
  border-color: var(--line-soft);
  background: rgba(4, 7, 13, 0.6);
  cursor: not-allowed;
}
.code-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 11px;
  color: var(--text-4);
  letter-spacing: 0.03em;
}

/* ---------------- 图形验证码 ---------------- */
.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.captcha-input {
  flex: 1;
  min-width: 0;
  height: 42px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.5);
  color: var(--text);
  font-family: var(--font-mono, ui-monospace, Menlo, Consolas, monospace);
  font-size: 15px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  outline: none;
  transition: border-color 0.22s, box-shadow 0.22s;
}
.captcha-input:focus {
  border-color: var(--gold);
  box-shadow: 0 0 0 1px rgba(216, 178, 90, 0.25);
}
.captcha-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.captcha-img {
  flex: 0 0 auto;
  height: 42px;
  border-radius: 8px;
  border: 1px solid var(--line-soft);
  cursor: pointer;
  background: #fff;
  transition: border-color 0.22s, transform 0.22s;
}
.captcha-img:hover {
  border-color: var(--gold);
  transform: translateY(-1px);
}
.captcha-refresh {
  flex: 0 0 auto;
  background: none;
  border: 0;
  padding: 0;
  color: var(--gold);
  font-size: 12.5px;
  letter-spacing: 0.04em;
  cursor: pointer;
  border-bottom: 1px solid transparent;
  transition: color 0.25s, border-color 0.25s;
}
.captcha-refresh:hover:not(:disabled) {
  color: var(--gold-lt);
  border-color: currentColor;
}
.captcha-refresh:disabled {
  color: var(--text-4);
  cursor: not-allowed;
}

/* ---------------- 言灵卡片 ---------------- */
.spirits {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
}
.spirit {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 10px 11px;
  border-radius: 11px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.5);
  cursor: pointer;
  text-align: left;
  overflow: hidden;
  transition: border-color 0.25s, background 0.25s, transform 0.25s, box-shadow 0.25s;
}
.spirit:hover {
  border-color: rgba(216, 178, 90, 0.4);
  transform: translateY(-2px);
}
.spirit.is-on {
  border-color: var(--gold);
  background: rgba(216, 178, 90, 0.1);
  box-shadow: 0 0 0 1px rgba(216, 178, 90, 0.16), 0 12px 30px -20px var(--gold-glow);
}
.spirit__icon {
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  border: 1px solid var(--line-soft);
  background: rgba(216, 178, 90, 0.06);
  color: var(--text-3);
  transition: color 0.25s, border-color 0.25s, background 0.25s;
}
.spirit.is-on .spirit__icon {
  color: var(--gold);
  border-color: rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.14);
}
.spirit__text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.spirit__text b {
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.spirit.is-on .spirit__text b {
  color: var(--gold-lt);
}
.spirit__text em {
  font-style: normal;
  font-size: 10.5px;
  line-height: 1.45;
  color: var(--text-4);
}
.spirit__tick {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 15px;
  height: 15px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #1a1206;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
  opacity: 0;
  transform: scale(0.4);
  transition: opacity 0.25s, transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.spirit.is-on .spirit__tick {
  opacity: 1;
  transform: scale(1);
}

/* ---------------- 密钥生成 ---------------- */
.gen-row {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin: -6px 0 0;
}
.as-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: none;
  border: 0;
  padding: 0;
  cursor: pointer;
  font-size: 12px;
  color: var(--gold);
  border-bottom: 1px solid transparent;
  transition: color 0.25s, border-color 0.25s;
}
.as-link:hover {
  color: var(--gold-lt);
  border-color: currentColor;
}

.rule-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  margin-top: -8px;
  font-size: 11px;
  color: var(--text-4);
  letter-spacing: 0.04em;
}
.rule-hints span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: color 0.3s;
}
.rule-hints span.ok {
  color: var(--ok);
}

/* ---------------- 动作区 ---------------- */
.actions {
  display: flex;
  gap: 10px;
}
.actions__back {
  flex: 0 0 96px;
  width: auto;
  letter-spacing: 0.12em;
}

.card-note kbd {
  display: inline-block;
  min-width: 16px;
  padding: 0 4px;
  margin: 0 1px;
  border: 1px solid var(--line-soft);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.04);
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-3);
}

/* ---------------- 签发成功过场 ---------------- */
.issued {
  position: absolute;
  inset: 0;
  z-index: 6;
  display: grid;
  place-items: center;
  background: rgba(4, 7, 13, 0.55);
  backdrop-filter: blur(2px);
  animation: issued-in 0.35s ease both;
  pointer-events: none;
}
.issued__ring {
  position: absolute;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  border: 1px solid var(--gold);
  animation: issued-ring 1.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
.issued__seal {
  position: relative;
  padding: 10px 22px;
  border: 2px solid var(--gold);
  border-radius: 8px;
  font-family: var(--font-serif);
  font-size: 26px;
  letter-spacing: 0.3em;
  text-indent: 0.3em;
  color: var(--gold-lt);
  text-shadow: 0 0 22px var(--gold-glow);
  transform: rotate(-11deg) scale(0.6);
  opacity: 0;
  animation: issued-seal 0.6s 0.15s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
@keyframes issued-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
@keyframes issued-ring {
  0% {
    transform: scale(0.3);
    opacity: 0;
  }
  40% {
    opacity: 0.9;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}
@keyframes issued-seal {
  0% {
    transform: rotate(-11deg) scale(0.6);
    opacity: 0;
  }
  70% {
    transform: rotate(-11deg) scale(1.06);
    opacity: 1;
  }
  100% {
    transform: rotate(-11deg) scale(1);
    opacity: 0.95;
  }
}

@media (max-width: 560px) {
  .grid-2 {
    grid-template-columns: 1fr;
    gap: 17px;
  }
  .spirits {
    grid-template-columns: 1fr;
  }
  .code-row {
    flex-direction: column;
  }
  .code-btn {
    flex: 0 0 auto;
    height: 42px;
  }
  .actions__back {
    flex: 0 0 82px;
  }
}

/* 协议摘要：原死链改为可展开的按钮 + 面板 */
.legal-link {
  font: inherit;
  background: none;
  border: 0;
  padding: 0;
  border-bottom: 1px solid rgba(216, 178, 90, 0.4);
  cursor: pointer;
}
.legal-link:hover {
  color: var(--gold-lt);
  border-color: currentColor;
}
.legal-panel {
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.5);
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-3);
  animation: fade-in 0.3s both;
}
.legal-panel strong {
  display: block;
  margin-bottom: 5px;
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.08em;
}
</style>
