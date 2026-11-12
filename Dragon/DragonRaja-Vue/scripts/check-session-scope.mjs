/**
 * 会话隔离回归验证（无需浏览器）
 *
 *   node scripts/check-session-scope.mjs
 *
 * 用 Vite 的 ssrLoadModule 加载真实的 utils/auth.js 与 utils/sessionScope.js，
 * 用内存对象模拟「多个标签页共享一份 localStorage」：
 *   - localStorage 全局一份（同一 origin 共享，这正是串号的根源）
 *   - sessionStorage 每个标签页一份（浏览器真实行为）
 * 每个标签页重新 import 一次 auth.js，等价于「一个独立的 JS 上下文 / 一次刷新」。
 *
 * 覆盖：旧实现复现、第二个账号自动分槽、刷新后各归各位、退出互不影响、
 *      ?as= 显式作用域、同账号重登、旧键迁移、会话级与持久级混用。
 */
import { createServer } from 'vite'

class MemStore {
  constructor(name) {
    this.name = name
    this.m = new Map()
  }
  get length() {
    return this.m.size
  }
  key(i) {
    return [...this.m.keys()][i] ?? null
  }
  getItem(k) {
    return this.m.has(k) ? this.m.get(k) : null
  }
  setItem(k, v) {
    this.m.set(k, String(v))
  }
  removeItem(k) {
    this.m.delete(k)
  }
  clear() {
    this.m.clear()
  }
  dump() {
    return Object.fromEntries(this.m)
  }
}

const local = new MemStore('local')
const tabs = [new MemStore('tab0'), new MemStore('tab1')]
let activeTab = 0
let loc = { search: '', hash: '#/forum' }

globalThis.window = {
  get localStorage() {
    return local
  },
  get sessionStorage() {
    return tabs[activeTab]
  },
  get location() {
    return loc
  }
}

const server = await createServer({
  server: { middlewareMode: true },
  appType: 'custom',
  logLevel: 'error'
})

let seq = 0
const scopeMod = await server.ssrLoadModule('/src/utils/sessionScope.js')

/** 切到某个「标签页」：换 sessionStorage + 清掉模块内存里缓存的作用域 */
function useTab(i, hash = '#/forum') {
  activeTab = i
  loc = { search: '', hash }
  scopeMod.resetScopeCache()
}

/** 重新加载 auth.js，等价于刷新页面（新的模块实例，同一份存储） */
function loadAuth() {
  seq += 1
  return server.ssrLoadModule(`/src/utils/auth.js?t=${seq}`)
}

let pass = 0
const fails = []
function ok(cond, label, extra = '') {
  if (cond) {
    pass += 1
    console.log(`  ✓ ${label}`)
  } else {
    fails.push(label)
    console.log(`  ✗ ${label}${extra ? `  → ${extra}` : ''}`)
  }
}
const section = (t) => console.log(`\n${t}`)

const A = { id: 1, account: 'CC20230001', codename: '路明非' }
const B = { id: 2, account: 'CC20230002', codename: '楚子航' }
const C = { id: 3, account: 'CC20230003', codename: '恺撒' }

section('0. 旧实现复现（证明诊断成立）')
const oldLocal = new MemStore('old-local')
const oldTab1 = new MemStore('old-tab1')
const oldTab2 = new MemStore('old-tab2')
const oldLogin = (tabStore, token, user) => {
  // 旧版 saveSession：先 clearSession() 清两个存储的全局键，再写一个
  for (const store of [oldLocal, tabStore]) {
    store.removeItem('dr:token')
    store.removeItem('dr:user')
  }
  oldLocal.setItem('dr:token', token)
  oldLocal.setItem('dr:user', JSON.stringify(user))
}
oldLogin(oldTab1, 'OLD_A', A)
oldLogin(oldTab2, 'OLD_B', B)
const oldTab1Read = oldTab1.getItem('dr:token') ?? oldLocal.getItem('dr:token')
ok(oldTab1Read === 'OLD_B', '旧实现下，标签页 1 读到的是 B 的令牌（复现成功）', oldTab1Read)

section('1. 标签页 1 登录账号 A')
useTab(0)
let auth = await loadAuth()
const scopeA = auth.saveSession({ token: 'TOKEN_A', user: A }, true)
ok(auth.getToken() === 'TOKEN_A', '标签页 1 拿到 A 的令牌')
ok(scopeA === 'default', `首账号落在默认槽（实际 ${scopeA}）`)

