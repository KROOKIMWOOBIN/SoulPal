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
          :class="['sort-tab', { active: activeTab === 'characters' && store.sort === tab.value }]"
          @click="activeTab = 'characters'; store.setSort(tab.value, projectId)"
        >
          <span>{{ tab.icon }}</span> {{ tab.label }}
        </button>
        <button
          :class="['sort-tab', { active: activeTab === 'groups' }]"
          @click="activeTab = 'groups'; loadGroups()"
        >
          👥 그룹
        </button>
      </div>
    </header>

    <main class="home-content">
      <!-- 캐릭터 탭 -->
      <template v-if="activeTab === 'characters'">
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
      </template>

      <!-- 그룹 탭 -->
      <template v-else>
        <div class="group-header">
          <button class="btn btn-primary btn-sm" @click="showGroupCreate = true">
            ＋ 그룹 만들기
          </button>
        </div>

        <div v-if="groupsLoading" class="empty-state"><div class="spinner"></div></div>

        <div v-else-if="groups.length === 0" class="empty-state">
          <div class="emoji">👥</div>
          <p>아직 그룹 대화방이 없어요.<br>여러 친구를 한 방에 초대해보세요!</p>
        </div>

        <div v-else class="character-list">
          <div
            v-for="g in groups"
            :key="g.id"
            class="group-card"
            @click="$router.push(`/group/${g.id}`)"
          >
            <div class="group-card-avatar">👥</div>
            <div class="group-card-info">
              <div class="group-card-name">{{ g.name }}</div>
              <div class="group-card-sub">{{ g.characterIds.length }}명 참여 · {{ g.lastMessage || '대화를 시작해보세요' }}</div>
            </div>
            <button class="icon-btn-sm" @click.stop="deleteGroup(g)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
              </svg>
            </button>
          </div>
        </div>
      </template>
    </main>

    <!-- Delete Confirm Modal (캐릭터) -->
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

    <!-- Group Create Modal -->
    <div v-if="showGroupCreate" class="modal-overlay" @click.self="showGroupCreate = false">
      <div class="modal card group-create-modal">
        <div class="modal-icon">👥</div>
        <div class="modal-title">그룹 대화방 만들기</div>

        <div class="form-field">
          <label>방 이름</label>
          <input v-model="groupForm.name" class="input" placeholder="우리들의 대화방" maxlength="50" />
        </div>

        <div class="form-field">
          <label>친구 선택 <span class="sub-label">(최소 2명)</span></label>
          <div class="char-select-list">
            <div
              v-for="c in store.characters"
              :key="c.id"
              :class="['char-select-item', { selected: groupForm.characterIds.includes(c.id) }]"
              @click="toggleGroupChar(c.id)"
            >
              <div class="char-select-avatar" :style="{ background: getRelGrad(c.relationshipId) }">
                {{ getRelEmoji(c.relationshipId) }}
              </div>
              <span>{{ c.name }}</span>
              <span v-if="groupForm.characterIds.includes(c.id)" class="check">✓</span>
            </div>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn btn-secondary" @click="showGroupCreate = false">취소</button>
          <button
            class="btn btn-primary"
            :disabled="!groupForm.name.trim() || groupForm.characterIds.length < 2 || groupCreating"
            @click="createGroup"
          >
            <span v-if="groupCreating" class="spinner small-white"></span>
            <span v-else>만들기</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCharacterStore } from '../stores/character'
import { useProjectStore } from '../stores/project'
import { groupRoomApi } from '../api'
import CharacterCard from '../components/CharacterCard.vue'

const props = defineProps({ projectId: String })
const router = useRouter()

const store = useCharacterStore()
const projectStore = useProjectStore()
const deleteTarget = ref(null)
const activeTab = ref('characters')

// 그룹 관련 상태
const groups = ref([])
const groupsLoading = ref(false)
const showGroupCreate = ref(false)
const groupCreating = ref(false)
const groupForm = ref({ name: '', characterIds: [] })

const sortTabs = [
  { value: 'recent', label: '최근 대화', icon: '🕐' },
  { value: 'name', label: '이름순', icon: '🔤' },
  { value: 'favorite', label: '즐겨찾기', icon: '⭐' }
]

const REL_GRADS = {
  bestfriend: 'linear-gradient(135deg,#f093fb,#f5576c)',
  mentor:     'linear-gradient(135deg,#4facfe,#00f2fe)',
  lover:      'linear-gradient(135deg,#f6d365,#fda085)',
  sibling_older: 'linear-gradient(135deg,#a18cd1,#fbc2eb)',
  sibling_younger: 'linear-gradient(135deg,#84fab0,#8fd3f4)',
  school_friend: 'linear-gradient(135deg,#30cfd0,#667eea)',
}
const REL_EMOJIS = { bestfriend:'💕', mentor:'🌟', lover:'💝', sibling_older:'🤗', sibling_younger:'😊', school_friend:'📚' }

