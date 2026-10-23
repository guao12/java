<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import {
  LEVELS,
  RANK_POINTS,
  completeMission,
  isCompleted,
  level,
  levelLabel,
  nextLevel,
  points
} from '@/utils/clearance'
import { generateMissions } from '@/data/missions'

const router = useRouter()
const route = useRoute()

/** 跨页跳转：顶部导航由 SiteNav 统一渲染，这里给正文里的跳转按钮复用 */
function go(to) {
  if (to === route.path) {
    showToast('已在当前页面')
    return
  }
  router.push(to)
}

/* ---------------- 权限进度（完成任务累积） ---------------- */

const lv = computed(() => level.value)
const lvLabel = computed(() => levelLabel.value)
const next = computed(() => nextLevel.value)
const progress = computed(() => (lv.value >= LEVELS.length ? 100 : (next.value?.pct ?? 100)))

/** 轻提示：领取 / 完成 / 刷新 / 权限提升共用 */
const toast = ref('')
let toastTimer = 0
function showToast(text) {
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => (toast.value = ''), 2600)
}
onBeforeUnmount(() => clearTimeout(toastTimer))

/* ---------------- 执行部任务看板 ---------------- */

/** 看板任务（刷新时重新生成） */
const board = ref(generateMissions(6))
/** 已领取任务编号集合（会话内状态，刷新页面后重置） */
const claimed = reactive(new Set())

/** 刷新看板：重新下发一批任务，清空本次已领取状态 */
function refreshBoard() {
  board.value = generateMissions(6)
  claimed.clear()
  showToast('看板已刷新 · 新任务已下发')
}

/** 点击任务卡：招募中 → 领取；已领取 / 已完成 / 封存 不响应卡片点击 */
function claimMission(m) {
  if (m.status !== '招募中' || isCompleted(m.code)) return
  if (claimed.has(m.code)) return
  claimed.add(m.code)
  showToast(`已领取 ${m.code} · ${m.name}`)
}

/** 放弃已领取的任务（交还名额，不计权限点） */
function abandonMission(m) {
  if (claimed.has(m.code)) {
    claimed.delete(m.code)
    showToast(`已放弃 ${m.code}`)
  }
}

/** 完成任务：登记编号 + 累加权限点（同一编号不重复计入） */
function finishMission(m) {
  if (!claimed.has(m.code)) return
  const gain = completeMission(m.code, m.rank)
  claimed.delete(m.code)
  if (gain > 0) {
    showToast(
      `${m.code} 已完成 · 权限点 +${gain}（${points.value} 点 / ${lvLabel.value} 密级）`
    )
  } else {
    showToast(`${m.code} 此前已完成，不再计入权限点`)
  }
}

/** 卡片展示状态：已完成 > 已领取 > 原始状态 */
function stateOf(m) {
  if (isCompleted(m.code)) return '已完成'
  if (claimed.has(m.code)) return '已领取'
  return m.status
}

/** 该任务完成可获得的权限点 */
function gainOf(m) {
  return RANK_POINTS[m.rank] || 1
}
</script>

<template>
  <div class="missions">
    <RuneBackground />

    <!-- ============ 顶部导航：与首页同源 ============ -->
    <SiteNav />

    <!-- ============ 主体 ============ -->
    <main class="ms__body">
      <section class="ms__hero">
        <div class="ms__hero-row">
          <DragonEmblem class="ms__hero-mark" />
          <div class="ms__hero-text">
            <h1 class="ms__title">
              执行部任务看板<span class="ms__title-en">EXECUTION BUREAU · MISSIONS</span>
            </h1>
            <p class="ms__sub">
              卡塞尔学院执行部任务招募 · 招募中可点击领取，已领取可完成或放弃 · 完成任务累积权限点
            </p>
          </div>
          <button type="button" class="ms__refresh ms__refresh--hero" @click="refreshBoard">
            <AppIcon name="refresh" :size="14" /> 刷新看板
          </button>
        </div>
      </section>

      <!-- 权限进度：完成任务 → 权限点 → 提升密级 → 解锁档案室 -->
      <section class="perm">
        <div class="perm__head">
          <span class="perm__icon"><AppIcon name="shield" :size="16" /></span>
          <div class="perm__text">
            <b>当前权限 {{ points }} 点</b>
            <span>
              可查阅密级 <em>{{ lvLabel }}</em>
              <template v-if="next">
                · 距「{{ LEVELS[lv]?.label }}」还需 {{ next.remain }} 点
              </template>
              <template v-else>· 已达最高密级</template>
            </span>
          </div>
          <button type="button" class="perm__go" @click="go('/archive')">
            <AppIcon name="arrow" :size="13" /> 前往档案室查阅
          </button>
        </div>
        <div class="perm__bar"><i :style="{ width: progress + '%' }" /></div>
        <div class="perm__steps">
          <span
            v-for="l in LEVELS"
            :key="l.value"
            class="perm__step"
            :class="{ 'is-on': lv >= l.value }"
          >
            {{ l.label }}
          </span>
        </div>
      </section>

      <!-- 执行部任务看板 -->
      <section class="ms__board">
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="sparkle" :size="15" /> 执行部任务看板</h2>
          <div class="shelf__ops">
            <span class="shelf__more">共 {{ board.length }} 项任务 · 完成得权限点</span>
            <button type="button" class="ms__refresh" @click="refreshBoard">
              <AppIcon name="refresh" :size="13" /> 刷新
            </button>
          </div>
        </header>

        <div class="missions__grid">
          <article
            v-for="m in board"
            :key="m.code"
            class="mcard"
            :data-rank="m.rank"
            :data-status="stateOf(m)"
            :class="{ 'is-joined': claimed.has(m.code), 'is-done': isCompleted(m.code) }"
            role="button"
            tabindex="0"
            @click="claimMission(m)"
            @keydown.enter.prevent="claimMission(m)"
            @keydown.space.prevent="claimMission(m)"
          >
            <div class="mcard__top">
              <span class="mcard__code">{{ m.code }}</span>
              <span class="mcard__status">{{ stateOf(m) }}</span>
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

            <!-- 操作区：已完成 / 已领取 / 招募中 三态 -->
            <div class="mcard__actions">
              <span v-if="isCompleted(m.code)" class="mcard__done">
                <AppIcon name="check" :size="12" /> 已完成 · 权限点 +{{ gainOf(m) }}
              </span>
              <template v-else-if="claimed.has(m.code)">
                <button
                  type="button"
                  class="mcard__finish"
                  @click.stop="finishMission(m)"
                  @keydown.enter.stop.prevent="finishMission(m)"
                  @keydown.space.stop.prevent="finishMission(m)"
                >
                  <AppIcon name="check" :size="12" /> 完成 (+{{ gainOf(m) }})
                </button>
                <button
                  type="button"
                  class="mcard__cancel"
                  @click.stop="abandonMission(m)"
                  @keydown.enter.stop.prevent="abandonMission(m)"
                  @keydown.space.stop.prevent="abandonMission(m)"
                >
                  <AppIcon name="close" :size="12" /> 放弃
                </button>
              </template>
              <span v-else-if="m.status === '招募中'" class="mcard__tip">点击卡片领取</span>
              <span v-else class="mcard__tip">{{ m.status }} · 不可领取</span>
            </div>

            <p class="mcard__issued">{{ m.issued }}</p>
          </article>
        </div>
      </section>
    </main>

    <!-- 轻提示 -->
    <transition name="toast">
      <div v-if="toast" class="toast"><AppIcon name="info" :size="14" /> {{ toast }}</div>
    </transition>

    <!-- ============ 页脚 ============ -->
    <footer class="ms__foot">
      <span>以龙档案馆 · 同人兴趣站 · 非商用</span>
      <span class="ms__foot-meta">版权归原作者所有 · 仅作学习交流</span>
    </footer>
  </div>
