<template>
  <div class="app">
    <RouterView />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useSettingsStore } from './stores/settings'

const settings = useSettingsStore()
onMounted(() => settings.init())
</script>
