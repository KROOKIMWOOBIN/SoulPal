import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import HomeView from '../views/HomeView.vue'
import CreationView from '../views/CreationView.vue'
import ChatView from '../views/ChatView.vue'
import SettingsView from '../views/SettingsView.vue'

const routes = [
  // 공개 라우트
  { path: '/login', component: LoginView, meta: { guest: true } },
  { path: '/register', component: RegisterView, meta: { guest: true } },

  // 인증 필요 라우트
  { path: '/', component: ProjectsView, meta: { auth: true } },
  { path: '/project/:projectId', component: HomeView, props: true, meta: { auth: true } },
  { path: '/project/:projectId/create', component: CreationView, props: true, meta: { auth: true } },
  { path: '/project/:projectId/edit/:id', component: CreationView, props: true, meta: { auth: true } },
  { path: '/chat/:id', component: ChatView, props: true, meta: { auth: true } },
  { path: '/settings', component: SettingsView, meta: { auth: true } }
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
