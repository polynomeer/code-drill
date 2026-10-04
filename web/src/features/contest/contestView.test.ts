import { describe, expect, it } from 'vitest'
import { clock, countdownOf, remaining, sectionOf, signed } from './contestView'
import type { ContestSummary } from '../../shared/types'

const contest = (status: ContestSummary['status']): ContestSummary => ({
  id: 'c',
  kind: 'CONTEST',
  title: '주간 대회',
  status,
  startsAt: '2026-10-04T10:00:00Z',
  endsAt: '2026-10-04T12:00:00Z',
  minutes: 120,
  joined: false,
  entrants: 3,
  problemCount: 4,
  rated: true,
  ratedAt: null,
})

describe('대회 규칙', () => {
  it('로비 구획은 서버 상태로 나눈다 — 대기·준비도 예정이다', () => {
    expect(sectionOf(contest('RUNNING'))).toBe('running')
    expect(sectionOf(contest('WAITING'))).toBe('upcoming')
    expect(sectionOf(contest('DRAFT'))).toBe('upcoming')
    expect(sectionOf(contest('FINISHED'))).toBe('finished')
  })

  it('카운트다운은 진행 중이면 끝까지, 예정이면 시작까지', () => {
    expect(countdownOf(contest('RUNNING'))).toEqual({ label: '끝까지', target: '2026-10-04T12:00:00Z' })
    expect(countdownOf(contest('SCHEDULED'))).toEqual({ label: '시작까지', target: '2026-10-04T10:00:00Z' })
    expect(countdownOf(contest('FINISHED'))).toBeNull()
  })

  it('남은 시간은 하루가 넘으면 일·시간, 지나면 0 에 머문다', () => {
    const now = Date.parse('2026-10-04T10:58:57Z')
    expect(remaining('2026-10-04T12:00:00Z', now)).toBe('1:01:03')
    expect(remaining('2026-10-06T13:00:00Z', now)).toBe('2일 2시간')
    expect(remaining('2026-10-04T10:00:00Z', now)).toBe('0:00:00')
    expect(clock(3930)).toBe('1:05:30')
    expect(signed(12)).toBe('+12')
    expect(signed(-8)).toBe('-8')
  })
})
