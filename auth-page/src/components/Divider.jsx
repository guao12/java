// 分隔线：用 role="separator" 提供语义。
export default function Divider({ text = '或' }) {
  return (
    <div className="flex items-center gap-3" role="separator" aria-label={text}>
      <span className="h-px flex-1 bg-slate-200" />
      <span className="text-xs uppercase tracking-wide text-slate-400">{text}</span>
      <span className="h-px flex-1 bg-slate-200" />
    </div>
  )
}
