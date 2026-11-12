<script setup>
/**
 * 落地页：徽记入场 → 序章 → 门户。
 *
 * 与 longzu 原型的差异（有意为之）：
 *  - 原型是 4 个 `.page` 靠 `display` 切换的独立"页"，这里只有 1 条路由 + 2 个内部阶段。
 *    「序章」是一次性过场，做成独立路由反而会让刷新 / 返回变成反复看片头。
 *  - 原型有个 1.5s 强制 Loading 屏（外加 3s CSS 兜底）。SPA 首屏本来就要等 JS，
 *    再压一层定时器只是纯粹的白屏延时，这里改成入场动画自然呈现。
 *  - 原型的翻页黑幕（#fade-overlay）由路由过渡代替。
 *  - 键盘流沿用：空格 / 回车推进、ESC 直奔门户；但输入框聚焦时不拦截。
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AmbientDust from '@/components/AmbientDust.vue'
import ProloguePlayer from '@/components/ProloguePlayer.vue'
import { MOTTO, PROLOGUE_LINES, imgSrc, imgSrcset } from '@/data/portal'
import { markLandingSeen } from '@/utils/landing'

const router = useRouter()

/** splash = 徽记首屏；prologue = 序章 */
const stage = ref('splash')
const playerRef = ref(null)

function startPrologue() {
  if (stage.value !== 'splash') return
  stage.value = 'prologue'
}

function enterPortal() {
  markLandingSeen()
  router.push('/portal')
}

function onKeydown(e) {
  // 输入框 / 文本域里按空格回车属于正常输入，不能抢
  const el = e.target
  if (el && el.closest && el.closest('input, textarea, select, [contenteditable="true"]')) return

  if (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar') {
    e.preventDefault()
    if (stage.value === 'splash') startPrologue()
    else playerRef.value?.advance()
  } else if (e.key === 'Escape') {
    enterPortal()
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="landing">
    <transition name="stage" mode="out-in">
      <div v-if="stage === 'splash'" key="splash" class="splash">
        <!-- 粒子层放在首屏内部：随首屏一起淡出，也保证离开后 canvas 立即卸载 -->
        <AmbientDust :density="45" />
        <div class="splash__glow" aria-hidden="true" />

        <div class="emblem">
          <div class="emblem__halo" aria-hidden="true" />
          <img
            class="emblem__img"
            :src="imgSrc('emblem-seal', 320)"
            :srcset="imgSrcset('emblem-seal', [512, 320, 192])"
            sizes="240px"
            alt="卡塞尔学院院徽"
            decoding="async"
            fetchpriority="high"
          />
          <div class="emblem__orbit emblem__orbit--a" aria-hidden="true"><span class="orbiter" /></div>
          <div class="emblem__orbit emblem__orbit--b" aria-hidden="true"><span class="orbiter" /></div>
          <div class="emblem__orbit emblem__orbit--c" aria-hidden="true"><span class="orbiter" /></div>
          <div class="emblem__sheen" aria-hidden="true" />
        </div>

        <h1 class="splash__title">CASSELL COLLEGE</h1>

        <div class="splash__sep" aria-hidden="true">
          <span class="rule" />
          <span class="gem" />
          <span class="phrase">{{ MOTTO }}</span>
          <span class="gem" />
          <span class="rule" />
        </div>

        <button class="splash__cta" type="button" @click="startPrologue">
          欢迎来到真实的世界<span class="chev">›</span>
        </button>
      </div>

      <ProloguePlayer
        v-else
        key="prologue"
        ref="playerRef"
        :lines="PROLOGUE_LINES"
        @finish="enterPortal"
      />
    </transition>
  </div>
</template>

<style scoped>
/*
 * 全部样式 scoped，且只用 theme.css 已有的令牌。
 * 原型里 `html, body { overflow: hidden }` 那种全局副作用一律不带进来 ——
 * 它会锁死 /forum、/archive 等所有需要滚动的页面。
 */
.landing {
  position: relative;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--bg-1);
  /* 银色系在 theme.css 里没有对应令牌（全站以鎏金为主），
     这里按落地页的冷色基调局部定义，不污染全局 */
  --silver: #b8c2d4;
  --silver-bright: #dce2ee;
}

/* ---------- 徽记首屏 ---------- */
.splash {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 24px;
}

.splash__glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse 80% 55% at 50% 38%, rgba(30, 45, 75, 0.45) 0%, transparent 65%),
    radial-gradient(ellipse 120% 100% at 50% 50%, transparent 30%, rgba(5, 8, 16, 0.7) 100%);
}

.emblem {
  position: relative;
  width: 200px;
  height: 200px;
  margin-bottom: 40px;
  animation: emblemFloat 7s ease-in-out infinite;
}

.emblem__halo {
  position: absolute;
  inset: -50px;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(80, 120, 190, 0.18) 0%,
    rgba(60, 90, 150, 0.08) 40%,
    transparent 70%
  );
  animation: glowPulse 5s ease-in-out infinite;
}

.emblem__img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  filter: brightness(0.85) contrast(1.1);
  position: relative;
  z-index: 2;
  background: radial-gradient(circle, #1a2540 0%, #0e1830 60%, #0a1020 100%);
  border: 1px solid rgba(150, 180, 225, 0.2);
}

.emblem__orbit {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(150, 180, 225, 0.22);
  animation: orbit linear infinite;
}

/* 用显式的修饰类而不是 nth-child / nth-of-type：
   轨道环前面还有一个 halo div 和一个 img，按序号数会数错位 */
.emblem__orbit--a {
  inset: -18px;
  animation-duration: 18s;
}

