// 品牌标识。tone="light" 用于深色背景，"dark" 用于浅色背景。
export default function Logo({ tone = 'light' }) {
  const text = tone === 'light' ? 'text-white' : 'text-slate-800'
  return (
    <div className="flex items-center gap-2">
      <span
        className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-base font-bold text-white"
        aria-hidden="true"
      >
        W
      </span>
      <span className={`text-lg font-semibold ${text}`}>WorkBuddy</span>
    </div>
  )
}
