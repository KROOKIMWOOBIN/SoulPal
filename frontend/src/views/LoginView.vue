<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-logo">💜 SoulPal</div>
      <h2>로그인</h2>

      <form @submit.prevent="handleLogin">
        <div class="field">
          <label>이메일</label>
          <input v-model="form.email" type="email" placeholder="email@example.com" required autocomplete="email" />
        </div>
        <div class="field">
          <label>비밀번호</label>
          <input v-model="form.password" type="password" placeholder="비밀번호" required autocomplete="current-password" />
        </div>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <button type="submit" class="btn btn-primary w-full" :disabled="loading">
          {{ loading ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <p class="auth-link">
        계정이 없으신가요?
        <router-link to="/register">회원가입</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = ref({ email: '', password: '' })
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await authStore.login(form.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '로그인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: var(--bg);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 32px;
}

.auth-logo {
  text-align: center;
  font-size: 1.8rem;
  font-weight: 700;
  margin-bottom: 8px;
}

h2 {
  text-align: center;
  font-size: 1.1rem;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.field {
  margin-bottom: 16px;
}

.field label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--text-secondary);
}

.field input {
  width: 100%;
  padding: 10px 12px;
  border: 1.5px solid var(--border);
  border-radius: 8px;
  font-size: 0.95rem;
  background: var(--surface2);
  color: var(--text);
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.field input:focus {
  outline: none;
  border-color: var(--primary);
}

.error-msg {
  color: #e53e3e;
  font-size: 0.85rem;
  margin-bottom: 12px;
}

.w-full { width: 100%; margin-top: 8px; }

.auth-link {
  text-align: center;
  margin-top: 20px;
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.auth-link a {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}
</style>
