<script setup>
/**
 * 龙文尘埃：Canvas 悬浮粒子背景。
 *
 * 从 longzu 原型的内联脚本重构而来，四处关键改动：
 *  1. 抽成组件，`paused` 由父级控制 —— 全站同一时刻只允许一个 rAF 循环在跑
 *     （内页已经有 RuneBackground 的法阵 + 粒子，两个循环叠加会让低端机掉帧）
 *  2. 卸载时 cancelAnimationFrame + 移除监听，避免离开路由后还在空转
 *  3. 按 devicePixelRatio 放大画布，高分屏粒子不再是糊的
 *  4. 尊重 prefers-reduced-motion：该偏好下只画一帧静态粒子，不启动循环
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  /** 粒子数量，默认 45（与原型一致）；落地页可按屏幕宽度减半 */
  density: { type: Number, default: 45 },
  /** 暂停循环但保留画面 */
  paused: { type: Boolean, default: false }
})

const canvasRef = ref(null)
let ctx = null
let raf = 0
let particles = []
let width = 0
let height = 0
let dpr = 1
let reduced = false

function resetParticle(p, initial) {
  p.x = Math.random() * width
  p.y = initial ? Math.random() * height : height + 5
  p.r = Math.random() * 1.8 + 0.4
  p.vy = -(Math.random() * 0.35 + 0.1)
  p.vx = (Math.random() - 0.5) * 0.18
  p.alpha = Math.random() * 0.35 + 0.08
  p.da = Math.random() * 0.001 + 0.0004
}

function build() {
  const canvas = canvasRef.value
  if (!canvas) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = canvas.clientWidth || window.innerWidth
  height = canvas.clientHeight || window.innerHeight
  canvas.width = Math.round(width * dpr)
  canvas.height = Math.round(height * dpr)
  ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  // 窄屏按比例减量，粒子数不随像素面积线性爆炸
  const count = width < 640 ? Math.round(props.density * 0.5) : props.density
  particles = Array.from({ length: count }, () => {
    const p = {}
    resetParticle(p, true)
    return p
  })
}

function drawFrame() {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)
  for (const p of particles) {
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(165, 195, 235, ${p.alpha})`
    ctx.fill()
  }
}

function step() {
  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy
    p.alpha -= p.da
    if (p.alpha <= 0 || p.y < -5) resetParticle(p, false)
  }
}

function loop() {
  step()
  drawFrame()
  raf = requestAnimationFrame(loop)
}

function start() {
  if (raf || reduced || props.paused) return
  raf = requestAnimationFrame(loop)
}

function stop() {
  if (raf) {
    cancelAnimationFrame(raf)
    raf = 0
  }
}

function onResize() {
  stop()
  build()
  drawFrame()
  start()
}

function onVisibility() {
  // 切到后台标签页时停掉，回来再续 —— 不占 CPU
  if (document.hidden) stop()
  else start()
}

onMounted(() => {
  reduced =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches

  build()
  drawFrame() // 先保证静态可看
  start()

  window.addEventListener('resize', onResize, { passive: true })
  document.addEventListener('visibilitychange', onVisibility)
})

watch(
  () => props.paused,
  (v) => (v ? stop() : start())
)

onBeforeUnmount(() => {
  stop()
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVisibility)
  particles = []
  ctx = null
})
</script>

<template>
  <canvas ref="canvasRef" class="dust-canvas" aria-hidden="true" />
</template>

<style scoped>
.dust-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  pointer-events: none;
  z-index: 2;
}
</style>
