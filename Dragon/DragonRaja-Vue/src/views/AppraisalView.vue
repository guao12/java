<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import { getUser } from '@/utils/auth'
import {
  DAILY_LIMIT,
  GRADES,
  consumeQuota,
  gradeOf,
  isBloodlineAdmin,
  loadRecords as readStore,
  saveRecords,
  todayKey,
  todayUsedCount
} from '@/utils/bloodline'

const route = useRoute()
const router = useRouter()

/* ---------------- 当前登录者 ---------------- */

const user = ref(getUser())
const myAccount = computed(() => (user.value?.account || '').trim().toUpperCase())
/** 管理员 = 教员 / 研究员（密级 4），可看全部鉴定记录与排行榜 */
const isAdmin = computed(() => isBloodlineAdmin(user.value))

/* ---------------- 顶部导航 ---------------- */

const NAV = [
  { label: '学院首页', to: '/home' },
  { label: '血统鉴定', to: '/appraisal' },
  { label: '我的身份卡', to: '/console' }
]

/** 当前页面高亮 */
function isCurrent(to) {
  return route.path === to || route.path.startsWith(to + '/')
}

/* ---------------- 血统评级标准：见 utils/bloodline.js（首页共用） ---------------- */

/* ---------------- 表单 ---------------- */

const KINDS = [
  { value: 'human', label: '纯人类', hint: '未检出龙类基因' },
  { value: 'mixed', label: '混血种', hint: '携带龙类基因序列' }
]

const form = reactive({
  studentId: '',
  name: '',
  kind: '',
  pct: 12,
  note: ''
})

const errors = reactive({ studentId: '', name: '', kind: '', pct: '' })

/** 选「纯人类」时龙血占比锁定为 0 */
watch(
  () => form.kind,
  (k) => {
    if (k === 'human') form.pct = 0
    errors.kind = ''
  }
)

const pctNum = computed(() => {
  const n = Number(form.pct)
  if (!Number.isFinite(n)) return 0
  return Math.min(100, Math.max(0, n))
})

/** 表单内的实时评级预览 */
const preview = computed(() => gradeOf(pctNum.value))

/* ---------------- 本机档案（权限过滤 + 保存报告） ---------------- */

const records = ref([])

function loadRecords() {
  records.value = readStore()
}

function persist() {
  saveRecords(records.value) // 与首页共享同一份本机档案
}

/**
 * 历史记录权限：
 * - 管理员（教员/研究员）可见全部；
 * - 学生只能看到自己提交的报告（owner 为本人账号），
 *   旧版本未记录 owner 的数据（!r.owner）视为本机公共档案，保持可见。
 */
const visibleRecords = computed(() => {
  if (isAdmin.value) return records.value
  const me = myAccount.value
  return records.value.filter((r) => !r.owner || r.owner === me || r.studentId === me)
})

/* ---------------- 每日鉴定次数限制（每人每天 3 次） ---------------- */

/** 手动 bump 用：localStorage 不是响应式来源，提交成功后 +1 触发重算 */
const quotaTick = ref(0)

/** 今日已用次数（按当前登录账号计） */
const todayUsed = computed(() => {
  void quotaTick.value // 响应式依赖
  return todayUsedCount(myAccount.value)
})
const todayLeft = computed(() => Math.max(0, DAILY_LIMIT - todayUsed.value))

function useQuota() {
  consumeQuota(myAccount.value)
  quotaTick.value++
}

/* ---------------- 排行榜（仅管理员）：全校血统等级统计 ---------------- */

const leaderboard = computed(() => {
  const list = records.value
  const total = list.length
  const rows = GRADES.map((g) => {
    const count = list.filter((r) => r.grade === g.grade).length
    return { ...g, count, ratio: total ? Math.round((count / total) * 100) : 0 }
  })
  const top = [...list].sort((a, b) => b.pct - a.pct).slice(0, 5)
  return { total, rows, top }
})

/* ---------------- 提交 ---------------- */

const submitting = ref(false)
const report = ref(null)
const saved = ref(false)

function validate() {
  let ok = true
  const sid = form.studentId.trim().toUpperCase()
  const name = form.name.trim()

  if (!sid) {
    errors.studentId = '请填写学生学号'
    ok = false
  } else if (!/^[A-Za-z0-9-]{4,20}$/.test(sid)) {
    errors.studentId = '学生学号为 4-20 位字母、数字或连字符'
    ok = false
  } else {
    errors.studentId = ''
  }

  if (!name) {
    errors.name = '请填写学生姓名'
    ok = false
  } else if (name.length > 20) {
    errors.name = '姓名不超过 20 个字符'
    ok = false
  } else {
    errors.name = ''
  }

  if (!form.kind) {
    errors.kind = '请选择血统类型'
    ok = false
  } else {
    errors.kind = ''
  }

  const raw = String(form.pct).trim()
  if (raw === '') {
    errors.pct = '请填写龙血占比'
    ok = false
  } else if (!Number.isFinite(Number(raw))) {
    errors.pct = '龙血占比需为数字'
    ok = false
  } else if (Number(raw) < 0 || Number(raw) > 100) {
    errors.pct = '龙血占比需在 0 ~ 100 之间'
    ok = false
  } else {
    errors.pct = ''
  }

  return ok
}

function makeCode() {
  return `BL-${new Date().getFullYear()}-${Math.floor(Math.random() * 9000 + 1000)}`
}

