<template>
  <div class="chat">
    <!-- Header -->
    <header class="chat-header">
      <button class="back-btn" @click="$router.push('/')">←</button>
      <div class="chat-title">
        <div class="avatar-sm">{{ emoji }}</div>
        <div>
          <div class="char-name">{{ store.character?.name }}</div>
          <div class="char-relation">{{ relationLabel }}</div>
        </div>
      </div>
      <div class="header-menu" @click.stop>
        <button class="icon-btn2" @click="showMenu = !showMenu">⋮</button>
        <div v-if="showMenu" class="dropdown">
          <button @click="toggleSearch">🔍 검색</button>
          <button @click="handleRegenerate">🔄 재생성</button>
          <button @click="handleClear" class="danger-item">🗑️ 대화 초기화</button>
        </div>
      </div>
    </header>

    <!-- Search bar -->
    <div v-if="searching" class="search-bar">
      <input
        v-model="searchQuery"
        class="input"
        placeholder="메시지 검색..."
        @input="doSearch"
        ref="searchInput"
      />
      <button class="btn btn-ghost" @click="closeSearch">닫기</button>
    </div>

    <!-- Messages -->
    <div class="messages-container" ref="msgContainer" @scroll="handleScroll">
      <div v-if="store.loading" class="load-more-spinner">
        <div class="spinner"></div>
      </div>

      <div v-if="store.displayMessages.length === 0 && !store.streaming" class="empty-state">
        <div class="emoji">💬</div>
        <p>{{ store.character?.name }}에게 말을 걸어보세요!</p>
      </div>

      <MessageBubble
        v-for="msg in store.displayMessages"
        :key="msg.id"
        :message="msg"
      />

      <!-- Streaming bubble -->
      <MessageBubble
        v-if="store.streaming"
        :message="{ content: store.streamingText || '...', user: false, streaming: true }"
      />

      <div ref="bottomAnchor"></div>
    </div>

    <!-- Error -->
    <div v-if="store.error" class="error-bar">
      ⚠️ {{ store.error }}
    </div>

    <!-- Input -->
    <ChatInput
      :disabled="store.streaming"
      @send="handleSend"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useChatStore } from '../stores/chat'
import { useSettingsStore } from '../stores/settings'
import MessageBubble from '../components/MessageBubble.vue'
import ChatInput from '../components/ChatInput.vue'

const props = defineProps({ id: String })
const store = useChatStore()
const settings = useSettingsStore()

const msgContainer = ref(null)
const bottomAnchor = ref(null)
const showMenu = ref(false)
const searching = ref(false)
const searchQuery = ref('')
const searchInput = ref(null)

const emojiMap = {
  bestfriend: '💕', mentor: '🌟', lover: '💝',
  sibling_older: '🤗', sibling_younger: '😊', school_friend: '📚'
}
const labelMap = {
  bestfriend: '베프', mentor: '멘토', lover: '연인',
  sibling_older: '언니·오빠', sibling_younger: '동생', school_friend: '학교친구'
}

const emoji = computed(() => emojiMap[store.character?.relationshipId] || '👤')
const relationLabel = computed(() => labelMap[store.character?.relationshipId] || '')

onMounted(async () => {
  await store.loadChat(props.id)
  scrollToBottom()
})

watch(() => store.messages.length, () => nextTick(scrollToBottom))
watch(() => store.streamingText, () => nextTick(scrollToBottom))

function scrollToBottom() {
  bottomAnchor.value?.scrollIntoView({ behavior: 'smooth' })
}

async function handleScroll() {
  if (msgContainer.value.scrollTop === 0) {
    await store.loadMore()
  }
}

async function handleSend(text) {
  showMenu.value = false
  await store.sendMessage(text, settings.historyCount)
}

async function handleRegenerate() {
  showMenu.value = false
  await store.regenerate(settings.historyCount)
}

async function handleClear() {
  showMenu.value = false
  if (confirm('대화 내역을 모두 삭제할까요?')) {
    await store.clearMessages()
  }
}

function toggleSearch() {
  showMenu.value = false
  searching.value = !searching.value
  if (searching.value) nextTick(() => searchInput.value?.focus())
  else closeSearch()
}

function closeSearch() {
  searching.value = false
  searchQuery.value = ''
  store.clearSearch()
}

async function doSearch() {
  await store.search(searchQuery.value)
}
</script>

<style scoped>
.chat { display: flex; flex-direction: column; height: 100vh; }

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  position: sticky; top: 0; z-index: 10;
}

.chat-title { display: flex; align-items: center; gap: 10px; flex: 1; }
.avatar-sm {
  width: 38px; height: 38px;
  border-radius: 50%;
  background: var(--primary-bg);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.1rem;
}
.char-name { font-weight: 600; font-size: 0.95rem; }
.char-relation { font-size: 0.75rem; color: var(--text-secondary); }

.header-menu { position: relative; }
.icon-btn2 {
  width: 36px; height: 36px;
  border: none; background: none; cursor: pointer;
  font-size: 1.3rem; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.icon-btn2:hover { background: var(--surface2); }
.dropdown {
  position: absolute; right: 0; top: 40px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  min-width: 150px;
  overflow: hidden;
  z-index: 20;
}
.dropdown button {
  display: block; width: 100%;
  padding: 10px 16px; text-align: left;
  border: none; background: none;
  cursor: pointer; font-family: inherit;
  font-size: 0.9rem; color: var(--text);
  transition: background 0.15s;
}
.dropdown button:hover { background: var(--surface2); }
.dropdown .danger-item { color: #FF4D6D; }

.search-bar {
  display: flex; gap: 8px;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.load-more-spinner { display: flex; justify-content: center; padding: 8px; }

.error-bar {
  background: #FFE0E6;
  color: #C0000A;
  padding: 10px 16px;
  font-size: 0.85rem;
  text-align: center;
}
</style>
