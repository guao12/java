<template>
  <transition name="reg" mode="out-in">
    <!-- 成功面板 -->
    <div v-if="done" class="reg-success" key="success">
      <div class="reg-check">✓</div>
      <div class="reg-s-title">入队认证成功</div>
      <div class="reg-s-sub">欢迎加入创骑战队，<b>{{ knightName }}</b>！</div>
      <button type="button" class="reg-s-btn" @click="$emit('switch')">前往登录 →</button>
    </div>

    <!-- 注册表单 -->
    <form v-else class="reg-form" key="form" @submit.prevent="onSubmit" autocomplete="off" novalidate>
      <div class="reg-head">
        <div class="reg-badge">⚔</div>
        <div class="reg-head-text">
          <div class="reg-title">骑士入队认证</div>
          <div class="reg-sub">填写信息，加入创骑战队</div>
        </div>
      </div>

      <div class="field">
        <label for="rname"><span class="dot r"></span>骑士名 / 编号</label>
        <input id="rname" v-model.trim="knightName" type="text" placeholder="给自己起一个骑士名"
               maxlength="20"
               :class="state('knightName')"
               @blur="touch('knightName')" @input="touchLive('knightName')" />
        <p v-if="touched.knightName && errors.knightName" class="err">{{ errors.knightName }}</p>
      </div>

      <div class="field">
        <label for="remail"><span class="dot b"></span>召集令邮箱</label>
        <input id="remail" v-model.trim="email" type="email" placeholder="you@example.com"
               :class="state('email')"
               @blur="touch('email')" @input="touchLive('email')" />
        <p v-if="touched.email && errors.email" class="err">{{ errors.email }}</p>
      </div>

      <div class="field">
        <label for="rpwd"><span class="dot r"></span>变身密码</label>
        <div class="pw">
          <input id="rpwd" v-model="password"
                 :type="showPwd ? 'text' : 'password'" placeholder="至少 6 位，建议含字母与数字"
                 :class="state('password')"
                 @blur="touch('password')" @input="touchLive('password')" />
          <button type="button" class="pw-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</button>
        </div>
        <div class="strength" v-if="password">
          <div class="strength-bar"><span :class="'lv' + strength.lv" :style="{ width: strength.pct + '%' }"></span></div>
          <div class="strength-label" :class="'t' + strength.lv">强度：{{ strength.label }}</div>
        </div>
        <p v-if="touched.password && errors.password" class="err">{{ errors.password }}</p>
      </div>

      <div class="field">
        <label for="rconfirm"><span class="dot b"></span>确认变身密码</label>
        <div class="pw">
          <input id="rconfirm" v-model="confirm"
                 :type="showPwd ? 'text' : 'password'" placeholder="再次输入密码"
                 :class="state('confirm')"
                 @blur="touch('confirm')" @input="touchLive('confirm')" />
        </div>
        <p v-if="touched.confirm && errors.confirm" class="err">{{ errors.confirm }}</p>
      </div>

      <label class="agree">
        <input type="checkbox" v-model="agree" @change="touchLive('agree')" />
        我已阅读并同意<a href="#" @click.prevent="pactTip">《骑士契约》</a>
      </label>
      <p v-if="touched.agree && errors.agree" class="err">{{ errors.agree }}</p>

      <button type="submit" class="henshin reg-submit" :disabled="loading">
        {{ loading ? '正在上报骑士总部…' : '最佳搭配 · 入队认证' }}
      </button>

      <div class="msg" :class="msgClass">{{ msgText }}</div>

      <p class="switch">
        已经是骑士了？<a href="#" @click.prevent="$emit('switch')">去登录</a>
      </p>
    </form>
  </transition>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
// 后端接口（Spring Boot，经 Vite 代理转发到 http://localhost:8081）
import { register as apiRegister } from '../api/user.js'

const props = defineProps({
  // 当前选择的形态，由父组件 App.vue 传入，注册时一并提交给后端
  formKey: { type: String, default: 'rabbittank' }
})

const emit = defineEmits(['switch', 'success'])

const knightName = ref('')
const email = ref('')
const password = ref('')
const confirm = ref('')
const agree = ref(false)
const showPwd = ref(false)
const done = ref(false)
const loading = ref(false)

const errors = reactive({})
const touched = reactive({})
const msgText = ref('')
const msgClass = ref('')

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// 密码强度：长度 + 字母数字组合 + 特殊字符
const strength = computed(() => {
  const v = password.value
  if (!v) return { lv: 0, pct: 0, label: '' }
  let s = 0
  if (v.length >= 6) s++
  if (v.length >= 10) s++
  if (/[a-zA-Z]/.test(v) && /[0-9]/.test(v)) s++
  if (/[^a-zA-Z0-9]/.test(v)) s++
  const lv = Math.min(s + 1, 4)
  return { lv, pct: [0, 30, 55, 80, 100][lv], label: ['', '弱', '中', '强', '极强'][lv] }
})

function checkAll() {
  const e = {}
  if (!knightName.value) e.knightName = '骑士名不能为空'
  else if (knightName.value.length < 2) e.knightName = '骑士名至少 2 个字符'
  if (!EMAIL_RE.test(email.value)) e.email = '请输入有效的召集令邮箱'
  if (password.value.length < 6) e.password = '变身密码至少 6 位'
  if (!confirm.value) e.confirm = '请再次输入变身密码'
  else if (confirm.value !== password.value) e.confirm = '两次输入的密码不一致'
  if (!agree.value) e.agree = '请先同意《骑士契约》'
  return e
}

function syncErrors(onlyTouched) {
  const e = checkAll()
  Object.keys(errors).forEach(k => delete errors[k])
  const keys = onlyTouched ? Object.keys(touched) : Object.keys(e)
  keys.forEach(k => { if (e[k]) errors[k] = e[k] })
}

function touch(name) {
  touched[name] = true
  syncErrors(true)
}

// 输入时若已触碰过该字段，则实时纠正错误提示
function touchLive(name) {
  if (touched[name]) syncErrors(true)
}

// 输入框有效 / 无效 视觉态
function state(name) {
  if (touched[name] && errors[name]) return 'invalid'
  if (touched[name] && !errors[name] && (name === 'knightName' ? knightName.value : name === 'email' ? email.value : name === 'password' ? password.value : confirm.value)) return 'valid'
  return ''
}

function setMsg(text, cls) {
  msgText.value = text
  msgClass.value = cls || ''
}

function pactTip() {
  setMsg('《骑士契约》：守护和平，善用最佳搭配之力。', 'ok')
}

// 提交注册：真正调用后端 POST /api/user/register
async function onSubmit() {
  if (loading.value) return

  ;['knightName', 'email', 'password', 'confirm', 'agree'].forEach(k => (touched[k] = true))
  syncErrors(false)
  if (Object.keys(errors).length) {
    setMsg('请检查表单后再提交', 'err')
    return
  }

  loading.value = true
  setMsg('正在上报骑士总部…', '')

  try {
    await apiRegister({
      username: knightName.value,
      email: email.value,
      password: password.value,
      confirmPassword: confirm.value,
      formKey: props.formKey
    })
    done.value = true
    setMsg('入队认证成功！', 'ok')
    emit('success', knightName.value)
  } catch (e) {
    // 直接把后端提示展示出来，例如「骑士名已被占用」「邮箱已被注册」
    setMsg(e.message, 'err')
  } finally {
    loading.value = false
  }
}

// 任一字段变化且已触碰，则实时刷新错误
watch([knightName, email, password, confirm, agree], () => {
  if (Object.keys(touched).length) syncErrors(true)
})
</script>