function fmtTime(d) {
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()} 年 ${p(d.getMonth() + 1)} 月 ${p(d.getDate())} 日 ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function onSubmit() {
  if (submitting.value) return
  if (!validate()) return

  // 每日限 3 次，防止频繁刷数据
  if (todayLeft.value <= 0) {
    errors.pct = `今日鉴定次数已用完（每日限 ${DAILY_LIMIT} 次），请明日再送检`
    return
  }

  submitting.value = true
  await new Promise((r) => setTimeout(r, 880)) // 采样 · 离心 · 比对的过场
  submitting.value = false

  const sid = form.studentId.trim().toUpperCase()
  const g = gradeOf(pctNum.value)
  useQuota()
  report.value = {
    code: makeCode(),
    studentId: sid,
    name: form.name.trim(),
    kind: form.kind === 'human' ? '纯人类' : '混血种',
    pct: pctNum.value,
    grade: g.grade,
    gradeLabel: g.label,
    gradeRgb: g.rgb,
    risk: g.risk,
    danger: pctNum.value > 50,
    /** S/A 级追加精神不稳定红色警告（原著设定） */
    unstable: g.grade === 'S' || g.grade === 'A',
    owner: myAccount.value || '',
    note: form.note.trim(),
    at: Date.now(),
    time: fmtTime(new Date())
  }
  saved.value = false
}

function resetForm() {
  form.studentId = ''
  form.name = ''
  form.kind = ''
  form.pct = 12
  form.note = ''
  errors.studentId = ''
  errors.name = ''
  errors.kind = ''
  errors.pct = ''
}

function closeReport() {
  report.value = null
}

function saveReport() {
  if (!report.value || saved.value) return
  const r = { ...report.value }
  records.value = [r, ...records.value].slice(0, 20)
  persist()
  saved.value = true
}

/** 查看已归档的报告（只读）；兼容旧数据补齐 unstable 标记 */
function viewRecord(rec) {
  report.value = {
    unstable: rec.grade === 'S' || rec.grade === 'A',
    ...rec
  }
  saved.value = true
}

