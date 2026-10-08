<template>
  <div class="driver"
       title="创骑驱动器 · 点击瓶槽装填 / 点击核心判定 / 点击拉杆变身"
       @click="onClick">
    <svg class="driver-svg" viewBox="0 0 180 100" xmlns="http://www.w3.org/2000/svg"
         :style="{ '--c-left': theme.left, '--c-right': theme.right }">
      <defs>
        <linearGradient id="dvBody" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#2c2f38"/><stop offset="100%" stop-color="#0b0c0f"/>
        </linearGradient>
        <linearGradient id="dvMetal" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stop-color="#3a3e48"/><stop offset="50%" stop-color="#15171c"/><stop offset="100%" stop-color="#3a3e48"/>
        </linearGradient>
        <linearGradient id="dvGold" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#f5c518"/><stop offset="100%" stop-color="#c79a00"/>
        </linearGradient>
        <linearGradient id="dvRain" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stop-color="#ff3b3b"/><stop offset="33%" stop-color="#f5c518"/>
          <stop offset="66%" stop-color="#00a6e6"/><stop offset="100%" stop-color="#7ad7ff"/>
        </linearGradient>
        <radialGradient id="dvCap" cx="50%" cy="32%" r="75%">
          <stop offset="0%" stop-color="#ffe88a"/><stop offset="100%" stop-color="#c79a00"/>
        </radialGradient>
        <clipPath id="slotL"><rect x="26" y="20" width="26" height="52" rx="11"/></clipPath>
        <clipPath id="slotR"><rect x="128" y="20" width="26" height="52" rx="11"/></clipPath>
        <clipPath id="core"><circle cx="90" cy="50" r="17"/></clipPath>
      </defs>

      <!-- 主体面板 -->
      <rect x="8" y="14" width="164" height="72" rx="20" fill="url(#dvBody)" stroke="url(#dvGold)" stroke-width="2.5"/>
      <rect x="14" y="20" width="152" height="60" rx="15" fill="none" stroke="#2a2d35" stroke-width="1"/>

      <!-- 四角铆钉 -->
      <g fill="#caa200">
        <circle cx="20" cy="26" r="2.3"/><circle cx="160" cy="26" r="2.3"/>
        <circle cx="20" cy="74" r="2.3"/><circle cx="160" cy="74" r="2.3"/>
      </g>

      <!-- 左满瓶槽（兔子） -->
      <g class="dv-slot" data-side="left">
        <rect x="26" y="20" width="26" height="52" rx="11" fill="#050608" stroke="#2a2d35" stroke-width="1.5"/>
        <g clip-path="url(#slotL)">
          <g class="dv-bottle" ref="bottleL">
            <rect x="31" y="27" width="16" height="40" rx="7" fill="var(--c-left,#ff3b3b)"/>
            <rect x="31" y="27" width="16" height="9" rx="4" fill="rgba(255,255,255,.24)"/>
            <ellipse cx="39" cy="47" rx="4" ry="11" fill="rgba(255,255,255,.16)"/>
            <rect x="36" y="20" width="6" height="9" rx="2" fill="url(#dvCap)"/>
          </g>
        </g>
        <rect x="33" y="16" width="12" height="6" rx="2" fill="url(#dvGold)"/>
      </g>

      <!-- 右满瓶槽（坦克） -->
      <g class="dv-slot" data-side="right">
        <rect x="128" y="20" width="26" height="52" rx="11" fill="#050608" stroke="#2a2d35" stroke-width="1.5"/>
        <g clip-path="url(#slotR)">
          <g class="dv-bottle" ref="bottleR">
            <rect x="133" y="27" width="16" height="40" rx="7" fill="var(--c-right,#00a6e6)"/>
            <rect x="133" y="27" width="16" height="9" rx="4" fill="rgba(255,255,255,.24)"/>
            <ellipse cx="141" cy="47" rx="4" ry="11" fill="rgba(255,255,255,.16)"/>
            <rect x="138" y="20" width="6" height="9" rx="2" fill="url(#dvCap)"/>
          </g>
        </g>
        <rect x="135" y="16" width="12" height="6" rx="2" fill="url(#dvGold)"/>
      </g>

      <!-- 中央 Build Reactor 反应器（双满瓶 Best Match 徽记） -->
      <g class="dv-core-wrap">
        <circle cx="90" cy="50" r="23" fill="#0a0b0e" stroke="url(#dvGold)" stroke-width="3"/>
        <circle class="dv-gear" ref="gear" cx="90" cy="50" r="21" fill="none" stroke="url(#dvGold)" stroke-width="2.5" stroke-dasharray="3 5"/>
        <circle class="dv-core" ref="core" cx="90" cy="50" r="17" fill="#0a0b0e" stroke="url(#dvGold)" stroke-width="2"/>
        <g clip-path="url(#core)">
          <!-- 左兔瓶 -->
          <rect x="74" y="40" width="15" height="24" rx="6" fill="var(--c-left,#ff3b3b)"/>
          <rect x="74" y="40" width="15" height="6" rx="3" fill="rgba(255,255,255,.22)"/>
          <rect x="79" y="33" width="5" height="8" rx="2" fill="url(#dvCap)"/>
          <ellipse cx="81.5" cy="52" rx="3" ry="8" fill="rgba(255,255,255,.16)"/>
          <!-- 右坦瓶 -->
          <rect x="91" y="40" width="15" height="24" rx="6" fill="var(--c-right,#00a6e6)"/>
          <rect x="91" y="40" width="15" height="6" rx="3" fill="rgba(255,255,255,.22)"/>
          <rect x="96" y="33" width="5" height="8" rx="2" fill="url(#dvCap)"/>
          <ellipse cx="98.5" cy="52" rx="3" ry="8" fill="rgba(255,255,255,.16)"/>
        </g>
        <line x1="90" y1="33" x2="90" y2="67" stroke="url(#dvGold)" stroke-width="1.5"/>
        <path d="M90,37 L94,41 L90,45 L86,41 Z" fill="url(#dvGold)"/>
        <circle class="dv-corebtn" cx="90" cy="50" r="3" fill="url(#dvCap)"/>
      </g>
      <text x="90" y="87" text-anchor="middle" font-size="7" letter-spacing="2" fill="#caa200" font-family="sans-serif" font-weight="700">BUILD</text>

      <!-- 右侧 Vortex Lever 拉杆 -->
      <g class="dv-lever" ref="lever">
        <rect x="150" y="42" width="11" height="24" rx="5.5" fill="url(#dvMetal)"/>
        <circle cx="155.5" cy="42" r="6.5" fill="#e02828" stroke="#fff" stroke-opacity=".35"/>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { FORMS } from '../data/forms.js'

