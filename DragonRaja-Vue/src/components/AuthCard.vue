<script setup>
/**
 * 身份认证卡片 —— 鎏金描边 + 玻璃拟态 + 四角封印
 */
defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  badge: { type: String, default: '' },
  wide: { type: Boolean, default: false }
})
</script>

<template>
  <section class="auth-card" :class="{ 'is-wide': wide }">
    <span class="corner corner--tl" />
    <span class="corner corner--tr" />
    <span class="corner corner--bl" />
    <span class="corner corner--br" />

    <header class="auth-card__head">
      <p v-if="badge" class="eyebrow"><i />{{ badge }}</p>
      <h1 class="auth-title">{{ title }}</h1>
      <p v-if="subtitle" class="auth-sub">{{ subtitle }}</p>
    </header>

    <div v-if="$slots.tabs" class="auth-tabs"><slot name="tabs" /></div>

    <div class="auth-card__body"><slot /></div>

    <footer v-if="$slots.foot" class="auth-card__foot"><slot name="foot" /></footer>
  </section>
</template>
