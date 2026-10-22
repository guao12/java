<script setup>
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import { getUser } from '@/utils/auth'
import { MISSION_GALLERY } from '@/data/missions'

const router = useRouter()
const route = useRoute()

const user = getUser()
const codename = user?.codename || '未知混血种'

/* 跨页导航：与「血统鉴定」页面同源结构（学院首页 / 执行部任务 / 血统鉴定 / 我的身份卡） */
const NAV = [
  { label: '学院首页', to: '/home' },
  { label: '执行部任务', to: '/missions' },
  { label: '血统鉴定', to: '/appraisal' },
  { label: '我的身份卡', to: '/console' }
]

function isCurrent(to) {
  return route.path === to
}
function go(to) {
  if (to !== route.path) router.push(to)
}

/* ---------------- 执行部任务看板 ---------------- */

/** 已报名任务编号集合（会话内状态，刷新后重置） */
const joined = reactive(new Set())

/** 点击任务卡：招募中 → 领取（报名）；其余状态不可领取（保持轻量，无弹窗） */
function claimMission(m) {
  if (m.status !== '招募中') return
  if (joined.has(m.code)) return
  joined.add(m.code)
}

/** 取消已领取的任务（仅对“已领取”状态生效，交还名额） */
function cancelMission(m) {
  if (joined.has(m.code)) joined.delete(m.code)
}
</script>

<template>
  <div class="missions">
    <RuneBackground />

    <!-- ============ 顶部导航 ============ -->
    <header class="ms__nav">
      <div class="ms__nav-inner">
        <div class="ms__brand">
          <DragonEmblem class="ms__mark" />
          <div class="ms__brand-text">
            <span class="ms__brand-cn">卡塞尔学院</span>
            <span class="ms__brand-en">Cassell College</span>
          </div>
        </div>

        <nav class="ms__links">
          <button
            v-for="item in NAV"
            :key="item.to"
            type="button"
            class="ms__link"
            :class="{ 'is-current': isCurrent(item.to) }"
            :aria-current="isCurrent(item.to) ? 'page' : undefined"
            @click="go(item.to)"
          >
            {{ item.label }}
          </button>
        </nav>

        <!-- 当前用户 -->
        <div class="ms__user" :title="codename">
          <span class="ms__user-avatar">{{ codename.slice(0, 1) }}</span>
          <span class="ms__user-name">{{ codename }}</span>
        </div>
      </div>
    </header>

    <!-- ============ 主体 ============ -->
    <main class="ms__body">
      <section class="ms__hero">
        <div class="ms__hero-row">
          <DragonEmblem class="ms__hero-mark" />
          <div class="ms__hero-text">
            <h1 class="ms__title">
              执行部任务看板<span class="ms__title-en">EXECUTION BUREAU · MISSIONS</span>
            </h1>
            <p class="ms__sub">卡塞尔学院执行部任务招募 · 招募中可点击领取，已领取可取消</p>
          </div>
        </div>
      </section>

      <!-- 执行部任务看板 -->
      <section class="ms__board">
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="sparkle" :size="15" /> 执行部任务看板</h2>
          <span class="shelf__more">共 {{ MISSION_GALLERY.length }} 项任务 · 招募中可点击领取，已领取可取消</span>
        </header>

        <div class="missions__grid">
          <article
            v-for="m in MISSION_GALLERY"
            :key="m.code"
            class="mcard"
            :data-rank="m.rank"
            :data-status="m.status"
            :class="{ 'is-joined': joined.has(m.code) }"
            role="button"
            tabindex="0"
            @click="claimMission(m)"
            @keydown.enter.prevent="claimMission(m)"
            @keydown.space.prevent="claimMission(m)"
          >
            <div class="mcard__top">
              <span class="mcard__code">{{ m.code }}</span>
              <span class="mcard__status">{{ joined.has(m.code) ? '已领取' : m.status }}</span>
            </div>
            <h3 class="mcard__name">{{ m.name }}</h3>
            <p class="mcard__desc">{{ m.desc }}</p>
            <div class="rcard__perks mcard__tags">
              <span v-for="t in m.tags" :key="t">{{ t }}</span>
            </div>
            <div class="mcard__foot">
              <span class="mcard__region">{{ m.region }}</span>
              <span class="mcard__rank">{{ m.rank }}</span>
            </div>
            <div class="mcard__reward">{{ m.reward }}</div>
            <div v-if="joined.has(m.code)" class="mcard__actions">
              <button
                type="button"
                class="mcard__cancel"
                @click.stop="cancelMission(m)"
                @keydown.enter.stop.prevent="cancelMission(m)"
                @keydown.space.stop.prevent="cancelMission(m)"
              >
                <AppIcon name="close" :size="12" /> 取消领取
              </button>
            </div>
            <p class="mcard__issued">{{ m.issued }}</p>
          </article>
        </div>
      </section>
    </main>

    <!-- ============ 页脚 ============ -->
    <footer class="ms__foot">
      <span>以龙档案馆 · 同人兴趣站 · 非商用</span>
      <span class="ms__foot-meta">版权归原作者所有 · 仅作学习交流</span>
    </footer>
  </div>
