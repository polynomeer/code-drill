import type { Confidence, MasteryLevel, MasteryView } from '../../shared/types'

/**
 * 역량 지도의 표현 규칙 (UI 디자인 문서 §9.4, 디자인 설계서 §10.4·§10.5).
 *
 * > Level과 Confidence를 분리하고, 표본이 부족하면 약점으로 단정하지 않습니다.
 *
 * 그래서 여기에는 **합치는 함수가 없다.** 역량군 요약도 평균을 내지 않고 수준별 개수를 센다 —
 * "기르는 중 2 · 해낸다 1"은 사실이고 "62%"는 지어낸 정밀도다.
 */

/** Level 막대의 칸 수. 재지 않은 것은 막대 대신 한 줄로 접는다. */
export const LEVEL_STEPS: Record<Exclude<MasteryLevel, 'UNMEASURED'>, number> = {
  DEVELOPING: 1,
  PROFICIENT: 2,
  STRONG: 3,
}

/** Confidence 점의 개수. Level 막대와 다른 모양이어야 둘을 하나로 읽지 않는다. */
export const CONFIDENCE_STEPS: Record<Confidence, number> = {
  NONE: 0,
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3,
}

export function isMeasured(item: MasteryView): boolean {
  return item.evidenceCount > 0 && item.level !== 'UNMEASURED'
}

/**
 * "추가 확인 필요" (DS §10.5 "Confidence가 낮으면 약점 확정 표현 대신 '추가 확인 필요'").
 *
 * 쟀지만 근거가 적은 것. 이것이 "기르는 중"이어도 약점이라고 부르지 않는다.
 */
export function needsMoreEvidence(item: MasteryView): boolean {
  return isMeasured(item) && (item.confidence === 'LOW' || item.confidence === 'NONE')
}

export interface GroupSummary {
  group: string
  total: number
  measured: number
  /** 수준별 개수. 잰 것만 센다. */
  levels: Partial<Record<MasteryLevel, number>>
  needsMoreEvidence: number
}

/** 역량군 요약 카드 (UI §9.4 Competency summary). 원래 순서(온톨로지 순)를 지킨다. */
export function summarize(items: MasteryView[]): GroupSummary[] {
  const groups = new Map<string, GroupSummary>()
  for (const item of items) {
    const summary = groups.get(item.group) ?? { group: item.group, total: 0, measured: 0, levels: {}, needsMoreEvidence: 0 }
    summary.total += 1
    if (isMeasured(item)) {
      summary.measured += 1
      summary.levels[item.level] = (summary.levels[item.level] ?? 0) + 1
    }
    if (needsMoreEvidence(item)) summary.needsMoreEvidence += 1
    groups.set(item.group, summary)
  }
  return [...groups.values()]
}

/**
 * 증거의 도움 수준 (FR-806 "근거 상세에서 확인할 수 있어야 한다"). 가중치 1 이면 도움 없이 얻은
 * 것이다. 가중치를 숫자 그대로 보이는 이유: 사용자가 "왜 이 증거가 덜 셌나"를 물을 때 답이 이것이다.
 */
export function helpLabel(weight: number): string {
  if (weight >= 1) return '도움 없음'
  return `도움 받음 · 무게 ${Math.round(weight * 100) / 100}`
}

/**
 * 통계를 말해도 되는가 (DS §10.4 "시도 수가 적으면 '데이터가 더 필요합니다'").
 * 다섯 번 미만의 제출로 정답률을 내면 한 번의 실수가 20% 가 된다.
 */
export const ENOUGH_SUBMISSIONS = 5

export function enoughData(submissions: number): boolean {
  return submissions >= ENOUGH_SUBMISSIONS
}
