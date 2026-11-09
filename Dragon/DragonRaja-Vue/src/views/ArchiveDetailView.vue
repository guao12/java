<script setup>
/**
 * 卷宗详情页 —— /archive/:kind/:key
 *
 * 档案室四卷的统一详情页：
 *   /archive/king/DK-002      龙王卷宗（资料 + 情报条目）
 *   /archive/person/luomingfei 部门人物（性格 / 武器 / 言灵 / 任务 / 荣誉 / 身世）
 *   /archive/arm/ARM-03        龙族武器（器物档案）
 * 卷四任务执行记录仍走 /log/:code/:person（内容更细，见 TaskLogView）。
 *
 * 数据全部来自 data/catalog.js → data/archive.js，与档案室卡片一一对应；
 * 任务系统已下线，正文一律完整展示，密级只作分级标注。
 */
import { computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import { findDossier, siblingDossiers } from '@/data/catalog'
import { ARCHIVE_MISSION_LOGS } from '@/data/archive'

const route = useRoute()
const router = useRouter()

const kind = computed(() => String(route.params.kind || ''))
const key = computed(() => decodeURIComponent(String(route.params.key || '')))

/** 目录条目（用于标题 / 密级 / 路由等公共信息）与原始档案（用于正文） */
const found = computed(() => findDossier(kind.value, key.value))
const item = computed(() => found.value.item)
const raw = computed(() => found.value.raw)

const KIND_LABEL = { king: '龙王卷宗', person: '部门人物', arm: '龙族武器' }

/** 档案头徽章上的文字（龙王=威胁等级 / 人物=血统 / 武器=器物评级） */
const badge = computed(() => {
  if (!item.value) return { text: '', label: '' }
  if (kind.value === 'king') return { text: raw.value?.threat || '—', label: '威胁等级' }
  if (kind.value === 'person') return { text: raw.value?.blood || '—', label: '血统评级' }
  return { text: raw.value?.rank || '—', label: '器物评级' }
})

/** 元信息表：按卷不同字段 */
const metaRows = computed(() => {
  const r = raw.value
  if (!r) return []
  if (kind.value === 'king') {
    return [
      { k: '王座', v: r.seats },
      { k: '状态', v: r.status },
      { k: '调阅', v: r.notes },
      { k: '别名', v: r.alias }
    ].filter((x) => x.v)
  }
  if (kind.value === 'person') {
    return [
      { k: '部门', v: r.deptLabel },
      { k: '血统', v: `${r.blood || '—'} 级 · ${r.bloodPct ?? '—'}%` },
      { k: '身份', v: r.role },
      { k: '别名', v: r.alias }
    ].filter((x) => x.v)
  }
  return [
    { k: '类型', v: r.type },
    { k: '产地', v: r.origin },
    { k: '状态', v: r.status }
  ].filter((x) => x.v)
})

/** 人物卷：任务条目 → 任务记录详情页（命中才给跳转） */
const personMissions = computed(() => {
  const p = raw.value
  if (kind.value !== 'person' || !p) return []
  return (p.missions || []).map((m) => {
    const hit = ARCHIVE_MISSION_LOGS.find((l) => l.code === m.code && l.person === p.name)
    return {
      ...m,
      path: hit ? `/log/${hit.code}/${encodeURIComponent(hit.person)}` : ''
    }
  })
})

/** 同卷其他卷宗 */
const siblings = computed(() => siblingDossiers(item.value, 6))

const backTab = computed(() => {
  const map = { king: 'kings', person: 'people', arm: 'arms', log: 'logs' }
  return `#tab-${map[kind.value] || 'kings'}`
})

function go(path) {
  if (!path) return
  router.push(path)
}

function goBack() {
  router.push({ path: '/archive', hash: backTab.value })
}

onMounted(() => {
  document.documentElement.style.colorScheme = 'dark'
})

// 同路由不同 key 时组件实例复用，必须监听参数变化（标题 + 回到顶部）
watch(
  () => [kind.value, key.value],
  () => {
    document.title = item.value
      ? `${KIND_LABEL[kind.value] || '卷宗'} · ${item.value.title} | 卡塞尔学院`
      : '卷宗详情 | 卡塞尔学院'
    window.scrollTo({ top: 0, behavior: 'auto' })
  },
  { immediate: true }
)
</script>

<template>
  <div class="ad">
    <RuneBackground />

    <SiteNav>
      <template #actions>
        <button type="button" class="ad__back" @click="goBack">
          <AppIcon name="arrow" :size="14" class="ad__back-ic" />
          <span class="ad__back-text">返回档案室</span>
        </button>
      </template>
    </SiteNav>

    <main class="ad__body">
      <!-- ============ 完整卷宗（档案已全量开放） ============ -->
      <article v-if="item" class="dossier">
        <i class="dossier__beam" />

        <header class="dossier__head">
          <div class="dossier__code">
            <span class="dossier__code-chip">{{ item.code }}</span>
            <i class="dossier__code-line" />
          </div>

          <div class="dossier__info">
            <p class="dossier__cat">
              <span class="dossier__cat-chip">{{ KIND_LABEL[kind] || item.topName }}</span>
              <span class="dossier__sub">{{ item.subName }}</span>
            </p>
            <h1 class="dossier__name">{{ item.title }}</h1>
            <p v-if="item.subtitle" class="dossier__role">{{ item.subtitle }}</p>
            <p class="dossier__desc">{{ item.summary }}</p>
          </div>

          <div class="dossier__rank">
            <b>{{ badge.text }}</b>
            <span>{{ badge.label }}</span>
            <em class="dossier__rank-seal" />
          </div>
        </header>

        <!-- 密级条 -->
        <div class="dossier__levelbar">
          <AppIcon name="lock" :size="12" />
          档案密级
          <b class="dossier__level">{{ item.levelLabel }}</b>
          <span class="dossier__level-note">中央档案馆 · {{ item.topName }}</span>
        </div>

        <!-- 元信息 -->
        <dl class="meta">
          <div v-for="row in metaRows" :key="row.k">
            <dt>{{ row.k }}</dt>
            <dd>{{ row.v }}</dd>
          </div>
        </dl>

        <!-- ================= 龙王卷宗 ================= -->
        <template v-if="kind === 'king'">
          <section class="sec">
            <h2 class="sec__t"><i />资料</h2>
            <p class="canon">{{ raw.desc }}</p>
          </section>

          <section v-if="(raw.intel || []).length" class="sec">
            <h2 class="sec__t"><i />情报条目 · {{ raw.intel.length }} 条</h2>
            <div class="intel-list">
              <div v-for="(it, i) in raw.intel" :key="i" class="intel">
                <span class="intel__no">{{ String(i + 1).padStart(2, '0') }}</span>
                <div class="intel__body">
                  <b>{{ it.title }}</b>
                  <p>{{ it.text }}</p>
                </div>
              </div>
            </div>
          </section>
        </template>

        <!-- ================= 部门人物 ================= -->
        <template v-else-if="kind === 'person'">
          <section class="sec">
            <h2 class="sec__t"><i />基础档案</h2>
            <p class="canon">{{ raw.desc }}</p>
            <p v-if="raw.quote" class="quote">{{ raw.quote }}</p>
            <p v-if="raw.bloodNote" class="note">{{ raw.bloodNote }}</p>
          </section>

          <section class="sec">
            <h2 class="sec__t"><i />血统</h2>
            <div class="blood">
              <div class="blood__bar">
                <i :style="{ width: `${raw.bloodPct || 0}%` }" />
              </div>
              <p class="blood__meta">
                <b>{{ raw.blood }} 级</b> · 龙血占比 {{ raw.bloodPct || 0 }}%
                <em>{{ raw.alias }}</em>
              </p>
            </div>
          </section>

          <section v-if="(raw.traits || []).length" class="sec">
            <h2 class="sec__t"><i />性格特点</h2>
            <ul class="bullets">
              <li v-for="(t, i) in raw.traits" :key="i">{{ t }}</li>
            </ul>
          </section>

          <section class="sec">
            <h2 class="sec__t"><i />武器</h2>
            <div v-if="raw.weapon" class="card">
              <div class="card__row">
                <b>{{ raw.weapon.name }}</b>
                <span class="chip">{{ raw.weapon.type }}</span>
              </div>
              <p>{{ raw.weapon.desc }}</p>
            </div>
            <p v-else class="muted">无登记制式武器。</p>
          </section>

          <section class="sec">
            <h2 class="sec__t"><i />言灵</h2>
            <div
              v-for="s in raw.spirits || []"
              :key="s.name"
              class="card card--flat"
            >
              <div class="card__row">
                <b>{{ s.name }}</b>
                <span class="chip chip--gold">{{ s.level }}</span>
              </div>
              <p>{{ s.desc }}</p>
            </div>
          </section>

          <section class="sec">
            <h2 class="sec__t"><i />任务执行过程与结果</h2>
            <div
              v-for="m in personMissions"
              :key="m.code"
              class="card card--mission"
              :class="{ 'is-link': !!m.path }"
              @click="go(m.path)"
            >
              <div class="card__row">
                <span class="chip">{{ m.code }}</span>
                <b>{{ m.name }}</b>
                <span v-if="m.path" class="card__more">
                  详情 <AppIcon name="arrow" :size="11" />
                </span>
              </div>
              <p class="kv"><i>过程</i>{{ m.process }}</p>
              <p class="kv kv--r"><i>结果</i>{{ m.result }}</p>
            </div>
            <p v-if="!personMissions.length" class="muted">暂无可公开的任务记录。</p>
          </section>

          <section v-if="(raw.honors || []).length" class="sec">
            <h2 class="sec__t"><i />获得荣誉</h2>
            <ul class="bullets bullets--honor">
              <li v-for="(h, i) in raw.honors" :key="i">{{ h }}</li>
            </ul>
          </section>

          <section class="sec sec--split">
            <div>
              <h2 class="sec__t"><i />身世</h2>
              <p class="canon">{{ raw.origin }}</p>
            </div>
            <div>
              <h2 class="sec__t"><i />家庭背景</h2>
              <p class="canon">{{ raw.family }}</p>
            </div>
          </section>
        </template>

        <!-- ================= 龙族武器 ================= -->
        <template v-else-if="kind === 'arm'">
          <section class="sec">
            <h2 class="sec__t"><i />器物档案</h2>
            <p class="canon">{{ raw.desc }}</p>
          </section>

          <section class="sec">
            <h2 class="sec__t"><i />特性加成</h2>
            <p class="perk">{{ raw.perk }}</p>
          </section>
        </template>

        <!-- 标签 -->
        <section v-if="item.tags.length" class="sec">
          <h2 class="sec__t"><i />关联标签</h2>
          <div class="tags">
            <span v-for="t in item.tags" :key="t" class="tag">{{ t }}</span>
          </div>
        </section>

        <!-- 人物志跳转 -->
        <button
          v-if="kind === 'person'"
          type="button"
          class="cross"
          @click="go(`/codex/${item.key}`)"
        >
          <AppIcon name="id" :size="14" />
          查看「人物志」版本 · 生平时间线与人物关系
          <AppIcon name="arrow" :size="13" />
        </button>

        <!-- 同卷其他卷宗 -->
        <section v-if="siblings.length" class="sec">
          <h2 class="sec__t"><i />同卷其他卷宗</h2>
          <div class="sibs">
            <button
              v-for="s in siblings"
              :key="s.uid"
              type="button"
              class="sib"
              @click="go(s.path)"
            >
              <span class="sib__code">{{ s.code }}</span>
              <span class="sib__name">{{ s.title }}</span>
              <span class="sib__lv">{{ s.levelLabel }}</span>
              <AppIcon name="arrow" :size="12" class="sib__ic" />
            </button>
          </div>
        </section>

        <footer class="dossier__foot">
          摘自《{{ item.topName }}》· 卡塞尔学院档案馆藏 · 本页调阅已计入日志
        </footer>
      </article>

      <!-- ============ 卷宗不存在 ============ -->
      <div v-else class="void">
        <AppIcon name="archive" :size="40" :stroke="1.2" />
        <p>档案馆中没有这份卷宗</p>
        <button type="button" class="void__btn" @click="goBack">返回档案室</button>
      </div>
    </main>
  </div>
</template>

<style scoped>
.ad {
  position: relative;
  z-index: 1;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* ================= 顶部导航 ================= */
.ad__back {
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

.ad__back:hover {
  border-color: var(--gold);
  background: rgba(216, 178, 90, 0.14);
  transform: translateX(-3px);
  box-shadow: 0 10px 26px -14px var(--gold-glow);
}

.ad__back-ic {
  transform: rotate(180deg);
}

@media (max-width: 940px) {
  .ad__back {
    padding: 8px 10px;
  }

  .ad__back-text {
    display: none;
  }
}

/* ================= 主体 ================= */
.ad__body {
  flex: 1;
  width: 100%;
  max-width: 960px;
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

.dossier__sub {
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

/* 评级徽章 */
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
  font-size: 22px;
  line-height: 1;
  font-weight: 800;
  color: #241a05;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.45);
  white-space: nowrap;
}

.dossier__rank span {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.24em;
  color: rgba(36, 26, 5, 0.75);
  white-space: nowrap;
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

.dossier__level-note {
  margin-left: auto;
  font-size: 10.5px;
  color: var(--text-4);
}

/* ================= 元信息 ================= */
.meta {
  margin: 14px 0 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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
.sec {
  margin-top: 26px;
}

.sec--split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.sec__t {
  display: flex;
  align-items: center;
  gap: 9px;
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 0.16em;
  color: var(--gold-lt);
  margin-bottom: 12px;
}

.sec__t i {
  width: 3px;
  height: 13px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold-dp));
}

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

.quote {
  margin-top: 12px;
  padding: 12px 16px;
  border-left: 3px solid var(--gold-dp);
  border-radius: var(--r-sm);
  background: rgba(216, 178, 90, 0.06);
  font-family: var(--font-serif);
  font-size: 13.5px;
  line-height: 1.9;
  letter-spacing: 0.06em;
  color: var(--gold-lt);
}

.note {
  margin-top: 10px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--text-4);
}

/* 情报条目 */
.intel-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.intel {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(140deg, rgba(26, 21, 38, 0.7), rgba(11, 9, 18, 0.86));
  transition: border-color 0.25s, transform 0.25s;
}

.intel:hover {
  border-color: rgba(216, 178, 90, 0.42);
  transform: translateX(3px);
}

.intel__no {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--gold-dp);
  padding-top: 3px;
}

.intel__body b {
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: var(--text);
}

.intel__body p {
  margin-top: 5px;
  font-size: 12.5px;
  line-height: 1.85;
  color: var(--text-3);
}

/* 血统条 */
.blood__bar {
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.blood__bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--gold-antique), var(--gold-lt));
  box-shadow: 0 0 12px rgba(216, 178, 90, 0.6);
  transition: width 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.blood__meta {
  margin-top: 9px;
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--text-3);
}

