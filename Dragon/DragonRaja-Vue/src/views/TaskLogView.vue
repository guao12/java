<script setup>
/**
 * 任务执行记录详情页 —— /log/:code/:person
 *
 * 档案室「任务执行记录」卷四的展开页：把一条记录讲完整。
 * 视觉语言与人物档案 / 言灵档案同源：鎏金卷宗卡 + 密级条 + 分节。
 *
 * 密级不足时不渲染任何正文，只给出封存提示（与档案室口径一致）。
 */
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import { ARCHIVE_MISSION_LOGS } from '@/data/archive'
import { logDetail, relicLink } from '@/data/logDetail'
import { LEVELS, hasClearance, level, pointsTo } from '@/utils/clearance'

const route = useRoute()
const router = useRouter()

/** 定位记录：编号 + 人名（人名在 URL 里是编码过的中文） */
const source = computed(() => {
  const code = String(route.params.code || '')
  const person = decodeURIComponent(String(route.params.person || ''))
  return (
    ARCHIVE_MISSION_LOGS.find((m) => m.code === code && m.person === person) ||
    ARCHIVE_MISSION_LOGS.find((m) => m.code === code) ||
    null
  )
})

const log = computed(() => logDetail(source.value))

const readable = computed(() => {
  const l = log.value
  if (!l) return false
  return level.value >= Math.max(l.req || 1, l.level || 1)
})

/** 需要的密级标签 */
const needLabel = computed(() => {
  const l = log.value
  if (!l) return ''
  return LEVELS[Math.max(l.req || 1, l.level || 1) - 1]?.label || '更高'
})

/** 同类任务（同编号的其他执行人），用于底部互跳 */
const sameOp = computed(() => {
  const l = log.value
  if (!l) return []
  return ARCHIVE_MISSION_LOGS.filter((m) => m.code === l.code && m.person !== l.person)
})

/** 武器 / 言灵标签：能在图鉴里找到的就渲染成可跳转 */
const relics = computed(() => (log.value?.relics || []).map(relicLink))

onMounted(() => {
  document.documentElement.style.colorScheme = 'dark'
  document.title = log.value
    ? `任务记录 · ${log.value.name} | 卡塞尔学院`
    : '任务记录 | 卡塞尔学院'
})

function goBack() {
  router.push({ path: '/archive', hash: '#tab-logs' })
}

function go(path) {
  router.push(path)
}

function goLog(m) {
  router.push(`/log/${m.code}/${encodeURIComponent(m.person)}`)
}
</script>

