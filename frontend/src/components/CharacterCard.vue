<template>
  <div class="char-card card" @click="$emit('click')">
    <div class="char-avatar" :style="{ background: avatarGrad }">
      <span>{{ emoji }}</span>
    </div>
    <div class="char-info">
      <div class="char-name-row">
        <span class="char-name">{{ character.name }}</span>
        <span v-if="character.favorite" class="fav-badge">⭐</span>
        <span class="relation-chip">{{ relationLabel }}</span>
      </div>
      <p class="char-last-msg">{{ character.lastMessage || '대화를 시작해보세요!' }}</p>
    </div>
    <div class="char-actions" @click.stop>
      <button class="action-btn" :class="{ starred: character.favorite }"
        @click="$emit('favorite')" :title="character.favorite ? '즐겨찾기 해제' : '즐겨찾기'">
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
const labelMap = {
  bestfriend: '베프', mentor: '멘토', lover: '연인',
  sibling_older: '언니·오빠', sibling_younger: '동생', school_friend: '학교친구'
}
const gradMap = {
  bestfriend: 'linear-gradient(135deg,#FF9A9E,#FECFEF)',
  mentor:     'linear-gradient(135deg,#a18cd1,#fbc2eb)',
  lover:      'linear-gradient(135deg,#FF758C,#FF7EB3)',
  sibling_older: 'linear-gradient(135deg,#FFC3A0,#FFAFBD)',
  sibling_younger: 'linear-gradient(135deg,#84FAB0,#8FD3F4)',
  school_friend: 'linear-gradient(135deg,#89F7FE,#66A6FF)'
}

const emoji = computed(() => emojiMap[props.character.relationshipId] || '👤')
const relationLabel = computed(() => labelMap[props.character.relationshipId] || '')
const avatarGrad = computed(() => gradMap[props.character.relationshipId] || 'var(--grad-soft)')
</script>

<style scoped>
.char-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: transform 0.18s cubic-bezier(0.34,1.56,0.64,1), box-shadow 0.18s;
}
.char-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }

.char-avatar {
  width: 52px; height: 52px; border-radius: 18px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.5rem; flex-shrink: 0;
}

.char-info { flex: 1; min-width: 0; }
.char-name-row { display: flex; align-items: center; gap: 6px; margin-bottom: 5px; flex-wrap: wrap; }
.char-name { font-weight: 700; font-size: 0.95rem; }
.fav-badge { font-size: 0.85rem; }
.relation-chip {
  font-size: 0.72rem; font-weight: 600; padding: 2px 8px;
  border-radius: 20px; background: var(--primary-bg); color: var(--primary);
}

.char-last-msg {
  font-size: 0.81rem; color: var(--text-secondary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  max-width: 180px;
}

.char-actions { display: flex; gap: 2px; flex-shrink: 0; }
.action-btn {
  width: 32px; height: 32px; border: none; background: none;
  cursor: pointer; border-radius: 8px; font-size: 1rem;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
}
.action-btn:hover { background: var(--surface2); }
.action-btn.starred { color: #F5A623; }
.action-btn.danger:hover { background: #FFE0E6; }
</style>
