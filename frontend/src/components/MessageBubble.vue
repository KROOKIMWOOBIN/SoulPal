<template>
  <div :class="['bubble-wrap', message.user ? 'user' : 'ai']">
    <div v-if="!message.user" class="ai-avatar">🤖</div>
    <div class="bubble-col">
      <div :class="['bubble', message.user ? 'bubble-user' : 'bubble-ai', { streaming: message.streaming }]">
        <span class="bubble-text">{{ message.content }}</span>
        <span v-if="message.streaming" class="cursor">▌</span>
      </div>
      <div v-if="!message.streaming" class="bubble-time">
        {{ formatTime(message.createdAt) }}
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ message: Object })

function formatTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.bubble-wrap {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 85%;
  animation: msgIn 0.2s ease;
}
@keyframes msgIn { from { opacity: 0; transform: translateY(8px) } to { opacity: 1; transform: none } }

.bubble-wrap.user { align-self: flex-end; flex-direction: row-reverse; }
.bubble-wrap.ai  { align-self: flex-start; }

.ai-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--primary-bg);
  display: flex; align-items: center; justify-content: center;
  font-size: 0.9rem; flex-shrink: 0; margin-bottom: 18px;
}

.bubble-col {
  display: flex; flex-direction: column;
}
.bubble-wrap.user .bubble-col { align-items: flex-end; }
.bubble-wrap.ai  .bubble-col { align-items: flex-start; }

.bubble {
  padding: 11px 15px;
  border-radius: 18px;
  font-size: 0.92rem;
  line-height: 1.55;
  word-break: break-word;
  white-space: pre-wrap;
}

.bubble-user {
  background: var(--grad-main);
  color: #fff;
  border-bottom-right-radius: 5px;
  box-shadow: 0 4px 12px var(--primary-glow);
}

.bubble-ai {
  background: var(--surface);
  color: var(--text);
  border: 1px solid var(--border);
  border-bottom-left-radius: 5px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.bubble.streaming { opacity: 0.9; }

.cursor {
  animation: blink 0.8s infinite;
  margin-left: 2px;
  color: var(--primary);
}
@keyframes blink { 0%, 100% { opacity: 1 } 50% { opacity: 0 } }

.bubble-time {
  font-size: 0.7rem;
  color: var(--text-secondary);
  margin-top: 3px;
  padding: 0 4px;
}
</style>
