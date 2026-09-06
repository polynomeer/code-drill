import type { ReplayState } from './reducer'
import type { TargetKind, TraceEvent } from './traceTypes'

/**
 * 렌더러 레지스트리 (기술 설계서 §7.5).
 *
 * `targetKind` 로 고른다. 트레이스에 등장한 종류만 그리므로, 배열만 계측한 풀이에 빈
 * 스택·큐 상자가 따라붙지 않는다.
 *
 * 등록되지 않은 종류는 그리지 않고 [EventFallback] 이 텍스트로 보여준다. 모르는 것을
 * 아는 척 그리면 사용자가 자기 코드를 오해한다.
 */
export interface Renderer {
  kind: TargetKind
  title: string
  render: (state: ReplayState, input: number[]) => React.ReactNode
}

/** 배열 + 포인터. 커서와 이름 붙은 포인터를 같은 칸 위에 얹는다. */
const arrayRenderer: Renderer = {
  kind: 'ARRAY',
  title: '배열',
  render: (state, input) => {
    const { visited, compared, matched, written, cursor, pointers } = state.array
    const size = Math.max(input.length, ...[...written.keys()].map((k) => k + 1), 0)
    const cells = Array.from({ length: size }, (_, index) => index)
    const pointerAt = new Map<number, string[]>()
    for (const [name, index] of pointers) {
      pointerAt.set(index, [...(pointerAt.get(index) ?? []), name])
    }

    return (
      <div className="cells">
        {cells.map((index) => {
          const state5 = matched.has(index)
            ? 'matched'
            : index === cursor
              ? 'cursor'
              : compared.has(index)
                ? 'compared'
                : visited.has(index)
                  ? 'visited'
                  : ''
          return (
            <div key={index} className={`cell ${state5}`}>
              <span className="cell-index">{index}</span>
              <span className="cell-value">{written.get(index) ?? input[index] ?? '·'}</span>
              {pointerAt.has(index) && (
                <span className="cell-pointer">{pointerAt.get(index)?.join(',')}</span>
              )}
            </div>
          )
        })}
      </div>
    )
  },
}

const stackRenderer: Renderer = {
  kind: 'STACK',
  title: '스택',
  render: (state) => (
    <div className="stack-view">
      {state.stack.length === 0 && <span className="muted">비어 있음</span>}
      {/* 위가 top 이 되도록 뒤집어 쌓는다. */}
      {[...state.stack].reverse().map((value, position) => (
        <div key={`${value}-${position}`} className={`slot ${position === 0 ? 'top' : ''}`}>
          {value}
          {position === 0 && <span className="slot-label">top</span>}
        </div>
      ))}
    </div>
  ),
}

const queueRenderer: Renderer = {
  kind: 'QUEUE',
  title: '큐',
  render: (state) => (
    <div className="queue-view">
      {state.queue.length === 0 && <span className="muted">비어 있음</span>}
      {state.queue.map((value, position) => (
        <div
          key={`${value}-${position}`}
          className={`slot ${position === 0 ? 'head' : ''} ${
            position === state.queue.length - 1 ? 'tail' : ''
          }`}
        >
          {value}
          {position === 0 && <span className="slot-label">head</span>}
        </div>
      ))}
    </div>
  ),
}

/**
 * 기초 그래프.
 *
 * 레이아웃은 원형 배치다. 힘 기반 레이아웃은 같은 그래프도 실행마다 다르게 그려져,
 * 사용자가 "왜 모양이 바뀌었지"를 먼저 묻게 된다. 결정적인 배치가 학습에 낫다.
 */
const graphRenderer: Renderer = {
  kind: 'GRAPH',
  title: '그래프',
  render: (state) => {
    const nodes = [...state.graph.nodes]
    if (nodes.length === 0) return <span className="muted">정점 없음</span>

    const radius = 70
    const center = 90
    const position = new Map(
      nodes.map((id, index) => {
        const angle = (2 * Math.PI * index) / nodes.length - Math.PI / 2
        return [id, { x: center + radius * Math.cos(angle), y: center + radius * Math.sin(angle) }]
      }),
    )

    return (
      <svg className="graph-view" viewBox="0 0 180 180" role="img" aria-label="그래프 상태">
        {state.graph.edges.map(([from, to], index) => {
          const a = position.get(from)
          const b = position.get(to)
          if (!a || !b) return null
          return (
            <line key={index} x1={a.x} y1={a.y} x2={b.x} y2={b.y} className="graph-edge" />
          )
        })}
        {nodes.map((id) => {
          const point = position.get(id)
          if (!point) return null
          return (
            <g key={id} className={id === state.graph.current ? 'graph-node current' : 'graph-node'}>
              <circle cx={point.x} cy={point.y} r={12} />
              <text x={point.x} y={point.y + 4} textAnchor="middle">
                {id}
              </text>
            </g>
          )
        })}
      </svg>
    )
  },
}

/** 재귀 호출 스택. 깊이를 들여쓰기로 보여 어디까지 내려갔는지 읽히게 한다. */
const callRenderer: Renderer = {
  kind: 'CALL',
  title: '재귀 호출',
  render: (state) => (
    <ol className="call-view">
      {state.calls.length === 0 && <span className="muted">호출 없음</span>}
      {state.calls.map((frame, depth) => (
        <li key={`${frame.label}-${depth}`} style={{ paddingLeft: `${Math.min(depth, 8) * 12}px` }}>
          <span className={frame.result === null ? 'open' : 'closed'}>{frame.label}</span>
          {frame.result !== null && <span className="muted"> → {frame.result}</span>}
        </li>
      ))}
    </ol>
  ),
}

export const RENDERERS: Renderer[] = [
  arrayRenderer,
  stackRenderer,
  queueRenderer,
  graphRenderer,
  callRenderer,
]

/** 트레이스에 실제로 등장한 종류의 렌더러만 고른다. */
export function renderersFor(kinds: Set<TargetKind>): Renderer[] {
  return RENDERERS.filter((renderer) => kinds.has(renderer.kind))
}

/**
 * 폴백 (§7.5 스키마 미지원 시 텍스트 이벤트 목록).
 *
 * 상태를 그릴 수 없을 때도 무엇이 일어났는지는 보여준다. 빈 화면은 "트레이스가 없다"와
 * 구분되지 않는다.
 */
export function EventFallback({ events, reason }: { events: TraceEvent[]; reason: string }) {
  return (
    <div>
      <p className="warn">{reason}</p>
      <ol className="event-fallback">
        {events.slice(0, 200).map((event) => (
          <li key={event.seq}>
            <span className="mono">#{event.seq}</span> {event.eventType} {event.targetRef}
            {event.after ? ` → ${event.after}` : ''}
          </li>
        ))}
      </ol>
    </div>
  )
}
