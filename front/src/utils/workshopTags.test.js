import { describe, it, expect, vi, afterEach } from 'vitest'
import { computeWorkshopTags } from './workshopTags'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('computeWorkshopTags', () => {
  it('собирает теги цехов по операциям позиций', () => {
    vi.spyOn(console, 'log').mockImplementation(() => {})
    const workshops = [
      { id: 1, name: 'Цех печати', operationIds: [10, 11] },
      { id: 2, name: 'Цех резки', operationIds: [20] }
    ]
    const items = [
      { operations: [{ id: 10 }, { id: 20 }] },
      { operations: [{ id: 11 }] }
    ]
    const result = computeWorkshopTags(workshops, items)
    expect(result).toContain('#Цех печати')
    expect(result).toContain('#Цех резки')
  })

  it('без совпадений — пустая строка', () => {
    vi.spyOn(console, 'log').mockImplementation(() => {})
    const result = computeWorkshopTags(
      [{ id: 1, name: 'X', operationIds: [1] }],
      [{ operations: [{ id: 99 }] }]
    )
    expect(result).toBe('')
  })

  it('устойчиво к пустым входам', () => {
    vi.spyOn(console, 'log').mockImplementation(() => {})
    expect(computeWorkshopTags([], [])).toBe('')
    expect(
      computeWorkshopTags([{ id: 1, name: 'X', operationIds: [1] }], [{}])
    ).toBe('')
  })

  it('не дублирует один и тот же цех', () => {
    vi.spyOn(console, 'log').mockImplementation(() => {})
    const workshops = [{ id: 1, name: 'Цех', operationIds: [1, 2] }]
    const items = [{ operations: [{ id: 1 }, { id: 2 }] }]
    const result = computeWorkshopTags(workshops, items)
    expect(result.match(/#Цех/g)).toHaveLength(1)
  })
})
