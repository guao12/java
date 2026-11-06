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
import DragonEmblem from '@/components/DragonEmblem.vue'
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
    <!-- 背景层：只留网格 + 极淡徽记，比前台更冷静，但不再是纯黑空场 -->
    <div class="admin-login__bg" aria-hidden="true">
      <span class="admin-login__grid" />
      <DragonEmblem class="admin-login__seal" />
    </div>

    <!-- 顶部身份条：说明「你在哪一个系统里」，并给出回前台的出口 -->
    <header class="admin-login__bar">
      <div class="admin-login__brand">
        <DragonEmblem class="admin-login__mark" />
        <span class="admin-login__brand-text">
          <b>龙族读者论坛</b>
          <i>Admin Console</i>
        </span>
      </div>
      <router-link class="admin-login__back" to="/forum">
        <AppIcon name="arrow" :size="12" /> 返回论坛前台
      </router-link>
    </header>

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
            后台不开放注册，账号由站点管理员预置；
            <router-link class="link-gold" to="/forum">返回论坛前台</router-link>
          </p>
        </form>
      </AuthCard>
    </div>
  </div>
</template>

<style scoped>
.admin-login {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 86px 20px 44px;
  overflow: hidden;
  background:
    radial-gradient(760px circle at 16% -4%, rgba(216, 178, 90, 0.1), transparent 60%),
    radial-gradient(680px circle at 88% 104%, rgba(42, 111, 146, 0.14), transparent 62%),
    linear-gradient(180deg, var(--bg-1) 0%, var(--bg-0) 58%, #03050a 100%);
}

/* ---------- 背景层 ---------- */
.admin-login__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.admin-login__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(216, 178, 90, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(216, 178, 90, 0.05) 1px, transparent 1px);
  background-size: 68px 68px;
  -webkit-mask-image: radial-gradient(72% 62% at 50% 42%, #000 8%, transparent 76%);
  mask-image: radial-gradient(72% 62% at 50% 42%, #000 8%, transparent 76%);
}

/* 极淡法阵徽记：给空场一个中心，但不抢卡片的注意力 */
.admin-login__seal {
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(74vh, 74vw);
  height: min(74vh, 74vw);
  transform: translate(-50%, -50%);
  opacity: 0.06;
  animation: seal-spin 200s linear infinite;
}

@keyframes seal-spin {
  to {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

/* ---------- 顶部身份条 ---------- */
.admin-login__bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
}

.admin-login__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-login__mark {
  width: 30px;
  height: 30px;
  filter: drop-shadow(0 0 8px rgba(216, 178, 90, 0.35));
  opacity: 0.85;
}

.admin-login__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.admin-login__brand-text b {
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.18em;
  color: var(--text-2);
}

.admin-login__brand-text i {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--text-4);
}

.admin-login__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--text-3);
  transition: color 0.25s, border-color 0.25s;
}

.admin-login__back:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.45);
}

.admin-login__inner {
  position: relative;
  z-index: 1;
  width: min(440px, 100%);
}

@media (max-width: 560px) {
  .admin-login {
    padding: 78px 16px 36px;
  }

  .admin-login__bar {
    padding: 14px 16px;
  }

  .admin-login__brand-text b {
    font-size: 13px;
  }
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
