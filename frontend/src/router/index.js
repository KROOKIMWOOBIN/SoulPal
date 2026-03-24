import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // 공개 라우트
  { path: '/login',    component: () => import('../views/LoginView.vue'),    meta: { guest: true } },
  { path: '/register', component: () => import('../views/RegisterView.vue'), meta: { guest: true } },

  // 인증 필요 라우트
  { path: '/',                                    component: () => import('../views/ProjectsView.vue'), meta: { auth: true } },
  { path: '/project/:projectId',                  component: () => import('../views/HomeView.vue'),     props: true, meta: { auth: true } },
  { path: '/project/:projectId/create',           component: () => import('../views/CreationView.vue'), props: true, meta: { auth: true } },
  { path: '/project/:projectId/edit/:id',         component: () => import('../views/CreationView.vue'), props: true, meta: { auth: true } },
  { path: '/chat/:id',                            component: () => import('../views/ChatView.vue'),      props: true, meta: { auth: true } },
  { path: '/group/:id',                           component: () => import('../views/GroupChatView.vue'), props: true, meta: { auth: true } },
  { path: '/settings',                            component: () => import('../views/SettingsView.vue'), meta: { auth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const isLoggedIn = !!localStorage.getItem('soulpal_token')

  if (to.meta.auth && !isLoggedIn) return next('/login')
  if (to.meta.guest && isLoggedIn) return next('/')
  next()
})

export default router
