/**
 * 시각 표기 (디자인 설계서 §9.1 — 상대 시간 + 상세 툴팁, §15.4 — locale formatter).
 *
 * 한 시간 안은 "n분 전", 하루 안은 "n시간 전", 일주일 안은 "n일 전", 그보다 오래면 날짜다.
 * 몇 주 전을 "23일 전"이라고 하면 사람은 다시 날짜를 계산한다.
 */
const RELATIVE = new Intl.RelativeTimeFormat('ko', { numeric: 'auto' })
const DATE = new Intl.DateTimeFormat('ko', { year: 'numeric', month: 'short', day: 'numeric' })
const FULL = new Intl.DateTimeFormat('ko', { dateStyle: 'medium', timeStyle: 'short' })

export function relativeTime(iso: string, now: Date = new Date()): string {
  const then = new Date(iso)
  const seconds = Math.round((then.getTime() - now.getTime()) / 1000)
  const abs = Math.abs(seconds)
  if (abs < 60) return '방금'
  if (abs < 3600) return RELATIVE.format(Math.round(seconds / 60), 'minute')
  if (abs < 86_400) return RELATIVE.format(Math.round(seconds / 3600), 'hour')
  if (abs < 7 * 86_400) return RELATIVE.format(Math.round(seconds / 86_400), 'day')
  return DATE.format(then)
}

export function fullTime(iso: string): string {
  return FULL.format(new Date(iso))
}
