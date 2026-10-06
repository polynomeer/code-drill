import type { MasteryLevel, PrescribedProblem } from '../../shared/types'
import type { GroupSummary } from '../competency/competencyView'

/**
 * 홈 퀘스트 보드의 표현 규칙 (docs/ui-overhaul.md §6.10).
 *
 * 퀘스트는 **처방을 다르게 그린 것**이지 새 규칙이 아니다. 경험치·레벨처럼 서버가 정하지 않은 숫자는
 * 만들지 않는다 — 화면이 지어낸 점수는 사용자가 믿고 쫓아가는 순간 거짓말이 된다 (No false precision).
 */
export interface QuestSplit {
  main: PrescribedProblem | null
  side: PrescribedProblem[]
}

/** 처방의 첫 칸이 메인 퀘스트다. 처방이 이미 우선순위대로 온다 — 화면이 다시 고르지 않는다 */
export function splitQuests(items: PrescribedProblem[]): QuestSplit {
  const [main = null, ...side] = items
  return { main, side }
}

export interface SkillTile {
  group: string
  /** 잰 역량이 하나도 없으면 잠긴 칸 — 점수를 단정하지 않는다 */
  locked: boolean
  measured: number
  total: number
  /** 잰 역량의 수준별 개수, 높은 수준부터. 별 하나 = 역량 하나 */
  stars: { level: Exclude<MasteryLevel, 'UNMEASURED'>; count: number }[]
  needsMoreEvidence: number
}

const ORDER = ['STRONG', 'PROFICIENT', 'DEVELOPING'] as const

export function skillTiles(groups: GroupSummary[]): SkillTile[] {
  return groups.map((group) => ({
    group: group.group,
    locked: group.measured === 0,
    measured: group.measured,
    total: group.total,
    stars: ORDER.map((level) => ({ level, count: group.levels[level] ?? 0 })).filter((item) => item.count > 0),
    needsMoreEvidence: group.needsMoreEvidence,
  }))
}

/** 이번 주 출석 — 주간 리포트의 활동한 날 수. 7을 넘지 않게 자른다 */
export function attendance(activeDays: number): { days: number; of: number } {
  return { days: Math.max(0, Math.min(7, activeDays)), of: 7 }
}
