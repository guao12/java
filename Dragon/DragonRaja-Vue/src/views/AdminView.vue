<script setup>
/**
 * 后台管理系统 · 言灵图鉴数据管理。
 * 对首页「言灵图鉴」区块与 /spirit/:key 详情页的数据做增删改查：
 *  - 列表：全部条目（含隐藏），key/名称/元素/密级/排序/状态
 *  - 新增 / 编辑：弹窗表单，key 唯一性由后端校验
 *  - 删除：二次确认（逻辑删除）
 * 数据接口见 src/api/admin.js，仅管理员令牌可访问。
 *
 * 加载速度相关的设计：
 *  - 侧边栏是独立组件，切菜单只换右侧面板，不重新挂载整页
 *  - 列表分页拉取（服务端分页优先，后端未实现时前端切片兜底），首屏只渲染 20 行
 *  - 请求层自带 GET 内存缓存 + 并发去重：重复进页 / 连点刷新不再重复往返
 *  - 搜索走 computed 过滤当前页，输入即时生效、不发请求
 *  - 装饰背景（Canvas 粒子）等首帧渲染完再挂载，不抢主线程
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AdminSidebar from '@/components/AdminSidebar.vue'
import AppIcon from '@/components/AppIcon.vue'
import GlowButton from '@/components/GlowButton.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import { adminApi } from '@/api/admin'
import { clearAdminSession, getAdminName } from '@/utils/adminAuth'

const router = useRouter()

/* ---------------- 会话 ---------------- */

const adminName = ref(getAdminName() || '管理员')

/* ---------------- 常量 ---------------- */

const LEVELS = ['内部', '机密', '高危', '绝密']
const ELEMENTS = ['火', '冰', '风', '重力', '时间', '精神', '暗影', '雷', '威压']

/* ---------------- 侧边栏菜单 ---------------- */

const MENU_KEY = 'dr:admin:menu'
const MENU_KEYS = ['spirits', 'overview']

/** 当前菜单：spirits 言灵图鉴 / overview 数据概览（记忆上次停留位置） */
const activeMenu = ref(
  MENU_KEYS.includes(localStorage.getItem(MENU_KEY)) ? localStorage.getItem(MENU_KEY) : 'spirits'
)

/** 桌面折叠态 */
const COLLAPSE_KEY = 'dr:admin:side-collapsed'
const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')
/** 移动端抽屉 */
const drawerOpen = ref(false)

const menuGroups = computed(() => [
  {
    title: '数据管理',
    items: [
      {
        key: 'spirits',
        label: '言灵图鉴',
        desc: '条目增删改查',
        icon: 'flame',
        badge: loaded.value ? total.value : ''
      },
      { key: 'overview', label: '数据概览', desc: '总量与密级分布', icon: 'chart' }
    ]
  }
])

function toggleCollapse() {
  collapsed.value = !collapsed.value
  localStorage.setItem(COLLAPSE_KEY, collapsed.value ? '1' : '0')
}

/** 菜单变化：持久化 + 概览页懒加载（概览依赖列表数据） */
watch(activeMenu, (v) => {
  localStorage.setItem(MENU_KEY, v)
  if (v === 'overview' && !loaded.value && !loading.value) load()
})

/* ---------------- 数据概览统计 ---------------- */

const stats = computed(() => {
  const byLevel = Object.fromEntries(LEVELS.map((l) => [l, 0]))
  for (const s of spirits.value) {
    if (byLevel[s.level] !== undefined) byLevel[s.level] += 1
  }
  return {
    total: total.value,
    onShelf: spirits.value.filter((s) => s.status === 1).length,
    hidden: spirits.value.filter((s) => s.status !== 1).length,
    byLevel,
    withUrl: spirits.value.filter((s) => s.imageUrl).length
  }
})

/* ---------------- 列表 ---------------- */

const spirits = ref([])
const total = ref(0)
const loading = ref(false)
const loaded = ref(false)
const listError = ref('')
const keyword = ref('')

/** 分页 */
const PAGE_SIZE = 20
const page = ref(1)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))
const pageNumbers = computed(() => {
  const n = pageCount.value
  const cur = page.value
  if (n <= 7) return Array.from({ length: n }, (_, i) => i + 1)
  // 首尾常显，中间以当前页为中心展开
  const set = new Set([1, n, cur, cur - 1, cur + 1])
  if (cur <= 3) [2, 3, 4].forEach((i) => set.add(i))
  if (cur >= n - 2) [n - 1, n - 2, n - 3].forEach((i) => set.add(i))
  const nums = [...set].filter((i) => i >= 1 && i <= n).sort((a, b) => a - b)
  const out = []
  for (let i = 0; i < nums.length; i++) {
    if (i && nums[i] - nums[i - 1] > 1) out.push('…')
    out.push(nums[i])
  }
  return out
})

/** 关键词过滤当前页（不发请求，输入即过滤） */
const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return spirits.value
  return spirits.value.filter(
    (s) =>
      s.key.toLowerCase().includes(k) ||
      s.name.toLowerCase().includes(k) ||
      s.element.toLowerCase().includes(k) ||
      s.level.toLowerCase().includes(k)
  )
})

