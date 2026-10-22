<script setup>
/**
 * 龙族主题背景层（可交互版）
 *  1. 极光 / 深渊光晕（CSS）
 *  2. 巨型龙文水印 + 旋转法阵，随鼠标做视差与 3D 微旋
 *  3. Canvas：龙文残影 + 尘埃流火 + 鼠标气流 + 星图连线 + 点击涟漪 + 龙文炸开
 *  4. 控制台：绚烂 / 静谧 / 关闭 三档，记忆到 localStorage；掉帧自动降密度
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const canvasRef = ref(null)
const watermarkRef = ref(null)
const ringRef = ref(null)
const spotRef = ref(null)

const RUNES = ['龍', '言', '灵', '血', '鳞', '瞳', '焰', '王', '咒', '帝']

const reduce =
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

/* ---------------- 模式：绚烂 / 静谧 / 关闭 ---------------- */
const MODES = [
  { key: 'rich', label: '绚烂', hint: '全量粒子 · 星图连线 · 鼠标视差' },
  { key: 'calm', label: '静谧', hint: '半量粒子 · 无视差无连线' },
  { key: 'off', label: '关闭', hint: '仅保留静态光晕' }
]
const MODE_KEY = 'dr:bg-mode'
const mode = ref(localStorage.getItem(MODE_KEY) || (reduce ? 'off' : 'rich'))
if (!MODES.some((m) => m.key === mode.value)) mode.value = reduce ? 'off' : 'rich'

watch(mode, () => applyMode())

/* ---------------- 运行时状态 ---------------- */
let raf = 0
let ctx = null
let width = 0
let height = 0
let dpr = 1
let motes = []
let glyphs = []
let ripples = [] // 点击涟漪 { x, y, r, max, life }
let bursts = [] // 点击处浮出的龙文 { x, y, char, life, size }
let running = false

/** 指针状态 */
const pointer = { x: -9999, y: -9999, has: false, vx: 0, vy: 0, speed: 0, down: false }

/** 视差目标值与当前值（当前值每帧向目标值插值，避免生硬跳动） */
const par = { tx: 0, ty: 0, x: 0, y: 0 }
let spotOn = 0 // 光斑强度 0–1

/** 输入时的「龙文躁动」：键入会让粒子短暂加速 */
let agitation = 0

/** 掉帧自适应：0 = 满密度，越大越稀疏 */
let degrade = 0
let fpsAcc = 0
let fpsFrames = 0
let lastTs = 0

const rand = (min, max) => Math.random() * (max - min) + min

function densityScale() {
  const byMode = mode.value === 'calm' ? 0.5 : 1
  const byFps = [1, 0.7, 0.5, 0.36][Math.min(degrade, 3)]
  return byMode * byFps
}