.blood__meta b {
  color: var(--gold-lt);
  font-family: var(--font-serif);
  font-size: 13px;
}

.blood__meta em {
  margin-left: 8px;
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

/* 列表 */
.bullets {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding-left: 18px;
  list-style: disc;
}

.bullets li {
  font-size: 12.5px;
  line-height: 1.85;
  color: var(--text-3);
}

.bullets li::marker {
  color: var(--gold-dp);
}

.bullets--honor li::marker {
  color: var(--gold-lt);
}

/* 卡片块（武器 / 言灵 / 任务） */
.card {
  padding: 13px 16px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.42);
}

.card + .card {
  margin-top: 9px;
}

.card--flat {
  background: linear-gradient(140deg, rgba(20, 28, 46, 0.55), rgba(8, 12, 22, 0.7));
}

.card.is-link {
  cursor: pointer;
  transition: border-color 0.24s, transform 0.24s, background 0.24s;
}

.card.is-link:hover {
  border-color: rgba(216, 178, 90, 0.45);
  background: linear-gradient(140deg, rgba(30, 26, 44, 0.8), rgba(12, 10, 20, 0.9));
  transform: translateY(-2px);
}

.card__row {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
}

.card__row b {
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--text);
}

.card__more {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--gold-lt);
}

.card p {
  margin-top: 6px;
  font-size: 12.5px;
  line-height: 1.85;
  color: var(--text-3);
}

