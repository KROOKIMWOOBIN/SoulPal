<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-logo">💜 SoulPal</div>
      <h2>회원가입</h2>

      <form @submit.prevent="handleRegister">
        <div class="field">
          <label>사용자명</label>
          <input v-model="form.username" type="text" placeholder="닉네임" required minlength="2" maxlength="50" />
        </div>
        <div class="field">
          <label>이메일</label>
          <input v-model="form.email" type="email" placeholder="email@example.com" required />
        </div>
        <div class="field">
          <label>비밀번호</label>
          <input v-model="form.password" type="password" placeholder="6자 이상" required minlength="6" />
        </div>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <button type="submit" class="btn btn-primary w-full" :disabled="loading">
          {{ loading ? '가입 중...' : '가입하기' }}
        </button>
      </form>

      <p class="auth-link">
        이미 계정이 있으신가요?
        <router-link to="/login">로그인</router-link>
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

const form = ref({ username: '', email: '', password: '' })
const loading = ref(false)
const error = ref('')

async function handleRegister() {
  error.value = ''
  loading.value = true
  try {
    await authStore.register(form.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '회원가입에 실패했습니다.'
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
.auth-card { width: 100%; max-width: 400px; padding: 32px; }
.auth-logo { text-align: center; font-size: 1.8rem; font-weight: 700; margin-bottom: 8px; }
h2 { text-align: center; font-size: 1.1rem; color: var(--text-secondary); margin-bottom: 24px; }
.field { margin-bottom: 16px; }
.field label { display: block; font-size: 0.85rem; font-weight: 600; margin-bottom: 6px; color: var(--text-secondary); }
.field input {
  width: 100%; padding: 10px 12px;
  border: 1.5px solid var(--border); border-radius: 8px;
  font-size: 0.95rem; background: var(--surface2); color: var(--text);
  box-sizing: border-box; transition: border-color 0.2s;
}
.field input:focus { outline: none; border-color: var(--primary); }
.error-msg { color: #e53e3e; font-size: 0.85rem; margin-bottom: 12px; }
.w-full { width: 100%; margin-top: 8px; }
.auth-link { text-align: center; margin-top: 20px; font-size: 0.9rem; color: var(--text-secondary); }
.auth-link a { color: var(--primary); font-weight: 600; text-decoration: none; }
</style>
