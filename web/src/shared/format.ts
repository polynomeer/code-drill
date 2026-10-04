/**
 * 표기 (디자인 설계서 §9.1 상대 시간 + 상세 툴팁, §15.4 locale formatter).
 *
 * 날짜·시각·수는 **여기서만** 문자열이 된다. 화면마다 `toLocaleString()` 을 부르면 브라우저 언어를
 * 따라가 같은 화면이 사람마다 다른 모양이 되고(영어 브라우저에서 "10/4/2026, 3:00 PM"), 형식을 바꾸려면
 * 화면을 하나씩 찾아다녀야 한다. 로캘은 [LOCALE] 하나다 — 다국어가 오면 여기를 고친다.
 *
 * 시간대는 브라우저의 것이다. 서버가 날짜로 자르는 것(처방·스트릭·히트맵)은 서울 기준으로 정해 오고,
 * 여기서는 시각을 사람에게 보일 뿐이다.
 */
export const LOCALE = 'ko-KR'

const RELATIVE = new Intl.RelativeTimeFormat(LOCALE, { numeric: 'auto' })
const DATE = new Intl.DateTimeFormat(LOCALE, { year: 'numeric', month: 'short', day: 'numeric' })
const FULL = new Intl.DateTimeFormat(LOCALE, { dateStyle: 'medium', timeStyle: 'short' })
const MONTH_DAY = new Intl.DateTimeFormat(LOCALE, { month: 'long', day: 'numeric' })
const YEAR_MONTH = new Intl.DateTimeFormat(LOCALE, { year: 'numeric', month: 'long' })
const NUMBER = new Intl.NumberFormat(LOCALE)

/**
 * 한 시간 안은 "n분 전", 하루 안은 "n시간 전", 일주일 안은 "n일 전", 그보다 오래면 날짜다.
 * 몇 주 전을 "23일 전"이라고 하면 사람은 다시 날짜를 계산한다.
 */
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

/** 툴팁·표의 정확한 시각. "2026. 10. 4. 오후 3:00" */
export function fullTime(iso: string): string {
  return FULL.format(new Date(iso))
}

/** "2026년 10월 4일" */
export function date(iso: string): string {
  return DATE.format(new Date(iso))
}

/** "10월 4일" — 올해 안의 가까운 날 (다음 측정, 재발) */
export function monthDay(iso: string): string {
  return MONTH_DAY.format(new Date(iso))
}

/** "2026년 1월" — 가입 시기처럼 날까지는 필요 없는 것 */
export function yearMonth(iso: string): string {
  return YEAR_MONTH.format(new Date(iso))
}

/** "1,234" */
export function count(value: number): string {
  return NUMBER.format(value)
}
