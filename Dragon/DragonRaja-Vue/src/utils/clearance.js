/**
 * 权限（密级）管理：完成任务累积权限点，权限点达到门槛即提升可查阅密级。
 * 与首页 overview.access.maxLevel 语义保持一致：1 公开 / 2 内部 / 3 机密 / 4 绝密。
 *
 * 存储策略（双写，换电脑不丢）：
 *  - 未登录（游客）：只写 localStorage（键 dr:clearance:guest）；
 *  - 已登录：localStorage 作为本地缓存 + 后端 /api/user/clearance 作为权威存档。
 *    登录/刷新时拉取并合并，完成任务后自动上报，多设备以「取并集」的方式合并，
 *    不会因为换设备把进度覆盖回 0。
 */
import { computed, reactive } from 'vue'
import { authApi } from '@/api/auth'
import { getToken, getUser } from './auth'

/** 密级定义 */
export const LEVELS = [
  { value: 1, label: '公开' },
  { value: 2, label: '内部' },
  { value: 3, label: '机密' },
  { value: 4, label: '绝密' }
]

/** 各密级所需权限点（下标 = 密级 - 1） */
const THRESHOLDS = [0, 3, 8, 15]

/** 任务难度 → 完成可获得的权限点 */
export const RANK_POINTS = {
  常规: 1,
  中危: 2,
  高危: 3,
  绝密: 5
}

const KEY = 'dr:clearance'
const MAX_KEEP = 500 // 完成任务编号保留上限（与后端一致）
const MAX_RECORDS = 60 // 已完成任务卡留存上限（用于「已完成」栏回显）

/** 上报防抖：连续完成多个任务只发一次请求 */
const PUSH_DELAY = 800

const state = reactive({ points: 0, completed: [], records: [] })
let loadedKey = null

/**
 * 同步状态（给界面看）。
 *  status: idle 未尝试 / guest 未登录（只存本机）/ syncing 上传下载中
 *          ok 已与账号存档一致 / offline 同步失败（仅本机）
 * 这层状态是必要的：以前失败是 catch{} 静默吞掉的，用户只能看到「权限归零」，
 * 完全不知道是「后端没起来 / 表没建 / 换电脑连了另一个库」。
 */
export const syncState = reactive({ status: 'idle', error: '', at: 0 })

function setSync(status, error = '') {
  syncState.status = status
  syncState.error = error
  syncState.at = Date.now()
}


/** 已从后端拉取过的账号，避免每次访问都重复请求 */
let hydratedKey = null
/** 进行中的拉取任务，防止并发重复拉取 */
let hydratePromise = null
/** 上报防抖定时器 */
let pushTimer = 0
/** 是否正在上报 */
let pushing = false

function storeKey() {
  const u = getUser()
  return `${KEY}:${u?.account || 'guest'}`
}

/** 任务卡 → 精简的已完成记录（只留展示所需字段，避免把整张大对象塞进 localStorage） */
function toRecord(m, rank) {
  return {
    code: m.code,
    name: m.name || m.code,
    rank: m.rank || rank || '常规',
    region: m.region || '',
    reward: m.reward || '',
    desc: m.desc || '',
    issued: m.issued || '',
    tags: Array.isArray(m.tags) ? m.tags.slice(0, 3) : [],
    status: '已完成',
    at: Date.now()
  }
}

function read(k) {
  try {
    const raw = localStorage.getItem(k)
    if (!raw) return { points: 0, completed: [], records: [] }
    const v = JSON.parse(raw)
    return {
      points: Number(v?.points) || 0,
      completed: Array.isArray(v?.completed) ? v.completed.filter((c) => typeof c === 'string') : [],
      records: Array.isArray(v?.records)
        ? v.records.filter((r) => r && typeof r.code === 'string')
        : []
    }
  } catch {
    return { points: 0, completed: [], records: [] }
  }
}

/** 首次访问 / 切换账号时重新载入权限 */
function ensure() {
  const k = storeKey()
  if (k === loadedKey) return
  const v = read(k)
  state.points = v.points
  state.completed = v.completed
  state.records = v.records
  loadedKey = k
}

