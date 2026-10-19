<script setup>
/**
 * 图鉴独立详情页 —— 言灵谱（/spirit/:key）与机构志（/org/:key）共用。
 * 人物志走 /codex/:key（CharacterView.vue）。
 * 视觉语言与人物档案页同源：鎏金卷宗卡 + 密级条 + 分节档案。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import DragonEmblem from '@/components/DragonEmblem.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import { ORG_GALLERY, SPIRIT_GALLERY, codexPath } from '@/data/codex'

const props = defineProps({
  /** spirit 言灵谱 / org 机构志 */
  kind: { type: String, default: 'spirit' }
})

const route = useRoute()
const router = useRouter()

const isSpirit = computed(() => props.kind === 'spirit')
const list = computed(() => (isSpirit.value ? SPIRIT_GALLERY : ORG_GALLERY))
const item = computed(() => list.value.find((i) => i.key === route.params.key) || null)

/** 同类其他条目（底部「相关档案」，可点击互跳） */
const related = computed(() => list.value.filter((i) => i.key !== route.params.key).slice(0, 6))

const catText = computed(() => (isSpirit.value ? '言灵谱 · 言灵档案' : '机构志 · 机构档案'))
const dir = computed(() => (isSpirit.value ? 'spirit' : 'org'))

/** 头像：图片加载失败时回退为「首字 + 纹章框」 */
const imgOk = ref(true)
function onImgError() {
  imgOk.value = false
}

onMounted(() => {
  document.documentElement.style.colorScheme = 'dark'
  document.title = item.value
    ? `${isSpirit.value ? '言灵' : '机构'}档案 · ${item.value.name} | 卡塞尔学院`
    : '图鉴档案 | 卡塞尔学院'
})

/** 返回时带上锚点，直接落回首页对应图鉴区块 */
function goBack() {
  router.push({ path: '/home', hash: isSpirit.value ? '#sec-codex' : '#sec-org' })
}

function go(key) {
  router.push(codexPath(props.kind, key))
}
</script>

