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
        <!-- Step 1: Relationship (단일 선택) -->
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
            <button :class="['option-card', { selected: form.relationshipId.startsWith('custom:') }]"
                    @click="openCustom('relationshipId')">
              <span class="option-emoji">✏️</span>
              <span class="option-label">직접 입력</span>
            </button>
          </div>
          <div v-if="form.relationshipId.startsWith('custom:')" class="custom-input-wrap">
            <input
              :value="form.relationshipId.slice(7)"
              @input="form.relationshipId = 'custom:' + $event.target.value"
              class="input custom-input"
              placeholder="예: 소울메이트, 라이벌, 단짝..."
              maxlength="100"
              autofocus
            />
          </div>
        </div>

        <!-- Step 2: Personality (복수 선택) -->
        <div v-if="step === 2" class="step">
          <div class="step-header">
            <div class="step-emoji">✨</div>
            <h2>어떤 성격인가요?</h2>
            <p class="step-sub">여러 개 선택 가능</p>
          </div>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.personalities" :key="item.id"
              :item="item" :selected="form.personalityIds.includes(item.id)"
              @click="toggleList('personalityIds', item.id)"
            />
            <button :class="['option-card', { selected: hasCustom('personalityIds') }]"
                    @click="openCustomList('personalityIds')">
              <span class="option-emoji">✏️</span>
              <span class="option-label">직접 입력</span>
            </button>
          </div>
          <div v-if="showCustomInput === 'personalityIds'" class="custom-input-wrap">
            <input
              v-model="customText"
              class="input custom-input"
              placeholder="예: 철학적이고 내성적인, 독특한 유머 감각..."
              maxlength="200"
              @keyup.enter="confirmCustom('personalityIds')"
              autofocus
            />
            <button class="btn btn-primary btn-sm" @click="confirmCustom('personalityIds')">추가</button>
          </div>
          <div v-if="customTags('personalityIds').length" class="custom-tags">
            <span v-for="tag in customTags('personalityIds')" :key="tag" class="custom-tag">
              {{ tag.slice(7) }}
              <button @click="removeFromList('personalityIds', tag)">×</button>
            </span>
          </div>
        </div>

        <!-- Step 3: Speech Style (복수 선택) -->
        <div v-if="step === 3" class="step">
          <div class="step-header">
            <div class="step-emoji">💬</div>
            <h2>말투를 선택하세요</h2>
            <p class="step-sub">여러 개 선택 가능</p>
          </div>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.speechStyles" :key="item.id"
              :item="item" :selected="form.speechStyleIds.includes(item.id)"
              @click="toggleList('speechStyleIds', item.id)"
            />
            <button :class="['option-card', { selected: hasCustom('speechStyleIds') }]"
                    @click="openCustomList('speechStyleIds')">
              <span class="option-emoji">✏️</span>
              <span class="option-label">직접 입력</span>
            </button>
          </div>
          <div v-if="showCustomInput === 'speechStyleIds'" class="custom-input-wrap">
            <input
              v-model="customText"
              class="input custom-input"
              placeholder="예: 문학적이고 시적인, 고양이처럼 귀엽게..."
              maxlength="200"
              @keyup.enter="confirmCustom('speechStyleIds')"
              autofocus
            />
            <button class="btn btn-primary btn-sm" @click="confirmCustom('speechStyleIds')">추가</button>
          </div>
          <div v-if="customTags('speechStyleIds').length" class="custom-tags">
            <span v-for="tag in customTags('speechStyleIds')" :key="tag" class="custom-tag">
              {{ tag.slice(7) }}
              <button @click="removeFromList('speechStyleIds', tag)">×</button>
            </span>
          </div>
        </div>

        <!-- Step 4: Interests (복수 선택) -->
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
              @click="toggleList('interestIds', item.id)"
            />
            <button :class="['option-card', { selected: hasCustom('interestIds') }]"
                    @click="openCustomList('interestIds')">
              <span class="option-emoji">✏️</span>
              <span class="option-label">직접 입력</span>
            </button>
          </div>
          <div v-if="showCustomInput === 'interestIds'" class="custom-input-wrap">
            <input
              v-model="customText"
              class="input custom-input"
              placeholder="예: 천문학, 재즈, 빈티지 카메라..."
              maxlength="200"
              @keyup.enter="confirmCustom('interestIds')"
              autofocus
            />
            <button class="btn btn-primary btn-sm" @click="confirmCustom('interestIds')">추가</button>
          </div>
          <div v-if="customTags('interestIds').length" class="custom-tags">
            <span v-for="tag in customTags('interestIds')" :key="tag" class="custom-tag">
              {{ tag.slice(7) }}
              <button @click="removeFromList('interestIds', tag)">×</button>
            </span>
          </div>
        </div>

        <!-- Step 5: Appearance (복수 선택) -->
        <div v-if="step === 5" class="step">
          <div class="step-header">
            <div class="step-emoji">🎨</div>
            <h2>어떤 분위기인가요?</h2>
            <p class="step-sub">여러 개 선택 가능</p>
          </div>
          <div class="options-grid">
            <OptionCard
              v-for="item in categories.appearances" :key="item.id"
              :item="item" :selected="form.appearanceIds.includes(item.id)"
              @click="toggleList('appearanceIds', item.id)"
            />
            <button :class="['option-card', { selected: hasCustom('appearanceIds') }]"
                    @click="openCustomList('appearanceIds')">
              <span class="option-emoji">✏️</span>
              <span class="option-label">직접 입력</span>
            </button>
          </div>
          <div v-if="showCustomInput === 'appearanceIds'" class="custom-input-wrap">
            <input
              v-model="customText"
              class="input custom-input"
              placeholder="예: 차갑지만 속이 따뜻한, 신비로운..."
              maxlength="200"
              @keyup.enter="confirmCustom('appearanceIds')"
              autofocus
            />
            <button class="btn btn-primary btn-sm" @click="confirmCustom('appearanceIds')">추가</button>
          </div>
          <div v-if="customTags('appearanceIds').length" class="custom-tags">
            <span v-for="tag in customTags('appearanceIds')" :key="tag" class="custom-tag">
              {{ tag.slice(7) }}
              <button @click="removeFromList('appearanceIds', tag)">×</button>
            </span>
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

