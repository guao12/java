<script setup>
/**
 * 身份卡实时预览 —— 随输入即时刷新，带扫描纹与血统评级
 * state: idle（待刷卡）| scan（识别中）| ok（已签发 / 认证通过）
 */
import { computed } from 'vue'
import DragonEmblem from './DragonEmblem.vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  name: { type: String, default: '' },
  account: { type: String, default: '' },
  identity: { type: String, default: '' },
  spirit: { type: String, default: '' },
  grade: { type: String, default: '' },
  state: { type: String, default: 'idle' },
  status: { type: String, default: '' },
  serial: { type: String, default: '' }
})

const shownName = computed(() => props.name || '待 识 别')
const shownAccount = computed(() => props.account || '— — — — — — — —')
const shownGrade = computed(() => props.grade || '—')

const statusText = computed(() => {
  if (props.status) return props.status
  if (props.state === 'ok') return '已签发 · 通行有效'
  if (props.state === 'scan') return '正在读取龙文…'
  return '等待刷卡 / 等待登记'
})

/** 条形码：由编号派生，保证同一编号始终同一图案 */
const bars = computed(() => {
  const seed = `${props.account || props.name || 'dragon'}${props.serial || ''}`
  let h = 7
  for (let i = 0; i < seed.length; i += 1) h = (h * 31 + seed.charCodeAt(i)) % 100003
  return Array.from({ length: 34 }, (_, i) => {
    h = (h * 1103515245 + 12345 + i * 7) % 2147483648
    return 1 + (h % 3)
  })
})
</script>

<template>
  <div class="idcard" :class="`idcard--${state}`">
    <span v-if="state !== 'idle'" class="idcard__scan" aria-hidden="true" />

    <div class="idcard__head">
      <span class="idcard__emblem"><DragonEmblem /></span>
      <div class="idcard__who">
        <p class="idcard__org">CASSELL COLLEGE · 身份卡</p>
        <p class="idcard__name">{{ shownName }}</p>
        <p class="idcard__code">{{ shownAccount }}</p>
      </div>
      <span class="idcard__grade">
        <i>{{ shownGrade }}</i>
        <em>血统</em>
      </span>
    </div>

    <div class="idcard__meta">
      <span class="idcard__chip">
        <AppIcon name="id" :size="12" />{{ identity || '身份未登记' }}
      </span>
      <span class="idcard__chip">
        <AppIcon name="flame" :size="12" />{{ spirit || '言灵未鉴定' }}
      </span>
    </div>

    <div class="idcard__foot">
      <span class="idcard__bars" aria-hidden="true">
        <i v-for="(w, i) in bars" :key="i" :style="{ width: w + 'px' }" />
      </span>
      <span class="idcard__status">
        <AppIcon :name="state === 'ok' ? 'check' : 'shield'" :size="12" />
        {{ statusText }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.idcard {
  position: relative;
  overflow: hidden;
  padding: 13px 15px 11px;
  border-radius: 13px;
  border: 1px solid var(--line-soft);
  background: linear-gradient(150deg, rgba(20, 28, 46, 0.82), rgba(6, 10, 18, 0.9));
  transition: border-color 0.4s, box-shadow 0.4s;
}
.idcard--scan {
  border-color: rgba(127, 212, 255, 0.35);
}
.idcard--ok {
  border-color: rgba(216, 178, 90, 0.62);
  box-shadow: 0 0 0 1px rgba(216, 178, 90, 0.12), 0 12px 34px -18px var(--gold-glow);
}

/* 扫描纹 */
.idcard__scan {
  position: absolute;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(
    180deg,
    transparent,
    rgba(127, 212, 255, 0.16),
    rgba(216, 178, 90, 0.2),
    transparent
  );
  animation: idcard-sweep 2.2s ease-in-out infinite;
  pointer-events: none;
}
.idcard--ok .idcard__scan {
  background: linear-gradient(180deg, transparent, rgba(216, 178, 90, 0.22), transparent);
}
@keyframes idcard-sweep {
  0% {
    transform: translateY(-70px);
    opacity: 0;
  }
  20% {
    opacity: 1;
  }
  80% {
    opacity: 1;
  }
  100% {
    transform: translateY(130px);
    opacity: 0;
  }
}

.idcard__head {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
}
.idcard__emblem {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  opacity: 0.55;
  transition: opacity 0.4s, filter 0.4s;
}
.idcard--scan .idcard__emblem,
.idcard--ok .idcard__emblem {
  opacity: 1;
  filter: drop-shadow(0 0 10px rgba(216, 178, 90, 0.45));
}

.idcard__who {
  flex: 1;
  min-width: 0;
}
.idcard__org {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.22em;
  color: var(--text-4);
  text-transform: uppercase;
}
.idcard__name {
  font-family: var(--font-serif);
  font-size: 17px;
  letter-spacing: 0.1em;
  color: var(--text);
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.idcard--ok .idcard__name {
  color: var(--gold-lt);
  text-shadow: 0 0 18px rgba(216, 178, 90, 0.35);
}
.idcard__code {
  font-family: var(--font-mono);
  font-size: 11.5px;
  letter-spacing: 0.18em;
  color: var(--text-3);
}

.idcard__grade {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  padding: 4px 9px;
  border-radius: 9px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.6);
}
.idcard__grade i {
  font-family: var(--font-mono);
  font-style: normal;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text-3);
  transition: color 0.4s, text-shadow 0.4s;
}
.idcard--ok .idcard__grade i {
  color: var(--gold);
  text-shadow: 0 0 14px rgba(216, 178, 90, 0.5);
}
.idcard__grade em {
  font-style: normal;
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.idcard__meta {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 10px;
}
.idcard__chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.45);
  font-size: 11px;
  letter-spacing: 0.04em;
  color: var(--text-3);
  white-space: nowrap;
}
.idcard--ok .idcard__chip {
  color: var(--text-2);
  border-color: rgba(216, 178, 90, 0.28);
}

.idcard__foot {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 11px;
  padding-top: 9px;
  border-top: 1px dashed rgba(255, 255, 255, 0.08);
}
.idcard__bars {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 16px;
  overflow: hidden;
  opacity: 0.5;
}
.idcard__bars i {
  display: block;
  height: 100%;
  background: linear-gradient(180deg, var(--gold-dp), rgba(216, 178, 90, 0.25));
  border-radius: 1px;
}
.idcard__status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-4);
  white-space: nowrap;
}
.idcard--scan .idcard__status {
  color: var(--ice);
}
.idcard--ok .idcard__status {
  color: var(--gold);
}

@media (prefers-reduced-motion: reduce) {
  .idcard__scan {
    display: none;
  }
}
</style>