<template>
  <div class="logd">
    <RuneBackground />

    <SiteNav>
      <template #actions>
        <button type="button" class="logd__back" @click="goBack">
          <AppIcon name="arrow" :size="14" class="logd__back-ic" />
          <span class="logd__back-text">返回档案室</span>
        </button>
      </template>
    </SiteNav>

    <main class="logd__body">
      <!-- ===== 正常阅读 ===== -->
      <article v-if="log && readable" class="dossier">
        <i class="dossier__beam" />

        <!-- 档案头 -->
        <header class="dossier__head">
          <div class="dossier__code">
            <span class="dossier__code-chip">{{ log.code }}</span>
            <i class="dossier__code-line" />
          </div>

          <div class="dossier__info">
            <p class="dossier__cat">
              <span class="dossier__cat-chip">任务执行记录</span>
              <span class="dossier__op">{{ log.op }}</span>
            </p>
            <h1 class="dossier__name">{{ log.name }}</h1>
            <p class="dossier__role">
              执行人
              <button
                v-if="log.figures.find((f) => f.name === log.person && f.link)"
                type="button"
                class="logd__link logd__link--strong"
                @click="go(`/codex/${log.personKey}`)"
              >
                {{ log.person }}
              </button>
              <span v-else>{{ log.person }}</span>
              · {{ log.deptLabel }}
            </p>
            <p class="dossier__desc">{{ log.summary }}</p>
          </div>

          <div class="dossier__rank" :data-rank="log.req">
            <b>{{ LEVELS[log.req - 1]?.label?.slice(0, 2) }}</b>
            <span>查阅密级</span>
            <em class="dossier__rank-seal" />
          </div>
        </header>

        <!-- 密级条 + 元信息 -->
        <div class="dossier__levelbar">
          <AppIcon name="lock" :size="12" />
          档案密级
          <b class="dossier__level" :data-level="LEVELS[log.req - 1]?.label">
            {{ LEVELS[log.req - 1]?.label }}
          </b>
          <span class="dossier__level-note">{{ log.archive || '中央档案馆 · 卷四 任务执行记录' }}</span>
        </div>

        <dl class="meta">
          <div>
            <dt>行动代号</dt>
            <dd>{{ log.op }}</dd>
          </div>
          <div>
            <dt>时间</dt>
            <dd>{{ log.date }}</dd>
          </div>
          <div>
            <dt>地点</dt>
            <dd>{{ log.site }}</dd>
          </div>
        </dl>

        <!-- 任务简报 -->
        <section class="dossier__sec">
          <h2 class="dossier__sec-t"><i />任务简报</h2>
          <ul class="brief">
            <li v-for="(b, i) in log.brief" :key="i">{{ b }}</li>
          </ul>
        </section>

        <!-- 原作背景 -->
        <section v-if="log.canon" class="dossier__sec">
          <h2 class="dossier__sec-t"><i />原作背景</h2>
          <p class="canon">{{ log.canon }}</p>
        </section>

        <!-- 执行过程 -->
        <section class="dossier__sec">
          <h2 class="dossier__sec-t"><i />执行过程</h2>
          <ol class="steps">
            <li v-for="(s, i) in log.steps" :key="i" class="step">
              <i class="step__dot" />
              <div class="step__box">
                <p class="step__t">{{ s.t }}</p>
                <p class="step__d">{{ s.d }}</p>
              </div>
            </li>
          </ol>
        </section>

        <!-- 结果与代价 -->
        <section class="dossier__sec">
          <h2 class="dossier__sec-t"><i />结果 · 代价</h2>
          <div class="outcome">
            <div class="outcome__row">
              <span class="outcome__k">完成结果</span>
              <p class="outcome__v">{{ log.outcome.result }}</p>
            </div>
            <div class="outcome__row">
              <span class="outcome__k">代价</span>
              <p class="outcome__v">{{ log.outcome.cost }}</p>
            </div>
            <div v-if="log.outcome.note" class="outcome__row outcome__row--note">
              <span class="outcome__k">批注</span>
              <p class="outcome__v">{{ log.outcome.note }}</p>
            </div>
          </div>
        </section>

        <!-- 涉及人物 -->
        <section v-if="log.figures.length" class="dossier__sec">
          <h2 class="dossier__sec-t"><i />涉及人物</h2>
          <div class="figs">
            <component
              :is="f.link ? 'button' : 'span'"
              v-for="f in log.figures"
              :key="f.key"
              class="fig"
              :class="{ 'is-link': f.link }"
              :type="f.link ? 'button' : undefined"
              @click="f.link && go(`/codex/${f.key}`)"
            >
              <b>{{ f.name }}</b>
              <AppIcon v-if="f.link" name="arrow" :size="12" class="fig__ic" />
            </component>
          </div>
        </section>

        <!-- 涉及武器 / 言灵 -->
        <section v-if="relics.length" class="dossier__sec">
          <h2 class="dossier__sec-t"><i />涉及武器 · 言灵</h2>
          <div class="figs">
            <component
              :is="r.kind === 'spirit' ? 'button' : 'span'"
              v-for="r in relics"
              :key="r.name"
              class="fig fig--relic"
              :class="{ 'is-link': r.kind === 'spirit' }"
              :type="r.kind === 'spirit' ? 'button' : undefined"
              @click="r.kind === 'spirit' && go(`/spirit/${r.key}`)"
            >
              <b>{{ r.name }}</b>
              <em v-if="r.code" class="fig__code">{{ r.code }}</em>
              <em v-else-if="r.note" class="fig__code">{{ r.note }}</em>
              <AppIcon v-if="r.kind === 'spirit'" name="arrow" :size="12" class="fig__ic" />
            </component>
          </div>
        </section>

        <blockquote v-if="log.quote" class="dossier__quote">
          <AppIcon name="sparkle" :size="16" />
          {{ log.quote }}
        </blockquote>

        <!-- 同一行动的其他记录 -->
        <section v-if="sameOp.length" class="dossier__sec">
          <h2 class="dossier__sec-t"><i />同一行动的其他记录</h2>
          <ul class="sames">
            <li v-for="m in sameOp" :key="m.person">
              <button type="button" class="same" @click="goLog(m)">
                <span class="same__who">{{ m.person }}</span>
                <span class="same__dept">{{ m.deptLabel }}</span>
                <span class="same__name">{{ m.name }}</span>
                <AppIcon name="arrow" :size="13" class="same__ic" />
              </button>
            </li>
          </ul>
        </section>

        <footer class="dossier__foot">
          摘自《任务执行记录 · 卷四》 · 卡塞尔学院档案馆藏 · 本页调阅已计入日志
        </footer>
      </article>

      <!-- ===== 密级不足 ===== -->
      <div v-else-if="log && !readable" class="dossier--void">
        <AppIcon name="lock" :size="40" :stroke="1.2" />
        <p>该记录的正文已封存</p>
        <p class="dossier__void-sub">
          需「{{ needLabel }}」密级 · 还差 {{ pointsTo(Math.max(log.req || 1, log.level || 1)) }} 权限点
        </p>
        <div class="dossier__void-acts">
          <button type="button" class="dossier__void-btn" @click="go('/missions')">去完成任务</button>
          <button type="button" class="dossier__void-btn dossier__void-btn--ghost" @click="goBack">
            返回档案室
          </button>
        </div>
      </div>

      <!-- ===== 记录不存在 ===== -->
      <div v-else class="dossier--void">
        <AppIcon name="archive" :size="40" :stroke="1.2" />
        <p>档案馆中没有该任务记录</p>
        <button type="button" class="dossier__void-btn" @click="goBack">返回档案室</button>
      </div>
    </main>
  </div>
