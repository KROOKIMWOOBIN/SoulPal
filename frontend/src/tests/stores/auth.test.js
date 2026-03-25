import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../../stores/auth'

// authApi 모킹
vi.mock('../../api', () => ({
  authApi: {
    register: vi.fn(),
    login:    vi.fn(),
    logout:   vi.fn(),
    refresh:  vi.fn(),
    me:       vi.fn()
  }
}))

import { authApi } from '../../api'

const mockAuthData = {
  accessToken:  'access-token',
  refreshToken: 'refresh-token',
  userId:       'user-1',
  username:     'testuser',
  email:        'test@example.com'
}

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('초기 상태 — localStorage 없으면 null', () => {
    const store = useAuthStore()
    expect(store.accessToken).toBeNull()
    expect(store.isLoggedIn).toBe(false)
  })

  it('login — 세션 저장 및 isLoggedIn true', async () => {
    authApi.login.mockResolvedValue({ data: mockAuthData })
    const store = useAuthStore()

    await store.login({ email: 'test@example.com', password: 'password1' })

    expect(store.accessToken).toBe('access-token')
    expect(store.isLoggedIn).toBe(true)
    expect(localStorage.getItem('soulpal_token')).toBe('access-token')
  })

  it('register — 세션 저장', async () => {
    authApi.register.mockResolvedValue({ data: mockAuthData })
    const store = useAuthStore()

    await store.register({ username: 'testuser', email: 'test@example.com', password: 'password1' })

    expect(store.user.username).toBe('testuser')
  })

  it('logout — 세션 초기화', async () => {
    authApi.logout.mockResolvedValue({})
    const store = useAuthStore()
    store.accessToken = 'access-token'
    store.user = { username: 'testuser' }

    await store.logout()

    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
    expect(localStorage.getItem('soulpal_token')).toBeNull()
  })

  it('refreshAccessToken — 새 액세스 토큰 저장', async () => {
    authApi.refresh.mockResolvedValue({ data: { accessToken: 'new-access-token' } })
    const store = useAuthStore()
    store.refreshToken = 'refresh-token'

    const newToken = await store.refreshAccessToken()

    expect(newToken).toBe('new-access-token')
    expect(store.accessToken).toBe('new-access-token')
  })

  it('refreshAccessToken — 리프레시 토큰 없으면 예외', async () => {
    const store = useAuthStore()
    store.refreshToken = null

    await expect(store.refreshAccessToken()).rejects.toThrow('no refresh token')
  })
})
