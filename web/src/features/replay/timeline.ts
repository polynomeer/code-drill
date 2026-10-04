import type { TraceEvent, TraceEventType } from './traceTypes'

/**
 * 리플레이 타임라인과 텍스트 폴백의 규칙 (UI 디자인 문서 §5.3, 디자인 설계서 §8.3·§8.5).
 *
 * 화면 네 개(코드·캔버스·검사기·타임라인)가 **같은 걸음**을 가리키려면 "지금 어디인가"를 정하는
 * 규칙이 한 곳에 있어야 한다. 여기 모은 함수는 전부 순수하다 — 걸음 번호 하나를 받아 다음 걸음
 * 번호나 문장을 돌려줄 뿐 상태를 들지 않는다.
 *
 * 걸음(step)은 "앞에서부터 적용한 이벤트 수"다. seq 는 1 부터 이어지므로 seq N 이벤트를 적용한
 * 상태가 곧 걸음 N 이다. 분기 진단의 `divergedAtSeq` 로 그대로 이동하는 것도 이 약속 덕분이다.
 */

export type MarkerTier = 'normal' | 'important' | 'error'

export interface Marker {
  seq: number
  tier: MarkerTier
}

/**
 * 중요 이벤트의 문턱. SDK 의 기본 중요도가 VISIT·POINTER·EDGE 는 1, 상태를 바꾸는 것(비교·기록·
 * push 등)은 2, 답을 확정하는 MATCH 는 3 이다 (judge/protocol Trace.kt). 상태를 바꾼 순간이
 * "중요"다 — 살펴보기만 한 걸음까지 세면 Shift+→ 가 한 걸음 이동과 다르지 않다.
 */
export const IMPORTANT = 2

/** 마커 세 크기 (UI §5.3 "오류 이벤트는 색상, 크기, 텍스트 레이블로 중복 인코딩"). */
export function tierOf(event: TraceEvent, divergedAtSeq: number | null): MarkerTier {
  if (event.seq === divergedAtSeq) return 'error'
  return event.importance >= IMPORTANT ? 'important' : 'normal'
}

/**
 * 타임라인 마커.
 *
 * manifest 요약(최대 1,000건)에서 만든다. 상세 청크는 앞에서부터만 받으므로 그것으로 그리면
 * 뒤쪽이 비어 보인다 — 요약은 트레이스 전체에 고르게 퍼져 있다.
 *
 * 요약 모드는 중요 이벤트만, 전체 모드는 요약 전부. 분기 지점은 요약에 없어도 늘 넣는다.
 */
export function markersOf(
  summary: TraceEvent[],
  divergedAtSeq: number | null,
  mode: 'summary' | 'all',
): Marker[] {
  const markers = summary
    .map((event) => ({ seq: event.seq, tier: tierOf(event, divergedAtSeq) }))
    .filter((marker) => mode === 'all' || marker.tier !== 'normal')
  if (divergedAtSeq !== null && !markers.some((marker) => marker.seq === divergedAtSeq)) {
    markers.push({ seq: divergedAtSeq, tier: 'error' })
    markers.sort((a, b) => a.seq - b.seq)
  }
  return markers
}

/**
 * Shift+←/→ — 다음(이전) 중요 이벤트 (UI §5.3).
 *
 * 마커가 없으면 걸음은 그대로다. 끝을 지나 처음으로 돌아가지 않는다 — 재생 위치가 순환하면
 * "뒤로 갔더니 맨 끝에 있다"는 것을 화면만으로 알아채기 어렵다.
 */
export function nextMarker(markers: Marker[], step: number, direction: 1 | -1): number {
  if (direction === 1) return markers.find((marker) => marker.seq > step)?.seq ?? step
  for (let i = markers.length - 1; i >= 0; i -= 1) {
    const marker = markers[i]
    if (marker && marker.seq < step) return marker.seq
  }
  return step
}

/**
 * 코드 줄 → 걸음 (CodeTraceLink, UI §5.1 "코드/이벤트 선택과 양방향 연결").
 *
 * 그 줄이 부른 다음 이벤트로 간다. 지금 걸음 뒤에 없으면 처음부터 찾는다 — 반복문 안의 줄을
 * 누르면 다음 반복으로 넘어가고, 마지막 반복 뒤에서 누르면 첫 반복으로 돌아온다.
 *
 * 받아 둔 상세 이벤트에서 먼저 찾고, 없으면 요약에서 찾는다. 요약에만 있는 걸음으로 가면
 * 그 청크를 그때 받는다.
 */
