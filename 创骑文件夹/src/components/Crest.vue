<template>
  <div ref="root" class="crest" :data-form="formKey"
       role="button" tabindex="0"
       :aria-label="cfg.alt" title="点击释放骑士之光"
       @click="burst"
       @keydown.enter.prevent="burst"
       @keydown.space.prevent="burst">
    <span class="crest-ripple r1"></span>
    <span class="crest-ripple r2"></span>

    <div class="crest-disc">
      <span class="crest-ticks"></span>
      <span class="crest-gear"></span>
      <span class="crest-face">
        <img :src="cfg.img" :alt="cfg.alt" draggable="false" />
        <span class="crest-vig"></span>
        <span class="crest-sheen"></span>
      </span>
      <span class="crest-edge"></span>
      <span class="crest-sweep"></span>
      <span class="crest-gem"></span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { CRESTS } from '../data/forms.js'

const props = defineProps({
  formKey: { type: String, required: true }
})

const root = ref(null)
const cfg = computed(() => CRESTS[props.formKey] || Object.values(CRESTS)[0])

// 释放一圈"骑士之光"：双重扩散环 + 高光扫过 + 纹章震颤
function burst() {
  const el = root.value
  if (!el) return
  el.classList.remove('burst')
  void el.offsetWidth          // 强制回流，让动画可以连续重放
  el.classList.add('burst')
}

// 形态切换：纹章翻转入场
watch(() => props.formKey, () => {
  const el = root.value
  if (!el) return
  el.classList.remove('swap')
  void el.offsetWidth
  el.classList.add('swap')
})

// 供父组件在核心判定 / 变身成功时调用
defineExpose({ spark: burst })
</script>