/** 导出文本版鉴定报告（.txt 下载） */
function exportReport() {
  if (!report.value) return
  const r = report.value
  const lines = [
    '═══════════════════════════════════',
    '   卡塞尔学院血统检测报告书',
    '   Cassell College · Bloodline Test',
    '═══════════════════════════════════',
    `报告编号：${r.code}`,
    `签发时间：${r.time}`,
    '-----------------------------------',
    `姓　　名：${r.name}`,
    `学生学号：${r.studentId}`,
    `血统类型：${r.kind}`,
    `龙血比例：${r.pct}%`,
    `血统评级：${r.grade} 级（${r.gradeLabel}）`,
    '-----------------------------------',
    `风险警告：${r.risk}`
  ]
  if (r.unstable) lines.push('特别警示：精神不稳定风险，须定期返校复查。')
  if (r.note) lines.push(`检测描述：${r.note}`)
  lines.push('-----------------------------------', '签发单位：卡塞尔学院 血统鉴定科', '本报告由基因检测系统自动生成，仅供校内存档。')

  const blob = new Blob(['﻿' + lines.join('\r\n')], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `血统检测报告_${r.studentId}_${r.code}.txt`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

function onKey(e) {
  if (e.key === 'Escape') closeReport()
}

onMounted(() => {
  document.documentElement.style.colorScheme = 'dark'
  document.title = '血统鉴定 · 卡塞尔学院'
  loadRecords()
  window.addEventListener('keydown', onKey)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div class="appr">
    <RuneBackground />

    <!-- ============ 顶部导航 ============ -->
    <header class="appr__nav">
      <div class="appr__nav-inner">
        <div class="appr__brand">
          <DragonEmblem class="appr__mark" />
          <div class="appr__brand-text">
            <span class="appr__brand-cn">卡塞尔学院</span>
            <span class="appr__brand-en">Cassell College</span>
          </div>
        </div>

        <nav class="appr__links">
          <button
            v-for="item in NAV"
            :key="item.to"
            type="button"
            class="appr__link"
            :class="{ 'is-current': isCurrent(item.to) }"
            :aria-current="isCurrent(item.to) ? 'page' : undefined"
            @click="router.push(item.to)"
          >
            {{ item.label }}
          </button>
        </nav>
      </div>
    </header>

    <!-- ============ 核心鉴定区域 ============ -->
    <main class="appr__body">
      <!-- 页面头部：与「言灵图鉴」标题样式同源（标识 + 衬线标题） -->
      <section class="appr__hero">
        <div class="appr__hero-row">
          <DragonEmblem class="appr__hero-mark" />
          <div class="appr__hero-text">
            <h1 class="appr__title">血统鉴定<span class="appr__title-en">BLOODLINE TEST</span></h1>
            <p class="appr__sub">卡塞尔学院基因检测系统・仅限在读学生提交检测</p>
          </div>
        </div>
      </section>

      <!-- 鉴定受理表 -->
      <section class="appr__card">
        <i class="appr__beam" />

        <header class="appr__card-head">
          <div>
            <h2 class="appr__card-title">鉴定受理表</h2>
            <p class="appr__card-desc">血统鉴定科 · 样本送检单</p>
          </div>
          <span class="appr__card-tag">
            <AppIcon name="shield" :size="12" />
            机密
          </span>
        </header>

        <form class="appr__form" novalidate @submit.prevent="onSubmit">
          <!-- ① 学院编号 -->
          <div class="af" :class="{ 'is-error': errors.studentId }">
            <label class="af__label" for="ap-sid">
              <span class="af__no">01</span> 学生学号
              <em>唯一</em>
            </label>
            <div class="af__control">
              <AppIcon name="id" :size="16" class="af__icon" />
              <input
                id="ap-sid"
                v-model="form.studentId"
                type="text"
                maxlength="20"
                placeholder="如 CC20260001"
                autocomplete="off"
              />
            </div>
            <p v-if="errors.studentId" class="af__msg">
              <AppIcon name="alert" :size="12" />{{ errors.studentId }}
            </p>
          </div>

          <!-- ② 学生姓名 -->
          <div class="af" :class="{ 'is-error': errors.name }">
            <label class="af__label" for="ap-name">
              <span class="af__no">02</span> 学生姓名
            </label>
            <div class="af__control">
              <AppIcon name="user" :size="16" class="af__icon" />
              <input
                id="ap-name"
                v-model="form.name"
                type="text"
                maxlength="20"
                placeholder="请输入受检者姓名"
                autocomplete="off"
              />
            </div>
            <p v-if="errors.name" class="af__msg">
              <AppIcon name="alert" :size="12" />{{ errors.name }}
            </p>
          </div>

          <!-- ③ 血统类型 -->
          <div class="af" :class="{ 'is-error': errors.kind }">
            <label class="af__label">
              <span class="af__no">03</span> 血统类型
            </label>
            <div class="af__kinds">
              <button
                v-for="k in KINDS"
                :key="k.value"
                type="button"
                class="af__kind"
                :class="{ 'is-on': form.kind === k.value }"
                @click="form.kind = k.value"
              >
                <span class="af__kind-name">{{ k.label }}</span>
                <span class="af__kind-hint">{{ k.hint }}</span>
              </button>
            </div>
            <p v-if="errors.kind" class="af__msg">
              <AppIcon name="alert" :size="12" />{{ errors.kind }}
            </p>
          </div>

          <!-- ④ 龙血占比 -->
          <div class="af" :class="{ 'is-error': errors.pct, 'is-locked': form.kind === 'human' }">
            <label class="af__label" for="ap-pct">
              <span class="af__no">04</span> 龙血占比
              <em>0 ~ 100</em>
            </label>
            <div class="af__pct">
              <input
                id="ap-pct"
                v-model="form.pct"
                class="af__pct-num"
                type="number"
                min="0"
                max="100"
                step="0.1"
                :disabled="form.kind === 'human'"
              />
              <span class="af__pct-unit">%</span>
              <input
                v-model.number="form.pct"
                class="af__pct-range"
                type="range"
                min="0"
                max="100"
                step="0.5"
                :disabled="form.kind === 'human'"
                aria-label="龙血占比滑块"
              />
            </div>
            <div class="af__gauge">
              <i
                class="af__gauge-fill"
                :style="{ width: pctNum + '%', background: `linear-gradient(90deg, rgba(${preview.rgb}, .35), rgba(${preview.rgb}, .95))` }"
              />
              <i class="af__gauge-mark" />
            </div>
            <p v-if="form.kind === 'human'" class="af__tip">
              <AppIcon name="info" :size="12" />纯人类不参与龙血占比测定，已锁定为 0。
            </p>
            <p v-else-if="errors.pct" class="af__msg">
              <AppIcon name="alert" :size="12" />{{ errors.pct }}
            </p>
            <p v-else class="af__tip">
              <AppIcon name="info" :size="12" />
              预判评级 <b :style="{ color: `rgb(${preview.rgb})` }">{{ preview.grade }}</b> · {{ preview.label }}
            </p>
          </div>

          <!-- ⑤ 备注 -->
          <div class="af">
            <label class="af__label" for="ap-note">
              <span class="af__no">05</span> 检测描述
              <em>选填</em>
            </label>
            <textarea
              id="ap-note"
              v-model="form.note"
              class="af__area"
              rows="3"
              maxlength="200"
              placeholder="采样环境、受检者状态、委托事由等"
            />
            <p class="af__tip af__tip--right">{{ form.note.length }} / 200</p>
          </div>

          <button type="submit" class="appr__submit" :disabled="submitting">
            <span v-if="!submitting" class="appr__submit-glow" />
            <AppIcon v-if="submitting" name="refresh" :size="15" class="appr__submit-spin" />
            <AppIcon v-else name="flame" :size="15" />
            {{ submitting ? '采样分析中…' : '开始血统鉴定' }}
          </button>
        </form>

        <footer class="appr__card-foot">
          <AppIcon name="info" :size="12" />
          鉴定数据仅存于本机档案（localStorage），不会上传学院服务器。
          <span class="appr__quota" :class="{ 'is-out': todayLeft <= 0 }">
            今日剩余 {{ todayLeft }}/{{ DAILY_LIMIT }} 次
          </span>
          <button type="button" class="appr__reset" @click="resetForm">清空表单</button>
        </footer>
      </section>

      <!-- 历史鉴定记录：学生只看本人，管理员看全部 -->
      <section v-if="visibleRecords.length" class="appr__arch">
        <h3 class="appr__arch-title">
          <AppIcon name="archive" :size="14" />
          {{ isAdmin ? '全部学生鉴定记录' : '历史鉴定记录' }}
          <span>{{ visibleRecords.length }}</span>
          <em v-if="isAdmin" class="appr__arch-badge">管理员视图</em>
        </h3>
        <ul class="appr__arch-grid">
          <li
            v-for="r in visibleRecords"
            :key="r.code"
            class="appr__arch-card"
            :class="{ 'is-danger': r.grade === 'S' || r.grade === 'A' }"
            @click="viewRecord(r)"
          >
            <span class="appr__arch-grade" :style="{ '--gc': r.gradeRgb }">{{ r.grade }}</span>
            <div class="appr__arch-info">
              <b>{{ r.name }}</b>
              <span class="mono">{{ r.studentId }} · {{ r.code }}</span>
            </div>
            <div class="appr__arch-side">
              <b class="appr__arch-pct" :style="{ color: `rgb(${r.gradeRgb})` }">{{ r.pct }}%</b>
              <span class="appr__arch-time">{{ r.time.slice(0, 11) }}</span>
            </div>
            <p v-if="r.grade === 'S' || r.grade === 'A'" class="appr__arch-warn">
              <AppIcon name="alert" :size="11" />精神不稳定风险 · 定期返校复查
            </p>
          </li>
        </ul>
      </section>

      <!-- 血统排行榜：仅管理员可见 -->
      <section v-if="isAdmin" class="appr__rank">
        <h3 class="appr__arch-title">
          <AppIcon name="shield" :size="14" /> 全校血统等级统计
          <span>{{ leaderboard.total }} 份报告</span>
          <em class="appr__arch-badge">仅管理员可见</em>
        </h3>

        <div class="appr__rank-card">
          <ul class="appr__rank-rows">
            <li v-for="row in leaderboard.rows" :key="row.grade" class="appr__rank-row">
              <span class="appr__rank-grade" :style="{ '--gc': row.rgb }">{{ row.grade }}</span>
              <span class="appr__rank-label">{{ row.label }}</span>
              <span class="appr__rank-bar">
                <i
                  :style="{
                    width: row.ratio + '%',
                    background: `linear-gradient(90deg, rgba(${row.rgb}, .35), rgba(${row.rgb}, .95))`
                  }"
                />
              </span>
              <b class="appr__rank-count">{{ row.count }}</b>
            </li>
          </ul>

          <div v-if="leaderboard.top.length" class="appr__rank-top">
            <p class="appr__rank-top-title">龙血占比 TOP 5</p>
            <ol class="appr__rank-top-list">
              <li v-for="(r, i) in leaderboard.top" :key="r.code">
                <em class="appr__rank-no">{{ i + 1 }}</em>
                <span class="appr__rank-name">{{ r.name }}</span>
                <span class="mono appr__rank-sid">{{ r.studentId }}</span>
                <b :style="{ color: `rgb(${r.gradeRgb})` }">{{ r.grade }} · {{ r.pct }}%</b>
              </li>
            </ol>
          </div>
          <p v-else class="appr__rank-empty">暂无鉴定数据，排行榜待生成。</p>
        </div>
      </section>
    </main>

    <!-- ============ 鉴定结果弹窗 ============ -->
    <Teleport to="body">
      <transition name="rpt">
        <div v-if="report" class="rpt" @click.self="closeReport">
          <div class="rpt__card" role="dialog" aria-modal="true" :style="{ '--gc': report.gradeRgb }">
            <i class="rpt__beam" />
            <button type="button" class="rpt__close" aria-label="关闭" @click="closeReport">
              <AppIcon name="chevron" :size="14" class="rpt__close-ic" />
            </button>

            <p class="rpt__eyebrow">Cassell College · Bloodline Test</p>
            <h2 class="rpt__title">《卡塞尔学院血统检测报告书》</h2>

            <!-- 评级印章 -->
            <div class="rpt__grade">
              <span class="rpt__grade-letter">{{ report.grade }}</span>
              <div class="rpt__grade-text">
                <b>{{ report.gradeLabel }}</b>
                <span>血统评级</span>
              </div>
            </div>

            <dl class="rpt__grid">
              <div><dt>姓名</dt><dd>{{ report.name }}</dd></div>
              <div><dt>学生学号</dt><dd class="mono">{{ report.studentId }}</dd></div>
              <div><dt>血统类型</dt><dd>{{ report.kind }}</dd></div>
              <div>
                <dt>龙血比例</dt>
                <dd>
                  <span class="rpt__bar"><i :style="{ width: Math.min(100, report.pct) + '%' }" /></span>
                  <b class="rpt__pct">{{ report.pct }}%</b>
                </dd>
              </div>
              <div class="rpt__grid-wide">
                <dt>风险警告</dt>
                <dd :class="['rpt__risk', { 'is-danger': report.danger }]">
                  <AppIcon :name="report.danger ? 'alert' : 'info'" :size="13" />
                  <span>{{ report.risk }}</span>
                </dd>
                <dd v-if="report.unstable" class="rpt__risk rpt__risk--unstable">
                  <AppIcon name="alert" :size="13" />
                  <span>精神不稳定风险，须定期返校复查。血统鉴定科已备案，复查周期由执行部另行通知。</span>
                </dd>
              </div>
              <div v-if="report.note">
                <dt>检测描述</dt>
                <dd>{{ report.note }}</dd>
              </div>
            </dl>

            <footer class="rpt__foot">
              <span class="rpt__code mono">
                {{ report.code }} · {{ report.time }} · 血统鉴定科签发
              </span>
              <div class="rpt__acts">
                <button type="button" class="rpt__btn" @click="exportReport">
                  <AppIcon name="chevron" :size="14" class="rpt__export-ic" />
                  导出报告
                </button>
                <button
                  type="button"
                  class="rpt__btn rpt__btn--primary"
                  :disabled="saved"
                  @click="saveReport"
                >
                  <AppIcon :name="saved ? 'check' : 'archive'" :size="14" />
                  {{ saved ? '已归档' : '保存报告' }}
                </button>
                <button type="button" class="rpt__btn" @click="closeReport">关闭弹窗</button>
              </div>
            </footer>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ================= 页面外壳 ================= */
.appr {
  position: relative;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 60px;
}

/* ================= 顶部导航 ================= */
.appr__nav {
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

.appr__nav-inner {
  width: 100%;
  max-width: 780px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.appr__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.appr__mark {
  width: 34px;
  height: 34px;
  filter: drop-shadow(0 0 10px rgba(216, 178, 90, 0.5));
  animation: spin 46s linear infinite;
}

.appr__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.appr__brand-cn {
  font-family: var(--font-serif);
  font-size: 15px;
  letter-spacing: 0.26em;
  color: var(--gold-lt);
}

.appr__brand-en {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.3em;
  color: var(--text-4);
  text-transform: uppercase;
}

.appr__links {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 导航项（button 需重置默认样式） */
.appr__link {
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

.appr__link:hover {
  color: var(--text);
  background: rgba(255, 255, 255, 0.04);
}

/* 当前页面高亮 */
.appr__link.is-current {
  color: var(--gold-lt);
  font-weight: 600;
  background: linear-gradient(135deg, rgba(216, 178, 90, 0.16), rgba(216, 178, 90, 0.04));
  box-shadow: inset 0 0 0 1px rgba(216, 178, 90, 0.26), 0 0 20px -10px var(--gold-glow);
}

.appr__link.is-current::after {
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

@keyframes nav-glow {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

/* ================= 主体 ================= */
.appr__body {
  width: 100%;
  max-width: 780px;
  padding: 34px 24px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* ================= 页面头部（与「言灵图鉴」标题样式同源） ================= */
.appr__hero {
  width: 100%;
  margin-bottom: 26px;
  animation: fade-up 0.7s 0.12s both;
}

.appr__hero-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.appr__hero-mark {
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  filter: drop-shadow(0 0 14px rgba(216, 178, 90, 0.55));
  animation: spin 46s linear infinite;
}

.appr__hero-text {
  min-width: 0;
}

.appr__title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--text);
  text-shadow: 0 0 30px rgba(216, 178, 90, 0.3);
  margin-bottom: 6px;
}

.appr__title-en {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.34em;
  color: var(--gold-dp);
  text-transform: uppercase;
  padding: 3px 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(216, 178, 90, 0.06);
  transform: translateY(-3px);
}

.appr__sub {
  font-size: 12px;
  line-height: 1.8;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

/* ================= 鉴定卡片（机密黑色金属质感） ================= */
.appr__card {
  position: relative;
  width: min(560px, 100%);
  padding: 28px 32px 22px;
  border-radius: var(--r-lg);
  border: 1px solid rgba(216, 178, 90, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent 16%),
    linear-gradient(165deg, #151d31 0%, #0a0f1a 54%, #05070d 100%);
  box-shadow:
    0 44px 96px -44px rgba(0, 0, 0, 0.96),
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 0 46px -24px rgba(216, 178, 90, 0.45);
  overflow: hidden;
  animation: fade-up 0.7s 0.2s both;
}

/* 顶部鎏金光带 */
.appr__beam {
  position: absolute;
  top: 0;
  left: 12%;
  right: 12%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
  opacity: 0.85;
}

/* 金属拉丝暗纹 */
.appr__card::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: repeating-linear-gradient(
    115deg,
    rgba(255, 255, 255, 0.014) 0 1px,
    transparent 1px 4px
  );
}

.appr__card-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 16px;
  margin-bottom: 20px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.08);
}

.appr__card-title {
  font-family: var(--font-serif);
  font-size: 19px;
  letter-spacing: 0.16em;
  color: var(--text);
  margin-bottom: 4px;
}

.appr__card-desc {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--text-4);
}

.appr__card-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: 0 0 auto;
  padding: 4px 11px;
  border-radius: 999px;
  font-size: 10.5px;
  letter-spacing: 0.16em;
  color: var(--blood-lt);
  border: 1px solid rgba(226, 86, 79, 0.34);
  background: rgba(179, 39, 45, 0.12);
}

/* ================= 表单 ================= */
.appr__form {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 17px;
}

.af {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.af__label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  letter-spacing: 0.16em;
  color: var(--text-3);
}

.af__no {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--gold-dp);
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid var(--line);
  background: rgba(216, 178, 90, 0.06);
}

.af__label em {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  color: var(--text-4);
  padding: 1px 7px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
}

/* 输入控件：渐变半透明底 + 玻璃质感 */
.af__control {
  display: flex;
  align-items: center;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(135deg, rgba(4, 7, 13, 0.72), rgba(20, 28, 46, 0.42));
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: border-color 0.28s, box-shadow 0.28s, background 0.28s;
}

.af__control:hover {
  border-color: rgba(216, 178, 90, 0.28);
}

.af__control:focus-within {
  border-color: var(--gold);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.13), 0 10px 30px -16px var(--gold-glow);
}

.af__icon {
  flex: 0 0 42px;
  display: grid;
  place-items: center;
  color: var(--text-4);
  transition: color 0.28s;
}

.af__control:focus-within .af__icon {
  color: var(--gold);
}

.af__control input {
  flex: 1;
  min-width: 0;
  padding: 12px 14px 12px 0;
  background: none;
  border: 0;
  outline: none;
  font-size: 14px;
  letter-spacing: 0.04em;
  color: var(--text);
}

.af__control input::placeholder {
  color: var(--text-4);
}

/* 错误态 */
.af.is-error .af__control,
.af.is-error .af__pct {
  border-color: rgba(239, 107, 98, 0.55);
  box-shadow: 0 0 0 3px rgba(239, 107, 98, 0.1);
}

.af__msg {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  letter-spacing: 0.03em;
  color: var(--err);
  animation: fade-in 0.25s both;
}

.af__tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  letter-spacing: 0.03em;
  color: var(--text-4);
}

.af__tip b {
  font-family: var(--font-serif);
  font-size: 14px;
}

.af__tip--right {
  justify-content: flex-end;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
}

/* ③ 血统类型：分段选择 */
.af__kinds {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.af__kind {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 12px 14px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(135deg, rgba(4, 7, 13, 0.7), rgba(20, 28, 46, 0.4));
  text-align: left;
  cursor: pointer;
  transition: border-color 0.28s, background 0.28s, box-shadow 0.28s, transform 0.28s;
}

.af__kind:hover {
  border-color: rgba(216, 178, 90, 0.34);
  transform: translateY(-2px);
}

.af__kind.is-on {
  border-color: var(--gold);
  background: linear-gradient(135deg, rgba(216, 178, 90, 0.18), rgba(216, 178, 90, 0.05));
  box-shadow: 0 0 26px -12px var(--gold-glow);
}

.af__kind-name {
  font-family: var(--font-serif);
  font-size: 15px;
  letter-spacing: 0.14em;
  color: var(--text);
}

.af__kind.is-on .af__kind-name {
  color: var(--gold-lt);
}

.af__kind-hint {
  font-size: 11px;
  letter-spacing: 0.04em;
  color: var(--text-4);
}

/* ④ 龙血占比 */
.af__pct {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(135deg, rgba(4, 7, 13, 0.72), rgba(20, 28, 46, 0.42));
  transition: border-color 0.28s, box-shadow 0.28s;
}

.af__pct:focus-within {
  border-color: var(--gold);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.13);
}

.af__pct-num {
  width: 74px;
  padding: 4px 8px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.8);
  outline: none;
  font-family: var(--font-mono);
  font-size: 16px;
  letter-spacing: 0.06em;
  color: var(--gold-lt);
  text-align: center;
}

.af__pct-unit {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-4);
}