</template>

<style scoped>
.logd {
  position: relative;
  z-index: 1;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* ================= 顶部导航 ================= */
.logd__back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex: 0 0 auto;
  white-space: nowrap;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.32);
  background: rgba(216, 178, 90, 0.06);
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: border-color 0.25s, background 0.25s, transform 0.25s, box-shadow 0.25s;
}

.logd__back:hover {
  border-color: var(--gold);
  background: rgba(216, 178, 90, 0.14);
  transform: translateX(-3px);
  box-shadow: 0 10px 26px -14px var(--gold-glow);
}

.logd__back-ic {
  transform: rotate(180deg);
}

@media (max-width: 940px) {
  .logd__back {
    padding: 8px 10px;
  }

  .logd__back-text {
    display: none;
  }
}

/* ================= 主体 ================= */
.logd__body {
  flex: 1;
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
  padding: 26px 24px 40px;
}

.dossier {
  position: relative;
  padding: 34px 38px 30px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: linear-gradient(165deg, rgba(32, 27, 46, 0.88), rgba(13, 11, 22, 0.95));
  box-shadow: var(--shadow-card), 0 0 30px -18px rgba(216, 178, 90, 0.3);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  animation: card-in 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
  overflow: hidden;
}

.dossier__beam {
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold-lt), var(--gold-antique), transparent);
}

/* ================= 档案头 ================= */
.dossier__head {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-bottom: 22px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.28);
}

/* 左侧编号竖牌 */
.dossier__code {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 12px;
  border-radius: 12px;
  border: 1px solid rgba(216, 178, 90, 0.4);
  background: linear-gradient(170deg, rgba(216, 178, 90, 0.16), rgba(8, 10, 18, 0.7));
}

.dossier__code-chip {
  writing-mode: vertical-rl;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.22em;
  color: var(--gold-lt);
  text-shadow: 0 0 14px var(--gold-glow);
}

.dossier__code-line {
  width: 1px;
  height: 26px;
  background: linear-gradient(180deg, var(--gold-dp), transparent);
}

.dossier__info {
  flex: 1;
  min-width: 0;
}

.dossier__cat {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--text-4);
  margin-bottom: 8px;
}

.dossier__cat-chip {
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(216, 178, 90, 0.07);
  color: var(--gold-dp);
}

.dossier__op {
  color: var(--text-3);
}

.dossier__name {
  font-family: var(--font-serif);
  font-size: 27px;
  font-weight: 600;
  letter-spacing: 0.1em;
  line-height: 1.3;
  color: var(--text);
  text-shadow: 0 0 26px rgba(216, 178, 90, 0.28);
}

.dossier__role {
  margin-top: 8px;
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: var(--gold-lt);
}

.dossier__desc {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-3);
}

/* 密级徽章 */
.dossier__rank {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 14px 18px 11px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(122, 90, 30, 0.9);
  background: linear-gradient(
    170deg,
    var(--gold-metal-hi) 0%,
    var(--gold-metal-md) 40%,
    var(--gold-dp) 68%,
    #8a6a28 100%
  );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.6),
    0 12px 32px -14px var(--gold-glow);
}

.dossier__rank b {
  font-family: var(--font-serif);
  font-size: 26px;
  line-height: 1;
  font-weight: 800;
  color: #241a05;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.45);
}

