<script setup>
/**
 * 门户页：双阵营背景 + 学院公告 + 登录入口。
 *
 * 从 longzu 的 main-page 重构：
 *  - 阵营不再只是换背景，改动会同步标题、副题、公告（数据来自 data/portal.js）
 *  - 「登 录」按钮接真实路由：未登录去 /login，已登录直接进 /forum
 *  - 「新生注册」指向 /register（原型里这里是空链接）
 *  - 背景图按 1024/768/512 三档出 WebP + srcset，不再是 2MB 单张 PNG
 *  - 章节背景用 absolute 而非 fixed：路由过渡会给根节点加 transform，
 *    fixed 子元素此时会退化成相对根节点定位，出现跳动
 */
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import FactionSwitch from '@/components/FactionSwitch.vue'
import { FACTIONS, MOTTO, imgSrc, imgSrcset } from '@/data/portal'
import { isLoggedIn } from '@/utils/auth'
import { saveFaction, savedFaction } from '@/utils/landing'

const router = useRouter()

const fallbackKey = FACTIONS[0].key
const remembered = savedFaction()
const active = ref(FACTIONS.some((f) => f.key === remembered) ? remembered : fallbackKey)

const faction = computed(() => FACTIONS.find((f) => f.key === active.value) || FACTIONS[0])
const logged = computed(() => isLoggedIn())

watch(active, (key) => saveFaction(key))

function backToPrologue() {
  router.push({ path: '/', query: { replay: '1' } })
}

function enter() {
  router.push(logged.value ? '/forum' : '/login')
}
</script>

<template>
  <div class="portal">
    <div class="portal__bg" :class="`portal__bg--${faction.tone}`" aria-hidden="true">
      <img
        v-for="f in FACTIONS"
        :key="f.key"
        class="portal__bg-img"
        :class="{ 'is-on': f.key === faction.key }"
        :src="imgSrc(f.bg, 1024)"
        :srcset="imgSrcset(f.bg, [1024, 768, 512])"
        sizes="100vw"
        alt=""
        decoding="async"
      />
    </div>

    <div class="portal__scroll">
      <header class="portal__nav">
        <button class="portal__back" type="button" @click="backToPrologue">← 序章</button>
        <FactionSwitch v-model="active" :factions="FACTIONS" />
      </header>

      <main class="portal__main">
        <section class="hero">
          <div class="hero__eyebrow">{{ faction.eyebrow }}</div>
          <h1 class="hero__title">
            <span v-for="(line, i) in faction.title" :key="i" class="hero__title-line">{{ line }}</span>
          </h1>
          <div class="hero__sub">
            <span class="star">✦</span>
            <span class="txt">{{ faction.subtitle }}</span>
            <span class="star">✦</span>
          </div>
          <p class="hero__desc">{{ faction.desc }}</p>
        </section>

        <div class="portal__grid">
          <section class="bulletin">
            <div class="bulletin__tag">{{ faction.bulletin.tag }}</div>
            <h2 class="bulletin__title">{{ faction.bulletin.title }}</h2>
            <p class="bulletin__text">{{ faction.bulletin.text }}</p>
          </section>

          <section class="cta">
            <p class="cta__motto">{{ MOTTO }}</p>
            <button class="cta__login" type="button" @click="enter">
              {{ logged ? '进入读者论坛' : '登 录' }}<span class="chv">›</span>
            </button>
            <div class="cta__links">
              <router-link :to="logged ? '/console' : '/register'">
                {{ logged ? '我的身份卡' : '新生注册' }}
              </router-link>
              <span class="dot" />
              <router-link to="/register">提交入学申请</router-link>
            </div>
          </section>
        </div>
      </main>

      <footer class="portal__foot">
        <button class="portal__replay" type="button" @click="backToPrologue">重看序章</button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.portal {
  position: relative;
  min-height: 100vh;
  background: var(--bg-1);
  --silver-bright: #dce2ee;
  --silver: #b8c2d4;
}

/* ---------- 背景（只覆盖首屏，底部渐变并入 --bg-1，滚动时无接缝） ---------- */
.portal__bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: linear-gradient(180deg, #0e1830 0%, #142040 30%, #0e1830 60%, var(--bg-1) 100%);
  z-index: 0;
}

