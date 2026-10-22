<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import { CHARACTER_GALLERY } from '@/data/characters'

const route = useRoute()
const router = useRouter()

const char = computed(
  () => CHARACTER_GALLERY.find((c) => c.key === route.params.key) || null
)

/** 龙血占比条：进场后从 0 涨到目标值 */
const pctNow = ref(0)
let pctTimer = 0

onMounted(() => {
  document.documentElement.style.colorScheme = 'dark'
  document.title = char.value
    ? `人物档案 · ${char.value.name} | 卡塞尔学院`
    : '人物档案 | 卡塞尔学院'
  pctTimer = window.setTimeout(() => {
    pctNow.value = char.value?.bloodPct || 0
  }, 260)
})

function goBack() {
  // 带锚点回到首页人物志区块
  router.push({ path: '/home', hash: '#sec-people' })
}
</script>

<template>
  <div class="char">
    <RuneBackground />

    <!-- ============ 顶部导航 ============ -->
    <header class="char__nav">
      <div class="char__nav-inner">
        <div class="char__brand">
          <DragonEmblem class="char__mark" />
          <div class="char__brand-text">
            <span class="char__brand-cn">卡塞尔学院</span>
            <span class="char__brand-en">Cassell College</span>
          </div>
        </div>
        <button type="button" class="char__back" @click="goBack">
          <AppIcon name="arrow" :size="14" class="char__back-ic" />
          返回档案馆
        </button>
      </div>
    </header>

    <!-- ============ 主体 ============ -->
    <main class="char__body">
      <template v-if="char">
        <article class="dossier">
          <!-- 顶部鎏金光带 -->
          <i class="dossier__beam" />

          <!-- 档案头：头像 + 基本信息 + 评级 -->
          <header class="dossier__head">
            <!-- 头像框：外圈纹章环 + 首字（接入立绘后替换为 <img>） -->
            <div class="dossier__avatar" :data-tone="char.tone">
              <span class="dossier__avatar-txt">{{ char.name.slice(0, 1) }}</span>
              <i class="dossier__avatar-ring" />
              <i class="dossier__avatar-corner dossier__avatar-corner--tl" />
              <i class="dossier__avatar-corner dossier__avatar-corner--br" />
            </div>

            <div class="dossier__info">
              <p class="dossier__cat">
                <span class="dossier__cat-chip">{{ char.categoryLabel }}</span>
                {{ char.alias }}
              </p>
              <h1 class="dossier__name">{{ char.name }}</h1>
              <p class="dossier__role">{{ char.role }}</p>
              <p class="dossier__desc">{{ char.desc }}</p>
            </div>

            <!-- 血统评级大徽章 -->
            <div class="dossier__rank" :data-rank="char.blood">
              <b>{{ char.blood }}</b>
              <span>血统评级</span>
              <em class="dossier__rank-seal" />
            </div>
          </header>

          <!-- 密级条 -->
          <div class="dossier__levelbar">
            <AppIcon name="lock" :size="12" />
            档案密级
            <b class="dossier__level" :data-level="char.level">{{ char.level }}</b>
            <span class="dossier__level-note">调阅已留痕 · 中央档案馆</span>
          </div>

          <!-- 血统鉴定：龙血占比 -->
          <section class="dossier__sec">
            <h2 class="dossier__sec-t"><i />血统鉴定</h2>
            <div class="dossier__blood">
              <div class="dossier__bloodbar">
                <i :style="{ width: pctNow + '%' }" />
                <span class="dossier__blood-ticks">
                  <b v-for="n in 4" :key="n" :style="{ left: (n - 1) * 25 + '%' }" />
                </span>
              </div>
              <p class="dossier__blood-meta">
                <span class="dossier__blood-pct">
                  龙血占比 <b>{{ char.bloodPct }}%</b>
                </span>
                <span class="dossier__blood-note">{{ char.bloodNote }}</span>
              </p>
            </div>
          </section>

          <!-- 言灵记录 -->
          <section class="dossier__sec">
            <h2 class="dossier__sec-t"><i />言灵记录</h2>
            <ul class="dossier__spirits">
              <li v-for="(s, i) in char.spirits" :key="i" class="dossier__spirit">
                <em>{{ String(i + 1).padStart(2, '0') }}</em>
                {{ s }}
              </li>
            </ul>
          </section>

          <!-- 人物事迹 -->
          <section class="dossier__sec">
            <h2 class="dossier__sec-t"><i />人物事迹</h2>
            <ol class="dossier__deeds">
              <li v-for="(d, i) in char.deeds" :key="i" class="dossier__deed">
                <b class="dossier__deed-n">{{ String(i + 1).padStart(2, '0') }}</b>
                <p>{{ d }}</p>
              </li>
            </ol>
          </section>

          <!-- 语录 -->
          <blockquote class="dossier__quote">
            <AppIcon name="sparkle" :size="16" />
            {{ char.quote }}
          </blockquote>

          <footer class="dossier__foot">
            摘自《混血种名册 · 人物志》 · 卡塞尔学院档案馆藏 · 本页调阅已计入日志
          </footer>
        </article>
      </template>

      <!-- 档案不存在 -->
      <div v-else class="dossier--void">
        <AppIcon name="archive" :size="40" :stroke="1.2" />
        <p>档案馆中没有该人物的卷宗</p>
        <button type="button" class="dossier__void-btn" @click="goBack">返回档案馆</button>
      </div>
    </main>
  </div>