.af__pct-range {
  flex: 1;
  min-width: 0;
  appearance: none;
  -webkit-appearance: none;
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(216, 178, 90, 0.5), rgba(255, 255, 255, 0.08));
  outline: none;
  cursor: pointer;
}

.af__pct-range::-webkit-slider-thumb {
  appearance: none;
  -webkit-appearance: none;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
  box-shadow: 0 0 12px -1px var(--gold-glow);
  cursor: pointer;
}

.af__pct-range::-moz-range-thumb {
  width: 15px;
  height: 15px;
  border: 0;
  border-radius: 50%;
  background: var(--gold);
  box-shadow: 0 0 12px -1px var(--gold-glow);
  cursor: pointer;
}

.af.is-locked .af__pct {
  opacity: 0.55;
}

.af.is-locked .af__pct-range {
  cursor: not-allowed;
}

/* 龙血刻度条 */
.af__gauge {
  position: relative;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.af__gauge-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  transition: width 0.35s cubic-bezier(0.16, 1, 0.3, 1), background 0.35s;
}

/* 50% 危险线 */
.af__gauge-mark {
  position: absolute;
  left: 50%;
  top: -2px;
  bottom: -2px;
  width: 1px;
  background: rgba(239, 107, 98, 0.75);
}

/* ⑤ 备注 */
.af__area {
  width: 100%;
  padding: 11px 14px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(135deg, rgba(4, 7, 13, 0.72), rgba(20, 28, 46, 0.42));
  outline: none;
  resize: vertical;
  font-size: 13px;
  line-height: 1.75;
  color: var(--text);
  transition: border-color 0.28s, box-shadow 0.28s;
}

