import { describe, expect, it } from 'vitest'
import type { CaseResult, GroupResult, Verdict } from '../../shared/types'
import { MAX_CELLS, caseRows, pendingRows } from './caseGridModel'

const measurements = { wallTimeMillis: 1, peakMemoryBytes: 1 }
const kase = (caseId: string, verdict: Verdict): CaseResult => ({ caseId, groupId: 'sample', verdict, measurements, message: null })
const group = (groupId: string, verdict: Verdict, cases: CaseResult[], score = 0, maxScore = 0): GroupResult => ({ groupId, verdict, score, maxScore, cases })

describe('caseRows', () => {
  it('공개 그룹은 케이스마다 한 칸, 처음 떨어진 칸 하나에만 표시한다', () => {
    const row = caseRows([group('sample', 'WRONG_ANSWER', [kase('01', 'ACCEPTED'), kase('02', 'WRONG_ANSWER'), kase('03', 'TIME_LIMIT')])], undefined)[0]!
    expect(row.cells.map((cell) => cell.state)).toEqual(['passed', 'failed', 'limit'])
    expect(row.cells.map((cell) => !!cell.first)).toEqual([false, true, false])
    expect(row.label).toBe('공개 케이스 3개 중 1개 통과')
  })

  it('첫 실패 표시는 그룹을 넘어 한 번뿐이다', () => {
    const rows = caseRows(
      [group('a', 'WRONG_ANSWER', [kase('01', 'WRONG_ANSWER')]), group('b', 'WRONG_ANSWER', [kase('02', 'WRONG_ANSWER')])],
      undefined,
    )
    expect(rows.flatMap((row) => row.cells).filter((cell) => cell.first)).toHaveLength(1)
  })

  it('숨은 그룹은 케이스 수만큼 그리되 어느 케이스에서 떨어졌는지는 그리지 않는다', () => {
    const info = [{ id: 'hidden', weight: 100, aggregation: 'ALL_OR_NOTHING' as const, caseCount: 4 }]
    const row = caseRows([group('hidden', 'WRONG_ANSWER', [], 0, 100)], info)[0]!
    expect(row.hidden).toBe(true)
    expect(row.cells.map((cell) => cell.state)).toEqual(['failed', 'failed', 'failed', 'failed'])
    expect(row.cells.some((cell) => cell.first || cell.caseId)).toBe(false)
    expect(row.label).toBe('숨은 케이스 4개 — 통과하지 못함')
  })

  it('숨은 부분 점수는 점수가 말한 만큼만 칠한다', () => {
    const info = [{ id: 'hidden', weight: 100, aggregation: 'SUM' as const, caseCount: 10 }]
    const row = caseRows([group('hidden', 'WRONG_ANSWER', [], 35, 100)], info)[0]!
    expect(row.cells.filter((cell) => cell.state === 'passed')).toHaveLength(3)
    expect(row.label).toBe('숨은 케이스 10개 — 점수 35 / 100')
  })

  it('케이스 수를 모르면 숨은 그룹은 한 칸이다', () => {
    const row = caseRows([group('hidden', 'ACCEPTED', [], 100, 100)], undefined)[0]!
    expect(row.cells).toHaveLength(1)
    expect(row.label).toBe('숨은 케이스 모두 통과')
  })

  it('칸이 많으면 접되 첫 실패 칸은 남긴다', () => {
    const cases = Array.from({ length: MAX_CELLS + 10 }, (_, index) => kase(String(index), index === MAX_CELLS + 5 ? 'WRONG_ANSWER' : 'ACCEPTED'))
    const row = caseRows([group('sample', 'WRONG_ANSWER', cases)], undefined)[0]!
    expect(row.cells).toHaveLength(MAX_CELLS)
    expect(row.folded).toBe(10)
    expect(row.cells.at(-1)?.first).toBe(true)
  })
})

describe('pendingRows', () => {
  it('그룹 정의의 케이스 수만큼 빈 칸', () => {
    const row = pendingRows([{ id: 'g1', weight: 50, aggregation: 'SUM', caseCount: 3 }])[0]!
    expect(row.cells.map((cell) => cell.state)).toEqual(['pending', 'pending', 'pending'])
  })
})