/**
 * 拉取列表。
 * @param {{ fresh?: boolean, keepData?: boolean }} [opts]
 *   fresh：跳过请求层缓存（手动刷新）
 *   keepData：翻页 / 刷新时保留旧数据，避免表格闪白
 */
async function load(opts = {}) {
  const { fresh = false, keepData = false } = opts
  loading.value = true
  listError.value = ''
  if (!keepData) spirits.value = []
  try {
    const res = await adminApi.listSpirits({
      page: page.value,
      size: PAGE_SIZE,
      keyword: keyword.value,
      fresh
    })
    spirits.value = res.list || []
    total.value = res.total || 0
    loaded.value = true
  } catch (e) {
    listError.value = e.message
    if (e.code === 401) router.replace('/admin/login')
  } finally {
    loading.value = false
  }
}

/** 手动刷新：强制绕过缓存 */
function refresh() {
  load({ fresh: true, keepData: true })
}

function goPage(p) {
  const next = Math.min(pageCount.value, Math.max(1, p))
  if (next === page.value) return
  page.value = next
  load({ keepData: true })
}

/** 搜索只在当前页内过滤，换关键词时回到第一页 */
watch(keyword, () => {
  page.value = 1
})

/* ---------------- 新增 / 编辑弹窗 ---------------- */

const showModal = ref(false)
const editingId = ref(null) // null = 新增
const saving = ref(false)
const modalError = ref('')

const emptyForm = () => ({
  key: '',
  name: '',
  element: '',
  level: '内部',
  effect: '',
  desc: '',
  holder: '',
  note: '',
  imageUrl: '',
  sort: 0,
  status: 1
})

const form = reactive(emptyForm())

/** 弹窗里网址图片预览加载失败标记（换网址时自动重置） */
const modalImgBroken = ref(false)
watch(
  () => form.imageUrl,
  () => {
    modalImgBroken.value = false
  }
)

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalError.value = ''
  showModal.value = true
}

function openEdit(s) {
  editingId.value = s.id
  Object.assign(form, {
    key: s.key,
    name: s.name,
    element: s.element,
    level: s.level,
    effect: s.effect,
    desc: s.desc,
    holder: s.holder || '',
    note: s.note || '',
    imageUrl: s.imageUrl || '',
    sort: s.sort ?? 0,
    status: s.status ?? 1
  })
  modalError.value = ''
  showModal.value = true
}

/** 插画来源：图片网址优先，其次本地 /img/spirit/<key>.webp */
function spiritImg(s) {
  return s.imageUrl || `/img/spirit/${s.key}.webp`
}

/**
 * 插画加载失败时隐藏 <img>，用首字占位。
 * 用 ref 包 Set：reactive(Set) 不代理集合方法，.add 会因 undefined 报错。
 * 每次替换成新 Set 触发更新，条目数量级很小，成本可忽略。
 */
const brokenImgs = ref(new Set())
function onThumbError(s) {
  if (brokenImgs.value.has(s.key)) return
  brokenImgs.value = new Set(brokenImgs.value).add(s.key)
}

function checkForm() {
  if (!/^[a-z0-9][a-z0-9-]{0,49}$/.test(form.key)) return 'key 只能是小写字母、数字和连字符（如 junyan）'
  if (!form.name.trim()) return '请填写言灵名'
  if (!form.element.trim()) return '请填写元素系别'
  if (!form.effect.trim()) return '请填写言灵效果'
  if (!form.desc.trim()) return '请填写详细介绍'
  const url = form.imageUrl.trim()
  if (url && !/^https?:\/\/\S+$/.test(url)) return '图片网址必须以 http:// 或 https:// 开头，且不能含空格'
  return ''
}

async function onSave() {
  modalError.value = checkForm()
  if (modalError.value || saving.value) return
  saving.value = true
  try {
    const payload = {
      key: form.key.trim(),
      name: form.name.trim(),
      element: form.element.trim(),
      level: form.level,
      effect: form.effect.trim(),
      desc: form.desc.trim(),
      holder: form.holder.trim(),
      note: form.note.trim(),
      imageUrl: form.imageUrl.trim(),
      sort: Number(form.sort) || 0,
      status: Number(form.status) || 1
    }
    if (editingId.value == null) await adminApi.createSpirit(payload)
    else await adminApi.updateSpirit(editingId.value, payload)
    showModal.value = false
    // 写操作已让缓存失效，这里再强制取最新一页
    await load({ fresh: true, keepData: true })
  } catch (e) {
    modalError.value = e.message
    if (e.code === 401) {
      showModal.value = false
      router.replace('/admin/login')
    }
  } finally {
    saving.value = false
  }
}

/* ---------------- 删除 ---------------- */

const confirmTarget = ref(null) // 待删除条目
const deleting = ref(false)

function askDelete(s) {
  confirmTarget.value = s
}

async function onDelete() {
  if (!confirmTarget.value || deleting.value) return
  deleting.value = true
  try {
    await adminApi.deleteSpirit(confirmTarget.value.id)
    confirmTarget.value = null
    // 删掉本页最后一条时回退一页，避免停在空页
    if (spirits.value.length === 1 && page.value > 1) page.value -= 1
    await load({ fresh: true, keepData: true })
  } catch (e) {
    listError.value = e.message
    confirmTarget.value = null
    if (e.code === 401) router.replace('/admin/login')
  } finally {
    deleting.value = false
  }
}

