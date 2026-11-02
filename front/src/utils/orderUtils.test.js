import { describe, it, expect } from 'vitest'
import { getStatusColor, getStatusLabel, isM2, isLinearMeter, isPiece } from './orderUtils'

describe('getStatusColor', () => {
  it('возвращает цвета известных статусов', () => {
    expect(getStatusColor('DRAFT')).toBe('default')
    expect(getStatusColor('IN_PROGRESS')).toBe('primary')
    expect(getStatusColor('READY')).toBe('success')
    expect(getStatusColor('REJECTED')).toBe('warning')
    expect(getStatusColor('CLOSED')).toBe('error')
  })

  it('для неизвестного статуса — default', () => {
    expect(getStatusColor('UNKNOWN')).toBe('default')
    expect(getStatusColor(undefined)).toBe('default')
  })
})

describe('getStatusLabel', () => {
  it('переводит известные статусы', () => {
    expect(getStatusLabel('DRAFT')).toBe('Черновик')
    expect(getStatusLabel('IN_PROGRESS')).toBe('В работе')
    expect(getStatusLabel('READY')).toBe('Готов')
    expect(getStatusLabel('REJECTED')).toBe('Отклонен')
    expect(getStatusLabel('CLOSED')).toBe('Закрыт')
  })

  it('неизвестный статус возвращается как есть', () => {
    expect(getStatusLabel('SOMETHING')).toBe('SOMETHING')
  })
})

describe('определение единиц измерения', () => {
  it('isM2 распознаёт м²', () => {
    expect(isM2({ unit: 'м2' })).toBe(true)
    expect(isM2({ unit: 'м²' })).toBe(true)
    expect(isM2({ unit: 'M2' })).toBe(true)
    expect(isM2({ unit: ' м.кв. ' })).toBe(false)
    expect(isM2({ unit: 'шт' })).toBe(false)
    expect(isM2(null)).toBe(false)
    expect(isM2({})).toBe(false)
  })

  it('isLinearMeter распознаёт погонные метры', () => {
    expect(isLinearMeter({ unit: 'м.п.' })).toBe(true)
    expect(isLinearMeter({ unit: 'м.п' })).toBe(true)
    expect(isLinearMeter({ unit: 'п.м.' })).toBe(true)
    expect(isLinearMeter({ unit: 'мп' })).toBe(true)
    expect(isLinearMeter({ unit: 'м2' })).toBe(false)
    expect(isLinearMeter(undefined)).toBe(false)
  })

  it('isPiece распознаёт штуки', () => {
    expect(isPiece({ unit: 'шт' })).toBe(true)
    expect(isPiece({ unit: 'шт.' })).toBe(true)
    expect(isPiece({ unit: 'м2' })).toBe(false)
    expect(isPiece({})).toBe(false)
  })
})
