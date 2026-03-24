<template>
  <div class="chat group-chat">
    <!-- Header -->
    <header class="chat-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M19 12H5M12 5l-7 7 7 7"/>
        </svg>
      </button>
      <div class="chat-title">
        <div class="avatar-sm group-avatar">👥</div>
        <div>
          <div class="char-name">{{ room?.name }}</div>
          <div class="char-relation">{{ room ? room.characterIds.length + '명 참여 중' : '' }}</div>
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
            <button class="danger-item" @click="handleClear">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
              </svg>
              대화방 나가기
            </button>
          </div>
        </Transition>
      </div>
    </header>

    <!-- Messages -->
    <div class="messages-container" ref="msgContainer" @click="showMenu = false">
      <div v-if="loading" class="empty-state"><div class="spinner"></div></div>

      <div v-else-if="!messages.length" class="empty-chat">
        <div class="empty-emoji">👥</div>
        <div class="empty-title">그룹 대화를 시작해보세요!</div>
        <div class="empty-sub">
          {{ characters.map(c => c.name).join(', ') }}이(가) 기다리고 있어요
        </div>
      </div>

      <template v-else>
        <div
          v-for="msg in displayMessages"
          :key="msg.id || msg._tempId"
          :class="['message-row', msg.isUser ? 'user-row' : 'ai-row']"
        >
          <!-- AI 메시지 -->
          <template v-if="!msg.isUser">
            <div class="avatar-sm ai-avatar" :style="{ background: getCharGrad(msg.senderCharacterId) }">
              {{ getCharEmoji(msg.senderCharacterId) }}
            </div>
            <div class="bubble-wrap">
              <div class="sender-name">{{ msg.senderName }}</div>
              <div class="bubble ai-bubble">
                <span v-if="msg._streaming" class="streaming-text">
                  {{ msg.content }}<span class="cursor">▌</span>
                </span>
                <span v-else>{{ msg.content }}</span>
              </div>
            </div>
          </template>

          <!-- 유저 메시지 -->
          <template v-else>
            <div class="bubble user-bubble">{{ msg.content }}</div>
          </template>
        </div>

        <!-- 타이핑 인디케이터 -->
        <div v-if="typingChar" class="message-row ai-row">
          <div class="avatar-sm ai-avatar" :style="{ background: getCharGrad(typingChar.id) }">
            {{ getCharEmoji(typingChar.id) }}
          </div>
          <div class="bubble-wrap">
            <div class="sender-name">{{ typingChar.name }}</div>
            <div class="bubble ai-bubble typing">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Input -->
    <div class="chat-input-area">
      <textarea
        v-model="input"
        class="message-textarea"
        placeholder="메시지를 입력하세요..."
        rows="1"
        :disabled="sending"
        @keydown.enter.exact.prevent="send"
        @input="autoResize"
        ref="textarea"
      ></textarea>
      <button
        class="send-btn"
        :class="{ active: input.trim() && !sending }"
        :disabled="!input.trim() || sending"
        @click="send"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
          <path d="M22 2L11 13M22 2L15 22l-4-9-9-4 20-7z"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { groupRoomApi, streamGroupChat } from '../api'

const props = defineProps({ id: String }) // roomId
const router = useRouter()

const room = ref(null)
const messages = ref([])
const characters = ref([]) // 방에 참여한 캐릭터 정보 목록
const loading = ref(true)
const sending = ref(false)
const input = ref('')
const showMenu = ref(false)
const msgContainer = ref(null)
const textarea = ref(null)
const typingChar = ref(null) // 현재 타이핑 중인 캐릭터

// 스트리밍 중인 임시 메시지 관리
const streamingMessages = ref({}) // characterId → { _tempId, content }

const displayMessages = computed(() => {
  const result = [...messages.value]
  // 스트리밍 중인 메시지를 삽입
  Object.values(streamingMessages.value).forEach(sm => {
    if (sm.content) result.push(sm)
  })
  return result
})

// 캐릭터별 그라디언트 (인덱스 기반)
const CHAR_GRADS = [
  'linear-gradient(135deg, #a18cd1, #fbc2eb)',
  'linear-gradient(135deg, #fd7043, #ff8a65)',
  'linear-gradient(135deg, #26c6da, #00acc1)',
  'linear-gradient(135deg, #66bb6a, #43a047)',
  'linear-gradient(135deg, #ffa726, #fb8c00)',
  'linear-gradient(135deg, #ab47bc, #8e24aa)',
]
const CHAR_EMOJIS = ['✨', '🌟', '💫', '⚡', '🌈', '🎯']

function getCharIndex(characterId) {
  if (!room.value) return 0
  return room.value.characterIds.indexOf(characterId)
}
function getCharGrad(characterId) {
  const idx = getCharIndex(characterId)
  return CHAR_GRADS[idx % CHAR_GRADS.length]
}
function getCharEmoji(characterId) {
  const idx = getCharIndex(characterId)
  return CHAR_EMOJIS[idx % CHAR_EMOJIS.length]
}

