<script setup>
/**
 * 后台管理系统 · 登录页。
 * 只有登录功能，没有注册入口 —— 管理员账号由后端 SQL 脚本预置（admin / Dragon@2026）。
 * 登录成功后进入 /admin（言灵图鉴数据管理）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import AuthCard from '@/components/AuthCard.vue'
import FormField from '@/components/FormField.vue'
import GlowButton from '@/components/GlowButton.vue'
import { adminApi } from '@/api/admin'
import { saveAdminSession } from '@/utils/adminAuth'

const router = useRouter()

const form = reactive({
  username: localStorage.getItem('dr:admin:last') || '',
  password: '',
  remember: !!localStorage.getItem('dr:admin:last')
})

const submitting = ref(false)
const alertMsg = ref('')
const touched = reactive({ username: false, password: false })

const errors = computed(() => ({
  username: touched.username && !form.username.trim() ? '请输入管理员账号' : '',
  password: touched.password && !form.password ? '请输入登录密码' : ''
}))

const canSubmit = computed(
  () => !!form.username.trim() && !!form.password && !submitting.value && !alertMsg.value
)

onMounted(() => {
  document.documentElement.style.colorScheme = 'dark'
  document.title = '后台管理系统 | 卡塞尔学院'
})

async function onSubmit() {
  touched.username = true
  touched.password = true
  if (!form.username.trim() || !form.password || submitting.value) return

  submitting.value = true
  alertMsg.value = ''
  try {
    const data = await adminApi.login(form.username.trim(), form.password, form.remember)
    saveAdminSession(data, form.remember)
    if (form.remember) localStorage.setItem('dr:admin:last', form.username.trim())
    else localStorage.removeItem('dr:admin:last')
    router.replace('/admin')
  } catch (e) {
    alertMsg.value = e.message || '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="admin-login">
    <div class="admin-login__inner">
      <AuthCard
        badge="Cassell College · Admin Console"
        title="后台管理系统"
        subtitle="执行部管理员通道 · 仅限授权人员出入"
      >
        <form class="form" novalidate @submit.prevent="onSubmit">
          <div v-if="alertMsg" class="alert alert--err">
            <AppIcon name="alert" :size="15" />
            <span>{{ alertMsg }}</span>
          </div>

          <FormField
            v-model="form.username"
            label="管理员账号"
            icon="id"
            placeholder="例：admin"
            autocomplete="username"
            :error="errors.username"
            @blur="touched.username = true"
            @enter="onSubmit"
          />

          <FormField
            v-model="form.password"
            label="登录密码"
            icon="lock"
            type="password"
            placeholder="请输入登录密码"
            autocomplete="current-password"
            :error="errors.password"
            @blur="touched.password = true"
            @enter="onSubmit"
          />

          <label class="remember">
            <input v-model="form.remember" type="checkbox" />
            <span>记住管理员账号（7 天内免登录）</span>
          </label>

          <GlowButton type="submit" class="submit" :disabled="!canSubmit">
            <AppIcon name="shield" :size="16" />
            {{ submitting ? '正在核验管理员身份…' : '进入后台管理系统' }}
          </GlowButton>

          <p class="foot-note">
            后台不开放注册，账号由学院最高会议预置；
            <router-link class="link-gold" to="/home">返回学院前台</router-link>
          </p>
        </form>
      </AuthCard>
    </div>
  </div>
</template>

<style scoped>
.admin-login {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 40px 20px;
  background:
    radial-gradient(900px circle at 50% 8%, rgba(216, 178, 90, 0.07), transparent 62%),
    radial-gradient(700px circle at 88% 92%, rgba(179, 39, 45, 0.06), transparent 60%),
    var(--bg-0);
}

.admin-login__inner {
  width: min(440px, 100%);
}

.form {
  display: grid;
  gap: 16px;
}

.alert {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px 14px;
  border-radius: var(--r-md);
  font-size: 12.5px;
  letter-spacing: 0.04em;
  line-height: 1.6;
}

.alert--err {
  color: var(--blood-lt);
  border: 1px solid rgba(179, 39, 45, 0.45);
  background: rgba(179, 39, 45, 0.1);
}

.remember {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12.5px;
  color: var(--text-3);
  cursor: pointer;
  user-select: none;
}

.remember input {
  accent-color: var(--gold);
  width: 15px;
  height: 15px;
  cursor: pointer;
}

.submit {
  width: 100%;
  justify-content: center;
  margin-top: 2px;
}

.foot-note {
  margin: 2px 0 0;
  text-align: center;
  font-size: 11.5px;
  letter-spacing: 0.05em;
  color: var(--text-4);
  line-height: 1.8;
}

.link-gold {
  color: var(--gold-lt);
  text-decoration: none;
  border-bottom: 1px dashed rgba(216, 178, 90, 0.5);
}

.link-gold:hover {
  color: var(--gold);
}
</style>