<template>
  <div class="codex">
    <RuneBackground />

    <!-- ============ 顶部导航 ============ -->
    <header class="codex__nav">
      <div class="codex__nav-inner">
        <div class="codex__brand">
          <DragonEmblem class="codex__mark" />
          <div class="codex__brand-text">
            <span class="codex__brand-cn">卡塞尔学院</span>
            <span class="codex__brand-en">Cassell College</span>
          </div>
        </div>
        <button type="button" class="codex__back" @click="goBack">
          <AppIcon name="arrow" :size="14" class="codex__back-ic" />
          返回档案馆
        </button>
      </div>
    </header>

    <!-- ============ 主体 ============ -->
    <main class="codex__body">
      <article v-if="item" class="dossier">
        <i class="dossier__beam" />

        <!-- 档案头 -->
        <header class="dossier__head">
          <div class="dossier__avatar" :data-tone="isSpirit ? 'fire' : 'gold'">
            <img
              v-if="imgOk"
              class="dossier__avatar-img"
              :src="`/img/${dir}/${item.key}.webp`"
              :alt="`${catText} · ${item.name}`"
              @error="onImgError"
            />
            <span v-else class="dossier__avatar-txt">{{ item.name.slice(0, 1) }}</span>
            <i class="dossier__avatar-ring" />
            <i class="dossier__avatar-corner dossier__avatar-corner--tl" />
            <i class="dossier__avatar-corner dossier__avatar-corner--br" />
          </div>

          <div class="dossier__info">
            <p class="dossier__cat">
              <span class="dossier__cat-chip">{{ catText }}</span>
              <template v-if="isSpirit">{{ item.element }}系 · {{ item.effect }}</template>
              <template v-else>{{ item.code }} · {{ item.duty }}</template>
            </p>
            <h1 class="dossier__name">{{ item.name }}</h1>
            <p class="dossier__role">
              <template v-if="isSpirit">言灵效果：{{ item.effect }}</template>
              <template v-else>机构职责：{{ item.duty }}</template>
            </p>
            <p class="dossier__desc">{{ item.desc }}</p>
          </div>

          <!-- 密级 / 编号大徽章 -->
          <div class="dossier__rank" :data-rank="item.level">
            <b>{{ isSpirit ? item.element : item.code.slice(0, 3) }}</b>
            <span>{{ isSpirit ? '元素系别' : '机构编号' }}</span>
            <em class="dossier__rank-seal" />
          </div>
        </header>

        <!-- 密级条 -->
        <div class="dossier__levelbar">
          <AppIcon name="lock" :size="12" />
          档案密级
          <b class="dossier__level" :data-level="item.level">{{ item.level }}</b>
          <span class="dossier__level-note">调阅已留痕 · 中央档案馆</span>
        </div>

        <!-- 言灵：持有者 / 副作用 -->
        <template v-if="isSpirit">
          <section class="dossier__sec">
            <h2 class="dossier__sec-t"><i />言灵参数</h2>
            <dl class="codex__meta">
              <div>
                <dt>元素系别</dt>
                <dd>{{ item.element }} 系</dd>
              </div>
              <div>
                <dt>效果</dt>
                <dd>{{ item.effect }}</dd>
              </div>
              <div>
                <dt>常见持有者</dt>
                <dd>{{ item.holder }}</dd>
              </div>
            </dl>
          </section>

          <section class="dossier__sec">
            <h2 class="dossier__sec-t"><i />使用代价</h2>
            <p class="codex__warn">
              <AppIcon name="alert" :size="14" />
              {{ item.note }}
            </p>
          </section>
        </template>

        <!-- 机构：编制 / 驻地 -->
        <template v-else>
          <section class="dossier__sec">
            <h2 class="dossier__sec-t"><i />机构编制</h2>
            <dl class="codex__meta">
              <div>
                <dt>机构编号</dt>
                <dd>{{ item.code }}</dd>
              </div>
              <div>
                <dt>{{ item.head.split('：')[0] }}</dt>
                <dd>{{ item.head.split('：').slice(1).join('：') }}</dd>
              </div>
              <div>
                <dt>{{ item.seat.split('：')[0] }}</dt>
                <dd>{{ item.seat.split('：').slice(1).join('：') }}</dd>
              </div>
            </dl>
            <div class="codex__tags">
              <span v-for="t in item.tags" :key="t" class="codex__tag">{{ t }}</span>
            </div>
          </section>

          <section class="dossier__sec">
            <h2 class="dossier__sec-t"><i />职能说明</h2>
            <p class="codex__para">{{ item.desc }}</p>
          </section>
        </template>

        <!-- 相关档案 -->
        <section class="dossier__sec">
          <h2 class="dossier__sec-t"><i />相关档案</h2>
          <ul class="codex__related">
            <li v-for="r in related" :key="r.key">
              <button type="button" class="codex__rel" @click="go(r.key)">
                <em>{{ isSpirit ? r.element : r.code }}</em>
                <b>{{ r.name }}</b>
                <span>{{ isSpirit ? r.effect : r.duty }}</span>
                <AppIcon name="arrow" :size="13" class="codex__rel-ic" />
              </button>
            </li>
          </ul>
        </section>

        <footer class="dossier__foot">
          摘自《{{ isSpirit ? '言灵谱' : '机构志' }}》 · 卡塞尔学院档案馆藏 · 本页调阅已计入日志
        </footer>
      </article>

      <!-- 条目不存在 -->
      <div v-else class="dossier--void">
        <AppIcon name="archive" :size="40" :stroke="1.2" />
        <p>档案馆中没有该条目的卷宗</p>
        <button type="button" class="dossier__void-btn" @click="goBack">返回档案馆</button>
      </div>
    </main>
  </div>
</template>

