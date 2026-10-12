<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthCard from '@/components/AuthCard.vue'
import FormField from '@/components/FormField.vue'
import GlowButton from '@/components/GlowButton.vue'
import AppIcon from '@/components/AppIcon.vue'
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

const SPIRITS = [
  { value: 'none', label: '未觉醒 · 待血统鉴定' },
  { value: 'junyan', label: '言灵 · 君焰' },
  { value: 'bronze', label: '言灵 · 青铜御座' },
  { value: 'shiling', label: '言灵 · 时零' },
  { value: 'fengwang', label: '言灵 · 风王之瞳' },
  { value: 'zhulong', label: '言灵 · 烛龙' },
  { value: 'wangquan', label: '言灵 · 王权' }
]

// 下拉项优先取后端字典（前后端共用一套 code）；后端未启动时回退到本地默认值
const identities = ref(IDENTITIES)
const spirits = ref(SPIRITS)

onMounted(async () => {
  try {
    const dict = await authApi.dictionaries()
    if (dict?.identities?.length) {
      identities.value = dict.identities.map((item) => ({ value: item.code, label: item.name }))
    }
    if (dict?.spirits?.length) {
      spirits.value = dict.spirits.map((item) => ({ value: item.code, label: item.name }))
    }
  } catch {
    /* 后端未启动：继续使用本地字典，提交时会给出明确错误提示 */
  }
})

const form = reactive({
  codename: '',
  email: '',
  identity: '',
  spirit: 'none',
  password: '',
  confirm: '',
  agree: false
})

const errors = reactive({ codename: '', email: '', identity: '', password: '', confirm: '', agree: '' })
const touched = reactive({ codename: false, email: false, identity: false, password: false, confirm: false })
const loading = ref(false)
const done = ref(false)
const notice = ref(null)

const EMAIL_RE = /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/
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

const RULES = {
  codename() {
    const v = form.codename.trim()
    if (!v) return '请输入姓名或行动代号'
    if (v.length < 2) return '长度至少 2 个字符'
    if (v.length > 16) return '长度不超过 16 个字符'
    return ''
  },
  email() {
    const v = form.email.trim()
    if (!v) return '请输入常用邮箱，用于接收学院通知'
    return EMAIL_RE.test(v) ? '' : '邮箱格式不正确'
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

function check(field) {
  touched[field] = true
  errors[field] = RULES[field]()
  if (field === 'password' && touched.confirm) errors.confirm = RULES.confirm()
}

const passwordValid = computed(
  () => touched.password && !errors.password && !!form.password
)
const confirmValid = computed(() => touched.confirm && !errors.confirm && !!form.confirm)

const canSubmit = computed(() => {
  return (
    !!form.codename.trim() &&
    !!form.email.trim() &&
    !!form.identity &&
    !!form.password &&
    !!form.confirm &&
    form.agree
  )
})

async function onSubmit() {
  notice.value = null
  Object.keys(RULES).forEach(check)
  errors.agree = form.agree ? '' : '请先阅读并同意《卡塞尔学院守则》'

  if (Object.values(errors).some(Boolean)) {
    notice.value = { type: 'err', text: '登记信息存在未通过校验的项目，请检查被标记的字段。' }
    return
  }

  loading.value = true
  try {
    // 调用后端 /api/auth/register，由后端签发学院编号（学号）并落库
    const data = await authApi.register({
      codename: form.codename.trim(),
      email: form.email.trim(),
      identity: form.identity,
      spirit: form.spirit,
      password: form.password
    })
    done.value = true
    // 把新学号写入"上次账号"，跳回登录页后自动预填
    rememberAccount(data.account)
    notice.value = {
      type: 'ok',
      text: `登记完成 · ${data.codename}，学院编号 ${data.account} 已签发（${data.identityName} / ${data.spiritName}），即将跳转登录…`
    }
    window.setTimeout(() => router.push('/login'), 2200)
  } catch (e) {
    notice.value = { type: 'err', text: e.message }
  } finally {
    loading.value = false
  }
}
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

    <form class="form" novalidate @submit.prevent="onSubmit">
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

      <FormField
        v-model="form.email"
        label="学院邮箱"
        icon="mail"
        type="email"
        placeholder="name@cassell.edu"
        autocomplete="email"
        :error="touched.email ? errors.email : ''"
        @blur="check('email')"
      />

      <FormField
        v-model="form.spirit"
        label="言灵"
        icon="flame"
        type="select"
        optional="可在鉴定后补填"
        :options="spirits"
      />

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
        @enter="onSubmit"
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
          <a href="javascript:void(0)">《卡塞尔学院守则》</a>
          与
          <a href="javascript:void(0)">《血统保密协议》</a>
        </span>
      </label>
      <p v-if="errors.agree" class="field__msg">
        <AppIcon name="alert" :size="13" />{{ errors.agree }}
      </p>

      <div v-if="notice" class="alert" :class="`alert--${notice.type}`">
        <AppIcon :name="notice.type === 'ok' ? 'check' : 'alert'" :size="15" />
        <span>{{ notice.text }}</span>
      </div>

      <GlowButton :loading="loading" :disabled="!canSubmit || done" loading-text="签发身份卡中">
        提交登记
      </GlowButton>

      <p class="card-note">
        演示项目 · 数据仅保存在浏览器本地，不会上传至任何服务器<br />
        本页面为 <b>龙族</b> 同人风格作品，与原著版权方无关
      </p>
    </form>

    <template #foot>
      已持有身份卡？<router-link class="link-gold" to="/login">返回登录</router-link>
    </template>
  </AuthCard>
</template>

<style scoped>
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
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

@media (max-width: 560px) {
  .grid-2 {
    grid-template-columns: 1fr;
    gap: 17px;
  }
}
</style>
