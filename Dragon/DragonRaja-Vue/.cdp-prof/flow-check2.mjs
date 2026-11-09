/**
 * 复现用户路径：登录态 → 讨论区 → 好友 → 点「讨论区」返回，确认不再出现 500 提示。
 *
 * 登录不做 UI 表单提交（无头环境里点击时机不稳），直接用 /api/auth/login 的响应
 * 写入 localStorage 的 dr:token / dr:user，等价于「勾选记住身份卡」登录后的状态。
 *
 * 用法：TOKEN=xxx USER_JSON='{...}' node flow-check2.mjs
 */
import fs from 'node:fs'
import path from 'node:path'

const PORT = Number(process.env.CDP_PORT || 9345)
const BASE = 'http://192.168.68.1:5177'
const token = (process.env.TOKEN || '').trim()
const userJson = (process.env.USER_JSON || '').trim()
if (!token) throw new Error('缺少 TOKEN 环境变量')

async function waitFor(fn, timeout = 25000, step = 300) {
  const at = Date.now()
  while (Date.now() - at < timeout) {
    try {
      const v = await fn()
      if (v) return v
    } catch {
      /* 继续等 */
    }
    await new Promise((r) => setTimeout(r, step))
  }
  throw new Error('waitFor timeout')
}

const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
const page = list.find((t) => t.type === 'page')
if (!page) throw new Error('没找到页面目标')

const ws = new WebSocket(page.webSocketDebuggerUrl)
await new Promise((res, rej) => {
  ws.onopen = res
  ws.onerror = rej
})

let seq = 0
const pending = new Map()
const consoleErrors = []

ws.onmessage = (ev) => {
  const msg = JSON.parse(ev.data)
  if (msg.id && pending.has(msg.id)) {
    const { resolve, reject } = pending.get(msg.id)
    pending.delete(msg.id)
    msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result)
    return
  }
  if (msg.method === 'Runtime.consoleAPICalled' && ['error', 'warning'].includes(msg.params.type)) {
    consoleErrors.push(
      `[console.${msg.params.type}] ` + msg.params.args.map((a) => a.value ?? a.description ?? '').join(' ')
    )
  }
  if (msg.method === 'Runtime.exceptionThrown') {
    const d = msg.params.exceptionDetails
    consoleErrors.push('[exception] ' + (d.exception?.description || d.text))
  }
}

function send(method, params = {}) {
  const id = ++seq
  ws.send(JSON.stringify({ id, method, params }))
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }))
}

async function evaluate(expression) {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || 'evaluate failed')
  return r.result.value
}

const snap = (name) =>
  send('Page.captureScreenshot', { format: 'png' }).then((s) => {
    const out = path.join(import.meta.dirname, name)
    fs.writeFileSync(out, Buffer.from(s.data, 'base64'))
    return out
  })

const state = () =>
  evaluate(`(() => {
    const t = document.body.innerText || ''
    return {
      hash: location.hash,
      offline: t.includes('无法连接学院服务器'),
      listNotLoaded: t.includes('主题列表未加载'),
      boards: (t.match(/(\\d+) 个讨论区/) || [])[1] || '',
      threadRows: document.querySelectorAll('.frow').length,
      bodyLen: t.length
    }
  })()`)

await send('Runtime.enable')
await send('Page.enable')

// 1) 先落地同源页写入登录态
await send('Page.navigate', { url: `${BASE}/` })
await waitFor(async () => (await evaluate('document.readyState')) === 'complete', 30000)
await evaluate(`(() => {
  localStorage.setItem('dr:token', ${JSON.stringify(token)})
  localStorage.setItem('dr:user', ${JSON.stringify(userJson || 'null')})
  return 'stored'
})()`)

// 2) 讨论区
await send('Page.navigate', { url: `${BASE}/#/forum` })
await waitFor(async () => {
  const t = await evaluate('document.body.innerText || ""')
  return t.includes('个讨论区')
}, 30000)
await new Promise((r) => setTimeout(r, 1200))
const firstForum = await state()
const shotForum = await snap('flow-forum.png')

// 3) 点「好友」
await evaluate(`(() => {
  const el = [...document.querySelectorAll('a,button')].find((a) => a.innerText.trim() === '好友')
  if (el) el.click()
  return !!el
})()`)
await waitFor(async () => (await evaluate('location.hash')).includes('friends'), 20000)
await new Promise((r) => setTimeout(r, 1500))
const friend = await state()
const shotFriend = await snap('flow-friends.png')

// 4) 点「讨论区」返回（用户报错的路径）
await evaluate(`(() => {
  const el = [...document.querySelectorAll('a,button')].find((a) => a.innerText.trim() === '讨论区')
  if (el) el.click()
  return !!el
})()`)
await waitFor(async () => (await evaluate('location.hash')).includes('forum'), 20000)
await new Promise((r) => setTimeout(r, 1800))
const backForum = await state()
const shotBack = await snap('flow-back-forum.png')

console.log(JSON.stringify({ firstForum, friend, backForum, consoleErrors, shots: [shotForum, shotFriend, shotBack] }, null, 2))
ws.close()