.kv i {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  margin-right: 7px;
}

.kv--r i {
  color: var(--gold-dp);
}

.chip {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-3);
}

.chip--gold {
  color: var(--gold-dp);
  border-color: rgba(216, 178, 90, 0.4);
}

.perk {
  padding: 13px 16px;
  border-radius: var(--r-md);
  border: 1px solid rgba(216, 178, 90, 0.3);
  background: linear-gradient(120deg, rgba(216, 178, 90, 0.1), rgba(216, 178, 90, 0.02));
  font-size: 12.5px;
  line-height: 1.85;
  letter-spacing: 0.04em;
  color: var(--gold-lt);
}

.muted {
  font-size: 12px;
  color: var(--text-4);
}

/* 标签 */
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(216, 178, 90, 0.05);
  font-size: 11.5px;
  letter-spacing: 0.06em;
  color: var(--text-3);
}

/* 人物志跳转 */
.cross {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  margin-top: 22px;
  padding: 13px 18px;
  border-radius: var(--r-md);
  border: 1px dashed rgba(216, 178, 90, 0.4);
  background: rgba(216, 178, 90, 0.05);
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: background 0.24s, border-color 0.24s;
}

.cross:hover {
  background: rgba(216, 178, 90, 0.14);
  border-color: var(--gold);
}

