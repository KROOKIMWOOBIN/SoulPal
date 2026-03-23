import { defineStore } from 'pinia'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('soulpal_token') || null,
    user: JSON.parse(localStorage.getItem('soulpal_user') || 'null')
  }),

  getters: {
    isLoggedIn: (state) => !!state.token
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

    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('soulpal_token')
      localStorage.removeItem('soulpal_user')
    },

    _setSession(data) {
      this.token = data.token
      this.user = { userId: data.userId, username: data.username, email: data.email }
      localStorage.setItem('soulpal_token', data.token)
      localStorage.setItem('soulpal_user', JSON.stringify(this.user))
    }
  }
})
