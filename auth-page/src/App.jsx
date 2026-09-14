import BrandPanel from './components/BrandPanel'
import AuthCard from './components/AuthCard'
import Logo from './components/Logo'

export default function App() {
  return (
    <main className="flex min-h-screen w-full bg-slate-50">
      {/* 桌面端左侧品牌展示区（移动端隐藏） */}
      <BrandPanel />

      {/* 右侧表单区：移动端占满，桌面端占一半并居中 */}
      <section className="flex w-full flex-col items-center justify-center px-4 py-10 sm:px-6 lg:w-1/2">
        <div className="mb-6 lg:hidden" aria-hidden="true">
          <Logo tone="dark" />
        </div>
        <AuthCard />
      </section>
    </main>
  )
}
