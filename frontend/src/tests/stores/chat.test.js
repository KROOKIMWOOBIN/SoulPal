import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useChatStore } from '../../stores/chat'

vi.mock('../../api', () => ({
  chatApi: {
    getMessages:        vi.fn(),
    saveAiMessage:      vi.fn(),
    deleteLastAiMessage: vi.fn(),
    clearMessages:      vi.fn(),
    searchMessages:     vi.fn()
  },
  characterApi: {
    getById: vi.fn()
  },
  streamChat: vi.fn()
}))

import { chatApi, characterApi, streamChat } from '../../api'

const mockCharacter = { id: 'char-1', name: '소울' }

const mockMessages = [
  { id: 'msg-1', content: '안녕!', user: true,  createdAt: '2024-01-01T00:00:00' },
  { id: 'msg-2', content: '반가워!', user: false, createdAt: '2024-01-01T00:00:01' }
]

describe('useChatStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ── 초기 상태 ────────────────────────────────────────────────────────────────

  it('초기 상태 — 기본값 확인', () => {
    const store = useChatStore()
    expect(store.character).toBeNull()
    expect(store.messages).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.streaming).toBe(false)
    expect(store.streamingText).toBe('')
    expect(store.error).toBeNull()
    expect(store.hasMore).toBe(true)
  })

  // ── loadChat ─────────────────────────────────────────────────────────────────

  it('loadChat — 캐릭터와 메시지 로드', async () => {
    characterApi.getById.mockResolvedValue({ data: mockCharacter })
    chatApi.getMessages.mockResolvedValue({ data: mockMessages })

    const store = useChatStore()
    await store.loadChat('char-1')

    expect(store.character).toEqual(mockCharacter)
    expect(store.messages).toEqual(mockMessages)
    expect(store.page).toBe(0)
  })

  it('loadChat — 메시지 30개 미만이면 hasMore=false', async () => {
    characterApi.getById.mockResolvedValue({ data: mockCharacter })
    chatApi.getMessages.mockResolvedValue({ data: mockMessages }) // 2개 < 30

    const store = useChatStore()
    await store.loadChat('char-1')

    expect(store.hasMore).toBe(false)
  })

  it('loadChat — 이전 상태 초기화', async () => {
    characterApi.getById.mockResolvedValue({ data: mockCharacter })
    chatApi.getMessages.mockResolvedValue({ data: mockMessages })

    const store = useChatStore()
    store.messages = [{ id: 'old' }]
    store.error = '이전 에러'

    await store.loadChat('char-1')

    expect(store.messages).toEqual(mockMessages)
    expect(store.error).toBeNull()
  })

  // ── loadMore ─────────────────────────────────────────────────────────────────

  it('loadMore — 추가 메시지를 앞에 prepend', async () => {
    characterApi.getById.mockResolvedValue({ data: mockCharacter })
    chatApi.getMessages.mockResolvedValueOnce({ data: Array(30).fill({ id: 'x', content: '', user: false, createdAt: '' }) })
    chatApi.getMessages.mockResolvedValueOnce({ data: mockMessages })

    const store = useChatStore()
    await store.loadChat('char-1')   // 30개 → hasMore=true
    await store.loadMore()

    expect(chatApi.getMessages).toHaveBeenCalledWith('char-1', 1, 30)
    expect(store.messages.length).toBe(32) // 2 prepend + 30 original
  })

  it('loadMore — hasMore=false면 호출 안 함', async () => {
    characterApi.getById.mockResolvedValue({ data: mockCharacter })
    chatApi.getMessages.mockResolvedValue({ data: mockMessages })

    const store = useChatStore()
    await store.loadChat('char-1')   // hasMore=false
    await store.loadMore()

    expect(chatApi.getMessages).toHaveBeenCalledTimes(1) // loadChat 1회만
  })

  // ── sendMessage ───────────────────────────────────────────────────────────────

  it('sendMessage — 낙관적 유저 메시지 추가 후 스트리밍', async () => {
    characterApi.getById.mockResolvedValue({ data: mockCharacter })
    chatApi.getMessages.mockResolvedValue({ data: [] })
    chatApi.saveAiMessage.mockResolvedValue({ data: { id: 'ai-1', content: 'AI 응답', user: false, createdAt: '' } })

    // streamChat을 동기적으로 onDone 즉시 호출하도록 mock
    streamChat.mockImplementation((payload, onToken, onDone) => {
      onToken('AI ')
      onToken('응답')
      onDone('AI 응답')
    })

    const store = useChatStore()
    await store.loadChat('char-1')
    await store.sendMessage('안녕!')

    // 유저 메시지 + AI 메시지
    expect(store.messages.some(m => m.content === '안녕!' && m.user)).toBe(true)
    expect(store.streaming).toBe(false)
    expect(store.streamingText).toBe('')
  })

  it('sendMessage — 빈 메시지는 무시', async () => {
    const store = useChatStore()
    store.character = mockCharacter

    await store.sendMessage('   ')

    expect(streamChat).not.toHaveBeenCalled()
  })

  it('sendMessage — 스트리밍 중이면 무시', async () => {
    const store = useChatStore()
    store.character = mockCharacter
    store.streaming = true

    await store.sendMessage('안녕!')

    expect(streamChat).not.toHaveBeenCalled()
  })

  it('sendMessage — 스트리밍 오류 시 error 상태 설정', async () => {
    characterApi.getById.mockResolvedValue({ data: mockCharacter })
    chatApi.getMessages.mockResolvedValue({ data: [] })

    streamChat.mockImplementation((payload, onToken, onDone, onError) => {
      onError(new Error('연결 실패'))
    })

    const store = useChatStore()
    await store.loadChat('char-1')
    await store.sendMessage('안녕!')

    expect(store.error).toBe('응답 생성 중 오류가 발생했습니다.')
    expect(store.streaming).toBe(false)
  })

  // ── clearMessages ─────────────────────────────────────────────────────────────

  it('clearMessages — 메시지 전체 삭제', async () => {
    chatApi.clearMessages = vi.fn().mockResolvedValue({})
    characterApi.getById.mockResolvedValue({ data: mockCharacter })
    chatApi.getMessages.mockResolvedValue({ data: mockMessages })

    const store = useChatStore()
    await store.loadChat('char-1')
    await store.clearMessages()

    expect(store.messages).toEqual([])
    expect(chatApi.clearMessages).toHaveBeenCalledWith('char-1')
  })

  // ── search / clearSearch ──────────────────────────────────────────────────────

  it('search — 키워드로 메시지 검색', async () => {
    chatApi.searchMessages.mockResolvedValue({ data: [mockMessages[0]] })
    characterApi.getById.mockResolvedValue({ data: mockCharacter })
    chatApi.getMessages.mockResolvedValue({ data: mockMessages })

    const store = useChatStore()
    await store.loadChat('char-1')
    await store.search('안녕')

    expect(store.searchResults).toEqual([mockMessages[0]])
    expect(store.searchQuery).toBe('안녕')
  })

  it('search — 빈 쿼리면 searchResults=null', async () => {
    const store = useChatStore()
    store.searchResults = [mockMessages[0]]

    await store.search('')

    expect(store.searchResults).toBeNull()
  })

  it('clearSearch — 검색 상태 초기화', () => {
    const store = useChatStore()
    store.searchQuery = '안녕'
    store.searchResults = [mockMessages[0]]

    store.clearSearch()

    expect(store.searchQuery).toBe('')
    expect(store.searchResults).toBeNull()
  })

  // ── displayMessages getter ────────────────────────────────────────────────────

  it('displayMessages — searchResults 없으면 messages 반환', () => {
    const store = useChatStore()
    store.messages = mockMessages
    store.searchResults = null

    expect(store.displayMessages).toEqual(mockMessages)
  })

  it('displayMessages — searchResults 있으면 searchResults 반환', () => {
    const store = useChatStore()
    store.messages = mockMessages
    store.searchResults = [mockMessages[0]]

    expect(store.displayMessages).toEqual([mockMessages[0]])
  })
})
