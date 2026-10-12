<script setup>
/**
 * 龙族主题背景层：
 *  1. 极光 / 深渊光晕（CSS）
 *  2. 悬浮龙文尘埃 + 上升流火（Canvas）
 *  3. 巨型龙文水印 + 旋转法阵（SVG）
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvasRef = ref(null)

const RUNES = ['龍', '言', '灵', '血', '鳞', '瞳', '焰', '王', '咒', '帝']

let raf = 0
let ctx = null
let width = 0
let height = 0
let dpr = 1
let motes = []
let glyphs = []
let running = true

const rand = (min, max) => Math.random() * (max - min) + min

function buildScene() {
  // —— 尘埃 / 流火 ——
  const count = Math.round(Math.min(Math.max(width / 15, 40), 130))
  motes = Array.from({ length: count }, () => {
    const big = Math.random() > 0.86
    const tone = Math.random()
    return {
      x: rand(0, width),
      y: rand(0, height),
      r: big ? rand(1.2, 2.1) : rand(0.35, 1.1),
      vx: rand(-0.16, 0.16),
      vy: big ? rand(-0.5, -0.22) : rand(-0.2, -0.05),
      base: big ? rand(0.5, 0.9) : rand(0.12, 0.44),
      phase: rand(0, Math.PI * 2),
      speed: rand(0.006, 0.02),
      color:
        tone > 0.82
          ? '127, 212, 255' // 言灵冰蓝
          : tone > 0.92
            ? '226, 86, 79' // 龙血
            : '216, 178, 90' // 鎏金
    }
  })

  // —— 龙文残影 ——
  const gCount = width < 700 ? 6 : 11
  glyphs = Array.from({ length: gCount }, () => ({
    x: rand(0, width),
    y: rand(0, height),
    size: rand(16, 46),
    vy: rand(-0.16, -0.04),
    rot: rand(-0.25, 0.25),
    vr: rand(-0.0016, 0.0016),
    alpha: rand(0.025, 0.075),
    char: RUNES[Math.floor(Math.random() * RUNES.length)]
  }))
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = window.innerWidth
  height = window.innerHeight
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  buildScene()
}

function draw() {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)

  // 龙文残影
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  for (const g of glyphs) {
    ctx.save()
    ctx.translate(g.x, g.y)
    ctx.rotate(g.rot)
    ctx.globalAlpha = g.alpha
    ctx.fillStyle = '#d8b25a'
    ctx.font = `${g.size}px "Cinzel","Songti SC","STSong",serif`
    ctx.fillText(g.char, 0, 0)
    ctx.restore()

    g.y += g.vy
    g.rot += g.vr
    if (g.y < -60) {
      g.y = height + 60
      g.x = rand(0, width)
    }
  }

  // 尘埃 / 流火
  for (const m of motes) {
    m.phase += m.speed
    const alpha = m.base * (0.55 + 0.45 * Math.sin(m.phase))
    ctx.globalAlpha = Math.max(alpha, 0.02)
    ctx.fillStyle = `rgb(${m.color})`
    if (m.r > 1.1) {
      ctx.shadowBlur = 12
      ctx.shadowColor = `rgba(${m.color}, 0.9)`
    } else {
      ctx.shadowBlur = 0
    }
    ctx.beginPath()
    ctx.arc(m.x, m.y, m.r, 0, Math.PI * 2)
    ctx.fill()

    m.x += m.vx
    m.y += m.vy
    if (m.y < -20) {
      m.y = height + 20
      m.x = rand(0, width)
    }
    if (m.x < -20) m.x = width + 20
    if (m.x > width + 20) m.x = -20
  }

  ctx.shadowBlur = 0
  ctx.globalAlpha = 1
  if (running) raf = requestAnimationFrame(draw)
}

function onVisibility() {
  if (document.hidden) {
    running = false
    cancelAnimationFrame(raf)
  } else if (!running) {
    running = true
    raf = requestAnimationFrame(draw)
  }
}

onMounted(() => {
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  resize()
  window.addEventListener('resize', resize)
  document.addEventListener('visibilitychange', onVisibility)
  if (!reduce) {
    raf = requestAnimationFrame(draw)
  } else {
    draw() // 静止渲染一帧
    running = false
  }
})

onBeforeUnmount(() => {
  running = false
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<template>
  <div class="bg-layer">
    <!-- 巨型龙文水印 -->
    <span class="rune-watermark">龍</span>

    <!-- 旋转法阵 -->
    <svg class="rune-ring" viewBox="0 0 800 800" aria-hidden="true">
      <defs>
        <linearGradient id="ringGold" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="rgba(216,178,90,0.85)" />
          <stop offset="50%" stop-color="rgba(216,178,90,0.25)" />
          <stop offset="100%" stop-color="rgba(216,178,90,0.7)" />
        </linearGradient>
        <path
          id="ringTextPath"
          d="M400,400 m-312,0 a312,312 0 1,1 624,0 a312,312 0 1,1 -624,0"
        />
      </defs>

      <!-- 外环 + 刻度 + 环形龙文 -->
      <g class="ring-a">
        <circle cx="400" cy="400" r="330" fill="none" stroke="url(#ringGold)" stroke-width="1" />
        <g stroke="rgba(216,178,90,0.5)" stroke-width="0.9">
          <line
            v-for="i in 120"
            :key="i"
            x1="400"
            :y1="i % 5 === 0 ? 62 : 70"
            x2="400"
            y2="80"
            :transform="`rotate(${i * 3} 400 400)`"
          />
        </g>
        <text
          fill="rgba(216,178,90,0.5)"
          font-family="'Cinzel','Songti SC',serif"
          font-size="19"
          letter-spacing="16"
        >
          <textPath href="#ringTextPath" startOffset="0%">
            龍文・言霊・混血種・カッセル学院・青銅与火・審判之瞳・王与王的战争・竜族
          </textPath>
        </text>
      </g>

      <!-- 中环虚线 -->
      <g class="ring-b">
        <circle
          cx="400"
          cy="400"
          r="262"
          fill="none"
          stroke="rgba(216,178,90,0.42)"
          stroke-width="1.4"
          stroke-dasharray="2 14"
        />
        <circle
          cx="400"
          cy="400"
          r="236"
          fill="none"
          stroke="rgba(127,212,255,0.16)"
          stroke-width="0.8"
          stroke-dasharray="26 12"
        />
      </g>

      <!-- 内环圆点 -->
      <g class="ring-c">
        <circle
          cx="400"
          cy="400"
          r="196"
          fill="none"
          stroke="rgba(216,178,90,0.3)"
          stroke-width="0.8"
        />
        <g fill="rgba(216,178,90,0.55)">
          <circle v-for="i in 36" :key="i" cx="400" cy="204" r="2.4" :transform="`rotate(${i * 10} 400 400)`" />
        </g>
      </g>
    </svg>

    <!-- 粒子画布 -->
    <canvas ref="canvasRef" class="bg-canvas" />
  </div>
</template>

<style scoped>
.bg-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
</style>
