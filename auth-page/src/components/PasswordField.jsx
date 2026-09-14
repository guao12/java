import { forwardRef, useState } from 'react'

// 密码输入：带显示/隐藏切换、忘记密码链接、错误提示与 aria 关联。
const PasswordField = forwardRef(function PasswordField(
  { id, label, error, forgot, ...props },
  ref,
) {
  const [visible, setVisible] = useState(false)
  const errorId = error ? `${id}-error` : undefined
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center justify-between">
        <label htmlFor={id} className="text-sm font-medium text-slate-700">
          {label}
        </label>
        {forgot && (
          <a
            href={forgot}
            className="text-xs font-medium text-brand-600 hover:text-brand-700 focus:outline-none focus-visible:underline"
          >
            忘记密码？
          </a>
        )}
      </div>
      <div className="relative">
        <input
          ref={ref}
          id={id}
          type={visible ? 'text' : 'password'}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={errorId}
          className={`w-full rounded-lg border px-3.5 py-2.5 pr-16 text-sm shadow-sm outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30 ${
            error ? 'border-red-400 focus:border-red-500 focus:ring-red-500/30' : 'border-slate-300'
          }`}
          {...props}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? '隐藏密码' : '显示密码'}
          className="absolute inset-y-0 right-0 flex items-center px-3 text-xs font-medium text-slate-400 hover:text-slate-600 focus:outline-none focus-visible:text-brand-600"
        >
          {visible ? '隐藏' : '显示'}
        </button>
      </div>
      {error && (
        <p id={errorId} className="text-xs text-red-500">
          {error}
        </p>
      )}
    </div>
  )
})

export default PasswordField
