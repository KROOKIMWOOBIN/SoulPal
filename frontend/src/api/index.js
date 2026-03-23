import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

// JWT 자동 주입
api.interceptors.request.use(config => {
  const token = localStorage.getItem('soulpal_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 401이면 로그인 페이지로
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('soulpal_token')
      localStorage.removeItem('soulpal_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

// ── Auth ──────────────────────────────────────────────────────────────────────
export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  me: () => api.get('/auth/me')
}

// ── Projects ──────────────────────────────────────────────────────────────────
export const projectApi = {
  getAll: () => api.get('/projects'),
  getById: (id) => api.get(`/projects/${id}`),
  create: (data) => api.post('/projects', data),
  update: (id, data) => api.put(`/projects/${id}`, data),
  delete: (id) => api.delete(`/projects/${id}`)
}

// ── Characters ────────────────────────────────────────────────────────────────
export const characterApi = {
  getAll: (projectId, sort = 'recent') => api.get('/characters', { params: { projectId, sort } }),
  getById: (id) => api.get(`/characters/${id}`),
  create: (data) => api.post('/characters', data),
  update: (id, data) => api.put(`/characters/${id}`, data),
  delete: (id) => api.delete(`/characters/${id}`),
  toggleFavorite: (id) => api.post(`/characters/${id}/favorite`),
  getCategories: () => api.get('/categories')
}

// ── Chat ──────────────────────────────────────────────────────────────────────
export const chatApi = {
  getMessages: (characterId, page = 0, size = 30) =>
    api.get(`/chat/messages/${characterId}`, { params: { page, size } }),
  searchMessages: (characterId, q) =>
    api.get(`/chat/messages/${characterId}/search`, { params: { q } }),
  send: (data) => api.post('/chat/send', data),
  saveAiMessage: (characterId, content) =>
    api.post('/chat/messages/save', { characterId, content }),
  deleteLastAiMessage: (characterId) =>
    api.delete(`/chat/messages/${characterId}/last-ai`),
  clearMessages: (characterId) =>
    api.delete(`/chat/messages/${characterId}`)
}

// ── SSE Streaming ─────────────────────────────────────────────────────────────
export function streamChatV2(payload, onToken, onDone, onError) {
  const ctrl = new AbortController()
  const token = localStorage.getItem('soulpal_token')

  fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload),
    signal: ctrl.signal
  }).then(async res => {
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let lastEvent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n')
      buffer = parts.pop()

      for (const line of parts) {
        if (line.startsWith('event:')) {
          lastEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const raw = line.slice(5).trim()
          try {
            const parsed = JSON.parse(raw)
            if (lastEvent === 'token') onToken(parsed)
            else if (lastEvent === 'done') onDone(parsed)
            else if (lastEvent === 'error') onError(parsed)
          } catch {}
          lastEvent = ''
        }
      }
    }
  }).catch(err => {
    if (err.name !== 'AbortError') onError(err)
  })

  return ctrl
}

export const streamChat = streamChatV2
