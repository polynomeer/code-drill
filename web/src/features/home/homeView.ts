import type { MasteryLevel, PrescribedProblem } from '../../shared/types'
import type { GroupSummary } from '../competency/competencyView'

/**
 * 홈 요약 카드의 표현 규칙 (docs/ui-overhaul.md §6.10).
 *
 * 홈은 이미 있는 처방·역량·통계를 요약할 뿐 새 규칙을 만들지 않는다. 서버가 정하지 않은 숫자(점수·레벨)는
 * 화면에서 지어내지 않는다 (No false precision).
 */
export interface TodaySplit {
  first: PrescribedProblem | null
  rest: PrescribedProblem[]
}

/** 처방의 첫 칸을 크게, 나머지를 아래에. 처방이 이미 우선순위대로 온다 — 화면이 다시 고르지 않는다 */
export function splitToday(items: PrescribedProblem[]): TodaySplit {
  const [first = null, ...rest] = items
  return { first, rest }
}

export interface GroupTile {
  group: string
  /** 잰 역량이 하나도 없는 역량군 — 수준을 단정하지 않는다 */
  unmeasured: boolean
  measured: number
  total: number
  /** 잰 역량의 수준별 개수, 높은 수준부터 */
  levels: { level: Exclude<MasteryLevel, 'UNMEASURED'>; count: number }[]
  needsMoreEvidence: number
}

const ORDER = ['STRONG', 'PROFICIENT', 'DEVELOPING'] as const

export function groupTiles(groups: GroupSummary[]): GroupTile[] {
  return groups.map((group) => ({
    group: group.group,
    unmeasured: group.measured === 0,
    measured: group.measured,
    total: group.total,
    levels: ORDER.map((level) => ({ level, count: group.levels[level] ?? 0 })).filter((item) => item.count > 0),
    needsMoreEvidence: group.needsMoreEvidence,
  }))
}

/** 이번 주 활동일 — 주간 리포트의 활동한 날 수. 0~7 로 자른다 */
export function activeWeek(activeDays: number): { days: number; of: number } {
  return { days: Math.max(0, Math.min(7, activeDays)), of: 7 }
}
