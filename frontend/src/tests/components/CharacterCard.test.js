import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import CharacterCard from '../../components/CharacterCard.vue'

const baseCharacter = {
  id: 'char-1',
  name: '소울',
  relationshipId: 'bestfriend',
  favorite: false,
  lastMessage: '안녕!'
}

describe('CharacterCard', () => {
  // ── 렌더링 ──────────────────────────────────────────────────────────────────

  it('캐릭터 이름을 표시한다', () => {
    const wrapper = mount(CharacterCard, { props: { character: baseCharacter } })
    expect(wrapper.text()).toContain('소울')
  })

  it('lastMessage가 있으면 표시한다', () => {
    const wrapper = mount(CharacterCard, { props: { character: baseCharacter } })
    expect(wrapper.text()).toContain('안녕!')
  })

  it('lastMessage가 없으면 기본 안내문을 표시한다', () => {
    const wrapper = mount(CharacterCard, {
      props: { character: { ...baseCharacter, lastMessage: null } }
    })
    expect(wrapper.text()).toContain('대화를 시작해보세요!')
  })

  it('favorite=true이면 ⭐ 배지를 표시한다', () => {
    const wrapper = mount(CharacterCard, {
      props: { character: { ...baseCharacter, favorite: true } }
    })
    expect(wrapper.text()).toContain('⭐')
  })

  it('favorite=false이면 ⭐ 배지가 없다', () => {
    const wrapper = mount(CharacterCard, { props: { character: baseCharacter } })
    expect(wrapper.find('.fav-badge').exists()).toBe(false)
  })

  it('relationshipId에 따라 한글 레이블을 표시한다', () => {
    const wrapper = mount(CharacterCard, { props: { character: baseCharacter } })
    expect(wrapper.text()).toContain('베프')
  })

  it('알 수 없는 relationshipId이면 기본 이모지 👤를 사용한다', () => {
    const wrapper = mount(CharacterCard, {
      props: { character: { ...baseCharacter, relationshipId: 'unknown' } }
    })
    expect(wrapper.find('.char-avatar').text()).toContain('👤')
  })

  // ── 이벤트 ──────────────────────────────────────────────────────────────────

  it('카드 클릭 시 click 이벤트를 발생시킨다', async () => {
    const wrapper = mount(CharacterCard, { props: { character: baseCharacter } })
    await wrapper.find('.char-card').trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('즐겨찾기 버튼 클릭 시 favorite 이벤트를 발생시킨다', async () => {
    const wrapper = mount(CharacterCard, { props: { character: baseCharacter } })
    await wrapper.find('.action-btn').trigger('click')
    expect(wrapper.emitted('favorite')).toHaveLength(1)
  })

  it('편집 버튼 클릭 시 edit 이벤트를 발생시킨다', async () => {
    const wrapper = mount(CharacterCard, { props: { character: baseCharacter } })
    const buttons = wrapper.findAll('.action-btn')
    await buttons[1].trigger('click')
    expect(wrapper.emitted('edit')).toHaveLength(1)
  })

  it('삭제 버튼 클릭 시 delete 이벤트를 발생시킨다', async () => {
    const wrapper = mount(CharacterCard, { props: { character: baseCharacter } })
    const buttons = wrapper.findAll('.action-btn')
    await buttons[2].trigger('click')
    expect(wrapper.emitted('delete')).toHaveLength(1)
  })

  it('액션 버튼 클릭이 카드 click 이벤트를 버블링시키지 않는다', async () => {
    const wrapper = mount(CharacterCard, { props: { character: baseCharacter } })
    await wrapper.find('.char-actions').trigger('click')
    // char-actions은 @click.stop이므로 click 이벤트 없음
    expect(wrapper.emitted('click')).toBeUndefined()
  })
})
