/**
 * Control Plane 과 공유하는 값 타입.
 *
 * 백엔드의 `SubmissionStatus`(§4.2), `Verdict`(§14.1), `TraceEvent`(§7.2)와 짝을 이룬다.
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

/** 진행 중 상태. 이 동안 UI 는 판정 결과를 단정하지 않는다 (§0.2 No false precision). */
export const IN_FLIGHT: ReadonlySet<SubmissionStatus> = new Set([
  'CREATED',
  'QUEUED',
  'LEASED',
  'COMPILING',
  'RUNNING',
  'AGGREGATING',
])

export const VERDICT_LABEL: Record<Verdict, string> = {
  ACCEPTED: '정답',
  WRONG_ANSWER: '오답',
  COMPILE_ERROR: '컴파일 실패',
  RUNTIME_ERROR: '런타임 오류',
  TIME_LIMIT: '시간 초과',
  MEMORY_LIMIT: '메모리 초과',
  OUTPUT_LIMIT: '출력 초과',
  SYSTEM_ERROR: '시스템 오류',
}

export interface Problem {
  id: string
  version: number
  title: string
  statement: string
  timeMillis: number
  memoryMb: number
  signature: string
  /** 공개 샘플만 담긴다. 숨은 케이스는 서버가 애초에 내려보내지 않는다 (§9.1). */
  samples: { id: string; args: unknown[]; expected: unknown }[]
  groups: GroupInfo[]
}

/** 그룹별 배점. `SUM` 은 통과 비율만큼 부분 점수를 준다 (§6.2). */
export interface GroupInfo {
  id: string
  weight: number
  aggregation: 'ALL_OR_NOTHING' | 'SUM'
  caseCount: number
}

export interface ProblemSummary {
  id: string
  version: number
  title: string
}

export type SubmissionLanguage = 'KOTLIN' | 'JAVA' | 'PYTHON'

export const LANGUAGE_LABEL: Record<SubmissionLanguage, string> = {
  KOTLIN: 'Kotlin',
  JAVA: 'Java',
  PYTHON: 'Python',
}

/** Monaco 언어 id. 채점 언어와 이름이 같지는 않다. */
export const EDITOR_LANGUAGE: Record<SubmissionLanguage, string> = {
  KOTLIN: 'kotlin',
  JAVA: 'java',
  PYTHON: 'python',
}

export interface CaseResult {
  caseId: string
  groupId: string
  verdict: Verdict
  measurements: { wallTimeMillis: number; peakMemoryBytes: number }
  message: string | null
}

export interface GroupResult {
  groupId: string
  verdict: Verdict
  score: number
  maxScore: number
  cases: CaseResult[]
}

export interface Submission {
  id: string
  problemId: string
  problemVersion: number
  language: string
  status: SubmissionStatus
  verdict: Verdict | null
  score: number | null
  compileLog: string | null
  groups: GroupResult[] | null
}

/* 트레이스 타입은 features/replay/traceTypes.ts 에 있다. 렌더러와 함께 두어야
   스키마를 바꿀 때 그리는 쪽과 계약이 같이 움직인다. */

/** cursor 페이지네이션 응답 (§9.1). nextCursor 가 null 이면 마지막 페이지다. */
export interface Page<T> {
  items: T[]
  nextCursor: string | null
}

/** 작업 중인 초안. version 은 CAS 용이며 저장할 때마다 오른다 (§8.2). */
export interface Draft {
  problemId: string
  language: string
  code: string
  version: number
  updatedAt: string
}

export interface DraftConflict {
  error: ApiError
  current: Draft
}

/** 표준 오류 응답 (§9.1). traceId 는 문의·조사에 그대로 쓴다. */
export interface ApiError {
  errorCode: string
  message: string
  traceId: string
}
