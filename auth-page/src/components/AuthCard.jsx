import { useState } from 'react'
import AuthTabs from './AuthTabs'
import TextField from './TextField'
import PasswordField from './PasswordField'
import SubmitButton from './SubmitButton'
import Divider from './Divider'
import SocialAuth from './SocialAuth'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// 登录 / 注册 主卡片：管理模式、表单值与校验。
export default function AuthCard() {
  const [mode, setMode] = useState('login') // 'login' | 'register'
  const [values, setValues] = useState({ name: '', email: '', password: '', confirm: '' })
  const [errors, setErrors] = useState({})

  const update = (key) => (e) => setValues((v) => ({ ...v, [key]: e.target.value }))

  const validate = () => {
    const next = {}
    if (mode === 'register' && !values.name.trim()) next.name = '请输入昵称'
    if (!EMAIL_RE.test(values.email)) next.email = '请输入有效的邮箱地址'
    if (values.password.length < 8) next.password = '密码至少 8 位'
    if (mode === 'register' && values.confirm !== values.password) next.confirm = '两次输入的密码不一致'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!validate()) return
    // 这里替换为真实接口调用 / 连接器逻辑
    // eslint-disable-next-line no-alert
    alert(mode === 'login' ? '提交登录' : '提交注册')
  }

  const switchMode = (next) => {
    setMode(next)
    setErrors({})
  }

  return (
    <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl shadow-slate-200/60 sm:p-8">
      <AuthTabs mode={mode} onChange={switchMode} />

      <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
        {mode === 'register' && (
          <TextField
            id="name"
            label="昵称"
            placeholder="你的名字"
            autoComplete="name"
            value={values.name}
            onChange={update('name')}
            error={errors.name}
          />
        )}

        <TextField
          id="email"
          label="邮箱"
          type="email"
          placeholder="you@example.com"
          autoComplete="email"
          value={values.email}
          onChange={update('email')}
          error={errors.email}
        />

        <PasswordField
          id="password"
          label="密码"
          placeholder="至少 8 位"
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          value={values.password}
          onChange={update('password')}
          error={errors.password}
          forgot={mode === 'login' ? '#' : undefined}
        />

        {mode === 'register' && (
          <PasswordField
            id="confirm"
            label="确认密码"
            placeholder="再次输入密码"
            autoComplete="new-password"
            value={values.confirm}
            onChange={update('confirm')}
            error={errors.confirm}
          />
        )}

        {mode === 'login' && (
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
            />
            记住我
          </label>
        )}

        <SubmitButton>{mode === 'login' ? '登录' : '创建账号'}</SubmitButton>
      </form>

      <div className="mt-6">
        <Divider />
        <div className="mt-4">
          <SocialAuth />
        </div>
      </div>

      <p className="mt-6 text-center text-sm text-slate-500">
        {mode === 'login' ? '还没有账号？' : '已经有账号了？'}{' '}
        <button
          type="button"
          onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}
          className="font-medium text-brand-600 hover:text-brand-700 focus:outline-none focus-visible:underline"
        >
          {mode === 'login' ? '立即注册' : '去登录'}
        </button>
      </p>
    </div>
  )
}