.emblem__orbit--b {
  inset: -38px;
  animation-duration: 28s;
  animation-direction: reverse;
  opacity: 0.7;
}

.emblem__orbit--c {
  inset: -60px;
  animation-duration: 42s;
  opacity: 0.45;
}
.orbiter {
  position: absolute;
  width: 5px;
  height: 5px;
  background: rgba(185, 210, 245, 0.7);
  border-radius: 50%;
  top: -2.5px;
  left: 50%;
  transform: translateX(-50%);
  box-shadow: 0 0 10px rgba(160, 195, 240, 0.5), 0 0 20px rgba(160, 195, 240, 0.2);
}

.emblem__sheen {
  position: absolute;
  inset: -20px;
  border-radius: 50%;
  overflow: hidden;
  z-index: 3;
  pointer-events: none;
}

.emblem__sheen::after {
  content: '';
  position: absolute;
  width: 50%;
  height: 300%;
  top: -100%;
  left: -60%;
  background: linear-gradient(
    115deg,
    transparent 42%,
    rgba(255, 255, 255, 0.06) 50%,
    transparent 58%
  );
  animation: sheenSweep 6s ease-in-out infinite;
}

.splash__title {
  font-family: var(--font-serif);
  font-size: clamp(26px, 6.5vw, 40px);
  font-weight: 500;
  letter-spacing: 14px;
  color: var(--silver-bright);
  text-shadow: 0 0 40px rgba(150, 185, 235, 0.2), 0 2px 10px rgba(0, 0, 0, 0.6);
  margin: 0 0 18px;
  opacity: 0;
  transform: translateY(24px);
  animation: fadeUp 1.4s cubic-bezier(0.22, 1, 0.36, 1) 0.4s forwards;
}

.splash__sep {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 44px;
  opacity: 0;
  transform: translateY(24px);
  animation: fadeUp 1.4s cubic-bezier(0.22, 1, 0.36, 1) 0.7s forwards;
}

.splash__sep .rule {
  width: 70px;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(216, 178, 90, 0.35) 30%,
    rgba(216, 178, 90, 0.35) 70%,
    transparent
  );
}

.splash__sep .gem {
  width: 5px;
  height: 5px;
  background: var(--gold);
  transform: rotate(45deg);
  box-shadow: 0 0 6px rgba(216, 178, 90, 0.4);
}

.splash__sep .phrase {
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 300;
  letter-spacing: 7px;
  color: var(--text-2);
  white-space: nowrap;
}

.splash__cta {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 15px 44px;
  border: 1px solid rgba(170, 195, 235, 0.22);
  border-radius: 50px;
  background: rgba(12, 18, 32, 0.45);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--text);
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 300;
  letter-spacing: 4px;
  cursor: pointer;
  overflow: hidden;
  opacity: 0;
  transform: translateY(24px);
  animation: fadeUp 1.4s cubic-bezier(0.22, 1, 0.36, 1) 1s forwards;
  transition: border-color 0.4s ease, box-shadow 0.4s ease;
}

.splash__cta:hover {
  border-color: rgba(216, 178, 90, 0.35);
  box-shadow: 0 0 40px rgba(90, 130, 200, 0.15), inset 0 0 40px rgba(90, 130, 200, 0.08);
}

.splash__cta:focus-visible {
  outline: 1px solid var(--gold);
  outline-offset: 3px;
}

.splash__cta .chev {
  font-size: 18px;
  color: var(--text-2);
  transition: transform 0.3s ease;
}

.splash__cta:hover .chev {
  transform: translateX(3px);
}

/* ---------- 阶段过渡（替代原型的 fade-overlay） ---------- */
.stage-enter-active,
.stage-leave-active {
  transition: opacity 0.55s cubic-bezier(0.4, 0, 0.2, 1),
    transform 0.55s cubic-bezier(0.4, 0, 0.2, 1);
}

.stage-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.stage-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* ---------- keyframes ---------- */
@keyframes fadeUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes emblemFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

@keyframes glowPulse {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.12);
  }
}

@keyframes orbit {
  to {
    transform: rotate(360deg);
  }
}

@keyframes sheenSweep {
  0%,
  100% {
    left: -60%;
  }
  50% {
    left: 110%;
  }
}

/* ---------- 桌面增强 ---------- */
@media (min-width: 768px) {
  .emblem {
    width: 240px;
    height: 240px;
    margin-bottom: 48px;
  }

  .splash__title {
    font-size: 56px;
    letter-spacing: 18px;
  }

  .splash__sep .rule {
    width: 110px;
  }

  .splash__sep .phrase {
    font-size: 16px;
    letter-spacing: 9px;
  }

  .splash__cta {
    padding: 17px 58px;
    font-size: 16px;
    letter-spacing: 5px;
  }
}

@media (min-width: 1200px) {
  .splash__title {
    font-size: 64px;
  }
}

@media (max-width: 420px) {
  .splash__title {
    letter-spacing: 8px;
  }

  .splash__sep .phrase {
    letter-spacing: 4px;
    font-size: 13px;
  }

  .splash__cta {
    padding: 13px 32px;
    font-size: 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .emblem,
  .emblem__halo,
  .emblem__orbit,
  .emblem__sheen::after {
    animation: none;
  }

  .splash__title,
  .splash__sep,
  .splash__cta {
    animation: none;
    opacity: 1;
    transform: none;
  }

  .stage-enter-active,
  .stage-leave-active {
    transition: opacity 0.2s linear;
  }

  .stage-enter-from,
  .stage-leave-to {
    transform: none;
  }
}
</style>