<style scoped>
.codex {
  position: relative;
  z-index: 1;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* ================= 顶部导航 ================= */
.codex__nav {
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
  padding: 26px 24px 0;
  animation: fade-down 0.7s 0.05s both;
}

.codex__nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.codex__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.codex__mark {
  width: 38px;
  height: 38px;
  filter: drop-shadow(0 0 10px rgba(216, 178, 90, 0.5));
  animation: spin 46s linear infinite;
}

.codex__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.codex__brand-cn {
  font-family: var(--font-serif);
  font-size: 15px;
  letter-spacing: 0.26em;
  color: var(--gold-lt);
}

.codex__brand-en {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.3em;
  color: var(--text-4);
  text-transform: uppercase;
}

.codex__back {
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

.codex__back:hover {
  border-color: var(--gold);
  background: rgba(216, 178, 90, 0.14);
  transform: translateX(-3px);
  box-shadow: 0 10px 26px -14px var(--gold-glow);
}

.codex__back-ic {
  transform: rotate(180deg);
}

/* ================= 主体 ================= */
.codex__body {
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
  box-shadow: var(--shadow-card), 0 0 30px -18px rgba(216, 178, 90, 0.3);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  animation: card-in 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
  overflow: hidden;
}

.dossier__beam {
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold-lt), var(--gold-antique), transparent);
}

/* ================= 档案头 ================= */
.dossier__head {
  display: flex;
  align-items: center;
  gap: 26px;
  padding-bottom: 22px;
  border-bottom: 1px solid rgba(138, 106, 40, 0.28);
}

.dossier__avatar {
  position: relative;
  flex: 0 0 118px;
  width: 118px;
  height: 118px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid rgba(216, 178, 90, 0.4);
  background: radial-gradient(circle at 50% 35%, rgba(216, 178, 90, 0.16), rgba(8, 10, 18, 0.9));
}

.dossier__avatar[data-tone='fire'] {
  border-color: rgba(239, 107, 98, 0.42);
  background: radial-gradient(circle at 50% 35%, rgba(239, 107, 98, 0.18), rgba(8, 10, 18, 0.9));
}

.dossier__avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.dossier__avatar-txt {
  font-family: var(--font-serif);
  font-size: 52px;
  font-weight: 700;
  color: var(--gold-lt);
  text-shadow: 0 0 24px var(--gold-glow);
}

.dossier__avatar-ring {
  position: absolute;
  inset: 7px;
  border-radius: 10px;
  border: 1px solid rgba(216, 178, 90, 0.28);
}

.dossier__avatar-corner {
  position: absolute;
  width: 14px;
  height: 14px;
  border: 1px solid var(--gold-dp);
}

.dossier__avatar-corner--tl {
  top: 4px;
  left: 4px;
  border-right: 0;
  border-bottom: 0;
}

.dossier__avatar-corner--br {
  bottom: 4px;
  right: 4px;
  border-left: 0;
  border-top: 0;
}

.dossier__info {
  flex: 1;
  min-width: 0;
}

.dossier__cat {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--text-4);
  margin-bottom: 7px;
}

.dossier__cat-chip {
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(216, 178, 90, 0.07);
  color: var(--gold-dp);
}

.dossier__name {
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--text);
  text-shadow: 0 0 26px rgba(216, 178, 90, 0.28);
  margin-bottom: 6px;
}

.dossier__role {
  font-size: 12.5px;
  letter-spacing: 0.08em;
  color: var(--gold-lt);
  margin-bottom: 10px;
}

.dossier__desc {
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-3);
}

/* 大徽章 */
.dossier__rank {
  flex: 0 0 92px;
  position: relative;
  display: grid;
  place-items: center;
  gap: 2px;
  padding: 14px 8px;
  border-radius: 12px;
  border: 1px solid rgba(216, 178, 90, 0.4);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.16), rgba(8, 10, 18, 0.6));
}

.dossier__rank b {
  font-family: var(--font-serif);
  font-size: 22px;
  letter-spacing: 0.04em;
  color: var(--gold-lt);
  text-shadow: 0 0 16px var(--gold-glow);
}

