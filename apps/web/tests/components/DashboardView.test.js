import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import DashboardView from '@/views/dashboard/index.vue'
import { getStatisticInfo, listBooking, listPark } from '@/api/parking'
import { setLocale } from '@/locales'

vi.mock('@/api/parking', () => ({
  getStatisticInfo: vi.fn(),
  listPark: vi.fn(),
  listBooking: vi.fn(),
}))

let wrapper

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  getStatisticInfo.mockResolvedValue({})
  listPark.mockResolvedValue({ rows: [] })
  listBooking.mockResolvedValue({ rows: [] })
})

afterEach(() => {
  wrapper?.unmount()
})

async function mountDashboard() {
  wrapper = mount(DashboardView, { global: { plugins: [ElementPlus] } })
  await flushPromises()
}

describe('dashboard footer', () => {
  it.each([
    ['en', 'Developed by Zhifei.org'],
    ['zh-CN', '由 Zhifei.org 开发'],
  ])('shows the %s developer attribution with a secure external link', async (locale, attribution) => {
    setLocale(locale)
    await mountDashboard()

    const link = wrapper.find('.site-footer a')
    expect(link.exists()).toBe(true)
    expect(link.text()).toBe(attribution)
    expect(link.attributes('href')).toBe('https://zhifei.org')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
  })
})
