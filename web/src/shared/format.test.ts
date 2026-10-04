import { describe, expect, it } from 'vitest'
import { count, date, monthDay, relativeTime, yearMonth } from './format'

describe('relativeTime', () => {
  const now = new Date('2026-10-03T12:00:00Z')
  const ago = (seconds: number) => new Date(now.getTime() - seconds * 1000).toISOString()

  it('가까우면 상대 시간', () => {
    expect(relativeTime(ago(10), now)).toBe('방금')
    expect(relativeTime(ago(5 * 60), now)).toBe('5분 전')
    expect(relativeTime(ago(3 * 3600), now)).toBe('3시간 전')
    expect(relativeTime(ago(2 * 86_400), now)).toBe('그저께')
  })

  it('일주일이 넘으면 날짜', () => {
    expect(relativeTime(ago(30 * 86_400), now)).toMatch(/2026/)
  })
})

describe('표기는 브라우저 언어가 아니라 한 로캘을 따른다', () => {
  // 정오(UTC) — 어느 시간대에서 돌려도 같은 날이다
  const noon = '2026-10-04T12:00:00Z'

  it('날짜', () => {
    expect(date(noon)).toBe('2026년 10월 4일')
    expect(monthDay(noon)).toBe('10월 4일')
    expect(yearMonth(noon)).toBe('2026년 10월')
  })

  it('수는 천 단위로 끊는다', () => {
    expect(count(1234567)).toBe('1,234,567')
  })
})
