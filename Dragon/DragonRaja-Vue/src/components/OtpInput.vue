<script setup>
/**
 * 六格验证码输入器
 * - 逐格输入自动跳格，退格自动回跳
 * - 支持整串粘贴（自动拆分到各格）
 * - 支持左右方向键移动光标
 * - 填满时抛出 complete 事件
 */
import { nextTick, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  length: { type: Number, default: 6 },
  disabled: { type: Boolean, default: false },
  invalid: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'complete'])

const inputs = ref([])
const digits = ref(Array.from({ length: props.length }, () => ''))

/* 外部 v-model 变化时同步回内部（如一键填入演示验证码） */
watch(
  () => props.modelValue,
  (v) => {
    const arr = String(v || '')
      .replace(/\D/g, '')
      .split('')
      .slice(0, props.length)
    const next = Array.from({ length: props.length }, (_, i) => arr[i] || '')
    if (next.join('|') !== digits.value.join('|')) digits.value = next
  },
  { immediate: true }
)

/** 把 DOM 的值强行同步回状态，避免「格内已有值再输入」时残留两位 */
function sync(i) {
  const el = inputs.value[i]
  if (el) el.value = digits.value[i] || ''
}

function emitValue() {
  const v = digits.value.join('')
  emit('update:modelValue', v)
  if (v.length === props.length) emit('complete', v)
}

function focus(i) {
  nextTick(() => {
    const el = inputs.value[Math.max(0, Math.min(i, props.length - 1))]
    if (!el) return
    el.focus()
    try {
      el.setSelectionRange(0, 1)
    } catch {
      /* 部分浏览器不支持，忽略 */
    }
  })
}

/** 从 startAt 起顺序写入一串数字 */
function push(text, startAt) {
  let idx = startAt
  for (const ch of text) {
    if (idx >= props.length) break
    digits.value[idx] = ch
    idx += 1
  }
  emitValue()
  focus(idx)
}

function onInput(i, e) {
  const raw = e.target.value.replace(/\D/g, '')
  sync(i)
  if (!raw) {
    digits.value[i] = ''
    emitValue()
    return
  }
  push(raw, i)
}

function onKeydown(i, e) {
  if (e.key === 'Backspace') {
    e.preventDefault()
    if (digits.value[i]) {
      digits.value[i] = ''
      sync(i)
      emitValue()
    } else if (i > 0) {
      digits.value[i - 1] = ''
      sync(i - 1)
      emitValue()
      focus(i - 1)
    }
  } else if (e.key === 'ArrowLeft' && i > 0) {
    e.preventDefault()
    focus(i - 1)
  } else if (e.key === 'ArrowRight' && i < props.length - 1) {
    e.preventDefault()
    focus(i + 1)
  }
}

function onPaste(e) {
  const text = (e.clipboardData?.getData('text') || '').replace(/\D/g, '')
  if (!text) return
  e.preventDefault()
  const active = inputs.value.indexOf(document.activeElement)
  push(text, active < 0 ? 0 : active)
}

defineExpose({ focus })
</script>

<template>
  <div class="otp" :class="{ 'is-invalid': invalid, 'is-disabled': disabled }">
    <input
      v-for="(d, i) in digits"
      :key="i"
      :ref="(el) => (inputs[i] = el)"
      class="otp__cell"
      :class="{ 'is-filled': !!d }"
      type="text"
      inputmode="numeric"
      autocomplete="one-time-code"
      maxlength="1"
      :value="d"
      :disabled="disabled"
      :aria-label="`验证码第 ${i + 1} 位`"
      @focus="$event.target.select()"
      @input="onInput(i, $event)"
      @keydown="onKeydown(i, $event)"
      @paste="onPaste"
    />
  </div>
</template>

<style scoped>
.otp {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}

.otp__cell {
  width: 100%;
  height: 50px;
  text-align: center;
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 600;
  color: var(--gold-lt);
  background: rgba(4, 7, 13, 0.66);
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  outline: none;
  caret-color: var(--gold);
  transition: border-color 0.22s, box-shadow 0.22s, background 0.22s, transform 0.22s;
}

.otp__cell::placeholder {
  color: var(--text-4);
}

.otp__cell:hover:not(:disabled) {
  border-color: rgba(216, 178, 90, 0.32);
}

.otp__cell:focus {
  border-color: var(--gold);
  background: rgba(6, 10, 18, 0.9);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.14), 0 10px 26px -14px var(--gold-glow);
  transform: translateY(-1px);
}

.otp__cell.is-filled {
  border-color: rgba(216, 178, 90, 0.5);
  color: var(--gold);
  text-shadow: 0 0 14px rgba(216, 178, 90, 0.45);
}

.otp.is-invalid .otp__cell {
  border-color: var(--err);
  box-shadow: 0 0 0 3px rgba(239, 107, 98, 0.12);
}

.otp.is-disabled .otp__cell {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .otp {
    gap: 6px;
  }
  .otp__cell {
    height: 46px;
    font-size: 18px;
  }
}
</style>
