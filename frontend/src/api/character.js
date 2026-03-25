import { api } from './http'

export const characterApi = {
  getAll:         (projectId, sort = 'recent', page = 0, size = 20) =>
    api.get('/characters', { params: { projectId, sort, page, size } }),
  getById:        (id)        => api.get(`/characters/${id}`),
  create:         (data)      => api.post('/characters', data),
  update:         (id, data)  => api.put(`/characters/${id}`, data),
  delete:         (id)        => api.delete(`/characters/${id}`),
  toggleFavorite: (id)        => api.post(`/characters/${id}/favorite`),
  getCategories:  ()          => api.get('/categories')
}