</template>

<style scoped>
/* ================= 顶部导航 ================= */
.ms__nav {
  position: sticky;
  top: 0;
  z-index: 30;
  width: 100%;
  padding: 14px 24px;
  background: linear-gradient(180deg, rgba(5, 7, 13, 0.92), rgba(5, 7, 13, 0.55));
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
  border-bottom: 1px solid var(--line-soft);
  animation: fade-down 0.7s 0.05s both;
}

.ms__nav-inner {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.ms__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ms__mark {
  width: 34px;
  height: 34px;
  filter: drop-shadow(0 0 10px rgba(216, 178, 90, 0.5));
  animation: spin 46s linear infinite;
}

.ms__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.ms__brand-cn {
  font-family: var(--font-serif);
  font-size: 15px;
  letter-spacing: 0.26em;
  color: var(--gold-lt);
}

.ms__brand-en {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.3em;
  color: var(--text-4);
  text-transform: uppercase;
}

.ms__links {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 导航项（button 需重置默认样式） */
.ms__link {
  position: relative;
  padding: 8px 16px;
  border: 0;
  background: none;
  font: inherit;
  font-size: 13px;
  letter-spacing: 0.1em;
  color: var(--text-3);
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.28s, background 0.28s, box-shadow 0.28s;
}

.ms__link:hover {
  color: var(--text);
  background: rgba(255, 255, 255, 0.04);
}

/* 当前页面高亮 */
.ms__link.is-current {
  color: var(--gold-lt);
  font-weight: 600;
  background: linear-gradient(135deg, rgba(216, 178, 90, 0.16), rgba(216, 178, 90, 0.04));
  box-shadow: inset 0 0 0 1px rgba(216, 178, 90, 0.26), 0 0 20px -10px var(--gold-glow);
}

.ms__link.is-current::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 1px;
  width: 16px;
  height: 2px;
  transform: translateX(-50%);
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, var(--gold-lt), transparent);
  box-shadow: 0 0 10px rgba(216, 178, 90, 0.9);
  animation: nav-glow 2.6s ease-in-out infinite;
}

.ms__user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 14px;
  border-left: 1px solid var(--line-soft);
}

.ms__user-avatar {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.1);
  border: 1px solid rgba(216, 178, 90, 0.4);
}

.ms__user-name {
  font-size: 12.5px;
  letter-spacing: 0.08em;
  color: var(--text-2);
}

/* ================= 主体 ================= */
.ms__body {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 30px 24px 64px;
}

.ms__hero {
  margin-bottom: 26px;
}

