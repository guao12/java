// 主提交按钮：品牌色填充，带 hover / focus 态。
export default function SubmitButton({ children, ...props }) {
  return (
    <button
      type="submit"
      className="w-full rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
      {...props}
    >
      {children}
    </button>
  )
}
