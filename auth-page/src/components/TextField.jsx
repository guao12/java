import { forwardRef } from 'react'

// 通用文本输入：带 label、错误提示、aria 关联，支持传入 type/autoComplete 等。
const TextField = forwardRef(function TextField(
  { id, label, type = 'text', error, hint, ...props },
  ref,
) {
  const errorId = error ? `${id}-error` : undefined
  const hintId = hint ? `${id}-hint` : undefined
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        ref={ref}
        id={id}
        type={type}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={[errorId, hintId].filter(Boolean).join(' ') || undefined}
        className={`w-full rounded-lg border px-3.5 py-2.5 text-sm shadow-sm outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30 ${
          error ? 'border-red-400 focus:border-red-500 focus:ring-red-500/30' : 'border-slate-300'
        }`}
        {...props}
      />
      {error && (
        <p id={errorId} className="text-xs text-red-500">
          {error}
        </p>
      )}
    </div>
  )
})

export default TextField
