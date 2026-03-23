<template>
  <div class="settings">
    <header class="page-header">
      <button class="back-btn" @click="$router.push('/')">←</button>
      <h1>설정</h1>
    </header>

    <main class="settings-content">
      <section class="settings-section">
        <h3>화면</h3>
        <div class="setting-item">
          <span>테마</span>
          <div class="toggle-group">
            <button
              v-for="opt in themeOptions"
              :key="opt.value"
              :class="['toggle-btn', { active: settings.theme === opt.value }]"
              @click="settings.setTheme(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
      </section>

      <section class="settings-section">
        <h3>AI 설정</h3>
        <div class="setting-item">
          <div>
            <span>대화 기록 수</span>
            <p class="setting-desc">AI에게 전달할 이전 대화 수 ({{ settings.historyCount }}개)</p>
          </div>
          <input
            type="range" min="1" max="30"
            :value="settings.historyCount"
            @input="settings.setHistoryCount(Number($event.target.value))"
            class="range-input"
          />
        </div>
      </section>

      <section class="settings-section">
        <h3>정보</h3>
        <div class="setting-item">
          <span>버전</span>
          <span class="setting-value">1.0.0 (Web)</span>
        </div>
        <div class="setting-item">
          <span>AI 엔진</span>
          <span class="setting-value">Ollama / llama3</span>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { useSettingsStore } from '../stores/settings'

const settings = useSettingsStore()

const themeOptions = [
  { value: 'light', label: '☀️ 라이트' },
  { value: 'dark', label: '🌙 다크' }
]
</script>

<style scoped>
.settings-content { padding: 16px; }

.settings-section {
  background: var(--surface);
  border-radius: var(--radius);
  margin-bottom: 16px;
  overflow: hidden;
  box-shadow: var(--shadow);
}

.settings-section h3 {
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-secondary);
  padding: 12px 16px 8px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-top: 1px solid var(--border);
  gap: 12px;
}

.setting-item span { font-size: 0.92rem; }
.setting-desc { font-size: 0.78rem; color: var(--text-secondary); margin-top: 2px; }
.setting-value { color: var(--text-secondary); font-size: 0.85rem; }

.toggle-group { display: flex; gap: 4px; }
.toggle-btn {
  padding: 6px 12px;
  border: 1.5px solid var(--border);
  border-radius: 20px;
  background: none;
  cursor: pointer;
  font-family: inherit;
  font-size: 0.82rem;
  color: var(--text-secondary);
  transition: all 0.2s;
}
.toggle-btn.active { border-color: var(--primary); color: var(--primary); background: var(--primary-bg); font-weight: 600; }

.range-input {
  width: 120px;
  accent-color: var(--primary);
}
</style>