.portal__bg-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  filter: brightness(0.55) contrast(1.15) saturate(0.9);
  opacity: 0;
  transition: opacity 0.9s cubic-bezier(0.4, 0, 0.2, 1);
}

.portal__bg-img.is-on {
  opacity: 1;
}

/* 背景只铺首屏，底部 240px 渐隐到 --bg-1，滚动时与页面底色无缝衔接 */
.portal__bg::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 240px;
  background: linear-gradient(180deg, transparent, var(--bg-1));
  pointer-events: none;
}

.portal__bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(8, 12, 22, 0.55) 0%,
    rgba(8, 12, 22, 0.25) 20%,
    rgba(8, 12, 22, 0.15) 45%,
    rgba(8, 12, 22, 0.4) 70%,
    rgba(8, 12, 22, 0.75) 100%
  );
}

/* 蛇岐八家：罩一层绯红，与卡塞尔的冷蓝区分 */
.portal__bg--warm::after {
  background: linear-gradient(
    180deg,
    rgba(24, 8, 12, 0.55) 0%,
    rgba(24, 8, 12, 0.25) 20%,
    rgba(24, 8, 12, 0.15) 45%,
    rgba(24, 8, 12, 0.4) 70%,
    rgba(24, 8, 12, 0.75) 100%
  );
}

.portal__scroll {
  position: relative;
  z-index: 10;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ---------- 导航 ---------- */
.portal__nav {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  background: rgba(8, 12, 22, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(130, 155, 200, 0.08);
  z-index: 100;
}

.portal__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: 1px solid var(--line-soft);
  border-radius: 24px;
  background: rgba(12, 18, 32, 0.5);
  color: var(--text-2);
  font-family: var(--font-serif);
  font-size: 12px;
  letter-spacing: 2px;
  cursor: pointer;
  white-space: nowrap;
  transition: border-color 0.3s ease, color 0.3s ease;
}

.portal__back:hover {
  border-color: rgba(216, 178, 90, 0.35);
  color: var(--gold-lt);
}

.portal__back:focus-visible {
  outline: 1px solid var(--gold);
  outline-offset: 2px;
}

/* ---------- Hero ---------- */
.portal__main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 32px 20px 10px;
}

.hero__eyebrow {
  font-family: var(--font-serif);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 5px;
  text-transform: uppercase;
  color: var(--text-2);
  line-height: 1.9;
  align-self: flex-start;
  margin-left: 10px;
}

.hero__title {
  margin: 28px 0 16px;
  font-family: var(--font-serif);
  font-size: clamp(34px, 8.5vw, 52px);
  font-weight: 500;
  letter-spacing: 12px;
  line-height: 1.3;
  color: var(--silver-bright);
  text-shadow: 0 0 50px rgba(150, 185, 235, 0.18), 0 4px 16px rgba(0, 0, 0, 0.6);
}

.hero__title-line {
  display: block;
}

.hero__sub {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.hero__sub .star {
  font-size: 9px;
  color: var(--gold);
}

.hero__sub .txt {
  font-family: var(--font-serif);
  font-size: 11px;
  letter-spacing: 4px;
  color: var(--text-2);
  text-transform: uppercase;
  line-height: 1.7;
}

.hero__desc {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 1.5px;
  color: var(--text-3);
  max-width: 28em;
}

/* ---------- 公告 + 入口 ---------- */
.portal__grid {
  display: flex;
  flex-direction: column;
}

.bulletin {
  margin: 16px 18px;
  padding: 22px;
  background: rgba(10, 16, 30, 0.6);
  border: 1px solid rgba(130, 155, 200, 0.16);
  border-radius: 14px;
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

.bulletin__tag {
  font-family: var(--font-serif);
  font-size: 10px;
  letter-spacing: 3.5px;
  text-transform: uppercase;
  color: var(--text-4);
  margin-bottom: 10px;
}

.bulletin__title {
  margin: 0 0 6px;
  font-family: var(--font-serif);
  font-size: 19px;
  font-weight: 500;
  color: var(--silver-bright);
  letter-spacing: 3px;
}

.bulletin__text {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--text-2);
  letter-spacing: 1px;
}

.cta {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cta__motto {
  text-align: center;
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 300;
  letter-spacing: 5px;
  color: var(--text-2);
  margin: 28px 0 20px;
}

.cta__login {
  display: flex;
  align-items: center;
  justify-content: center;
  width: calc(100% - 36px);
  margin: 0 18px;
  padding: 17px;
  border: 1px solid rgba(170, 195, 235, 0.25);
  border-radius: 12px;
  background: rgba(12, 18, 32, 0.55);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: var(--silver-bright);
  font-family: var(--font-serif);
  font-size: 17px;
  letter-spacing: 8px;
  cursor: pointer;
  transition: border-color 0.35s ease, box-shadow 0.35s ease, background 0.35s ease;
}

.cta__login:hover {
  border-color: rgba(216, 178, 90, 0.3);
  box-shadow: 0 0 35px rgba(90, 130, 200, 0.12);
  background: rgba(18, 28, 50, 0.65);
}

.cta__login:focus-visible {
  outline: 1px solid var(--gold);
  outline-offset: 3px;
}

.cta__login .chv {
  margin-left: 4px;
}

.cta__links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin: 14px 18px 40px;
}

.cta__links a {
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--text-2);
  text-decoration: none;
  letter-spacing: 1.5px;
  transition: color 0.3s ease;
}

