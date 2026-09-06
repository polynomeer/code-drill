import type { TraceEvent } from './traceTypes'

/**
 * 리플레이 상태 (기술 설계서 §7.5).
 *
 * 이벤트를 처음부터 적용해 만든다. **리듀서는 이 하나뿐이다** — 서버가 상태 스냅샷을
 * 만들려면 같은 리듀서를 Kotlin 으로 한 번 더 구현해야 하고, 두 구현이 갈라지는 순간
 * "seek 한 결과와 처음부터 재생한 결과가 다르다"는 버그가 생긴다. 그래서 서버는 청크
 * 경계만 목차로 주고, 상태 복원은 여기서만 한다.
 */
export interface ReplayState {
  /** 배열 셀 상태. 인덱스별로 마지막 이벤트가 남긴 흔적이다. */
  array: {
    visited: Set<number>
    compared: Set<number>
    matched: Set<number>
    written: Map<number, string>
    cursor: number | null
    /** 이름 붙은 포인터. 투 포인터 풀이를 되짚을 때 쓴다. */
    pointers: Map<string, number>
  }
  stack: string[]
  queue: string[]
  graph: {
    nodes: Set<string>
    edges: [string, string][]
    /** 마지막으로 방문한 정점. */
    current: string | null
  }
  /** 재귀 호출 스택. 들어간 순서대로 쌓이고 반환하면 결과가 붙는다. */
  calls: { label: string; result: string | null }[]
  /** 해석하지 못한 이벤트. 조용히 버리지 않고 세어 사용자에게 알린다. */
  unknown: number
}

export function emptyState(): ReplayState {
  return {
    array: {
      visited: new Set(),
      compared: new Set(),
      matched: new Set(),
      written: new Map(),
      cursor: null,
      pointers: new Map(),
    },
    stack: [],
    queue: [],
    graph: { nodes: new Set(), edges: [], current: null },
    calls: [],
    unknown: 0,
  }
}

/**
 * 이벤트 하나를 적용한다.
 *
 * 상태를 제자리에서 고친다. 이벤트가 수만 건이 될 수 있어 매번 새 객체를 만들면 재생이
 * 눈에 띄게 느려진다. 대신 [applyAll] 이 항상 빈 상태에서 시작하도록 해 공유 상태가
 * 밖으로 새지 않게 한다.
 */
export function apply(state: ReplayState, event: TraceEvent): ReplayState {
  const index = Number(event.targetRef)

  switch (event.eventType) {
    case 'VISIT':
      state.array.visited.add(index)
      state.array.cursor = index
      break

    case 'COMPARE':
      state.array.compared.add(index)
      state.array.cursor = index
      break

    case 'SWAP': {
      // after 는 상대 인덱스다. 둘 다 표시해야 무엇과 바뀌었는지 보인다.
      const other = Number(event.after)
      state.array.compared.add(index)
      if (Number.isFinite(other)) state.array.compared.add(other)
      state.array.cursor = index
      break
    }

    case 'WRITE':
      state.array.written.set(index, event.after ?? '')
      state.array.cursor = index
      break

    case 'POINTER':
      // after 가 포인터 이름이다. 같은 이름은 옮겨 다니므로 덮어쓴다.
      if (event.after) state.array.pointers.set(event.after, index)
      break

    case 'MATCH': {
      const other = Number(event.after)
      state.array.matched.add(index)
      if (Number.isFinite(other)) state.array.matched.add(other)
      break
    }

    case 'PUSH':
      state.stack.push(event.after ?? '')
      break

    case 'POP':
      state.stack.pop()
      break

    case 'ENQUEUE':
      state.queue.push(event.after ?? '')
      break

    case 'DEQUEUE':
      state.queue.shift()
      break

    case 'NODE':
      state.graph.nodes.add(event.targetRef)
      state.graph.current = event.targetRef
      break

    case 'EDGE':
      state.graph.nodes.add(event.targetRef)
      if (event.after) {
        state.graph.nodes.add(event.after)
        state.graph.edges.push([event.targetRef, event.after])
      }
      break

    case 'CALL':
      state.calls.push({ label: event.targetRef, result: null })
      break

    case 'RETURN': {
      // 가장 안쪽의 같은 라벨 호출에 결과를 붙이고 닫는다.
      for (let i = state.calls.length - 1; i >= 0; i -= 1) {
        const frame = state.calls[i]
        if (frame && frame.label === event.targetRef && frame.result === null) {
          frame.result = event.after ?? ''
          break
        }
      }
      break
    }

    default:
      // 모르는 이벤트를 아는 척 그리지 않는다 (§7.5 폴백).
      state.unknown += 1
      break
  }

  return state
}

/** 앞에서부터 [count] 개를 적용한 상태. 항상 빈 상태에서 시작한다. */
export function applyAll(events: TraceEvent[], count: number): ReplayState {
  const state = emptyState()
  for (let i = 0; i < Math.min(count, events.length); i += 1) {
    const event = events[i]
    if (event) apply(state, event)
  }
  return state
}
