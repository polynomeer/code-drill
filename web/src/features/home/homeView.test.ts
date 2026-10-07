import { describe, expect, it } from 'vitest'
import type { PrescribedProblem } from '../../shared/types'
import { activeWeek, groupTiles, splitToday } from './homeView'

const item = (problemId: string): PrescribedProblem => ({ problemId, reason: 'NEXT_ON_PATH', detail: '', competency: null, nextMeasurement: '2026-10-07' })

describe('splitToday', () => {
  it('처방의 첫 칸을 크게, 나머지는 순서를 바꾸지 않는다', () => {
    const today = splitToday([item('a'), item('b'), item('c')])
    expect(today.first?.problemId).toBe('a')
    expect(today.rest.map((quest) => quest.problemId)).toEqual(['b', 'c'])
  })

  it('처방이 비면 첫 칸도 없다', () => {
    expect(splitToday([])).toEqual({ first: null, rest: [] })
  })
})

describe('groupTiles', () => {
  it('잰 역량이 없는 역량군은 수준을 말하지 않는다', () => {
    const [tile] = groupTiles([{ group: 'DESIGN', total: 3, measured: 0, levels: {}, needsMoreEvidence: 0 }])
    expect(tile).toMatchObject({ unmeasured: true, levels: [] })
  })

  it('수준은 높은 것부터, 없는 수준은 뺀다', () => {
    const [tile] = groupTiles([{ group: 'EXECUTION', total: 4, measured: 3, levels: { DEVELOPING: 2, STRONG: 1 }, needsMoreEvidence: 1 }])
    expect(tile?.levels).toEqual([
      { level: 'STRONG', count: 1 },
      { level: 'DEVELOPING', count: 2 },
    ])
    expect(tile?.unmeasured).toBe(false)
  })
})

describe('activeWeek', () => {
  it('0~7 로 자른다', () => {
    expect(activeWeek(9)).toEqual({ days: 7, of: 7 })
    expect(activeWeek(-1)).toEqual({ days: 0, of: 7 })
  })
})
