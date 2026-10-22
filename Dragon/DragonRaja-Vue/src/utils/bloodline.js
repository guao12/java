/**
 * 血统鉴定共享模块 —— 血统鉴定页（/appraisal）与龙文档案馆首页（/home）共用同一套
 * 评级标准与本机档案，避免两处各算各的、评级对不上。
 *
 * 数据存放：localStorage['dr:appraisal:records']（报告数组）
 * 每日限额：localStorage['dr:appraisal:daily']（{ "日期:账号": 次数 }）
 */

/** 血统评级标准：S >50 / A 30~50 / B 15~30 / C 5~15 / D 1~5 / E <1 */
export const GRADES = [
  {
    grade: 'S',
    label: '极度危险',
    rgb: '239, 107, 98',
    risk: '龙血浓度超过 50%，存在龙族化与暴走风险。依学院条例，须由执行部持续监测并每季度复核。'
  },
  {
    grade: 'A',
    label: '高危混血种',
    rgb: '232, 180, 90',
    risk: '龙血浓度处于高危区间，存在情绪失控与暴血风险，须按月复核。'
  },
  {
    grade: 'B',
    label: '混血种',
    rgb: '216, 178, 90',
    risk: '龙血浓度偏高，建议定期参加血统稳定性训练。'
  },
  {
    grade: 'C',
    label: '低浓度混血种',
    rgb: '127, 212, 255',
    risk: '龙血浓度偏低，按常规周期监测即可。'
  },
  {
    grade: 'D',
    label: '微量龙血残留',
    rgb: '150, 176, 214',
    risk: '检出微量龙类基因，不影响正常学籍。'
  },
  {
    grade: 'E',
    label: '普通人类',
    rgb: '95, 211, 154',
    risk: '未检出龙类基因，按普通人类登记。'
  }
]

/** 由龙血占比得到评级对象 */
export function gradeOf(pct) {
  const n = Number(pct) || 0
  if (n > 50) return GRADES[0]
  if (n >= 30) return GRADES[1]
  if (n >= 15) return GRADES[2]
  if (n >= 5) return GRADES[3]
  if (n >= 1) return GRADES[4]
  return GRADES[5]
}

/** 按等级取评级对象（旧数据兼容） */
export function gradeByLetter(g) {
  return GRADES.find((x) => x.grade === g) || GRADES[5]
}

/* ------------------------------------------------------------------ *
 * 本机档案读写
 * ------------------------------------------------------------------ */

const STORE_KEY = 'dr:appraisal:records'

export function loadRecords() {
  try {
    const arr = JSON.parse(localStorage.getItem(STORE_KEY) || '[]')
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

export function saveRecords(list) {
  try {
    localStorage.setItem(STORE_KEY, JSON.stringify(list))
  } catch {
    /* 隐私模式下写入失败，不影响本次流程 */
  }
}

/**
 * 我的最新一份鉴定报告。
 * 匹配规则：owner（提交者账号）为本人，或学号 == 本人账号；
 * 旧版本无 owner 的记录视为本机公共档案，也计入。
 */
export function myLatestReport(account) {
  const me = (account || '').trim().toUpperCase()
  const mine = loadRecords().filter((r) => !r.owner || r.owner === me || r.studentId === me)
  if (!mine.length) return null
  // 优先按签发时间戳 at（旧数据没有则回退按编号 code，形如 BL-2026-1234）倒序取最新
  return [...mine].sort((a, b) => {
    const ta = Number(a.at) || 0
    const tb = Number(b.at) || 0
    if (ta && tb && ta !== tb) return tb - ta
    return String(b.code).localeCompare(String(a.code))
  })[0]
}

/** 管理员（教员 / 研究员）可见全部记录 */
export function isBloodlineAdmin(user) {
  return user?.identity === 'teacher'
}

/* ------------------------------------------------------------------ *
 * 每日鉴定次数限制（每人每天 3 次）
 * ------------------------------------------------------------------ */

const DAILY_KEY = 'dr:appraisal:daily'
export const DAILY_LIMIT = 3

export function todayKey() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export function todayUsedCount(account) {
  try {
    const obj = JSON.parse(localStorage.getItem(DAILY_KEY) || '{}')
    if (!obj || typeof obj !== 'object') return 0
    const me = (account || '').trim().toUpperCase() || 'anonymous'
    return obj[`${todayKey()}:${me}`] || 0
  } catch {
    return 0
  }
}

/** 消耗一次配额，返回剩余次数 */
export function consumeQuota(account) {
  const me = (account || '').trim().toUpperCase() || 'anonymous'
  let obj = {}
  try {
    const raw = JSON.parse(localStorage.getItem(DAILY_KEY) || '{}')
    if (raw && typeof raw === 'object') obj = raw
  } catch {
    obj = {}
  }
  const k = `${todayKey()}:${me}`
  const used = (obj[k] || 0) + 1
  obj[k] = used
  try {
    localStorage.setItem(DAILY_KEY, JSON.stringify(obj))
  } catch {
    /* 忽略写入失败 */
  }
  return Math.max(0, DAILY_LIMIT - used)
}
