<template>
  <div :class="['bubble-wrap', message.user ? 'user' : 'ai']">
    <div :class="['bubble', message.user ? 'bubble-user' : 'bubble-ai', { streaming: message.streaming }]">
      <span class="bubble-text">{{ message.content }}</span>
      <span v-if="message.streaming" class="cursor">▌</span>
    </div>
    <div v-if="!message.streaming" class="bubble-time">
      {{ formatTime(message.createdAt) }}
    </div>
  </div>
</template>

<script setup>
const props = defineProps({ message: Object })

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.bubble-wrap { display: flex; flex-direction: column; max-width: 78%; }
.bubble-wrap.user { align-self: flex-end; align-items: flex-end; }
.bubble-wrap.ai { align-self: flex-start; align-items: flex-start; }

.bubble {
  padding: 10px 14px;
  border-radius: 18px;
  font-size: 0.92rem;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.bubble-user {
  background: var(--primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble-ai {
  background: var(--surface);
  color: var(--text);
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.bubble.streaming { opacity: 0.85; }

.cursor {
  animation: blink 0.8s infinite;
  margin-left: 2px;
  color: var(--primary);
}
@keyframes blink { 0%, 100% { opacity: 1 } 50% { opacity: 0 } }

.bubble-time {
  font-size: 0.72rem;
  color: var(--text-secondary);
  margin-top: 2px;
  padding: 0 4px;
}
</style>
