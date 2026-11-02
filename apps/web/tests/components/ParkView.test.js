import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus, { ElMessage } from 'element-plus'
import ParkView from '@/views/park/index.vue'
import { addPark, deletePark, exportParks, listPark, updatePark } from '@/api/parking'

vi.mock('@/api/parking', () => ({
  listPark: vi.fn(),
  addPark: vi.fn(),
  updatePark: vi.fn(),
  deletePark: vi.fn(),
  exportParks: vi.fn(),
}))
vi.spyOn(ElMessage, 'success').mockImplementation(() => {})

const legacyPark = {
  id: 7, name: 'Legacy lot', country: null, lotNum: 12, leftLotNum: 8,
  openDateBegin: null, openDateEnd: null, openTime: null, closeTime: null,
  remark: '', createBy: 'admin', createTime: '2026-11-02 00:00:00',
  updateBy: 'admin', updateTime: '2026-11-02 00:00:00',
}

let wrapper

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  document.body.replaceChildren()
  listPark.mockResolvedValue({ rows: [legacyPark], total: 1 })
  updatePark.mockResolvedValue(undefined)
  addPark.mockResolvedValue(undefined)
  deletePark.mockResolvedValue(undefined)
  exportParks.mockResolvedValue(new Blob())
})

afterEach(() => {
  wrapper?.unmount()
  document.body.replaceChildren()
})

async function mountParkView(errorHandler) {
  wrapper = mount(ParkView, {
    attachTo: document.body,
    global: { plugins: [ElementPlus], config: errorHandler ? { errorHandler } : {} },
  })
  await flushPromises()
}

async function clickButton(text) {
  const button = wrapper.findAllComponents({ name: 'ElButton' }).find((node) => node.text() === text)
  expect(button, `${text} button`).toBeDefined()
  await button.trigger('click')
  await flushPromises()
}

function countrySelect() {
  const selects = wrapper.findAllComponents({ name: 'ElSelect' })
  return selects[selects.length - 1]
}

describe('park admin country selection', () => {
  it('leaves a legacy country unselected and submits only the explicit choice', async () => {
    await mountParkView()
    await clickButton('Edit')

    const select = countrySelect()
    expect(select.props('modelValue')).toBe('')
    await select.setValue('HK')
    await clickButton('Confirm')

    expect(updatePark).toHaveBeenCalledWith({
      id: 7, name: 'Legacy lot', country: 'HK', lotNum: 12,
      openDateBegin: null, openDateEnd: null, openTime: null, closeTime: null, remark: '',
    })
  })

  it('blocks saving a legacy lot until a country is selected', async () => {
    // Element Plus rejects invalid validate() calls; capture only that expected field error.
    const validationError = vi.fn()
    await mountParkView(validationError)
    await clickButton('Edit')
    await clickButton('Confirm')

    expect(updatePark).not.toHaveBeenCalled()
    await vi.waitFor(() => {
      expect(document.querySelector('.el-form-item__error')?.textContent).toBe('Country or region is required')
    })
    expect(validationError).toHaveBeenCalledTimes(1)
    expect(validationError.mock.calls[0][0]).toHaveProperty('country')
  })

  it('starts a new lot with no country selected', async () => {
    await mountParkView()
    await clickButton('Add')
    expect(countrySelect().props('modelValue')).toBe('')
  })
})
