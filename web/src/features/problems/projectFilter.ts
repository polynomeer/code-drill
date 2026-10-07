import type { Difficulty, ProjectSummary } from '../../shared/types'

/**
 * 프로젝트형 문제 목록의 거르기 (docs/ui-overhaul.md §6.1 "프로젝트형 탭").
 *
 * 열일곱 개뿐이라 서버에 묻지 않고 받은 목록을 화면에서 거른다. 알고리즘 문제의 필터와 섞지 않는다 —
 * 두 번째 판정기의 문제라 태그 체계도, 상태 필터의 뜻도 다르다.
 */
export interface ProjectFilter {
  languages: string[]
  difficulty: Difficulty[]
}

export const EMPTY_PROJECT_FILTER: ProjectFilter = { languages: [], difficulty: [] }

/** 같은 묶음 안은 "또는", 묶음끼리는 "그리고" — 알고리즘 목록의 칩과 같은 규칙 */
export function filterProjects(items: ProjectSummary[], filter: ProjectFilter): ProjectSummary[] {
  return items.filter(
    (item) =>
      (filter.languages.length === 0 || filter.languages.includes(item.language)) &&
      (filter.difficulty.length === 0 || filter.difficulty.includes(item.difficulty)),
  )
}

/** 목록에 실제로 있는 언어만 칩으로 — 없는 언어 칩은 누르면 빈 표가 된다 */
export function projectLanguages(items: ProjectSummary[]): string[] {
  const order = ['KOTLIN', 'JAVA', 'PYTHON']
  return [...new Set(items.map((item) => item.language))].sort((a, b) => order.indexOf(a) - order.indexOf(b))
}

/** 열 곳 — 프로젝트형 작업 공간. 둘러보는 사람은 로그인을 거쳐 그리로 돌아온다 */
export function projectHref(id: string, signedIn: boolean): string {
  const target = `/projects/${encodeURIComponent(id)}`
  return signedIn ? target : `/login?next=${encodeURIComponent(target)}`
}
