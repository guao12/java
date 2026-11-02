import { createServer } from 'vite'

const server = await createServer({
  server: { middlewareMode: true },
  appType: 'custom',
  logLevel: 'error'
})
const { ARCHIVE_MISSION_LOGS } = await server.ssrLoadModule('/src/data/archive.js')
const { logDetail, relicLink } = await server.ssrLoadModule('/src/data/logDetail.js')

const rows = []
const problems = []
for (const m of ARCHIVE_MISSION_LOGS) {
  const d = logDetail(m)
  const key = `${m.code}|${m.person}`
  const canon = (d.canon || '').length
  rows.push({
    key,
    lvl: d.level,
    steps: (d.steps || []).length,
    brief: (d.brief || []).length,
    canon,
    figs: (d.figures || []).length,
    relics: (d.relics || []).length
  })
  if (!d.canon) problems.push(`${key} 缺 canon`)
  else if (canon < 90) problems.push(`${key} canon 偏短 (${canon})`)
  if ((d.steps || []).length < 3) problems.push(`${key} steps 过少`)
  if ((d.brief || []).length < 2) problems.push(`${key} brief 过少`)
  if (!d.outcome?.result || !d.outcome?.cost) problems.push(`${key} outcome 不全`)
  if (!d.quote) problems.push(`${key} 缺 quote`)
  if (!d.date || !d.site || !d.op) problems.push(`${key} 元信息缺失`)
}

console.log('记录总数:', ARCHIVE_MISSION_LOGS.length)
for (const r of rows) {
  console.log(`  ${r.lvl} ${r.key.padEnd(28)} steps=${r.steps} brief=${r.brief} canon=${String(r.canon).padStart(3)} figs=${r.figs} relics=${r.relics}`)
}
console.log('\ncanon 平均:', Math.round(rows.reduce((s, r) => s + r.canon, 0) / rows.length), ' 问题数:', problems.length)
for (const p of problems) console.log('  !', p)

const names = [...new Set(ARCHIVE_MISSION_LOGS.flatMap((m) => logDetail(m).relics || []))]
const unresolved = names.map((n) => ({ n, r: relicLink(n) })).filter((x) => !x.r.kind)
console.log('\nrelics 去重:', names.length, ' 未解析:', unresolved.length, unresolved.map((x) => x.n).join(', '))

await server.close()
