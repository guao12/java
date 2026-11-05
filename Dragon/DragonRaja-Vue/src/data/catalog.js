/**
 * 卷宗总目录（首页与档案室唯一数据源）
 *
 * 背景：档案室（ArchiveView）的内容在 data/archive.js 里，是四卷结构：
 *   卷一 龙王卷宗 / 卷二 部门人物 / 卷三 龙族武器 / 卷四 任务执行记录
 * 而首页原来读的是后端 dr_archive（AR-* 编号），两处条目完全对不上：
 * 首页看不到龙王，档案室里的武器与任务在首页也搜不到。
 *
 * 本文件把 archive.js 的四卷数据拍平成一份「卷宗目录」，首页的
 * 精选 / 热门 / 最新 / 分类 / 密级 / 检索 全部改为读这份目录，
 * 每条卷宗都带一个详情页路由（path），点卡片即可进入。
 *
 * 字段规范（HomeView / ArchiveView / ArchiveDetailView 共用）：
 *   uid        唯一键 `${kind}:${key}`
 *   kind       king 龙王 / person 人物 / arm 武器 / log 任务记录
 *   key        卷内定位键（龙王=编号、人物=人物 key、武器=编号、任务=编号|人名）
 *   code       卷宗编号（展示用）
 *   title      标题
 *   subtitle   副标题（别名 / 类型 / 执行人）
 *   summary    摘要
 *   topId/topName      一级分类（四卷）
 *   subId/subName      二级分类（卷内细目）
 *   level/levelLabel   密级 1~4
 *   rankLabel  评级 / 威胁等级
 *   tags       标签
 *   source     归档部门
 *   viewCount / heat / savedAt   浏览 / 热度 / 归档时间（按编号派生，稳定不抖）
 *   search     检索用文本（标题 + 摘要 + 标签 + 正文关键词，全小写）
 *   path       详情页路由
 */
import {
  ARCHIVE_ARMS,
  ARCHIVE_DEPTS,
  ARCHIVE_KINGS,
  ARCHIVE_MISSION_LOGS,
  ARCHIVE_PEOPLE
} from './archive'
import { ORG_GALLERY, SPIRIT_GALLERY } from './codex'

/** 密级文案：与 utils/clearance.js 的 LEVELS 同口径 */
export const LEVEL_LABELS = { 1: '公开', 2: '内部', 3: '机密', 4: '绝密' }

/**
 * 无需密级（level 1）即可调阅的卷宗。
 *
 * 现实里档案馆也有「公开阅览区」：人物名录、制式装备、常规任务记录本来就挂在
 * 大厅里任人翻阅，真正封存的是龙王卷宗与带血的细节。所以这里按类目挑，
 * 而不是把龙王降级 —— 龙王卷宗一律保持机密以上。
 *
 * 这些条目在档案室里同样是「公开」，两处口径一致（同一个 CATALOG）。
 */
const PUBLIC_PERSON_KEYS = ['nuonuo', 'fingel']
const PUBLIC_ARM_CODES = ['ARM-11', 'ARM-12']

/** 密级文本 → 数字（言灵 / 机构数据里是「高危 / 机密」这种词） */
const LEVEL_BY_TEXT = { 公开: 1, 内部: 2, 高危: 2, 机密: 3, 绝密: 4 }

/** 四卷目录（二级分类顺序即页面上的细目 chips 顺序） */
export const CATALOG_CATEGORIES = [
  {
    id: 'kings',
    name: '龙王卷宗',
    icon: 'flame',
    desc: '皇帝 · 四大君主 · 篡夺者',
    children: [
      { id: 'kings.royal', name: '皇帝' },
      { id: 'kings.lord', name: '四大君主' },
      { id: 'kings.usurper', name: '篡夺者' }
    ]
  },
  {
    id: 'people',
    name: '部门人物',
    icon: 'user',
    desc: '性格 · 武器 · 言灵 · 任务 · 身世',
    children: ARCHIVE_DEPTS.map((d) => ({ id: `people.${d.id}`, name: d.label }))
  },
  {
    id: 'arms',
    name: '龙族武器',
    icon: 'sparkle',
    desc: '七宗罪 · 王器 · 制式配给',
    children: [
      { id: 'arms.sin', name: '七宗罪' },
      { id: 'arms.gear', name: '王器 · 制式' }
    ]
  },
  {
    id: 'logs',
    name: '任务执行记录',
    icon: 'archive',
    desc: '过程 · 结果 · 代价',
    children: ARCHIVE_DEPTS.map((d) => ({ id: `logs.${d.id}`, name: d.label }))
  }
]

