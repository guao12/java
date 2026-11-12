/**
 * 落地层的本机状态。
 *
 * 只存两个键，都在 localStorage：
 *  - `dr:landing-seen`：是否已经看过序章。看过之后 `/` 直接跳到门户，
 *    避免每次进来都重播片头（对应方案里的「回访用户不播放」）。
 *  - `dr:faction`：上次停留的阵营，门户页据此恢复背景与文案。
 *
 * 全部包 try/catch：隐私模式 / 禁用存储时不能因为一个 key 读不出来就把页面搞崩。
 */

const SEEN_KEY = 'dr:landing-seen'
const FACTION_KEY = 'dr:faction'

export function landingSeen() {
  try {
    return localStorage.getItem(SEEN_KEY) === '1'
  } catch {
    return false
  }
}

export function markLandingSeen() {
  try {
    localStorage.setItem(SEEN_KEY, '1')
  } catch {
    /* 存不了就算了，下次再看一遍序章，不影响功能 */
  }
}

/** 门户页「重看序章」用 */
export function clearLandingSeen() {
  try {
    localStorage.removeItem(SEEN_KEY)
  } catch {
    /* noop */
  }
}

export function savedFaction() {
  try {
    return localStorage.getItem(FACTION_KEY) || ''
  } catch {
    return ''
  }
}

export function saveFaction(key) {
  try {
    localStorage.setItem(FACTION_KEY, key)
  } catch {
    /* noop */
  }
}