.af__area::placeholder {
  color: var(--text-4);
}

.af__area:focus {
  border-color: var(--gold);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.13);
}

/* 提交按钮：暗金 + hover 发光 */
.appr__submit {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  margin-top: 6px;
  padding: 14px 22px;
  border: 1px solid rgba(216, 178, 90, 0.42);
  border-radius: var(--r-md);
  cursor: pointer;
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: #f3e6bd;
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.9) 0%, #a67f2f 58%, #6e5218 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35), 0 14px 34px -16px rgba(216, 178, 90, 0.6);
  transition: transform 0.24s, box-shadow 0.32s, filter 0.32s;
}

.appr__submit:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.08) saturate(112%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.45),
    0 20px 44px -16px rgba(216, 178, 90, 0.9),
    0 0 34px -6px rgba(216, 178, 90, 0.75);
}

.appr__submit:active:not(:disabled) {
  transform: translateY(0);
}

.appr__submit:disabled {
  cursor: progress;
  opacity: 0.82;
}

/* 扫光 */
.appr__submit-glow {
  position: absolute;
  top: 0;
  left: -120%;
  width: 55%;
  height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  transform: skewX(-18deg);
  transition: left 0.7s cubic-bezier(0.5, 0, 0.2, 1);
}

.appr__submit:hover:not(:disabled) .appr__submit-glow {
  left: 130%;
}