.dossier__rank span {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.24em;
  color: rgba(36, 26, 5, 0.75);
}

.dossier__rank-seal {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 45%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  animation: metal-sheen-d 3.6s ease-in-out infinite;
}

@keyframes metal-sheen-d {
  0% {
    transform: translateX(-140%) skewX(-18deg);
  }

  45%,
  100% {
    transform: translateX(420%) skewX(-18deg);
  }
}

/* ================= 密级条 ================= */
.dossier__levelbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 9px 14px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.45);
  font-size: 11.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.dossier__level {
  font-family: var(--font-mono);
  font-size: 11.5px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
}

.dossier__level[data-level='绝密'] {
  color: var(--err);
}

.dossier__level-note {
  margin-left: auto;
  font-size: 10.5px;
  color: var(--text-4);
}

/* ================= 元信息 ================= */
.meta {
  margin: 14px 0 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}

.meta > div {
  padding: 11px 14px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.4);
}

.meta dt {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  margin-bottom: 5px;
}

.meta dd {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-2);
}

/* ================= 分节 ================= */
.dossier__sec {
  margin-top: 26px;
}

.dossier__sec-t {
  display: flex;
  align-items: center;
  gap: 9px;
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 0.16em;
  color: var(--gold-lt);
  margin-bottom: 12px;
}

.dossier__sec-t i {
  width: 3px;
  height: 13px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold-dp));
}

/* 任务简报 */
.brief {
  margin: 0;
  padding: 14px 18px 14px 32px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  border-left: 2px solid rgba(216, 178, 90, 0.5);
  background: linear-gradient(120deg, rgba(216, 178, 90, 0.05), rgba(255, 255, 255, 0.015));
  list-style: disc;
}

.brief li {
  font-size: 12.5px;
  line-height: 1.9;
  color: var(--text-3);
}

.brief li + li {
  margin-top: 5px;
}

.brief li::marker {
  color: var(--gold-dp);
}

/* 原作背景 */
.canon {
  font-size: 13px;
  line-height: 2;
  color: var(--text-2);
  text-align: justify;
  padding: 16px 20px;
  border-radius: var(--r-md);
  border: 1px solid rgba(138, 106, 40, 0.26);
  background: linear-gradient(120deg, rgba(179, 39, 45, 0.06), rgba(216, 178, 90, 0.04));
}

/* 执行过程 */
.steps {
  position: relative;
  margin: 0;
  padding: 0 0 0 24px;
  list-style: none;
  display: grid;
  gap: 10px;
}

.steps::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 10px;
  bottom: 10px;
  width: 1px;
  background: linear-gradient(
    180deg,
    transparent,
    rgba(216, 178, 90, 0.45) 10%,
    rgba(216, 178, 90, 0.45) 90%,
    transparent
  );
}

.step {
  position: relative;
}

.step__dot {
  position: absolute;
  left: -24px;
  top: 15px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 1px solid var(--gold);
  background: linear-gradient(180deg, var(--gold-metal-hi), var(--gold-dp));
  box-shadow:
    0 0 0 3px rgba(13, 11, 22, 0.9),
    0 0 14px -2px var(--gold-glow);
}

.step__box {
  padding: 11px 15px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(140deg, rgba(26, 21, 38, 0.7), rgba(11, 9, 18, 0.86));
  transition: border-color 0.25s, transform 0.25s;
}

.step__box:hover {
  border-color: rgba(216, 178, 90, 0.42);
  transform: translateX(3px);
}

.step__t {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--gold-dp);
}

.step__d {
  margin-top: 5px;
  font-size: 12.5px;
  line-height: 1.85;
  color: var(--text-3);
}

/* 结果与代价 */
.outcome {
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  overflow: hidden;
}

.outcome__row {
  display: flex;
  gap: 14px;
  padding: 12px 16px;
  background: rgba(4, 7, 13, 0.42);
}

.outcome__row + .outcome__row {
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
}

.outcome__row--note {
  background: linear-gradient(120deg, rgba(216, 178, 90, 0.07), rgba(4, 7, 13, 0.42));
}

.outcome__k {
  flex: 0 0 64px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--gold-dp);
  padding-top: 2px;
}

.outcome__row--note .outcome__k {
  color: var(--gold-lt);
}

.outcome__v {
  flex: 1;
  font-size: 12.5px;
  line-height: 1.85;
  color: var(--text-2);
}

/* 人物 / 器物标签 */
.figs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.fig {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 13px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(14, 20, 34, 0.55);
  color: var(--text-2);
  font-size: 12.5px;
  letter-spacing: 0.06em;
}

