import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ChatInput from '../../components/ChatInput.vue'

describe('ChatInput', () => {
  // ── 기본 렌더링 ───────────────────────────────────────────────────────────────

  it('textarea와 전송 버튼을 렌더링한다', () => {
    const wrapper = mount(ChatInput)
    expect(wrapper.find('textarea').exists()).toBe(true)
    expect(wrapper.find('.send-btn').exists()).toBe(true)
  })

  it('disabled=false이면 textarea가 활성화된다', () => {
    const wrapper = mount(ChatInput, { props: { disabled: false } })
    expect(wrapper.find('textarea').attributes('disabled')).toBeUndefined()
  })

  it('disabled=true이면 textarea가 비활성화된다', () => {
    const wrapper = mount(ChatInput, { props: { disabled: true } })
    expect(wrapper.find('textarea').element.disabled).toBe(true)
  })

  it('disabled=true이면 전송 버튼도 비활성화된다', () => {
    const wrapper = mount(ChatInput, { props: { disabled: true } })
    expect(wrapper.find('.send-btn').element.disabled).toBe(true)
  })

  // ── 전송 이벤트 ───────────────────────────────────────────────────────────────

  it('전송 버튼 클릭 시 입력 텍스트로 send 이벤트를 발생시킨다', async () => {
    const wrapper = mount(ChatInput)
    await wrapper.find('textarea').setValue('안녕하세요')
    await wrapper.find('.send-btn').trigger('click')
    expect(wrapper.emitted('send')).toBeTruthy()
    expect(wrapper.emitted('send')[0]).toEqual(['안녕하세요'])
  })

  it('Enter 키 입력 시 send 이벤트를 발생시킨다', async () => {
    const wrapper = mount(ChatInput)
    await wrapper.find('textarea').setValue('테스트 메시지')
    await wrapper.find('textarea').trigger('keydown.enter')
    expect(wrapper.emitted('send')).toBeTruthy()
    expect(wrapper.emitted('send')[0]).toEqual(['테스트 메시지'])
  })

  it('전송 후 textarea 내용이 초기화된다', async () => {
    const wrapper = mount(ChatInput)
    await wrapper.find('textarea').setValue('메시지')
    await wrapper.find('.send-btn').trigger('click')
    await nextTick()
    expect(wrapper.find('textarea').element.value).toBe('')
  })

  it('공백만 있는 입력은 send 이벤트를 발생시키지 않는다', async () => {
    const wrapper = mount(ChatInput)
    await wrapper.find('textarea').setValue('   ')
    await wrapper.find('.send-btn').trigger('click')
    expect(wrapper.emitted('send')).toBeUndefined()
  })

  it('텍스트가 없으면 send 이벤트를 발생시키지 않는다', async () => {
    const wrapper = mount(ChatInput)
    await wrapper.find('.send-btn').trigger('click')
    expect(wrapper.emitted('send')).toBeUndefined()
  })

  it('disabled 상태에서 버튼 클릭해도 send 이벤트가 발생하지 않는다', async () => {
    const wrapper = mount(ChatInput, { props: { disabled: true } })
    await wrapper.find('textarea').setValue('메시지')
    await wrapper.find('.send-btn').trigger('click')
    expect(wrapper.emitted('send')).toBeUndefined()
  })

  // ── 전송 버튼 active 상태 ─────────────────────────────────────────────────────

  it('텍스트 입력 시 전송 버튼에 active 클래스가 붙는다', async () => {
    const wrapper = mount(ChatInput)
    await wrapper.find('textarea').setValue('내용')
    expect(wrapper.find('.send-btn').classes()).toContain('active')
  })

  it('텍스트가 없으면 전송 버튼에 active 클래스가 없다', () => {
    const wrapper = mount(ChatInput)
    expect(wrapper.find('.send-btn').classes()).not.toContain('active')
  })
})
