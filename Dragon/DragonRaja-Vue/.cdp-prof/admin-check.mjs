/**
 * 用 CDP 打开后台「主题管理」面板，校验渲染并截图。
 *
 * 为什么不用现成工具：本机没装 agent-browser，装它要拉一份 Chromium（几百 MB），
 * 而本机已有 Chrome，Node 22 又自带 fetch + WebSocket，直接说 CDP 协议最省事。
 *
 * 用法：node admin-check.mjs <token>
 */
import fs from 'node:fs'
import path from 'node:path'

const PORT = Number(process.env.CDP_PORT || 9223)
const BASE = 'http://192.168.68.1:5177'
const token = fs.readFileSync(path.join(import.meta.dirname, 'adm.token'), 'utf8').trim()

async function targets() {
  const res = await fetch(`http://127.0.0.1:${PORT}/json/list`)
  return res.json()
}

async function waitFor(fn, timeout = 20000, step = 300) {
  const at = Date.now()
  while (Date.now() - at < timeout) {
    try {
      const v = await fn()
      if (v) return v
    } catch {
      /* 页面还没就绪，继续等 */
    }
    await new Promise((r) => setTimeout(r, step))
  }
  throw new Error('waitFor timeout')
}

const list = await targets()
const page = list.find((t) => t.type === 'page')
if (!page) throw new Error('没找到可用的页面目标')

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

await send('Runtime.enable')
await send('Page.enable')

// 1) 先落到同源页面，把管理员令牌写进 localStorage
await send('Page.navigate', { url: `${BASE}/` })
await waitFor(async () => (await evaluate('document.readyState')) === 'complete', 30000)
await evaluate(`localStorage.setItem('dr:admin:token', ${JSON.stringify(token)});
localStorage.setItem('dr:admin:name', '执行部管理员');`)

// 2) 进后台，并切到「主题管理」
await send('Page.navigate', { url: `${BASE}/#/admin` })
await waitFor(async () => {
  const t = await evaluate('document.body.innerText || ""')
  return t.includes('后台管理系统')
}, 30000)

// 菜单是按钮，按文本点第二项「主题管理」
await evaluate(`(() => {
  const btns = [...document.querySelectorAll('.sb__item')]
  const target = btns.find((b) => b.innerText.includes('主题管理'))
  if (!target) return 'no-menu'
  target.click()
  return 'clicked'
})()`)

await waitFor(async () => (await evaluate('document.querySelectorAll(".th__item").length')) > 0, 30000)
await new Promise((r) => setTimeout(r, 800))

const report = await evaluate(`(() => {
  const items = [...document.querySelectorAll('.th__item')]
  return {
    menu: [...document.querySelectorAll('.sb__item')].map((b) => b.innerText.split('\\n')[0]),
    count: items.length,
    firstTitle: items[0]?.querySelector('.th__title')?.innerText || '',
    tags: items[0] ? [...items[0].querySelectorAll('.th__tag')].map((t) => t.innerText) : [],
    ops: items[0] ? [...items[0].querySelectorAll('.th__ops button')].map((b) => b.innerText) : [],
    pager: document.querySelector('.adm__pager')?.innerText.replace(/\\n/g, ' ') || '',
    bar: document.querySelector('.adm__sync')?.innerText.replace(/\\n/g, ' ') || '',
    hasError: !!document.querySelector('.adm__alert'),
    bodyLen: document.body.innerText.length
  }
})()`)

const shot = await send('Page.captureScreenshot', { format: 'png' })
const out = path.join(import.meta.dirname, 'admin-threads.png')
fs.writeFileSync(out, Buffer.from(shot.data, 'base64'))

console.log(JSON.stringify({ report, consoleErrors, screenshot: out }, null, 2))
ws.close()
