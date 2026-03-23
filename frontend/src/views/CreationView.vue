<template>
  <div class="creation">
    <header class="page-header">
      <button class="back-btn" @click="$router.back()">←</button>
      <h1>{{ isEdit ? '캐릭터 편집' : '새 친구 만들기' }}</h1>
      <span class="step-count">{{ step }}/6</span>
    </header>

    <div class="progress-bar">
      <div class="progress-fill" :style="{ width: (step / 6 * 100) + '%' }"></div>
    </div>

    <main class="creation-content">
      <div v-if="!categories" class="empty-state"><div class="spinner"></div></div>

      <template v-else>
        <!-- Step 1: Relationship -->
        <div v-if="step === 1" class="step">
          <h2>어떤 관계를 원하나요?</h2>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.relationships"
              :key="item.id"
              :item="item"
              :selected="form.relationshipId === item.id"
              @click="form.relationshipId = item.id"
            />
          </div>
        </div>

        <!-- Step 2: Personality -->
        <div v-if="step === 2" class="step">
          <h2>어떤 성격인가요?</h2>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.personalities"
              :key="item.id"
              :item="item"
              :selected="form.personalityId === item.id"
              @click="form.personalityId = item.id"
            />
          </div>
        </div>

        <!-- Step 3: Speech Style -->
        <div v-if="step === 3" class="step">
          <h2>말투를 선택하세요</h2>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.speechStyles"
              :key="item.id"
              :item="item"
              :selected="form.speechStyleId === item.id"
              @click="form.speechStyleId = item.id"
            />
          </div>
        </div>

        <!-- Step 4: Interests (multi-select) -->
        <div v-if="step === 4" class="step">
          <h2>관심사를 골라보세요</h2>
          <p class="step-subtitle">여러 개 선택 가능</p>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.interests"
              :key="item.id"
              :item="item"
              :selected="form.interestIds.includes(item.id)"
              @click="toggleInterest(item.id)"
            />
          </div>
        </div>

        <!-- Step 5: Appearance -->
        <div v-if="step === 5" class="step">
          <h2>어떤 분위기인가요?</h2>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.appearances"
              :key="item.id"
              :item="item"
              :selected="form.appearanceId === item.id"
              @click="form.appearanceId = item.id"
            />
          </div>
        </div>

        <!-- Step 6: Name -->
        <div v-if="step === 6" class="step">
          <h2>이름을 정해주세요</h2>
          <input
            v-model="form.name"
            class="input name-input"
            placeholder="친구 이름 입력"
            maxlength="20"
            @keyup.enter="submit"
          />
          <div class="name-suggestions">
            <span class="suggestion-label">추천 이름</span>
            <div class="suggestion-chips">
              <button
                v-for="name in suggestions"
                :key="name"
                class="chip suggestion-chip"
                @click="form.name = name"
              >
                {{ name }}
              </button>
            </div>
          </div>
        </div>
      </template>
    </main>

    <footer class="creation-footer">
      <button v-if="step > 1" class="btn btn-secondary" @click="step--">이전</button>
      <div v-else></div>
      <button
        class="btn btn-primary"
        :disabled="!canProceed || saving"
        @click="next"
      >
        {{ step === 6 ? (saving ? '저장 중...' : '완료') : '다음' }}
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

const props = defineProps({ id: String })
const router = useRouter()
const store = useCharacterStore()

const step = ref(1)
const saving = ref(false)
const categories = ref(null)
const isEdit = computed(() => !!props.id)

const form = ref({
  name: '',
  relationshipId: '',
  personalityId: '',
  speechStyleId: '',
  interestIds: [],
  appearanceId: ''
})

const suggestions = computed(() => {
  const rel = form.value.relationshipId
  const map = {
    bestfriend: ['하늘', '봄이', '별이', '다온'],
    mentor: ['준혁', '지수', '민준', '서연'],
    lover: ['유나', '준서', '하은', '민호'],
    sibling_older: ['언니', '오빠', '누나', '형'],
    sibling_younger: ['동동', '봄봄', '해찬', '다빈'],
    school_friend: ['지민', '태양', '로제', '진']
  }
  return map[rel] || ['소울', '팔', '루나', '노바']
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
    router.push('/')
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
      appearanceId: data.appearanceId
    }
  }
})
</script>

<style scoped>
.creation { display: flex; flex-direction: column; height: 100vh; }

.step-count { font-size: 0.85rem; color: var(--text-secondary); font-weight: 500; }

.progress-bar { height: 3px; background: var(--border); }
.progress-fill { height: 100%; background: var(--primary); transition: width 0.3s ease; }

.creation-content { flex: 1; overflow-y: auto; padding: 24px 16px; }

.step h2 { font-size: 1.2rem; margin-bottom: 6px; }
.step-subtitle { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 16px; }

.options-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.name-input { font-size: 1.1rem; margin-top: 16px; }
.name-suggestions { margin-top: 20px; }
.suggestion-label { font-size: 0.82rem; color: var(--text-secondary); display: block; margin-bottom: 8px; }
.suggestion-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.suggestion-chip {
  border: none; cursor: pointer; font-family: inherit;
  background: var(--primary-bg); color: var(--primary);
  padding: 6px 14px; border-radius: 20px; font-size: 0.85rem;
  transition: background 0.15s;
}
.suggestion-chip:hover { background: var(--primary-light); color: #fff; }

.creation-footer {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  background: var(--surface);
}
.creation-footer .btn { min-width: 80px; justify-content: center; }
</style>
