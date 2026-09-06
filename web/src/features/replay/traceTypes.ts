/**
 * 트레이스 계약 (기술 설계서 §7.2, §7.4).
 *
 * 백엔드의 `TraceEvent` / `TraceManifest` 와 짝을 이룬다. 한쪽만 바꾸면 리플레이가 조용히
 * 잘못된 상태를 그리므로 함께 고친다.
 */

export type TargetKind = 'ARRAY' | 'STACK' | 'QUEUE' | 'GRAPH' | 'CALL'

export type TraceEventType =
  | 'VISIT'
  | 'COMPARE'
  | 'SWAP'
  | 'WRITE'
  | 'POINTER'
  | 'PUSH'
  | 'POP'
  | 'ENQUEUE'
  | 'DEQUEUE'
  | 'NODE'
  | 'EDGE'
  | 'CALL'
  | 'RETURN'
  | 'MATCH'

export interface TraceEvent {
  seq: number
  logicalTime: number
  eventType: TraceEventType
  targetKind: TargetKind
  targetRef: string
  before: string | null
  after: string | null
  importance: number
  sourceLine: number | null
  attributes: Record<string, string>
}

export type TraceStatus = 'READY' | 'EMPTY' | 'INVALID'

export interface ChunkRef {
  index: number
  firstSeq: number
  lastSeq: number
  eventCount: number
}

export interface TraceManifest {
  schemaVersion: string
  traceId: string
  submissionId: string
  caseId: string
  status: TraceStatus
  eventCount: number
  chunks: ChunkRef[]
  summary: TraceEvent[]
  truncated: boolean
  diagnostics: string | null
}

export interface TraceChunk {
  schemaVersion: string
  traceId: string
  index: number
  events: TraceEvent[]
}

/**
 * 클라이언트가 이해하는 스키마 버전.
 *
 * manifest 의 버전이 이보다 높으면 상태를 그리지 않고 텍스트 이벤트 목록으로 폴백한다
 * (§7.5 스키마 미지원 시 안전 폴백). 모르는 이벤트를 아는 척 그리는 것이 가장 나쁘다.
 */
export const SUPPORTED_SCHEMA_MAJOR = 2

export function schemaSupported(version: string): boolean {
  const major = Number(version.split('.')[0])
  return Number.isFinite(major) && major === SUPPORTED_SCHEMA_MAJOR
}
