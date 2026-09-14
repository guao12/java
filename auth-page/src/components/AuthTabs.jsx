// 登录 / 注册 切换标签，使用 role="tablist" 提供语义与可访问性。
export default function AuthTabs({ mode, onChange }) {
  const tabs = [
    { id: 'login', label: '登录' },
    { id: 'register', label: '注册' },
  ]
  return (
    <div
      className="flex rounded-lg bg-slate-100 p-1"
      role="tablist"
      aria-label="选择登录或注册"
    >
      {tabs.map((t) => (
        <button
          key={t.id}
          type="button"
          role="tab"
          aria-selected={mode === t.id}
          onClick={() => onChange(t.id)}
          className={`flex-1 rounded-md px-4 py-2 text-sm font-medium transition focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 ${
            mode === t.id
              ? 'bg-white text-brand-700 shadow-sm'
              : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          {t.label}
        </button>
      ))}
    </div>
  )
}
