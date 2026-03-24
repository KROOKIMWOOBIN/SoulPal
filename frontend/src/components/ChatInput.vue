<template>
  <div class="chat-input-wrap">
    <textarea
      v-model="text"
      class="chat-textarea"
      placeholder="메시지를 입력하세요..."
      :disabled="disabled"
      rows="1"
      @keydown.enter.exact.prevent="send"
      @keydown.enter.shift.exact="() => {}"
      @input="autoResize"
      ref="textarea"
    ></textarea>
    <button
      class="send-btn"
      :class="{ active: text.trim() && !disabled }"
      :disabled="disabled || !text.trim()"
      @click="send"
    >
      <span v-if="disabled" class="spinner small-white"></span>
      <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <line x1="22" y1="2" x2="11" y2="13"/>
        <polygon points="22 2 15 22 11 13 2 9 22 2"/>
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({ disabled: Boolean })
const emit = defineEmits(['send'])

const text = ref('')
const textarea = ref(null)

function send() {
  if (!text.value.trim() || props.disabled) return
  emit('send', text.value.trim())
  text.value = ''
  nextTick(() => {
    if (textarea.value) {
      textarea.value.style.height = 'auto'
      textarea.value.focus()
    }
  })
}

function autoResize() {
  const el = textarea.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}
</script>

<style scoped>
.chat-input-wrap {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 14px 16px;
  background: var(--surface);
  border-top: 1px solid var(--border);
}

.chat-textarea {
  flex: 1;
  padding: 11px 16px;
  border: 1.5px solid var(--border);
  border-radius: 24px;
  background: var(--surface2);
  color: var(--text);
  font-family: inherit;
  font-size: 0.93rem;
  resize: none;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  line-height: 1.45;
  max-height: 120px;
  overflow-y: auto;
}
.chat-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-glow);
}
.chat-textarea:disabled { opacity: 0.5; }

.send-btn {
  width: 42px; height: 42px;
  border-radius: 50%; border: none;
  background: var(--surface2);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}
.send-btn.active {
  background: var(--grad-main);
  color: #fff;
  box-shadow: 0 4px 12px var(--primary-glow);
}
.send-btn:disabled:not(.active) { cursor: not-allowed; opacity: 0.4; }
.send-btn.active:hover { filter: brightness(1.08); }

.small-white {
  width: 16px; height: 16px;
  border-width: 2px;
  border-color: rgba(255,255,255,0.3);
  border-top-color: #fff;
}
</style>