function getRelGrad(relId) { return REL_GRADS[relId] || 'linear-gradient(135deg,#667eea,#764ba2)' }
function getRelEmoji(relId) { return REL_EMOJIS[relId] || '✨' }

const projectName = computed(() => {
  const p = projectStore.projects.find(p => p.id === props.projectId)
  return p?.name || '캐릭터'
})

async function loadGroups() {
  groupsLoading.value = true
  try {
    const { data } = await groupRoomApi.getAll(props.projectId)
    groups.value = data
  } catch (e) {
    console.error(e)
  } finally {
    groupsLoading.value = false
  }
}

function toggleGroupChar(id) {
  const idx = groupForm.value.characterIds.indexOf(id)
  if (idx === -1) groupForm.value.characterIds.push(id)
  else groupForm.value.characterIds.splice(idx, 1)
}

async function createGroup() {
  if (groupForm.value.characterIds.length < 2) return
  groupCreating.value = true
  try {
    const { data } = await groupRoomApi.create({
      projectId: props.projectId,
      name: groupForm.value.name,
      characterIds: groupForm.value.characterIds
    })
    groups.value.unshift(data)
    showGroupCreate.value = false
    groupForm.value = { name: '', characterIds: [] }
    router.push(`/group/${data.id}`)
  } catch (e) {
    alert('그룹 생성 중 오류가 발생했습니다.')
  } finally {
    groupCreating.value = false
  }
}

async function deleteGroup(g) {
  if (!confirm(`"${g.name}" 대화방을 삭제할까요?`)) return
  await groupRoomApi.delete(g.id)
  groups.value = groups.value.filter(r => r.id !== g.id)
}

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

.sort-tabs { display: flex; gap: 2px; overflow-x: auto; }
.sort-tab {
  padding: 8px 12px; border: none; background: none; cursor: pointer;
  font-family: inherit; font-size: 0.82rem; color: var(--text-secondary);
  border-bottom: 2px solid transparent; transition: all 0.2s;
  border-radius: var(--radius-xs) var(--radius-xs) 0 0;
  display: flex; align-items: center; gap: 4px; white-space: nowrap;
}
.sort-tab.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }

.home-content { flex: 1; padding: 12px; }
.character-list { display: flex; flex-direction: column; gap: 8px; }

/* 그룹 탭 */
.group-header { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.btn-sm { padding: 8px 16px; font-size: 0.85rem; }

.group-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; background: var(--surface);
  border: 1px solid var(--border); border-radius: var(--radius);
  cursor: pointer; transition: all 0.2s;
}
.group-card:hover { border-color: var(--primary-light); background: var(--primary-bg); }
.group-card-avatar {
  width: 46px; height: 46px; border-radius: 14px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.3rem; flex-shrink: 0;
}
.group-card-info { flex: 1; min-width: 0; }
.group-card-name { font-size: 0.95rem; font-weight: 600; margin-bottom: 2px; }
.group-card-sub { font-size: 0.8rem; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.icon-btn-sm {
  width: 30px; height: 30px; border-radius: 50%; border: none;
  background: var(--surface2); color: var(--text-secondary);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; flex-shrink: 0; transition: background 0.2s;
}
.icon-btn-sm:hover { background: var(--border); }

/* 모달 */
.modal { padding: 28px; }
.modal-icon { font-size: 2rem; text-align: center; margin-bottom: 8px; }
.modal-title { font-size: 1.05rem; font-weight: 700; text-align: center; margin-bottom: 16px; }
.modal-desc { text-align: center; margin-bottom: 20px; line-height: 1.6; font-size: 0.9rem; }
.modal-desc small { color: var(--text-secondary); font-size: 0.82rem; }
.modal-actions { display: flex; gap: 8px; justify-content: center; margin-top: 20px; }

/* 그룹 생성 모달 */
.group-create-modal { max-height: 90vh; overflow-y: auto; }
.form-field { margin-bottom: 16px; }
.form-field label { display: block; font-size: 0.85rem; font-weight: 600; margin-bottom: 6px; }
.sub-label { color: var(--text-secondary); font-weight: 400; }

.char-select-list { display: flex; flex-direction: column; gap: 6px; max-height: 200px; overflow-y: auto; }
.char-select-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border: 1.5px solid var(--border);
  border-radius: var(--radius); cursor: pointer; transition: all 0.2s;
}
.char-select-item:hover { border-color: var(--primary-light); }
.char-select-item.selected { border-color: var(--primary); background: var(--primary-bg); }
.char-select-avatar {
  width: 30px; height: 30px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; font-size: 0.9rem; flex-shrink: 0;
}
.char-select-item span { flex: 1; font-size: 0.9rem; font-weight: 500; }
.check { color: var(--primary); font-weight: 700; font-size: 1rem; }

.small-white { width: 16px; height: 16px; border-width: 2px; border-color: rgba(255,255,255,0.3); border-top-color: #fff; }
</style>