/* ---------------- 退出 ---------------- */

function logout() {
  clearAdminSession()
  router.replace('/admin/login')
}

/* ---------------- 首屏挂载 / 快捷键 ---------------- */

/**
 * 背景层（Canvas 粒子）延后挂载：
 * 先渲染顶栏 + 表格骨架，等首帧画完再挂背景，避免和列表渲染抢主线程。
 */
const bgReady = ref(false)
let idleTimer = 0

/** 数字键 1/2 切菜单，Esc 关弹窗 / 抽屉 */
function onKeydown(e) {
  if (e.key === 'Escape') {
    if (showModal.value) showModal.value = false
    else if (confirmTarget.value) confirmTarget.value = null
    else if (drawerOpen.value) drawerOpen.value = false
    return
  }
  const tag = (e.target?.tagName || '').toLowerCase()
  if (tag === 'input' || tag === 'textarea' || tag === 'select' || e.target?.isContentEditable) return
  if (e.metaKey || e.ctrlKey || e.altKey) return
  const idx = Number(e.key) - 1
  if (idx >= 0 && idx < MENU_KEYS.length) activeMenu.value = MENU_KEYS[idx]
}

onMounted(() => {
  document.documentElement.style.colorScheme = 'dark'
  document.title = '后台管理系统 | 卡塞尔学院'
  window.addEventListener('keydown', onKeydown)
  // 数据请求与背景层并行：请求先发；背景等首帧渲染完再挂
  load()
  const mountBg = () => {
    bgReady.value = true
  }
  if ('requestIdleCallback' in window) {
    idleTimer = window.requestIdleCallback(mountBg, { timeout: 900 })
  } else {
    idleTimer = window.setTimeout(mountBg, 200)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  if ('cancelIdleCallback' in window) window.cancelIdleCallback(idleTimer)
  else window.clearTimeout(idleTimer)
})
</script>

<template>
  <!-- 移动端抽屉遮罩：必须放在 .adm 外，否则会被 .adm 的 z-index 困住 -->
  <div
    v-if="drawerOpen"
    class="adm__scrim"
    aria-hidden="true"
    @click="drawerOpen = false"
  />

  <div class="adm">
    <!-- 装饰背景延后一帧挂载，先让骨架和顶栏出来，避免首屏被粒子拖慢 -->
    <RuneBackground v-if="bgReady" />

    <!-- ============ 顶栏 ============ -->
    <header class="adm__bar">
      <div class="adm__bar-inner">
        <div class="adm__bar-left">
          <!-- 移动端抽屉开关 -->
          <button
            type="button"
            class="adm__burger"
            aria-label="打开导航"
            @click="drawerOpen = true"
          >
            <AppIcon name="menu" :size="18" />
          </button>
          <div class="adm__title">
            <AppIcon name="shield" :size="18" />
            <div>
              <h1>后台管理系统</h1>
              <p class="adm__crumb">
                数据管理 <i>/</i>
                <b>{{ activeMenu === 'overview' ? '数据概览' : '言灵图鉴' }}</b>
              </p>
            </div>
          </div>
        </div>

        <div class="adm__bar-side">
          <span class="adm__sync" :class="{ 'is-busy': loading }">
            <i class="adm__who-dot" />
            {{ loading ? '同步中…' : `共 ${total} 条` }}
          </span>
          <span class="adm__kbd" title="快捷键：1 言灵图鉴 / 2 数据概览">1 / 2</span>
        </div>
      </div>
    </header>

    <!-- ============ 侧边栏 + 主体 ============ -->
    <div class="adm__layout" :class="{ 'is-collapsed': collapsed }">
      <AdminSidebar
        v-model="activeMenu"
        :groups="menuGroups"
        :admin-name="adminName"
        :collapsed="collapsed"
        :open="drawerOpen"
        @toggle-collapse="toggleCollapse"
        @close="drawerOpen = false"
        @logout="logout"
      />

      <main class="adm__body">
        <!-- ============ 菜单：言灵图鉴 ============ -->
        <template v-if="activeMenu === 'spirits'">
      <!-- 工具条 -->
      <div class="adm__tools">
        <label class="adm__search">
          <AppIcon name="search" :size="14" />
          <input v-model="keyword" type="text" placeholder="按 key / 名称 / 元素 / 密级过滤本页" />
          <button
            v-if="keyword"
            type="button"
            class="adm__search-x"
            aria-label="清空筛选"
            @click.prevent="keyword = ''"
          >
            <AppIcon name="close" :size="12" />
          </button>
        </label>
        <div class="adm__tools-side">
          <span class="adm__count">
            共 {{ total }} 条 / 本页 {{ filtered.length }} 条
          </span>
          <GlowButton variant="ghost" type="button" :disabled="loading" @click="refresh">
            <AppIcon name="refresh" :size="15" :class="{ 'is-spin': loading }" />
            刷新
          </GlowButton>
          <GlowButton type="button" @click="openCreate">
            <AppIcon name="plus" :size="15" /> 新增言灵
          </GlowButton>
        </div>
      </div>

      <div v-if="listError" class="adm__alert">
        <AppIcon name="alert" :size="15" />
        <span>{{ listError }}</span>
      </div>

      <!-- 数据表 -->
      <div class="adm__table-wrap">
        <table class="adm__table">
          <thead>
            <tr>
              <th>排序</th>
              <th>插画</th>
              <th>插画 Key</th>
              <th>言灵名</th>
              <th>元素</th>
              <th>密级</th>
              <th>言灵效果</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in filtered" :key="s.id">
              <td class="num">{{ s.sort }}</td>
              <td>
                <span v-if="brokenImgs.has(s.key)" class="thumb thumb--fallback">{{ s.name.slice(0, 1) }}</span>
                <img
                  v-else
                  class="thumb"
                  :src="spiritImg(s)"
                  :alt="`插画 · ${s.name}`"
                  loading="lazy"
                  :title="s.imageUrl ? '图片来源：网址' : '图片来源：本地文件'"
                  @error="onThumbError(s)"
                />
              </td>
              <td>
                <code class="key">{{ s.key }}</code>
                <em v-if="s.imageUrl" class="key-url" title="使用图片网址">网址</em>
              </td>
              <td class="name">{{ s.name }}</td>
              <td>{{ s.element }}</td>
              <td>
                <span class="lv" :data-lv="s.level">{{ s.level }}</span>
              </td>
              <td class="effect">{{ s.effect }}</td>
              <td>
                <span class="st" :class="s.status === 1 ? 'is-on' : 'is-off'">
                  {{ s.status === 1 ? '展示中' : '已隐藏' }}
                </span>
              </td>
              <td class="ops">
                <button type="button" class="op" @click="openEdit(s)">编辑</button>
                <button type="button" class="op is-danger" @click="askDelete(s)">删除</button>
              </td>
            </tr>
            <tr v-if="!loading && !filtered.length">
              <td colspan="9" class="empty">
                {{
                  listError
                    ? '加载失败，请刷新重试'
                    : keyword
                      ? '本页没有匹配「' + keyword + '」的条目'
                      : '暂无数据，点击右上角「新增言灵」录入'
                }}
              </td>
            </tr>
            <!-- 骨架屏：首屏只有一屏占位，比转圈更早给出结构感 -->
            <template v-if="loading && !spirits.length">
              <tr v-for="i in 6" :key="`sk-${i}`" class="is-skel">
                <td><span class="sk sk--xs" /></td>
                <td><span class="sk sk--thumb" /></td>
                <td><span class="sk sk--sm" /></td>
                <td><span class="sk sk--md" /></td>
                <td><span class="sk sk--xs" /></td>
                <td><span class="sk sk--sm" /></td>
                <td><span class="sk sk--lg" /></td>
                <td><span class="sk sk--sm" /></td>
                <td><span class="sk sk--md" /></td>
              </tr>
            </template>
          </tbody>
        </table>
        <div v-if="loading && spirits.length" class="adm__loading-bar" />
      </div>

      <!-- 分页 -->
      <nav v-if="pageCount > 1" class="adm__pager" aria-label="分页">
        <button type="button" class="pg" :disabled="page <= 1 || loading" @click="goPage(page - 1)">
          <AppIcon name="chevron-left" :size="13" /> 上一页
        </button>
        <template v-for="(p, i) in pageNumbers" :key="`p-${i}`">
          <span v-if="p === '…'" class="pg__gap">…</span>
          <button
            v-else
            type="button"
            class="pg pg--num"
            :class="{ 'is-active': p === page }"
            :disabled="loading"
            @click="goPage(p)"
          >
            {{ p }}
          </button>
        </template>
        <button
          type="button"
          class="pg"
          :disabled="page >= pageCount || loading"
          @click="goPage(page + 1)"
        >
          下一页 <AppIcon name="chevron-right" :size="13" />
        </button>
      </nav>

      <p class="adm__tip">
        <AppIcon name="alert" :size="13" />
        插画默认按 <code>/img/spirit/&lt;key&gt;.webp</code> 放置；填写「图片网址」后前台将优先加载该网址图片。「隐藏」的条目前台不再展示，数据保留。
      </p>
        </template>

        <!-- ============ 菜单：数据概览 ============ -->
        <template v-else>
          <div class="ov__grid">
            <div class="ov__card">
              <span class="ov__num">{{ stats.total }}</span>
              <span class="ov__label">条目总数</span>
            </div>
            <div class="ov__card is-gold">
              <span class="ov__num">{{ stats.onShelf }}</span>
              <span class="ov__label">展示中</span>
            </div>
            <div class="ov__card is-dim">
              <span class="ov__num">{{ stats.hidden }}</span>
              <span class="ov__label">已隐藏</span>
            </div>
            <div class="ov__card is-gold">
              <span class="ov__num">{{ stats.withUrl }}</span>
              <span class="ov__label">使用图片网址</span>
            </div>
          </div>

          <div class="ov__panel">
            <header class="ov__head">
              <h3><AppIcon name="info" :size="14" /> 密级分布</h3>
              <span class="ov__head-note">按言灵密级统计条目数</span>
            </header>
            <div class="ov__rows">
              <div v-for="l in LEVELS" :key="l" class="ov__row">
                <span class="lv" :data-lv="l">{{ l }}</span>
                <div class="ov__bar">
                  <i
                    class="ov__bar-fill"
                    :style="{ width: `${stats.total ? Math.round((stats.byLevel[l] / stats.total) * 100) : 0}%` }"
                  />
                </div>
                <b class="ov__bar-num">{{ stats.byLevel[l] }}</b>
              </div>
            </div>
          </div>

          <p class="adm__tip">
            <AppIcon name="info" :size="13" />
            数据实时来自「言灵图鉴」列表；在图鉴菜单中增删改后回到本页即可看到最新统计。
          </p>
        </template>
      </main>
    </div>

    <!-- ============ 新增 / 编辑弹窗 ============ -->
    <div v-if="showModal" class="adm__mask" @click.self="showModal = false">
      <div class="adm__modal">
        <header class="adm__modal-head">
          <h2>{{ editingId == null ? '新增言灵' : `编辑 · ${form.name}` }}</h2>
          <button type="button" class="adm__modal-x" @click="showModal = false">
            <AppIcon name="close" :size="16" />
          </button>
        </header>

        <div class="adm__modal-body">
          <div v-if="modalError" class="adm__alert">
            <AppIcon name="alert" :size="15" />
            <span>{{ modalError }}</span>
          </div>

          <div class="grid-2">
            <label class="fld">
              <span>插画 Key <em>* 路由与图片地址，新增后不建议修改</em></span>
              <input v-model.trim="form.key" type="text" placeholder="如 junyan" />
            </label>
            <label class="fld">
              <span>言灵名 *</span>
              <input v-model.trim="form.name" type="text" placeholder="如 君焰" />
            </label>
            <label class="fld">
              <span>元素系别 *</span>
              <input v-model.trim="form.element" list="adm-elements" placeholder="如 火 / 冰 / 风暴" />
              <datalist id="adm-elements">
                <option v-for="e in ELEMENTS" :key="e" :value="e" />
              </datalist>
            </label>
            <label class="fld">
              <span>密级 *</span>
              <select v-model="form.level">
                <option v-for="l in LEVELS" :key="l" :value="l">{{ l }}</option>
              </select>
            </label>
          </div>

          <label class="fld">
            <span>言灵效果 *（一句话，列表与详情页标题）</span>
            <input v-model.trim="form.effect" type="text" maxlength="100" placeholder="如 领域内可燃物瞬间爆燃" />
          </label>

          <label class="fld">
            <span>详细介绍 *</span>
            <textarea v-model.trim="form.desc" rows="4" maxlength="2000" placeholder="展示在首页卡片与详情页的介绍文字" />
          </label>

          <label class="fld">
            <span>典型持有者</span>
            <input v-model.trim="form.holder" type="text" maxlength="100" placeholder="如 攻坚型混血种 · 多见于战斗编制" />
          </label>

          <label class="fld">
            <span>使用禁忌 / 备注</span>
            <textarea v-model.trim="form.note" rows="2" maxlength="200" placeholder="如 持续释放会灼伤自身血脉" />
          </label>

          <div class="imgurl">
            <label class="fld">
              <span>插画图片网址 <em>* 可选，填写后前台优先加载该网址图片，留空则用本地 /img/spirit/&lt;key&gt;.webp</em></span>
              <input
                v-model.trim="form.imageUrl"
                type="text"
                maxlength="500"
                inputmode="url"
                placeholder="https://example.com/spirit.jpg"
              />
            </label>
            <div class="imgurl__preview" :class="{ 'is-empty': !form.imageUrl.trim() || modalImgBroken }">
              <img
                v-if="form.imageUrl.trim() && !modalImgBroken"
                :src="form.imageUrl"
                alt="插画预览"
                @error="modalImgBroken = true"
              />
              <span v-else-if="form.imageUrl.trim()" class="imgurl__empty">图片加载失败，请检查网址</span>
              <span v-else class="imgurl__empty">网址预览</span>
            </div>
          </div>

          <div class="grid-2">
            <label class="fld">
              <span>排序（越小越靠前）</span>
              <input v-model.number="form.sort" type="number" min="0" max="9999" />
            </label>
            <label class="fld">
              <span>状态</span>
              <select v-model.number="form.status">
                <option :value="1">展示中（前台可见）</option>
                <option :value="0">隐藏（前台不可见）</option>
              </select>
            </label>
          </div>
        </div>

        <footer class="adm__modal-foot">
          <GlowButton variant="ghost" type="button" @click="showModal = false">取消</GlowButton>
          <GlowButton type="button" :disabled="saving" @click="onSave">
            {{ saving ? '保存中…' : '保存' }}
          </GlowButton>
        </footer>
      </div>
    </div>

    <!-- ============ 删除确认 ============ -->
    <div v-if="confirmTarget" class="adm__mask" @click.self="confirmTarget = null">
      <div class="adm__modal adm__modal--sm">
        <header class="adm__modal-head">
          <h2>删除确认</h2>
          <button type="button" class="adm__modal-x" @click="confirmTarget = null">
            <AppIcon name="close" :size="16" />
          </button>
        </header>
        <p class="adm__confirm-txt">
          确定要删除言灵「<b>{{ confirmTarget.name }}</b>（key: {{ confirmTarget.key }}）」吗？
          删除后首页图鉴与详情页将不再展示该条目。
        </p>
        <footer class="adm__modal-foot">
          <GlowButton variant="ghost" type="button" @click="confirmTarget = null">取消</GlowButton>
          <GlowButton type="button" class="is-danger" :disabled="deleting" @click="onDelete">
            {{ deleting ? '删除中…' : '确认删除' }}
          </GlowButton>
        </footer>
      </div>
    </div>
  </div>
</template>

<style scoped>
.adm {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ================= 顶栏 ================= */
.adm__bar {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(5, 7, 13, 0.82);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom: 1px solid var(--line);
}

.adm__bar-inner {
  max-width: 1320px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 11px 24px;
}

.adm__bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.adm__burger {
  display: none;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.5);
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.adm__burger:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.45);
}

