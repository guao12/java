<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthCard from '@/components/AuthCard.vue'
import FormField from '@/components/FormField.vue'
import GlowButton from '@/components/GlowButton.vue'
import AppIcon from '@/components/AppIcon.vue'
import { authApi } from '@/api/auth'
import { lastAccount, rememberAccount, saveSession } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const form = reactive({ account: '', password: '', remember: true })

/** 后端连通状态：null 检测中，true 已连接，false 未连接 */
const backendOnline = ref(null)

onMounted(async () => {
  form.account = lastAccount()
  try {
    await authApi.dictionaries()
    backendOnline.value = true
  } catch {
    backendOnline.value = false
  }
})
const errors = reactive({ account: '', password: '' })
const touched = reactive({ account: false, password: false })
const loading = ref(false)
const notice = ref(null) // { type: 'err' | 'ok' | 'info', text }

const EMAIL_RE = /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/
const CODE_RE = /^[A-Za-z0-9_-]{4,20}$/

function validateAccount() {
  const v = form.account.trim()
  if (!v) return '请输入学号 / 邮箱 / 执行部编号'
  if (v.includes('@')) return EMAIL_RE.test(v) ? '' : '邮箱格式不正确'
  return CODE_RE.test(v) ? '' : '编号应为 4–20 位字母、数字或下划线'
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

function fillDemo() {
  form.account = 'luffy.cassell@cassell.edu'
  form.password = 'Dragon@2026'
  errors.account = ''
  errors.password = ''
  touched.account = true
  touched.password = true
  notice.value = { type: 'info', text: '已载入演示身份卡（密钥 Dragon@2026），点击「进入学院」即可登录。' }
}

async function onSubmit() {
  check('account')
  check('password')
  notice.value = null
  if (errors.account || errors.password) {
    notice.value = { type: 'err', text: '认证信息不完整，请检查被标记的字段。' }
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
    notice.value = {
      type: 'ok',
      text: `认证通过 · 欢迎归来，${data.user.codename}。血统扫描正常，正在接入学院内网…`
    }
    window.setTimeout(() => {
      router.push(String(route.query.redirect || '/console'))
    }, 900)
  } catch (e) {
    notice.value = { type: 'err', text: e.message }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthCard
    badge="Cassell College · Access"
    title="身份认证"
    subtitle="出示你的身份卡与言灵密钥，进入学院内部网络"
  >
    <template #tabs>
      <router-link to="/login" active-class="active">登录</router-link>
      <router-link to="/register" active-class="active">注册</router-link>
    </template>

    <form class="form" novalidate @submit.prevent="onSubmit">
      <FormField
        v-model="form.account"
        label="学号 / 邮箱"
        icon="id"
        placeholder="例：Luffy.Cassell 或 name@cassell.edu"
        autocomplete="username"
        :error="touched.account ? errors.account : ''"
        :valid="accountValid"
        @blur="check('account')"
      />

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

      <div class="form-row">
        <label class="check">
          <input v-model="form.remember" type="checkbox" />
          <span class="check__box" />
          <span>记住身份卡</span>
        </label>
        <router-link class="link-gold" to="/forgot">忘记密钥？</router-link>
      </div>

      <div v-if="notice" class="alert" :class="`alert--${notice.type}`">
        <AppIcon :name="notice.type === 'ok' ? 'check' : notice.type === 'err' ? 'alert' : 'info'" :size="15" />
        <span>{{ notice.text }}</span>
      </div>

      <GlowButton :loading="loading" :disabled="!canSubmit" loading-text="校验密钥中">进入学院</GlowButton>

      <div class="divider">或</div>

      <GlowButton variant="ghost" type="button" :disabled="loading" @click="fillDemo">
        <AppIcon name="ticket" :size="16" />
        使用演示身份卡
      </GlowButton>

      <p class="card-note">
        后端通道 ·
        <b v-if="backendOnline === true">已连接（127.0.0.1:8081）</b>
        <b v-else-if="backendOnline === false" class="is-offline">未连接，请先启动 Spring Boot 服务</b>
        <b v-else>检测中…</b>
      </p>
    </form>

    <template #foot>
      还没有身份卡？<router-link class="link-gold" to="/register">前往新生登记</router-link>
    </template>
  </AuthCard>
</template>