.appr__submit-spin {
  animation: spin 0.8s linear infinite;
}

.appr__card-foot {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed rgba(255, 255, 255, 0.07);
  font-size: 11px;
  letter-spacing: 0.04em;
  color: var(--text-4);
}

.appr__quota {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--gold-lt);
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.28);
  background: rgba(216, 178, 90, 0.07);
}

.appr__quota.is-out {
  color: var(--err);
  border-color: rgba(239, 107, 98, 0.4);
  background: rgba(179, 39, 45, 0.12);
}

.appr__reset {
  margin-left: auto;
  padding: 0;
  border: 0;
  background: none;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--text-3);
  cursor: pointer;
  border-bottom: 1px solid transparent;
  transition: color 0.24s, border-color 0.24s;
}

.appr__reset:hover {
  color: var(--gold);
  border-color: rgba(216, 178, 90, 0.5);
}

/* ================= 历史鉴定记录（卡片网格） ================= */
.appr__arch,
.appr__rank {
  width: min(560px, 100%);
  margin-top: 26px;
  animation: fade-up 0.7s 0.28s both;
}

.appr__arch-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 0.16em;
  color: var(--text-2);
  padding: 0 4px 12px;
}

.appr__arch-title span {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.appr__arch-badge {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
  padding: 2px 9px;
  margin-left: auto;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.34);
  background: rgba(216, 178, 90, 0.08);
}