</template>

<style scoped>
.char {
  position: relative;
  z-index: 1;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* ================= 顶部导航 ================= */
.char__nav {
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
  padding: 26px 24px 0;
  animation: fade-down 0.7s 0.05s both;
}

.char__nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.char__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.char__mark {
  width: 38px;
  height: 38px;
  filter: drop-shadow(0 0 10px rgba(216, 178, 90, 0.5));
  animation: spin 46s linear infinite;
}

.char__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.char__brand-cn {
  font-family: var(--font-serif);
  font-size: 15px;
  letter-spacing: 0.26em;
  color: var(--gold-lt);
}

.char__brand-en {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.3em;
  color: var(--text-4);
  text-transform: uppercase;
}

.char__back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.32);
  background: rgba(216, 178, 90, 0.06);
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: border-color 0.25s, background 0.25s, transform 0.25s, box-shadow 0.25s;
}

.char__back:hover {
  border-color: var(--gold);
  background: rgba(216, 178, 90, 0.14);
  transform: translateX(-3px);
  box-shadow: 0 10px 26px -14px var(--gold-glow);
}

.char__back-ic {
  transform: rotate(180deg);
}

/* ================= 主体 ================= */
.char__body {
  flex: 1;
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
  padding: 26px 24px 40px;
}

.dossier {
  position: relative;
  padding: 34px 38px 30px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: linear-gradient(165deg, rgba(32, 27, 46, 0.88), rgba(13, 11, 22, 0.95));
  box-shadow:
    var(--shadow-card),
    0 0 30px -18px rgba(216, 178, 90, 0.3);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  animation: card-in 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
  overflow: hidden;
}

/* 顶部鎏金光带 */
.dossier__beam {
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    var(--gold-lt),
    var(--gold-antique),
    transparent
  );
}

/* ================= 档案头 ================= */
.dossier__head {
  display: flex;
  align-items: center;
  gap: 26px;
  padding-bottom: 22px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.28);
}

