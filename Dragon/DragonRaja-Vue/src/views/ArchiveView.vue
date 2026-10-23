<script setup>
/**
 * 档案室（独立页面 /archive）
 * - 不在首页展示，入口在顶部导航栏「档案室」
 * - 内容按密级解锁：权限点由「执行部任务」页完成任务累积（utils/clearance.js）
 * - 四卷：龙王卷宗（资料+情报）/ 部门人物 / 龙族武器 / 任务执行记录
 */
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import {
  LEVELS,
  hasClearance,
  level,
  levelLabel,
  nextLevel,
  points,
  pointsTo
} from '@/utils/clearance'
import {
  ARCHIVE_ARMS,
  ARCHIVE_DEPTS,
  ARCHIVE_KINGS,
  ARCHIVE_MISSION_LOGS,
  ARCHIVE_PEOPLE
} from '@/data/archive'

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

/* ---------------- 权限状态 ---------------- */
const lv = computed(() => level.value)
const lvLabel = computed(() => levelLabel.value)
const next = computed(() => nextLevel.value)
const progress = computed(() => (lv.value >= LEVELS.length ? 100 : (next.value?.pct ?? 100)))

/** 解锁提示语：按目标密级实际门槛计算，还差多少权限点 */
function needText(req) {
  if (lv.value >= req) return ''
  const target = LEVELS.find((l) => l.value === req)?.label || '更高'
  return `需「${target}」密级 · 还差 ${pointsTo(req)} 权限点`
}

/** 轻提示：点击封存内容 / 停留在当前页时给出明确反馈（避免"点了没反应"） */
const toast = ref('')
let toastTimer = 0
function showToast(text) {
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => (toast.value = ''), 4000)
}

/** 点击封存内容：抖动被点区域 + 弹提示，让点击有可见反馈 */
function lockHint(e, req) {
  const el = e?.currentTarget
  if (el && el.classList) {
    el.classList.remove('is-shake')
    // 强制 reflow，让抖动动画可以重复触发
    void el.offsetWidth
    el.classList.add('is-shake')
  }
  const t = needText(req)
  showToast(
    t ? `该内容已封存 · ${t} · 去「执行部任务」完成任务即可解锁` : '该内容已封存'
  )
}
onBeforeUnmount(() => clearTimeout(toastTimer))

/** 点击卡片头部：已解锁则展开，未解锁则提示 */
function onKingHead(e, k) {
  if (hasClearance(k.req)) toggleKing(k.code)
  else lockHint(e, k.req)
}
function onPersonHead(e, p) {
  if (hasClearance(p.req)) togglePerson(p.key)
  else lockHint(e, p.req)
}

/* ---------------- 分卷切换 ---------------- */
const TABS = [
  { id: 'kings', label: '龙王卷宗', icon: 'flame' },
  { id: 'people', label: '部门人物', icon: 'user' },
  { id: 'arms', label: '龙族武器', icon: 'sparkle' },
  { id: 'logs', label: '任务执行记录', icon: 'archive' }
]
const tab = ref('kings')

/* ---------------- 部门人物 ---------------- */
const deptFilter = ref('')
const filteredPeople = computed(() =>
  deptFilter.value ? ARCHIVE_PEOPLE.filter((p) => p.dept === deptFilter.value) : ARCHIVE_PEOPLE
)
function countDept(id) {
  return ARCHIVE_PEOPLE.filter((p) => p.dept === id).length
}

/** 展开的人物档案（手风琴，一次一个） */
const openKey = ref('')
function togglePerson(key) {
  openKey.value = openKey.value === key ? '' : key
}

/** 已解锁人物的统计 */
const unlockedPeople = computed(() => ARCHIVE_PEOPLE.filter((p) => hasClearance(p.req)).length)

/* ---------------- 龙王卷宗 ---------------- */
const openKing = ref(ARCHIVE_KINGS[0]?.code || '')
function toggleKing(code) {
  openKing.value = openKing.value === code ? '' : code
}