.cross :deep(svg:last-child) {
  margin-left: auto;
}

/* 同卷互跳 */
.sibs {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 8px;
}

.sib {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 13px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line-soft);
  background: linear-gradient(120deg, rgba(20, 28, 46, 0.55), rgba(8, 12, 22, 0.7));
  color: var(--text-2);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.24s, transform 0.24s;
}

.sib:hover {
  border-color: rgba(216, 178, 90, 0.45);
  transform: translateY(-2px);
}

.sib__code {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--gold-dp);
}

.sib__name {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sib__lv {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.sib__ic {
  color: var(--gold-lt);
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
.void {
  display: grid;
  place-items: center;
  gap: 12px;
  padding: 72px 20px;
  border-radius: var(--r-lg);
  border: 1px dashed var(--line);
  background: rgba(255, 255, 255, 0.018);
  color: var(--text-2);
  text-align: center;
  font-size: 15px;
  letter-spacing: 0.08em;
}

.void__btn {
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

.void__btn:hover {
  background: rgba(216, 178, 90, 0.2);
  border-color: var(--gold);
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
    flex-direction: row;
    gap: 8px;
    padding: 8px 16px;
  }

  .dossier__rank b {
    font-size: 16px;
  }

  .dossier__level-note {
    display: none;
  }

  .sec--split {
    grid-template-columns: 1fr;
  }
}
</style>
