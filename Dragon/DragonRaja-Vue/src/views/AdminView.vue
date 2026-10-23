<script setup>
/**
 * 后台管理系统 · 言灵图鉴数据管理。
 * 对首页「言灵图鉴」区块与 /spirit/:key 详情页的数据做增删改查：
 *  - 列表：全部条目（含隐藏），key/名称/元素/密级/排序/状态
 *  - 新增 / 编辑：弹窗表单，key 唯一性由后端校验
 *  - 删除：二次确认（逻辑删除）
 * 数据接口见 src/api/admin.js，仅管理员令牌可访问。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import GlowButton from '@/components/GlowButton.vue'
import RuneBackground from '@/components/RuneBackground.vue'
import { adminApi } from '@/api/admin'
import { clearAdminSession, getAdminName, saveAdminSession } from '@/utils/adminAuth'

const router = useRouter()

/* ---------------- 会话 ---------------- */

const adminName = ref(getAdminName() || '管理员')

/* ---------------- 列表 ---------------- */

const spirits = ref([])
const loading = ref(false)
const listError = ref('')
const keyword = ref('')

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return spirits.value
  return spirits.value.filter(
    (s) =>
      s.key.toLowerCase().includes(k) ||
      s.name.toLowerCase().includes(k) ||
      s.element.toLowerCase().includes(k) ||
      s.level.toLowerCase().includes(k)
  )
})

async function load() {
  loading.value = true
  listError.value = ''
  try {
    const data = await adminApi.listSpirits()
    spirits.value = data || []
  } catch (e) {
    listError.value = e.message
    if (e.code === 401) router.replace('/admin/login')
  } finally {
    loading.value = false
  }
}

/* ---------------- 新增 / 编辑弹窗 ---------------- */

const LEVELS = ['内部', '机密', '高危', '绝密']
const ELEMENTS = ['火', '冰', '风', '重力', '时间', '精神', '暗影', '雷', '威压']

const showModal = ref(false)
const editingId = ref(null) // null = 新增
const saving = ref(false)
const modalError = ref('')

const emptyForm = () => ({
  key: '',
  name: '',
  element: '',
  level: '内部',
  effect: '',
  desc: '',
  holder: '',
  note: '',
  sort: 0,
  status: 1
})

const form = reactive(emptyForm())

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalError.value = ''
  showModal.value = true
}

function openEdit(s) {
  editingId.value = s.id
  Object.assign(form, {
    key: s.key,
    name: s.name,
    element: s.element,
    level: s.level,
    effect: s.effect,
    desc: s.desc,
    holder: s.holder || '',
    note: s.note || '',
    sort: s.sort ?? 0,
    status: s.status ?? 1
  })
  modalError.value = ''
  showModal.value = true
}

function checkForm() {
  if (!/^[a-z0-9][a-z0-9-]{0,49}$/.test(form.key)) return 'key 只能是小写字母、数字和连字符（如 junyan）'
  if (!form.name.trim()) return '请填写言灵名'
  if (!form.element.trim()) return '请填写元素系别'
  if (!form.effect.trim()) return '请填写言灵效果'
  if (!form.desc.trim()) return '请填写详细介绍'
  return ''
}

async function onSave() {
  modalError.value = checkForm()
  if (modalError.value || saving.value) return
  saving.value = true
  try {
    const payload = {
      key: form.key.trim(),
      name: form.name.trim(),
      element: form.element.trim(),
      level: form.level,
      effect: form.effect.trim(),
      desc: form.desc.trim(),
      holder: form.holder.trim(),
      note: form.note.trim(),
      sort: Number(form.sort) || 0,
      status: Number(form.status) || 1
    }
    if (editingId.value == null) await adminApi.createSpirit(payload)
    else await adminApi.updateSpirit(editingId.value, payload)
    showModal.value = false
    await load()
  } catch (e) {
    modalError.value = e.message
    if (e.code === 401) {
      showModal.value = false
      router.replace('/admin/login')
    }
  } finally {
    saving.value = false
  }
}