/* ---------------- 任务执行记录 ---------------- */
const logs = computed(() => ARCHIVE_MISSION_LOGS.filter((m) => hasClearance(m.req)))
const lockedLogs = computed(() => ARCHIVE_MISSION_LOGS.filter((m) => !hasClearance(m.req)))
</script>

<template>
  <div class="arc">
    <RuneBackground />

    <!-- ============ 顶部导航：与首页同源 ============ -->
    <SiteNav />

    <!-- ============ 主体 ============ -->
    <main class="arc__body">
      <section class="arc__hero">
        <div class="arc__hero-row">
          <DragonEmblem class="arc__hero-mark" />
          <div class="arc__hero-text">
            <h1 class="arc__title">
              档案室<span class="arc__title-en">ARCHIVE · DRAGON DOSSIER</span>
            </h1>
            <p class="arc__sub">
              龙王资料与情报 · 各部门人物性格 / 武器 / 言灵 · 任务执行过程与结果 · 荣誉与身世
            </p>
          </div>
        </div>
      </section>

      <!-- ============ 权限面板 ============ -->
      <section class="perm">
        <div class="perm__head">
          <span class="perm__icon"><AppIcon name="shield" :size="16" /></span>
          <div class="perm__text">
            <b>当前权限 {{ points }} 点</b>
            <span>
              可查阅密级 <em>{{ lvLabel }}</em>
              <template v-if="next"> · 距「{{ LEVELS[lv]?.label }}」还需 {{ next.remain }} 点</template>
              <template v-else> · 已达最高密级</template>
            </span>
          </div>
          <button type="button" class="perm__go" @click="go('/missions')">
            <AppIcon name="arrow" :size="13" /> 去做任务提升权限
          </button>
        </div>
        <div class="perm__bar">
          <i :style="{ width: progress + '%' }" />
        </div>
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

      <!-- 密级不足时的显著提示 -->
      <div v-if="lv < 4" class="notice">
        <AppIcon name="lock" :size="15" />
        <span>
          档案室大部分内容已按密级<em>封存</em>：当前「{{ lvLabel }}」密级 ·
          完成「执行部任务」累积权限点即可解锁（3 点升「内部」· 8 点升「机密」· 15 点升「绝密」）
        </span>
        <button type="button" class="notice__go" @click="go('/missions')">
          <AppIcon name="arrow" :size="13" /> 去完成任务累积权限点
        </button>
      </div>

      <!-- ============ 分卷 ============ -->
      <div class="tabs">
        <button
          v-for="t in TABS"
          :key="t.id"
          type="button"
          class="tab"
          :class="{ 'is-on': tab === t.id }"
          @click="tab = t.id"
        >
          <AppIcon :name="t.icon" :size="14" /> {{ t.label }}
        </button>
      </div>

      <!-- ===== 卷一 · 龙王卷宗 ===== -->
      <section v-if="tab === 'kings'" class="shelf">
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="flame" :size="15" /> 龙王卷宗 · 资料与情报</h2>
          <span class="shelf__more">共 {{ ARCHIVE_KINGS.length }} 位 · 点击展开情报</span>
        </header>

        <div class="kgrid">
          <article
            v-for="k in ARCHIVE_KINGS"
            :key="k.code"
            class="kcard"
            :class="{ 'is-locked': !hasClearance(k.req) }"
          >
            <div class="kcard__top" @click="onKingHead($event, k)">
              <div class="kcard__id">
                <span class="kcard__code">{{ k.code }}</span>
                <span class="kcard__threat" :data-t="k.threat">{{ k.threat }}</span>
              </div>
              <div class="kcard__name">
                <h3>{{ k.name }}</h3>
                <p>{{ k.alias }} · {{ k.seat }}</p>
              </div>
              <span class="kcard__lv" :data-lv="k.req">{{ LEVELS[k.req - 1]?.label }}</span>
              <span class="kcard__caret" :class="{ 'is-open': openKing === k.code }">
                <AppIcon name="chevron" :size="14" />
              </span>
            </div>

            <!-- 基础资料：公开可见 -->
            <div class="kcard__body">
              <p class="kcard__desc">{{ k.desc }}</p>
              <div class="kcard__meta">
                <span><i>王座</i>{{ k.seats }}</span>
                <span><i>状态</i>{{ k.status }}</span>
                <span><i>调阅</i>{{ k.notes }}</span>
              </div>
              <div class="kcard__tags">
                <span v-for="t in k.tags" :key="t">{{ t }}</span>
              </div>

              <!-- 情报条目：按密级解锁 -->
              <template v-if="hasClearance(k.req)">
                <button type="button" class="kcard__toggle" @click="toggleKing(k.code)">
                  <AppIcon name="info" :size="13" />
                  情报条目 · {{ k.intel.length }} 条
                  <span class="kcard__caret" :class="{ 'is-open': openKing === k.code }">
                    <AppIcon name="chevron" :size="13" />
                  </span>
                </button>

                <div v-if="openKing === k.code" class="kcard__intel">
                  <div v-for="(it, i) in k.intel" :key="i" class="intel">
                    <span class="intel__no">{{ String(i + 1).padStart(2, '0') }}</span>
                    <div class="intel__body">
                      <b>{{ it.title }}</b>
                      <p>{{ it.text }}</p>
                    </div>
                  </div>
                </div>
              </template>

              <div v-else class="kcard__locked" @click="lockHint($event, k.req)">
                <AppIcon name="lock" :size="16" />
                <span>情报条目已封存 · {{ needText(k.req) }}</span>
                <button type="button" class="kcard__unlock" @click.stop="go('/missions')">
                  去完成任务
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <!-- ===== 卷二 · 部门人物 ===== -->
      <section v-else-if="tab === 'people'" class="shelf">
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="user" :size="15" /> 部门人物档案</h2>
          <span class="shelf__more">
            已解锁 {{ unlockedPeople }} / {{ ARCHIVE_PEOPLE.length }} 人 · 点击卡片展开完整档案
          </span>
        </header>

        <div class="chips pchips">
          <button
            type="button"
            class="chip chip--sm"
            :class="{ 'is-on': !deptFilter }"
            @click="deptFilter = ''"
          >
            全部 <i class="chip__n">{{ ARCHIVE_PEOPLE.length }}</i>
          </button>
          <button
            v-for="d in ARCHIVE_DEPTS"
            :key="d.id"
            type="button"
            class="chip chip--sm"
            :class="{ 'is-on': deptFilter === d.id }"
            @click="deptFilter = d.id"
          >
            <AppIcon :name="d.icon" :size="12" /> {{ d.label }}
            <i class="chip__n">{{ countDept(d.id) }}</i>
          </button>
        </div>

        <div class="pgrid">
          <article
            v-for="p in filteredPeople"
            :key="p.key"
            class="person"
            :data-tone="p.tone"
            :class="{ 'is-locked': !hasClearance(p.req), 'is-open': openKey === p.key }"
          >
            <div class="person__top" @click="onPersonHead($event, p)">
              <div class="person__avatar"><span>{{ p.name.slice(0, 1) }}</span></div>
              <div class="person__id">
                <h3>{{ p.name }}</h3>
                <p>{{ p.alias }}</p>
                <span class="person__dept">{{ p.deptLabel }}</span>
              </div>
              <div class="person__right">
                <span class="person__blood" :data-rank="p.blood">{{ p.blood }} 级 · {{ p.bloodPct }}%</span>
                <span class="person__lv" :data-lv="p.req">{{ LEVELS[p.req - 1]?.label }}</span>
                <span class="person__caret" :class="{ 'is-open': openKey === p.key }">
                  <AppIcon name="chevron" :size="14" />
                </span>
              </div>
            </div>

            <!-- 基础档案：公开可见 -->
            <div class="person__body">
              <div class="person__brief">
                <p>{{ p.desc }}</p>
                <p class="person__quote">{{ p.quote }}</p>
                <p class="person__note">{{ p.bloodNote }}</p>
              </div>

              <!-- 完整档案（性格/武器/言灵/任务/荣誉/身世）：按密级解锁 -->
              <template v-if="hasClearance(p.req)">
                <button type="button" class="kcard__toggle" @click="togglePerson(p.key)">
                  <AppIcon name="id" :size="13" />
                  完整档案 · 性格 / 武器 / 言灵 / 任务 / 荣誉 / 身世
                  <span class="kcard__caret" :class="{ 'is-open': openKey === p.key }">
                    <AppIcon name="chevron" :size="13" />
                  </span>
                </button>

                <div v-if="openKey === p.key" class="dossier">
                <!-- 性格特点 -->
                <div class="block">
                  <h4><AppIcon name="user" :size="13" /> 性格特点</h4>
                  <ul>
                    <li v-for="(t, i) in p.traits" :key="i">{{ t }}</li>
                  </ul>
                </div>

                <!-- 武器 -->
                <div class="block">
                  <h4><AppIcon name="sparkle" :size="13" /> 武器</h4>
                  <div v-if="p.weapon" class="weapon">
                    <div class="weapon__row">
                      <b>{{ p.weapon.name }}</b>
                      <span class="weapon__type">{{ p.weapon.type }}</span>
                    </div>
                    <p>{{ p.weapon.desc }}</p>
                  </div>
                  <p v-else class="muted">无登记制式武器。</p>
                </div>

                <!-- 言灵 -->
                <div class="block">
                  <h4><AppIcon name="flame" :size="13" /> 言灵</h4>
                  <div v-for="s in p.spirits" :key="s.name" class="spirit">
                    <div class="spirit__row">
                      <b>{{ s.name }}</b>
                      <span class="spirit__lv">{{ s.level }}</span>
                    </div>
                    <p>{{ s.desc }}</p>
                  </div>
                </div>

                <!-- 任务执行过程与结果 -->
                <div class="block">
                  <h4><AppIcon name="archive" :size="13" /> 任务执行过程与结果</h4>
                  <div v-for="m in p.missions" :key="m.code" class="mission">
                    <div class="mission__row">
                      <span class="mission__code">{{ m.code }}</span>
                      <b>{{ m.name }}</b>
                    </div>
                    <p class="mission__p"><i>过程</i>{{ m.process }}</p>
                    <p class="mission__r"><i>结果</i>{{ m.result }}</p>
                  </div>
                  <p v-if="!p.missions.length" class="muted">暂无可公开的任务记录。</p>
                </div>

                <!-- 荣誉 -->
                <div class="block">
                  <h4><AppIcon name="ticket" :size="13" /> 获得荣誉</h4>
                  <ul class="honors">
                    <li v-for="(h, i) in p.honors" :key="i">{{ h }}</li>
                  </ul>
                </div>

                <!-- 身世 / 家庭背景 -->
                <div class="block block--split">
                  <div>
                    <h4><AppIcon name="info" :size="13" /> 身世</h4>
                    <p>{{ p.origin }}</p>
                  </div>
                  <div>
                    <h4><AppIcon name="home" :size="13" /> 家庭背景</h4>
                    <p>{{ p.family }}</p>
                  </div>
                </div>

                <p class="dossier__note">{{ p.bloodNote }}</p>
              </div>

                </template>

                <div v-else class="kcard__locked" @click="lockHint($event, p.req)">
                  <AppIcon name="lock" :size="16" />
                  <span>完整档案已封存 · {{ needText(p.req) }}</span>
                  <button type="button" class="kcard__unlock" @click.stop="go('/missions')">
                    去完成任务
                  </button>
                </div>
              </div>
            </article>
        </div>
      </section>

      <!-- ===== 卷三 · 龙族武器 ===== -->
      <section v-else-if="tab === 'arms'" class="shelf">
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="sparkle" :size="15" /> 龙族武器情报</h2>
          <span class="shelf__more">共 {{ ARCHIVE_ARMS.length }} 件 · 按密级解锁</span>
        </header>

        <div class="agrid">
          <article
            v-for="a in ARCHIVE_ARMS"
            :key="a.code"
            class="acard"
            :class="{ 'is-locked': !hasClearance(a.req) }"
          >
            <div class="acard__top">
              <span class="acard__code">{{ a.code }}</span>
              <span class="acard__rank">{{ a.rank }}</span>
            </div>
            <h3>{{ a.name }}</h3>
            <p class="acard__type">{{ a.type }} · {{ a.origin }}</p>

            <!-- 详情按密级解锁 -->
            <template v-if="hasClearance(a.req)">
              <p class="acard__desc">{{ a.desc }}</p>
              <p class="acard__perk">{{ a.perk }}</p>
              <div class="acard__tags">
                <span v-for="t in a.tags" :key="t">{{ t }}</span>
              </div>
              <p class="acard__status">状态：{{ a.status }}</p>
            </template>
            <div v-else class="acard__locked" @click="lockHint($event, a.req)">
              <AppIcon name="lock" :size="16" />
              <span>武器详情已封存 · {{ needText(a.req) }}</span>
            </div>
          </article>
        </div>
      </section>

      <!-- ===== 卷四 · 任务执行记录 ===== -->
      <section v-else class="shelf">
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="archive" :size="15" /> 任务执行记录</h2>
          <span class="shelf__more">
            已解锁 {{ logs.length }} 条 · 封存 {{ lockedLogs.length }} 条
          </span>
        </header>

        <div class="lgrid">
          <article v-for="(m, i) in ARCHIVE_MISSION_LOGS" :key="m.code + i" class="log">
            <div class="log__head">
              <span class="log__code">{{ m.code }}</span>
              <b>{{ m.name }}</b>
              <span class="log__who">
                <template v-if="hasClearance(m.req)">{{ m.person }} · {{ m.deptLabel }}</template>
                <template v-else>执行人封存</template>
              </span>
            </div>

            <template v-if="hasClearance(m.req)">
              <p class="log__p"><i>执行过程</i>{{ m.process }}</p>
              <p class="log__r"><i>完成结果</i>{{ m.result }}</p>
            </template>
            <div v-else class="log__lock" @click="lockHint($event, m.req)">
              <AppIcon name="lock" :size="14" /> 过程与结果已封存 · {{ needText(m.req) }}
            </div>
          </article>
        </div>
      </section>
    </main>

    <!-- 轻提示：点击封存内容时给出反馈 -->
    <transition name="toast">
      <div v-if="toast" class="toast"><AppIcon name="lock" :size="14" /> {{ toast }}</div>
    </transition>

    <!-- ============ 页脚 ============ -->
    <footer class="arc__foot">
      <span>以龙档案馆 · 同人兴趣站 · 非商用</span>
      <span class="arc__foot-meta">版权归原作者所有 · 仅作学习交流</span>
    </footer>
  </div>
