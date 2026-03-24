<template>
  <div class="chat">
    <!-- Header -->
    <header class="chat-header">
      <button class="back-btn" @click="goBack">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M19 12H5M12 5l-7 7 7 7"/>
        </svg>
      </button>
      <div class="chat-title">
        <div class="avatar-sm" :style="{ background: avatarGrad }">{{ emoji }}</div>
        <div>
          <div class="char-name">{{ store.character?.name }}</div>
          <div class="char-relation">{{ relationLabel }}</div>
        </div>
      </div>
      <div class="header-menu" @click.stop>
        <button class="icon-btn2" @click="showMenu = !showMenu">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/>
          </svg>
        </button>
        <Transition name="dropdown">
          <div v-if="showMenu" class="dropdown">
            <button @click="toggleSearch">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
              검색
            </button>
            <button @click="handleRegenerate">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 4v6h-6"/><path d="M1 20v-6h6"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
              재생성
            </button>
            <div class="dropdown-divider"></div>
            <button class="danger-item" @click="handleClear">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6M14 11v6"/></svg>
              대화 초기화
            </button>
          </div>
        </Transition>
      </div>
    </header>

    <!-- Search bar -->
    <Transition name="search">
      <div v-if="searching" class="search-bar">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        <input
          v-model="searchQuery"
          placeholder="메시지 검색..."
          @input="doSearch"
          ref="searchInput"
        />
        <button @click="closeSearch">닫기</button>
      </div>
    </Transition>

    <!-- Messages -->
    <div class="messages-container" ref="msgContainer" @scroll="handleScroll" @click="showMenu = false">
      <div v-if="store.loading" class="load-more-spinner">
        <div class="spinner"></div>
      </div>

      <div v-if="store.displayMessages.length === 0 && !store.streaming" class="empty-chat">
        <div class="empty-avatar" :style="{ background: avatarGrad }">{{ emoji }}</div>
        <div class="empty-name">{{ store.character?.name }}</div>
        <div class="empty-desc">대화를 시작해보세요 👋</div>
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
    <Transition name="fade">
      <div v-if="store.error" class="error-bar">
        ⚠️ {{ store.error }}
      </div>
    </Transition>

    <!-- Input -->
    <ChatInput :disabled="store.streaming" @send="handleSend" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '../stores/chat'
import { useSettingsStore } from '../stores/settings'
import MessageBubble from '../components/MessageBubble.vue'
import ChatInput from '../components/ChatInput.vue'

const props = defineProps({ id: String })
const store = useChatStore()
const settings = useSettingsStore()
const router = useRouter()

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
const gradMap = {
  bestfriend: 'linear-gradient(135deg,#FF9A9E,#FECFEF)',
  mentor: 'linear-gradient(135deg,#a18cd1,#fbc2eb)',
  lover: 'linear-gradient(135deg,#FF758C,#FF7EB3)',
  sibling_older: 'linear-gradient(135deg,#FFC3A0,#FFAFBD)',
  sibling_younger: 'linear-gradient(135deg,#84FAB0,#8FD3F4)',
  school_friend: 'linear-gradient(135deg,#89F7FE,#66A6FF)'
}

const emoji = computed(() => emojiMap[store.character?.relationshipId] || '👤')
const relationLabel = computed(() => labelMap[store.character?.relationshipId] || '')
const avatarGrad = computed(() => gradMap[store.character?.relationshipId] || 'var(--grad-soft)')

function goBack() {
  const projectId = store.character?.projectId
  if (projectId) router.push(`/project/${projectId}`)
  else router.push('/')
}

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
  if (msgContainer.value.scrollTop === 0) await store.loadMore()
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
  if (confirm('대화 내역을 모두 삭제할까요?')) await store.clearMessages()
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
.chat { display: flex; flex-direction: column; height: 100vh; background: var(--bg); }

.chat-header {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  position: sticky; top: 0; z-index: 10;
  backdrop-filter: blur(12px);
}

.back-btn {
  width: 36px; height: 36px;
  background: var(--surface2); border: none; border-radius: 50%;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: var(--text); flex-shrink: 0; transition: background 0.2s;
}
.back-btn:hover { background: var(--border); }

.chat-title { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.avatar-sm {
  width: 40px; height: 40px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.15rem; flex-shrink: 0;
}
.char-name { font-weight: 700; font-size: 0.95rem; }
.char-relation { font-size: 0.72rem; color: var(--text-secondary); margin-top: 1px; }

.header-menu { position: relative; }
.icon-btn2 {
  width: 36px; height: 36px; border: none; background: var(--surface2);
  cursor: pointer; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary); transition: background 0.2s;
}
.icon-btn2:hover { background: var(--border); }

.dropdown {
  position: absolute; right: 0; top: 42px;
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius-sm); box-shadow: var(--shadow-lg);
  min-width: 160px; overflow: hidden; z-index: 20;
}
.dropdown button {
  display: flex; align-items: center; gap: 8px;
  width: 100%; padding: 11px 16px;
  border: none; background: none; cursor: pointer;
  font-family: inherit; font-size: 0.88rem; color: var(--text);
  transition: background 0.15s;
}
.dropdown button:hover { background: var(--surface2); }
.dropdown .danger-item { color: #FF4D6D; }
.dropdown-divider { height: 1px; background: var(--border); margin: 4px 0; }

/* dropdown transition */
.dropdown-enter-active, .dropdown-leave-active { transition: opacity 0.15s, transform 0.15s; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-6px); }

.search-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}
.search-bar input {
  flex: 1; border: none; background: transparent;
  font-family: inherit; font-size: 0.9rem; color: var(--text); outline: none;
}
.search-bar button {
  background: none; border: none; color: var(--primary); font-size: 0.85rem;
  cursor: pointer; font-family: inherit; font-weight: 600;
}

/* search transition */
.search-enter-active, .search-leave-active { transition: all 0.2s; }
.search-enter-from, .search-leave-to { opacity: 0; transform: translateY(-10px); }

.messages-container {
  flex: 1; overflow-y: auto;
  padding: 16px 14px;
  display: flex; flex-direction: column; gap: 6px;
}

.empty-chat {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 40px 20px; gap: 10px; text-align: center;
}
.empty-avatar {
  width: 72px; height: 72px; border-radius: 24px;
  display: flex; align-items: center; justify-content: center;
  font-size: 2rem;
}
.empty-name { font-weight: 700; font-size: 1.1rem; }
.empty-desc { font-size: 0.88rem; color: var(--text-secondary); }

.load-more-spinner { display: flex; justify-content: center; padding: 10px; }

.error-bar {
  background: #FFF0F3; color: #C0000A;
  padding: 10px 16px; font-size: 0.85rem; text-align: center;
  border-top: 1px solid #FFCDD6;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