.adm__crumb {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  text-transform: uppercase;
  display: flex;
  align-items: center;
  gap: 5px;
}

.adm__crumb i {
  font-style: normal;
  opacity: 0.5;
}

.adm__crumb b {
  color: var(--gold);
  font-weight: 500;
}

.adm__sync {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 12px;
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--text-3);
  border: 1px solid var(--line-soft);
  white-space: nowrap;
}

.adm__sync .adm__who-dot {
  transition: background 0.2s;
}

.adm__sync.is-busy {
  color: var(--gold-lt);
  border-color: rgba(138, 106, 40, 0.5);
}

.adm__sync.is-busy .adm__who-dot {
  animation: adm-pulse 1s ease-in-out infinite;
}

.adm__kbd {
  display: inline-grid;
  place-items: center;
  min-width: 40px;
  padding: 4px 8px;
  border-radius: 6px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-4);
  border: 1px dashed var(--line-soft);
}

@keyframes adm-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.45;
    transform: scale(0.72);
  }
}

.adm__title {
  display: flex;
  align-items: center;
  gap: 11px;
  color: var(--gold-lt);
}

.adm__title h1 {
  font-family: var(--font-serif);
  font-size: 17px;
  letter-spacing: 0.16em;
  color: var(--text);
  white-space: nowrap;
}

