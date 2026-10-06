import { describe, expect, it } from 'vitest'
import type { PrescribedProblem } from '../../shared/types'
import { attendance, skillTiles, splitQuests } from './questView'

const item = (problemId: string): PrescribedProblem => ({ problemId, reason: 'NEXT_ON_PATH', detail: '', competency: null, nextMeasurement: '2026-10-07' })

describe('splitQuests', () => {
  it('처방의 첫 칸이 메인, 나머지가 사이드 — 순서를 바꾸지 않는다', () => {
    const quests = splitQuests([item('a'), item('b'), item('c')])
    expect(quests.main?.problemId).toBe('a')
    expect(quests.side.map((quest) => quest.problemId)).toEqual(['b', 'c'])
  })

  it('처방이 비면 메인도 없다', () => {
    expect(splitQuests([])).toEqual({ main: null, side: [] })
  })
})

describe('skillTiles', () => {
  it('잰 역량이 없는 역량군은 잠긴 칸이고 별을 그리지 않는다', () => {
    const [tile] = skillTiles([{ group: 'DESIGN', total: 3, measured: 0, levels: {}, needsMoreEvidence: 0 }])
    expect(tile).toMatchObject({ locked: true, stars: [] })
  })

  it('별은 높은 수준부터, 없는 수준은 뺀다', () => {
    const [tile] = skillTiles([{ group: 'EXECUTION', total: 4, measured: 3, levels: { DEVELOPING: 2, STRONG: 1 }, needsMoreEvidence: 1 }])
    expect(tile?.stars).toEqual([
      { level: 'STRONG', count: 1 },
      { level: 'DEVELOPING', count: 2 },
    ])
    expect(tile?.locked).toBe(false)
  })
})

describe('attendance', () => {
  it('0~7 로 자른다', () => {
    expect(attendance(9)).toEqual({ days: 7, of: 7 })
    expect(attendance(-1)).toEqual({ days: 0, of: 7 })
  })
})
