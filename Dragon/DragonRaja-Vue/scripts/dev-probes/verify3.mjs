import { spawn } from 'node:child_process'
import { setTimeout as sleep } from 'node:timers/promises'

const PORT = 9333
const CHROME = 'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe'
const BASE = 'http://127.0.0.1:5177/'

const child = spawn(CHROME, [
  `--remote-debugging-port=${PORT}`,
  '--headless=new',
  '--disable-gpu',
  '--no-first-run',
  '--no-default-browser-check',
  '--user-data-dir=D:/workSpace/java/Dragon/DragonRaja-Vue/.cdp-prof',
  'about:blank'
], { stdio: 'ignore', detached: false })

let ws, id = 0
const pending = new Map()
const logs = []

function send(method, params = {}) {
  const mid = ++id
  ws.send(JSON.stringify({ id: mid, method, params }))
  return new Promise((res, rej) => {
    pending.set(mid, { res, rej })
    setTimeout(() => { if (pending.has(mid)) { pending.delete(mid); rej(new Error('timeout ' + method)) } }, 25000)
  })
}

async function evaluate(expression) {
  const r = await send('Runtime.evaluate', {
    expression, returnByValue: true, awaitPromise: true, userGesture: true
  })
  if (r.exceptionDetails) {
    throw new Error('EVAL: ' + (r.exceptionDetails.exception?.description || r.exceptionDetails.text))
  }
  return r.result?.value
}

async function navigate(url) {
  await send('Page.navigate', { url })
  await sleep(2200)
}

