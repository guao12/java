<script setup>
defineProps({
  label: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  loadingText: { type: String, default: '校验中' },
  disabled: { type: Boolean, default: false },
  variant: { type: String, default: 'primary' }, // primary | ghost
  type: { type: String, default: 'submit' } // submit | button
})
</script>

<template>
  <button
    class="btn"
    :class="`btn--${variant}`"
    :type="type"
    :disabled="disabled || loading"
  >
    <span v-if="loading" class="btn__spinner" />
    <template v-if="loading">{{ loadingText }}</template>
    <template v-else>
      <slot>{{ label }}</slot>
    </template>
  </button>
</template>
