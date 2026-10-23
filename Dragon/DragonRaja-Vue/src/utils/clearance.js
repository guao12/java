/**
 * 权限（密级）管理：完成任务累积权限点，权限点达到门槛即提升可查阅密级。
 * 与首页 overview.access.maxLevel 语义保持一致：1 公开 / 2 内部 / 3 机密 / 4 绝密。
 * 状态持久化在 localStorage，按登录账号分别保存（避免多账号共用一份权限）。
 */
import { computed, reactive } from 'vue'
import { getUser } from './auth'

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
const MAX_KEEP = 200 // 完成任务编号保留上限

const state = reactive({ points: 0, completed: [] })
let loadedKey = null

function storeKey() {
  const u = getUser()
  return `${KEY}:${u?.account || 'guest'}`
}

function read(k) {
  try {
    const raw = localStorage.getItem(k)
    if (!raw) return { points: 0, completed: [] }
    const v = JSON.parse(raw)
    return {
      points: Number(v?.points) || 0,
      completed: Array.isArray(v?.completed) ? v.completed.filter((c) => typeof c === 'string') : []
    }
  } catch {
    return { points: 0, completed: [] }
  }
}

/** 首次访问 / 切换账号时重新载入权限 */
function ensure() {
  const k = storeKey()
  if (k === loadedKey) return
  const v = read(k)
  state.points = v.points
  state.completed = v.completed
  loadedKey = k
}

function persist() {
  try {
    localStorage.setItem(
      storeKey(),
      JSON.stringify({ points: state.points, completed: state.completed.slice(-MAX_KEEP) })
    )
  } catch {
    /* 隐私模式 / 配额不足时忽略 */
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
 * 完成任务：登记编号并累加权限点（同一编号不会重复计入）。
 * @param {string} code 任务编号
 * @param {string} rank 难度（常规 / 中危 / 高危 / 绝密）
 * @returns {number} 本次获得的权限点，重复完成返回 0
 */
export function completeMission(code, rank) {
  ensure()
  if (!code || state.completed.includes(code)) return 0
  const gain = RANK_POINTS[rank] || 1
  state.completed.push(code)
  state.points += gain
  persist()
  return gain
}