</template>

<style scoped>
.arc {
  position: relative;
  z-index: 1;
  min-height: 100%;
}

/* ================= 主体 ================= */
.arc__body {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 30px 24px 64px;
}

.arc__hero {
  margin-bottom: 22px;
}

.arc__hero-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.arc__hero-mark {
  width: 52px;
  height: 52px;
  filter: drop-shadow(0 0 14px rgba(216, 178, 90, 0.45));
  animation: spin 60s linear infinite;
}

.arc__title {
  font-family: var(--font-serif);
  font-size: 26px;
  letter-spacing: 0.12em;
  color: var(--text);
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
}

.arc__title-en {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.28em;
  color: var(--gold-dp);
  text-transform: uppercase;
}

.arc__sub {
  margin-top: 6px;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--text-3);
}

/* ================= 权限面板 ================= */
.perm {
  padding: 16px 20px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  box-shadow: 0 0 24px -14px rgba(216, 178, 90, 0.4);
  margin-bottom: 20px;
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

/* ================= 密级提示条 ================= */
.notice {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  padding: 12px 16px;
  border-radius: var(--r-lg);
  border: 1px solid rgba(216, 178, 90, 0.32);
  background: linear-gradient(120deg, rgba(216, 178, 90, 0.12), rgba(216, 178, 90, 0.03));
  font-size: 12.5px;
  letter-spacing: 0.04em;
  color: var(--text-2);
}

.notice em {
  font-style: normal;
  color: var(--gold-lt);
}

.notice__go {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.12);
  color: var(--gold-lt);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.22s, transform 0.2s;
}

