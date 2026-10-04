import type { ProblemSummary } from '../../shared/types'

/**
 * 전역 검색의 순위 (docs/ui-overhaul.md §4 "검색(⌘K)").
 *
 * 문제는 번호·제목·slug·태그로 찾는다. 순위는 사람이 친 것이 **무엇이었을 법한가**를 따른다:
 * 숫자만 쳤으면 그 번호의 문제가 맨 위, 제목이 그 말로 시작하면 다음, 제목 안에 있으면 그다음, 태그만
 * 맞으면 마지막. 같은 순위끼리는 번호 순 — 결정적이어야 같은 검색어에 같은 줄이 같은 자리에 온다.
 *
 * 순수 함수다. 화면은 이 결과를 그릴 뿐이다.
 */
export interface Command {
  id: string
  label: string
  /** 찾을 때 함께 보는 말 (예: "settings" 로도 설정이 나오게) */
  keywords?: string[]
  group: '이동' | '행동'
  run: () => void
}

export type Hit =
  | { kind: 'problem'; problem: ProblemSummary; rank: number }
  | { kind: 'command'; command: Command; rank: number }

const PROBLEM_LIMIT = 8

export function normalize(text: string): string {
  return text.trim().toLowerCase().replace(/\s+/g, ' ')
}

/** 문제 하나의 순위. 0 이면 맞지 않는다. 클수록 위. */
export function rankProblem(problem: ProblemSummary, query: string): number {
  const q = normalize(query)
  if (!q) return 0
  if (/^\d+$/.test(q)) {
    if (problem.number === Number(q)) return 100
    return String(problem.number ?? '').startsWith(q) ? 40 : 0
  }
  const title = normalize(problem.title)
  if (title.startsWith(q)) return 80
  if (title.includes(q)) return 60
  if (problem.id.includes(q.replace(/ /g, '-'))) return 50
  if (problem.tags.some((tag) => normalize(tag).includes(q))) return 20
  return 0
}

function rankCommand(command: Command, query: string): number {
  const q = normalize(query)
  if (!q) return 1
  const words = [command.label, ...(command.keywords ?? [])].map(normalize)
  if (words.some((word) => word.startsWith(q))) return 70
  if (words.some((word) => word.includes(q))) return 30
  return 0
}

/**
 * 검색어에 맞는 문제와 명령. 검색어가 비면 명령만 (문제 190개를 다 늘어놓는 것은 검색이 아니다).
 */
export function search(query: string, problems: ProblemSummary[], commands: Command[]): Hit[] {
  const q = normalize(query)
  const problemHits: Hit[] = q
    ? problems
        .map((problem) => ({ kind: 'problem' as const, problem, rank: rankProblem(problem, q) }))
        .filter((hit) => hit.rank > 0)
        .sort((a, b) => b.rank - a.rank || (a.problem.number ?? 0) - (b.problem.number ?? 0))
        .slice(0, PROBLEM_LIMIT)
    : []
  const commandHits: Hit[] = commands
    .map((command) => ({ kind: 'command' as const, command, rank: rankCommand(command, q) }))
    .filter((hit) => hit.rank > 0)
  // 숫자를 쳤으면 문제를 찾는 것이다 — 문제가 위. 말을 쳤으면 이동 명령이 정확히 맞을 때만 위로
  const topCommand = commandHits.some((hit) => hit.rank >= 70) && !/^\d+$/.test(q)
  return topCommand ? [...commandHits, ...problemHits] : [...problemHits, ...commandHits]
}
