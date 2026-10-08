<template>
  <BackgroundFx :active-form="activeForm" />

  <div class="card">
    <Crest ref="crest" :form-key="activeForm" />

    <h1 class="title">假面骑士<span class="b">创骑</span></h1>
    <div class="subtitle">最佳搭配 · 骑士系统</div>

    <!-- 后端连接状态：由 /api/health 探测（Spring Boot + MySQL + MyBatis-Plus） -->
    <div class="backend" :class="backendState">
      <span class="bdot"></span>
      <span class="btext">{{ backendText }}</span>
      <button type="button" class="brefresh" @click="checkBackend" title="重新检测">↻</button>
    </div>

    <!-- 登录 / 注册 切换 -->
    <div class="seg" role="tablist" aria-label="选择登录或注册">
      <button type="button" role="tab" :aria-selected="mode === 'login'"
              :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
      <button type="button" role="tab" :aria-selected="mode === 'register'"
              :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
    </div>

    <form v-if="mode === 'login'" id="loginForm" @submit.prevent="onSubmit" autocomplete="off">
      <div class="field">
        <label for="uid"><span class="dot r"></span>骑士名 / 编号</label>
        <input id="uid" v-model="uid" type="text" placeholder="请输入骑士名" required />
      </div>
      <div class="field">
        <label for="pwd"><span class="dot b"></span>变身密码 / 口令</label>
        <input id="pwd" v-model="pwd" type="password" placeholder="••••••••" required />
      </div>
      <div class="row">
        <label><input type="checkbox" v-model="remember" /> 记住此骑士</label>
        <a href="#" @click.prevent="onForgot">忘记密码？</a>
      </div>
      <button type="submit" class="henshin" ref="henshinBtn" :disabled="loading">
        {{ loading ? '与骑士总部通讯中…' : '最佳搭配 · 变身' }}
      </button>
    </form>

    <RegisterForm v-else :form-key="activeForm"
                  @switch="mode = 'login'" @success="onRegisterSuccess" />

    <div v-if="mode === 'login'" class="msg" :class="msgClass">{{ msgText }}</div>
    <p v-if="mode === 'login'" class="switch">
      还没有账号？<a href="#" @click.prevent="mode = 'register'">立即注册</a>
    </p>

    <div class="divider">选择形态</div>
    <div class="alt">
      <button type="button" v-for="f in formList" :key="f.key"
              :data-form="f.key" :class="{ active: activeForm === f.key }"
              @click="selectForm(f.key)">{{ f.name }}</button>
    </div>

    <Driver ref="driver" :form-key="activeForm"
            @bottle="onBottle" @judge="onJudge" @lever="onLever" />
    <div class="match-label">创骑驱动器 · 当前形态：<b>{{ currentName }}</b></div>

    <div class="footer">© 假面骑士创骑 · 骑士系统</div>
  </div>

  <div class="flash-overlay" ref="flash"></div>

  <!-- 变身成功全屏特写：必杀飞踢横版图 + 形态名，1.7s 后淡出 -->
  <div class="henshin-reveal" ref="reveal" aria-hidden="true">
    <img :src="CREST_FINISH" alt="" draggable="false" />
    <div class="hr-shade"></div>
    <div class="hr-text">
      <b>{{ revealName }}</b>
      <span>{{ revealSub }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import BackgroundFx from './components/BackgroundFx.vue'
import Driver from './components/Driver.vue'
import RegisterForm from './components/RegisterForm.vue'
import Crest from './components/Crest.vue'
import { FORMS, CREST_FINISH } from './data/forms.js'
// 后端接口（Spring Boot，经 Vite 代理转发到 http://localhost:8081）
import { login as apiLogin, me as apiMe, health as apiHealth } from './api/user.js'

const uid = ref('')
const pwd = ref('')
const remember = ref(false)
const mode = ref('login') // 'login' | 'register'
const activeForm = ref('rabbittank')
const msgText = ref('')
const msgClass = ref('')
const loading = ref(false)

// 后端连接状态：checking | ok | fail
const backendState = ref('checking')
const backendText = ref('正在检测后端连接…')

const crest = ref(null)
const henshinBtn = ref(null)
const flash = ref(null)
const reveal = ref(null)
const driver = ref(null)

const currentName = computed(() => FORMS[activeForm.value].name)

// 全屏特写文案：形态名 + 必杀技名
const revealName = ref('')
const revealSub = ref('')
const formList = [
  { key: 'rabbittank', name: '兔子坦克' },
  { key: 'hazard', name: '危险形态' },
  { key: 'genius', name: '天才形态' }
]

// 每种形态对应的星云配色，切换形态时背景星云一起切换
const NEBULA = {
  rabbittank: ['rgba(96,52,160,0.42)', 'rgba(24,72,140,0.36)'],
  hazard: ['rgba(150,20,20,0.48)', 'rgba(60,0,0,0.42)'],
  genius: ['rgba(245,197,24,0.42)', 'rgba(0,166,230,0.36)']
}

function setMsg(text, cls) { msgText.value = text; msgClass.value = cls || '' }

function applyForm(key) {
  const t = FORMS[key].theme
  activeForm.value = key
  const root = document.documentElement
  root.style.setProperty('--kr-red', t.red)
  root.style.setProperty('--kr-blue', t.blue)
  root.style.setProperty('--kr-accent', t.accent)
  const neb = NEBULA[key] || NEBULA.rabbittank
  document.body.style.setProperty('--neb1', neb[0])
  document.body.style.setProperty('--neb2', neb[1])
  if (henshinBtn.value) henshinBtn.value.style.background = t.btn
  document.body.classList.toggle('hazard', key === 'hazard')
  document.body.classList.toggle('genius', key === 'genius')
}

function selectForm(key) {
  applyForm(key)
  setMsg(`已切换形态：${FORMS[key].name}`, 'ok')
}

// 纹章释放骑士之光（具体动画由 Crest 组件内部处理）
function crestSpark() {
  crest.value?.spark()
}

// 变身成功全屏特写：淡入必杀飞踢横版图，标注形态与必杀技名
let revealTimer = 0
function triggerReveal(key) {
  const k = key || activeForm.value
  const f = FORMS[k] || FORMS[activeForm.value]
  if (!reveal.value) return
  revealName.value = f.name.endsWith('形态') ? f.name : f.name + '形态'
  revealSub.value = (k === 'genius' ? 'GENIUS FORM' : 'KAMEN RIDER BUILD') + ' · ' + f.finish
  reveal.value.classList.remove('go')
  void reveal.value.offsetWidth
  reveal.value.classList.add('go')
  clearTimeout(revealTimer)
  revealTimer = setTimeout(() => reveal.value && reveal.value.classList.remove('go'), 1750)
}

// ===== 变身器交互回调 =====
function onBottle(side) { setMsg(side === 'left' ? '兔子！' : '坦克！', 'ok') }
function onJudge() { crestSpark(); setMsg('最佳搭配！', 'ok') }
function onLever() { setMsg(activeForm.value === 'hazard' ? '危险启动！' : '准备就绪！', 'ok') }

function onForgot(e) {
  e.preventDefault()
  setMsg('请前往骑士总部重置变身密码。', 'err')
}

// 注册成功：触发一次变身闪光 + 全屏特写作为入队仪式感
function onRegisterSuccess() {
  triggerFlash()
  triggerReveal()
}

function triggerFlash() {
  const t = FORMS[activeForm.value].theme
  flash.value.style.background = t.flash
  flash.value.classList.remove('go')
  void flash.value.offsetWidth
  flash.value.classList.add('go')
}

/* ============ 与后端交互 ============ */

// 探测后端连通性（顺带确认 MySQL 通了）
async function checkBackend() {
  backendState.value = 'checking'
  backendText.value = '正在检测后端连接…'
  try {
    const data = await apiHealth()
    backendState.value = 'ok'
    backendText.value = `后端已连接 · MySQL 在线 · 当前骑士 ${data.userCount} 位`
  } catch (e) {
    backendState.value = 'fail'
    backendText.value = '后端未连接：' + e.message
  }
}

// 页面打开时若已有登录态（Session），直接提示欢迎回来
async function restoreSession() {
  try {
    const user = await apiMe()
    if (user) {
      if (FORMS[user.formKey]) applyForm(user.formKey)
      setMsg(`欢迎回来，${user.username} · ${FORMS[user.formKey]?.name || ''}`, 'ok')
    }
  } catch (e) {
    // 未登录属于正常情况，静默忽略
  }
}

// 原著变身流程动画：装填双瓶 → 核心判定 → 拉动拉杆 → 全屏闪
function playHenshin(user) {
  const f = FORMS[user.formKey] || FORMS[activeForm.value]
  if (!driver.value) return
  driver.value.loadBottle('left')
  driver.value.loadBottle('right')
  setTimeout(() => driver.value.coreJudge(), 200)
  setTimeout(() => {
    driver.value.pullLever()
    triggerFlash()
    crestSpark()
    // 闪光到顶后接全屏必杀特写
    setTimeout(() => triggerReveal(user.formKey), 420)
    const card = document.querySelector('.card')
    if (card) {
      card.animate(
        [{ boxShadow: `0 0 0 3px ${f.theme.accent}, 0 24px 60px rgba(0,0,0,0.65)` },
         { boxShadow: '0 0 0 1px rgba(245,197,24,0.10), 0 24px 60px rgba(0,0,0,0.65)' }],
        { duration: 700, easing: 'ease-out' }
      )
    }
  }, 430)
}

// 提交登录：真正调用后端 POST /api/user/login
async function onSubmit() {
  if (loading.value) return

  if (!uid.value.trim() || !pwd.value) {
    setMsg('骑士名与变身密码不能为空！', 'err')
    return
  }
  if (pwd.value.length < 6) {
    setMsg('变身密码至少 6 位！', 'err')
    return
  }

  loading.value = true
  setMsg('正在与骑士总部通讯…', '')

  try {
    const user = await apiLogin({
      username: uid.value.trim(),
      password: pwd.value,
      remember: remember.value
    })

    // 登录成功：同步后端记录的形态，并播放变身动画
    if (user.formKey && FORMS[user.formKey]) applyForm(user.formKey)
    playHenshin(user)

    if (remember.value) {
      localStorage.setItem('kr_remember_username', user.username)
    } else {
      localStorage.removeItem('kr_remember_username')
    }

    setMsg(`最佳搭配！变身成功，欢迎回来，${user.username} · ${FORMS[user.formKey]?.name || ''}`, 'ok')
    pwd.value = ''
    checkBackend()
  } catch (e) {
    setMsg(e.message, 'err')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  applyForm('rabbittank')

  // 回填"记住此骑士"的账号
  const saved = localStorage.getItem('kr_remember_username')
  if (saved) {
    uid.value = saved
    remember.value = true
  }

  checkBackend()
  restoreSession()
})
onBeforeUnmount(() => {
  clearTimeout(revealTimer)
})
</script>