.cta__links a:hover {
  color: var(--gold-lt);
}

.cta__links .dot {
  width: 3px;
  height: 3px;
  background: var(--text-4);
  border-radius: 50%;
}

/* ---------- 页脚 ---------- */
.portal__foot {
  display: flex;
  justify-content: center;
  padding: 0 18px 32px;
}

.portal__replay {
  border: 0;
  background: transparent;
  color: var(--text-4);
  font-family: var(--font-serif);
  font-size: 12px;
  letter-spacing: 2px;
  cursor: pointer;
  padding: 6px 10px;
}

.portal__replay:hover {
  color: var(--gold-lt);
}

/* ---------- 桌面 ---------- */
@media (min-width: 768px) {
  .portal__nav {
    padding: 14px 48px;
  }

  .portal__back {
    font-size: 13px;
    padding: 9px 20px;
  }

  .portal__main {
    max-width: 1080px;
    width: 100%;
    margin: 0 auto;
    padding: 0 48px;
  }

  .hero {
    padding: 72px 0 28px;
  }

  .hero__title {
    font-size: 72px;
    letter-spacing: 16px;
    margin: 36px 0 20px;
  }

  .hero__eyebrow {
    font-size: 11px;
    letter-spacing: 6px;
    margin-left: 2px;
  }

  .hero__sub .txt {
    font-size: 12px;
    letter-spacing: 5px;
  }

  .hero__desc {
    font-size: 14px;
  }

  .portal__grid {
    display: grid;
    grid-template-columns: 1.25fr 1fr;
    gap: 24px;
    align-items: stretch;
    margin: 12px 0 56px;
  }

  .bulletin {
    margin: 0;
    padding: 32px 34px;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .bulletin__tag {
    font-size: 11px;
    margin-bottom: 12px;
  }

  .bulletin__title {
    font-size: 22px;
    margin-bottom: 8px;
  }

  .bulletin__text {
    font-size: 14px;
    line-height: 1.9;
  }

  .cta {
    padding: 32px 30px;
    background: rgba(10, 16, 30, 0.6);
    border: 1px solid rgba(130, 155, 200, 0.16);
    border-radius: 14px;
    backdrop-filter: blur(18px);
    -webkit-backdrop-filter: blur(18px);
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
    justify-content: center;
  }

  .cta__motto {
    font-size: 17px;
    letter-spacing: 6px;
    margin: 0 0 24px;
  }

  .cta__login {
    width: 100%;
    margin: 0;
    padding: 18px;
  }

  .cta__links {
    margin: 20px 0 0;
  }

  .portal__foot {
    padding-bottom: 40px;
  }
}

@media (min-width: 1200px) {
  .portal__main {
    max-width: 1160px;
  }

  .hero__title {
    font-size: 84px;
    letter-spacing: 18px;
  }
}

@media (max-width: 420px) {
  .hero__title {
    letter-spacing: 7px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .portal__bg-img {
    transition: none;
  }
}
</style>