function buildScene() {
  const scale = densityScale()
  const count = Math.round(Math.min(Math.max(width / 15, 40), 130) * scale)
  motes = Array.from({ length: count }, () => {
    const big = Math.random() > 0.86
    const tone = Math.random()
    return {
      x: rand(0, width),
      y: rand(0, height),
      r: big ? rand(1.2, 2.1) : rand(0.35, 1.1),
      vx: rand(-0.16, 0.16),
      vy: big ? rand(-0.5, -0.22) : rand(-0.2, -0.05),
      /* 鼠标气流造成的附加位移，每帧衰减 */
      ox: 0,
      oy: 0,
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

  const gCount = Math.max(3, Math.round((width < 700 ? 6 : 11) * scale))
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
  if (mode.value === 'off') draw(performance.now())
}

/* ---------------- 指针事件 ---------------- */
const INTERACTIVE = 'button, a, input, select, textarea, label, .auth-card, .bg-panel'

function onPointerMove(e) {
  if (e.pointerType === 'touch') return
  pointer.vx = e.clientX - pointer.x
  pointer.vy = e.clientY - pointer.y
  pointer.x = e.clientX
  pointer.y = e.clientY
  pointer.has = true
  pointer.speed = Math.min(Math.hypot(pointer.vx, pointer.vy), 60)

  // 视差目标：以屏幕中心为原点，归一化到 -1 ~ 1
  par.tx = (pointer.x / width - 0.5) * 2
  par.ty = (pointer.y / height - 0.5) * 2
  spotOn = 1
}

function onPointerLeave() {
  pointer.has = false
  pointer.x = -9999
  pointer.y = -9999
  pointer.speed = 0
  par.tx = 0
  par.ty = 0
  spotOn = 0
}

function onPointerDown(e) {
  if (mode.value === 'off' || reduce) return
  const t = e.target
  if (t && t.closest && t.closest(INTERACTIVE)) return // 点按钮 / 输入框时不放涟漪
  ripples.push({ x: e.clientX, y: e.clientY, r: 6, max: 190, life: 1 })
  if (ripples.length > 6) ripples.shift()
  bursts.push({
    x: e.clientX,
    y: e.clientY,
    char: RUNES[Math.floor(Math.random() * RUNES.length)],
    life: 1,
    size: rand(38, 64)
  })
  if (bursts.length > 4) bursts.shift()
  pointer.down = true
}

function onPointerUp() {
  pointer.down = false
}

/** 键入时让背景「躁动」一下 */
function onTyping() {
  agitation = 1
}

function onVisibility() {
  if (document.hidden) stop()
  else start()
}

/* ---------------- 绘制 ---------------- */
function draw(ts) {
  if (!ctx) return

  /* —— 掉帧自适应 —— */
  if (lastTs) {
    fpsAcc += ts - lastTs
    fpsFrames += 1
  }
  lastTs = ts
  if (fpsFrames >= 90) {
    const avg = fpsAcc / fpsFrames
    if (avg > 23 && degrade < 3) {
      degrade += 1
      buildScene()
    } else if (avg < 15 && degrade > 0) {
      degrade -= 1
      buildScene()
    }
    fpsAcc = 0
    fpsFrames = 0
  }

  /* —— 视差插值 + 直接写 DOM（不走响应式，避免每帧重渲染组件） —— */
  if (mode.value === 'rich') {
    par.x += (par.tx - par.x) * 0.075
    par.y += (par.ty - par.y) * 0.075
  } else {
    par.x = 0
    par.y = 0
  }
  if (watermarkRef.value) {
    watermarkRef.value.style.transform = `translate3d(${(par.x * -26).toFixed(2)}px, ${(
      par.y * -18
    ).toFixed(2)}px, 0)`
  }
  if (ringRef.value) {
    ringRef.value.style.transform = `translate3d(${(par.x * 14).toFixed(2)}px, ${(par.y * 10).toFixed(
      2
    )}px, 0) rotateX(${(par.y * -4).toFixed(2)}deg) rotateY(${(par.x * 5).toFixed(2)}deg)`
  }
  if (spotRef.value) {
    spotRef.value.style.transform = `translate3d(${pointer.x}px, ${pointer.y}px, 0) translate(-50%, -50%)`
    spotRef.value.style.opacity = (spotOn * (pointer.down ? 0.95 : 0.6)).toFixed(3)
  }

  ctx.clearRect(0, 0, width, height)

  // 躁动衰减：影响粒子上升速度
  agitation *= 0.94
  const boost = 1 + agitation * 1.6

  /* —— 龙文残影 —— */
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

    g.y += g.vy * boost
    g.rot += g.vr
    if (g.y < -60) {
      g.y = height + 60
      g.x = rand(0, width)
    }
  }

  /* —— 鼠标气流：附近粒子被卷动绕行 —— */
  const interactive = mode.value === 'rich' && pointer.has && !reduce
  if (interactive) {
    for (const m of motes) {
      const dx = m.x - pointer.x
      const dy = m.y - pointer.y
      const d2 = dx * dx + dy * dy
      if (d2 > 26000) continue // 半径约 161px
      const d = Math.sqrt(d2) || 1
      const f = (1 - d / 161) * (0.55 + pointer.speed / 90)
      // 切向绕行 + 轻微外推，避免粒子全糊在光标上
      m.ox += (-dy / d) * f * 0.9 + (dx / d) * f * 0.32
      m.oy += (dx / d) * f * 0.9 + (dy / d) * f * 0.32
    }
  }

  /* —— 星图连线：只连光标附近的粒子（O(k²)，k 通常 < 30） —— */
  if (interactive) {
    const near = []
    for (const m of motes) {
      if (Math.hypot(m.x - pointer.x, m.y - pointer.y) < 230) near.push(m)
    }
    ctx.lineWidth = 1
    for (let i = 0; i < near.length; i += 1) {
      for (let j = i + 1; j < near.length; j += 1) {
        const a = near[i]
        const b = near[j]
        const d = Math.hypot(a.x - b.x, a.y - b.y)
        if (d > 118) continue
        ctx.globalAlpha = (1 - d / 118) * 0.3
        ctx.strokeStyle = 'rgba(216, 178, 90, 1)'
        ctx.beginPath()
        ctx.moveTo(a.x, a.y)
        ctx.lineTo(b.x, b.y)
        ctx.stroke()
      }
    }
    // 光标与近处粒子的牵引线
    for (const m of near) {
      const d = Math.hypot(m.x - pointer.x, m.y - pointer.y)
      if (d > 170) continue
      ctx.globalAlpha = (1 - d / 170) * 0.22
      ctx.strokeStyle = 'rgba(127, 212, 255, 1)'
      ctx.beginPath()
      ctx.moveTo(pointer.x, pointer.y)
      ctx.lineTo(m.x, m.y)
      ctx.stroke()
    }
  }

  /* —— 尘埃 / 流火 —— */
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

    m.x += m.vx + m.ox
    m.y += m.vy * boost + m.oy
    m.ox *= 0.92
    m.oy *= 0.92

    if (m.y < -20) {
      m.y = height + 20
      m.x = rand(0, width)
    }
    if (m.x < -20) m.x = width + 20
    if (m.x > width + 20) m.x = -20
  }
  ctx.shadowBlur = 0

  /* —— 点击涟漪 —— */
  for (let i = ripples.length - 1; i >= 0; i -= 1) {
    const r = ripples[i]
    r.r += (r.max - r.r) * 0.06
    r.life -= 0.018
    if (r.life <= 0) {
      ripples.splice(i, 1)
      continue
    }
    ctx.globalAlpha = Math.max(r.life * 0.55, 0)
    ctx.strokeStyle = 'rgba(216, 178, 90, 1)'
    ctx.lineWidth = 1.4
    ctx.beginPath()
    ctx.arc(r.x, r.y, r.r, 0, Math.PI * 2)
    ctx.stroke()

    ctx.globalAlpha = Math.max(r.life * 0.25, 0)
    ctx.strokeStyle = 'rgba(127, 212, 255, 1)'
    ctx.lineWidth = 0.8
    ctx.beginPath()
    ctx.arc(r.x, r.y, r.r * 0.62, 0, Math.PI * 2)
    ctx.stroke()
  }

  /* —— 点击处浮出的龙文 —— */
  for (let i = bursts.length - 1; i >= 0; i -= 1) {
    const b = bursts[i]
    b.life -= 0.012
    if (b.life <= 0) {
      bursts.splice(i, 1)
      continue
    }
    const grow = 1 + (1 - b.life) * 0.5
    ctx.save()
    ctx.globalAlpha = Math.max(b.life * 0.5, 0)
    ctx.fillStyle = '#f2dc9b'
    ctx.font = `${(b.size * grow).toFixed(1)}px "Cinzel","Songti SC","STSong",serif`
    ctx.fillText(b.char, b.x, b.y - (1 - b.life) * 40)
    ctx.restore()
  }

  ctx.globalAlpha = 1
  if (running) raf = requestAnimationFrame(draw)
}

function start() {
  if (running || mode.value === 'off') return
  running = true
  lastTs = 0
  fpsAcc = 0
  fpsFrames = 0
  raf = requestAnimationFrame(draw)
}

function stop() {
  running = false
  cancelAnimationFrame(raf)
}

/** 模式切换：off 停循环；其余重建场景并启动 */
function applyMode() {
  stop()
  ripples = []
  bursts = []
  localStorage.setItem(MODE_KEY, mode.value)
  if (mode.value === 'off') {
    if (ctx) ctx.clearRect(0, 0, width, height)
    if (watermarkRef.value) watermarkRef.value.style.transform = 'translate3d(0,0,0)'
    if (ringRef.value) ringRef.value.style.transform = 'translate3d(0,0,0)'
    if (spotRef.value) spotRef.value.style.opacity = '0'
    return
  }
  buildScene()
  start()
}

/** 控制台上的「重排星图」 */
function reshuffle() {
  buildScene()
  if (mode.value === 'off') mode.value = 'rich'
  else if (!running) start()
}

const panelOpen = ref(false)

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
  document.addEventListener('visibilitychange', onVisibility)
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('pointerdown', onPointerDown, { passive: true })
  window.addEventListener('pointerup', onPointerUp, { passive: true })
  document.addEventListener('pointerleave', onPointerLeave)
  document.addEventListener('input', onTyping, { passive: true })
  applyMode()
})