.adm__title p {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.22em;
  color: var(--text-4);
  text-transform: uppercase;
}

.adm__bar-side {
  display: flex;
  align-items: center;
  gap: 10px;
}

.adm__who {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--text-2);
}

.adm__who-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--gold-lt);
  box-shadow: 0 0 8px var(--gold-glow);
}

/* ================= 侧边栏 + 主体布局 ================= */
.adm__layout {
  flex: 1;
  display: flex;
  align-items: flex-start;
  gap: 0;
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  padding: 18px 24px 46px;
}

/* 移动端抽屉遮罩 */
.adm__scrim {
  display: none;
}

@media (max-width: 920px) {
  .adm__scrim {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 66;
    background: rgba(3, 4, 8, 0.78);
    animation: fade-in 0.22s both;
  }
}

/* ================= 主体 ================= */
.adm__body {
  flex: 1;
  min-width: 0;
  padding-left: 26px;
}

.adm__tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.adm__search {
  flex: 1;
  min-width: 220px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 15px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.55);
  color: var(--text-4);
  transition: border-color 0.2s;
}

.adm__search:focus-within {
  border-color: rgba(216, 178, 90, 0.4);
}

.adm__search-x {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 0;
  background: rgba(255, 255, 255, 0.07);
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.18s, background 0.18s;
}

