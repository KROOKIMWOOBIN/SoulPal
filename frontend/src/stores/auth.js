import { defineStore } from 'pinia'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken:  sessionStorage.getItem('soulpal_token') || null,
    refreshToken: sessionStorage.getItem('soulpal_refresh') || null,
    user: JSON.parse(sessionStorage.getItem('soulpal_user') || 'null')
  }),

  getters: {
    isLoggedIn: (state) => !!state.accessToken
  },

  actions: {
    async register(payload) {
      const { data } = await authApi.register(payload)
      this._setSession(data)
      return data
    },

    async login(payload) {
      const { data } = await authApi.login(payload)
      this._setSession(data)
      return data
    },

    async logout() {
      try { await authApi.logout() } catch (_) {}
      this._clearSession()
    },

    /** 액세스 토큰 만료 시 리프레시 (rotation: refreshToken도 교체) */
    async refreshAccessToken() {
      const rt = this.refreshToken
      if (!rt) throw new Error('no refresh token')
      const { data } = await authApi.refresh(rt)
      this.accessToken  = data.accessToken
      this.refreshToken = data.refreshToken
      sessionStorage.setItem('soulpal_token',   data.accessToken)
      sessionStorage.setItem('soulpal_refresh', data.refreshToken)
      return data.accessToken
    },

    _setSession(data) {
      this.accessToken  = data.accessToken
      this.refreshToken = data.refreshToken
      this.user = { userId: data.userId, username: data.username, email: data.email }
      sessionStorage.setItem('soulpal_token',   data.accessToken)
      sessionStorage.setItem('soulpal_refresh', data.refreshToken)
      sessionStorage.setItem('soulpal_user',    JSON.stringify(this.user))
    },

    _clearSession() {
      this.accessToken  = null
      this.refreshToken = null
      this.user = null
      sessionStorage.removeItem('soulpal_token')
      sessionStorage.removeItem('soulpal_refresh')
      sessionStorage.removeItem('soulpal_user')
    }
  }
})
