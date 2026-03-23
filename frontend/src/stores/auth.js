import { defineStore } from 'pinia'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken:  localStorage.getItem('soulpal_token') || null,
    refreshToken: localStorage.getItem('soulpal_refresh') || null,
    user: JSON.parse(localStorage.getItem('soulpal_user') || 'null')
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

    /** 액세스 토큰 만료 시 리프레시 */
    async refreshAccessToken() {
      const rt = this.refreshToken
      if (!rt) throw new Error('no refresh token')
      const { data } = await authApi.refresh(rt)
      this.accessToken = data.accessToken
      localStorage.setItem('soulpal_token', data.accessToken)
      return data.accessToken
    },

    _setSession(data) {
      this.accessToken  = data.accessToken
      this.refreshToken = data.refreshToken
      this.user = { userId: data.userId, username: data.username, email: data.email }
      localStorage.setItem('soulpal_token',   data.accessToken)
      localStorage.setItem('soulpal_refresh', data.refreshToken)
      localStorage.setItem('soulpal_user',    JSON.stringify(this.user))
    },

    _clearSession() {
      this.accessToken  = null
      this.refreshToken = null
      this.user = null
      localStorage.removeItem('soulpal_token')
      localStorage.removeItem('soulpal_refresh')
      localStorage.removeItem('soulpal_user')
    }
  }
})
