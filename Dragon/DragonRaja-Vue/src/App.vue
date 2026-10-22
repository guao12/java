<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AuthLayout from '@/components/AuthLayout.vue'

const route = useRoute()
/** 首页等带独立导航栏的页面不套认证页外壳 */
const standalone = computed(() => !!route.meta.standalone)
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
