<template>
  <button :class="['option-card', { selected }]" @click="$emit('click')">
    <span class="option-emoji">{{ item.emoji }}</span>
    <span class="option-label">{{ item.labelKo }}</span>
  </button>
</template>

<script setup>
defineProps({ item: Object, selected: Boolean })
defineEmits(['click'])
</script>

<style scoped>
.option-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 12px;
  border: 2px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.option-card:hover { border-color: var(--primary-light); background: var(--primary-bg); }
.option-card.selected { border-color: var(--primary); background: var(--primary-bg); }

.option-emoji { font-size: 1.6rem; }
.option-label { font-size: 0.85rem; font-weight: 500; color: var(--text); }
.option-card.selected .option-label { color: var(--primary); }
</style>
