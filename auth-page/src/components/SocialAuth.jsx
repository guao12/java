// 第三方登录按钮组。
const providers = [
  { id: 'github', label: 'GitHub' },
  { id: 'google', label: 'Google' },
]

export default function SocialAuth() {
  return (
    <div className="grid grid-cols-2 gap-3">
      {providers.map((p) => (
        <a
          key={p.id}
          href={`#${p.id}`}
          aria-label={`使用 ${p.label} 继续`}
          className="flex items-center justify-center gap-2 rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
        >
          {p.label}
        </a>
      ))}
    </div>
  )
}