async function loadMessages() {
  try {
    const { data } = await groupRoomApi.getMessages(props.id)
    messages.value = data.map(m => ({ ...m, isUser: !m.senderCharacterId }))
  } catch (e) {
    console.error(e)
  }
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return

  input.value = ''
  sending.value = true
  resetTextarea()

  // 유저 메시지 즉시 표시
  const userMsg = {
    _tempId: Date.now() + '-user',
    content: text,
    isUser: true,
    senderName: '나'
  }
  messages.value.push(userMsg)
  scrollBottom()

  let ctrl = null
  ctrl = streamGroupChat(
    { roomId: props.id, message: text },
    // onCharStart
    ({ characterId, characterName }) => {
      typingChar.value = { id: characterId, name: characterName }
      streamingMessages.value[characterId] = {
        _tempId: Date.now() + '-' + characterId,
        content: '',
        isUser: false,
        senderCharacterId: characterId,
        senderName: characterName,
        _streaming: true
      }
      scrollBottom()
    },
    // onToken
    ({ characterId, token }) => {
      if (streamingMessages.value[characterId]) {
        streamingMessages.value[characterId].content += token
        scrollBottom()
      }
    },
    // onCharDone
    ({ characterId, messageId, message }) => {
      // 스트리밍 임시 메시지 제거 후 실제 메시지로 교체
      delete streamingMessages.value[characterId]
      const charName = typingChar.value?.name || ''
      messages.value.push({
        id: messageId,
        content: message,
        isUser: false,
        senderCharacterId: characterId,
        senderName: charName
      })
      typingChar.value = null
      scrollBottom()
    },
    // onDone
    () => {
      sending.value = false
      // 유저 임시 메시지를 실제로 전환 (ID 없어도 무방)
      const idx = messages.value.findIndex(m => m._tempId === userMsg._tempId)
      if (idx !== -1) messages.value[idx] = { ...messages.value[idx], _tempId: undefined }
    },
    // onError
    (err) => {
      console.error('[GroupChat SSE]', err)
      sending.value = false
      typingChar.value = null
      streamingMessages.value = {}
    }
  )
}

async function handleClear() {
  if (!confirm('대화방을 삭제하시겠습니까?')) return
  showMenu.value = false
  await groupRoomApi.delete(props.id)
  router.back()
}

function scrollBottom() {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight
    }
  })
}

function autoResize() {
  if (textarea.value) {
    textarea.value.style.height = 'auto'
    textarea.value.style.height = Math.min(textarea.value.scrollHeight, 120) + 'px'
  }
}

function resetTextarea() {
  if (textarea.value) {
    textarea.value.style.height = 'auto'
  }
}

onMounted(async () => {
  try {
    const { data } = await groupRoomApi.getById(props.id)
    room.value = data
  } catch (e) {
    router.back()
    return
  }
  await loadMessages()
  loading.value = false
  scrollBottom()
})
</script>

<style scoped>
.group-chat { display: flex; flex-direction: column; height: 100vh; }

.group-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  font-size: 1.1rem;
}

.messages-container {
  flex: 1; overflow-y: auto; padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
}

.message-row { display: flex; align-items: flex-end; gap: 8px; }
.user-row { flex-direction: row-reverse; }
.ai-row { flex-direction: row; }

.bubble-wrap { display: flex; flex-direction: column; gap: 2px; max-width: 70%; }
.sender-name { font-size: 0.75rem; color: var(--text-secondary); font-weight: 500; padding-left: 2px; }

.bubble {
  padding: 10px 14px; border-radius: 18px; font-size: 0.92rem;
  line-height: 1.5; word-break: break-word; max-width: 100%;
}
.ai-bubble {
  background: var(--surface2); border-radius: 4px 18px 18px 18px;
  border: 1px solid var(--border);
}
.user-bubble {
  background: var(--grad-main); color: #fff;
  border-radius: 18px 4px 18px 18px;
  max-width: 70%; padding: 10px 14px;
  font-size: 0.92rem; line-height: 1.5; word-break: break-word;
  align-self: flex-end;
}

.avatar-sm {
  width: 34px; height: 34px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1rem; flex-shrink: 0;
}

/* 타이핑 인디케이터 */
.typing { display: flex; gap: 4px; align-items: center; padding: 12px 16px; }
.dot {
  width: 7px; height: 7px; border-radius: 50%; background: var(--text-secondary);
  animation: bounce 1.2s infinite;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

.streaming-text { white-space: pre-wrap; }
.cursor { animation: blink 1s step-end infinite; }
@keyframes blink { 50% { opacity: 0; } }

/* 빈 상태 */
.empty-chat { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; }
.empty-emoji { font-size: 3rem; }
.empty-title { font-size: 1rem; font-weight: 600; }
.empty-sub { font-size: 0.85rem; color: var(--text-secondary); text-align: center; }

/* 입력 영역 */
.chat-input-area {
  display: flex; align-items: flex-end; gap: 8px;
  padding: 10px 12px 16px;
  border-top: 1px solid var(--border);
  background: var(--surface);
}

.message-textarea {
  flex: 1; border: 1.5px solid var(--border); border-radius: 20px;
  padding: 10px 14px; font-size: 0.92rem; font-family: inherit;
  background: var(--surface2); color: var(--text);
  resize: none; overflow: hidden; min-height: 40px; max-height: 120px;
  transition: border-color 0.2s;
  outline: none; line-height: 1.4;
}
.message-textarea:focus { border-color: var(--primary-light); }

.send-btn {
  width: 40px; height: 40px; border-radius: 50%; border: none;
  background: var(--border); color: var(--text-secondary);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s; flex-shrink: 0;
}
.send-btn.active { background: var(--grad-main); color: #fff; }
.send-btn:disabled { cursor: not-allowed; }
</style>