/** 只写本地缓存（不触发上报，供同步流程内部使用） */
function persistLocal() {
  try {
    localStorage.setItem(
      storeKey(),
      JSON.stringify({
        points: state.points,
        completed: state.completed.slice(-MAX_KEEP),
        records: state.records.slice(0, MAX_RECORDS)
      })
    )
  } catch {
    /* 隐私模式 / 配额不足时忽略 */
  }
}

/** 写本地 + 异步上报后端 */
function persist() {
  persistLocal()
  schedulePush()
}

/* ---------------- 后端同步（跨设备） ---------------- */

/** 是否具备同步条件：已登录且拿到令牌 */
function canSync() {
  return !!getToken() && !!getUser()?.account
}

function normalize(v) {
  return {
    points: Number(v?.points) || 0,
    completed: Array.isArray(v?.completed)
      ? v.completed.filter((c) => typeof c === 'string' && c)
      : [],
    records: Array.isArray(v?.records) ? v.records.filter((r) => r && typeof r.code === 'string') : []
  }
}

function payloadOf(v) {
  return {
    points: v.points,
    completed: v.completed.slice(-MAX_KEEP),
    records: v.records.slice(0, MAX_RECORDS)
  }
}

/** 合并两份进度：权限点取大、编号取并集、记录按编号去重保留较新 */
function mergeStates(a, b) {
  const x = normalize(a)
  const y = normalize(b)

  const completed = []
  const seen = new Set()
  for (const code of [...x.completed, ...y.completed]) {
    if (seen.has(code)) continue
    seen.add(code)
    completed.push(code)
  }

  const byCode = new Map()
  for (const r of [...x.records, ...y.records]) {
    const old = byCode.get(r.code)
    if (!old || (Number(r.at) || 0) >= (Number(old.at) || 0)) byCode.set(r.code, r)
  }
  const records = [...byCode.values()]
    .sort((m, n) => (Number(n.at) || 0) - (Number(m.at) || 0))
    .slice(0, MAX_RECORDS)

  return {
    points: Math.max(x.points, y.points),
    completed: completed.slice(-MAX_KEEP),
    records
  }
}

/** 内容是否相同（用于判断要不要回传后端） */
function sameStates(a, b) {
  const x = normalize(a)
  const y = normalize(b)
  if (x.points !== y.points) return false
  if (x.completed.join('|') !== y.completed.join('|')) return false
  if (x.records.length !== y.records.length) return false
  return x.records.every((r, i) => r.code === y.records[i].code && (r.at || 0) === (y.records[i].at || 0))
}

function applyState(v) {
  const n = normalize(v)
  state.points = n.points
  state.completed = n.completed
  state.records = n.records
  loadedKey = storeKey()
}

/** 防抖上报 */
function schedulePush() {
  if (!canSync()) return
  clearTimeout(pushTimer)
  pushTimer = window.setTimeout(() => {
    pushClearance()
  }, PUSH_DELAY)
}

/* ---------------- 失败重试 ---------------- */

/** 退避间隔：后端刚启动、网络刚恢复时，等一会儿再试通常就好了 */
const RETRY_DELAYS = [4000, 12000, 30000, 60000, 120000]
let retryTimer = 0
let retryStep = 0
let listenersBound = false

function clearRetry() {
  clearTimeout(retryTimer)
  retryTimer = 0
  retryStep = 0
}

function armRetry() {
  if (!canSync() || retryStep >= RETRY_DELAYS.length) return
  clearTimeout(retryTimer)
  const wait = RETRY_DELAYS[retryStep]
  retryStep += 1
  retryTimer = window.setTimeout(() => {
    hydrateClearance(true)
  }, wait)
}

