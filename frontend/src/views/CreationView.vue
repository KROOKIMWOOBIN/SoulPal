<template>
  <div class="creation">
    <header class="page-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 5l-7 7 7 7"/></svg>
      </button>
      <h1>{{ isEdit ? '캐릭터 편집' : '새 친구 만들기' }}</h1>
      <span class="step-count">{{ step }} / 6</span>
    </header>

    <!-- Progress -->
    <div class="progress-wrap">
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: (step / 6 * 100) + '%' }"></div>
      </div>
      <div class="step-dots">
        <div v-for="i in 6" :key="i" :class="['dot', { done: i < step, active: i === step }]"></div>
      </div>
    </div>

    <main class="creation-content">
      <div v-if="!categories" class="empty-state"><div class="spinner"></div></div>

      <template v-else>
        <!-- Step 1: Relationship -->
        <div v-if="step === 1" class="step">
          <div class="step-header">
            <div class="step-emoji">💞</div>
            <h2>어떤 관계를 원하나요?</h2>
          </div>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.relationships" :key="item.id"
              :item="item" :selected="form.relationshipId === item.id"
              @click="form.relationshipId = item.id"
            />
          </div>
        </div>

        <!-- Step 2: Personality -->
        <div v-if="step === 2" class="step">
          <div class="step-header">
            <div class="step-emoji">✨</div>
            <h2>어떤 성격인가요?</h2>
          </div>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.personalities" :key="item.id"
              :item="item" :selected="form.personalityId === item.id"
              @click="form.personalityId = item.id"
            />
          </div>
        </div>

        <!-- Step 3: Speech Style -->
        <div v-if="step === 3" class="step">
          <div class="step-header">
            <div class="step-emoji">💬</div>
            <h2>말투를 선택하세요</h2>
          </div>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.speechStyles" :key="item.id"
              :item="item" :selected="form.speechStyleId === item.id"
              @click="form.speechStyleId = item.id"
            />
          </div>
        </div>

        <!-- Step 4: Interests (multi-select) -->
        <div v-if="step === 4" class="step">
          <div class="step-header">
            <div class="step-emoji">🎯</div>
            <h2>관심사를 골라보세요</h2>
            <p class="step-sub">여러 개 선택 가능</p>
          </div>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.interests" :key="item.id"
              :item="item" :selected="form.interestIds.includes(item.id)"
              @click="toggleInterest(item.id)"
            />
          </div>
        </div>

        <!-- Step 5: Appearance -->
        <div v-if="step === 5" class="step">
          <div class="step-header">
            <div class="step-emoji">🎨</div>
            <h2>어떤 분위기인가요?</h2>
          </div>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.appearances" :key="item.id"
              :item="item" :selected="form.appearanceId === item.id"
              @click="form.appearanceId = item.id"
            />
          </div>
        </div>

        <!-- Step 6: Name -->
        <div v-if="step === 6" class="step">
          <div class="step-header">
            <div class="step-emoji">🏷️</div>
            <h2>이름을 정해주세요</h2>
          </div>
          <input
            v-model="form.name"
            class="name-input input"
            placeholder="친구 이름 입력"
            maxlength="20"
            @keyup.enter="submit"
            autofocus
          />
          <div class="suggestions-wrap">
            <div class="suggestions-label">💡 추천 이름</div>
            <div class="suggestion-chips">
              <button
                v-for="name in suggestions" :key="name"
                class="suggestion-chip"
                @click="form.name = name"
              >{{ name }}</button>
            </div>
          </div>
        </div>
      </template>
    </main>

    <footer class="creation-footer">
      <button v-if="step > 1" class="btn btn-secondary" @click="step--">← 이전</button>
      <div v-else></div>
      <button class="btn btn-primary" :disabled="!canProceed || saving" @click="next">
        <span v-if="saving" class="spinner small-white"></span>
        <span v-else>{{ step === 6 ? '완료 ✨' : '다음 →' }}</span>
      </button>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCharacterStore } from '../stores/character'
import { characterApi } from '../api'
import OptionCard from '../components/OptionCard.vue'

const props = defineProps({ id: String, projectId: String })
const router = useRouter()
const store = useCharacterStore()

const step = ref(1)
const saving = ref(false)
const categories = ref(null)
const isEdit = computed(() => !!props.id)