.adm__search-x:hover {
  color: var(--text);
  background: rgba(255, 255, 255, 0.14);
}

.adm__search input {
  flex: 1;
  min-width: 0;
  border: 0;
  background: none;
  font: inherit;
  font-size: 13px;
  color: var(--text);
  outline: none;
}

/* 表格骨架屏 */
.sk {
  display: block;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.05) 25%,
    rgba(255, 255, 255, 0.11) 37%,
    rgba(255, 255, 255, 0.05) 63%
  );
  background-size: 300% 100%;
  animation: sk-shimmer 1.3s ease-in-out infinite;
}

.sk--xs {
  width: 24px;
}

.sk--sm {
  width: 56px;
}

.sk--md {
  width: 92px;
}

.sk--lg {
  width: 170px;
}

.sk--thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
}

.adm__table tr.is-skel td {
  border-bottom-color: rgba(255, 255, 255, 0.03);
}

@keyframes sk-shimmer {
  from {
    background-position: 130% 0;
  }
  to {
    background-position: -30% 0;
  }
}

/* 翻页时顶部细进度条 */
.adm__loading-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  overflow: hidden;
  background: rgba(216, 178, 90, 0.12);
}

.adm__loading-bar::after {
  content: '';
  position: absolute;
  inset: 0;
  width: 40%;
  background: linear-gradient(90deg, transparent, var(--gold-lt), transparent);
  animation: adm-loading 1.1s ease-in-out infinite;
}

@keyframes adm-loading {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(350%);
  }
}