/** 网络恢复 / 切回标签页时立刻补一次同步（不占用退避次数） */
function bindRecoverListeners() {
  if (listenersBound || typeof window === 'undefined') return
  listenersBound = true
  window.addEventListener('online', () => {
    if (canSync() && syncState.status !== 'ok') {
      clearRetry()
      hydrateClearance(true)
    }
  })
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && canSync() && syncState.status !== 'ok') {
      hydrateClearance(true)
    }
  })
}


/**
 * 拉取后端进度并与本地合并（登录 / 刷新页面 / 进入任务页时调用）。
 * 幂等：同一账号只成功拉取一次；后端不可用时静默降级为纯本地。
 *
 * @param {boolean} [force] 忽略「已拉取过」标记，强制再拉一次
 */
export function hydrateClearance(force = false) {
  ensure()
  const account = getUser()?.account || ''
  if (!account || !getToken()) {
    // 未登录：进度只在本机，换电脑必然没有 —— 如实告诉界面
    setSync('guest')
    return Promise.resolve(state)
  }
  if (!force && hydratedKey === account) return Promise.resolve(state)
  if (hydratePromise) return hydratePromise

  bindRecoverListeners()
  setSync('syncing')

  hydratePromise = (async () => {
    try {
      const remote = normalize(await authApi.clearance())
      const merged = mergeStates(state, remote)
      applyState(merged)
      persistLocal()
      hydratedKey = account

      // 本地比后端多（首次迁移，或离线期间完成的任务）→ 回传合并
      if (!sameStates(merged, remote)) {
        const saved = normalize(await authApi.saveClearance(payloadOf(merged)))
        applyState(mergeStates(merged, saved))
        persistLocal()
      }
      setSync('ok')
      clearRetry()
    } catch (e) {
      // 保留本地进度，但不许静默：记录原因并安排退避重试
      setSync('offline', e?.message || '无法连接权限存档')
      armRetry()
    } finally {
      hydratePromise = null
    }
    return state
  })()

  return hydratePromise
}

/** 立即上报当前进度（合并式写入，后端返回最终结果） */
export async function pushClearance() {
  if (!canSync() || pushing) return
  pushing = true
  try {
    const saved = normalize(await authApi.saveClearance(payloadOf(state)))
    // 后端可能合并了其他设备的进度，回写本地
    const merged = mergeStates(state, saved)
    applyState(merged)
    persistLocal()
    setSync('ok')
    clearRetry()
  } catch (e) {
    // 离线：本地已落盘，但要标出来并稍后重试，不能假装同步成功
    setSync('offline', e?.message || '进度未上传')
    armRetry()
  } finally {
    pushing = false
  }
}

function levelOf(p) {
  let lv = 1
  for (let i = 0; i < THRESHOLDS.length; i += 1) {
    if (p >= THRESHOLDS[i]) lv = i + 1
  }
  return lv
}

export const points = computed(() => {
  ensure()
  return state.points
})

export const level = computed(() => levelOf(points.value))

export const levelLabel = computed(
  () => LEVELS.find((l) => l.value === level.value)?.label || '公开'
)

export const completed = computed(() => {
  ensure()
  return state.completed
})

/** 已完成任务卡记录（新的在前），用于「已完成」栏跨刷新 / 跨会话回显 */
export const records = computed(() => {
  ensure()
  return state.records
})

/** 下一密级进度（已满级返回 null） */
export const nextLevel = computed(() => {
  const lv = level.value
  if (lv >= LEVELS.length) return null
  const need = THRESHOLDS[lv]
  return {
    need,
    remain: Math.max(need - points.value, 0),
    pct: Math.min(100, Math.round((points.value / need) * 100))
  }
})

/** 密级是否足够 */
export function hasClearance(req) {
  return level.value >= (req || 1)
}

/** 升到指定密级总共需要的权限点 */
export function requiredPoints(req) {
  return THRESHOLDS[(req || 1) - 1] ?? THRESHOLDS[THRESHOLDS.length - 1]
}

/** 距离升到指定密级还差多少权限点（已达标返回 0） */
export function pointsTo(req) {
  return Math.max(requiredPoints(req) - points.value, 0)
}