section('2. 标签页 2（同一 origin）登录账号 B')
useTab(1)
auth = await loadAuth()
ok(auth.getToken() === 'TOKEN_A', '新标签页初始沿用最近作用域，能看到 A 的会话（预期）')
const scopeB = auth.saveSession({ token: 'TOKEN_B', user: B }, true)
ok(scopeB !== scopeA, `第二个账号被分到独立槽 ${scopeB}，没有覆盖 A`)
ok(auth.getToken() === 'TOKEN_B', '标签页 2 拿到 B 的令牌')

section('3. 回到标签页 1 —— 不被覆盖（原始 bug 的核心断言）')
useTab(0)
auth = await loadAuth()
ok(auth.getToken() === 'TOKEN_A', '标签页 1 仍是 A 的令牌', String(auth.getToken()))
ok(auth.getUser()?.codename === '路明非', '标签页 1 用户信息仍是路明非')
auth = await loadAuth()
ok(auth.getToken() === 'TOKEN_A', '标签页 1 刷新后仍是 A')

section('4. 标签页 2 刷新后仍是 B')
useTab(1)
auth = await loadAuth()
ok(auth.getToken() === 'TOKEN_B', '标签页 2 刷新后仍是 B', String(auth.getToken()))
ok(auth.getUser()?.codename === '楚子航', '标签页 2 用户信息仍是楚子航')

section('5. 三份令牌在 localStorage 里互不相交')
const dump = local.dump()
const tokenKeys = Object.keys(dump).filter((k) => k.endsWith(':token'))
ok(tokenKeys.length >= 2, `localStorage 中存在 ${tokenKeys.length} 个作用域令牌键`)
ok(dump['dr:s:default:token'] === 'TOKEN_A', '默认槽是 A')
ok(!dump['dr:token'], '旧的全局键已不存在')

section('6. 标签页 2 退出，不影响标签页 1')
useTab(1)
auth = await loadAuth()
auth.clearSession()
ok(auth.getToken() === null, '标签页 2 已退出')
useTab(0)
auth = await loadAuth()
ok(auth.getToken() === 'TOKEN_A', '标签页 1 仍在线（旧版这里会被一起踢下线）')

section('7. ?as= 显式作用域：一个网址绑定一个账号')
tabs.push(new MemStore('tab2'))
useTab(2, '#/forum?as=student')
auth = await loadAuth()
ok(auth.currentScopeName() === 'student', '?as=student 生效')
auth.saveSession({ token: 'TOKEN_C', user: C }, true)
ok(auth.getToken() === 'TOKEN_C', '学生槽拿到 C 的令牌')
loc = { search: '', hash: '#/archive' }
ok(auth.currentScopeName() === 'student', '离开带 ?as= 的地址后，本标签页仍绑定该槽')
ok(auth.getToken() === 'TOKEN_C', '仍能读到 C 的令牌')

section('8. 同一账号重新登录：不新开槽')
useTab(0)
auth = await loadAuth()
const again = auth.saveSession({ token: 'TOKEN_A2', user: A }, true)
ok(again === 'default', `重登同账号仍落在 default（实际 ${again}）`)
ok(auth.getToken() === 'TOKEN_A2', '令牌已刷新')

section('9. 旧版本全局键自动迁移')
local.clear()
tabs.forEach((t) => t.clear())
useTab(0)
local.setItem('dr:token', 'LEGACY_TOKEN')
local.setItem('dr:user', JSON.stringify({ id: 9, codename: '旧账号' }))
auth = await loadAuth()
ok(auth.getToken() === 'LEGACY_TOKEN', '旧令牌被接管，用户无需重登')
ok(auth.getUser()?.codename === '旧账号', '旧用户信息一并迁移')
ok(local.getItem('dr:token') === null, '旧键已清理，不会二次迁移')

section('10. remember=false 与 remember=true 混用不互相清空')
local.clear()
tabs[0].clear()
useTab(0)
auth = await loadAuth()
auth.saveSession({ token: 'SESSION_ONLY', user: A }, false)
ok(auth.getToken() === 'SESSION_ONLY', '仅会话级登录可读')
tabs.push(new MemStore('tab3'))
useTab(3)
auth = await loadAuth()
auth.saveSession({ token: 'PERSIST_B', user: B }, true)
useTab(0)
auth = await loadAuth()
ok(auth.getToken() === 'SESSION_ONLY', '持久登录没有覆盖另一个标签页的会话级登录', String(auth.getToken()))

console.log(`\n结果：${pass} 通过，${fails.length} 失败`)
if (fails.length) {
  console.log('失败项：')
  fails.forEach((f) => console.log(`  - ${f}`))
}
await server.close()
process.exit(fails.length ? 1 : 0)
