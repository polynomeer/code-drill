import { useQuery } from '@tanstack/react-query'
import { listProblems } from '../../api/client'
import { EMPTY_FILTER } from '../../shared/types'
import type { ProblemSummary } from '../../shared/types'

/**
 * slug → 번호·제목 (제출 기록처럼 문제 id 만 아는 화면이 사람이 읽는 이름을 보이려고 쓴다).
 *
 * 제출 모듈은 문제 모듈을 모른다 (docs/project-context.md 모듈 경계). 그래서 서버가 이름을 붙여
 * 보내지 않고, 화면이 문제 목록을 한 번 받아 둔다 — 문제는 수백 개 단위라 두세 번 요청이면 다 온다.
 */
export function useProblemIndex(): Map<string, ProblemSummary> {
  const query = useQuery({
    queryKey: ['problems', 'index'],
    staleTime: 5 * 60_000,
    queryFn: async () => {
      const all: ProblemSummary[] = []
      for (let page = 1; ; page++) {
        const result = await listProblems({ ...EMPTY_FILTER, page }, 100)
        all.push(...result.items)
        if (!result.pageCount || page >= result.pageCount) break
      }
      return new Map(all.map((item) => [item.id, item]))
    },
  })
  return query.data ?? EMPTY
}

const EMPTY = new Map<string, ProblemSummary>()

/** `1042. 겹치는 구간 합치기` — 모르면 slug 그대로 */
export function problemLabel(index: Map<string, ProblemSummary>, slug: string): string {
  const item = index.get(slug)
  if (!item) return slug
  return item.number ? `${item.number}. ${item.title}` : item.title
}
