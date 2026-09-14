import Logo from './Logo'

// 桌面端左侧品牌展示面板，仅在 lg 及以上显示。
export default function BrandPanel() {
  return (
    <aside
      className="relative hidden w-1/2 flex-col justify-between bg-brand-600 bg-gradient-to-br from-brand-600 to-brand-800 p-10 text-white lg:flex"
    >
      <Logo />
      <div className="max-w-md">
        <h2 className="text-3xl font-bold leading-tight">欢迎回来 👋</h2>
        <p className="mt-4 text-brand-100">
          一处登录，连接你的代码、仓库与自动化工作流。安全、简洁，随时随地继续你的创作。
        </p>
      </div>
      <p className="text-sm text-brand-200">© 2026 WorkBuddy. 保留所有权利。</p>
    </aside>
  )
}
