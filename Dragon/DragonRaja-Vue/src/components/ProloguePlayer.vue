<script setup>
/**
 * 序章播放器：逐字打字机 + 进度条。
 *
 * 从 longzu 原型重构：
 *  - 原型用 `setTimeout` 给每个字符挂一个定时器（100 字就是 100 个定时器），
 *    这里改成一个 25ms 的 interval 推进「已显示字数」，只占一个定时器
 *  - 原型每次渲染都重建 innerHTML；这里交给 Vue 的响应式，切换段落时自然重绘
 *  - 补齐 prefers-reduced-motion：该偏好下直接整段呈现，不做逐字动画
 *
 * 交互沿用视觉小说的通行做法：
 *  正在打字时点击 → 立刻显示完整段落；已显示完 → 进入下一段；最后一段 → 抛出 finish
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { imgSrc, imgSrcset } from '@/data/portal'

const props = defineProps({
  /** 段落数组；给空数组兜底，漏传时渲染空序章而不是整页白屏 */
  lines: { type: Array, default: () => [] },
  /** 每个字符的出现间隔（毫秒），与原型一致为 25 */
  charDelay: { type: Number, default: 25 }
})

const emit = defineEmits(['finish'])

const index = ref(0)
const revealed = ref(0)
let timer = null
let reduced = false

const current = computed(() => props.lines[index.value] || '')
const chars = computed(() => Array.from(current.value))
const isTyping = computed(() => revealed.value < chars.value.length)
const isLast = computed(() => index.value >= props.lines.length - 1)
const progress = computed(() =>
  props.lines.length ? ((index.value + 1) / props.lines.length) * 100 : 0
)

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function typeCurrent() {
  stopTimer()
  if (reduced) {
    revealed.value = chars.value.length
    return
  }
  revealed.value = 0
  timer = setInterval(() => {
    if (revealed.value >= chars.value.length) {
      stopTimer()
      return
    }
    revealed.value += 1
  }, props.charDelay)
}

/** 供父级（键盘事件）调用 */
function advance() {
  if (isTyping.value) {
    revealed.value = chars.value.length
    stopTimer()
    return
  }
  if (isLast.value) {
    emit('finish')
    return
  }
  index.value += 1
  typeCurrent()
}

function skip() {
  emit('finish')
}

onMounted(() => {
  reduced =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  typeCurrent()
})

onBeforeUnmount(stopTimer)

defineExpose({ advance, skip })
</script>

<template>
  <section class="prologue" aria-label="序章">
    <div class="prologue__relief" aria-hidden="true">
      <img
        :src="imgSrc('story-relief', 768)"
        :srcset="imgSrcset('story-relief', [1024, 768, 512])"
        sizes="100vw"
        alt=""
        decoding="async"
        fetchpriority="low"
      />
    </div>

    <button class="prologue__skip" type="button" @click="skip">跳过序章</button>

    <div class="prologue__progress" role="progressbar" :aria-valuenow="Math.round(progress)" aria-valuemin="0" aria-valuemax="100">
      <div class="prologue__track">
        <div class="prologue__fill" :style="{ width: progress + '%' }" />
      </div>
      <div class="prologue__orn">
        <span class="d" />
        <span class="d" />
      </div>
    </div>

    <div class="prologue__body" @click="advance">
      <p class="prologue__text">
        <span
          v-for="(ch, i) in chars"
          :key="`${index}-${i}`"
          class="ch"
          :class="{ 'is-on': i < revealed }"
        >{{ ch }}</span>
      </p>
      <div v-if="!isTyping" class="prologue__next">
        {{ isLast ? '进入学院' : '点击继续' }}<span class="chv">›</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.prologue {
  position: absolute;
  inset: 0;
  z-index: 10;
}

/* ---- 浮雕背景 ---- */
.prologue__relief {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 62%;
  overflow: hidden;
  background: linear-gradient(180deg, #151e30 0%, #101828 50%, var(--bg-1) 100%);
  z-index: 0;
}

.prologue__relief img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  filter: brightness(0.7) contrast(1.1);
}

.prologue__relief::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(8, 12, 22, 0.15) 0%,
    rgba(8, 12, 22, 0.3) 60%,
    rgba(8, 12, 22, 0.85) 90%,
    var(--bg-1) 100%
  );
}

.prologue__skip {
  position: absolute;
  top: 22px;
  right: 22px;
  padding: 9px 22px;
  background: rgba(10, 15, 28, 0.65);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid var(--line-soft);
  border-radius: 24px;
  color: var(--text-2);
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 2px;
  cursor: pointer;
  z-index: 20;
  transition: border-color 0.3s ease, color 0.3s ease;
}

.prologue__skip:hover {
  border-color: rgba(216, 178, 90, 0.35);
  color: var(--gold-lt);
}

/* ---- 进度条 ---- */
.prologue__progress {
  position: absolute;
  top: 62%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 82%;
  z-index: 15;
}

.prologue__track {
  width: 100%;
  height: 1.5px;
  background: var(--line-soft);
  border-radius: 2px;
}

.prologue__fill {
  height: 100%;
  background: linear-gradient(90deg, var(--gold), #b8c2d4);
  border-radius: 2px;
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.prologue__orn {
  display: flex;
  justify-content: space-between;
  margin-top: -6px;
}

.prologue__orn .d {
  width: 8px;
  height: 8px;
  border: 1px solid rgba(216, 178, 90, 0.35);
  transform: rotate(45deg);
  background: var(--bg-1);
}

/* ---- 正文 ---- */
.prologue__body {
  position: absolute;
  top: 66%;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 32px 50px;
  z-index: 15;
  cursor: pointer;
  /* 小屏上正文可能超过可视区，允许内部滚动，避免被裁掉半段 */
  overflow-y: auto;
}

.prologue__text {
  font-family: var(--font-serif);
  font-size: clamp(15px, 3.8vw, 17px);
  font-weight: 400;
  line-height: 2.15;
  color: var(--text);
  letter-spacing: 1.5px;
  max-width: 22em;
  min-height: 140px;
  margin: 0;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.5);
  text-align: left;
}

.ch {
  display: inline-block;
  opacity: 0;
  transform: translateY(16px);
  transition: opacity 0.5s ease, transform 0.5s ease;
}

.ch.is-on {
  opacity: 1;
  transform: translateY(0);
}

.prologue__next {
  margin-top: 28px;
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 3px;
  color: var(--text-2);
  display: flex;
  align-items: center;
  gap: 6px;
  animation: gentlePulse 2.5s ease-in-out infinite;
}

.prologue__next .chv {
  display: inline-block;
  font-size: 18px;
  animation: chvNudge 1.8s ease-in-out infinite;
}

@keyframes gentlePulse {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 1;
  }
}

@keyframes chvNudge {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(3px);
  }
}

@media (min-width: 768px) {
  .prologue__relief {
    height: 56%;
  }

  /* 桌面视口宽，cover 裁剪后只露图片顶部一截；浮雕主体在图中部，下移取景框 */
  .prologue__relief img {
    object-position: center 38%;
  }

  .prologue__skip {
    top: 28px;
    right: 40px;
  }

  .prologue__progress {
    width: min(600px, 72%);
  }

  .prologue__body {
    top: 59%;
    padding: 24px 48px 64px;
  }

  .prologue__text {
    max-width: 38em;
    font-size: 17px;
  }

  .prologue__next {
    font-size: 15px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ch {
    transition: none;
    transform: none;
  }

  .prologue__next,
  .prologue__next .chv {
    animation: none;
  }

  .prologue__fill {
    transition: none;
  }
}
</style>
