import { defineStore } from 'pinia'

const STORAGE_KEY = 'soulpal_settings'

const defaults = {
  theme: 'light',
  historyCount: 10
}

export const useSettingsStore = defineStore('settings', {
  state: () => {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    return { ...defaults, ...saved }
  },

  actions: {
    setTheme(theme) {
      this.theme = theme
      this._save()
      document.documentElement.setAttribute('data-theme', theme)
    },
    setHistoryCount(count) {
      this.historyCount = count
      this._save()
    },
    _save() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ theme: this.theme, historyCount: this.historyCount }))
    },
    init() {
      document.documentElement.setAttribute('data-theme', this.theme)
    }
  }
})
