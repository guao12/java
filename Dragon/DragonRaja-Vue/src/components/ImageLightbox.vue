<script setup>
/**
 * 图片放大查看（灯箱）
 *
 * 交互：
 *  - 点击遮罩 / 右上关闭按钮 / Esc → 关闭
 *  - 滚轮、工具栏 ± 按钮 → 以光标为锚点缩放（1x ~ maxScale）
 *  - 放大后按住拖拽平移；双击在「适应窗口 / 2 倍」之间切换
 *  - 打开时锁定背景滚动，关闭后原样恢复
 *
 * 用 Teleport 挂到 body，避免被父级容器（overflow / transform / z-index）裁剪。
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import AppIcon from '@/components/AppIcon.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  /** 原图地址 */
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  /** 顶部标题（如「言灵档案 · 君焰」） */
  title: { type: String, default: '' },
  /** 底部说明文字 */
  caption: { type: String, default: '' },
  maxScale: { type: Number, default: 4 }
})
const emit = defineEmits(['update:open'])

const MIN_SCALE = 1
const scale = ref(1)
const off = ref({ x: 0, y: 0 })
const failed = ref(false)
const stage = ref(null)
const dragging = ref(false)

const zoomed = computed(() => scale.value > 1.001)
const scaleText = computed(() => `${Math.round(scale.value * 100)}%`)
const imgStyle = computed(() => ({
  transform: `translate3d(${off.value.x}px, ${off.value.y}px, 0) scale(${scale.value})`
}))

function clampScale(v) {
  return Math.min(props.maxScale, Math.max(MIN_SCALE, Math.round(v * 100) / 100))
}

function reset() {
  scale.value = MIN_SCALE
  off.value = { x: 0, y: 0 }
}

function close() {
  emit('update:open', false)
}

/** 以屏幕坐标 (clientX, clientY) 为锚点缩放，锚点下的像素保持不动 */
function zoomAt(nextScale, clientX, clientY) {
  const next = clampScale(nextScale)
  const prev = scale.value
  if (next === prev) return
  if (next === MIN_SCALE) {
    reset()
    return
  }
  const rect = stage.value?.getBoundingClientRect()
  const cx = rect ? clientX - (rect.left + rect.width / 2) : 0
  const cy = rect ? clientY - (rect.top + rect.height / 2) : 0
  // transform: translate(t) scale(s)，锚点不动 → t' = t - (c - t) * (k - 1)
  const k = next / prev - 1
  off.value = {
    x: off.value.x - (cx - off.value.x) * k,
    y: off.value.y - (cy - off.value.y) * k
  }
  scale.value = next
}

/** 点按钮缩放：以视口中心为锚点 */
function stepZoom(delta) {
  zoomAt(scale.value + delta, window.innerWidth / 2, window.innerHeight / 2)
}

function onWheel(e) {
  // 每格按 12% 递进，触控板的小增量也能获得可见反馈
  const delta = e.deltaY > 0 ? -0.12 : 0.12
  zoomAt(scale.value + delta * Math.min(3, Math.abs(e.deltaY) / 40 || 1), e.clientX, e.clientY)
}

function onDblClick(e) {
  if (zoomed.value) reset()
  else zoomAt(2, e.clientX, e.clientY)
}

/* ---------------- 拖拽平移 ---------------- */
let drag = null

function onPointerDown(e) {
  if (!zoomed.value) return
  drag = { x: e.clientX, y: e.clientY, ox: off.value.x, oy: off.value.y }
  dragging.value = true
  e.currentTarget.setPointerCapture?.(e.pointerId)
}

function onPointerMove(e) {
  if (!drag) return
  off.value = { x: drag.ox + (e.clientX - drag.x), y: drag.oy + (e.clientY - drag.y) }
}

function onPointerUp(e) {
  if (!drag) return
  drag = null
  dragging.value = false
  e.currentTarget.releasePointerCapture?.(e.pointerId)
}

/* ---------------- 键盘 / 滚动锁定 ---------------- */
function onKeydown(e) {
  if (!props.open) return
  if (e.key === 'Escape') {
    e.preventDefault()
    close()
  } else if (e.key === '+' || e.key === '=') {
    stepZoom(0.25)
  } else if (e.key === '-' || e.key === '_') {
    stepZoom(-0.25)
  } else if (e.key === '0') {
    reset()
  }
}

let prevOverflow = ''

function lockScroll(on) {
  if (on) {
    prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = prevOverflow || ''
  }
}

watch(
  () => props.open,
  (v) => {
    lockScroll(v)
    if (v) {
      reset()
      failed.value = false
    }
  },
  { immediate: true }
)

window.addEventListener('keydown', onKeydown)
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  lockScroll(false)
})
</script>