.appr__arch-grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
}

.appr__arch-card {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 14px 14px 12px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(120deg, rgba(20, 28, 46, 0.6), rgba(8, 12, 22, 0.75));
  cursor: pointer;
  transition: border-color 0.26s, transform 0.26s, box-shadow 0.26s;
}

.appr__arch-card:hover {
  border-color: rgba(216, 178, 90, 0.34);
  transform: translateY(-2px);
  box-shadow: 0 18px 40px -22px rgba(0, 0, 0, 0.9);
}

.appr__arch-card.is-danger {
  border-color: rgba(239, 107, 98, 0.3);
  background: linear-gradient(120deg, rgba(179, 39, 45, 0.1), rgba(8, 12, 22, 0.78));
}

.appr__arch-card.is-danger:hover {
  border-color: rgba(239, 107, 98, 0.55);
}

.appr__arch-grade {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 17px;
  color: rgb(var(--gc));
  border: 1px solid rgba(var(--gc), 0.5);
  background: rgba(var(--gc), 0.12);
}

.appr__arch-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.appr__arch-info b {
  font-size: 13.5px;
  letter-spacing: 0.08em;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.appr__arch-info .mono {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--text-4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.appr__arch-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}

.appr__arch-pct {
  font-family: var(--font-mono);
  font-size: 14px;
  letter-spacing: 0.04em;
}

.appr__arch-time {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.06em;
  color: var(--text-4);
}

.appr__arch-warn {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 2px 0 0;
  padding-top: 9px;
  border-top: 1px dashed rgba(239, 107, 98, 0.22);
  font-size: 11px;
  letter-spacing: 0.04em;
  color: var(--err);
}

/* ================= 排行榜（仅管理员） ================= */
.appr__rank-card {
  padding: 18px 20px 16px;
  border-radius: var(--r-md);
  border: 1px solid rgba(216, 178, 90, 0.16);
  background: linear-gradient(165deg, rgba(21, 29, 49, 0.9), rgba(8, 12, 22, 0.85));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.appr__rank-rows {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.appr__rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.appr__rank-grade {
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 13px;
  color: rgb(var(--gc));
  border: 1px solid rgba(var(--gc), 0.5);
  background: rgba(var(--gc), 0.12);
}

.appr__rank-label {
  flex: 0 0 96px;
  font-size: 11.5px;
  letter-spacing: 0.06em;
  color: var(--text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.appr__rank-bar {
  flex: 1;
  min-width: 0;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.appr__rank-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.appr__rank-count {
  flex: 0 0 26px;
  text-align: right;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-2);
}

.appr__rank-top {
  margin-top: 16px;
  padding-top: 13px;
  border-top: 1px dashed rgba(255, 255, 255, 0.08);
}

.appr__rank-top-title {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--gold-dp);
  margin-bottom: 9px;
}

.appr__rank-top-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.appr__rank-top-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;
  color: var(--text-2);
}

.appr__rank-no {
  flex: 0 0 20px;
  font-style: normal;
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--gold-lt);
  text-align: center;
}

.appr__rank-name {
  letter-spacing: 0.06em;
}

.appr__rank-sid {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-4);
}

.appr__rank-top-list b {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.appr__rank-empty {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--text-4);
  text-align: center;
  padding: 8px 0 4px;
}

/* ================= 报告弹窗 ================= */
.rpt {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(3, 5, 10, 0.72);
  backdrop-filter: blur(9px) saturate(120%);
  -webkit-backdrop-filter: blur(9px) saturate(120%);
}

.rpt__card {
  position: relative;
  width: min(560px, 100%);
  max-height: 86vh;
  overflow-y: auto;
  padding: 30px 34px 24px;
  border-radius: var(--r-lg);
  border: 1px solid rgba(var(--gc), 0.32);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 14%),
    linear-gradient(165deg, #17203a 0%, #0a0f1a 56%, #05070d 100%);
  box-shadow:
    0 50px 110px -40px rgba(0, 0, 0, 0.96),
    0 0 60px -28px rgba(var(--gc), 0.55);
}

.rpt__beam {
  position: absolute;
  top: 0;
  left: 12%;
  right: 12%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
}

.rpt__close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--line-soft);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.22s, border-color 0.22s, transform 0.22s;
}

.rpt__close:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  transform: rotate(180deg);
}

.rpt__eyebrow {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: var(--gold-dp);
  margin-bottom: 8px;
}

.rpt__title {
  font-family: var(--font-serif);
  font-size: 22px;
  letter-spacing: 0.1em;
  color: var(--text);
  margin-bottom: 20px;
  padding-right: 40px;
  text-shadow: 0 0 26px rgba(216, 178, 90, 0.22);
}

/* 评级印章 */
.rpt__grade {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 18px;
  border-radius: var(--r-md);
  border: 1px solid rgba(var(--gc), 0.3);
  background: linear-gradient(120deg, rgba(var(--gc), 0.14), rgba(4, 7, 13, 0.4));
}