.ms__hero-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ms__hero-mark {
  width: 52px;
  height: 52px;
  filter: drop-shadow(0 0 14px rgba(216, 178, 90, 0.45));
  animation: spin 60s linear infinite;
}

.ms__hero-text {
  display: flex;
  flex-direction: column;
}

.ms__title {
  font-family: var(--font-serif);
  font-size: 26px;
  letter-spacing: 0.12em;
  color: var(--text);
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
}

.ms__title-en {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.28em;
  color: var(--gold-dp);
  text-transform: uppercase;
}

.ms__sub {
  margin-top: 6px;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--text-3);
}

/* ================= 区块标题（与首页同源样式） ================= */
.shelf__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px 13px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.26); /* 暗金发丝线 */
  margin-bottom: 16px;
}

.shelf__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  position: relative;
  padding-left: 15px;
  font-family: var(--font-serif);
  font-size: 17px;
  letter-spacing: 0.14em;
  color: var(--text);
}

/* 暗金装饰立柱 */
.shelf__title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 15px;
  border-radius: 2px;
  transform: translateY(-50%);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold) 40%, var(--gold-antique));
  box-shadow: 0 0 8px -2px var(--gold-glow);
}

.shelf__more {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

/* ================= 执行部任务看板 ================= */
.missions__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  grid-auto-rows: 1fr; /* 行内等高，网格对齐 */
  gap: 16px;
}

.mcard {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 18px 20px 16px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  box-shadow: 0 0 0 1px rgba(216, 178, 90, 0.05), 0 0 22px -14px rgba(216, 178, 90, 0.3);
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.28s, transform 0.28s, box-shadow 0.28s;
}

/* 等高卡片：末块沉底 */
.mcard > .mcard__issued {
  margin-top: auto;
}

/* 悬停扫光 */
.mcard::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 2;
  width: 42%;
  background: linear-gradient(100deg, transparent, rgba(242, 220, 155, 0.12), transparent);
  pointer-events: none;
  transform: translateX(-130%) skewX(-16deg);
}

.mcard:hover {
  border-color: rgba(216, 178, 90, 0.45);
  transform: translateY(-5px) scale(1.012);
  animation: card-glow 2.4s ease-in-out infinite;
}

.mcard:hover::before {
  animation: card-sheen 0.9s cubic-bezier(0.5, 0, 0.2, 1) both;
}

.mcard:focus-visible {
  outline: 2px solid var(--gold);
  outline-offset: 2px;
}

/* 已报名：常亮描边 */
.mcard.is-joined {
  border-color: rgba(216, 178, 90, 0.6);
  box-shadow:
    inset 0 0 0 1px rgba(216, 178, 90, 0.28),
    0 0 26px -10px var(--gold-glow);
}

.mcard__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.mcard__code {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--gold-dp);
  padding: 3px 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(216, 178, 90, 0.06);
}

/* 状态徽标 */
.mcard__status {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.14em;
  padding: 3px 11px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-3);
  background: rgba(4, 7, 13, 0.5);
}

.mcard[data-status='招募中'] .mcard__status {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.55);
  background: rgba(216, 178, 90, 0.1);
  box-shadow: 0 0 14px -4px var(--gold-glow);
  animation: nav-glow 2.6s ease-in-out infinite;
}

.mcard[data-status='进行中'] .mcard__status {
  color: #a5e4ff;
  border-color: rgba(74, 168, 212, 0.55);
  background: rgba(74, 168, 212, 0.12);
}

.mcard[data-status='已完结'] .mcard__status {
  color: #7ee2b0;
  border-color: rgba(63, 174, 123, 0.55);
  background: rgba(63, 174, 123, 0.1);
}

.mcard[data-status='封存'] .mcard__status {
  color: var(--err);
  border-color: rgba(239, 107, 98, 0.45);
  background: rgba(179, 39, 45, 0.12);
}

.mcard__name {
  font-family: var(--font-serif);
  font-size: 16.5px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--text);
}