onBeforeUnmount(() => {
  stop()
  window.removeEventListener('resize', resize)
  document.removeEventListener('visibilitychange', onVisibility)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerdown', onPointerDown)
  window.removeEventListener('pointerup', onPointerUp)
  document.removeEventListener('pointerleave', onPointerLeave)
  document.removeEventListener('input', onTyping)
})
</script>

<template>
  <div class="bg-layer" :class="`bg-layer--${mode}`">
    <!-- 龙纹暗纹：低透明度龙鳞平铺（不随模式关闭，常驻） -->
    <span class="bg-dragon" aria-hidden="true" />

    <!-- 巨型龙文水印（外层做视差，内层保留呼吸动画） -->
    <span ref="watermarkRef" class="rune-watermark-par">
      <span class="rune-watermark">龍</span>
    </span>

    <!-- 旋转法阵（外层视差 + 3D 微旋，内层保留自转） -->
    <span ref="ringRef" class="rune-ring-par">
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
    </span>

    <!-- 粒子画布 -->
    <canvas ref="canvasRef" class="bg-canvas" />

    <!-- 跟随光标的柔光 -->
    <span ref="spotRef" class="bg-spot" />

    <!-- 四角压暗，让卡片更突出 -->
    <span class="bg-vignette" />
  </div>

  <!-- 背景控制台（不能放进 bg-layer，那边是 pointer-events: none） -->
  <div class="bg-panel" :class="{ 'is-open': panelOpen }">
    <button
      class="bg-panel__toggle"
      type="button"
      :aria-label="panelOpen ? '收起背景设置' : '展开背景设置'"
      :aria-expanded="panelOpen"
      @click="panelOpen = !panelOpen"
    >
      <AppIcon name="sparkle" :size="14" />
    </button>

    <transition name="bg-fade">
      <div v-if="panelOpen" class="bg-panel__body">
        <p class="bg-panel__title">背景 · 龙文场域</p>
        <div class="bg-panel__modes">
          <button
            v-for="m in MODES"
            :key="m.key"
            type="button"
            class="bg-mode"
            :class="{ 'is-on': mode === m.key }"
            :title="m.hint"
            @click="mode = m.key"
          >
            {{ m.label }}
          </button>
        </div>
        <p class="bg-panel__hint">{{ (MODES.find((m) => m.key === mode) || {}).hint }}</p>
        <button class="bg-panel__act" type="button" @click="reshuffle">
          <AppIcon name="refresh" :size="11" />重排星图
        </button>
        <p class="bg-panel__tip">移动鼠标搅动尘埃，点击空白处荡开涟漪</p>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.bg-layer {
  perspective: 1400px;
}

