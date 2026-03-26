import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import OptionCard from '../../components/OptionCard.vue'

const item = { emoji: '💕', labelKo: '베프' }

describe('OptionCard', () => {
  // ── 렌더링 ──────────────────────────────────────────────────────────────────

  it('이모지와 라벨을 렌더링한다', () => {
    const wrapper = mount(OptionCard, { props: { item, selected: false } })
    expect(wrapper.find('.option-emoji').text()).toBe('💕')
    expect(wrapper.find('.option-label').text()).toBe('베프')
  })

  it('selected=true이면 selected 클래스가 붙는다', () => {
    const wrapper = mount(OptionCard, { props: { item, selected: true } })
    expect(wrapper.find('.option-card').classes()).toContain('selected')
  })

  it('selected=false이면 selected 클래스가 없다', () => {
    const wrapper = mount(OptionCard, { props: { item, selected: false } })
    expect(wrapper.find('.option-card').classes()).not.toContain('selected')
  })

  // ── 이벤트 ──────────────────────────────────────────────────────────────────

  it('클릭 시 click 이벤트를 발생시킨다', async () => {
    const wrapper = mount(OptionCard, { props: { item, selected: false } })
    await wrapper.find('.option-card').trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('button 태그로 렌더링된다', () => {
    const wrapper = mount(OptionCard, { props: { item, selected: false } })
    expect(wrapper.element.tagName.toLowerCase()).toBe('button')
  })

  // ── 다양한 아이템 ─────────────────────────────────────────────────────────────

  it('다른 아이템도 올바르게 렌더링한다', () => {
    const mentorItem = { emoji: '🌟', labelKo: '멘토' }
    const wrapper = mount(OptionCard, { props: { item: mentorItem, selected: false } })
    expect(wrapper.find('.option-emoji').text()).toBe('🌟')
    expect(wrapper.find('.option-label').text()).toBe('멘토')
  })
})