.rpt__grade-letter {
  flex: 0 0 auto;
  width: 62px;
  height: 62px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
  color: rgb(var(--gc));
  border: 2px solid rgba(var(--gc), 0.55);
  background: radial-gradient(70% 70% at 50% 30%, rgba(var(--gc), 0.2), transparent 72%);
  box-shadow: 0 0 30px -8px rgba(var(--gc), 0.7), inset 0 0 22px -8px rgba(var(--gc), 0.6);
  animation: seal-in 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes seal-in {
  from {
    opacity: 0;
    transform: scale(1.5) rotate(-12deg);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: none;
    filter: none;
  }
}

.rpt__grade-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.rpt__grade-text b {
  font-family: var(--font-serif);
  font-size: 17px;
  letter-spacing: 0.14em;
  color: rgb(var(--gc));
}

.rpt__grade-text span {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--text-4);
}

/* 报告字段 */
.rpt__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 0 0 16px;
  padding: 15px 17px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(135deg, rgba(4, 7, 13, 0.62), rgba(20, 28, 46, 0.35));
}

.rpt__grid-wide {
  grid-column: 1 / -1;
}

.rpt__grid > div {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.rpt__grid dt {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.18em;
  color: var(--text-4);
}

.rpt__grid dd {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  letter-spacing: 0.04em;
  color: var(--text-2);
}

.rpt__grid dd.mono,
.rpt__code.mono {
  font-family: var(--font-mono);
  letter-spacing: 0.1em;
}

/* 龙血条 */
.rpt__bar {
  flex: 1;
  min-width: 0;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
  display: block;
}

.rpt__bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(var(--gc), 0.4), rgb(var(--gc)));
  box-shadow: 0 0 12px -2px rgba(var(--gc), 0.8);
  animation: bar-in 0.8s 0.15s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes bar-in {
  from {
    width: 0 !important;
  }
}

.rpt__grid dd:has(.rpt__bar) {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rpt__pct {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-size: 14px;
  color: rgb(var(--gc));
}

/* 风险警告 */
.rpt__risk {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 11px 13px;
  border-radius: var(--r-sm);
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-2);
  border: 1px solid rgba(var(--gc), 0.28);
  background: linear-gradient(120deg, rgba(var(--gc), 0.1), rgba(4, 7, 13, 0.4));
}

.rpt__risk.is-danger {
  color: #ffd9d6;
  border-color: rgba(239, 107, 98, 0.45);
  background: linear-gradient(120deg, rgba(179, 39, 45, 0.24), rgba(4, 7, 13, 0.45));
  animation: danger-pulse 2.6s ease-in-out infinite;
}

/* S/A 级精神不稳定红色警告 */
.rpt__risk--unstable {
  margin-top: 8px;
  color: #ffb3ad;
  border: 1px solid rgba(239, 107, 98, 0.55);
  background: linear-gradient(120deg, rgba(179, 39, 45, 0.3), rgba(4, 7, 13, 0.5));
  box-shadow: 0 0 20px -8px rgba(239, 107, 98, 0.6);
}

.rpt__export-ic {
  transform: rotate(-90deg);
}

@keyframes danger-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(239, 107, 98, 0);
  }
  50% {
    box-shadow: 0 0 22px -6px rgba(239, 107, 98, 0.55);
  }
}

/* 页脚 */
.rpt__foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 15px;
  border-top: 1px dashed rgba(255, 255, 255, 0.08);
}

.rpt__code {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.rpt__acts {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.rpt__btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 18px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
  font-size: 12.5px;
  letter-spacing: 0.1em;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.24s, border-color 0.24s, background 0.24s, box-shadow 0.28s;
}

.rpt__btn:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
  background: rgba(216, 178, 90, 0.08);
}

.rpt__btn--primary {
  color: #1a1206;
  font-weight: 600;
  border-color: transparent;
  background: linear-gradient(180deg, var(--gold-lt) 0%, var(--gold) 52%, #b98f38 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5), 0 12px 30px -14px rgba(216, 178, 90, 0.8);
}

.rpt__btn--primary:hover:not(:disabled) {
  color: #1a1206;
  filter: brightness(1.06);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55), 0 16px 36px -14px rgba(216, 178, 90, 0.95),
    0 0 26px -6px rgba(216, 178, 90, 0.7);
}

.rpt__btn--primary:disabled {
  cursor: default;
  opacity: 0.72;
  box-shadow: none;
  color: rgba(26, 18, 6, 0.8);
}

/* 弹窗过渡 */
.rpt-enter-active,
.rpt-leave-active {
  transition: opacity 0.26s;
}

.rpt-enter-active .rpt__card {
  animation: card-in 0.36s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.rpt-enter-from,
.rpt-leave-to {
  opacity: 0;
}

/* ================= 响应式 ================= */
@media (max-width: 620px) {
  .appr__nav-inner {
    gap: 10px;
  }

  .appr__links {
    gap: 0;
  }

  .appr__link {
    padding: 7px 10px;
    font-size: 12px;
    letter-spacing: 0.04em;
  }

  .appr__brand-en {
    display: none;
  }

  .appr__title {
    font-size: 26px;
  }

  .appr__card {
    padding: 24px 20px 18px;
  }

  .rpt__card {
    padding: 26px 20px 20px;
  }

  .rpt__title {
    font-size: 18px;
  }

  .rpt__grid {
    grid-template-columns: 1fr;
  }

  .rpt__acts {
    width: 100%;
  }

  .rpt__btn {
    flex: 1;
    justify-content: center;
  }
}

/* 尊重系统的「减少动态效果」设置 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
