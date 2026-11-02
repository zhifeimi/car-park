import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { h } from 'vue'
import { PlateKeyboard } from '@car-park/ui'

function mountKeyboard(plate = '') {
  return mount(PlateKeyboard, {
    props: { show: true, plate },
    global: {
      stubs: {
        'van-popup': { render() { return h('div', this.$slots.default?.()) } },
        'van-icon': true,
      },
    },
  })
}

describe('PlateKeyboard', () => {
  it('follows empty/non-empty plates and preserves manual override within a state', async () => {
    const wrapper = mountKeyboard()
    expect(wrapper.find('.kb-grid').exists()).toBe(true)
    expect(wrapper.findAll('.kb-key-province')).toHaveLength(31)

    await wrapper.setProps({ plate: '粤' })
    expect(wrapper.find('.kb-grid').exists()).toBe(false)

    await wrapper.find('.kb-key-switch').trigger('click')
    expect(wrapper.find('.kb-grid').exists()).toBe(true)
    await wrapper.setProps({ plate: '粤A' })
    expect(wrapper.find('.kb-grid').exists()).toBe(true)

    await wrapper.find('.kb-key-switch').trigger('click')
    expect(wrapper.find('.kb-grid').exists()).toBe(false)
    await wrapper.setProps({ plate: '' })
    expect(wrapper.find('.kb-grid').exists()).toBe(true)
    wrapper.unmount()
  })

  it('opens on letters and digits for an existing plate', () => {
    const wrapper = mountKeyboard('粤A')
    expect(wrapper.find('.kb-grid').exists()).toBe(false)
    expect(wrapper.find('.kb-key-switch').text()).toBe('省')
    wrapper.unmount()
  })
})
