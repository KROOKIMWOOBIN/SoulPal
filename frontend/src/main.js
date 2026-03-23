import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import { setupErrorReporter } from './errorReporter'

const app = createApp(App)
app.use(createPinia())
app.use(router)
setupErrorReporter(app)
app.mount('#app')
