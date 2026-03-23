import { defineStore } from 'pinia'
import { projectApi } from '../api'

export const useProjectStore = defineStore('project', {
  state: () => ({
    projects: [],
    loading: false,
    error: null
  }),

  actions: {
    async fetchAll() {
      this.loading = true
      try {
        const { data } = await projectApi.getAll()
        this.projects = data
      } catch (e) {
        this.error = e.message
      } finally {
        this.loading = false
      }
    },

    async create(payload) {
      const { data } = await projectApi.create(payload)
      this.projects.unshift(data)
      return data
    },

    async update(id, payload) {
      const { data } = await projectApi.update(id, payload)
      const idx = this.projects.findIndex(p => p.id === id)
      if (idx !== -1) this.projects[idx] = data
      return data
    },

    async delete(id) {
      await projectApi.delete(id)
      this.projects = this.projects.filter(p => p.id !== id)
    }
  }
})
