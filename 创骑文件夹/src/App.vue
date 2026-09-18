<template>
  <BackgroundFx :active-form="activeForm" />

  <div class="card">
    <div class="crest" ref="crest" v-html="crestSVG" @click="onCrestClick" title="点击让眼睛发光"></div>

    <h1 class="title">假面骑士<span class="b">创骑</span></h1>
    <div class="subtitle">最佳搭配 · 骑士登录</div>

    <form id="loginForm" @submit.prevent="onSubmit" autocomplete="off">
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
      <button type="submit" class="henshin" ref="henshinBtn">最佳搭配 · 变身</button>
    </form>

    <div class="msg" :class="msgClass">{{ msgText }}</div>

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
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import BackgroundFx from './components/BackgroundFx.vue'
import Driver from './components/Driver.vue'
import { FORMS, CRESTS, GENIUS_RAINBOW } from './data/forms.js'

const uid = ref('')
const pwd = ref('')
const remember = ref(false)
const activeForm = ref('rabbittank')
const msgText = ref('')
const msgClass = ref('')

const crest = ref(null)
const henshinBtn = ref(null)
const flash = ref(null)
const driver = ref(null)

const crestSVG = computed(() => CRESTS[activeForm.value])
const currentName = computed(() => FORMS[activeForm.value].name)
const formList = [
  { key: 'rabbittank', name: '兔子坦克' },
  { key: 'hazard', name: '危险形态' },
  { key: 'genius', name: '天才形态' }
]

function setMsg(text, cls) { msgText.value = text; msgClass.value = cls || '' }

function applyForm(key) {
  const t = FORMS[key].theme
  activeForm.value = key
  const root = document.documentElement
  root.style.setProperty('--kr-red', t.red)
  root.style.setProperty('--kr-blue', t.blue)
  root.style.setProperty('--kr-accent', t.accent)
  if (henshinBtn.value) henshinBtn.value.style.background = t.btn
  document.body.classList.toggle('hazard', key === 'hazard')
  document.body.classList.toggle('genius', key === 'genius')
}

function selectForm(key) {
  applyForm(key)
  setMsg(`已切换形态：${FORMS[key].name}`, 'ok')
}

// 纹章眼睛发光：天才形态走彩虹色，其余走形态双色
function rainbowKeyframes(c1, c2) {
  return [
    { filter: `drop-shadow(0 0 0px ${c1})` },
    { filter: 'drop-shadow(0 0 9px #f5c518)' },
    { filter: 'drop-shadow(0 0 9px #00a6e6)' },
    { filter: 'drop-shadow(0 0 9px #9b5de5)' },
    { filter: `drop-shadow(0 0 0px ${c2})` }
  ]
}

function glowEyes() {
  const el = crest.value
  if (!el) return
  const svg = el.querySelector('svg')
  if (!svg) return

  if (activeForm.value === 'genius') {
    svg.querySelectorAll('.eye-l').forEach(e => e.animate(rainbowKeyframes('#ff3b3b', '#9b5de5'), { duration: 1000, easing: 'ease-out' }))
    svg.querySelectorAll('.eye-r').forEach(e => e.animate(rainbowKeyframes('#00a6e6', '#f5c518'), { duration: 1000, easing: 'ease-out' }))
    svg.querySelectorAll('.halo').forEach(h => {
      h.setAttribute('stroke', 'url(#gnRain)')
      h.animate(
        [{ transform: 'scale(.4)', opacity: .95 }, { transform: 'scale(3.6)', opacity: 0 }],
        { duration: 820, easing: 'ease-out' }
      )
    })
    return
  }

  const t = FORMS[activeForm.value].theme
  svg.querySelectorAll('.eye-l').forEach(el2 => el2.animate(
    [{ filter: `drop-shadow(0 0 0px ${t.gl})` }, { filter: `drop-shadow(0 0 9px ${t.gl})` }, { filter: `drop-shadow(0 0 0px ${t.gl})` }],
    { duration: 900, easing: 'ease-out' }))
  svg.querySelectorAll('.eye-r').forEach(el2 => el2.animate(
    [{ filter: `drop-shadow(0 0 0px ${t.gr})` }, { filter: `drop-shadow(0 0 9px ${t.gr})` }, { filter: `drop-shadow(0 0 0px ${t.gr})` }],
    { duration: 900, easing: 'ease-out' }))
  svg.querySelectorAll('.halo').forEach((h, i) => {
    const c = i === 0 ? t.gl : t.gr
    h.setAttribute('stroke', c)
    h.animate(
      [{ transform: 'scale(.4)', opacity: .95 }, { transform: 'scale(3.4)', opacity: 0 }],
      { duration: 720, easing: 'ease-out' }
    )
  })
}

function onCrestClick(e) {
  e.stopPropagation()
  glowEyes()
}

// ===== 变身器交互回调 =====
function onBottle(side) { setMsg(side === 'left' ? '兔子！' : '坦克！', 'ok') }
function onJudge() { glowEyes(); setMsg('最佳搭配！', 'ok') }
function onLever() { setMsg(activeForm.value === 'hazard' ? '危险启动！' : '准备就绪！', 'ok') }

function onForgot(e) {
  e.preventDefault()
  setMsg('请前往骑士总部重置变身密码。', 'err')
}

function triggerFlash() {
  const t = FORMS[activeForm.value].theme
  flash.value.style.background = t.flash
  flash.value.classList.remove('go')
  void flash.value.offsetWidth
  flash.value.classList.add('go')
}

function onSubmit() {
  if (!uid.value.trim() || !pwd.value) {
    setMsg('骑士名与变身密码不能为空！', 'err')
    return
  }
  if (pwd.value.length < 6) {
    setMsg('变身密码至少 6 位！', 'err')
    return
  }

  const f = FORMS[activeForm.value]
  // 原著变身流程演示：装填双瓶 → 核心判定 → 拉动拉杆 → 全屏闪
  driver.value.loadBottle('left')
  driver.value.loadBottle('right')
  setTimeout(() => driver.value.coreJudge(), 200)
  setTimeout(() => {
    driver.value.pullLever()
    triggerFlash()
    glowEyes()
    setMsg(`最佳搭配！变身成功，欢迎回来，${uid.value.trim()} · ${f.name}`, 'ok')
    const card = document.querySelector('.card')
    card.animate(
      [{ boxShadow: `0 0 0 3px ${f.theme.accent}, 0 24px 60px rgba(0,0,0,0.65)` },
       { boxShadow: '0 0 0 1px rgba(245,197,24,0.10), 0 24px 60px rgba(0,0,0,0.65)' }],
      { duration: 700, easing: 'ease-out' }
    )
  }, 430)
}

// 快捷键 1/2/3 切换形态
function onKey(e) {
  if (e.key === '1') selectForm('rabbittank')
  if (e.key === '2') selectForm('hazard')
  if (e.key === '3') selectForm('genius')
}

onMounted(() => {
  applyForm('rabbittank')
  window.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
})
</script>