export function nextOnLine(
  events: TraceEvent[],
  summary: TraceEvent[],
  line: number,
  step: number,
): number | null {
  for (const source of [events, summary]) {
    const onLine = source.filter((event) => event.sourceLine === line)
    const after = onLine.find((event) => event.seq > step)
    if (after) return after.seq
    if (onLine[0]) return onLine[0].seq
  }
  return null
}

/** 캔버스에서 고른 대상을 마지막으로 건드린 걸음. 고른 칸이 "왜 이 값인가"에 답한다. */
export function lastTouch(events: TraceEvent[], step: number, target: string): TraceEvent | null {
  for (let i = Math.min(step, events.length) - 1; i >= 0; i -= 1) {
    const event = events[i]
    if (event && touches(event, target)) return event
  }
  return null
}

/** 고른 대상을 지금까지 건드린 코드 줄. 코드 pane 이 옅게 칠한다. */
export function linesTouching(events: TraceEvent[], step: number, target: string): Set<number> {
  const lines = new Set<number>()
  for (let i = 0; i < Math.min(step, events.length); i += 1) {
    const event = events[i]
    if (event && event.sourceLine !== null && touches(event, target)) lines.add(event.sourceLine)
  }
  return lines
}

/** 코드 줄마다 이벤트가 몇 번 났나. 줄 번호 옆 표시와 "이 줄로 가기" 버튼의 근거다. */
export function linesWithEvents(events: TraceEvent[], summary: TraceEvent[]): Set<number> {
  const lines = new Set<number>()
  for (const event of [...summary, ...events]) if (event.sourceLine !== null) lines.add(event.sourceLine)
  return lines
}

/**
 * 캔버스의 선택 대상 키. 배열은 인덱스, 그래프는 정점이다. SWAP·MATCH 는 `after` 가 상대 인덱스라
 * 그 칸도 건드린 것으로 센다 (reducer.ts 와 같은 해석).
 */
export function targetKey(kind: 'ARRAY' | 'GRAPH', ref: string): string {
  return `${kind}:${ref}`
}

function touches(event: TraceEvent, target: string): boolean {
  if (event.targetKind === 'ARRAY' && target === targetKey('ARRAY', event.targetRef)) return true
  if (event.targetKind === 'GRAPH' && target === targetKey('GRAPH', event.targetRef)) return true
  if (event.after === null) return false
  if (event.eventType === 'SWAP' || event.eventType === 'MATCH') return target === targetKey('ARRAY', event.after)
  if (event.eventType === 'EDGE') return target === targetKey('GRAPH', event.after)
  return false
}

/* ─── 재생 ─── */

/** 배속 (UI §5.3 "0.5x, 1x, 2x 세 단계로 제한"). */
export const SPEEDS = [0.5, 1, 2] as const
export type Speed = (typeof SPEEDS)[number]

/** 1x 에서 한 걸음에 머무는 시간. 눈이 셀 하나의 바뀜을 읽고 코드 줄로 옮겨 가기에 충분한 시간. */
const BASE_INTERVAL_MS = 800

export function intervalOf(speed: Speed): number {
  return BASE_INTERVAL_MS / speed
}

/**
 * 재생할 때의 다음 걸음. 요약 모드는 중요 이벤트 사이를 건너뛰고(디자인 설계서 §8.3 "기본은 중요
 * 이벤트 요약 모드"), 전체 모드는 한 걸음씩 간다. 끝이면 null — 재생이 멈춘다.
 */
export function nextPlayStep(markers: Marker[], step: number, total: number, mode: 'summary' | 'all'): number | null {
  if (step >= total) return null
  if (mode === 'all') return step + 1
  const next = nextMarker(markers, step, 1)
  // 마지막 중요 이벤트를 지났으면 끝까지 한 번에 간다. 거기서 멈추면 "끝났나?"를 묻게 된다.
  return next > step ? next : total
}

/* ─── 텍스트 폴백 (디자인 설계서 §8.5) ─── */

/** 이벤트 종류의 짧은 이름. 검사기의 칩과 예측 보기에 쓴다. */
export const EVENT_LABEL: Record<TraceEventType, string> = {
  VISIT: '살펴봄',
  COMPARE: '비교',
  SWAP: '교환',
  WRITE: '기록',
  POINTER: '포인터 이동',
  PUSH: 'push',
  POP: 'pop',
  ENQUEUE: 'enqueue',
  DEQUEUE: 'dequeue',
  NODE: '정점 방문',
  EDGE: '간선',
  CALL: '호출',
  RETURN: '반환',
  MATCH: '답을 찾음',
}

