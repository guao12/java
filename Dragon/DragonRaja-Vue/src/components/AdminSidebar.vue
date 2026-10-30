<script setup>
/**
 * 后台管理系统 · 侧边栏。
 *
 * 结构：品牌 → 分组菜单 → 会话/快捷操作。
 * 交互：
 *  - 桌面端可折叠成图标栏（状态记在 localStorage，刷新后保持）
 *  - 移动端（≤ 920px）变成抽屉：点遮罩 / Esc 关闭
 *  - 数字键 1..n 快速切菜单（输入框内不触发）
 *
 * 纯展示组件：当前菜单、折叠态、抽屉开合都由父级传入，便于父页面持久化与快捷键联动。
 */
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'

const props = defineProps({
  /** 菜单分组：[{ title, items: [{ key, label, desc, icon, badge }] }] */
  groups: { type: Array, default: () => [] },
  /** 当前激活项 key */
  modelValue: { type: String, default: '' },
  /** 管理员名 */
  adminName: { type: String, default: '管理员' },
  /** 桌面折叠态 */
  collapsed: { type: Boolean, default: false },
  /** 移动端抽屉是否展开 */
  open: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'toggle-collapse', 'close', 'logout'])

/** 是否有分组标题需要显示（折叠时隐藏标题） */
const showCaps = computed(() => !props.collapsed)

function pick(key) {
  emit('update:modelValue', key)
  emit('close')
}
</script>

<template>
  <aside
    class="sb"
    :class="{ 'is-collapsed': collapsed, 'is-open': open }"
    aria-label="后台导航"
  >
    <!-- 品牌区 -->
    <div class="sb__brand">
      <span class="sb__mark">
        <AppIcon name="shield" :size="17" />
      </span>
      <span class="sb__brand-txt">
        <b>后台管理系统</b>
        <em>Cassell · Admin</em>
      </span>
    </div>

    <!-- 菜单 -->
    <nav class="sb__nav">
      <template v-for="(g, gi) in groups" :key="g.title || gi">
        <p v-if="showCaps && g.title" class="sb__cap">{{ g.title }}</p>
        <button
          v-for="m in g.items"
          :key="m.key"
          type="button"
          class="sb__item"
          :class="{ 'is-active': modelValue === m.key }"
          :title="collapsed ? `${m.label} · ${m.desc}` : undefined"
          :aria-current="modelValue === m.key ? 'page' : undefined"
          @click="pick(m.key)"
        >
          <i v-if="modelValue === m.key" class="sb__glow" />
          <AppIcon :name="m.icon" :size="16" class="sb__icon" />
          <span v-if="showCaps" class="sb__txt">
            <b>{{ m.label }}</b>
            <em>{{ m.desc }}</em>
          </span>
          <span v-if="m.badge != null && m.badge !== ''" class="sb__badge" :class="{ 'is-dot': !showCaps }">
            {{ m.badge }}
          </span>
        </button>
      </template>
    </nav>

    <!-- 底部：折叠 / 关闭 + 会话 -->
    <div class="sb__foot">
      <div class="sb__switch">
        <button
          type="button"
          class="sb__collapse"
          :title="collapsed ? '展开侧边栏' : '收起侧边栏'"
          :aria-label="collapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="emit('toggle-collapse')"
        >
          <AppIcon :name="collapsed ? 'chevron-right' : 'chevron-left'" :size="14" />
          <span v-if="showCaps">{{ collapsed ? '展开' : '收起' }}</span>
        </button>
        <button
          type="button"
          class="sb__close"
          title="关闭"
          aria-label="关闭导航"
          @click="emit('close')"
        >
          <AppIcon name="close" :size="15" />
          <span>关闭</span>
        </button>
      </div>
      <div class="sb__who" :title="adminName">
        <i class="sb__who-dot" />
        <span v-if="showCaps">
          <b>{{ adminName }}</b>
          <em>管理员会话 · 已加密</em>
        </span>
      </div>
      <router-link class="sb__link" to="/home" title="学院前台">
        <AppIcon name="home" :size="14" />
        <span v-if="showCaps">学院前台</span>
      </router-link>
      <button type="button" class="sb__link is-danger" title="退出登录" @click="emit('logout')">
        <AppIcon name="logout" :size="14" />
        <span v-if="showCaps">退出登录</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sb {
  --sb-w: 232px;
  position: sticky;
  top: 76px;
  flex: 0 0 var(--sb-w);
  width: var(--sb-w);
  align-self: flex-start;
  max-height: calc(100vh - 96px);
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 12px 10px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: linear-gradient(170deg, rgba(26, 21, 38, 0.82), rgba(11, 9, 18, 0.94));
  box-shadow: var(--shadow-card);
  transition: width 0.24s cubic-bezier(0.16, 1, 0.3, 1),
    flex-basis 0.24s cubic-bezier(0.16, 1, 0.3, 1);
}