export function isCompleted(code) {
  return !!code && completed.value.includes(code)
}

/**
 * 完成任务：登记编号并累加权限点（同一编号不会重复计入），
 * 同时把任务卡快照存成「已完成记录」，供任务页「已完成」栏回显。
 * @param {string} code 任务编号
 * @param {string} rank 难度（常规 / 中危 / 高危 / 绝密）
 * @param {object} [snapshot] 任务卡（name / region / reward / tags 等）
 * @returns {number} 本次获得的权限点，重复完成返回 0
 */
export function completeMission(code, rank, snapshot) {
  ensure()
  if (!code) return 0
  const isNew = !state.completed.includes(code)
  const gain = isNew ? RANK_POINTS[rank] || 1 : 0
  if (isNew) {
    state.completed.push(code)
    state.points += gain
  }
  if (snapshot && !state.records.some((r) => r.code === code)) {
    state.records = [toRecord(snapshot, rank), ...state.records].slice(0, MAX_RECORDS)
  }
  persist()
  return gain
}

/** 手动重试（界面上「重新同步」按钮） */
export function syncNow() {
  clearRetry()
  return hydrateClearance(true)
}

/* ---------------- 跨设备搬运：备份码 ---------------- */

/**
 * 备用通道：把进度压成一串可复制的文本。
 *
 * 为什么还需要它：后端存档只在「两台机器连同一个 MySQL」时才通用。
 * 如果换的电脑是另一套本地数据库（各自装了 MySQL、各自跑 启动.bat），
 * 账号能登录，但那台机器上根本没有你的权限行 —— 这不是同步坏了，
 * 而是两边压根不是同一个仓库。此时只能由人把备份码搬过去。
 */
const CODE_TAG = 'DRC1'

/** UTF-8 安全的 base64（btoa 只吃 latin1，中文任务名会炸） */
function b64encode(str) {
  const bytes = new TextEncoder().encode(str)
  let bin = ''
  for (let i = 0; i < bytes.length; i += 0x8000) {
    bin += String.fromCharCode(...bytes.subarray(i, i + 0x8000))
  }
  return btoa(bin)
}

function b64decode(b64) {
  const bin = atob(b64)
  const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i += 1) bytes[i] = bin.charCodeAt(i)
  return new TextDecoder().decode(bytes)
}

/** 导出：返回形如 `DRC1.eyJ2Ijox...` 的文本 */
export function exportClearanceCode() {
  ensure()
  const payload = {
    v: 1,
    account: getUser()?.account || '',
    points: state.points,
    completed: state.completed,
    records: state.records,
    at: Date.now()
  }
  return `${CODE_TAG}.${b64encode(JSON.stringify(payload))}`
}

/**
 * 导入备份码：与当前进度取并集（不会把已有的点覆盖小），随后自动上报。
 * @returns {{ok: boolean, message: string, points?: number, gained?: number}}
 */
export function importClearanceCode(raw) {
  if (!raw || !String(raw).trim()) {
    return { ok: false, message: '请先粘贴备份码' }
  }
  let text = String(raw).trim().replace(/\s+/g, '')
  if (text.startsWith(`${CODE_TAG}.`)) text = text.slice(CODE_TAG.length + 1)

  let payload
  try {
    payload = JSON.parse(b64decode(text))
  } catch {
    return { ok: false, message: '备份码无法识别，请确认整段复制完整' }
  }
  if (!payload || payload.v !== 1 || !Array.isArray(payload.completed)) {
    return { ok: false, message: '备份码版本不匹配，无法导入' }
  }

  ensure()
  const before = state.points
  const merged = mergeStates(state, {
    points: payload.points,
    completed: payload.completed,
    records: payload.records
  })
  applyState(merged)
  persist()

  const gained = merged.points - before
  const from = payload.account ? `（来自 ${payload.account}）` : ''
  return {
    ok: true,
    points: merged.points,
    gained,
    message: gained > 0 ? `已导入${from} · 新增 ${gained} 点` : `已并入${from} · 没有新增权限点`
  }
}

