import type { ApiError } from '../shared/types'

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

/**
 * 생성 요청은 모두 Idempotency-Key 를 보낸다 (기술 설계서 §9.1).
 *
 * 제출 버튼 중복 클릭이나 네트워크 재시도가 제출을 두 번 만들지 않게 하는 유일한 장치다.
 * 서버는 `UNIQUE(user_id, idempotency_key)` 로 기존 제출을 그대로 돌려준다 (§4.3).
 */
export async function post<T>(path: string, body: unknown, idempotencyKey: string): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new ApiFailure(response.status, (await response.json()) as ApiError)
  }
  return (await response.json()) as T
}

export async function get<T>(path: string): Promise<T> {
  const response = await fetch(`${BASE}${path}`)
  if (!response.ok) {
    throw new ApiFailure(response.status, (await response.json()) as ApiError)
  }
  return (await response.json()) as T
}

export function newIdempotencyKey(): string {
  return crypto.randomUUID()
}