.sb.is-collapsed {
  --sb-w: 66px;
}

/* ---------- 品牌 ---------- */
.sb__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px 10px;
  border-bottom: 1px solid var(--line-soft);
  min-height: 44px;
  overflow: hidden;
}

.sb__mark {
  display: grid;
  place-items: center;
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  color: var(--gold-lt);
  border: 1px solid rgba(138, 106, 40, 0.55);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.16), rgba(138, 106, 40, 0.06));
}

.sb__brand-txt {
  display: grid;
  gap: 1px;
  min-width: 0;
  flex: 1;
}

.sb__brand-txt b {
  font-family: var(--font-serif);
  font-size: 13.5px;
  letter-spacing: 0.12em;
  color: var(--text);
  white-space: nowrap;
}

.sb__brand-txt em {
  font-family: var(--font-mono);
  font-style: normal;
  font-size: 9.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-4);
  white-space: nowrap;
}

/* 折叠 / 关闭开关（放在底部，避免遮住菜单） */
.sb__switch {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sb__collapse,
.sb__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  flex: 1;
  min-height: 28px;
  padding: 4px 8px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line-soft);
  background: none;
  font: inherit;
  font-size: 11px;
  letter-spacing: 0.06em;
  color: var(--text-4);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}

.sb__collapse:hover,
.sb__close:hover {
  color: var(--gold-lt);
  border-color: rgba(216, 178, 90, 0.4);
  background: rgba(216, 178, 90, 0.06);
}

.sb__close {
  display: none;
}

.sb.is-collapsed .sb__brand {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

/* 折叠时只留徽标，避免文字溢出压到主体 */
.sb.is-collapsed .sb__brand-txt {
  display: none;
}

.sb.is-collapsed .sb__switch {
  justify-content: center;
}

/* ---------- 菜单 ---------- */
.sb__nav {
  display: grid;
  gap: 4px;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1;
  min-height: 0;
  padding-bottom: 4px;
}

.sb__cap {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--text-4);
  padding: 8px 10px 2px;
}

.sb__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: 1px solid transparent;
  border-radius: var(--r-md);
  background: none;
  font: inherit;
  text-align: left;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.18s, border-color 0.18s, background 0.18s;
}

.sb__item:hover {
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.06);
}

.sb__item.is-active {
  color: var(--gold-lt);
  border-color: rgba(138, 106, 40, 0.55);
  background: linear-gradient(180deg, rgba(216, 178, 90, 0.15), rgba(138, 106, 40, 0.07));
}

.sb__glow {
  position: absolute;
  left: -1px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 58%;
  border-radius: 999px;
  background: var(--gold-lt);
  box-shadow: 0 0 10px var(--gold-glow);
}

.sb__icon {
  flex: 0 0 auto;
}

.sb__txt {
  display: grid;
  gap: 1px;
  min-width: 0;
  flex: 1;
}

