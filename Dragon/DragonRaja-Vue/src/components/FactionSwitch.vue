<script setup>
/**
 * 阵营切换胶囊（卡塞尔学院 / 蛇岐八家）。
 *
 * longzu 原型里这是纯装饰的页签：切了只换背景图。
 * 这里升级为真正的受控组件 —— v-model 绑阵营 key，父级据此切换
 * 背景图、标题、公告，并把选择记在本地，下次进来还是同一阵营。
 *
 * 键盘可操作（←/→ 切换），屏幕阅读器按 tablist 语义播报。
 */
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, required: true },
  factions: { type: Array, required: true }
})

const emit = defineEmits(['update:modelValue'])

const rootRef = ref(null)

const activeIndex = computed(() =>
  Math.max(
    0,
    props.factions.findIndex((f) => f.key === props.modelValue)
  )
)

function select(key) {
  if (key === props.modelValue) return
  emit('update:modelValue', key)
}

/** ←/→ 切换，并把焦点移到新选中的页签（符合 tablist 交互惯例） */
function onKeydown(e) {
  if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return
  e.preventDefault()
  const len = props.factions.length
  const dir = e.key === 'ArrowRight' ? 1 : -1
  const next = (activeIndex.value + dir + len) % len
  select(props.factions[next].key)
  requestAnimationFrame(() => {
    rootRef.value?.querySelectorAll('[role="tab"]')[next]?.focus()
  })
}
</script>

<template>
  <div ref="rootRef" class="faction" role="tablist" aria-label="阵营切换" @keydown="onKeydown">
    <button
      v-for="f in factions"
      :key="f.key"
      class="faction__pill"
      :class="{ 'is-on': f.key === modelValue }"
      type="button"
      role="tab"
      :aria-selected="f.key === modelValue"
      :tabindex="f.key === modelValue ? 0 : -1"
      @click="select(f.key)"
    >
      {{ f.name }}
    </button>
  </div>
</template>

<style scoped>
.faction {
  display: flex;
  flex: 1;
  min-width: 0;
  background: rgba(12, 18, 32, 0.5);
  border: 1px solid var(--line-soft);
  border-radius: 28px;
  overflow: hidden;
}

.faction__pill {
  flex: 1;
  padding: 9px 12px;
  border: 0;
  background: transparent;
  color: var(--text-4);
  font-family: var(--font-serif);
  font-size: 13.5px;
  letter-spacing: 2px;
  font-weight: 400;
  cursor: pointer;
  position: relative;
  user-select: none;
  transition: color 0.35s ease, background 0.35s ease;
}

.faction__pill:focus-visible {
  outline: 1px solid var(--gold);
  outline-offset: -2px;
}

.faction__pill:hover {
  color: var(--text-2);
}

.faction__pill.is-on {
  color: #dce2ee;
  background: rgba(22, 32, 55, 0.7);
}

.faction__pill.is-on::after {
  content: '';
  position: absolute;
  bottom: 1px;
  left: 25%;
  right: 25%;
  height: 1.5px;
  background: linear-gradient(90deg, transparent, rgba(185, 210, 245, 0.5), transparent);
}

@media (min-width: 768px) {
  .faction {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    /* 绝对定位后 flex-basis 不再生效，宽度要显式给 */
    flex: none;
    width: min(400px, 46vw);
  }

  .faction__pill {
    padding: 10px 12px;
    font-size: 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .faction__pill {
    transition: none;
  }
}
</style>
