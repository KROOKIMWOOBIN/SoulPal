import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCharacterStore } from '../../stores/character'

vi.mock('../../api', () => ({
  characterApi: {
    getAll:         vi.fn(),
    getById:        vi.fn(),
    create:         vi.fn(),
    update:         vi.fn(),
    delete:         vi.fn(),
    toggleFavorite: vi.fn(),
    getCategories:  vi.fn()
  }
}))

import { characterApi } from '../../api'

const mockPage = (items) => ({
  content:        items,
  totalPages:     1,
  totalElements:  items.length,
  number:         0
})

const char1 = { id: 'c1', name: 'Alice' }
const char2 = { id: 'c2', name: 'Bob' }

describe('useCharacterStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchAll — Page 응답에서 content 추출', async () => {
    characterApi.getAll.mockResolvedValue({ data: mockPage([char1, char2]) })
    const store = useCharacterStore()

    await store.fetchAll('proj-1')

    expect(store.characters).toEqual([char1, char2])
    expect(store.totalElements).toBe(2)
    expect(store.currentPage).toBe(0)
  })

  it('fetchAll — API 오류 시 error 상태 설정', async () => {
    characterApi.getAll.mockRejectedValue(new Error('network error'))
    const store = useCharacterStore()

    await store.fetchAll('proj-1')

    expect(store.error).toBe('network error')
    expect(store.loading).toBe(false)
  })

  it('create — 새 캐릭터를 목록 앞에 추가', async () => {
    characterApi.create.mockResolvedValue({ data: char1 })
    characterApi.getAll.mockResolvedValue({ data: mockPage([char2]) })
    const store = useCharacterStore()
    await store.fetchAll('proj-1')

    await store.create({ name: 'Alice', projectId: 'proj-1' })

    expect(store.characters[0]).toEqual(char1)
  })

  it('delete — 캐릭터를 목록에서 제거', async () => {
    characterApi.delete.mockResolvedValue({})
    characterApi.getAll.mockResolvedValue({ data: mockPage([char1, char2]) })
    const store = useCharacterStore()
    await store.fetchAll('proj-1')

    await store.delete('c1')

    expect(store.characters.find(c => c.id === 'c1')).toBeUndefined()
    expect(store.characters).toHaveLength(1)
  })

  it('toggleFavorite — 대상 캐릭터 업데이트', async () => {
    const updated = { ...char1, favorite: true }
    characterApi.toggleFavorite.mockResolvedValue({ data: updated })
    characterApi.getAll.mockResolvedValue({ data: mockPage([char1]) })
    const store = useCharacterStore()
    await store.fetchAll('proj-1')

    await store.toggleFavorite('c1')

    expect(store.characters[0].favorite).toBe(true)
  })
})