/**
 * 받침 있는 말 뒤에는 '을·과·이', 없는 말 뒤에는 '를·와·가'. 숫자는 읽는 소리로 정한다
 * (3 "삼" → 을, 2 "이" → 를). 영문 이름은 읽는 법이 하나가 아니라 괄호 형태로 둔다.
 */
export function josa(word: string, pair: '을/를' | '과/와' | '이/가'): string {
  const [withFinal, withoutFinal] = pair.split('/') as [string, string]
  const last = word.trim().at(-1) ?? ''
  const code = last.charCodeAt(0)
  let final: boolean | null = null
  if (code >= 0xac00 && code <= 0xd7a3) final = (code - 0xac00) % 28 !== 0
  else if (/[0-9]/.test(last)) final = '013678'.includes(last)
  return word + (final === null ? `${withFinal}(${withoutFinal})` : final ? withFinal : withoutFinal)
}

/** 이벤트를 한 문장으로. "단계 12: 인덱스 3을 비교했습니다" (§8.5). 스크린리더가 읽는 것이 이것이다. */
export function describe(event: Pick<TraceEvent, 'eventType' | 'targetRef' | 'after' | 'before'>): string {
  const ref = event.targetRef
  const after = event.after ?? ''
  const index = `인덱스 ${ref}`
  switch (event.eventType as TraceEventType) {
    case 'VISIT':
      return `${josa(index, '을/를')} 살펴봤습니다`
    case 'COMPARE':
      return `${josa(index, '을/를')} 비교했습니다`
    case 'SWAP':
      return after ? `${josa(index, '과/와')} ${josa(after, '을/를')} 맞바꿨습니다` : `${josa(index, '을/를')} 맞바꿨습니다`
    case 'WRITE':
      return `${index}에 ${josa(after || '값', '을/를')} 썼습니다`
    case 'POINTER':
      return after ? `포인터 ${josa(after, '을/를')} ${index}에 두었습니다` : `포인터를 ${index}에 두었습니다`
    case 'MATCH':
      return after ? `${josa(index, '과/와')} ${after}에서 답을 찾았습니다` : `${index}에서 답을 찾았습니다`
    case 'PUSH':
      return `스택에 ${josa(after || ref, '을/를')} 넣었습니다`
    case 'POP':
      return '스택 맨 위를 꺼냈습니다'
    case 'ENQUEUE':
      return `큐 뒤에 ${josa(after || ref, '을/를')} 넣었습니다`
    case 'DEQUEUE':
      return '큐 앞을 꺼냈습니다'
    case 'NODE':
      return `정점 ${josa(ref, '을/를')} 방문했습니다`
    case 'EDGE':
      return after ? `간선 ${ref} → ${josa(after, '을/를')} 따라갔습니다` : `정점 ${ref}의 간선을 봤습니다`
    case 'CALL':
      return `${josa(ref, '을/를')} 호출했습니다`
    case 'RETURN':
      return after ? `${josa(ref, '이/가')} ${josa(after, '을/를')} 돌려줬습니다` : `${josa(ref, '이/가')} 끝났습니다`
    default:
      return `${event.eventType} ${ref}${after ? ` → ${after}` : ''}`
  }
}

export function describeStep(event: TraceEvent): string {
  return `단계 ${event.seq}: ${describe(event)}`
}

/**
 * 분기 진단이 주는 한 걸음 문자열을 문장으로 되돌린다.
 *
 * 서버는 내 걸음을 `COMPARE array[3] → 5`, 참조 걸음을 `COMPARE [3] → 5` 로 준다
 * (DivergenceService). 둘 다 "무엇을·어디에·어떻게" 셋뿐이라 같은 문장 규칙에 넣을 수 있다.
 * 형식이 다르면(끝났다는 문장 등) 받은 그대로 둔다.
 */
export function describeDivergenceStep(text: string | null): string | null {
  if (text === null) return null
  const match = /^([A-Z_]+) (?:[a-z]+)?\[([^\]]*)\](?: → (.*))?$/.exec(text)
  if (!match) return text
  const [, type, ref, after] = match
  return describe({ eventType: type as TraceEventType, targetRef: ref ?? '', after: after ?? null, before: null })
}

/**
 * 배열 렌더러가 시작 상태로 삼을 첫 인자. 격자는 행 우선으로 편다 — 계측 이벤트의 인덱스가
 * `행 * 열 + 열` 이므로 편 배열과 자리가 맞는다.
 */
export function sampleArray(value: unknown): number[] {
  if (!Array.isArray(value)) return []
  return value.flatMap((item) =>
    Array.isArray(item)
      ? item.filter((cell): cell is number => typeof cell === 'number')
      : typeof item === 'number'
        ? [item]
        : [],
  )
}
