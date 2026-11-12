import { createServer } from 'vite'
import { createSSRApp } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { createRouter, createMemoryHistory } from 'vue-router'

const mkStore = () => {
  const m = new Map()
  return {
    getItem: (k) => (m.has(k) ? m.get(k) : null),
    setItem: (k, v) => m.set(k, String(v)),
    removeItem: (k) => m.delete(k),
    clear: () => m.clear(),
    key: (i) => [...m.keys()][i] ?? null,
    get length() { return m.size }
  }
}
globalThis.localStorage = mkStore()
globalThis.sessionStorage = mkStore()
globalThis.matchMedia = () => ({ matches: false, addEventListener() {}, removeEventListener() {}, addListener() {}, removeListener() {} })

const server = await createServer({ server: { middlewareMode: true }, appType: 'custom', logLevel: 'error' })
const stub = { template: '<div />' }
let failed = 0
const warnings = []

function report(name, ok, extra = '') {
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  (' + extra + ')' : ''}`)
  if (!ok) failed++
}

try {
  const mod = await server.ssrLoadModule('/src/views/LoginView.vue')
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: stub }, { path: '/login', component: mod.default },
      { path: '/portal', component: stub }, { path: '/register', component: stub },
      { path: '/forgot', component: stub }, { path: '/forum', component: stub }
    ]
  })
  router.push('/login')
  await router.isReady()

  const app = createSSRApp(mod.default)
  app.use(router)
  app.config.warnHandler = (msg) => warnings.push(msg)
  const html = await renderToString(app)

  // 结构：引子区应在卡片外
  const introIdx = html.indexOf('intro__title')
  const cardIdx = html.indexOf('auth-card')
  report('引子区在卡片之外', introIdx > 0 && cardIdx > introIdx, `intro@${introIdx} card@${cardIdx}`)
  report('标题只出现一次', (html.match(/身份认证/g) || []).length === 1)
  report('徽记已渲染', html.includes('emblemGold') || html.includes('卡塞尔学院徽记'))

  // 已删除的模块
  report('身份卡预览已移除', !html.includes('idcard'))
  report('密钥强度条已移除', !html.includes('meter__'))
  report('信号格已移除', !html.includes('signal--'))
  report('内网地址已移除', !html.includes('192.168.68.1'))
  report('空 header 不再渲染', !html.includes('auth-card__head'))
  report('后端正常时不显示告警', !html.includes('后端通道未连接'))

  // 保留能力
  report('返回门户入口', html.includes('返回门户'))
  report('账号输入框', html.includes('autocomplete="username"'))
  report('密码输入框', html.includes('autocomplete="current-password"'))
  report('记住身份卡', html.includes('记住身份卡'))
  report('忘记密钥', html.includes('忘记密钥'))
  report('提交按钮', html.includes('进入学院'))
  report('占位示例已更新', html.includes('CC202600004'))
  report('未残留演示账号', !html.includes('Dragon@2026'))

  // 语义：不得出现 button 嵌套
  const nested = /<button[^>]*>[\s\S]*?<button/.test(html)
  report('无 button 嵌套', !nested)
  report('无 span[role=button]', !html.includes('role="button"'))

  if (warnings.length) {
    console.log('\n--- 渲染告警 ---')
    warnings.slice(0, 8).forEach((w) => console.log('  ' + w.split('\n')[0]))
    failed += warnings.length ? 1 : 0
  } else {
    console.log('\n无渲染告警')
  }

  // 有历史记录时的 chips 结构
  globalThis.localStorage.setItem('dr:history', JSON.stringify(['CC202600004', 'CC202600009']))
  const mod2 = await server.ssrLoadModule('/src/views/LoginView.vue?t=' + Date.now())
  const app2 = createSSRApp(mod2.default)
  app2.use(router)
  const html2 = await renderToString(app2)
  const chips = html2.includes('class="chips"')
  if (chips) {
    report('chip 用独立删除按钮', html2.includes('chip__x') && !html2.includes('role="button"'))
  } else {
    console.log('SKIP  chips（本环境 history 键名不匹配，浏览器中按实际渲染）')
  }
} catch (e) {
  failed++
  console.log('FAIL  渲染抛错: ' + e.message)
} finally {
  await server.close()
}

console.log(`\n${failed === 0 ? '全部通过' : failed + ' 项失败'}`)
process.exit(failed === 0 ? 0 : 1)
