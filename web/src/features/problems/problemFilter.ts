import { DIFFICULTIES, EMPTY_FILTER, PROBLEM_SORTS, type Difficulty, type ProblemFilter, type ProblemSort } from '../../shared/types'

/**
 * 필터를 URL 에 보존한다 (PRD FR-201 수용 기준).
 *
 * > 필터 조합이 URL 에 보존되고 새로고침 후 동일합니다.
 *
 * 화면 상태로만 두면 새로고침에 사라지고, 무엇보다 **남에게 보낼 수 없다**. "그래프
 * 문제 중 아직 안 푼 것" 같은 목록은 링크 하나로 건네지는 것이 자연스럽다.
 *
 * 읽을 때는 모르는 값을 조용히 버린다. 링크는 손으로 고쳐지고 오래 살아남는다 —
 * 없어진 난이도 값 하나 때문에 화면이 빈 목록이나 오류가 되면, 사용자는 자기 링크가
 * 왜 깨졌는지 알 수 없다.
 */
export function readFilter(search: string): ProblemFilter {
  const params = new URLSearchParams(search)
  const known = new Set<string>(DIFFICULTIES)
  const status = params.get('status')
  const sort = params.get('sort')
  const page = Number(params.get('page'))

  return {
    query: params.get('query') ?? '',
    difficulty: params.getAll('difficulty').filter((v): v is Difficulty => known.has(v)),
    tags: params.getAll('tags').filter((v) => v.trim() !== ''),
    status: status === 'SOLVED' || status === 'UNSOLVED' ? status : null,
    sort: (PROBLEM_SORTS as readonly string[]).includes(sort ?? '') ? (sort as ProblemSort) : EMPTY_FILTER.sort,
    order: params.get('order') === 'DESC' ? 'DESC' : 'ASC',
    page: Number.isInteger(page) && page > 1 ? page : 1,
  }
}

/** 기본값은 싣지 않는다. 아무것도 고르지 않은 상태의 URL 이 깨끗해야 공유가 자연스럽다. */
export function writeFilter(filter: ProblemFilter): string {
  const params = new URLSearchParams()
  if (filter.query.trim()) params.set('query', filter.query.trim())
  for (const value of filter.difficulty) params.append('difficulty', value)
  for (const value of filter.tags) params.append('tags', value)
  if (filter.status) params.set('status', filter.status)
  if (filter.sort !== EMPTY_FILTER.sort) params.set('sort', filter.sort)
  if (filter.order !== EMPTY_FILTER.order) params.set('order', filter.order)
  if (filter.page > 1) params.set('page', String(filter.page))
  return params.toString()
}

/**
 * 거르는 조건이 하나도 없는가. 정렬과 쪽은 거르는 것이 아니라 "필터 지우기"가 지우지 않는다 —
 * 정답률 순으로 보던 사람이 태그를 지웠다고 번호 순으로 돌아가면 안 된다.
 */
export function isEmpty(filter: ProblemFilter): boolean {
  return (
    filter.query.trim() === '' && filter.difficulty.length === 0 && filter.tags.length === 0 && filter.status === null
  )
}

/** 거르는 조건만 비운다. 정렬은 남긴다 (위 isEmpty 와 같은 이유). */
export function clearFilters(filter: ProblemFilter): ProblemFilter {
  return { ...EMPTY_FILTER, sort: filter.sort, order: filter.order }
}

/** 켜져 있으면 끄고, 꺼져 있으면 켠다. 다중 선택 필터는 전부 이 동작이다. */
export function toggle<T>(values: T[], value: T): T[] {
  return values.includes(value) ? values.filter((v) => v !== value) : [...values, value]
}
