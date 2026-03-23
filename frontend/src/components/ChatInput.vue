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
      :disabled="disabled || !text.trim()"
      @click="send"
    >
      <span v-if="disabled" class="spinner small"></span>
      <span v-else>↑</span>
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
    if (textarea.value) textarea.value.style.height = 'auto'
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
  padding: 10px 16px 14px;
  background: var(--surface);
  border-top: 1px solid var(--border);
}

.chat-textarea {
  flex: 1;
  padding: 10px 14px;
  border: 1.5px solid var(--border);
  border-radius: 22px;
  background: var(--surface2);
  color: var(--text);
  font-family: inherit;
  font-size: 0.92rem;
  resize: none;
  outline: none;
  transition: border-color 0.2s;
  line-height: 1.4;
  max-height: 120px;
  overflow-y: auto;
}
.chat-textarea:focus { border-color: var(--primary); }
.chat-textarea:disabled { opacity: 0.6; }

.send-btn {
  width: 40px; height: 40px;
  border-radius: 50%;
  border: none;
  background: var(--primary);
  color: #fff;
  font-size: 1.1rem;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}
.send-btn:disabled { background: var(--border); cursor: not-allowed; }
.send-btn:not(:disabled):hover { background: var(--primary-dark); }

.spinner.small {
  width: 16px; height: 16px;
  border-width: 2px;
  border-color: rgba(255,255,255,0.3);
  border-top-color: #fff;
}
</style>