/* 视差层：外层只做位移，内层保留原有定位与动画，避免 transform 互相覆盖 */
.rune-watermark-par,
.rune-ring-par {
  position: absolute;
  inset: 0;
  display: block;
  will-change: transform;
}
.rune-ring-par {
  transform-style: preserve-3d;
}

.bg-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* 跟随光标的柔光 */
.bg-spot {
  position: absolute;
  top: 0;
  left: 0;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(216, 178, 90, 0.1) 0%,
    rgba(127, 212, 255, 0.05) 38%,
    transparent 68%
  );
  mix-blend-mode: screen;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.4s ease;
  will-change: transform, opacity;
}

/* 四角压暗 */
.bg-vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(
    120% 90% at 50% 45%,
    transparent 42%,
    rgba(3, 5, 10, 0.34) 78%,
    rgba(3, 5, 10, 0.62) 100%
  );
}

/* 关闭模式：压暗法阵与水印，只留 CSS 光晕 */
.bg-layer--off .rune-ring-par,
.bg-layer--off .rune-watermark-par {
  opacity: 0.35;
  transition: opacity 0.4s ease;
}

@media (prefers-reduced-motion: reduce) {
  .bg-spot {
    display: none;
  }
}

/* ---------------- 控制台 ---------------- */
.bg-panel {
  position: fixed;
  left: 16px;
  bottom: 16px;
  z-index: 20;
  display: flex;
  /* 向上展开：按钮永远在最底部，面板在其上方冒出，不会溢出视口 */
  flex-direction: column-reverse;
  align-items: flex-start;
  gap: 8px;
}

