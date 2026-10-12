<script setup>
import AuthLayout from '@/components/AuthLayout.vue'
</script>

<template>
  <AuthLayout>
    <router-view v-slot="{ Component }">
      <transition name="route-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </AuthLayout>
</template>
