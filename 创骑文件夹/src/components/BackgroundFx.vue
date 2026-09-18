<template>
  <canvas ref="bgCanvas" id="bgfx"></canvas>
  <div class="rainbow-aura"></div>
  <div ref="spotEl" class="spot" id="spot"></div>
  <div ref="glowREl" class="glow-r" id="glowR"></div>
  <div ref="glowBEl" class="glow-b" id="glowB"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { FORMS, GENIUS_RAINBOW } from '../data/forms.js'

const props = defineProps({ activeForm: { type: String, required: true } })

const bgCanvas = ref(null)
const spotEl = ref(null)
const glowREl = ref(null)
const glowBEl = ref(null)

let W = 0, H = 0, ctx = null, raf = 0, particles = []
const R = 150
let pmx = 0, pmy = 0
let mx = 0, my = 0, rafMove = null

function themeColors() {
  if (props.activeForm === 'genius') return GENIUS_RAINBOW
  if (props.activeForm === 'hazard') return ['#ff1f1f', '#ff5a5a', '#7a0000']
  const t = FORMS[props.activeForm].theme
  return [t.left, t.right, t.accent]
}

function resize() {
  W = bgCanvas.value.width = window.innerWidth
  H = bgCanvas.value.height = window.innerHeight
}

function initParticles() {
  particles = []
  for (let i = 0; i < 60; i++) {
    particles.push({
      x: Math.random() * W, y: Math.random() * H,
      vx: (Math.random() - .5) * 0.3, vy: (Math.random() - .5) * 0.3,
      r: Math.random() * 2 + 1, a: Math.random() * 0.45 + 0.15,
      ci: i % 3, ph: Math.random() * Math.PI * 2
    })
  }
}

function draw(t) {
  ctx.clearRect(0, 0, W, H)
  const cols = themeColors()
  for (const p of particles) {
    const dx = p.x - pmx, dy = p.y - pmy, d2 = dx * dx + dy * dy
    if (d2 < R * R) {                 // 鼠标力场：靠近被推开
      const d = Math.sqrt(d2) || 1
      const f = (R - d) / R
      p.vx += (dx / d) * f * 0.12
      p.vy += (dy / d) * f * 0.12
    }
    p.vx = p.vx * 0.97 + Math.cos(p.ph + t * 0.0004) * 0.01
    p.vy = p.vy * 0.97 + Math.sin(p.ph + t * 0.0004) * 0.01
    p.x += p.vx; p.y += p.vy
    if (p.x < -10) p.x = W + 10; if (p.x > W + 10) p.x = -10
    if (p.y < -10) p.y = H + 10; if (p.y > H + 10) p.y = -10
    ctx.globalAlpha = p.a
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = cols[p.ci % cols.length]
    ctx.fill()
  }
  ctx.globalAlpha = 1
  raf = requestAnimationFrame(draw)
}

// 点击能量波：多重扩散环 + 放射状光线（天才形态使用彩色调色板）
function spawnWave(x, y, palette) {
  const mkRing = (border, scale, dur, delay) => {
    const d = document.createElement('div')
    d.className = 'ripple'
    d.style.left = x + 'px'; d.style.top = y + 'px'
    d.style.borderColor = border
    document.body.appendChild(d)
    d.animate(
      [{ transform: 'translate(-50%,-50%) scale(0)', opacity: .75 },
       { transform: `translate(-50%,-50%) scale(${scale})`, opacity: 0 }],
      { duration: dur, easing: 'ease-out', delay: delay || 0 }
    ).onfinish = () => d.remove()
  }
  mkRing(palette[0], 3.4, 720, 0)
  mkRing(palette[1 % palette.length], 2.0, 520, 90)

  const N = 12
  for (let i = 0; i < N; i++) {
    const ang = i * (360 / N)
    const c = palette[i % palette.length]
    const ray = document.createElement('div')
    ray.className = 'ray'
    ray.style.left = x + 'px'; ray.style.top = y + 'px'
    ray.style.background = c
    ray.style.transform = `translateY(-50%) rotate(${ang}deg) translateX(6px)`
    document.body.appendChild(ray)
    ray.animate(
      [{ transform: `translateY(-50%) rotate(${ang}deg) translateX(6px) scaleX(.3)`, opacity: .9 },
       { transform: `translateY(-50%) rotate(${ang}deg) translateX(70px) scaleX(1)`, opacity: 0 }],
      { duration: 520, easing: 'ease-out' }
    ).onfinish = () => ray.remove()
  }
}

function onMove(e) {
  mx = e.clientX; my = e.clientY; pmx = mx; pmy = my
  if (spotEl.value) {
    spotEl.value.style.left = mx + 'px'
    spotEl.value.style.top = my + 'px'
  }
  if (rafMove) return
  rafMove = requestAnimationFrame(() => {
    const cx = window.innerWidth / 2, cy = window.innerHeight / 2
    const gx = (mx - cx) * -0.03
    const gy = (my - cy) * -0.03
    const rx = ((my - cy) / cy) * 6     // 上下倾斜
    const ry = ((mx - cx) / cx) * 10    // 左右倾斜
    document.body.style.setProperty('--gx', gx + 'px')
    document.body.style.setProperty('--gy', gy + 'px')
    document.body.style.setProperty('--rx', rx + 'deg')
    document.body.style.setProperty('--ry', ry + 'deg')
    rafMove = null
  })
}

function onClick(e) {
  if (e.target !== document.body) return
  const t = FORMS[props.activeForm].theme
  if (props.activeForm === 'genius') spawnWave(e.clientX, e.clientY, GENIUS_RAINBOW)
  else spawnWave(e.clientX, e.clientY, [t.ray1, t.ray2])
}

function applyTheme() {
  const t = FORMS[props.activeForm].theme
  if (glowREl.value) glowREl.value.style.background = `radial-gradient(circle, ${t.glowR}, transparent 65%)`
  if (glowBEl.value) glowBEl.value.style.background = `radial-gradient(circle, ${t.glowB}, transparent 65%)`
  if (spotEl.value) spotEl.value.style.background = t.spot
  document.body.style.setProperty('--grid', t.grid)
}

onMounted(() => {
  resize(); initParticles(); ctx = bgCanvas.value.getContext('2d')
  pmx = W / 2; pmy = H / 2
  applyTheme()
  raf = requestAnimationFrame(draw)
  window.addEventListener('resize', resize)
  document.addEventListener('mousemove', onMove)
  document.addEventListener('click', onClick)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  document.removeEventListener('mousemove', onMove)
  document.removeEventListener('click', onClick)
})

watch(() => props.activeForm, () => applyTheme())
</script>