/* ---------------- 删除 ---------------- */

const confirmTarget = ref(null) // 待删除条目
const deleting = ref(false)

function askDelete(s) {
  confirmTarget.value = s
}

async function onDelete() {
  if (!confirmTarget.value || deleting.value) return
  deleting.value = true
  try {
    await adminApi.deleteSpirit(confirmTarget.value.id)
    confirmTarget.value = null
    await load()
  } catch (e) {
    listError.value = e.message
    confirmTarget.value = null
    if (e.code === 401) router.replace('/admin/login')
  } finally {
    deleting.value = false
  }
}

/* ---------------- 退出 ---------------- */

function logout() {
  clearAdminSession()
  router.replace('/admin/login')
}

onMounted(() => {
  document.documentElement.style.colorScheme = 'dark'
  document.title = '后台管理系统 | 卡塞尔学院'
  load()
})
</script>

<template>
  <div class="adm">
    <RuneBackground />

    <!-- ============ 顶栏 ============ -->
    <header class="adm__bar">
      <div class="adm__bar-inner">
        <div class="adm__title">
          <AppIcon name="shield" :size="18" />
          <div>
            <h1>后台管理系统</h1>
            <p>言灵图鉴 · 数据管理</p>
          </div>
        </div>
        <div class="adm__bar-side">
          <span class="adm__who">
            <i class="adm__who-dot" />
            {{ adminName }}
          </span>
          <router-link class="adm__link" to="/home">
            <AppIcon name="home" :size="14" /> 学院前台
          </router-link>
          <button type="button" class="adm__link is-danger" @click="logout">
            <AppIcon name="logout" :size="14" /> 退出登录
          </button>
        </div>
      </div>
    </header>

    <!-- ============ 主体 ============ -->
    <main class="adm__body">
      <!-- 工具条 -->
      <div class="adm__tools">
        <label class="adm__search">
          <AppIcon name="search" :size="14" />
          <input v-model="keyword" type="text" placeholder="按 key / 名称 / 元素 / 密级过滤" />
        </label>
        <div class="adm__tools-side">
          <span class="adm__count">共 {{ filtered.length }} 条 / 上架 {{ spirits.filter((s) => s.status === 1).length }} 条</span>
          <GlowButton variant="ghost" type="button" :disabled="loading" @click="load">
            <AppIcon name="refresh" :size="15" :class="{ 'is-spin': loading }" />
            刷新
          </GlowButton>
          <GlowButton type="button" @click="openCreate">
            <AppIcon name="plus" :size="15" /> 新增言灵
          </GlowButton>
        </div>
      </div>

      <div v-if="listError" class="adm__alert">
        <AppIcon name="alert" :size="15" />
        <span>{{ listError }}</span>
      </div>

      <!-- 数据表 -->
      <div class="adm__table-wrap">
        <table class="adm__table">
          <thead>
            <tr>
              <th>排序</th>
              <th>插画 Key</th>
              <th>言灵名</th>
              <th>元素</th>
              <th>密级</th>
              <th>言灵效果</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in filtered" :key="s.id">
              <td class="num">{{ s.sort }}</td>
              <td><code class="key">{{ s.key }}</code></td>
              <td class="name">{{ s.name }}</td>
              <td>{{ s.element }}</td>
              <td>
                <span class="lv" :data-lv="s.level">{{ s.level }}</span>
              </td>
              <td class="effect">{{ s.effect }}</td>
              <td>
                <span class="st" :class="s.status === 1 ? 'is-on' : 'is-off'">
                  {{ s.status === 1 ? '展示中' : '已隐藏' }}
                </span>
              </td>
              <td class="ops">
                <button type="button" class="op" @click="openEdit(s)">编辑</button>
                <button type="button" class="op is-danger" @click="askDelete(s)">删除</button>
              </td>
            </tr>
            <tr v-if="!loading && !filtered.length">
              <td colspan="8" class="empty">
                {{ listError ? '加载失败，请刷新重试' : '暂无数据，点击右上角「新增言灵」录入' }}
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="loading && !spirits.length" class="adm__loading">正在读取图鉴数据…</div>
      </div>

      <p class="adm__tip">
        <AppIcon name="alert" :size="13" />
        插画按 <code>/img/spirit/&lt;key&gt;.webp</code> 命名放置；「隐藏」的条目前台（首页图鉴与详情页）不再展示，数据保留。
      </p>
    </main>

    <!-- ============ 新增 / 编辑弹窗 ============ -->
    <div v-if="showModal" class="adm__mask" @click.self="showModal = false">
      <div class="adm__modal">
        <header class="adm__modal-head">
          <h2>{{ editingId == null ? '新增言灵' : `编辑 · ${form.name}` }}</h2>
          <button type="button" class="adm__modal-x" @click="showModal = false">
            <AppIcon name="close" :size="16" />
          </button>
        </header>

        <div class="adm__modal-body">
          <div v-if="modalError" class="adm__alert">
            <AppIcon name="alert" :size="15" />
            <span>{{ modalError }}</span>
          </div>

          <div class="grid-2">
            <label class="fld">
              <span>插画 Key <em>* 路由与图片地址，新增后不建议修改</em></span>
              <input v-model.trim="form.key" type="text" placeholder="如 junyan" />
            </label>
            <label class="fld">
              <span>言灵名 *</span>
              <input v-model.trim="form.name" type="text" placeholder="如 君焰" />
            </label>
            <label class="fld">
              <span>元素系别 *</span>
              <input v-model.trim="form.element" list="adm-elements" placeholder="如 火 / 冰 / 风暴" />
              <datalist id="adm-elements">
                <option v-for="e in ELEMENTS" :key="e" :value="e" />
              </datalist>
            </label>
            <label class="fld">
              <span>密级 *</span>
              <select v-model="form.level">
                <option v-for="l in LEVELS" :key="l" :value="l">{{ l }}</option>
              </select>
            </label>
          </div>

          <label class="fld">
            <span>言灵效果 *（一句话，列表与详情页标题）</span>
            <input v-model.trim="form.effect" type="text" maxlength="100" placeholder="如 领域内可燃物瞬间爆燃" />
          </label>

          <label class="fld">
            <span>详细介绍 *</span>
            <textarea v-model.trim="form.desc" rows="4" maxlength="2000" placeholder="展示在首页卡片与详情页的介绍文字" />
          </label>

          <label class="fld">
            <span>典型持有者</span>
            <input v-model.trim="form.holder" type="text" maxlength="100" placeholder="如 攻坚型混血种 · 多见于战斗编制" />
          </label>

          <label class="fld">
            <span>使用禁忌 / 备注</span>
            <textarea v-model.trim="form.note" rows="2" maxlength="200" placeholder="如 持续释放会灼伤自身血脉" />
          </label>

          <div class="grid-2">
            <label class="fld">
              <span>排序（越小越靠前）</span>
              <input v-model.number="form.sort" type="number" min="0" max="9999" />
            </label>
            <label class="fld">
              <span>状态</span>
              <select v-model.number="form.status">
                <option :value="1">展示中（前台可见）</option>
                <option :value="0">隐藏（前台不可见）</option>
              </select>
            </label>
          </div>
        </div>

        <footer class="adm__modal-foot">
          <GlowButton variant="ghost" type="button" @click="showModal = false">取消</GlowButton>
          <GlowButton type="button" :disabled="saving" @click="onSave">
            {{ saving ? '保存中…' : '保存' }}
          </GlowButton>
        </footer>
      </div>
    </div>

    <!-- ============ 删除确认 ============ -->
    <div v-if="confirmTarget" class="adm__mask" @click.self="confirmTarget = null">
      <div class="adm__modal adm__modal--sm">
        <header class="adm__modal-head">
          <h2>删除确认</h2>
          <button type="button" class="adm__modal-x" @click="confirmTarget = null">
            <AppIcon name="close" :size="16" />
          </button>
        </header>
        <p class="adm__confirm-txt">
          确定要删除言灵「<b>{{ confirmTarget.name }}</b>（key: {{ confirmTarget.key }}）」吗？
          删除后首页图鉴与详情页将不再展示该条目。
        </p>
        <footer class="adm__modal-foot">
          <GlowButton variant="ghost" type="button" @click="confirmTarget = null">取消</GlowButton>
          <GlowButton type="button" class="is-danger" :disabled="deleting" @click="onDelete">
            {{ deleting ? '删除中…' : '确认删除' }}
          </GlowButton>
        </footer>
      </div>
    </div>
  </div>
