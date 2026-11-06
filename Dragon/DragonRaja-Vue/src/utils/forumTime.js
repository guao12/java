/**
 * 论坛里的时间显示工具。
 *
 * 后端统一返回 ISO 本地时间（yyyy-MM-ddTHH:mm:ss），这里做两件事：
 *  - relativeTime：列表里显示「3 小时前」，比一串时间戳更好扫读
 *  - fullTime：详情 / 悬浮提示里显示完整时间
 *
 * 注意不要直接把 ISO 串丢给 new Date 之后就当成本地时间：
 * 没有时区后缀的串，Safari 会按 UTC 解析，差 8 小时。这里统一用
 * 手工替换成带 T 的格式前先补一个空格兼容分支，保证两端一致。
 */

function toDate(iso) {
  if (!iso) return null
  // '2026-11-06T10:30:00' → 本地时间（不带 Z 的串各浏览器都按本地解析）
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? null : d
}

const MIN = 60 * 1000
const HOUR = 60 * MIN
const DAY = 24 * HOUR

/** 相对时间：刚刚 / n 分钟前 / n 小时前 / n 天前 / 具体日期 */
export function relativeTime(iso) {
  const d = toDate(iso)
  if (!d) return ''
  const diff = Date.now() - d.getTime()
  if (diff < 0) return fullTime(iso)
  if (diff < MIN) return '刚刚'
  if (diff < HOUR) return `${Math.floor(diff / MIN)} 分钟前`
  if (diff < DAY) return `${Math.floor(diff / HOUR)} 小时前`
  if (diff < 30 * DAY) return `${Math.floor(diff / DAY)} 天前`
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 完整时间：2026-11-06 10:30 */
export function fullTime(iso) {
  const d = toDate(iso)
  if (!d) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
    d.getMinutes()
  )}`
}

function pad(n) {
  return String(n).padStart(2, '0')
}

/**
 * 纯文本 → 段落数组。
 *
 * 帖子正文是纯文本，绝不能用 v-html 渲染（XSS）。
 * 这里按空行切段，段内单换行保留为 <br> 由模板的 white-space 处理。
 */
export function paragraphs(text) {
  if (!text) return []
  return String(text)
    .replace(/\r\n/g, '\n')
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter(Boolean)
}