.bg-panel__toggle {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: rgba(10, 15, 26, 0.72);
  color: var(--gold-dp);
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: color 0.25s, border-color 0.25s, transform 0.25s, box-shadow 0.25s;
}
.bg-panel__toggle:hover {
  color: var(--gold-lt);
  border-color: var(--gold);
  transform: translateY(-2px);
  box-shadow: 0 10px 26px -14px var(--gold-glow);
}
.bg-panel.is-open .bg-panel__toggle {
  color: var(--gold-lt);
  border-color: var(--gold);
}

.bg-panel__body {
  width: 208px;
  padding: 12px 13px 11px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: rgba(10, 15, 26, 0.86);
  backdrop-filter: blur(12px);
  box-shadow: var(--shadow-card);
}

.bg-panel__title {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--text-4);
  text-transform: uppercase;
  margin-bottom: 9px;
}

.bg-panel__modes {
  display: flex;
  gap: 5px;
  margin-bottom: 8px;
}
.bg-mode {
  flex: 1;
  padding: 5px 0;
  border-radius: 7px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.6);
  color: var(--text-3);
  font-size: 11.5px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.bg-mode:hover {
  color: var(--text);
  border-color: rgba(216, 178, 90, 0.4);
}
.bg-mode.is-on {
  color: #1a1206;
  border-color: var(--gold);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
  font-weight: 600;
}

.bg-panel__hint {
  font-size: 10.5px;
  line-height: 1.5;
  color: var(--text-4);
  margin-bottom: 9px;
  min-height: 16px;
}

.bg-panel__act {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: 7px;
  border: 1px solid rgba(216, 178, 90, 0.34);
  background: rgba(216, 178, 90, 0.07);
  color: var(--gold-lt);
  font-size: 11.5px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.bg-panel__act:hover {
  background: rgba(216, 178, 90, 0.15);
  border-color: var(--gold);
}

.bg-panel__tip {
  margin-top: 9px;
  padding-top: 8px;
  border-top: 1px dashed rgba(255, 255, 255, 0.08);
  font-size: 10px;
  line-height: 1.55;
  color: var(--text-4);
}

.bg-fade-enter-active,
.bg-fade-leave-active {
  transition: opacity 0.22s ease, transform 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}
.bg-fade-enter-from,
.bg-fade-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.97);
}

@media (max-width: 640px) {
  .bg-panel {
    left: 10px;
    bottom: 10px;
  }
  .bg-panel__toggle {
    width: 28px;
    height: 28px;
  }
  .bg-panel__body {
    width: 188px;
  }
  .bg-panel__tip {
    display: none;
  }
}
</style>