.dossier__rank span {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--text-4);
}

.dossier__rank-seal {
  position: absolute;
  right: -6px;
  bottom: -6px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid rgba(216, 178, 90, 0.45);
  background: rgba(216, 178, 90, 0.1);
}

/* ================= 密级条 ================= */
.dossier__levelbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 9px 14px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.45);
  font-size: 11.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.dossier__level {
  font-family: var(--font-mono);
  font-size: 11.5px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
}

.dossier__level[data-level='绝密'] {
  color: var(--err);
}

.dossier__level-note {
  margin-left: auto;
  font-size: 10.5px;
  color: var(--text-5, var(--text-4));
}

/* ================= 分节 ================= */
.dossier__sec {
  margin-top: 26px;
}

.dossier__sec-t {
  display: flex;
  align-items: center;
  gap: 9px;
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 0.16em;
  color: var(--gold-lt);
  margin-bottom: 12px;
}

.dossier__sec-t i {
  width: 3px;
  height: 13px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--gold-lt), var(--gold-dp));
}

.codex__meta {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.codex__meta > div {
  padding: 12px 14px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.4);
}

.codex__meta dt {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  margin-bottom: 5px;
}

.codex__meta dd {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-2);
}

.codex__para {
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-3);
}

.codex__warn {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 14px;
  border-radius: var(--r-sm);
  font-size: 12.5px;
  line-height: 1.8;
  color: #ffd9d6;
  border: 1px solid rgba(239, 107, 98, 0.4);
  background: linear-gradient(120deg, rgba(179, 39, 45, 0.2), rgba(4, 7, 13, 0.45));
}

.codex__warn svg {
  flex: 0 0 auto;
  margin-top: 2px;
}

.codex__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.codex__tag {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--gold-lt);
  padding: 3px 11px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.3);
  background: rgba(216, 178, 90, 0.08);
}

/* ================= 相关档案 ================= */
.codex__related {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 9px;
}

.codex__rel {
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 11px 13px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line-soft);
  background: linear-gradient(120deg, rgba(20, 28, 46, 0.55), rgba(8, 12, 22, 0.7));
  cursor: pointer;
  text-align: left;
  transition: border-color 0.24s, transform 0.24s, box-shadow 0.24s;
}

.codex__rel:hover {
  border-color: rgba(216, 178, 90, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 16px 34px -20px rgba(0, 0, 0, 0.9);
}

.codex__rel em {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: var(--gold-dp);
  padding: 3px 7px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(216, 178, 90, 0.06);
}

.codex__rel b {
  font-size: 13px;
  letter-spacing: 0.08em;
  color: var(--text);
}

.codex__rel span {
  font-size: 11.5px;
  color: var(--text-4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.codex__rel-ic {
  color: var(--gold-lt);
}

.dossier__foot {
  margin-top: 26px;
  padding-top: 14px;
  border-top: 1px dashed rgba(138, 106, 40, 0.28);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--text-4);
  text-align: center;
}

/* ================= 空态 ================= */
.dossier--void {
  display: grid;
  place-items: center;
  gap: 14px;
  padding: 90px 20px;
  border-radius: var(--r-lg);
  border: 1px dashed var(--line);
  color: var(--text-4);
  text-align: center;
}

.dossier__void-btn {
  padding: 9px 20px;
  border-radius: 999px;
  border: 1px solid rgba(216, 178, 90, 0.32);
  background: rgba(216, 178, 90, 0.07);
  color: var(--gold-lt);
  font-size: 12.5px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: background 0.24s, border-color 0.24s;
}

.dossier__void-btn:hover {
  background: rgba(216, 178, 90, 0.16);
  border-color: var(--gold);
}

@media (max-width: 720px) {
  .dossier__head {
    flex-wrap: wrap;
    gap: 16px;
  }

  .dossier__rank {
    flex: 0 0 auto;
    width: 92px;
  }
}
</style>
