<script setup>
/**
 * 档案室（独立页面 /archive）
 * - 不在首页展示，入口在顶部导航栏「档案室」
 * - 内容按密级解锁：权限点由「执行部任务」页完成任务累积（utils/clearance.js）
 * - 四卷：龙王卷宗（资料+情报）/ 部门人物 / 龙族武器 / 任务执行记录
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import SiteNav from '@/components/SiteNav.vue'
import {
  LEVELS,
  exportClearanceCode,
  hasClearance,
  importClearanceCode,
  level,
  levelLabel,
  nextLevel,
  points,
  pointsTo,
  syncNow,
  syncState
} from '@/utils/clearance'
import {
  ARCHIVE_ARMS,
  ARCHIVE_DEPTS,
  ARCHIVE_KINGS,
  ARCHIVE_MISSION_LOGS,
  ARCHIVE_PEOPLE
} from '@/data/archive'
import { logPath } from '@/data/logDetail'
import { isIn, markIn, revealInView } from '@/utils/reveal'

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

/* ---------------- 存档状态 / 备份码 ---------------- */
const codeOpen = ref(false)
const code = ref('')
const codeInput = ref('')
const codeMsg = ref('')

const syncTone = computed(
  () =>
    ({
      ok: 'is-ok',
      offline: 'is-bad',
      syncing: 'is-busy',
      guest: 'is-idle'
    })[syncState.status] || 'is-idle'
)

const syncText = computed(() => {
  switch (syncState.status) {
    case 'ok':
      return '已同步到账号存档 · 换设备登录可恢复'
    case 'syncing':
      return '正在与账号存档同步…'
    case 'offline':
      return `同步失败，进度只在本机${syncState.error ? ` · ${syncState.error}` : ''}`
    case 'guest':
      return '未登录 · 进度只存在这台电脑'
    default:
      return '权限进度尚未同步'
  }
})

function retrySync() {
  syncNow()
}

function toggleCode() {
  codeOpen.value = !codeOpen.value
  if (codeOpen.value && !code.value) code.value = exportClearanceCode()
}

function doExport() {
  code.value = exportClearanceCode()
  codeMsg.value = '已生成 · 点一下文本框即可全选复制'
}

function doImport() {
  const r = importClearanceCode(codeInput.value)
  codeMsg.value = r.message
  if (r.ok) {
    codeInput.value = ''
    code.value = exportClearanceCode()
  }
}

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

/**
 * 支持 hash 直达某个卷宗：#tab-kings / #tab-people / #tab-arms / #tab-logs
 * 任务记录详情页的「返回」会带 #tab-logs 回来，方便用户接着看列表。
 * 同时兼容查询串写法（/archive?tab=people）：早期分享出去的链接长这样，
 * 不认 query 的话点进去会停在「龙王卷宗」，看起来像跳错了页。
 */
function tabFromHash() {
  const q = String(route.query?.tab || '')
  if (TABS.some((t) => t.id === q)) {
    tab.value = q
    return
  }
  const h = String(route.hash || '').replace(/^#/, '')
  const m = h.match(/^tab-(\w+)$/)
  if (m && TABS.some((t) => t.id === m[1])) tab.value = m[1]
}
tabFromHash()
watch([() => route.hash, () => route.query?.tab], tabFromHash)

/** 切换分卷时同步 hash（用 replace 避免污染浏览器历史） */
function selectTab(id) {
  if (tab.value === id) return
  tab.value = id
  router.replace({ path: route.path, hash: `#tab-${id}` })
}

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

/**
 * 整张卡片就是入口：已解锁 → 打开详情页；未解锁 → 抖一下并提示还差多少权限点。
 * （原来只有右下角那个小按钮能点，卡身看着可点却没反应。）
 */
function openLog(e, m) {
  if (hasClearance(m.req)) go(logPath(m))
  else lockHint(e, m.req)
}

/* ---------------- 卷宗入架动效 ----------------
   档案馆的隐喻是「一卷一卷从架上抽出」。切卷时让卡片依次入场，
   而不是整块同时淡入。观察器只盯当前这一卷，切入时重建，
   避免跨卷残留的旧观察目标。

   入场态一律走 utils/reveal 的 markIn（= data-rise 属性），不要用
   classList.add('is-in')：卡片模板上有 :class="{ 'is-locked': ... }" 这类动态
   class 绑定，Vue 重渲染会把整串 class 重写，手上的 is-in 被冲掉，
   .rise 的 opacity: 0 立刻生效 —— 「进档案室先 0 点全封存、同步到 34 点后
   密级翻转」就会把入场态全清掉，表现正是「部门人物只剩一两张卡，其余全空」。 */
const shelfEl = ref(null)
let shelfIO = null

function bindShelfObserver() {
  shelfIO?.disconnect()
  const root = shelfEl.value
  if (!root) return

  // 不支持 IntersectionObserver 的环境：直接全部入场，别让整页停在 opacity: 0
  if (typeof IntersectionObserver === 'undefined') {
    for (const el of root.querySelectorAll('.rise')) markIn(el)
    return
  }

  shelfIO = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        markIn(entry.target)
        shelfIO.unobserve(entry.target)
      }
    },
    { rootMargin: '0px 0px -8% 0px', threshold: 0.06 }
  )

  for (const el of root.querySelectorAll('.rise')) {
    // 已经入场的跳过，避免重挂观察器时重复播放入场动画
    if (isIn(el)) continue
    shelfIO.observe(el)
  }
}

