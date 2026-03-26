import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProjectStore } from '../../stores/project'

vi.mock('../../api', () => ({
  projectApi: {
    getAll:  vi.fn(),
    create:  vi.fn(),
    update:  vi.fn(),
    delete:  vi.fn()
  }
}))

import { projectApi } from '../../api'

const proj1 = { id: 'p1', name: '프로젝트 A', description: '설명 A' }
const proj2 = { id: 'p2', name: '프로젝트 B', description: '설명 B' }

describe('useProjectStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ── 초기 상태 ────────────────────────────────────────────────────────────────

  it('초기 상태 — 기본값 확인', () => {
    const store = useProjectStore()
    expect(store.projects).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  // ── fetchAll ──────────────────────────────────────────────────────────────────

  it('fetchAll — 프로젝트 목록 로드', async () => {
    projectApi.getAll.mockResolvedValue({ data: [proj1, proj2] })

    const store = useProjectStore()
    await store.fetchAll()

    expect(store.projects).toEqual([proj1, proj2])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('fetchAll — 빈 목록', async () => {
    projectApi.getAll.mockResolvedValue({ data: [] })

    const store = useProjectStore()
    await store.fetchAll()

    expect(store.projects).toEqual([])
  })

  it('fetchAll — API 오류 시 error 상태 설정', async () => {
    projectApi.getAll.mockRejectedValue(new Error('서버 오류'))

    const store = useProjectStore()
    await store.fetchAll()

    expect(store.error).toBe('서버 오류')
    expect(store.loading).toBe(false)
    expect(store.projects).toEqual([])
  })

  it('fetchAll — 로딩 중 loading=true, 완료 후 false', async () => {
    let resolvePromise
    projectApi.getAll.mockReturnValue(new Promise(res => { resolvePromise = res }))

    const store = useProjectStore()
    const fetchPromise = store.fetchAll()
    expect(store.loading).toBe(true)

    resolvePromise({ data: [] })
    await fetchPromise
    expect(store.loading).toBe(false)
  })

  // ── create ────────────────────────────────────────────────────────────────────

  it('create — 새 프로젝트를 목록 앞에 추가', async () => {
    projectApi.getAll.mockResolvedValue({ data: [proj2] })
    projectApi.create.mockResolvedValue({ data: proj1 })

    const store = useProjectStore()
    await store.fetchAll()
    const result = await store.create({ name: '프로젝트 A', description: '설명 A' })

    expect(result).toEqual(proj1)
    expect(store.projects[0]).toEqual(proj1) // 맨 앞에 추가
    expect(store.projects).toHaveLength(2)
  })

  it('create — API 오류 시 예외 전파', async () => {
    projectApi.create.mockRejectedValue(new Error('생성 실패'))

    const store = useProjectStore()
    await expect(store.create({ name: '실패' })).rejects.toThrow('생성 실패')
  })

  // ── update ────────────────────────────────────────────────────────────────────

  it('update — 프로젝트 정보 업데이트', async () => {
    const updated = { ...proj1, name: '수정된 이름' }
    projectApi.getAll.mockResolvedValue({ data: [proj1, proj2] })
    projectApi.update.mockResolvedValue({ data: updated })

    const store = useProjectStore()
    await store.fetchAll()
    const result = await store.update('p1', { name: '수정된 이름', description: '설명 A' })

    expect(result).toEqual(updated)
    expect(store.projects.find(p => p.id === 'p1').name).toBe('수정된 이름')
  })

  it('update — 목록에 없는 ID면 기존 목록 유지', async () => {
    const updated = { id: 'ghost', name: '유령' }
    projectApi.getAll.mockResolvedValue({ data: [proj1] })
    projectApi.update.mockResolvedValue({ data: updated })

    const store = useProjectStore()
    await store.fetchAll()
    await store.update('ghost', { name: '유령' })

    // 기존 목록 변경 없음
    expect(store.projects).toHaveLength(1)
    expect(store.projects[0]).toEqual(proj1)
  })

  // ── delete ────────────────────────────────────────────────────────────────────

  it('delete — 프로젝트를 목록에서 제거', async () => {
    projectApi.getAll.mockResolvedValue({ data: [proj1, proj2] })
    projectApi.delete.mockResolvedValue({})

    const store = useProjectStore()
    await store.fetchAll()
    await store.delete('p1')

    expect(store.projects.find(p => p.id === 'p1')).toBeUndefined()
    expect(store.projects).toHaveLength(1)
    expect(store.projects[0]).toEqual(proj2)
  })

  it('delete — 마지막 프로젝트 삭제 시 빈 목록', async () => {
    projectApi.getAll.mockResolvedValue({ data: [proj1] })
    projectApi.delete.mockResolvedValue({})

    const store = useProjectStore()
    await store.fetchAll()
    await store.delete('p1')

    expect(store.projects).toEqual([])
  })
})