</template>

<style scoped>
.adm {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ================= 顶栏 ================= */
.adm__bar {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(5, 7, 13, 0.82);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom: 1px solid var(--line);
}

.adm__bar-inner {
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 24px;
}

.adm__title {
  display: flex;
  align-items: center;
  gap: 11px;
  color: var(--gold-lt);
}

.adm__title h1 {
  font-family: var(--font-serif);
  font-size: 17px;
  letter-spacing: 0.16em;
  color: var(--text);
}

.adm__title p {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.22em;
  color: var(--text-4);
  text-transform: uppercase;
}

.adm__bar-side {
  display: flex;
  align-items: center;
  gap: 10px;
}

.adm__who {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--text-2);
}

.adm__who-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--gold-lt);
  box-shadow: 0 0 8px var(--gold-glow);
}

.adm__link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 13px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--text-2);
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.adm__link:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.45);
}

.adm__link.is-danger:hover {
  color: var(--blood-lt);
  border-color: rgba(179, 39, 45, 0.5);
}

/* ================= 主体 ================= */
.adm__body {
  flex: 1;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  padding: 26px 24px 46px;
}

.adm__tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.adm__search {
  flex: 1;
  min-width: 220px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 15px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.55);
  color: var(--text-4);
}

.adm__search input {
  flex: 1;
  border: 0;
  background: none;
  font: inherit;
  font-size: 13px;
  color: var(--text);
  outline: none;
}

