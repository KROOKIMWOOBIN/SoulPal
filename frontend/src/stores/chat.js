import { defineStore } from 'pinia'
import { chatApi, streamChat as streamChatV2 } from '../api'
import { characterApi } from '../api'

export const useChatStore = defineStore('chat', {
  state: () => ({
    character: null,
    messages: [],
    loading: false,
    streaming: false,
    streamingText: '',
    error: null,
    page: 0,
    hasMore: true,
    searchQuery: '',
    searchResults: null
  }),

  actions: {
    async loadChat(characterId) {
      this.page = 0
      this.hasMore = true
      this.messages = []
      this.searchQuery = ''
      this.searchResults = null
      this.error = null

      const [charRes, msgRes] = await Promise.all([
        characterApi.getById(characterId),
        chatApi.getMessages(characterId, 0, 30)
      ])
      this.character = charRes.data
      this.messages = msgRes.data
      if (msgRes.data.length < 30) this.hasMore = false
    },

    async loadMore() {
      if (!this.hasMore || this.loading) return
      this.loading = true
      this.page++
      try {
        const { data } = await chatApi.getMessages(this.character.id, this.page, 30)
        if (data.length < 30) this.hasMore = false
        this.messages = [...data, ...this.messages]
      } finally {
        this.loading = false
      }
    },

    async sendMessage(text, historyCount = 10) {
      if (this.streaming || !text.trim()) return

      // Optimistically add user message
      const userMsg = { id: Date.now().toString(), content: text, user: true, createdAt: new Date().toISOString() }
      this.messages.push(userMsg)
      this.streaming = true
      this.streamingText = ''
      this.error = null

      const payload = {
        characterId: this.character.id,
        message: text,
        historyCount
      }

      return new Promise((resolve) => {
        const ctrl = streamChatV2(
          payload,
          (token) => { this.streamingText += token },
          async (fullText) => {
            // Save AI message to backend
            try {
              const { data } = await chatApi.saveAiMessage(this.character.id, fullText)
              this.messages.push(data)
            } catch {
              this.messages.push({
                id: Date.now().toString(),
                content: fullText,
                user: false,
                createdAt: new Date().toISOString()
              })
            }
            this.streaming = false
            this.streamingText = ''
            resolve()
          },
          (err) => {
            this.error = '응답 생성 중 오류가 발생했습니다.'
            this.streaming = false
            this.streamingText = ''
            resolve()
          }
        )
      })
    },

    async regenerate(historyCount = 10) {
      if (this.streaming) return
      // Remove last AI message
      const last = this.messages[this.messages.length - 1]
      if (last && !last.user) {
        await chatApi.deleteLastAiMessage(this.character.id)
        this.messages.pop()
      }
      // Re-send last user message
      const lastUser = [...this.messages].reverse().find(m => m.user)
      if (lastUser) {
        await chatApi.deleteLastAiMessage(this.character.id) // remove it from DB too
        this.messages = this.messages.filter(m => m.id !== lastUser.id)
        await this.sendMessage(lastUser.content, historyCount)
      }
    },

    async clearMessages() {
      await chatApi.clearMessages(this.character.id)
      this.messages = []
    },

    async search(query) {
      this.searchQuery = query
      if (!query.trim()) {
        this.searchResults = null
        return
      }
      const { data } = await chatApi.searchMessages(this.character.id, query)
      this.searchResults = data
    },

    clearSearch() {
      this.searchQuery = ''
      this.searchResults = null
    }
  },

  getters: {
    displayMessages: (state) => state.searchResults ?? state.messages
  }
})