/* 分页 */
.adm__pager {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.pg {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 32px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.45);
  font: inherit;
  font-size: 12px;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.18s, border-color 0.18s, background 0.18s;
}

.pg--num {
  justify-content: center;
  min-width: 32px;
  padding: 5px 0;
  font-family: var(--font-mono);
}

.pg:hover:not(:disabled) {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.45);
}

.pg.is-active {
  color: var(--bg-0);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold));
  border-color: transparent;
  font-weight: 600;
}

.pg:disabled {
  opacity: 0.38;
  cursor: not-allowed;
}

.pg__gap {
  padding: 0 2px;
  color: var(--text-4);
  font-family: var(--font-mono);
  font-size: 12px;
}


.adm__tools-side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* 工具条按钮按内容取宽，不跟着全局 .btn 的 100% 撑满 */
.adm__tools-side :deep(.btn) {
  width: auto;
  flex: 0 0 auto;
  padding: 8px 16px;
  font-size: 12.5px;
  gap: 7px;
}

.adm__count {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--text-4);
  margin-right: 2px;
}

.adm__alert {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px 14px;
  margin-bottom: 14px;
  border-radius: var(--r-md);
  font-size: 12.5px;
  color: var(--blood-lt);
  border: 1px solid rgba(179, 39, 45, 0.45);
  background: rgba(179, 39, 45, 0.1);
}

/* ================= 数据表 ================= */
.adm__table-wrap {
  position: relative;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: linear-gradient(165deg, rgba(26, 21, 38, 0.72), rgba(11, 9, 18, 0.9));
  box-shadow: var(--shadow-card);
  overflow: auto;
}

.adm__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.adm__table th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 12px 14px;
  text-align: left;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: var(--gold-lt);
  background: rgba(14, 11, 23, 0.96);
  border-bottom: 1px solid var(--line);
}

.adm__table td {
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: var(--text-2);
  vertical-align: middle;
}

.adm__table tbody tr {
  transition: background 0.18s;
}

.adm__table tbody tr:hover {
  background: rgba(216, 178, 90, 0.05);
}

.adm__table .num {
  font-family: var(--font-mono);
  color: var(--text-4);
}

.adm__table .key {
  font-family: var(--font-mono);
  font-size: 11.5px;
  padding: 2px 8px;
  border-radius: 5px;
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.09);
  border: 1px solid rgba(138, 106, 40, 0.4);
}

.key-url {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 10px;
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 4px;
  color: var(--gold-lt);
  border: 1px dashed rgba(216, 178, 90, 0.5);
  letter-spacing: 0.08em;
}

.thumb {
  display: block;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid rgba(138, 106, 40, 0.45);
  background: rgba(4, 7, 13, 0.6);
}

.thumb--fallback {
  display: grid;
  place-items: center;
  font-family: var(--font-serif);
  font-size: 16px;
  color: var(--text-3);
}

.adm__table .name {
  font-family: var(--font-serif);
  font-size: 14.5px;
  letter-spacing: 0.08em;
  color: var(--text);
  white-space: nowrap;
}

.adm__table .effect {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lv {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--text-3);
  border: 1px solid var(--line);
}

.lv[data-lv='高危'] {
  color: var(--blood-lt);
  border-color: rgba(179, 39, 45, 0.5);
  background: rgba(179, 39, 45, 0.1);
}

.lv[data-lv='机密'] {
  color: var(--gold-lt);
  border-color: rgba(138, 106, 40, 0.7);
  background: rgba(216, 178, 90, 0.1);
}

.lv[data-lv='绝密'] {
  color: #f8ecc4;
  border-color: rgba(216, 178, 90, 0.65);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.3), rgba(138, 106, 40, 0.3));
}

.st {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  white-space: nowrap;
}

.st::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.st.is-on {
  color: var(--gold-lt);
}

.st.is-on::before {
  background: var(--gold-lt);
  box-shadow: 0 0 7px var(--gold-glow);
}

.st.is-off {
  color: var(--text-4);
}

.st.is-off::before {
  background: var(--text-4);
}

.adm__table .ops {
  white-space: nowrap;
  text-align: right;
}

.op {
  padding: 5px 13px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: none;
  font: inherit;
  font-size: 12px;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.op:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
}

.op.is-danger:hover {
  color: var(--blood-lt);
  border-color: rgba(179, 39, 45, 0.55);
}

.adm__table .empty {
  padding: 46px 14px;
  text-align: center;
  color: var(--text-4);
  letter-spacing: 0.08em;
}

.adm__loading {
  padding: 40px;
  text-align: center;
  font-size: 12.5px;
  color: var(--text-4);
  letter-spacing: 0.1em;
}

.adm__tip {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 14px;
  font-size: 11.5px;
  letter-spacing: 0.05em;
  color: var(--text-4);
}

.adm__tip code {
  font-family: var(--font-mono);
  color: var(--gold);
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(216, 178, 90, 0.08);
}

/* ================= 图片网址表单 ================= */
.imgurl {
  display: grid;
  grid-template-columns: 1fr 96px;
  gap: 12px;
  align-items: start;
}

