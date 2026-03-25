import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { createSseStream } from '../../api/http'

describe('createSseStream', () => {
  beforeEach(() => {
    // localStorage mock
    vi.spyOn(Storage.prototype, 'getItem').mockReturnValue('test-token')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AbortController를 반환한다', () => {
    global.fetch = vi.fn().mockResolvedValue({
      body: {
        getReader: () => ({
          read: vi.fn().mockResolvedValue({ done: true })
        })
      }
    })

    const ctrl = createSseStream('/api/test', {}, {})
    expect(ctrl).toBeInstanceOf(AbortController)
  })

  it('token 이벤트를 핸들러로 전달한다', async () => {
    const onToken = vi.fn()
    const sseData = 'event: token\ndata: "hello"\n\n'

    let readCount = 0
    global.fetch = vi.fn().mockResolvedValue({
      body: {
        getReader: () => ({
          read: vi.fn().mockImplementation(() => {
            if (readCount++ === 0) {
              return Promise.resolve({
                done: false,
                value: new TextEncoder().encode(sseData)
              })
            }
            return Promise.resolve({ done: true })
          })
        })
      }
    })

    createSseStream('/api/test', {}, { token: onToken })

    // 비동기 처리 대기
    await new Promise(r => setTimeout(r, 50))

    expect(onToken).toHaveBeenCalledWith('hello')
  })

  it('AbortError는 onError를 호출하지 않는다', async () => {
    const onError = vi.fn()
    const abortErr = new DOMException('aborted', 'AbortError')
    global.fetch = vi.fn().mockRejectedValue(abortErr)

    createSseStream('/api/test', {}, { error: onError })
    await new Promise(r => setTimeout(r, 50))

    expect(onError).not.toHaveBeenCalled()
  })

  it('네트워크 오류는 onError를 호출한다', async () => {
    const onError = vi.fn()
    const networkErr = new Error('network error')
    global.fetch = vi.fn().mockRejectedValue(networkErr)

    createSseStream('/api/test', {}, { error: onError })
    await new Promise(r => setTimeout(r, 50))

    expect(onError).toHaveBeenCalledWith(networkErr)
  })
})