.fig b {
  font-family: var(--font-serif);
  font-weight: 600;
}

.fig.is-link {
  cursor: pointer;
  border-color: rgba(216, 178, 90, 0.36);
  color: var(--gold-lt);
  transition: background 0.22s, border-color 0.22s, transform 0.2s;
}

.fig.is-link:hover {
  background: rgba(216, 178, 90, 0.16);
  border-color: var(--gold);
  transform: translateY(-1px);
}

.fig__ic {
  opacity: 0.8;
}

.fig__code {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

.fig--relic {
  border-radius: var(--r-sm);
}

/* 其他记录 */
.sames {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

.same {
  width: 100%;
  display: grid;
  grid-template-columns: auto auto 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line-soft);
  background: linear-gradient(120deg, rgba(20, 28, 46, 0.55), rgba(8, 12, 22, 0.7));
  cursor: pointer;
  text-align: left;
  transition: border-color 0.24s, transform 0.24s;
}

.same:hover {
  border-color: rgba(216, 178, 90, 0.4);
  transform: translateY(-2px);
}

.same__who {
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--text);
}

.same__dept {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--line);
}

.same__name {
  font-size: 11.5px;
  color: var(--text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.same__ic {
  color: var(--gold-lt);
}

/* 正文文字链接 */
.logd__link {
  padding: 0;
  border: 0;
  background: none;
  color: inherit;
  font: inherit;
  cursor: pointer;
}

.logd__link--strong {
  color: var(--gold-lt);
  text-decoration: underline;
  text-underline-offset: 3px;
  text-decoration-color: rgba(216, 178, 90, 0.5);
}

.logd__link--strong:hover {
  text-decoration-color: var(--gold);
}

/* 语录 / 页脚 */
.dossier__quote {
  margin: 26px 0 0;
  padding: 16px 20px;
  border-radius: var(--r-md);
  border: 1px solid rgba(138, 106, 40, 0.4);
  border-left: 3px solid var(--gold-dp);
  background: linear-gradient(120deg, rgba(216, 178, 90, 0.07), rgba(179, 39, 45, 0.05));
  font-family: var(--font-serif);
  font-size: 15px;
  line-height: 1.9;
  letter-spacing: 0.08em;
  color: var(--gold-lt);
  text-shadow: 0 0 18px rgba(216, 178, 90, 0.3);
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.dossier__quote :deep(svg) {
  flex: 0 0 auto;
  margin-top: 5px;
  opacity: 0.7;
}

.dossier__foot {
  margin-top: 26px;
  padding-top: 14px;
  border-top: 1px dashed rgba(138, 106, 40, 0.28);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--text-4);
  text-align: center;
}

/* ================= 空态 ================= */
.dossier--void {
  display: grid;
  place-items: center;
  gap: 12px;
  padding: 90px 20px;
  border-radius: var(--r-lg);
  border: 1px dashed var(--line);
  color: var(--text-4);
  text-align: center;
  font-size: 13.5px;
  letter-spacing: 0.08em;
}

.dossier__void-sub {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-4);
}

.dossier__void-acts {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.dossier__void-btn {
  padding: 9px 20px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.4);
  background: rgba(216, 178, 90, 0.1);
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: background 0.24s, border-color 0.24s;
}

.dossier__void-btn:hover {
  background: rgba(216, 178, 90, 0.2);
  border-color: var(--gold);
}

.dossier__void-btn--ghost {
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-3);
}

/* ================= 响应式 ================= */
@media (max-width: 720px) {
  .dossier {
    padding: 24px 18px 22px;
  }

  .dossier__head {
    flex-wrap: wrap;
    gap: 14px;
  }

  .dossier__code {
    flex-direction: row;
    padding: 8px 12px;
  }

  .dossier__code-chip {
    writing-mode: horizontal-tb;
  }

  .dossier__code-line {
    width: 24px;
    height: 1px;
    background: linear-gradient(90deg, var(--gold-dp), transparent);
  }

  .dossier__name {
    font-size: 21px;
  }

  .dossier__rank {
    flex: 0 0 auto;
    flex-direction: row;
    gap: 8px;
    padding: 8px 16px;
  }

  .dossier__rank b {
    font-size: 18px;
  }

  .dossier__level-note {
    display: none;
  }

  .outcome__row {
    flex-direction: column;
    gap: 4px;
  }

  .same {
    grid-template-columns: 1fr auto;
  }

  .same__dept,
  .same__name {
    display: none;
  }
}
</style>
