import { api, createSseStream } from './http'

export const chatApi = {
  getMessages:       (characterId, page = 0, size = 30) =>
    api.get(`/chat/messages/${characterId}`, { params: { page, size } }),
  searchMessages:    (characterId, q) =>
    api.get(`/chat/messages/${characterId}/search`, { params: { q } }),
  send:              (data)          => api.post('/chat/send', data),
  saveAiMessage:     (characterId, content) =>
    api.post('/chat/messages/save', { characterId, content }),
  deleteLastAiMessage: (characterId) => api.delete(`/chat/messages/${characterId}/last-ai`),
  clearMessages:     (characterId)   => api.delete(`/chat/messages/${characterId}`)
}

/**
 * 1:1 채팅 SSE 스트리밍
 * @returns {AbortController}
 */
export function streamChat(payload, onToken, onDone, onError) {
  return createSseStream('/api/chat/stream', payload, {
    token: onToken,
    done:  onDone,
    error: onError
  })
}