.adm__tools-side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.adm__count {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--text-4);
  margin-right: 2px;
}

.adm__alert {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px 14px;
  margin-bottom: 14px;
  border-radius: var(--r-md);
  font-size: 12.5px;
  color: var(--blood-lt);
  border: 1px solid rgba(179, 39, 45, 0.45);
  background: rgba(179, 39, 45, 0.1);
}

/* ================= 数据表 ================= */
.adm__table-wrap {
  position: relative;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: linear-gradient(165deg, rgba(26, 21, 38, 0.72), rgba(11, 9, 18, 0.9));
  box-shadow: var(--shadow-card);
  overflow: auto;
}

.adm__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.adm__table th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 12px 14px;
  text-align: left;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: var(--gold-lt);
  background: rgba(14, 11, 23, 0.96);
  border-bottom: 1px solid var(--line);
}

.adm__table td {
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: var(--text-2);
  vertical-align: middle;
}

.adm__table tbody tr {
  transition: background 0.18s;
}

.adm__table tbody tr:hover {
  background: rgba(216, 178, 90, 0.05);
}

.adm__table .num {
  font-family: var(--font-mono);
  color: var(--text-4);
}

.adm__table .key {
  font-family: var(--font-mono);
  font-size: 11.5px;
  padding: 2px 8px;
  border-radius: 5px;
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.09);
  border: 1px solid rgba(138, 106, 40, 0.4);
}

.adm__table .name {
  font-family: var(--font-serif);
  font-size: 14.5px;
  letter-spacing: 0.08em;
  color: var(--text);
  white-space: nowrap;
}

.adm__table .effect {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lv {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--text-3);
  border: 1px solid var(--line);
}

.lv[data-lv='高危'] {
  color: var(--blood-lt);
  border-color: rgba(179, 39, 45, 0.5);
  background: rgba(179, 39, 45, 0.1);
}