// 커스텀 입력 UI 상태
const showCustomInput = ref(null) // 현재 열린 커스텀 입력 필드명
const customText = ref('')

const form = ref({
  name: '',
  relationshipId: '',
  personalityIds: [],
  speechStyleIds: [],
  interestIds: [],
  appearanceIds: [],
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
  const relId = form.value.relationshipId.startsWith('custom:')
    ? null
    : form.value.relationshipId
  return map[relId] || ['소울', '팔', '루나', '노바']
})

const canProceed = computed(() => {
  switch (step.value) {
    case 1: return !!form.value.relationshipId && !form.value.relationshipId.endsWith(':')
    case 2: return form.value.personalityIds.length > 0
    case 3: return form.value.speechStyleIds.length > 0
    case 4: return form.value.interestIds.length > 0
    case 5: return form.value.appearanceIds.length > 0
    case 6: return form.value.name.trim().length > 0
  }
})

// 단일 선택 (관계) 커스텀 열기
function openCustom(field) {
  if (!form.value[field].startsWith('custom:')) {
    form.value[field] = 'custom:'
  }
}

// 복수 선택 리스트 토글
function toggleList(field, id) {
  const list = form.value[field]
  const idx = list.indexOf(id)
  if (idx === -1) list.push(id)
  else list.splice(idx, 1)
}

function removeFromList(field, id) {
  const list = form.value[field]
  const idx = list.indexOf(id)
  if (idx !== -1) list.splice(idx, 1)
}

// 복수 선택 커스텀 관련
function openCustomList(field) {
  showCustomInput.value = showCustomInput.value === field ? null : field
  customText.value = ''
}

function confirmCustom(field) {
  const text = customText.value.trim()
  if (!text) return
  const customId = 'custom:' + text
  if (!form.value[field].includes(customId)) {
    form.value[field].push(customId)
  }
  customText.value = ''
  showCustomInput.value = null
}

function hasCustom(field) {
  return form.value[field].some(id => id.startsWith('custom:'))
}

function customTags(field) {
  return form.value[field].filter(id => id.startsWith('custom:'))
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
      relationshipId: data.relationshipId || '',
      personalityIds: data.personalityIds || [],
      speechStyleIds: data.speechStyleIds || [],
      interestIds: data.interestIds || [],
      appearanceIds: data.appearanceIds || [],
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

/* 직접 입력 카드 스타일 (OptionCard와 동일하게) */
.option-card {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; padding: 16px 12px;
  border: 2px solid var(--border); border-radius: var(--radius);
  background: var(--surface); cursor: pointer; transition: all 0.2s; font-family: inherit;
}
.option-card:hover { border-color: var(--primary-light); background: var(--primary-bg); }
.option-card.selected { border-color: var(--primary); background: var(--primary-bg); }
.option-emoji { font-size: 1.6rem; }
.option-label { font-size: 0.85rem; font-weight: 500; color: var(--text); }
.option-card.selected .option-label { color: var(--primary); }

/* 커스텀 입력 영역 */
.custom-input-wrap {
  display: flex; gap: 8px; margin-top: 12px;
}
.custom-input { flex: 1; }

.btn-sm { padding: 8px 16px; font-size: 0.85rem; white-space: nowrap; }

/* 커스텀 태그 */
.custom-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.custom-tag {
  display: flex; align-items: center; gap: 4px;
  background: var(--primary-bg); border: 1.5px solid var(--primary-light);
  color: var(--primary); border-radius: 20px;
  padding: 4px 10px 4px 12px; font-size: 0.82rem; font-weight: 500;
}
.custom-tag button {
  background: none; border: none; cursor: pointer;
  color: var(--primary); font-size: 1rem; line-height: 1;
  padding: 0 2px; font-family: inherit;
}

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
