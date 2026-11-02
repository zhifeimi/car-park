import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { h } from 'vue'
import Vant, * as vant from 'vant'
import BookingAdd from '@/views/BookingAdd.vue'
import { addBooking, getLeftLotNum, listParks } from '@/api/parking'
import { setLocale } from '@/locales'

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/api/parking', () => ({
  listParks: vi.fn(),
  getLeftLotNum: vi.fn(),
  addBooking: vi.fn(),
}))

const showFailToast = vi.spyOn(vant, 'showFailToast').mockImplementation(() => {})
vi.spyOn(vant, 'showSuccessToast').mockImplementation(() => {})

function park(id, name, country) {
  return {
    id, name, country, lotNum: 10, leftLotNum: 5,
    openDateBegin: '2026-11-02', openDateEnd: '2027-11-02',
    openTime: '00:00:00', closeTime: '23:59:00', remark: null,
    createBy: 'admin', createTime: '2026-11-02 00:00:00',
    updateBy: 'admin', updateTime: '2026-11-02 00:00:00',
  }
}

let wrapper

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubEnv('VITE_REGION', 'cn')
  localStorage.clear()
  setLocale('en')
  listParks.mockResolvedValue({
    rows: [park(1, 'GB lot', 'GB'), park(2, 'CN lot', 'CN'), park(3, 'Legacy lot', null)],
    today: '2026-11-02',
  })
  getLeftLotNum.mockResolvedValue(5)
  addBooking.mockResolvedValue({})
})

afterEach(() => {
  wrapper?.unmount()
  vi.unstubAllEnvs()
})

async function mountBooking({ realKeyboard = false } = {}) {
  const keyboardStubs = realKeyboard
    ? { 'van-popup': { render() { return h('div', this.$slots.default?.()) } } }
    : { PlateKeyboard: true }
  wrapper = mount(BookingAdd, {
    global: { plugins: [Vant], stubs: { LangSwitch: true, ...keyboardStubs } },
  })
  await flushPromises()
}

async function choosePark(name) {
  const radio = wrapper.findAll('.van-radio').find((node) => node.text() === name)
  expect(radio, `radio for ${name}`).toBeDefined()
  await radio.trigger('click')
  await flushPromises()
}

async function submit(phone = '13800138000') {
  await wrapper.find('input[type="tel"]').setValue(phone)
  await wrapper.find('.van-checkbox').trigger('click')
  await wrapper.find('.submit-bar button').trigger('click')
  await flushPromises()
}

describe('BookingAdd country-driven plate validation', () => {
  it.each(['cn', 'intl'])('submits a canonical non-CN plate independently of the %s phone region', async (region) => {
    vi.stubEnv('VITE_REGION', region)
    await mountBooking()
    expect(wrapper.find('.plate-field input').attributes('placeholder')).toBe('As shown on the plate')

    await choosePark('GB lot')
    await wrapper.find('.plate-field input').setValue('ab-123')
    await submit(region === 'intl' ? '+44 7700 900123' : '13800138000')

    expect(addBooking).toHaveBeenCalledExactlyOnceWith({
      carPlate: 'AB123', cellPhone: region === 'intl' ? '+447700900123' : '13800138000',
      park: 'GB lot', bookingDate: '2026-11-02',
    })
  })

  it.each(['cn', 'intl'])('keeps strict mainland validation independently of the %s phone region', async (region) => {
    vi.stubEnv('VITE_REGION', region)
    await mountBooking()
    await wrapper.find('.plate-field input').setValue('ABC')
    await choosePark('CN lot')
    expect(wrapper.find('.plate-field input').attributes('readonly')).toBeDefined()

    await submit(region === 'intl' ? '+447700900123' : '13800138000')

    expect(addBooking).not.toHaveBeenCalled()
    expect(showFailToast).toHaveBeenCalledWith('Invalid license plate format')
  })

  it.each([undefined, null, '', 'ZZ', 'CHN'])('blocks a selected lot with invalid country %s', async (country) => {
    listParks.mockResolvedValue({ rows: [park(3, 'Legacy lot', country)], today: '2026-11-02' })
    await mountBooking()
    await choosePark('Legacy lot')
    await wrapper.find('.plate-field input').setValue('ABC')
    await submit()

    expect(addBooking).not.toHaveBeenCalled()
    expect(showFailToast).toHaveBeenCalledWith('Parking lot country/region is not configured; please contact staff')
  })

  it('submits a CN plate entered through the real keyboard', async () => {
    await mountBooking({ realKeyboard: true })
    await choosePark('CN lot')
    await wrapper.find('.plate-field input').trigger('click')

    for (const key of ['粤', 'A', '1', '2', '3', '4', '5']) {
      const button = wrapper.findAll('.kb-key').find((node) => node.text() === key)
      expect(button, `keyboard key ${key}`).toBeDefined()
      await button.trigger('click')
      if (key === '粤') expect(wrapper.find('.kb-grid').exists()).toBe(false)
    }
    expect(wrapper.find('.plate-field input').element.value).toBe('粤A12345')

    await submit()

    expect(addBooking).toHaveBeenCalledExactlyOnceWith({
      carPlate: '粤A12345', cellPhone: '13800138000',
      park: 'CN lot', bookingDate: '2026-11-02',
    })
  })

  it('keeps the plate and removes the keyboard when leaving a CN lot', async () => {
    await mountBooking()
    await wrapper.find('.plate-field input').setValue('粤A12345')
    await choosePark('CN lot')
    await wrapper.find('.plate-field input').trigger('click')
    expect(wrapper.findComponent({ name: 'PlateKeyboard' }).props('show')).toBe(true)

    await choosePark('GB lot')

    expect(wrapper.findComponent({ name: 'PlateKeyboard' }).exists()).toBe(false)
    expect(wrapper.find('.plate-field input').attributes('readonly')).toBeUndefined()
    expect(wrapper.find('.plate-field input').element.value).toBe('粤A12345')

    await choosePark('CN lot')
    expect(wrapper.findComponent({ name: 'PlateKeyboard' }).props('show')).toBe(false)
  })
})
