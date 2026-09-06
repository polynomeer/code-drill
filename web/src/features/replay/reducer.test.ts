import { describe, expect, it } from 'vitest'
import { applyAll, emptyState } from './reducer'
import type { TraceEvent, TraceEventType } from './traceTypes'

/**
 * 시각화 검증 (기술 설계서 §14.3).
 *
 * 리듀서가 지켜야 하는 것은 두 가지다.
 * - 임의 위치로 간 상태가 처음부터 재생한 상태와 같아야 한다.
 * - 모르는 이벤트를 아는 척 그리지 않아야 한다.
 *
 * 첫 번째가 깨지면 사용자는 슬라이더를 움직였다는 이유만으로 다른 그림을 보게 된다.
 */
describe('replay reducer', () => {
  it('임의 위치의 상태가 처음부터 재생한 상태와 같다', () => {
    const events = mixedTrace(400)

    for (const position of [0, 1, 37, 199, 200, 399, 400]) {
      const direct = applyAll(events, position)
      // "처음부터 한 걸음씩" 과 "한 번에 position 까지" 가 같아야 한다.
      let stepwise = emptyState()
      for (let i = 0; i < position; i += 1) stepwise = applyAll(events, i + 1)

      expect(serialize(direct), `position=${position}`).toEqual(serialize(stepwise))
    }
  })

  it('배열 이벤트가 커서와 표시를 남긴다', () => {
    const state = applyAll(
      [event(1, 'VISIT', '2', '7'), event(2, 'COMPARE', '3', '9'), event(3, 'MATCH', '2', '3')],
      3,
    )

    expect(state.array.cursor).toBe(3)
    expect([...state.array.visited]).toEqual([2])
    expect([...state.array.matched].sort()).toEqual([2, 3])
  })

  it('스택과 큐가 순서를 지킨다', () => {
    const state = applyAll(
      [
        event(1, 'PUSH', 'top', '1'),
        event(2, 'PUSH', 'top', '2'),
        event(3, 'POP', 'top', '2'),
        event(4, 'ENQUEUE', 'tail', 'a'),
        event(5, 'ENQUEUE', 'tail', 'b'),
        event(6, 'DEQUEUE', 'head', 'a'),
      ],
      6,
    )

    expect(state.stack).toEqual(['1'])
    expect(state.queue).toEqual(['b'])
  })

  it('재귀 호출은 가장 안쪽 프레임부터 닫힌다', () => {
    const state = applyAll(
      [
        event(1, 'CALL', 'fib(3)'),
        event(2, 'CALL', 'fib(2)'),
        event(3, 'RETURN', 'fib(2)', '1'),
        event(4, 'RETURN', 'fib(3)', '2'),
      ],
      4,
    )

    expect(state.calls.map((frame) => [frame.label, frame.result])).toEqual([
      ['fib(3)', '2'],
      ['fib(2)', '1'],
    ])
  })

  it('그래프는 간선이 정점을 함께 만든다', () => {
    const state = applyAll([event(1, 'EDGE', 'a', 'b')], 1)

    expect([...state.graph.nodes].sort()).toEqual(['a', 'b'])
    expect(state.graph.edges).toEqual([['a', 'b']])
  })

  it('모르는 이벤트는 상태에 반영하지 않고 센다', () => {
    const unknown = { ...event(1, 'VISIT', '1'), eventType: 'FUTURE_KIND' as TraceEventType }

    const state = applyAll([unknown], 1)

    expect(state.unknown).toBe(1)
    expect(state.array.visited.size).toBe(0)
  })
})

function event(
  seq: number,
  eventType: TraceEventType,
  targetRef: string,
  after: string | null = null,
): TraceEvent {
  const kind =
    eventType === 'PUSH' || eventType === 'POP'
      ? 'STACK'
      : eventType === 'ENQUEUE' || eventType === 'DEQUEUE'
        ? 'QUEUE'
        : eventType === 'NODE' || eventType === 'EDGE'
          ? 'GRAPH'
          : eventType === 'CALL' || eventType === 'RETURN'
            ? 'CALL'
            : 'ARRAY'

  return {
    seq,
    logicalTime: seq,
    eventType,
    targetKind: kind,
    targetRef,
    before: null,
    after,
    importance: 1,
    sourceLine: null,
    attributes: {},
  }
}

/** 다섯 종류를 섞어 만든다. 한 종류만으로는 seek 불변식을 충분히 흔들지 못한다. */
function mixedTrace(count: number): TraceEvent[] {
  const types: TraceEventType[] = [
    'VISIT',
    'COMPARE',
    'PUSH',
    'POP',
    'ENQUEUE',
    'DEQUEUE',
    'NODE',
    'EDGE',
    'CALL',
    'RETURN',
    'WRITE',
    'POINTER',
  ]
  return Array.from({ length: count }, (_, i) => {
    const type = types[i % types.length] as TraceEventType
    const ref = type === 'NODE' || type === 'EDGE' || type === 'CALL' || type === 'RETURN'
      ? `n${i % 7}`
      : String(i % 12)
    return event(i + 1, type, ref, String(i % 5))
  })
}

/** Set/Map 을 비교 가능한 형태로 편다. */
function serialize(state: ReturnType<typeof emptyState>) {
  return {
    visited: [...state.array.visited].sort(),
    compared: [...state.array.compared].sort(),
    matched: [...state.array.matched].sort(),
    written: [...state.array.written.entries()].sort(),
    cursor: state.array.cursor,
    pointers: [...state.array.pointers.entries()].sort(),
    stack: state.stack,
    queue: state.queue,
    nodes: [...state.graph.nodes].sort(),
    edges: state.graph.edges,
    current: state.graph.current,
    calls: state.calls,
    unknown: state.unknown,
  }
}
