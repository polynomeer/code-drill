/**
 * Control Plane 과 공유하는 값 타입.
 *
 * 백엔드의 `SubmissionStatus`(§4.2), `Verdict`(§14.1), `ErrorCode`(§9.4)와 짝을 이룬다.
 * 한쪽만 바꾸면 계약이 깨지므로 함께 고친다.
 */

export type SubmissionStatus =
  | 'CREATED'
  | 'QUEUED'
  | 'LEASED'
  | 'COMPILING'
  | 'RUNNING'
  | 'AGGREGATING'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'SYSTEM_ERROR'

export type Verdict =
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'COMPILE_ERROR'
  | 'RUNTIME_ERROR'
  | 'TIME_LIMIT'
  | 'MEMORY_LIMIT'
  | 'OUTPUT_LIMIT'
  | 'SYSTEM_ERROR'

/** 진행 중 상태. 이 동안 UI 는 판정 결과를 단정하지 않는다. */
export const IN_FLIGHT: ReadonlySet<SubmissionStatus> = new Set([
  'CREATED',
  'QUEUED',
  'LEASED',
  'COMPILING',
  'RUNNING',
  'AGGREGATING',
])

/** 표준 오류 응답 (§9.1). traceId 는 문의·조사에 그대로 쓴다. */
export interface ApiError {
  errorCode: string
  message: string
  traceId: string
}
