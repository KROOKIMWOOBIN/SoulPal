import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import MessageBubble from '../../components/MessageBubble.vue'

const userMessage = {
  id: 'msg-1',
  content: '안녕하세요!',
  user: true,
  streaming: false,
  createdAt: '2024-06-01T14:30:00'
}

const aiMessage = {
  id: 'msg-2',
  content: 'AI 응답입니다.',
  user: false,
  streaming: false,
  createdAt: '2024-06-01T14:30:01'
}

describe('MessageBubble', () => {
  // ── 사용자 메시지 ─────────────────────────────────────────────────────────────

  it('사용자 메시지 내용을 렌더링한다', () => {
    const wrapper = mount(MessageBubble, { props: { message: userMessage } })
    expect(wrapper.text()).toContain('안녕하세요!')
  })

  it('사용자 메시지에 bubble-user 클래스가 붙는다', () => {
    const wrapper = mount(MessageBubble, { props: { message: userMessage } })
    expect(wrapper.find('.bubble').classes()).toContain('bubble-user')
  })

  it('사용자 메시지에 user 정렬 클래스가 붙는다', () => {
    const wrapper = mount(MessageBubble, { props: { message: userMessage } })
    expect(wrapper.find('.bubble-wrap').classes()).toContain('user')
  })

  it('사용자 메시지에는 AI 아바타가 없다', () => {
    const wrapper = mount(MessageBubble, { props: { message: userMessage } })
    expect(wrapper.find('.ai-avatar').exists()).toBe(false)
  })

  // ── AI 메시지 ─────────────────────────────────────────────────────────────────

  it('AI 메시지 내용을 렌더링한다', () => {
    const wrapper = mount(MessageBubble, { props: { message: aiMessage } })
    expect(wrapper.text()).toContain('AI 응답입니다.')
  })

  it('AI 메시지에 bubble-ai 클래스가 붙는다', () => {
    const wrapper = mount(MessageBubble, { props: { message: aiMessage } })
    expect(wrapper.find('.bubble').classes()).toContain('bubble-ai')
  })

  it('AI 메시지에 ai 정렬 클래스가 붙는다', () => {
    const wrapper = mount(MessageBubble, { props: { message: aiMessage } })
    expect(wrapper.find('.bubble-wrap').classes()).toContain('ai')
  })

  it('AI 메시지에 아바타 이모지가 표시된다', () => {
    const wrapper = mount(MessageBubble, { props: { message: aiMessage } })
    expect(wrapper.find('.ai-avatar').exists()).toBe(true)
    expect(wrapper.find('.ai-avatar').text()).toBe('🤖')
  })

  // ── 스트리밍 상태 ─────────────────────────────────────────────────────────────

  it('streaming=true이면 커서(▌)가 표시된다', () => {
    const streaming = { ...aiMessage, streaming: true }
    const wrapper = mount(MessageBubble, { props: { message: streaming } })
    expect(wrapper.find('.cursor').exists()).toBe(true)
    expect(wrapper.find('.cursor').text()).toBe('▌')
  })

  it('streaming=false이면 커서가 없다', () => {
    const wrapper = mount(MessageBubble, { props: { message: aiMessage } })
    expect(wrapper.find('.cursor').exists()).toBe(false)
  })

  it('streaming=true이면 시간이 표시되지 않는다', () => {
    const streaming = { ...aiMessage, streaming: true }
    const wrapper = mount(MessageBubble, { props: { message: streaming } })
    expect(wrapper.find('.bubble-time').exists()).toBe(false)
  })

  it('streaming=false이면 시간이 표시된다', () => {
    const wrapper = mount(MessageBubble, { props: { message: aiMessage } })
    expect(wrapper.find('.bubble-time').exists()).toBe(true)
  })

  it('streaming=true이면 bubble에 streaming 클래스가 붙는다', () => {
    const streaming = { ...aiMessage, streaming: true }
    const wrapper = mount(MessageBubble, { props: { message: streaming } })
    expect(wrapper.find('.bubble').classes()).toContain('streaming')
  })

  // ── 시간 포맷 ─────────────────────────────────────────────────────────────────

  it('createdAt이 있으면 시간 포맷을 렌더링한다', () => {
    const wrapper = mount(MessageBubble, { props: { message: userMessage } })
    const timeEl = wrapper.find('.bubble-time')
    expect(timeEl.exists()).toBe(true)
    expect(timeEl.text()).not.toBe('')
  })

  it('createdAt이 null이면 시간 텍스트가 비어있다', () => {
    const msg = { ...userMessage, createdAt: null }
    const wrapper = mount(MessageBubble, { props: { message: msg } })
    const timeEl = wrapper.find('.bubble-time')
    expect(timeEl.text()).toBe('')
  })
})