</template>

<style scoped>
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

/* 已完成卡片：常亮绿边 */
.mcard.is-done {
  border-color: rgba(63, 174, 123, 0.55);
  box-shadow:
    inset 0 0 0 1px rgba(63, 174, 123, 0.24),
    0 0 26px -12px rgba(63, 174, 123, 0.6);
}

.mcard[data-status='已领取'] .mcard__status {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.55);
  background: rgba(216, 178, 90, 0.12);
}

.mcard[data-status='已完成'] .mcard__status {
  color: #7ee2b0;
  border-color: rgba(63, 174, 123, 0.6);
  background: rgba(63, 174, 123, 0.14);
}

.mcard__finish {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 15px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.55);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.22), rgba(216, 178, 90, 0.08));
  color: var(--gold-lt);
  font-size: 11.5px;
  letter-spacing: 0.1em;
  font-family: var(--font-mono);
  cursor: pointer;
  transition: background 0.24s, border-color 0.24s, transform 0.18s;
}

.mcard__finish:hover {
  background: rgba(216, 178, 90, 0.32);
  border-color: rgba(216, 178, 90, 0.9);
  transform: translateY(-1px);
}

.mcard__finish:active {
  transform: translateY(0);
}

.mcard__done {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 13px;
  border-radius: 999px;
  border: 1px solid rgba(63, 174, 123, 0.45);
  background: rgba(63, 174, 123, 0.12);
  color: #8ee9b8;
  font-size: 11.5px;
  letter-spacing: 0.1em;
  font-family: var(--font-mono);
}

.mcard__tip {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

/* 看板操作区（条数 + 刷新按钮） */
.shelf__ops {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ms__refresh {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 15px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.1);
  color: var(--gold-lt);
  font-size: 12px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: background 0.24s, transform 0.2s;
}

.ms__refresh:hover {
  background: rgba(216, 178, 90, 0.22);
  transform: translateY(-1px);
}

/* ================= 权限进度面板 ================= */
.perm {
  margin-bottom: 22px;
  padding: 16px 20px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  box-shadow: 0 0 24px -14px rgba(216, 178, 90, 0.4);
}

.perm__head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.perm__icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.1);
  border: 1px solid rgba(216, 178, 90, 0.4);
}

.perm__text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 12.5px;
  color: var(--text-3);
}

.perm__text b {
  font-family: var(--font-serif);
  font-size: 14.5px;
  letter-spacing: 0.08em;
  color: var(--text);
}

.perm__text em {
  font-style: normal;
  color: var(--gold-lt);
}

.perm__go {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.1);
  color: var(--gold-lt);
  font-size: 12px;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: background 0.24s, transform 0.2s;
}

.perm__go:hover {
  background: rgba(216, 178, 90, 0.2);
  transform: translateY(-1px);
}

.perm__bar {
  margin-top: 14px;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.perm__bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--gold-antique), var(--gold-lt));
  box-shadow: 0 0 12px rgba(216, 178, 90, 0.6);
  transition: width 0.5s ease;
}

.perm__steps {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--text-4);
}

.perm__step {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
}

.perm__step.is-on {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.1);
}

/* ================= 轻提示 ================= */
.toast {
  position: fixed;
  left: 50%;
  bottom: 34px;
  z-index: 60;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.96), rgba(14, 11, 23, 0.98));
  box-shadow: 0 16px 40px -20px rgba(0, 0, 0, 0.9), 0 0 24px -12px var(--gold-glow);
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: var(--gold-lt);
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.24s, transform 0.24s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
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

  .ms__title {
    font-size: 22px;
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
