<template>
  <div class="projects-page">
    <header class="projects-header">
      <div class="header-top">
        <h1>💜 SoulPal</h1>
        <div class="header-actions">
          <span class="username">{{ authStore.user?.username }}</span>
          <button class="icon-btn" @click="$router.push('/settings')" title="설정">⚙️</button>
          <button class="icon-btn" @click="handleLogout" title="로그아웃">🚪</button>
        </div>
      </div>
      <p class="subtitle">프로젝트를 선택하거나 새로 만들어보세요</p>
    </header>

    <main class="projects-content">
      <div v-if="store.loading" class="empty-state">
        <div class="spinner"></div>
      </div>

      <div v-else>
        <!-- 새 프로젝트 버튼 -->
        <button class="new-project-btn" @click="showCreate = true">
          <span class="plus">＋</span>
          <span>새 프로젝트</span>
        </button>

        <!-- 프로젝트 목록 -->
        <div v-if="store.projects.length === 0" class="empty-state" style="margin-top:24px">
          <div class="emoji">📁</div>
          <p>프로젝트가 없습니다.<br>첫 프로젝트를 만들어보세요!</p>
        </div>

        <div v-else class="project-list">
          <div
            v-for="project in store.projects"
            :key="project.id"
            class="project-card card"
            @click="$router.push(`/project/${project.id}`)"
          >
            <div class="project-info">
              <div class="project-icon">📂</div>
              <div>
                <div class="project-name">{{ project.name }}</div>
                <div v-if="project.description" class="project-desc">{{ project.description }}</div>
                <div class="project-date">{{ formatDate(project.createdAt) }}</div>
              </div>
            </div>
            <div class="project-actions" @click.stop>
              <button class="icon-btn-sm" @click="startEdit(project)" title="수정">✏️</button>
              <button class="icon-btn-sm danger" @click="deleteTarget = project" title="삭제">🗑️</button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 생성/수정 모달 -->
    <div v-if="showCreate || editTarget" class="modal-overlay" @click.self="closeModal">
      <div class="modal card">
        <h3>{{ editTarget ? '프로젝트 수정' : '새 프로젝트' }}</h3>
        <div class="field">
          <label>프로젝트 이름</label>
          <input v-model="modalForm.name" type="text" placeholder="예: 판타지 세계관" maxlength="50" ref="nameInput" />
        </div>
        <div class="field">
          <label>설명 (선택)</label>
          <input v-model="modalForm.description" type="text" placeholder="간단한 메모" />
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="closeModal">취소</button>
          <button class="btn btn-primary" @click="handleSubmit" :disabled="!modalForm.name.trim()">
            {{ editTarget ? '저장' : '만들기' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 삭제 확인 모달 -->
    <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
      <div class="modal card">
        <p>
          <strong>{{ deleteTarget.name }}</strong> 프로젝트를 삭제할까요?<br>
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
  position: sticky; top: 0; z-index: 10;
}

.header-top {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 4px;
}
.header-top h1 { font-size: 1.4rem; font-weight: 700; }
.header-actions { display: flex; align-items: center; gap: 8px; }
.username { font-size: 0.85rem; color: var(--text-secondary); }

.icon-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  cursor: pointer; font-size: 1.1rem;
  display: flex; align-items: center; justify-content: center;
  background: var(--surface2); transition: all 0.2s;
}

.subtitle { font-size: 0.85rem; color: var(--text-secondary); margin-top: 4px; }

.projects-content { flex: 1; padding: 16px; max-width: 600px; margin: 0 auto; width: 100%; }

.new-project-btn {
  width: 100%;
  padding: 16px;
  border: 2px dashed var(--border);
  border-radius: 12px;
  background: none;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  font-size: 1rem; color: var(--text-secondary);
  transition: all 0.2s;
  margin-bottom: 16px;
}
.new-project-btn:hover { border-color: var(--primary); color: var(--primary); background: var(--surface2); }
.new-project-btn .plus { font-size: 1.3rem; }

.project-list { display: flex; flex-direction: column; gap: 10px; }

.project-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px; cursor: pointer; transition: all 0.2s;
}
.project-card:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.project-info { display: flex; align-items: flex-start; gap: 12px; flex: 1; min-width: 0; }
.project-icon { font-size: 1.8rem; flex-shrink: 0; }
.project-name { font-weight: 600; font-size: 1rem; margin-bottom: 2px; }
.project-desc { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }
.project-date { font-size: 0.75rem; color: var(--text-secondary); }

.project-actions { display: flex; gap: 4px; flex-shrink: 0; }
.icon-btn-sm {
  width: 30px; height: 30px; border-radius: 8px; border: none;
  cursor: pointer; font-size: 0.9rem;
  display: flex; align-items: center; justify-content: center;
  background: var(--surface2); transition: all 0.2s;
}
.icon-btn-sm.danger:hover { background: #fff5f5; }

.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 100; padding: 20px;
}
.modal { width: 100%; max-width: 360px; padding: 24px; }
.modal h3 { margin-bottom: 20px; font-size: 1.1rem; }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: 0.85rem; font-weight: 600; margin-bottom: 6px; color: var(--text-secondary); }
.field input {
  width: 100%; padding: 10px 12px; border: 1.5px solid var(--border);
  border-radius: 8px; font-size: 0.95rem; background: var(--surface2);
  color: var(--text); box-sizing: border-box; transition: border-color 0.2s;
}
.field input:focus { outline: none; border-color: var(--primary); }
.modal p { margin-bottom: 16px; line-height: 1.6; }
.modal small { color: var(--text-secondary); font-size: 0.85rem; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; }
</style>
