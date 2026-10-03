import { describe, expect, it } from 'vitest'
import { EMPTY_FILTER } from '../../shared/types'
import { clearFilters, isEmpty, readFilter, writeFilter } from './problemFilter'

describe('problemFilter', () => {
  it('기본값은 주소에 싣지 않는다', () => {
    expect(writeFilter(EMPTY_FILTER)).toBe('')
  })

  it('정렬과 쪽을 주소로 오가도 같다', () => {
    const filter = { ...EMPTY_FILTER, tags: ['dp'], sort: 'ACCURACY' as const, order: 'DESC' as const, page: 3 }
    expect(readFilter(`?${writeFilter(filter)}`)).toEqual(filter)
  })

  it('모르는 정렬·잘못된 쪽은 기본값으로 읽는다', () => {
    const read = readFilter('?sort=POPULARITY&page=-2&order=sideways')
    expect(read.sort).toBe('NUMBER')
    expect(read.page).toBe(1)
    expect(read.order).toBe('ASC')
  })

  it('필터 지우기는 거르는 조건만 지우고 정렬은 남긴다', () => {
    const filter = { ...EMPTY_FILTER, query: 'x', sort: 'SOLVERS' as const, order: 'DESC' as const, page: 2 }
    expect(isEmpty(filter)).toBe(false)
    const cleared = clearFilters(filter)
    expect(isEmpty(cleared)).toBe(true)
    expect(cleared.sort).toBe('SOLVERS')
    expect(cleared.order).toBe('DESC')
    expect(cleared.page).toBe(1)
  })
})
