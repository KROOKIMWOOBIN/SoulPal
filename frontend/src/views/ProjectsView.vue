<template>
  <div class="projects-page">
    <header class="projects-header">
      <div class="header-brand">
        <div class="brand-icon">💜</div>
        <div>
          <div class="brand-name">SoulPal</div>
          <div class="brand-sub">안녕하세요, {{ authStore.user?.username }}님 👋</div>
        </div>
      </div>
      <div class="header-actions">
        <button class="icon-btn" @click="$router.push('/settings')" title="설정">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
        </button>
        <button class="icon-btn logout-btn" @click="handleLogout" title="로그아웃">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        </button>
      </div>
    </header>

    <main class="projects-content">
      <div v-if="store.loading" class="empty-state">
        <div class="spinner"></div>
      </div>

      <template v-else>
        <!-- 새 프로젝트 버튼 -->
        <button class="new-project-btn" @click="showCreate = true">
          <div class="new-project-icon">＋</div>
          <div>
            <div class="new-project-label">새 프로젝트 만들기</div>
            <div class="new-project-sub">세계관, 관계, 이야기를 담아보세요</div>
          </div>
        </button>

        <!-- 빈 상태 -->
        <div v-if="store.projects.length === 0" class="empty-state" style="padding-top:32px">
          <div class="emoji">🌱</div>
          <p>첫 번째 프로젝트를 만들어<br>AI 친구와 이야기를 시작해보세요!</p>
        </div>

        <!-- 프로젝트 목록 -->
        <div v-else class="project-list">
          <div
            v-for="(project, i) in store.projects"
            :key="project.id"
            class="project-card card"
            :style="{ '--card-color': cardColors[i % cardColors.length] }"
            @click="$router.push(`/project/${project.id}`)"
          >
            <div class="project-color-bar"></div>
            <div class="project-body">
              <div class="project-icon-wrap">
                <div class="project-icon">{{ cardIcons[i % cardIcons.length] }}</div>
              </div>
              <div class="project-info">
                <div class="project-name">{{ project.name }}</div>
                <div v-if="project.description" class="project-desc">{{ project.description }}</div>
                <div class="project-date">{{ formatDate(project.createdAt) }}</div>
              </div>
              <div class="project-arrow">›</div>
            </div>
            <div class="project-actions" @click.stop>
              <button class="action-sm" @click="startEdit(project)">✏️</button>
              <button class="action-sm danger" @click="deleteTarget = project">🗑️</button>
            </div>
          </div>
        </div>
      </template>
    </main>

    <!-- 생성/수정 모달 -->
    <div v-if="showCreate || editTarget" class="modal-overlay" @click.self="closeModal">
      <div class="modal card">
        <div class="modal-title">{{ editTarget ? '프로젝트 수정' : '새 프로젝트' }}</div>
        <div class="field">
          <label>프로젝트 이름 *</label>
          <input v-model="modalForm.name" type="text" placeholder="예: 판타지 세계관" maxlength="50" ref="nameInput" />
        </div>
        <div class="field">
          <label>설명 (선택)</label>
          <input v-model="modalForm.description" type="text" placeholder="간단한 메모를 남겨보세요" />
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="closeModal">취소</button>
          <button class="btn btn-primary" @click="handleSubmit" :disabled="!modalForm.name.trim()">
            {{ editTarget ? '저장하기' : '만들기' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 삭제 확인 모달 -->
    <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
      <div class="modal card">
        <div class="delete-icon">🗑️</div>
        <div class="modal-title">프로젝트 삭제</div>
        <p class="modal-desc">
          <strong>{{ deleteTarget.name }}</strong>을(를) 삭제할까요?<br>
          <small>포함된 캐릭터와 대화 내역은 남아있습니다.</small>
        </p>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="deleteTarget = null">취소</button>
          <button class="btn btn-danger" @click="confirmDelete">삭제</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useProjectStore } from '../stores/project'

const router = useRouter()
const authStore = useAuthStore()
const store = useProjectStore()

const showCreate = ref(false)
const editTarget = ref(null)
const deleteTarget = ref(null)
const modalForm = ref({ name: '', description: '' })
const nameInput = ref(null)

const cardColors = ['#7C5CBF', '#FF6B8A', '#4FACFE', '#43E97B', '#FA8231', '#A55EEA']
const cardIcons = ['🌟', '💫', '🌙', '🔮', '🌸', '🎭']

onMounted(() => store.fetchAll())

function startEdit(project) {
  editTarget.value = project
  modalForm.value = { name: project.name, description: project.description || '' }
  nextTick(() => nameInput.value?.focus())
}

function closeModal() {
  showCreate.value = false
  editTarget.value = null
  modalForm.value = { name: '', description: '' }
}

async function handleSubmit() {
  if (!modalForm.value.name.trim()) return
  if (editTarget.value) {
    await store.update(editTarget.value.id, modalForm.value)
  } else {
    const project = await store.create(modalForm.value)
    closeModal()
    router.push(`/project/${project.id}`)
    return
  }
  closeModal()
}

async function confirmDelete() {
  await store.delete(deleteTarget.value.id)
  deleteTarget.value = null
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.projects-page { min-height: 100vh; display: flex; flex-direction: column; }

.projects-header {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  padding: 16px;
  display: flex; align-items: center; justify-content: space-between;
  position: sticky; top: 0; z-index: 10;
  backdrop-filter: blur(12px);
}

.header-brand { display: flex; align-items: center; gap: 12px; }
.brand-icon { font-size: 1.8rem; line-height: 1; }
.brand-name { font-size: 1.2rem; font-weight: 700; background: var(--grad-main); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.brand-sub { font-size: 0.8rem; color: var(--text-secondary); margin-top: 1px; }

.header-actions { display: flex; gap: 6px; }
.icon-btn {
  width: 38px; height: 38px; border-radius: 50%; border: none;
  cursor: pointer; color: var(--text-secondary);
  display: flex; align-items: center; justify-content: center;
  background: var(--surface2); transition: all 0.2s;
}
.icon-btn:hover { background: var(--border); color: var(--text); }
.logout-btn:hover { background: #FFF0F3; color: #C0000A; }

.projects-content { flex: 1; padding: 16px; }

.new-project-btn {
  width: 100%; padding: 18px;
  border: 2px dashed var(--border); border-radius: var(--radius);
  background: none; cursor: pointer;
  display: flex; align-items: center; gap: 14px;
  text-align: left; margin-bottom: 16px;
  transition: all 0.2s;
}
.new-project-btn:hover {
  border-color: var(--primary);
  background: var(--primary-bg);
}
.new-project-icon {
  width: 44px; height: 44px; border-radius: 50%;
  background: var(--grad-main);
  color: #fff; font-size: 1.3rem; font-weight: 300;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.new-project-label { font-size: 0.95rem; font-weight: 600; color: var(--text); }
.new-project-sub { font-size: 0.78rem; color: var(--text-secondary); margin-top: 2px; }

.project-list { display: flex; flex-direction: column; gap: 10px; }

.project-card {
  cursor: pointer; overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  padding: 0;
}
.project-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }
.project-color-bar { height: 4px; background: var(--card-color, var(--primary)); }

.project-body {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
}
.project-icon-wrap {
  width: 44px; height: 44px; border-radius: 14px;
  background: var(--surface2);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.4rem; flex-shrink: 0;
}
.project-info { flex: 1; min-width: 0; }
.project-name { font-weight: 600; font-size: 0.98rem; margin-bottom: 2px; }
.project-desc { font-size: 0.8rem; color: var(--text-secondary); margin-bottom: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.project-date { font-size: 0.74rem; color: var(--text-secondary); }
.project-arrow { font-size: 1.4rem; color: var(--text-secondary); }

.project-actions {
  display: flex; gap: 4px; padding: 0 12px 12px;
  justify-content: flex-end;
}
.action-sm {
  width: 30px; height: 30px; border-radius: var(--radius-xs); border: none;
  cursor: pointer; font-size: 0.9rem;
  display: flex; align-items: center; justify-content: center;
  background: var(--surface2); transition: background 0.15s;
}
.action-sm:hover { background: var(--border); }
.action-sm.danger:hover { background: #FFE0E6; }

.modal { padding: 28px; }
.delete-icon { font-size: 2rem; text-align: center; margin-bottom: 8px; }
.modal-title { font-size: 1.1rem; font-weight: 700; margin-bottom: 18px; text-align: center; }
.modal-desc { text-align: center; margin-bottom: 20px; line-height: 1.6; font-size: 0.9rem; }
.modal-desc small { color: var(--text-secondary); font-size: 0.82rem; }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: 0.82rem; font-weight: 600; margin-bottom: 6px; color: var(--text-secondary); }
.field input {
  width: 100%; padding: 11px 14px;
  border: 1.5px solid var(--border); border-radius: var(--radius-sm);
  font-size: 0.93rem; background: var(--surface2); color: var(--text);
  box-sizing: border-box; outline: none; font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.field input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow); }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 4px; }
</style>
