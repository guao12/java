/**
 * 入场态（.rise / .reveal）标记工具。
 *
 * 为什么不用 classList.add('is-in')：
 * 列表卡片普遍写着 `:class="{ 'is-locked': ... }"` 这类动态绑定。Vue 每次重渲染
 * 都会把 class 整串重写成「静态类 + 当前动态类」，手动加上的 is-in 会被安静地
 * 冲掉 —— 而 .rise 的初始样式是 opacity: 0，class 一丢，整片卡片就凭空消失。
 *
 * 典型触发：进档案室时权限点还是 0（全封存），hydrateClearance 拿到后端 34 点后
 * 密级跳到「绝密」，所有卡片的 is-locked 同时翻转 → class 重写 → 入场态全丢，
 * 「部门人物」区只剩零星一两张卡，其余空白。
 *
 * 因此改用 data 属性作为唯一真源：Vue 不会去动它没在模板里声明的属性，
 * 重渲染、筛选、切卷都不会把入场态弄丢。
 */

const ATTR = 'data-rise'

/** 标记已入场（CSS: .rise[data-rise='in']） */
export function markIn(el) {
  if (el && el.getAttribute(ATTR) !== 'in') el.setAttribute(ATTR, 'in')
}

/** 是否已入场 */
export function isIn(el) {
  return !!el && el.getAttribute(ATTR) === 'in'
}

/**
 * 让 root 内已经进入视口的 .rise 立即入场（新增节点没有进过观察器时兜底）。
 * @param {Element|null} root
 */
export function revealInView(root) {
  if (!root) return
  for (const el of root.querySelectorAll(`.rise:not([${ATTR}])`)) {
    const r = el.getBoundingClientRect()
    if (r.top < window.innerHeight && r.bottom > 0) markIn(el)
  }
}