.notice__go:hover {
  background: rgba(216, 178, 90, 0.24);
  transform: translateY(-1px);
}

/* ================= 分卷 ================= */
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(14, 20, 34, 0.5);
  color: var(--text-2);
  font-size: 12.5px;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: color 0.22s, border-color 0.22s, background 0.22s;
}

.tab:hover {
  color: var(--text);
  border-color: rgba(216, 178, 90, 0.45);
}

.tab.is-on {
  color: #1a1206;
  font-weight: 600;
  border-color: var(--gold);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
}

/* ================= 区块标题 ================= */
.shelf__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px 13px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.26);
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

/* ================= 通用 chips ================= */
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pchips {
  margin-bottom: 14px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(14, 20, 34, 0.5);
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}

.chip:hover {
  color: var(--text);
  border-color: rgba(216, 178, 90, 0.45);
}

.chip.is-on {
  color: #1a1206;
  font-weight: 600;
  border-color: var(--gold);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
}

.chip--sm {
  padding: 5px 12px;
  font-size: 12px;
}

.chip__n {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-4);
}

.chip.is-on .chip__n {
  background: rgba(0, 0, 0, 0.22);
  color: rgba(26, 18, 6, 0.75);
}

/* ================= 龙王卷宗 ================= */
.kgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 16px;
  align-items: start;
}