<template>
  <Teleport to="body">
    <transition name="lb">
      <div
        v-if="open && src"
        class="lb"
        role="dialog"
        aria-modal="true"
        :aria-label="title || alt || '图片查看'"
        @click.self="close"
      >
        <!-- 顶栏 -->
        <header class="lb__bar" @click.stop>
          <span class="lb__title">{{ title }}</span>
          <span v-if="caption" class="lb__cap-top">{{ caption }}</span>

          <div class="lb__tools">
            <button
              type="button"
              class="lb__btn"
              title="缩小（-）"
              aria-label="缩小"
              :disabled="!zoomed"
              @click="stepZoom(-0.25)"
            >
              <AppIcon name="minus" :size="15" />
            </button>
            <button type="button" class="lb__scale" title="还原（0）" @click="reset">
              {{ scaleText }}
            </button>
            <button
              type="button"
              class="lb__btn"
              title="放大（+）"
              aria-label="放大"
              :disabled="scale >= maxScale"
              @click="stepZoom(0.25)"
            >
              <AppIcon name="plus" :size="15" />
            </button>
            <button type="button" class="lb__btn lb__btn--close" title="关闭（Esc）" @click="close">
              <AppIcon name="close" :size="16" />
            </button>
          </div>
        </header>

        <!-- 图像舞台 -->
        <div
          ref="stage"
          class="lb__stage"
          :class="{ 'is-dragging': dragging, 'is-zoomed': zoomed }"
          @click.self="close"
          @wheel.prevent="onWheel"
        >
          <img
            class="lb__img"
            :src="src"
            :alt="alt"
            :style="imgStyle"
            draggable="false"
            @error="failed = true"
            @dblclick="onDblClick"
            @pointerdown="onPointerDown"
            @pointermove="onPointerMove"
            @pointerup="onPointerUp"
            @pointercancel="onPointerUp"
          />

          <p v-if="failed" class="lb__err">原图加载失败，请稍后重试</p>
        </div>

        <p class="lb__hint">
          <span>滚轮缩放 · 双击 2 倍 · 放大后拖拽平移 · Esc 关闭</span>
        </p>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
.lb {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  background: rgba(3, 5, 10, 0.94);
  backdrop-filter: blur(18px) saturate(120%);
  -webkit-backdrop-filter: blur(18px) saturate(120%);
  animation: lb-in 0.26s ease both;
}

.lb__bar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid rgba(216, 178, 90, 0.22);
  background: linear-gradient(180deg, rgba(10, 13, 22, 0.9), rgba(10, 13, 22, 0.4));
}

.lb__title {
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lb__cap-top {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lb__tools {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
}

.lb__btn,
.lb__scale {
  display: grid;
  place-items: center;
  height: 30px;
  min-width: 30px;
  padding: 0 9px;
  border-radius: 8px;
  border: 1px solid rgba(216, 178, 90, 0.28);
  background: rgba(216, 178, 90, 0.07);
  color: var(--gold-lt);
  cursor: pointer;
  transition: background 0.22s, border-color 0.22s, opacity 0.22s;
}

.lb__scale {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  min-width: 58px;
}

.lb__btn:hover:not(:disabled),
.lb__scale:hover {
  background: rgba(216, 178, 90, 0.18);
  border-color: var(--gold);
}

.lb__btn:disabled {
  opacity: 0.34;
  cursor: default;
}

.lb__btn--close {
  border-color: rgba(239, 107, 98, 0.4);
  background: rgba(179, 39, 45, 0.16);
  color: #ffd9d6;
}

.lb__btn--close:hover {
  background: rgba(179, 39, 45, 0.32);
  border-color: rgba(239, 107, 98, 0.8);
}

.lb__stage {
  position: relative;
  flex: 1;
  min-height: 0;
  display: grid;
  place-items: center;
  padding: 22px;
  overflow: hidden;
}

.lb__img {
  max-width: min(92vw, 1400px);
  max-height: 100%;
  object-fit: contain;
  border-radius: 10px;
  box-shadow: 0 30px 80px -30px rgba(0, 0, 0, 0.95), 0 0 0 1px rgba(216, 178, 90, 0.22);
  cursor: zoom-in;
  user-select: none;
  -webkit-user-drag: none;
  will-change: transform;
}

.lb__stage.is-zoomed .lb__img {
  cursor: grab;
}

.lb__stage.is-dragging .lb__img {
  cursor: grabbing;
}

.lb__err {
  position: absolute;
  bottom: 16px;
  font-size: 12.5px;
  letter-spacing: 0.1em;
  color: #ffd9d6;
}

.lb__hint {
  flex: 0 0 auto;
  padding: 10px 18px 14px;
  text-align: center;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--text-4);
}

@keyframes lb-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.lb-enter-active,
.lb-leave-active {
  transition: opacity 0.24s ease;
}

.lb-enter-from,
.lb-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .lb__cap-top {
    display: none;
  }

  .lb__hint span {
    font-size: 9px;
  }
}
</style>