.imgurl__preview {
  width: 96px;
  height: 96px;
  border-radius: var(--r-md);
  border: 1px dashed rgba(216, 178, 90, 0.4);
  background: rgba(4, 7, 13, 0.55);
  overflow: hidden;
  display: grid;
  place-items: center;
}

.imgurl__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.imgurl__empty {
  font-size: 10.5px;
  color: var(--text-4);
  letter-spacing: 0.06em;
  text-align: center;
  padding: 0 8px;
  line-height: 1.6;
}

/* ================= 数据概览 ================= */
.ov__grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 13px;
  margin-bottom: 16px;
}

.ov__card {
  display: grid;
  gap: 4px;
  padding: 18px 16px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: linear-gradient(165deg, rgba(26, 21, 38, 0.72), rgba(11, 9, 18, 0.9));
  box-shadow: var(--shadow-card);
}

.ov__card.is-gold {
  border-color: rgba(138, 106, 40, 0.55);
}

.ov__card.is-dim {
  opacity: 0.75;
}

.ov__num {
  font-family: var(--font-mono);
  font-size: 26px;
  color: var(--gold-lt);
  line-height: 1.1;
}

.ov__label {
  font-size: 11.5px;
  letter-spacing: 0.14em;
  color: var(--text-3);
}

.ov__panel {
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: linear-gradient(165deg, rgba(26, 21, 38, 0.72), rgba(11, 9, 18, 0.9));
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.ov__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line-soft);
}

.ov__head h3 {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 0.12em;
  color: var(--gold-lt);
}

.ov__head-note {
  font-size: 11px;
  color: var(--text-4);
  letter-spacing: 0.06em;
}

.ov__rows {
  display: grid;
  gap: 13px;
  padding: 18px;
}

.ov__row {
  display: grid;
  grid-template-columns: 64px 1fr 40px;
  align-items: center;
  gap: 14px;
}

.ov__bar {
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.ov__bar-fill {
  display: block;
  height: 100%;
  min-width: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(138, 106, 40, 0.9), var(--gold-lt));
  box-shadow: 0 0 8px rgba(216, 178, 90, 0.35);
  transition: width 0.4s ease;
}

.ov__bar-num {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-2);
  text-align: right;
}

/* ================= 弹窗 ================= */
.adm__mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(3, 4, 8, 0.72);
  backdrop-filter: blur(6px);
  animation: fade-in 0.2s both;
}

.adm__modal {
  width: min(720px, 100%);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: linear-gradient(165deg, rgba(28, 24, 42, 0.97), rgba(10, 8, 17, 0.98));
  box-shadow: var(--shadow-card), 0 0 40px -18px var(--gold-glow);
  animation: card-in 0.35s cubic-bezier(0.16, 1, 0.3, 1) both;
  overflow: hidden;
}

.adm__modal--sm {
  width: min(440px, 100%);
}

.adm__modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line-soft);
}

.adm__modal-head h2 {
  font-family: var(--font-serif);
  font-size: 15.5px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
}

.adm__modal-x {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid var(--line-soft);
  background: none;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.adm__modal-x:hover {
  color: var(--text);
  border-color: rgba(216, 178, 90, 0.45);
}

.adm__modal-body {
  padding: 18px 20px;
  display: grid;
  gap: 13px;
  overflow: auto;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 13px;
}

.fld {
  display: grid;
  gap: 6px;
}

.fld > span {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--text-3);
}

.fld > span em {
  font-style: normal;
  font-size: 10.5px;
  color: var(--text-4);
  margin-left: 6px;
}

.fld input,
.fld select,
.fld textarea {
  font: inherit;
  font-size: 13px;
  color: var(--text);
  padding: 9px 12px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.55);
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.fld textarea {
  resize: vertical;
  line-height: 1.7;
}

.fld input:focus,
.fld select:focus,
.fld textarea:focus {
  border-color: rgba(216, 178, 90, 0.55);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.1);
}

.adm__confirm-txt {
  padding: 20px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-2);
}

.adm__confirm-txt b {
  color: var(--blood-lt);
}

.adm__modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--line-soft);
}

.is-spin {
  animation: spin 1s linear infinite;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 平板：侧边栏收窄、主体让位 */
@media (max-width: 1080px) {
  .adm__bar-inner {
    padding: 11px 18px;
  }

  .adm__layout {
    padding: 16px 18px 42px;
  }

  .adm__kbd {
    display: none;
  }
}

/* 移动端：侧边栏改为抽屉，主体全宽 */
@media (max-width: 920px) {
  .adm__burger {
    display: grid;
  }

  .adm__layout {
    padding: 14px 14px 40px;
  }

  .adm__body {
    padding-left: 0;
  }

  .adm__bar-side {
    gap: 8px;
  }
}

@media (max-width: 720px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }

  .adm__crumb {
    display: none;
  }

  .adm__sync {
    /* 窄屏只留数字，省出横向空间给标题 */
    padding: 4px 9px;
    font-size: 10px;
  }

  .adm__search {
    min-width: 0;
  }

  .ov__grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .ov__row {
    grid-template-columns: 52px 1fr 34px;
  }

  .imgurl {
    grid-template-columns: 1fr;
  }

  .imgurl__preview {
    width: 100%;
    height: 120px;
  }
}
</style>