.kcard {
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  overflow: hidden;
  transition: border-color 0.26s, box-shadow 0.26s;
}

.kcard:hover {
  border-color: rgba(216, 178, 90, 0.45);
  box-shadow: 0 0 26px -14px rgba(216, 178, 90, 0.6);
}

.kcard.is-locked {
  opacity: 0.9;
}

.kcard__top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  cursor: pointer;
}

.kcard__id {
  display: flex;
  flex-direction: column;
  gap: 5px;
  align-items: flex-start;
}

.kcard__code {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--gold-dp);
  padding: 3px 9px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(216, 178, 90, 0.06);
}

.kcard__threat {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--blood-lt);
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(226, 86, 79, 0.45);
  background: rgba(179, 39, 45, 0.12);
}

.kcard__name {
  flex: 1;
  min-width: 0;
}

.kcard__name h3 {
  font-family: var(--font-serif);
  font-size: 16px;
  letter-spacing: 0.08em;
  color: var(--text);
}

.kcard__name p {
  margin-top: 3px;
  font-size: 11.5px;
  letter-spacing: 0.05em;
  color: var(--text-4);
}

.kcard__lv {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-3);
}

.kcard__lv[data-lv='3'] {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.08);
}

.kcard__lv[data-lv='4'] {
  color: #f0a39c;
  border-color: rgba(239, 107, 98, 0.5);
  background: rgba(179, 39, 45, 0.14);
}

