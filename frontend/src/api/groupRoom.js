import { api, createSseStream } from './http'

export const groupRoomApi = {
  getAll:      (projectId)           => api.get('/group-rooms', { params: { projectId } }),
  getById:     (id)                  => api.get(`/group-rooms/${id}`),
  create:      (data)                => api.post('/group-rooms', data),
  delete:      (id)                  => api.delete(`/group-rooms/${id}`),
  getMessages: (roomId, page = 0, size = 30) =>
    api.get(`/group-chat/messages/${roomId}`, { params: { page, size } })
}

/**
 * 그룹 채팅 SSE 스트리밍
 * @returns {AbortController}
 */
export function streamGroupChat(payload, onCharStart, onToken, onCharDone, onDone, onError) {
  return createSseStream('/api/group-chat/stream', payload, {
    'char-start': onCharStart,
    token:        onToken,
    'char-done':  onCharDone,
    done:         onDone,
    error:        onError
  })
}