/* 头像框 */
.dossier__avatar {
  position: relative;
  flex: 0 0 118px;
  width: 118px;
  height: 118px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  font-family: var(--font-serif);
  font-size: 52px;
  font-weight: 700;
  color: var(--gold-lt);
  text-shadow: 0 0 24px var(--gold-glow);
  background:
    radial-gradient(80% 80% at 50% 30%, rgba(216, 178, 90, 0.18), transparent 70%),
    linear-gradient(160deg, rgba(38, 32, 54, 0.95), rgba(14, 11, 22, 0.98));
  border: 1px solid rgba(216, 178, 90, 0.4);
  box-shadow:
    inset 0 1px 0 rgba(246, 230, 176, 0.22),
    0 0 26px -12px var(--gold-glow);
  animation: avatar-in 0.8s 0.15s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.dossier__avatar[data-tone='ice'] {
  color: var(--ice);
  text-shadow: 0 0 24px rgba(127, 212, 255, 0.45);
  border-color: rgba(127, 212, 255, 0.38);
  background:
    radial-gradient(80% 80% at 50% 30%, rgba(127, 212, 255, 0.14), transparent 70%),
    linear-gradient(160deg, rgba(28, 34, 52, 0.95), rgba(11, 14, 24, 0.98));
}

.dossier__avatar[data-tone='fire'] {
  color: var(--blood-lt);
  text-shadow: 0 0 24px rgba(226, 86, 79, 0.5);
  border-color: rgba(226, 86, 79, 0.42);
  background:
    radial-gradient(80% 80% at 50% 30%, rgba(179, 39, 45, 0.2), transparent 70%),
    linear-gradient(160deg, rgba(46, 28, 32, 0.95), rgba(16, 10, 14, 0.98));
}

.dossier__avatar[data-tone='rose'] {
  color: #f2b8c6;
  text-shadow: 0 0 24px rgba(242, 184, 198, 0.4);
  border-color: rgba(242, 184, 198, 0.38);
}

/* 头像旋转纹章环 */
.dossier__avatar-ring {
  position: absolute;
  inset: -7px;
  border-radius: 18px;
  border: 1px dashed rgba(216, 178, 90, 0.34);
  animation: spin 40s linear infinite;
}

.dossier__avatar-corner {
  position: absolute;
  width: 12px;
  height: 12px;
  border: 1px solid var(--gold);
  opacity: 0.75;
}

.dossier__avatar-corner--tl {
  top: -5px;
  left: -5px;
  border-right: 0;
  border-bottom: 0;
}

.dossier__avatar-corner--br {
  bottom: -5px;
  right: -5px;
  border-left: 0;
  border-top: 0;
}

@keyframes avatar-in {
  from {
    opacity: 0;
    transform: scale(0.88);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.dossier__info {
  flex: 1;
  min-width: 0;
}

.dossier__cat {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: var(--text-3);
  margin-bottom: 7px;
}

.dossier__cat-chip {
  padding: 2px 10px;
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
  border: 1px solid rgba(138, 106, 40, 0.7);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.24), rgba(138, 106, 40, 0.3));
}

.dossier__name {
  font-family: var(--font-serif);
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text);
  text-shadow: 0 0 30px rgba(216, 178, 90, 0.35);
  line-height: 1.2;
}

.dossier__role {
  margin-top: 7px;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--gold-lt);
}

.dossier__desc {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-2);
}

/* 血统评级大徽章 */
.dossier__rank {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 22px 13px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(122, 90, 30, 0.9);
  background: linear-gradient(
    170deg,
    var(--gold-metal-hi) 0%,
    var(--gold-metal-md) 40%,
    var(--gold-dp) 68%,
    #8a6a28 100%
  );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.6),
    inset 0 -1px 0 rgba(0, 0, 0, 0.4),
    0 12px 32px -14px var(--gold-glow);
  animation: card-in 0.7s 0.22s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.dossier__rank b {
  font-family: var(--font-serif);
  font-size: 40px;
  line-height: 1;
  font-weight: 800;
  color: #241a05;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.45);
}

.dossier__rank span {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.3em;
  color: rgba(36, 26, 5, 0.75);
}

/* 徽章上的往复高光 */
.dossier__rank-seal {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 45%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  animation: metal-sheen-d 3.6s ease-in-out infinite;
}

@keyframes metal-sheen-d {
  0% {
    transform: translateX(-140%) skewX(-18deg);
  }
  45%,
  100% {
    transform: translateX(420%) skewX(-18deg);
  }
}

