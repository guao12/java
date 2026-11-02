<script setup>
import { computed, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import AuthLayout from '@/components/AuthLayout.vue'

const route = useRoute()
/** 首页等带独立导航栏的页面不套认证页外壳 */
const standalone = computed(() => !!route.meta.standalone)

/**
 * ⚠ 约束：每个路由视图的模板必须是「单一根节点」。
 * 这里用 <transition mode="out-in"> 包裹路由组件，而 Vue 无法给多根 fragment 做过渡：
 * 旧页面的离场钩子永远不触发 → 新页面不会被插入 → 整页空白。
 * 历史事故：AdminView（遮罩 + .adm 两个根节点）→ 后台点「学院前台」白屏。
 * 下面的开发期自检会在再次出现白屏时把根因直接打到控制台。
 */
if (import.meta.env.DEV) {
  watch(
    () => route.fullPath,
    async () => {
      await nextTick()
      const app = document.querySelector('#app')
      if (app && app.childElementCount === 0) {
        console.error(
          '[路由自检] 视图渲染为空：请检查该视图模板是否只有单一根节点' +
            '（<transition mode="out-in"> 无法处理多根 fragment）。当前路由：' +
            route.fullPath
        )
      }
    }
  )
}
</script>

<template>
  <router-view v-slot="{ Component }">
    <AuthLayout v-if="!standalone">
      <transition name="route-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </AuthLayout>
    <transition v-else name="route-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>
