import { defineStore } from 'pinia'
import { characterApi } from '../api'

export const useCharacterStore = defineStore('character', {
  state: () => ({
    characters: [],
    categories: null,
    sort: 'recent',
    loading: false,
    error: null
  }),

  actions: {
    async fetchAll(projectId) {
      this.loading = true
      try {
        const { data } = await characterApi.getAll(projectId, this.sort)
        this.characters = data
      } catch (e) {
        this.error = e.message
      } finally {
        this.loading = false
      }
    },

    async fetchCategories() {
      if (this.categories) return
      const { data } = await characterApi.getCategories()
      this.categories = data
    },

    async create(payload) {
      const { data } = await characterApi.create(payload)
      this.characters.unshift(data)
      return data
    },

    async update(id, payload) {
      const { data } = await characterApi.update(id, payload)
      const idx = this.characters.findIndex(c => c.id === id)
      if (idx !== -1) this.characters[idx] = data
      return data
    },

    async delete(id) {
      await characterApi.delete(id)
      this.characters = this.characters.filter(c => c.id !== id)
    },

    async toggleFavorite(id) {
      const { data } = await characterApi.toggleFavorite(id)
      const idx = this.characters.findIndex(c => c.id === id)
      if (idx !== -1) this.characters[idx] = data
    },

    setSort(sort, projectId) {
      this.sort = sort
      if (projectId) this.fetchAll(projectId)
    }
  }
})