/** 视口内的卡片立即入场（筛选 / 切卷重建出来的新节点没有进过观察器） */
function revealShelf() {
  revealInView(shelfEl.value)
}

/** 列表结构变化后：等 DOM 落地 → 重挂观察器 → 补首屏入场 */
function refreshShelf() {
  nextTick(() => {
    bindShelfObserver()
    requestAnimationFrame(revealShelf)
  })
}

// 切卷时 DOM 换了一整批卡片
watch(tab, refreshShelf)
// 按部门筛选会重建卡片（不补入场就是「点一次筛选，卡片全消失」）
watch(filteredPeople, refreshShelf)
// 权限点异步同步（0 → 34）会让 is-locked 整体翻转，同样是重渲染
watch([logs, lockedLogs], refreshShelf)

onMounted(() =>
  nextTick(() => {
    bindShelfObserver()
    revealShelf()
  })
)
onBeforeUnmount(() => shelfIO?.disconnect())
</script>

<template>
  <div class="arc">
    <RuneBackground />

    <!-- ============ 顶部导航：与首页同源 ============ -->
    <SiteNav />

    <!-- ============ 主体 ============ -->
    <main ref="shelfEl" class="arc__body">
      <section class="arc__hero">
        <div class="arc__hero-row">
          <DragonEmblem class="arc__hero-mark" />
          <div class="arc__hero-text">
            <h1 class="arc__title">
              <span class="sheen-gold">档案室</span>
              <span class="arc__title-en">ARCHIVE · DRAGON DOSSIER</span>
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

        <!-- 存档状态：让「权限为什么没了」有答案，而不是安静地归零 -->
        <div class="perm__sync">
          <i class="perm__dot" :class="syncTone" />
          <span class="perm__sync-txt">{{ syncText }}</span>
          <button
            v-if="syncState.status === 'offline'"
            type="button"
            class="perm__link"
            @click="retrySync"
          >
            重新同步
          </button>
          <button type="button" class="perm__link" @click="toggleCode">
            {{ codeOpen ? '收起备份码' : '备份码 · 换电脑用' }}
          </button>
        </div>

        <!-- 备份码：两台电脑各有一套本地库时，作为后端存档之外的搬运通道 -->
        <div v-if="codeOpen" class="perm__code">
          <div class="perm__code-row">
            <span>导出 · 复制整段，到另一台电脑导入</span>
            <button type="button" class="perm__mini" @click="doExport">生成</button>
          </div>
          <textarea
            class="perm__area"
            readonly
            :value="code"
            placeholder="点「生成」后这里出现备份码"
            @focus="$event.target.select()"
          />

          <div class="perm__code-row">
            <span>导入 · 粘贴另一台电脑的备份码</span>
            <button type="button" class="perm__mini" @click="doImport">导入</button>
          </div>
          <textarea
            v-model="codeInput"
            class="perm__area"
            placeholder="DRC1...."
            spellcheck="false"
          />
          <p v-if="codeMsg" class="perm__code-msg">{{ codeMsg }}</p>
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
          @click="selectTab(t.id)"
        >
          <AppIcon :name="t.icon" :size="14" /> {{ t.label }}
        </button>
      </div>

      <!-- ===== 卷一 · 龙王卷宗 ===== -->
      <section v-if="tab === 'kings'" class="shelf rise">
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="flame" :size="15" /> 龙王卷宗 · 资料与情报</h2>
          <span class="shelf__more">共 {{ ARCHIVE_KINGS.length }} 位 · 点击展开情报</span>
        </header>

        <div class="kgrid">
          <article
            v-for="(k, i) in ARCHIVE_KINGS"
            :key="k.code"
            class="kcard rise"
            :class="{ 'is-locked': !hasClearance(k.req) }"
            :style="{ '--d': `${(i % 6) * 55}ms` }"
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
      <section v-else-if="tab === 'people'" class="shelf rise">
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
            v-for="(p, i) in filteredPeople"
            :key="p.key"
            class="person rise"
            :data-tone="p.tone"
            :class="{ 'is-locked': !hasClearance(p.req), 'is-open': openKey === p.key }"
            :style="{ '--d': `${(i % 8) * 50}ms` }"
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
      <section v-else-if="tab === 'arms'" class="shelf rise">
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="sparkle" :size="15" /> 龙族武器情报</h2>
          <span class="shelf__more">共 {{ ARCHIVE_ARMS.length }} 件 · 按密级解锁</span>
        </header>

        <div class="agrid">
          <article
            v-for="(a, i) in ARCHIVE_ARMS"
            :key="a.code"
            class="acard rise"
            :class="{ 'is-locked': !hasClearance(a.req) }"
            :style="{ '--d': `${(i % 8) * 50}ms` }"
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
      <section v-else class="shelf rise">
        <header class="shelf__head">
          <h2 class="shelf__title"><AppIcon name="archive" :size="15" /> 任务执行记录</h2>
          <span class="shelf__more">
            已解锁 {{ logs.length }} 条 · 封存 {{ lockedLogs.length }} 条
          </span>
        </header>

        <div class="lgrid">
          <article
            v-for="(m, i) in ARCHIVE_MISSION_LOGS"
            :key="m.code + i"
            class="log rise"
            :class="{ 'log--open': hasClearance(m.req) }"
            :style="{ '--d': `${(i % 8) * 50}ms` }"
            role="button"
            tabindex="0"
            :aria-label="
              hasClearance(m.req)
                ? `查看任务详情：${m.code} ${m.name}`
                : `该记录已封存：${m.code} ${m.name}`
            "
            @click="openLog($event, m)"
            @keydown.enter.prevent="openLog($event, m)"
            @keydown.space.prevent="openLog($event, m)"
          >
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
              <span class="log__more">查看详情 <AppIcon name="arrow" :size="12" /></span>
            </template>
            <div v-else class="log__lock" @click.stop="lockHint($event, m.req)">
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

