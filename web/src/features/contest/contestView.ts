import type { ContestSummary } from '../../shared/types'

/**
 * 대회 화면의 규칙 (docs/ui-overhaul.md §6.6).
 *
 * 상태는 서버가 정한다(`status`). 화면이 시각으로 다시 판단하지 않는다 — 시계가 어긋난 브라우저에서
 * "진행 중"과 "끝남"이 서버와 다르게 보이면 안 된다. 화면이 시각으로 하는 일은 카운트다운뿐이다.
 */

export const KIND_LABEL: Record<ContestSummary['kind'], string> = {
  CONTEST: '대회',
  DUEL: '미니 대결',
  HACK: '반례 대전',
  VIRTUAL: '가상 참가',
}

export const STATUS_LABEL: Record<ContestSummary['status'], string> = {
  DRAFT: '준비 중',
  WAITING: '상대를 기다림',
  SCHEDULED: '예정',
  RUNNING: '진행 중',
  FINISHED: '끝남',
}

export type Section = 'running' | 'upcoming' | 'finished'

/** 로비의 세 구획. 대기·예정·준비는 모두 "예정"이다 — 아직 시작하지 않았다. */
export function sectionOf(contest: ContestSummary): Section {
  if (contest.status === 'RUNNING') return 'running'
  if (contest.status === 'FINISHED') return 'finished'
  return 'upcoming'
}

/** 카운트다운이 세는 것. 진행 중이면 끝까지, 예정이면 시작까지. 그 밖은 셀 것이 없다. */
export function countdownOf(contest: ContestSummary): { label: string; target: string } | null {
  if (contest.status === 'RUNNING' && contest.endsAt) return { label: '끝까지', target: contest.endsAt }
  if (contest.status === 'SCHEDULED' && contest.startsAt) return { label: '시작까지', target: contest.startsAt }
  return null
}

/**
 * 남은 시간. 하루가 넘으면 "2일 3시간", 아니면 "1:02:03". 지났으면 "0:00:00" — 음수를 보이지 않는다;
 * 서버가 상태를 바꿀 때까지의 몇 초는 0 에 머문다.
 */
export function remaining(target: string, now: number): string {
  const seconds = Math.max(0, Math.floor((Date.parse(target) - now) / 1000))
  const days = Math.floor(seconds / 86400)
  if (days > 0) return `${days}일 ${Math.floor((seconds % 86400) / 3600)}시간`
  return clock(seconds)
}

/** 걸린 시간 "1:05:30" — 순위표의 동점 기준이다 (시각이 아니라 걸린 시간, Contest.kt Standing). */
export function clock(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/** 레이팅 변화 "+12" / "-8" / "0". 부호가 늘 보여야 위아래가 읽힌다. */
export function signed(delta: number): string {
  return delta > 0 ? `+${delta}` : String(delta)
}
