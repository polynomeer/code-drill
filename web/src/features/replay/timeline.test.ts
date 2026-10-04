import { describe as group, expect, it } from 'vitest'
import {
  describe,
  describeDivergenceStep,
  josa,
  lastTouch,
  linesTouching,
  markersOf,
  nextMarker,
  nextOnLine,
  nextPlayStep,
  targetKey,
} from './timeline'
import type { TraceEvent, TraceEventType } from './traceTypes'

/**
 * 리플레이 네 pane 이 같은 걸음을 가리키게 하는 규칙 (UI 디자인 문서 §11.2 Replay).
 */
group('타임라인 마커', () => {
  const summary = [
    event(1, 'VISIT', '0', null, 1, 3),
    event(2, 'COMPARE', '0', null, 2, 4),
    event(3, 'POINTER', '1', 'left', 1, 5),
    event(4, 'MATCH', '0', '1', 3, 6),
  ]

  it('요약 모드는 중요 이벤트만, 분기 지점은 요약에 없어도 넣는다', () => {
    expect(markersOf(summary, 3, 'summary')).toEqual([
      { seq: 2, tier: 'important' },
      { seq: 3, tier: 'error' },
      { seq: 4, tier: 'important' },
    ])
    expect(markersOf(summary, 9, 'all').map((marker) => marker.seq)).toEqual([1, 2, 3, 4, 9])
  })

  it('Shift+화살표는 다음·이전 중요 이벤트로 가고, 끝에서 돌지 않는다', () => {
    const markers = markersOf(summary, null, 'summary')
    expect(nextMarker(markers, 0, 1)).toBe(2)
    expect(nextMarker(markers, 2, 1)).toBe(4)
    expect(nextMarker(markers, 4, 1)).toBe(4)
    expect(nextMarker(markers, 4, -1)).toBe(2)
    expect(nextMarker(markers, 2, -1)).toBe(2)
  })

  it('요약 재생은 중요 이벤트 사이를 건너뛰고, 마지막 뒤에는 끝으로 간다', () => {
    const markers = markersOf(summary, null, 'summary')
    expect(nextPlayStep(markers, 0, 10, 'summary')).toBe(2)
    expect(nextPlayStep(markers, 4, 10, 'summary')).toBe(10)
    expect(nextPlayStep(markers, 10, 10, 'summary')).toBeNull()
    expect(nextPlayStep(markers, 4, 10, 'all')).toBe(5)
  })
})

group('코드 ↔ 걸음', () => {
  const events = [
    event(1, 'COMPARE', '0', null, 2, 4),
    event(2, 'SWAP', '0', '2', 2, 5),
    event(3, 'COMPARE', '1', null, 2, 4),
    event(4, 'WRITE', '2', '9', 2, 6),
  ]

  it('줄을 누르면 그 줄의 다음 걸음으로, 마지막 뒤에서는 첫 걸음으로 간다', () => {
    expect(nextOnLine(events, [], 4, 0)).toBe(1)
    expect(nextOnLine(events, [], 4, 1)).toBe(3)
    expect(nextOnLine(events, [], 4, 3)).toBe(1)
    expect(nextOnLine(events, [], 99, 0)).toBeNull()
  })

  it('받지 않은 구간은 요약에서 찾는다', () => {
    expect(nextOnLine(events, [event(700, 'MATCH', '3', null, 3, 12)], 12, 4)).toBe(700)
  })

  it('고른 칸을 마지막으로 건드린 걸음과 그 줄들 — 교환의 상대 칸도 센다', () => {
    const cell2 = targetKey('ARRAY', '2')
    expect(lastTouch(events, 3, cell2)?.seq).toBe(2)
    expect(lastTouch(events, 4, cell2)?.seq).toBe(4)
    expect(lastTouch(events, 1, cell2)).toBeNull()
    expect([...linesTouching(events, 4, cell2)].sort()).toEqual([5, 6])
  })
})

group('텍스트 폴백 (디자인 설계서 §8.5)', () => {
  it('받침에 맞춰 조사를 붙인다', () => {
    expect(josa('인덱스 3', '을/를')).toBe('인덱스 3을')
    expect(josa('인덱스 2', '을/를')).toBe('인덱스 2를')
    expect(josa('정점', '이/가')).toBe('정점이')
    expect(josa('left', '을/를')).toBe('left을(를)')
  })

  it('이벤트마다 한 문장이 있다', () => {
    expect(describe(event(1, 'COMPARE', '3', null))).toBe('인덱스 3을 비교했습니다')
    expect(describe(event(1, 'SWAP', '3', '4'))).toBe('인덱스 3과 4를 맞바꿨습니다')
    expect(describe(event(1, 'RETURN', 'fib(2)', '1'))).toBe('fib(2)이(가) 1을 돌려줬습니다')
  })

  it('분기 진단의 걸음 문자열을 같은 문장으로 되돌린다', () => {
    expect(describeDivergenceStep('COMPARE array[3] → 5')).toBe('인덱스 3을 비교했습니다')
    expect(describeDivergenceStep('POINTER [1] → left')).toBe('포인터 left을(를) 인덱스 1에 두었습니다')
    expect(describeDivergenceStep('내 풀이는 여기서 끝났다')).toBe('내 풀이는 여기서 끝났다')
  })
})

function event(
  seq: number,
  eventType: TraceEventType,
  targetRef: string,
  after: string | null,
  importance = 1,
  sourceLine: number | null = null,
): TraceEvent {
  return {
    seq,
    logicalTime: seq,
    eventType,
    targetKind: eventType === 'NODE' || eventType === 'EDGE' ? 'GRAPH' : 'ARRAY',
    targetRef,
    before: null,
    after,
    importance,
    sourceLine,
    attributes: {},
  }
}