/* ------------------------------------------------------------------ *
 * 派生数值：按卷宗编号哈希，保证每次刷新都一致（不能随机，否则
 * 「热门」排序会自己跳动，用户会以为数据错乱）
 * ------------------------------------------------------------------ */

function hash(str) {
  let h = 2166136261
  const s = String(str)
  for (let i = 0; i < s.length; i += 1) {
    h ^= s.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

const pad2 = (n) => String(n).padStart(2, '0')
/** 归档时间基准：2026-11-01，按编号往前推 0~260 天 */
const BASE_TS = Date.UTC(2026, 10, 1, 4, 0)

function stampOf(seed) {
  const days = hash(seed) % 260
  const hours = hash(`${seed}#h`) % 24
  const d = new Date(BASE_TS - days * 86400000 - hours * 3600000)
  return (
    `${d.getUTCFullYear()}-${pad2(d.getUTCMonth() + 1)}-${pad2(d.getUTCDate())} ` +
    `${pad2(d.getUTCHours())}:${pad2(d.getUTCMinutes())}`
  )
}

/** 首页「精选卷宗」：人工挑的 6 条，覆盖四卷 */
const SPOTLIGHT_UIDS = [
  'king:DK-000',
  'king:DK-002',
  'king:DK-007',
  'person:luomingfei',
  'person:chuzihang',
  'arm:ARM-08'
]

/** 统一装配：补齐派生字段（bodyText 只参与检索，不留在条目上） */
function make(partial) {
  const { bodyText = '', ...rest } = partial
  const uid = `${rest.kind}:${rest.key}`
  const h = hash(uid)
  const text = [
    rest.code,
    rest.title,
    rest.subtitle,
    rest.summary,
    rest.rankLabel,
    rest.topName,
    rest.subName,
    rest.source,
    ...(rest.tags || []),
    bodyText
  ]
  return {
    uid,
    topId: '',
    subId: '',
    topName: '',
    subName: '',
    level: 1,
    rankLabel: '',
    tags: [],
    source: '中央档案馆',
    path: '',
    ...rest,
    levelLabel: LEVEL_LABELS[rest.level || 1] || '公开',
    spotlight: SPOTLIGHT_UIDS.includes(uid),
    viewCount: 600 + (h % 4400),
    heat: 42 + ((h >>> 7) % 58),
    savedAt: stampOf(uid),
    search: text.filter(Boolean).join(' ').toLowerCase()
  }
}

/* ------------------------------------------------------------------ *
 * 卷一 · 龙王卷宗
 * ------------------------------------------------------------------ */

const KING_ITEMS = ARCHIVE_KINGS.map((k, i) => {
  const royal = (k.tags || []).includes('零代种')
  const lord = (k.tags || []).includes('初代种')
  const sub = royal ? 'kings.royal' : lord ? 'kings.lord' : 'kings.usurper'
  const subName = CATALOG_CATEGORIES[0].children.find((c) => c.id === sub)?.name || '龙王'
  return make({
    kind: 'king',
    key: k.code,
    code: k.code,
    title: k.name,
    subtitle: `${k.alias} · ${k.seat}`,
    summary: k.desc,
    topId: 'kings',
    topName: '龙王卷宗',
    subId: sub,
    subName,
    level: k.req || 1,
    rankLabel: k.threat || '',
    tags: k.tags || [],
    source: '龙类研究院',
    order: i,
    bodyText: [k.status, k.seats, k.notes, ...(k.intel || []).map((it) => `${it.title} ${it.text}`)]
      .filter(Boolean)
      .join(' '),
    path: `/archive/king/${k.code}`
  })
})

/* ------------------------------------------------------------------ *
 * 卷二 · 部门人物
 * ------------------------------------------------------------------ */

const PEOPLE_ITEMS = ARCHIVE_PEOPLE.map((p, i) => {
  const sub = `people.${p.dept || p.category || 'student'}`
  const subName = CATALOG_CATEGORIES[1].children.find((c) => c.id === sub)?.name || '部门人物'
  return make({
    kind: 'person',
    key: p.key,
    code: `P-${String(i + 1).padStart(3, '0')}`,
    title: p.name,
    subtitle: `${p.alias} · ${p.deptLabel || ''}`.replace(/\s·\s$/, ''),
    summary: p.desc,
    topId: 'people',
    topName: '部门人物',
    subId: sub,
    subName,
    level: PUBLIC_PERSON_KEYS.includes(p.key) ? 1 : p.req || 1,
    rankLabel: `${p.blood || ''} 级血统`.trim(),
    // 血统已由 rankLabel 展示，标签里只放部门与言灵，避免同一个词出现两遍
    tags: [p.deptLabel, ...(p.spirits || []).map((s) => s.name)].filter(Boolean).slice(0, 4),
    source: p.deptLabel || '人事科',
    order: i,
    bodyText: [
      p.role,
      p.bloodNote,
      p.quote,
      ...(p.traits || []),
      p.weapon?.name,
      p.weapon?.desc,
      ...(p.spirits || []).map((s) => `${s.name} ${s.desc}`),
      ...(p.missions || []).map((m) => `${m.code} ${m.name} ${m.process} ${m.result}`),
      ...(p.honors || []),
      p.origin,
      p.family
    ]
      .filter(Boolean)
      .join(' '),
    path: `/archive/person/${p.key}`
  })
})

/* ------------------------------------------------------------------ *
 * 卷三 · 龙族武器
 * ------------------------------------------------------------------ */

const ARM_ITEMS = ARCHIVE_ARMS.map((a, i) => {
  const sin = (a.tags || []).includes('七宗罪')
  const sub = sin ? 'arms.sin' : 'arms.gear'
  const subName = sin ? '七宗罪' : '王器 · 制式'
  return make({
    kind: 'arm',
    key: a.code,
    code: a.code,
    title: a.name,
    subtitle: `${a.type} · ${a.origin}`,
    summary: a.desc,
    topId: 'arms',
    topName: '龙族武器',
    subId: sub,
    subName,
    level: PUBLIC_ARM_CODES.includes(a.code) ? 1 : a.req || 1,
    rankLabel: a.rank || '',
    tags: a.tags || [],
    source: '炼金术与科学部',
    order: i,
    bodyText: [a.perk, a.status, a.origin].filter(Boolean).join(' '),
    path: `/archive/arm/${a.code}`
  })
})

/* ------------------------------------------------------------------ *
 * 卷四 · 任务执行记录
 * ------------------------------------------------------------------ */

const LOG_ITEMS = ARCHIVE_MISSION_LOGS.map((m, i) => {
  const person = ARCHIVE_PEOPLE.find((p) => p.key === m.personKey)
  const dept = person?.dept || 'exec'
  const sub = `logs.${dept}`
  const subName = CATALOG_CATEGORIES[3].children.find((c) => c.id === sub)?.name || '任务记录'
  return make({
    kind: 'log',
    key: `${m.code}|${m.person}`,
    code: m.code,
    title: m.name,
    subtitle: `${m.person} · ${m.deptLabel || ''}`.replace(/\s·\s$/, ''),
    summary: m.process,
    topId: 'logs',
    topName: '任务执行记录',
    subId: sub,
    subName,
    // 执行人档案公开 → 其任务记录一并进公开阅览区
    level: PUBLIC_PERSON_KEYS.includes(m.personKey) ? 1 : m.req || 1,
    rankLabel: m.person,
    tags: [m.person, m.deptLabel].filter(Boolean),
    source: m.deptLabel || '执行部',
    order: i,
    bodyText: [m.result, m.person].filter(Boolean).join(' '),
    path: `/log/${m.code}/${encodeURIComponent(m.person)}`
  })
})

/** 全部卷宗（顺序：龙王 → 人物 → 武器 → 任务记录） */
export const CATALOG = [...KING_ITEMS, ...PEOPLE_ITEMS, ...ARM_ITEMS, ...LOG_ITEMS]

/* ------------------------------------------------------------------ *
 * 图鉴侧条目（言灵 / 机构）
 *
 * 它们不进四卷馆藏统计（首页统计仍按 51 份卷宗算），只进检索池：
 * 用户在首页搜「君焰」「执行部」「炼金」时，能直接命中对应档案页，
 * 而不是丢一句「未检索到」。路径走 /spirit/:key 与 /org/:key。
 * ------------------------------------------------------------------ */

const SPIRIT_ITEMS = SPIRIT_GALLERY.map((s, i) =>
  make({
    kind: 'spirit',
    key: s.key,
    code: `SP-${String(i + 1).padStart(2, '0')}`,
    title: s.name,
    subtitle: `${s.element}属性 · ${s.level}`,
    summary: s.desc,
    topId: 'spirits',
    topName: '言灵图鉴',
    subId: 'spirits.all',
    subName: `言灵 · ${s.element}`,
    level: LEVEL_BY_TEXT[s.level] || 2,
    rankLabel: s.level,
    tags: [s.element, s.effect].filter(Boolean),
    source: '言灵谱系',
    order: i,
    bodyText: [s.effect, s.holder, s.note, s.desc].filter(Boolean).join(' '),
    path: `/spirit/${s.key}`
  })
)

const ORG_ITEMS = ORG_GALLERY.map((o, i) =>
  make({
    kind: 'org',
    key: o.key,
    code: o.code,
    title: o.name,
    subtitle: `${o.level}级机构 · ${o.head || ''}`.replace(/\s·\s$/, ''),
    summary: o.duty,
    topId: 'orgs',
    topName: '学院机构',
    subId: 'orgs.all',
    subName: `机构 · ${(o.tags || [])[0] || '编制'}`,
    level: LEVEL_BY_TEXT[o.level] || 3,
    rankLabel: o.level,
    tags: o.tags || [],
    source: '校董会编制',
    order: i,
    bodyText: [o.desc, o.duty, o.head, o.seat, ...(o.tags || [])].filter(Boolean).join(' '),
    path: `/org/${o.key}`
  })
)

/** 图鉴侧条目（仅用于检索，不参与馆藏统计与书架） */
export const CODEX_CATALOG = [...SPIRIT_ITEMS, ...ORG_ITEMS]

/**
 * 检索时才出现的一级分类（首页 chips 用）。
 * 不用预先挂到 CATALOG_CATEGORIES 上 —— 那样会把四卷统计撑大。
 */
export const CATALOG_EXTRA_CATEGORIES = [
  { id: 'spirits', name: '言灵图鉴', icon: 'flame', desc: '言灵谱系', children: [] },
  { id: 'orgs', name: '学院机构', icon: 'shield', desc: '机构志', children: [] }
]

/** 各卷条数 */
export const CATALOG_TOTALS = CATALOG_CATEGORIES.reduce((acc, c) => {
  acc[c.id] = CATALOG.filter((i) => i.topId === c.id).length
  return acc
}, {})

/* ------------------------------------------------------------------ *
 * 排序 / 精选 / 热门 / 最新
 * ------------------------------------------------------------------ */

const SORTERS = {
  heat: (a, b) => b.heat - a.heat || b.viewCount - a.viewCount,
  new: (a, b) => (a.savedAt < b.savedAt ? 1 : a.savedAt > b.savedAt ? -1 : 0),
  view: (a, b) => b.viewCount - a.viewCount,
  level: (a, b) => b.level - a.level || b.heat - a.heat
}

/** 首页书架条数 */
const SHELF_SIZE = 6

function take(list, n = SHELF_SIZE) {
  return list.slice(0, n)
}

export const CATALOG_SPOTLIGHT = take(
  CATALOG.filter((i) => i.spotlight).sort(SORTERS.heat)
)

/**
 * 公开阅览区：无需任何权限点即可全文调阅的卷宗。
 * 档案室里也是同一批（密级标记为「公开」），首页单独给一个书架，
 * 保证新账号进来先有东西可读，而不是满屏「已封存」。
 */
export const CATALOG_PUBLIC = take(
  CATALOG.filter((i) => i.level === 1).sort(SORTERS.heat),
  8
)

export const CATALOG_HOT = take([...CATALOG].sort(SORTERS.heat))

export const CATALOG_LATEST = take([...CATALOG].sort(SORTERS.new))

/**
 * 热门检索词：全部取自目录里真实存在的关键词，
 * 保证点下去一定能搜到东西（不能出现「热门词搜出 0 条」）。
 */
export const CATALOG_HOT_KEYWORDS = [
  '诺顿',
  '七宗罪',
  '路明非',
  '楚子航',
  '龙王',
  '青铜与火',
  '村雨',
  '血统'
]

/* ------------------------------------------------------------------ *
 * 检索
 * ------------------------------------------------------------------ */

/** 按维度统计条数（首页 chips 上的数字） */
function facetOf(list, field) {
  const map = new Map()
  for (const item of list) {
    const key = item[field]
    if (!key) continue
    map.set(key, (map.get(key) || 0) + 1)
  }
  return [...map.entries()].map(([key, total]) => ({ key, total }))
}

/**
 * 本地检索：口径与后端 /api/archive/search 一致（关键词 + 两级分类 + 密级 + 排序 + 翻页），
 * 但数据源换成档案室的四卷目录，保证首页检索结果与档案室完全对得上。
 *
 * @param {object} q keyword / top / sub / level / sort / page / size
 */
export function searchCatalog(q = {}) {
  const keyword = String(q.keyword || '').trim().toLowerCase()
  const size = Math.max(1, Number(q.size) || 12)
  const page = Math.max(1, Number(q.page) || 1)
  const sort = SORTERS[q.sort] ? q.sort : 'heat'

  // 1) 关键词先过滤 —— 分面统计都基于这一步，与后端「检索后条数联动」一致
  const matched = keyword ? CATALOG.filter((i) => i.search.includes(keyword)) : CATALOG.slice()

  // 2) 分面
  const topCategoryFacets = facetOf(matched, 'topId')
  const inTop = q.top ? matched.filter((i) => i.topId === q.top) : matched
  const categoryFacets = q.top ? facetOf(inTop, 'subId') : []
  const inSub = q.sub ? inTop.filter((i) => i.subId === q.sub) : inTop
  const levelFacets = facetOf(inSub, 'level').map((f) => ({ key: Number(f.key), total: f.total }))

  // 3) 过滤 + 排序
  let list = inSub
  if (q.level) list = list.filter((i) => i.level === Number(q.level))
  list = [...list].sort(SORTERS[sort])

  const total = list.length
  const pages = total === 0 ? 0 : Math.ceil(total / size)
  const start = (page - 1) * size
  const pageList = list.slice(start, start + size)

  return {
    list: pageList,
    total,
    page: { list: pageList, page, size, total, pages },
    topCategoryFacets,
    categoryFacets,
    levelFacets
  }
}

/* ------------------------------------------------------------------ *
 * 详情页定位
 * ------------------------------------------------------------------ */

/**
 * 取一条卷宗详情数据。
 * @param {'king'|'person'|'arm'|'log'} kind
 * @param {string} key
 * @returns {{ item: object|null, raw: object|null }}
 */
export function findDossier(kind, key) {
  const k = String(kind || '')
  const id = String(key || '')
  const item = CATALOG.find((i) => i.kind === k && i.key === id) || null
  let raw = null
  if (item) {
    if (k === 'king') raw = ARCHIVE_KINGS.find((x) => x.code === id) || null
    else if (k === 'person') raw = ARCHIVE_PEOPLE.find((x) => x.key === id) || null
    else if (k === 'arm') raw = ARCHIVE_ARMS.find((x) => x.code === id) || null
  }
  return { item, raw }
}

/** 同卷（同一二级分类优先）的其他卷宗，用于详情页底部互跳 */
export function siblingDossiers(item, n = 6) {
  if (!item) return []
  return CATALOG.filter((i) => i.uid !== item.uid && i.topId === item.topId)
    .sort((a, b) => {
      const sa = a.subId === item.subId ? 0 : 1
      const sb = b.subId === item.subId ? 0 : 1
      return sa - sb || b.heat - a.heat
    })
    .slice(0, n)
}

/** 卷宗详情页路由（找不到时回档案室） */
export function dossierPath(item) {
  return item?.path || '/archive'
}
