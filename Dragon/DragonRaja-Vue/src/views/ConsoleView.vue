<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthCard from '@/components/AuthCard.vue'
import GlowButton from '@/components/GlowButton.vue'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import { authApi } from '@/api/auth'
import { CODE_UNAUTHORIZED } from '@/api/http'
import { clearSession, getUser } from '@/utils/auth'

const router = useRouter()

/** 先用本地缓存渲染，避免白屏；随后用令牌拉取最新数据 */
const profile = ref(getUser())
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    profile.value = await authApi.profile()
  } catch (e) {
    if (e.code === CODE_UNAUTHORIZED) {
      router.replace({ path: '/login', query: { redirect: '/console' } })
      return
    }
    error.value = e.message
  } finally {
    loading.value = false
  }
}

/** 回到龙文档案馆首页 */
function goHome() {
  router.push('/home')
}

async function logout() {
  try {
    await authApi.logout()
  } catch {
    /* 令牌可能已过期，本地清理即可 */
  }
  clearSession()
  router.replace('/login')
}

onMounted(load)
</script>

<template>
  <AuthCard
    wide
    badge="Cassell Intranet"
    :title="profile ? `${profile.codename} 的身份卡` : '学院内网'"
    subtitle="令牌鉴权通过，以下信息来自 MySQL · dr_user"
  >
    <div v-if="loading && !profile" class="state">
      <span class="state__spinner" />
      正在调取身份档案…
    </div>

    <template v-else>
      <div class="id-card">
        <DragonEmblem class="id-card__mark" />
        <div class="id-card__main">
          <p class="id-card__name">{{ profile?.codename }}</p>
          <p class="id-card__account">
            <AppIcon name="id" :size="13" />
            {{ profile?.account }}
          </p>
        </div>
        <span class="id-card__status"><i class="dot-live" />在册</span>
      </div>

      <dl class="info-list">
        <div class="info-row">
          <dt>验证方式</dt>
          <dd>系统验证码核验</dd>
        </div>
        <div class="info-row">
          <dt>在校身份</dt>
          <dd>{{ profile?.identityName }}</dd>
        </div>
        <div class="info-row">
          <dt>言灵</dt>
          <dd>{{ profile?.spiritName }}</dd>
        </div>
        <div class="info-row">
          <dt>累计登录</dt>
          <dd>{{ profile?.loginCount ?? 0 }} 次</dd>
        </div>
        <div class="info-row">
          <dt>最近登录</dt>
          <dd>{{ profile?.lastLoginAt || '——' }}</dd>
        </div>
        <div class="info-row">
          <dt>登记时间</dt>
          <dd>{{ profile?.createdAt || '——' }}</dd>
        </div>
      </dl>

      <div v-if="error" class="alert alert--err">
        <AppIcon name="alert" :size="15" />
        <span>{{ error }}</span>
      </div>

      <div class="actions">
        <GlowButton variant="ghost" type="button" :disabled="loading" @click="load">
          <AppIcon name="refresh" :size="16" />
          {{ loading ? '刷新中…' : '刷新档案' }}
        </GlowButton>
        <GlowButton variant="ghost" type="button" @click="goHome">
          <AppIcon name="home" :size="16" />
          返回首页
        </GlowButton>
        <GlowButton type="button" @click="logout">
          <AppIcon name="logout" :size="16" />
          退出登录
        </GlowButton>
      </div>
    </template>

    <template #foot>
      需要换一张身份卡？<router-link class="link-gold" to="/login">返回登录页</router-link>
    </template>
  </AuthCard>
</template>

<style scoped>
.state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 34px 0;
  font-size: 13px;
  color: var(--text-3);
}
.state__spinner {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  border: 2px solid rgba(216, 178, 90, 0.25);
  border-top-color: var(--gold);
  animation: spin 0.7s linear infinite;
}

.id-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  background: linear-gradient(140deg, rgba(216, 178, 90, 0.09), rgba(8, 12, 22, 0.5));
}
.id-card__mark {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
}
.id-card__main {
  flex: 1;
  min-width: 0;
}
.id-card__name {
  font-family: var(--font-serif);
  font-size: 16px;
  letter-spacing: 0.1em;
  color: var(--text);
}
.id-card__account {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
  font-family: var(--font-mono);
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: var(--gold-dp);
}
.id-card__status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.16em;
  color: var(--ok);
}

.info-list {
  margin: 18px 0 0;
  display: flex;
  flex-direction: column;
}
.info-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 2px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.07);
}
.info-row:last-child {
  border-bottom: 0;
}
.info-row dt {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--text-4);
  white-space: nowrap;
}
.info-row dd {
  margin: 0;
  font-size: 13px;
  color: var(--text);
  text-align: right;
  word-break: break-all;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
.actions > * {
  flex: 1;
}
</style>
