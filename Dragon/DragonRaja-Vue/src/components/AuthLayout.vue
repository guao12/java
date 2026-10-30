<script setup>
/**
 * 认证页公共外壳：品牌栏 / 主体舞台 / 页脚
 */
import { onMounted } from 'vue'
import RuneBackground from '@/components/RuneBackground.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'

const year = new Date().getFullYear()

onMounted(() => {
  document.documentElement.style.colorScheme = 'dark'
})
</script>

<template>
  <div class="wrap">
    <RuneBackground />

    <div class="app-shell">
      <header class="brand-bar">
        <div class="brand">
          <DragonEmblem class="brand__mark" />
          <div class="brand__text">
            <span class="brand__cn">卡塞尔学院</span>
            <span class="brand__en">Cassell College · Secret Archive</span>
          </div>
        </div>
        <div class="brand-bar__meta">
          <span class="dot-live" />
          龙文密钥通道 · 已加密
        </div>
      </header>

      <main class="stage">
        <slot />
      </main>

      <footer class="page-foot">
        <span>© {{ year }} CASSELL COLLEGE · 龙族身份认证系统 v1.0</span>
        <nav>
          <router-link to="/login">登录</router-link>
          <router-link to="/register">注册</router-link>
          <router-link to="/forgot">找回密钥</router-link>
        </nav>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  position: relative;
  min-height: 100%;
}
</style>
