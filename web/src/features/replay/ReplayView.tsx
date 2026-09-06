import { useEffect, useMemo, useRef, useState } from 'react'
import { applyAll } from './reducer'
import { EventFallback, renderersFor } from './renderers'
import { kindsIn, useTrace } from './useTrace'
import { schemaSupported } from './traceTypes'
import type { TraceManifest } from './traceTypes'

const TYPE_LABEL: Record<string, string> = {
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
 * 실행 리플레이 (기술 설계서 §7.5, 디자인 설계서 §0.2 관찰 가능).
 *
 * manifest 와 요약을 먼저 받아 타임라인을 그리고, 상세 이벤트는 현재 위치 주변만
 * 내려받는다. 스키마를 모르거나 상태를 그릴 수 없으면 텍스트 이벤트 목록으로 폴백한다.
 *
 * 키보드로도 조작할 수 있어야 한다 (§14.3 접근성). ←/→ 는 한 걸음, Home/End 는 처음과 끝.
 */
export function ReplayView({
  submissionId,
  manifest,
  input,
}: {
  submissionId: string
  manifest: TraceManifest
  input: number[]
}) {
  const [step, setStep] = useState(0)
  const { events, error, ensureLoaded } = useTrace(submissionId, manifest)
  const container = useRef<HTMLElement>(null)

  useEffect(() => {
    void ensureLoaded(step)
  }, [step, ensureLoaded])

  useEffect(() => setStep(0), [manifest.traceId])

  const state = useMemo(() => applyAll(events, step), [events, step])
  const kinds = useMemo(() => kindsIn(events, manifest), [events, manifest])
  const renderers = useMemo(() => renderersFor(kinds), [kinds])
  const current = step > 0 ? events[step - 1] : null

  if (manifest.status === 'EMPTY') {
    return (
      <section className="panel">
        <h3>실행 리플레이</h3>
        <p className="muted">{manifest.diagnostics ?? '트레이스가 없습니다.'}</p>
      </section>
    )
  }

  if (manifest.status === 'INVALID' || !schemaSupported(manifest.schemaVersion)) {
    return (
      <section className="panel">
        <h3>실행 리플레이</h3>
        <EventFallback
          events={manifest.summary}
          reason={
            manifest.status === 'INVALID'
              ? `트레이스를 신뢰할 수 없어 상태를 그리지 않습니다: ${manifest.diagnostics ?? ''}`
              : `이 버전(${manifest.schemaVersion})을 아직 그릴 수 없습니다.`
          }
        />
      </section>
    )
  }

  const total = manifest.eventCount
  const clamp = (next: number) => Math.max(0, Math.min(total, next))

  return (
    <section
      className="panel"
      ref={container}
      tabIndex={0}
      role="group"
      aria-label="실행 리플레이"
      onKeyDown={(event) => {
        const moves: Record<string, number | undefined> = {
          ArrowRight: step + 1,
          ArrowLeft: step - 1,
          Home: 0,
          End: total,
        }
        const next = moves[event.key]
        if (next === undefined) return
        event.preventDefault()
        setStep(clamp(next))
      }}
    >
      <h3>
        실행 리플레이 <span className="muted">{manifest.caseId}</span>
      </h3>

      {error && <p className="warn">{error}</p>}

      {renderers.map((renderer) => (
        <div key={renderer.kind} className="renderer">
          <h4>{renderer.title}</h4>
          {renderer.render(state, input)}
        </div>
      ))}

      {state.unknown > 0 && (
        <p className="warn">해석하지 못한 이벤트 {state.unknown}건은 상태에 반영하지 않았습니다.</p>
      )}

      <div className="controls">
        <button onClick={() => setStep(0)} disabled={step === 0}>
          처음
        </button>
        <button onClick={() => setStep(clamp(step - 1))} disabled={step === 0}>
          이전
        </button>
        <input
          type="range"
          min={0}
          max={total}
          value={step}
          onChange={(event) => setStep(clamp(Number(event.target.value)))}
          aria-label="재생 위치"
        />
        <button onClick={() => setStep(clamp(step + 1))} disabled={step >= total}>
          다음
        </button>
        <span className="muted mono">
          {step} / {total}
        </span>
      </div>

      <p className="event-line">
        {current
          ? `#${current.seq} ${TYPE_LABEL[current.eventType] ?? current.eventType} — ${
              current.targetRef
            }${current.after ? ` → ${current.after}` : ''}`
          : '재생 전'}
        {step > events.length && <span className="muted"> (구간을 불러오는 중…)</span>}
      </p>

      {manifest.truncated && (
        <p className="warn">이벤트 예산을 넘겨 이후가 잘렸습니다. 요약만 완전합니다.</p>
      )}
    </section>
  )
}
