import { describe, expect, it } from 'vitest'
import { relativeTime } from './time'

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
