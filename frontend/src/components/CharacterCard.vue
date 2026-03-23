<template>
  <div class="char-card card" @click="$emit('click')">
    <div class="char-avatar">
      <span>{{ emoji }}</span>
    </div>
    <div class="char-info">
      <div class="char-name-row">
        <span class="char-name">{{ character.name }}</span>
        <span v-if="character.favorite" class="fav-star">⭐</span>
      </div>
      <p class="char-last-msg">{{ character.lastMessage || '대화를 시작해보세요!' }}</p>
    </div>
    <div class="char-actions" @click.stop>
      <button class="action-btn" @click="$emit('favorite')" :title="character.favorite ? '즐겨찾기 해제' : '즐겨찾기'">
        {{ character.favorite ? '⭐' : '☆' }}
      </button>
      <button class="action-btn" @click="$emit('edit')" title="편집">✏️</button>
      <button class="action-btn danger" @click="$emit('delete')" title="삭제">🗑️</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ character: Object })
defineEmits(['click', 'favorite', 'edit', 'delete'])

const emojiMap = {
  bestfriend: '💕', mentor: '🌟', lover: '💝',
  sibling_older: '🤗', sibling_younger: '😊', school_friend: '📚'
}
const emoji = computed(() => emojiMap[props.character.relationshipId] || '👤')
</script>

<style scoped>
.char-card {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.char-card:hover { transform: translateY(-1px); box-shadow: 0 4px 20px rgba(124,92,191,0.18); }

.char-avatar {
  width: 52px; height: 52px;
  border-radius: 50%;
  background: var(--primary-bg);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.5rem;
  flex-shrink: 0;
}

.char-info { flex: 1; min-width: 0; }
.char-name-row { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.char-name { font-weight: 600; font-size: 0.95rem; }
.fav-star { font-size: 0.85rem; }
.char-last-msg {
  font-size: 0.82rem;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.char-actions { display: flex; gap: 2px; }
.action-btn {
  width: 32px; height: 32px;
  border: none; background: none;
  cursor: pointer; border-radius: 6px;
  font-size: 0.95rem;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
}
.action-btn:hover { background: var(--surface2); }
.action-btn.danger:hover { background: #FFE0E6; }
</style>