/* S 级：底色偏暗金红，更贵重 */
.dossier__rank[data-rank='S'] {
  background: linear-gradient(
    170deg,
    #f8ecc4 0%,
    var(--gold-metal-md) 34%,
    #9c5b2a 72%,
    var(--gold-antique) 100%
  );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.62),
    inset 0 -1px 0 rgba(0, 0, 0, 0.42),
    0 14px 38px -14px rgba(226, 86, 79, 0.55);
}

/* ================= 密级条 ================= */
.dossier__levelbar {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 16px 0 6px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.14em;
  color: var(--text-4);
}

.dossier__level {
  position: relative;
  display: inline-flex;
  align-items: center;
  overflow: hidden;
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 600;
  color: #241a05;
  border: 1px solid rgba(122, 90, 30, 0.9);
  background: linear-gradient(
    180deg,
    var(--gold-metal-hi),
    var(--gold-metal-md) 45%,
    var(--gold-dp)
  );
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55);
}

.dossier__level[data-level='机密'] {
  color: #f8ecc4;
  border-color: rgba(138, 106, 40, 0.85);
  background: linear-gradient(180deg, var(--gold-metal-hi) -40%, var(--gold-antique) 30%, var(--gold-metal-lo) 62%, #9c742c);
  box-shadow: inset 0 1px 0 rgba(246, 230, 176, 0.35), inset 0 -1px 0 rgba(0, 0, 0, 0.5);
}

.dossier__level[data-level='内部'] {
  color: var(--text-3);
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: none;
}

.dossier__levelbar .dossier__level {
  color: #241a05;
}

.dossier__level-note {
  margin-left: auto;
}

/* ================= 区块 ================= */
.dossier__sec {
  margin-top: 24px;
  animation: fade-up 0.6s both;
}

.dossier__sec:nth-of-type(1) {
  animation-delay: 0.3s;
}

.dossier__sec-t {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  padding-left: 14px;
  margin-bottom: 13px;
  font-family: var(--font-serif);
  font-size: 16px;
  letter-spacing: 0.18em;
  color: var(--text);
}

/* 暗金立柱 */
.dossier__sec-t::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 14px;
  border-radius: 2px;
  transform: translateY(-50%);
  background: linear-gradient(180deg, var(--gold-lt), var(--gold) 40%, var(--gold-antique));
  box-shadow: 0 0 8px -2px var(--gold-glow);
}

.dossier__sec-t i {
  width: 5px;
  height: 5px;
  transform: rotate(45deg);
  background: linear-gradient(135deg, var(--gold-metal-hi), var(--gold-antique));
  box-shadow: 0 0 6px var(--gold-glow);
}

/* —— 龙血占比 —— */
.dossier__bloodbar {
  position: relative;
  height: 16px;
  border-radius: 999px;
  overflow: hidden;
  border: 1px solid rgba(138, 106, 40, 0.55);
  background: rgba(4, 7, 13, 0.66);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.6);
}

.dossier__bloodbar > i {
  position: absolute;
  inset: 1px auto 1px 1px;
  width: 0;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    #6e5218 0%,
    var(--gold-dp) 30%,
    var(--gold-metal-md) 62%,
    var(--gold-metal-hi) 86%,
    var(--blood-lt) 100%
  );
  box-shadow:
    0 0 18px -4px var(--gold-glow),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  transition: width 1.4s cubic-bezier(0.22, 1, 0.36, 1);
}

/* 刻度 */
.dossier__blood-ticks b {
  position: absolute;
  top: 2px;
  bottom: 2px;
  width: 1px;
  background: rgba(4, 7, 13, 0.55);
}

.dossier__blood-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px 16px;
  margin-top: 9px;
}

.dossier__blood-pct {
  font-size: 12.5px;
  letter-spacing: 0.08em;
  color: var(--text-2);
}

.dossier__blood-pct b {
  font-family: var(--font-serif);
  font-size: 19px;
  color: var(--gold-lt);
  text-shadow: 0 0 16px var(--gold-glow);
}

.dossier__blood-note {
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--text-4);
}

