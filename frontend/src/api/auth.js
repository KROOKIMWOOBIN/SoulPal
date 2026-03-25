import { api } from './http'

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login:    (data) => api.post('/auth/login', data),
  logout:   ()     => api.post('/auth/logout'),
  refresh:  (rt)   => api.post('/auth/refresh', { refreshToken: rt }),
  me:       ()     => api.get('/auth/me')
}