.sb__txt b {
  font-size: 13px;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.sb__txt em {
  font-style: normal;
  font-size: 10.5px;
  color: var(--text-4);
  letter-spacing: 0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sb__badge {
  flex: 0 0 auto;
  min-width: 20px;
  padding: 1px 6px;
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  text-align: center;
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.12);
  border: 1px solid rgba(138, 106, 40, 0.45);
}

.sb.is-collapsed .sb__item {
  justify-content: center;
  padding: 10px 0;
}

.sb.is-collapsed .sb__badge {
  position: absolute;
  top: 4px;
  right: 6px;
  min-width: 0;
  padding: 0 4px;
  font-size: 9px;
  line-height: 14px;
}

.sb.is-collapsed .sb__badge.is-dot {
  top: 7px;
  right: 9px;
  width: 6px;
  height: 6px;
  min-width: 0;
  padding: 0;
  border: 0;
  color: transparent;
  background: var(--gold-lt);
  box-shadow: 0 0 6px var(--gold-glow);
}

/* ---------- 底部 ---------- */
.sb__foot {
  display: grid;
  gap: 7px;
  padding-top: 10px;
  border-top: 1px solid var(--line-soft);
}

.sb__who {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px 4px;
  min-width: 0;
}

.sb__who-dot {
  flex: 0 0 7px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--gold-lt);
  box-shadow: 0 0 8px var(--gold-glow);
}

.sb__who span {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.sb__who b {
  font-size: 12.5px;
  color: var(--text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sb__who em {
  font-style: normal;
  font-size: 10px;
  color: var(--text-4);
  white-space: nowrap;
}

.sb__link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border: 1px solid transparent;
  border-radius: var(--r-md);
  background: none;
  font: inherit;
  font-size: 12px;
  letter-spacing: 0.04em;
  color: var(--text-3);
  text-decoration: none;
  cursor: pointer;
  transition: color 0.18s, background 0.18s, border-color 0.18s;
}

.sb__link:hover {
  color: var(--gold-lt);
  background: rgba(216, 178, 90, 0.06);
  border-color: rgba(216, 178, 90, 0.22);
}

.sb__link.is-danger:hover {
  color: var(--blood-lt);
  background: rgba(179, 39, 45, 0.08);
  border-color: rgba(179, 39, 45, 0.4);
}

.sb.is-collapsed .sb__link {
  justify-content: center;
  padding: 8px 0;
}

.sb.is-collapsed .sb__who {
  justify-content: center;
  padding: 0 0 6px;
}

/* ---------- 移动端遮罩 ---------- */
@media (max-width: 920px) {
  .sb {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 70;
    width: min(288px, 84vw);
    max-height: none;
    box-shadow: 0 0 60px -10px rgba(0, 0, 0, 0.9);
    border-radius: 0 var(--r-lg) var(--r-lg) 0;
    border-left: 0;
    padding: 14px 12px;
    overflow-y: auto;
    transform: translateX(-102%);
    transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1);
    will-change: transform;
  }

  /* 抽屉要有实底，否则会透出下层内容 */
  .sb.sb {
    background: linear-gradient(170deg, #171223 0%, #0a0811 100%);
  }

  .sb.is-collapsed {
    width: min(288px, 84vw);
  }

  .sb.is-open {
    transform: translateX(0);
  }

  /* 移动端强制展开内容，忽略折叠态 */
  .sb.is-collapsed .sb__brand {
    justify-content: flex-start;
  }

  .sb.is-collapsed .sb__item {
    justify-content: flex-start;
    padding: 9px 10px;
  }

  .sb.is-collapsed .sb__link {
    justify-content: flex-start;
    padding: 7px 10px;
  }

  .sb.is-collapsed .sb__who {
    justify-content: flex-start;
    padding: 0 8px 4px;
  }

  .sb__collapse {
    display: none;
  }

  .sb__close {
    display: inline-flex;
  }

  .sb__switch {
    margin-bottom: 2px;
  }


}
</style>
