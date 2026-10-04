import { describe, expect, it } from 'vitest'
import { enoughData, helpLabel, needsMoreEvidence, summarize } from './competencyView'
import type { MasteryView } from '../../shared/types'

describe('역량 지도 표현 규칙', () => {
  const items: MasteryView[] = [
    view('READING', 'UNDERSTANDING', 'DEVELOPING', 'LOW', 2, 1),
    view('CONSTRAINTS', 'UNDERSTANDING', 'PROFICIENT', 'HIGH', 9, 8),
    view('MODELING', 'DESIGN', 'UNMEASURED', 'NONE', 0, 0),
    view('CORRECTNESS', 'DESIGN', 'DEVELOPING', 'MEDIUM', 5, 2),
  ]

  it('역량군은 평균을 내지 않고 수준별로 센다', () => {
    expect(summarize(items)).toEqual([
      { group: 'UNDERSTANDING', total: 2, measured: 2, levels: { DEVELOPING: 1, PROFICIENT: 1 }, needsMoreEvidence: 1 },
      { group: 'DESIGN', total: 2, measured: 1, levels: { DEVELOPING: 1 }, needsMoreEvidence: 0 },
    ])
  })

  it('근거가 적으면 "기르는 중"이어도 약점이 아니라 추가 확인이다', () => {
    expect(needsMoreEvidence(items[0]!)).toBe(true)
    expect(needsMoreEvidence(items[3]!)).toBe(false)
    // 재지 않은 것은 "추가 확인"도 아니다 — 아직 시작하지 않은 것이다
    expect(needsMoreEvidence(items[2]!)).toBe(false)
  })

  it('도움 수준과 데이터 부족을 말한다', () => {
    expect(helpLabel(1)).toBe('도움 없음')
    expect(helpLabel(0.5)).toBe('도움 받음 · 무게 0.5')
    expect(enoughData(4)).toBe(false)
    expect(enoughData(5)).toBe(true)
  })
})

function view(
  competency: string,
  group: string,
  level: MasteryView['level'],
  confidence: MasteryView['confidence'],
  evidenceCount: number,
  successCount: number,
): MasteryView {
  return { competency, group, level, confidence, evidenceCount, successCount }
}