.kcard__caret {
  color: var(--text-4);
  transition: transform 0.28s;
}

.kcard__caret.is-open {
  transform: rotate(180deg);
  color: var(--gold-lt);
}

.kcard__body {
  padding: 0 18px 18px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
}

.kcard__desc {
  margin-top: 12px;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--text-3);
}

.kcard__meta {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 11.5px;
  color: var(--text-3);
}

.kcard__meta i {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  margin-right: 8px;
}

.kcard__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.kcard__tags span {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  font-size: 11px;
  color: var(--text-3);
  background: rgba(216, 178, 90, 0.05);
}

.kcard__intel {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
}

.kcard__intel-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.12em;
  color: var(--gold-lt);
  margin-bottom: 10px;
}

.intel {
  display: flex;
  gap: 10px;
  padding: 8px 0;
}

.intel__no {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--gold-dp);
  padding-top: 2px;
}

.intel__body b {
  font-size: 12.5px;
  color: var(--text);
  letter-spacing: 0.06em;
}

.intel__body p {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--text-3);
}

/* ================= 封存条 / 展开按钮 ================= */
.kcard__toggle {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: 12px;
  padding: 7px 15px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.42);
  background: rgba(216, 178, 90, 0.08);
  color: var(--gold-lt);
  font-size: 12px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: background 0.22s, border-color 0.22s;
}

