<template>
  <div class="home">
    <header class="home-header">
      <div class="header-top">
        <div class="back-title">
          <button class="back-btn" @click="$router.push('/')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 5l-7 7 7 7"/></svg>
          </button>
          <div>
            <div class="project-label">프로젝트</div>
            <h1>{{ projectName }}</h1>
          </div>
        </div>
        <div class="header-actions">
          <button class="icon-btn" @click="$router.push('/settings')" title="설정">⚙️</button>
          <button class="icon-btn add-btn" @click="$router.push(`/project/${projectId}/create`)" title="새 캐릭터">＋</button>
        </div>
      </div>
      <div class="sort-tabs">
        <button
          v-for="tab in sortTabs"
          :key="tab.value"
          :class="['sort-tab', { active: store.sort === tab.value }]"
          @click="store.setSort(tab.value, projectId)"
        >
          <span>{{ tab.icon }}</span> {{ tab.label }}
        </button>
      </div>
    </header>

    <main class="home-content">
      <div v-if="store.loading" class="empty-state">
        <div class="spinner"></div>
      </div>

      <div v-else-if="store.characters.length === 0" class="empty-state">
        <div class="emoji">🌟</div>
        <p>아직 친구가 없어요.<br>첫 AI 친구를 만들어보세요!</p>
        <button class="btn btn-primary" style="margin-top:16px"
          @click="$router.push(`/project/${projectId}/create`)">
          첫 친구 만들기 ✨
        </button>
      </div>

      <div v-else class="character-list">
        <CharacterCard
          v-for="c in store.characters"
          :key="c.id"
          :character="c"
          @click="$router.push(`/chat/${c.id}`)"
          @favorite="store.toggleFavorite(c.id)"
          @edit="$router.push(`/project/${projectId}/edit/${c.id}`)"
          @delete="handleDelete(c)"
        />
      </div>
    </main>

    <!-- Delete Confirm Modal -->
    <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
      <div class="modal card">
        <div class="modal-icon">🗑️</div>
        <div class="modal-title">캐릭터 삭제</div>
        <p class="modal-desc">
          <strong>{{ deleteTarget.name }}</strong>을(를) 삭제할까요?<br>
          <small>대화 내역도 모두 삭제됩니다.</small>
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
import { ref, onMounted, computed } from 'vue'
import { useCharacterStore } from '../stores/character'
import { useProjectStore } from '../stores/project'
import CharacterCard from '../components/CharacterCard.vue'

const props = defineProps({ projectId: String })

const store = useCharacterStore()
const projectStore = useProjectStore()
const deleteTarget = ref(null)

const sortTabs = [
  { value: 'recent', label: '최근 대화', icon: '🕐' },
  { value: 'name', label: '이름순', icon: '🔤' },
  { value: 'favorite', label: '즐겨찾기', icon: '⭐' }
]

const projectName = computed(() => {
  const p = projectStore.projects.find(p => p.id === props.projectId)
  return p?.name || '캐릭터'
})

onMounted(async () => {
  if (!projectStore.projects.length) await projectStore.fetchAll()
  await store.fetchAll(props.projectId)
})

function handleDelete(c) { deleteTarget.value = c }

async function confirmDelete() {
  await store.delete(deleteTarget.value.id)
  deleteTarget.value = null
}
</script>

<style scoped>
.home { min-height: 100vh; display: flex; flex-direction: column; }

.home-header {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  padding: 14px 16px 0;
  position: sticky; top: 0; z-index: 10;
  backdrop-filter: blur(12px);
}

.header-top {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.back-title { display: flex; align-items: center; gap: 10px; }
.back-btn {
  width: 36px; height: 36px; background: var(--surface2);
  border: none; border-radius: 50%; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: var(--text); transition: background 0.2s;
}
.back-btn:hover { background: var(--border); }
.project-label { font-size: 0.72rem; color: var(--text-secondary); font-weight: 500; margin-bottom: 1px; }
h1 { font-size: 1.15rem; font-weight: 700; }

.header-actions { display: flex; gap: 6px; }
.icon-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  cursor: pointer; font-size: 1rem;
  display: flex; align-items: center; justify-content: center;
  background: var(--surface2); transition: all 0.2s;
}
.add-btn { background: var(--grad-main); color: #fff; font-size: 1.2rem; box-shadow: 0 4px 12px var(--primary-glow); }
.add-btn:hover { filter: brightness(1.1); }

.sort-tabs { display: flex; gap: 2px; }
.sort-tab {
  padding: 8px 12px; border: none; background: none; cursor: pointer;
  font-family: inherit; font-size: 0.82rem; color: var(--text-secondary);
  border-bottom: 2px solid transparent; transition: all 0.2s;
  border-radius: var(--radius-xs) var(--radius-xs) 0 0;
  display: flex; align-items: center; gap: 4px;
}
.sort-tab.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }

.home-content { flex: 1; padding: 12px; }
.character-list { display: flex; flex-direction: column; gap: 8px; }

.modal { padding: 28px; }
.modal-icon { font-size: 2rem; text-align: center; margin-bottom: 8px; }
.modal-title { font-size: 1.05rem; font-weight: 700; text-align: center; margin-bottom: 12px; }
.modal-desc { text-align: center; margin-bottom: 20px; line-height: 1.6; font-size: 0.9rem; }
.modal-desc small { color: var(--text-secondary); font-size: 0.82rem; }
.modal-actions { display: flex; gap: 8px; justify-content: center; }
</style>
