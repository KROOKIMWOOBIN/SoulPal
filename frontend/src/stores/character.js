import { defineStore } from 'pinia'
import { characterApi } from '../api'

export const useCharacterStore = defineStore('character', {
  state: () => ({
    characters: [],
    categories: null,
    sort: 'recent',
    loading: false,
    error: null,
    totalPages: 0,
    totalElements: 0,
    currentPage: 0,
    pageSize: 20
  }),

  actions: {
    async fetchAll(projectId, page = 0) {
      this.loading = true
      try {
        const { data } = await characterApi.getAll(projectId, this.sort, page, this.pageSize)
        this.characters = data.content
        this.totalPages = data.totalPages
        this.totalElements = data.totalElements
        this.currentPage = data.number
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