/* 存档状态行：一行小字交代「进度到底存在哪」 */
.perm__sync {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding-top: 11px;
  border-top: 1px dashed var(--line-soft);
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: var(--text-3);
}

.perm__dot {
  width: 6px;
  height: 6px;
  flex: none;
  border-radius: 50%;
  background: var(--text-4);
}

.perm__dot.is-ok {
  background: #6fbf88;
  box-shadow: 0 0 8px rgba(111, 191, 136, 0.55);
}

.perm__dot.is-bad {
  background: #cf6b5c;
  box-shadow: 0 0 8px rgba(207, 107, 92, 0.55);
}

.perm__dot.is-busy {
  background: var(--gold-lt);
  animation: perm-pulse 1.1s ease-in-out infinite;
}

@keyframes perm-pulse {
  50% {
    opacity: 0.28;
  }
}

.perm__sync-txt {
  margin-right: auto;
}

.perm__link {
  padding: 3px 9px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: transparent;
  font-family: inherit;
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.perm__link:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.07);
}

.perm__link:focus-visible {
  outline: none;
  border-color: var(--gold-dp);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.18);
}

/* 备份码面板 */
.perm__code {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  background: rgba(9, 7, 15, 0.6);
}

.perm__code-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: var(--text-3);
}

.perm__mini {
  flex: none;
  padding: 4px 11px;
  border: 1px solid rgba(216, 178, 90, 0.35);
  border-radius: 999px;
  background: rgba(216, 178, 90, 0.08);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--gold-lt);
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.perm__mini:hover {
  background: rgba(216, 178, 90, 0.16);
  border-color: rgba(216, 178, 90, 0.55);
}

.perm__area {
  width: 100%;
  min-height: 64px;
  max-height: 128px;
  padding: 8px 10px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.34);
  color: var(--text-2);
  font-family: var(--font-mono);
  font-size: 10.5px;
  line-height: 1.55;
  word-break: break-all;
  resize: vertical;
}

.perm__area:focus {
  outline: none;
  border-color: rgba(216, 178, 90, 0.45);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.12);
}

.perm__code-msg {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--gold-lt);
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
.log.is-shake,
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
  /* 整张卡片都是入口 */
  cursor: pointer;
  transition: border-color 0.24s ease, background 0.24s ease, box-shadow 0.24s ease;
}

/* 已解锁：悬停给出「可以进去」的信号。
   这里刻意不用 transform 抬升 —— 卡片带 .rise 入场动画，
   animation 的前景填充会压住 :hover 的 transform，抬升会失效。 */
.log--open:hover {
  border-color: rgba(216, 178, 90, 0.42);
  background: linear-gradient(165deg, rgba(41, 35, 58, 0.9), rgba(16, 13, 26, 0.96));
  box-shadow: 0 20px 44px -26px rgba(0, 0, 0, 0.95), 0 0 22px -16px var(--gold-glow);
}

.log:focus-visible {
  outline: none;
  border-color: var(--gold-dp);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.18);
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

/* 未解锁的卡片整块可点，提示还差多少密级 */
.log__lock {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-4);
}

/* 「查看详情」：卡片右下角的细边框指示片。
   真正的点击热区是整张卡片，所以这里是 span 而不是 button —— 避免按钮套按钮。 */
.log__more {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  padding: 5px 11px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  color: var(--text-3);
  transition: color 0.22s ease, border-color 0.22s ease, background 0.22s ease;
}

.log--open:hover .log__more,
.log:focus-visible .log__more {
  color: var(--gold-lt);
  border-color: var(--gold-dp);
  background: rgba(216, 178, 90, 0.08);
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
