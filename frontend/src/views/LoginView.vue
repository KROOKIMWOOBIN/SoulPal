<template>
  <div class="auth-page">
    <div class="auth-bg"></div>
    <div class="auth-card card">
      <div class="auth-logo">
        <div class="logo-icon">💜</div>
        <div class="logo-text">SoulPal</div>
        <div class="logo-sub">AI 친구와 대화하세요</div>
      </div>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="field">
          <label>이메일</label>
          <div class="input-wrap">
            <span class="input-icon">✉️</span>
            <input v-model="form.email" type="email" placeholder="email@example.com"
              required autocomplete="email" />
          </div>
        </div>
        <div class="field">
          <label>비밀번호</label>
          <div class="input-wrap">
            <span class="input-icon">🔒</span>
            <input v-model="form.password" :type="showPw ? 'text' : 'password'"
              placeholder="비밀번호 입력" required autocomplete="current-password" />
            <button type="button" class="pw-toggle" @click="showPw = !showPw">
              {{ showPw ? '🙈' : '👁️' }}
            </button>
          </div>
        </div>

        <div v-if="error" class="error-msg">
          <span>⚠️</span> {{ error }}
        </div>

        <button type="submit" class="btn btn-primary submit-btn" :disabled="loading">
          <span v-if="loading" class="spinner small-white"></span>
          <span v-else>로그인</span>
        </button>
      </form>

      <div class="auth-divider"><span>또는</span></div>

      <p class="auth-link">
        계정이 없으신가요?
        <router-link to="/register">회원가입 →</router-link>
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
const showPw = ref(false)

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
  padding: 24px 20px;
  position: relative;
  overflow: hidden;
}

.auth-bg {
  position: fixed;
  inset: 0;
  background: linear-gradient(160deg, #EDE6FF 0%, #F7F3FF 40%, #E8F4FF 100%);
  z-index: 0;
}
[data-theme="dark"] .auth-bg {
  background: linear-gradient(160deg, #0D0B14 0%, #130F22 60%, #0A1220 100%);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 36px 32px;
  position: relative;
  z-index: 1;
  border: 1px solid rgba(255,255,255,0.8);
  backdrop-filter: blur(20px);
}

.auth-logo {
  text-align: center;
  margin-bottom: 32px;
}
.logo-icon { font-size: 2.8rem; line-height: 1; margin-bottom: 6px; }
.logo-text { font-size: 1.8rem; font-weight: 700; background: var(--grad-main); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.logo-sub { font-size: 0.85rem; color: var(--text-secondary); margin-top: 4px; }

.auth-form { display: flex; flex-direction: column; gap: 16px; }

.field label {
  display: block;
  font-size: 0.82rem;
  font-weight: 600;
  margin-bottom: 7px;
  color: var(--text-secondary);
  letter-spacing: 0.02em;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.input-wrap input {
  width: 100%;
  padding: 12px 42px 12px 40px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface2);
  color: var(--text);
  font-family: inherit;
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.input-wrap input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-glow);
}
.input-icon {
  position: absolute;
  left: 13px;
  font-size: 0.95rem;
  pointer-events: none;
}
.pw-toggle {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  padding: 4px;
  border-radius: 6px;
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #FFF0F3;
  color: #C0000A;
  border-radius: var(--radius-xs);
  padding: 10px 14px;
  font-size: 0.85rem;
}
[data-theme="dark"] .error-msg { background: #2D0B10; color: #FF8096; }

.submit-btn { width: 100%; padding: 14px; font-size: 1rem; border-radius: var(--radius-sm); margin-top: 4px; }

.small-white {
  width: 18px; height: 18px;
  border-color: rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-width: 2px;
}

.auth-divider {
  text-align: center;
  margin: 20px 0 0;
  position: relative;
  color: var(--text-secondary);
  font-size: 0.82rem;
}
.auth-divider::before, .auth-divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 38%;
  height: 1px;
  background: var(--border);
}
.auth-divider::before { left: 0; }
.auth-divider::after { right: 0; }

.auth-link {
  text-align: center;
  margin-top: 14px;
  font-size: 0.88rem;
  color: var(--text-secondary);
}
.auth-link a {
  color: var(--primary);
  font-weight: 700;
  text-decoration: none;
  transition: opacity 0.2s;
}
.auth-link a:hover { opacity: 0.8; }
</style>
