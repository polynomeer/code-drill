import type { PublicProfile } from '../../shared/types'

/**
 * 공개 프로필의 표현 규칙 (docs/ui-overhaul.md §6.7).
 */

export type Day = PublicProfile['activity'][number]

/**
 * 히트맵 칸의 진하기 0~4.
 *
 * **고정 문턱이다.** 그 사람의 가장 바쁜 날에 맞춰 늘리면 하루 두 번 제출한 사람과 스무 번 제출한
 * 사람의 히트맵이 똑같이 진해 보인다 — 프로필끼리 견줄 때 거짓말이 된다.
 */
export function levelOf(submissions: number): 0 | 1 | 2 | 3 | 4 {
  if (submissions <= 0) return 0
  if (submissions <= 2) return 1
  if (submissions <= 5) return 2
  if (submissions <= 9) return 3
  return 4
}

/**
 * 주(열) 단위로 묶는다. 한 열은 일요일부터 토요일. 첫 열의 앞쪽 빈칸은 null — 그 날들은 1년 밖이다.
 */
export function weeks(days: Day[]): (Day | null)[][] {
  if (days.length === 0) return []
  const first = new Date(`${days[0]!.date}T00:00:00Z`).getUTCDay()
  const cells: (Day | null)[] = [...Array<null>(first).fill(null), ...days]
  const columns: (Day | null)[][] = []
  for (let i = 0; i < cells.length; i += 7) columns.push(cells.slice(i, i + 7))
  return columns
}

/** 열마다 그 주에 새 달이 시작하면 달 이름 ("10월"). 아니면 null. */
export function monthLabels(columns: (Day | null)[][]): (string | null)[] {
  let last = -1
  return columns.map((column) => {
    const firstDay = column.find((day): day is Day => day !== null)
    if (!firstDay) return null
    const month = Number(firstDay.date.slice(5, 7))
    if (month === last) return null
    last = month
    return `${month}월`
  })
}

/** 1년 요약 — 히트맵을 못 보는 사람에게 같은 정보를 한 문장으로. */
export function activitySummary(days: Day[]): { submissions: number; activeDays: number } {
  return {
    submissions: days.reduce((sum, day) => sum + day.submissions, 0),
    activeDays: days.filter((day) => day.submissions > 0).length,
  }
}

/** 레이팅 기록을 시간순으로 (서버는 최근 것부터 준다). 첫 점은 첫 대회 전의 값이다. */
export function ratingSeries(history: NonNullable<PublicProfile['rating']>['history']): { at: string; value: number; title: string | null }[] {
  const ordered = [...history].sort((a, b) => a.at.localeCompare(b.at))
  if (ordered.length === 0) return []
  return [
    { at: ordered[0]!.at, value: ordered[0]!.before, title: null },
    ...ordered.map((change) => ({ at: change.at, value: change.after, title: change.title })),
  ]
}

export const TIER_LABEL: Record<PublicProfile['contributorTier'], string> = {
  NEW: '새 기여자',
  ACTIVE: '기여자',
  TRUSTED: '믿을 만한 기여자',
}
