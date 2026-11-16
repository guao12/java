/**
 * 用 CDP 打开登录页，确认身份卡上的「血统等级」块已不再渲染，并截图。
 *
 * 判据：.idcard__grade 元素数量必须为 0（空表单 + 填满表单两种状态下都查一遍），
 * 同时身份卡本身还在、控制台无报错 —— 防止「等级块删了但整块卡片一起崩了」。
 *
 * 用法：node login-check.mjs
 */
import fs from 'node:fs'
import path from 'node:path'

const PORT = Number(process.env.CDP_PORT || 9337)
const BASE = 'http://192.168.68.1:5177'

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

const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
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
await send('Page.navigate', { url: `${BASE}/#/login` })
await waitFor(async () => (await evaluate('document.body.innerText || ""')).includes('身份认证'), 30000)
await waitFor(async () => (await evaluate('document.querySelectorAll(".idcard").length')) > 0, 20000)

const empty = await evaluate(`(() => ({
  cards: document.querySelectorAll('.idcard').length,
  grade: document.querySelectorAll('.idcard__grade').length,
  cardText: document.querySelector('.idcard').innerText.replace(/\\n/g, ' | ')
}))()`)

// 填满表单后再查一遍：等级块原本是随密码强度变化的，最容易被漏掉的就是这个状态
await evaluate(`(() => {
  const inputs = [...document.querySelectorAll('.form input')]
  const fill = (el, val) => {
    el.value = val
    el.dispatchEvent(new Event('input', { bubbles: true }))
    el.dispatchEvent(new Event('change', { bubbles: true }))
  }
  if (inputs[0]) fill(inputs[0], 'CC20260001')
  if (inputs[1]) fill(inputs[1], 'Dragon@2026')
  return inputs.length
})()`)
await new Promise((r) => setTimeout(r, 900))

const filled = await evaluate(`(() => ({
  grade: document.querySelectorAll('.idcard__grade').length,
  cardText: document.querySelector('.idcard').innerText.replace(/\\n/g, ' | '),
  hasBloodWord: (document.querySelector('.idcard').innerText || '').includes('血统')
}))()`)

const shot = await send('Page.captureScreenshot', { format: 'png' })
const out = path.join(import.meta.dirname, 'login-card.png')
fs.writeFileSync(out, Buffer.from(shot.data, 'base64'))

console.log(JSON.stringify({ empty, filled, consoleErrors, screenshot: out }, null, 2))
ws.close()