const form = ref({
  name: '', relationshipId: '', personalityId: '',
  speechStyleId: '', interestIds: [], appearanceId: '',
  projectId: props.projectId || null
})

const suggestions = computed(() => {
  const map = {
    bestfriend: ['하늘', '봄이', '별이', '다온'],
    mentor: ['준혁', '지수', '민준', '서연'],
    lover: ['유나', '준서', '하은', '민호'],
    sibling_older: ['언니', '오빠', '누나', '형'],
    sibling_younger: ['동동', '봄봄', '해찬', '다빈'],
    school_friend: ['지민', '태양', '로제', '진']
  }
  return map[form.value.relationshipId] || ['소울', '팔', '루나', '노바']
})

const canProceed = computed(() => {
  switch (step.value) {
    case 1: return !!form.value.relationshipId
    case 2: return !!form.value.personalityId
    case 3: return !!form.value.speechStyleId
    case 4: return form.value.interestIds.length > 0
    case 5: return !!form.value.appearanceId
    case 6: return form.value.name.trim().length > 0
  }
})

function toggleInterest(id) {
  const idx = form.value.interestIds.indexOf(id)
  if (idx === -1) form.value.interestIds.push(id)
  else form.value.interestIds.splice(idx, 1)
}

async function next() {
  if (step.value < 6) { step.value++; return }
  await submit()
}

async function submit() {
  saving.value = true
  try {
    if (isEdit.value) {
      await store.update(props.id, form.value)
    } else {
      await store.create(form.value)
    }
    const target = props.projectId ? `/project/${props.projectId}` : '/'
    router.push(target)
  } catch (e) {
    alert('저장 중 오류가 발생했습니다.')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await store.fetchCategories()
  categories.value = store.categories

  if (isEdit.value) {
    const { data } = await characterApi.getById(props.id)
    form.value = {
      name: data.name,
      relationshipId: data.relationshipId,
      personalityId: data.personalityId,
      speechStyleId: data.speechStyleId,
      interestIds: data.interestIds || [],
      appearanceId: data.appearanceId,
      projectId: data.projectId
    }
  }
})
</script>

<style scoped>
.creation { display: flex; flex-direction: column; height: 100vh; }

.step-count { font-size: 0.8rem; color: var(--text-secondary); font-weight: 600; background: var(--surface2); padding: 4px 10px; border-radius: 20px; }

.progress-wrap { padding: 12px 16px 4px; }
.progress-track { height: 4px; background: var(--border); border-radius: 4px; overflow: hidden; margin-bottom: 10px; }
.progress-fill { height: 100%; background: var(--grad-main); border-radius: 4px; transition: width 0.4s cubic-bezier(0.34,1.56,0.64,1); }

.step-dots { display: flex; gap: 6px; justify-content: center; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: var(--border); transition: all 0.3s; }
.dot.done { background: var(--primary-light); }
.dot.active { background: var(--primary); width: 20px; border-radius: 4px; }

.creation-content { flex: 1; overflow-y: auto; padding: 20px 16px; }

.step-header { text-align: center; margin-bottom: 20px; }
.step-emoji { font-size: 2.2rem; margin-bottom: 8px; }
.step-header h2 { font-size: 1.2rem; font-weight: 700; }
.step-sub { font-size: 0.82rem; color: var(--text-secondary); margin-top: 4px; }

.options-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }

.name-input { font-size: 1.1rem; margin-top: 4px; }

.suggestions-wrap { margin-top: 22px; }
.suggestions-label { font-size: 0.82rem; color: var(--text-secondary); margin-bottom: 10px; font-weight: 500; }
.suggestion-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.suggestion-chip {
  border: 1.5px solid var(--border); background: var(--surface);
  cursor: pointer; font-family: inherit;
  color: var(--text); padding: 7px 16px;
  border-radius: 20px; font-size: 0.88rem; font-weight: 500;
  transition: all 0.2s;
}
.suggestion-chip:hover { border-color: var(--primary); color: var(--primary); background: var(--primary-bg); }

.creation-footer {
  display: flex; justify-content: space-between;
  padding: 12px 16px 18px;
  border-top: 1px solid var(--border);
  background: var(--surface);
}
.creation-footer .btn { min-width: 100px; }

.small-white { width: 16px; height: 16px; border-width: 2px; border-color: rgba(255,255,255,0.3); border-top-color: #fff; }
</style>