.kcard__toggle:hover {
  background: rgba(216, 178, 90, 0.2);
  border-color: rgba(216, 178, 90, 0.8);
}

.kcard__locked,
.person__locked,
.acard__locked,
.log__lock {
  cursor: pointer;
  user-select: none;
}

.person__note {
  margin-top: 6px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--text-4);
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
  padding: 11px 22px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.96), rgba(14, 11, 23, 0.98));
  box-shadow: 0 16px 40px -20px rgba(0, 0, 0, 0.9), 0 0 24px -12px var(--gold-glow);
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--gold-lt);
}

/* 点击封存内容时的抖动反馈：证明点击已生效 */
.kcard__locked.is-shake,
.person__locked.is-shake,
.acard__locked.is-shake,
.log__lock.is-shake {
  animation: lock-shake 0.5s ease;
}

@keyframes lock-shake {
  0%,
  100% {
    transform: translateX(0);
  }
  20% {
    transform: translateX(-5px);
  }
  40% {
    transform: translateX(5px);
  }
  60% {
    transform: translateX(-3px);
  }
  80% {
    transform: translateX(3px);
  }
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

/* ================= 锁态 ================= */
.kcard__locked,
.person__locked,
.acard__locked {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 14px 18px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
  color: var(--text-4);
  font-size: 12px;
  letter-spacing: 0.06em;
}

.kcard__unlock {
  margin-left: auto;
  padding: 5px 13px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.4);
  background: rgba(216, 178, 90, 0.1);
  color: var(--gold-lt);
  font-size: 11.5px;
  cursor: pointer;
  transition: background 0.22s;
}

.kcard__unlock:hover {
  background: rgba(216, 178, 90, 0.22);
}

/* ================= 部门人物 ================= */
.pgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
  align-items: start;
}

.person {
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  overflow: hidden;
  transition: border-color 0.26s, box-shadow 0.26s;
}

.person:hover {
  border-color: rgba(216, 178, 90, 0.4);
}

.person.is-open {
  border-color: rgba(216, 178, 90, 0.6);
  box-shadow: 0 0 30px -14px rgba(216, 178, 90, 0.7);
}

.person__top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  cursor: pointer;
}

.person__avatar {
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 17px;
  color: #1a1206;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5), 0 0 16px -6px var(--gold-glow);
}

.person[data-tone='ice'] .person__avatar {
  background: linear-gradient(180deg, #a5e4ff, #4aa8d4);
}

.person[data-tone='fire'] .person__avatar {
  background: linear-gradient(180deg, #f6c07a, #cf6a36);
}

.person[data-tone='rose'] .person__avatar {
  background: linear-gradient(180deg, #f7b6c2, #c05a72);
}

.person__id {
  flex: 1;
  min-width: 0;
}

.person__id h3 {
  font-family: var(--font-serif);
  font-size: 16px;
  letter-spacing: 0.08em;
  color: var(--text);
}

.person__id p {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--text-4);
}

.person__dept {
  display: inline-block;
  margin-top: 5px;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: var(--text-3);
}

.person__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.person__blood {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--gold-lt);
}

.person__lv {
  font-family: var(--font-mono);
  font-size: 10px;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-3);
}