async function main() {
  let targets
  for (let i = 0; i < 40; i++) {
    try {
      targets = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
      if (targets.some(t => t.type === 'page')) break
    } catch { /* not up yet */ }
    await sleep(400)
  }
  const page = targets.find(t => t.type === 'page')
  ws = new WebSocket(page.webSocketDebuggerUrl)
  await new Promise(r => { ws.onopen = r })

  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data)
    if (msg.id && pending.has(msg.id)) {
      const { res, rej } = pending.get(msg.id); pending.delete(msg.id)
      msg.error ? rej(new Error(JSON.stringify(msg.error))) : res(msg.result)
      return
    }
    if (msg.method === 'Runtime.consoleAPICalled') {
      const lvl = msg.params.type
      const text = (msg.params.args || []).map(a => a.value ?? a.description ?? '').join(' ')
      if (lvl === 'error' || lvl === 'warning') logs.push(`[${lvl}] ${text}`)
    }
    if (msg.method === 'Runtime.exceptionThrown') {
      logs.push('[exception] ' + (msg.params.exceptionDetails.exception?.description || msg.params.exceptionDetails.text))
    }
  }

  await send('Page.enable')
  await send('Runtime.enable')
  await send('Log.enable')
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false })

  // 播种登录态 + 满级密级（key 带账号后缀，见 clearance.js storeKey）
  await navigate(BASE + '#/login')
  await evaluate(`
    localStorage.setItem('dr:token','cdp-verify-token');
    localStorage.setItem('dr:user', JSON.stringify({account:'cdpverify',name:'验证员',id:1}));
    localStorage.setItem('dr:clearance:cdpverify', JSON.stringify({points:99,completed:[],records:[]}));
    'seeded'
  `)

  const out = {}

  // ---------- 1. 档案室 → 任务执行记录 卷 + 「查看详情」入口 ----------
  await navigate(BASE + '#/archive#tab-logs')
  await sleep(1200)
  out.archive = await evaluate(`
    (() => {
      const btns = [...document.querySelectorAll('.log__more')]
      const onTab = !!document.querySelector('.shelf__title')
      return {
        activeTab: document.querySelector('.tab.is-on')?.textContent.trim() || '',
        moreCount: btns.length,
        firstMoreText: btns[0]?.textContent.trim() || '',
        firstCardCode: document.querySelector('.log__code')?.textContent.trim() || ''
      }
    })()
  `)

  // 点第一个「查看详情」，看实际跳到哪
  await evaluate(`document.querySelector('.log__more').click(); 'clicked'`)
  await sleep(1800)
  out.jump = await evaluate(`
    (() => ({
      hash: location.hash,
      title: document.title,
      h1: document.querySelector('.dossier__name')?.textContent.trim() || '',
      code: document.querySelector('.dossier__code-chip')?.textContent.trim() || '',
      secs: [...document.querySelectorAll('.dossier__sec-t')].map(e => e.textContent.trim())
    }))()
  `)

  // ---------- 2. 指定记录详情：EXE-4521 / 楚子航 ----------
  await navigate(BASE + '#/log/EXE-4521/' + encodeURIComponent('楚子航'))
  await sleep(1400)
  out.detail = await evaluate(`
    (() => {
      const q = s => document.querySelector(s)
      const txt = s => q(s)?.textContent.trim() || ''
      return {
        hash: decodeURIComponent(location.hash),
        title: document.title,
        name: txt('.dossier__name'),
        op: txt('.dossier__op'),
        role: txt('.dossier__role').replace(/\\s+/g,' '),
        rank: txt('.dossier__rank b'),
        level: txt('.dossier__level'),
        meta: [...document.querySelectorAll('.meta > div')].map(d => d.textContent.trim().replace(/\\s+/g,' ')),
        brief: [...document.querySelectorAll('.brief li')].map(l => l.textContent.trim()),
        canonLen: txt('.canon').length,
        canonHead: txt('.canon').slice(0, 60),
        steps: document.querySelectorAll('.step').length,
        outcome: [...document.querySelectorAll('.outcome__row')].map(r => r.textContent.trim().replace(/\\s+/g,' ')),
        figs: [...document.querySelectorAll('.fig')].map(f => f.textContent.trim().replace(/\\s+/g,' ')),
        quote: txt('.dossier__quote').replace(/\\s+/g,' ').slice(0, 80),
        same: [...document.querySelectorAll('.same__who')].map(e => e.textContent.trim()),
        bodyScrollWidthOk: document.documentElement.scrollWidth <= window.innerWidth + 1
      }
    })()
  `)

  await send('Page.captureScreenshot', { format: 'png' }).then(async (r) => {
    const fs = await import('node:fs')
    fs.writeFileSync('D:/workSpace/java/Dragon/DragonRaja-Vue/.cdp-prof/log-4521.png', Buffer.from(r.data, 'base64'))
  })

  // ---------- 3. 「返回档案室」回到 logs 卷 ----------
  await evaluate(`document.querySelector('.logd__back').click(); 'back'`)
  await sleep(1600)
  out.back = await evaluate(`
    (() => ({
      hash: location.hash,
      activeTab: document.querySelector('.tab.is-on')?.textContent.trim() || '',
      moreCount: document.querySelectorAll('.log__more').length
    }))()
  `)

  // ---------- 4. 密级不足：换一个零权限账号，访问机密记录 ----------
  // 注意：clearance 的 state 是模块级单例，ensure() 只在账号变化时重读 localStorage，
  // 所以必须换账号（而不是原地改 points）才能真正触发重载。
  await evaluate(`
    localStorage.setItem('dr:user', JSON.stringify({account:'novice',name:'见习员',id:2}));
    localStorage.setItem('dr:clearance:novice', JSON.stringify({points:0,completed:[],records:[]}));
    'switch-account'
  `)
  await navigate(BASE + '#/log/EXE-4521/' + encodeURIComponent('楚子航'))
  await sleep(1500)
  out.locked = await evaluate(`
    (() => ({
      hasArticle: !!document.querySelector('.dossier'),
      voidText: document.querySelector('.dossier--void')?.textContent.replace(/\\s+/g,' ').trim() || '',
      leaks: document.querySelectorAll('.canon, .step, .outcome, .brief').length,
      levelBadge: document.querySelector('.arc__lv, .nav__lv, [data-level]')?.textContent.trim() || ''
    }))()
  `)

  // 恢复满级账号
  await evaluate(`
    localStorage.setItem('dr:user', JSON.stringify({account:'cdpverify',name:'验证员',id:1}));
    'restore'
  `)

  // ---------- 5. 言灵详情页「放大」按钮稳定性 ----------
  await navigate(BASE + '#/spirit/junyan')
  await sleep(1600)
  out.zoom = await evaluate(`
    (() => {
      const frame = document.querySelector('.dossier__avatar')
      const btn = document.querySelector('.dossier__avatar-btn')
      const hint = document.querySelector('.dossier__avatar-zoom')
      if (!frame || !btn || !hint) return { missing: true, frame: !!frame, btn: !!btn, hint: !!hint }
      const f = frame.getBoundingClientRect()
      const cs = getComputedStyle(hint)
      // 采样框内 3x3 网格，检查每一点命中的元素是否还能回溯到按钮
      const hits = []
      for (let ix = 0; ix < 3; ix++) {
        for (let iy = 0; iy < 3; iy++) {
          const x = f.left + f.width * (0.05 + ix * 0.45)
          const y = f.top + f.height * (0.05 + iy * 0.45)
          const el = document.elementFromPoint(x, y)
          hits.push({ ix, iy, tag: el?.tagName, cls: el?.className?.baseVal ?? el?.className,
                      inBtn: !!(el && btn.contains(el)) || el === btn })
        }
      }
      return {
        frame: { w: Math.round(f.width), h: Math.round(f.height) },
        hintOpacity: cs.opacity,
        hintPointerEvents: cs.pointerEvents,
        hintVisibleNoHover: Number(cs.opacity) > 0,
        hits
      }
    })()
  `)

  // 全屏截图留档
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false })
  const shot = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true })
  const fs = await import('node:fs')
  fs.writeFileSync('D:/workSpace/java/Dragon/DragonRaja-Vue/.cdp-prof/log-detail.png', Buffer.from(shot.data, 'base64'))

  out.console = logs
  console.log(JSON.stringify(out, null, 2))

  ws.close()
  child.kill()
}

main().catch(e => { console.error('FATAL', e); try { child.kill() } catch {} process.exit(1) })
