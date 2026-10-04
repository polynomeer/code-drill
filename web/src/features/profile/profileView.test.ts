import { describe, expect, it } from 'vitest'
import { activitySummary, levelOf, monthLabels, ratingSeries, weeks } from './profileView'

describe('프로필 표현', () => {
  it('히트맵은 고정 문턱이다 — 그 사람의 최대치에 맞춰 늘리지 않는다', () => {
    expect([0, 1, 2, 3, 5, 6, 9, 10, 40].map(levelOf)).toEqual([0, 1, 1, 2, 2, 3, 3, 4, 4])
  })

  it('주 단위 열은 일요일에서 시작하고, 1년 밖의 앞 칸은 비운다', () => {
    // 2026-10-01 은 목요일
    const days = ['2026-10-01', '2026-10-02', '2026-10-03', '2026-10-04'].map((date) => ({ date, submissions: 1 }))
    const columns = weeks(days)
    expect(columns).toHaveLength(2)
    expect(columns[0]!.slice(0, 4)).toEqual([null, null, null, null])
    expect(columns[1]![0]!.date).toBe('2026-10-04')
    expect(monthLabels(columns)).toEqual(['10월', null])
    expect(activitySummary(days)).toEqual({ submissions: 4, activeDays: 4 })
  })

  it('레이팅은 시간순이고 첫 점은 첫 대회 전의 값이다', () => {
    const series = ratingSeries([
      { contestId: 'b', title: '둘째', rank: 2, before: 1520, after: 1490, at: '2026-09-20T00:00:00Z' },
      { contestId: 'a', title: '첫째', rank: 1, before: 1500, after: 1520, at: '2026-09-01T00:00:00Z' },
    ])
    expect(series.map((point) => point.value)).toEqual([1500, 1520, 1490])
  })
})
