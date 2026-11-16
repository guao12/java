// 校验 logDetail.js 对全部 22 条 ARCHIVE_MISSION_LOGS 的覆盖与篇幅
import { ARCHIVE_MISSION_LOGS } from '../src/data/archive.js'
import { logDetail } from '../src/data/logDetail.js'

const rows = []
const problems = []

for (const m of ARCHIVE_MISSION_LOGS) {
  const d = logDetail(m)
  const key = `${m.code}|${m.person}`
  const steps = d.steps?.length || 0
  const brief = d.brief?.length || 0
  const canon = (d.canon || '').length
  rows.push({ key, name: d.name, lvl: d.level, steps, brief, canon, figs: (d.figures||[]).length, relics: (d.relics||[]).length })

  if (!d.canon) problems.push(`${key} 缺 canon`)
  else if (canon < 90) problems.push(`${key} canon 偏短 (${canon})`)
  if (steps < 3) problems.push(`${key} steps 过少 (${steps})`)
  if (brief < 2) problems.push(`${key} brief 过少 (${brief})`)
  if (!d.outcome?.result || !d.outcome?.cost) problems.push(`${key} outcome 不全`)
  if (!d.quote) problems.push(`${key} 缺 quote`)
  if (!d.date || !d.site || !d.op) problems.push(`${key} 元信息缺失`)
}

console.log('记录总数:', ARCHIVE_MISSION_LOGS.length)
for (const r of rows) {
  console.log(
    `  ${r.lvl}  ${r.key.padEnd(30)} steps=${r.steps} brief=${r.brief} canon=${String(r.canon).padStart(3)} figs=${r.figs} relics=${r.relics}`
  )
}
console.log('\ncanon 平均长度:', Math.round(rows.reduce((s, r) => s + r.canon, 0) / rows.length))
console.log('问题数:', problems.length)
for (const p of problems) console.log('  !', p)