const props = defineProps({ formKey: { type: String, required: true } })
const emit = defineEmits(['bottle', 'judge', 'lever'])

const theme = computed(() => FORMS[props.formKey].theme)

const lever = ref(null)
const gear = ref(null)
const core = ref(null)
const bottleL = ref(null)
const bottleR = ref(null)

// 装填满瓶：瓶身咔哒回弹
function loadBottle(side) {
  const b = side === 'left' ? bottleL.value : bottleR.value
  if (!b) return
  b.animate(
    [{ transform: 'translateY(0)' }, { transform: 'translateY(5px)' }, { transform: 'translateY(0)' }],
    { duration: 280, easing: 'ease-out' }
  )
}

// 中央核心判定：齿轮旋转 + 核心爆闪
function coreJudge() {
  if (gear.value) gear.value.animate([{ transform: 'rotate(0deg)' }, { transform: 'rotate(360deg)' }], { duration: 700, easing: 'ease-out' })
  if (core.value) core.value.animate(
    [{ strokeWidth: 2, opacity: 1 }, { strokeWidth: 8, opacity: .3 }, { strokeWidth: 2, opacity: 1 }],
    { duration: 600 }
  )
}

// 拉动 Vortex 拉杆：旋转回弹
function pullLever() {
  if (!lever.value) return
  lever.value.classList.add('pull')
  setTimeout(() => lever.value.classList.remove('pull'), 430)
}

function onClick(e) {
  e.stopPropagation()
  if (e.target.closest('.dv-lever')) { pullLever(); emit('lever'); return }
  if (e.target.closest('.dv-core-wrap')) { coreJudge(); emit('judge'); return }
  const slot = e.target.closest('.dv-slot')
  if (slot) { loadBottle(slot.dataset.side); emit('bottle', slot.dataset.side); return }
}

defineExpose({ loadBottle, coreJudge, pullLever })
</script>