.person__lv[data-lv='3'] {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.08);
}

.person__lv[data-lv='4'] {
  color: #f0a39c;
  border-color: rgba(239, 107, 98, 0.5);
  background: rgba(179, 39, 45, 0.14);
}

.person__caret {
  color: var(--text-4);
  transition: transform 0.28s;
}

.person__caret.is-open {
  transform: rotate(180deg);
  color: var(--gold-lt);
}

.person__body {
  padding: 0 18px 18px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
}

.person__brief {
  margin-top: 12px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-3);
}

.person__quote {
  margin-top: 8px;
  font-family: var(--font-serif);
  font-size: 12.5px;
  color: var(--gold-dp);
}

.dossier {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.block h4 {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.12em;
  color: var(--gold-lt);
  margin-bottom: 8px;
}

.block ul {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 16px;
  list-style: disc;
}

.block li {
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-3);
}

.honors li::marker {
  color: var(--gold);
}

.block p {
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--text-3);
}

.block--split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.weapon__row,
.spirit__row,
.mission__row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.weapon__row b,
.spirit__row b,
.mission__row b {
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--text);
}

.weapon__type,
.spirit__lv,
.mission__code {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-3);
}

.spirit {
  padding: 6px 0;
}

.spirit + .spirit {
  border-top: 1px dashed rgba(255, 255, 255, 0.05);
}

.spirit__lv {
  color: var(--gold-dp);
}

.mission {
  padding: 8px 10px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.4);
}

.mission + .mission {
  margin-top: 8px;
}

.mission__p,
.mission__r {
  margin-top: 5px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--text-3);
}

.mission__p i,
.mission__r i {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  margin-right: 7px;
}

.mission__r i {
  color: var(--gold-dp);
}

.muted {
  font-size: 12px;
  color: var(--text-4);
}

.dossier__note {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--text-4);
  padding-top: 4px;
  border-top: 1px dashed rgba(255, 255, 255, 0.05);
}

/* ================= 龙族武器 ================= */
.agrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  grid-auto-rows: 1fr;
  gap: 16px;
}

.acard {
  display: flex;
  flex-direction: column;
  padding: 16px 18px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
  transition: border-color 0.26s, transform 0.26s;
}

.acard:hover {
  border-color: rgba(216, 178, 90, 0.45);
  transform: translateY(-3px);
}

.acard__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.acard__code {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--gold-dp);
}

.acard__rank {
  font-family: var(--font-serif);
  font-size: 12px;
  color: var(--gold-lt);
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.08);
}

.acard h3 {
  font-family: var(--font-serif);
  font-size: 15.5px;
  letter-spacing: 0.08em;
  color: var(--text);
}

.acard__type {
  margin-top: 4px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.acard__desc {
  margin-top: 10px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-3);
}

.acard__perk {
  margin-top: 10px;
  font-size: 12px;
  color: var(--gold-dp);
}

.acard__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.acard__tags span {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  font-size: 11px;
  color: var(--text-3);
}

.acard__status {
  margin-top: auto;
  padding-top: 12px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

/* ================= 任务执行记录 ================= */
.lgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 16px;
}

.log {
  padding: 14px 16px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(165deg, rgba(33, 28, 47, 0.85), rgba(14, 11, 23, 0.94));
}

.log__head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.log__code {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.18em;
  color: var(--gold-dp);
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
}

.log__head b {
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 0.06em;
  color: var(--text);
}

.log__who {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-4);
}

.log__p,
.log__r {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--text-3);
}

.log__p i,
.log__r i {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  margin-right: 7px;
}

.log__r i {
  color: var(--gold-dp);
}

.log--locked {
  opacity: 0.75;
}

.log__lock {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-4);
}

/* ================= 页脚 ================= */
.arc__foot {
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

/* ================= 响应式 ================= */
@media (max-width: 720px) {
  .block--split {
    grid-template-columns: 1fr;
  }

  .arc__title {
    font-size: 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
