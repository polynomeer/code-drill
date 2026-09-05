import type { ApiError, Problem, Submission, TraceCapture } from '../shared/types'

const BASE = '/api/v1'

export class ApiFailure extends Error {
  constructor(
    readonly status: number,
    readonly detail: ApiError,
  ) {
    super(detail.message)
    this.name = 'ApiFailure'
  }
}

async function json<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new ApiFailure(response.status, (await response.json()) as ApiError)
  }
  return (await response.json()) as T
}

export function getProblem(slug: string): Promise<Problem> {
  return fetch(`${BASE}/problems/${slug}`).then(json<Problem>)
}

export function getSubmission(id: string): Promise<Submission> {
  return fetch(`${BASE}/submissions/${id}`).then(json<Submission>)
}

/**
 * 트레이스는 판정과 독립이라 아직 없을 수 있다. 없는 것은 오류가 아니므로 null 이다.
 */
export async function getTrace(id: string): Promise<TraceCapture | null> {
  const response = await fetch(`${BASE}/submissions/${id}/trace`)
  if (response.status === 204) return null
  return json<TraceCapture>(response)
}

/**
 * 제출 생성 (기술 설계서 §9.1).
 *
 * Idempotency-Key 는 선택이 아니다. 버튼 중복 클릭이나 네트워크 재시도가 제출을 두 번
 * 만들지 않게 하는 유일한 장치라서, 호출부가 빠뜨릴 수 없도록 여기서 항상 붙인다.
 */
export function createSubmission(problemId: string, problemVersion: number, source: string) {
  return fetch(`${BASE}/submissions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': crypto.randomUUID(),
    },
    body: JSON.stringify({ problemId, problemVersion, language: 'KOTLIN', source }),
  }).then(json<Submission>)
}
