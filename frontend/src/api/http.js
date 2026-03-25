import axios from 'axios'

export const api = axios.create({ baseURL: '/api' })

// JWT 자동 주입
api.interceptors.request.use(config => {
  const token = localStorage.getItem('soulpal_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 401 → 리프레시 토큰으로 재시도, 실패 시 로그인 페이지
let isRefreshing = false
let refreshQueue = []

api.interceptors.response.use(
  res => res,
  async err => {
    const original = err.config
    if (err.response?.status !== 401 || original._retry || original.url === '/auth/refresh') {
      return Promise.reject(err)
    }

    const refreshToken = localStorage.getItem('soulpal_refresh')
    if (!refreshToken) {
      redirectLogin()
      return Promise.reject(err)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        refreshQueue.push({ resolve, reject })
      }).then(token => {
        original.headers.Authorization = `Bearer ${token}`
        return api(original)
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      const { data } = await api.post('/auth/refresh', { refreshToken })
      const newToken = data.accessToken
      localStorage.setItem('soulpal_token', newToken)
      api.defaults.headers.common.Authorization = `Bearer ${newToken}`
      refreshQueue.forEach(q => q.resolve(newToken))
      refreshQueue = []
      original.headers.Authorization = `Bearer ${newToken}`
      return api(original)
    } catch (_) {
      refreshQueue.forEach(q => q.reject(_))
      refreshQueue = []
      redirectLogin()
      return Promise.reject(err)
    } finally {
      isRefreshing = false
    }
  }
)

export function redirectLogin() {
  localStorage.removeItem('soulpal_token')
  localStorage.removeItem('soulpal_refresh')
  localStorage.removeItem('soulpal_user')
  window.location.href = '/login'
}

/**
 * 공통 SSE 스트림 처리
 * @param {string} url
 * @param {object} payload
 * @param {Record<string, Function>} handlers - event name → callback
 * @returns {AbortController}
 */
export function createSseStream(url, payload, handlers) {
  const ctrl = new AbortController()

  const readStream = async (res) => {
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
            handlers[lastEvent]?.(parsed)
          } catch {}
          lastEvent = ''
        }
      }
    }
  }

  const doFetch = async (retried = false) => {
    const token = localStorage.getItem('soulpal_token')

    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify(payload),
      signal: ctrl.signal
    })

    // 401 → 리프레시 토큰으로 1회 재시도
    if (res.status === 401 && !retried) {
      const refreshToken = localStorage.getItem('soulpal_refresh')
      if (!refreshToken) { redirectLogin(); return }
      try {
        const r = await fetch('/api/auth/refresh', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        })
        if (!r.ok) { redirectLogin(); return }
        const { accessToken } = await r.json()
        localStorage.setItem('soulpal_token', accessToken)
        // axios 기본 헤더도 동기화
        api.defaults.headers.common.Authorization = `Bearer ${accessToken}`
        return doFetch(true)
      } catch {
        redirectLogin(); return
      }
    }

    if (!res.ok) {
      // 재시도 후에도 401이면 세션 만료 → 로그인으로 이동
      if (res.status === 401) {
        redirectLogin()
        return
      }
      handlers['error']?.({ status: res.status })
      return
    }

    await readStream(res)
  }

  doFetch().catch(err => {
    if (err.name !== 'AbortError') handlers['error']?.(err)
  })

  return ctrl
}
