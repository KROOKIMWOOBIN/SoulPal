<template>
  <div class="home">
    <header class="home-header">
      <div class="header-top">
        <div class="back-title">
          <button class="icon-btn" @click="$router.push('/')" title="프로젝트 목록">←</button>
          <h1>{{ projectName }}</h1>
        </div>
        <div class="header-actions">
          <button class="icon-btn" @click="$router.push('/settings')" title="설정">⚙️</button>
          <button class="icon-btn btn-primary-icon" @click="$router.push(`/project/${projectId}/create`)" title="새 캐릭터">＋</button>
        </div>
      </div>
      <div class="sort-tabs">
        <button
          v-for="tab in sortTabs"
          :key="tab.value"
          :class="['sort-tab', { active: store.sort === tab.value }]"
          @click="store.setSort(tab.value, projectId)"
        >
          {{ tab.label }}
        </button>
      </div>
    </header>

    <main class="home-content">
      <div v-if="store.loading" class="empty-state">
        <div class="spinner"></div>
      </div>

      <div v-else-if="store.characters.length === 0" class="empty-state">
        <div class="emoji">🌟</div>
        <p>아직 친구가 없어요.<br>새 캐릭터를 만들어보세요!</p>
        <button class="btn btn-primary" style="margin-top:16px" @click="$router.push(`/project/${projectId}/create`)">
          첫 친구 만들기
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
        <p>{{ deleteTarget.name }}을(를) 삭제할까요?<br><small>대화 내역도 모두 삭제됩니다.</small></p>
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
  { value: 'recent', label: '최근 대화' },
  { value: 'name', label: '이름순' },
  { value: 'favorite', label: '즐겨찾기' }
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
  padding: 16px 16px 0;
  position: sticky; top: 0; z-index: 10;
}

.header-top {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
}

.back-title { display: flex; align-items: center; gap: 8px; }
.back-title h1 { font-size: 1.2rem; font-weight: 700; }

.header-actions { display: flex; gap: 8px; }
.icon-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  cursor: pointer; font-size: 1.1rem;
  display: flex; align-items: center; justify-content: center;
  background: var(--surface2); transition: all 0.2s;
}
.btn-primary-icon { background: var(--primary); color: #fff; font-size: 1.3rem; }
.btn-primary-icon:hover { background: var(--primary-dark); }

.sort-tabs { display: flex; gap: 4px; }
.sort-tab {
  padding: 8px 14px; border: none; background: none; cursor: pointer;
  font-family: inherit; font-size: 0.85rem; color: var(--text-secondary);
  border-bottom: 2px solid transparent; transition: all 0.2s;
}
.sort-tab.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }

.home-content { flex: 1; padding: 12px; }
.character-list { display: flex; flex-direction: column; gap: 8px; }

.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 100; padding: 20px;
}
.modal { text-align: center; }
.modal p { margin-bottom: 16px; line-height: 1.6; }
.modal small { color: var(--text-secondary); font-size: 0.85rem; }
.modal-actions { display: flex; gap: 8px; justify-content: center; }
</style>