.mcard__desc {
  margin-top: 8px;
  font-size: 12.5px;
  line-height: 1.75;
  letter-spacing: 0.04em;
  color: var(--text-3);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 标签容器（与言灵/机构卡片同源） */
.rcard__perks {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.mcard__tags {
  margin-top: 10px;
}

.mcard__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

/* 难度徽标：随等级着色 */
.mcard__rank {
  flex: 0 0 auto;
  padding: 2px 10px;
  border-radius: 999px;
  border: 1px solid rgba(138, 106, 40, 0.5);
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.08);
}

.mcard[data-rank='高危'] .mcard__rank {
  color: var(--blood-lt);
  border-color: rgba(226, 86, 79, 0.45);
  background: rgba(179, 39, 45, 0.12);
}

.mcard[data-rank='绝密'] .mcard__rank {
  color: #f58d86;
  border-color: rgba(239, 107, 98, 0.6);
  background: rgba(179, 39, 45, 0.2);
  text-shadow: 0 0 10px rgba(239, 107, 98, 0.5);
}

.mcard__reward {
  margin-top: 8px;
  font-size: 11.5px;
  letter-spacing: 0.05em;
  color: var(--gold-dp);
}

.mcard__reward::before {
  content: '酬劳 ';
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.2em;
  color: var(--text-4);
}

.mcard__issued {
  margin-top: 5px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

/* 招募中卡片底部领取提示 */
.mcard[data-status='招募中']:not(.is-joined) .mcard__reward::after {
  content: ' · 点击领取';
  color: var(--gold-lt);
  opacity: 0.85;
}

.mcard.is-joined .mcard__issued {
  color: var(--gold-lt);
}

.mcard.is-joined .mcard__issued::before {
  content: '已领取 · ';
}

/* 已领取卡片：取消按钮 */
.mcard__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.mcard__cancel {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 13px;
  border-radius: 999px;
  border: 1px solid rgba(239, 107, 98, 0.4);
  background: rgba(179, 39, 45, 0.1);
  color: #f0a39c;
  font-size: 11px;
  letter-spacing: 0.12em;
  font-family: var(--font-mono);
  cursor: pointer;
  transition: background 0.24s, border-color 0.24s, color 0.24s, transform 0.18s;
}

.mcard__cancel:hover {
  background: rgba(179, 39, 45, 0.22);
  border-color: rgba(239, 107, 98, 0.7);
  color: #ffd9d6;
  transform: translateY(-1px);
}

.mcard__cancel:active {
  transform: translateY(0);
}

.mcard__cancel svg {
  opacity: 0.85;
}

/* ================= 页脚 ================= */
.ms__foot {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 22px 24px 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid var(--line-soft);
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

/* ================= 关键帧（与首页任务卡同源） ================= */
@keyframes nav-glow {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

@keyframes card-glow {
  0%,
  100% {
    box-shadow: 0 22px 50px -24px rgba(0, 0, 0, 0.9), 0 0 22px -12px rgba(216, 178, 90, 0.38);
  }
  50% {
    box-shadow:
      0 26px 60px -24px rgba(0, 0, 0, 0.95),
      0 0 0 1px rgba(216, 178, 90, 0.26),
      0 0 48px -8px rgba(216, 178, 90, 0.66);
  }
}

@keyframes card-sheen {
  from {
    transform: translateX(-130%) skewX(-16deg);
  }
  to {
    transform: translateX(340%) skewX(-16deg);
  }
}

/* ================= 响应式 ================= */
@media (max-width: 560px) {
  .missions__grid {
    grid-template-columns: 1fr;
  }

  .ms__nav-inner {
    flex-wrap: wrap;
  }

  .ms__title {
    font-size: 22px;
  }

  .ms__user {
    display: none;
  }
}

/* ================= 尊重系统的「减少动态效果」设置 ================= */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