.lv[data-lv='机密'] {
  color: var(--gold-lt);
  border-color: rgba(138, 106, 40, 0.7);
  background: rgba(216, 178, 90, 0.1);
}

.lv[data-lv='绝密'] {
  color: #f8ecc4;
  border-color: rgba(216, 178, 90, 0.65);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.3), rgba(138, 106, 40, 0.3));
}

.st {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  white-space: nowrap;
}

.st::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.st.is-on {
  color: var(--gold-lt);
}

.st.is-on::before {
  background: var(--gold-lt);
  box-shadow: 0 0 7px var(--gold-glow);
}

.st.is-off {
  color: var(--text-4);
}

.st.is-off::before {
  background: var(--text-4);
}

.adm__table .ops {
  white-space: nowrap;
  text-align: right;
}

.op {
  padding: 5px 13px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: none;
  font: inherit;
  font-size: 12px;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.op:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.5);
}

.op.is-danger:hover {
  color: var(--blood-lt);
  border-color: rgba(179, 39, 45, 0.55);
}

.adm__table .empty {
  padding: 46px 14px;
  text-align: center;
  color: var(--text-4);
  letter-spacing: 0.08em;
}

.adm__loading {
  padding: 40px;
  text-align: center;
  font-size: 12.5px;
  color: var(--text-4);
  letter-spacing: 0.1em;
}

.adm__tip {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 14px;
  font-size: 11.5px;
  letter-spacing: 0.05em;
  color: var(--text-4);
}

.adm__tip code {
  font-family: var(--font-mono);
  color: var(--gold);
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(216, 178, 90, 0.08);
}

/* ================= 弹窗 ================= */
.adm__mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(3, 4, 8, 0.72);
  backdrop-filter: blur(6px);
  animation: fade-in 0.2s both;
}

.adm__modal {
  width: min(720px, 100%);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: linear-gradient(165deg, rgba(28, 24, 42, 0.97), rgba(10, 8, 17, 0.98));
  box-shadow: var(--shadow-card), 0 0 40px -18px var(--gold-glow);
  animation: card-in 0.35s cubic-bezier(0.16, 1, 0.3, 1) both;
  overflow: hidden;
}

.adm__modal--sm {
  width: min(440px, 100%);
}

.adm__modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line-soft);
}

.adm__modal-head h2 {
  font-family: var(--font-serif);
  font-size: 15.5px;
  letter-spacing: 0.14em;
  color: var(--gold-lt);
}

.adm__modal-x {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid var(--line-soft);
  background: none;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.adm__modal-x:hover {
  color: var(--text);
  border-color: rgba(216, 178, 90, 0.45);
}

.adm__modal-body {
  padding: 18px 20px;
  display: grid;
  gap: 13px;
  overflow: auto;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 13px;
}

.fld {
  display: grid;
  gap: 6px;
}

.fld > span {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--text-3);
}

.fld > span em {
  font-style: normal;
  font-size: 10.5px;
  color: var(--text-4);
  margin-left: 6px;
}

.fld input,
.fld select,
.fld textarea {
  font: inherit;
  font-size: 13px;
  color: var(--text);
  padding: 9px 12px;
  border-radius: var(--r-md);
  border: 1px solid var(--line-soft);
  background: rgba(4, 7, 13, 0.55);
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.fld textarea {
  resize: vertical;
  line-height: 1.7;
}

.fld input:focus,
.fld select:focus,
.fld textarea:focus {
  border-color: rgba(216, 178, 90, 0.55);
  box-shadow: 0 0 0 3px rgba(216, 178, 90, 0.1);
}

.adm__confirm-txt {
  padding: 20px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-2);
}

.adm__confirm-txt b {
  color: var(--blood-lt);
}

.adm__modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--line-soft);
}

.is-spin {
  animation: spin 1s linear infinite;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@media (max-width: 720px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }

  .adm__title p {
    display: none;
  }
}
</style>