/* —— 言灵 —— */
.dossier__spirits {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.dossier__spirit {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 15px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: linear-gradient(120deg, rgba(26, 21, 38, 0.72), rgba(11, 9, 18, 0.85));
  font-family: var(--font-serif);
  font-size: 14.5px;
  letter-spacing: 0.1em;
  color: var(--text);
  transition: border-color 0.25s, transform 0.25s, box-shadow 0.25s;
}

.dossier__spirit:hover {
  border-color: rgba(216, 178, 90, 0.45);
  transform: translateY(-2px);
  box-shadow: 0 12px 28px -16px var(--gold-glow);
}

.dossier__spirit em {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 5px;
  color: #241a05;
  background: linear-gradient(180deg, var(--gold-metal-hi), #a87f2f 70%, #b98f38);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

/* —— 事迹时间线 —— */
.dossier__deeds {
  margin: 0;
  padding: 0;
  list-style: none;
  position: relative;
  display: grid;
  gap: 14px;
}

/* 纵向暗金时间轴线 */
.dossier__deeds::before {
  content: '';
  position: absolute;
  left: 19px;
  top: 8px;
  bottom: 8px;
  width: 1px;
  background: linear-gradient(
    180deg,
    transparent,
    rgba(216, 178, 90, 0.45) 12%,
    rgba(216, 178, 90, 0.45) 88%,
    transparent
  );
}

.dossier__deed {
  position: relative;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.dossier__deed-n {
  position: relative;
  z-index: 1;
  flex: 0 0 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  color: #241a05;
  background: linear-gradient(180deg, var(--gold-metal-hi), #a87f2f 68%, #b98f38);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.5),
    0 0 16px -4px var(--gold-glow);
}

.dossier__deed p {
  flex: 1;
  padding-top: 8px;
  font-size: 13px;
  line-height: 1.85;
  color: var(--text-2);
}

/* —— 语录 —— */
.dossier__quote {
  margin: 26px 0 0;
  padding: 16px 20px;
  border-radius: var(--r-md);
  border: 1px solid rgba(138, 106, 40, 0.4);
  border-left: 3px solid var(--gold-dp);
  background: linear-gradient(120deg, rgba(216, 178, 90, 0.07), rgba(179, 39, 45, 0.05));
  font-family: var(--font-serif);
  font-size: 15px;
  line-height: 1.9;
  letter-spacing: 0.08em;
  color: var(--gold-lt);
  text-shadow: 0 0 18px rgba(216, 178, 90, 0.3);
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.dossier__quote :deep(svg) {
  flex: 0 0 auto;
  margin-top: 5px;
  opacity: 0.7;
}

/* —— 页脚 —— */
.dossier__foot {
  margin-top: 22px;
  padding-top: 14px;
  border-top: 1px dashed rgba(255, 255, 255, 0.07);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

/* —— 无此档案 —— */
.dossier--void {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 90px 20px;
  text-align: center;
  color: var(--text-3);
  font-size: 13.5px;
  letter-spacing: 0.08em;
}

.dossier__void-btn {
  margin-top: 8px;
  padding: 9px 22px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.4);
  background: rgba(216, 178, 90, 0.08);
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.12em;
  cursor: pointer;
  transition: background 0.25s, border-color 0.25s;
}

.dossier__void-btn:hover {
  background: rgba(216, 178, 90, 0.16);
  border-color: var(--gold);
}

/* ================= 响应式 ================= */
@media (max-width: 720px) {
  .dossier__head {
    flex-direction: column;
    text-align: center;
  }

  .dossier__cat {
    justify-content: center;
  }

  .dossier__rank {
    flex-direction: row;
    gap: 10px;
    padding: 10px 22px;
  }

  .dossier__rank b {
    font-size: 30px;
  }

  .dossier {
    padding: 26px 20px 22px;
  }

  .dossier__deed {
    flex-direction: column;
    gap: 8px;
  }

  .dossier__deeds::before {
    display: none;
  }

  .dossier__level-note {
    display: none;
  }
}
</style>
