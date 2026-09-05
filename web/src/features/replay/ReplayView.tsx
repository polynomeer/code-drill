import { useState } from 'react'
import type { TraceCapture, TraceEventType } from '../../shared/types'

const TYPE_LABEL: Record<TraceEventType, string> = {
  VISIT: '살펴봄',
  COMPARE: '짝을 찾아봄',
  MATCH: '답을 찾음',
}

/**
 * 실행 리플레이 (기술 설계서 §7.5, 디자인 설계서 §0.2 관찰 가능).
 *
 * 이벤트를 처음부터 적용해 현재 상태를 복원한다. 원본 설계의 checkpoint 기반 seek 은
 * 아직 없다 — 이벤트 수가 예산(1,000) 안이라 전체 재생이 충분히 싸다.
 */
export function ReplayView({ capture, input }: { capture: TraceCapture; input: number[] }) {
  const [step, setStep] = useState(0)

  if (capture.events.length === 0) {
    return (
      <section className="panel">
        <h3>실행 리플레이</h3>
        <p className="muted">{capture.diagnostics ?? '트레이스가 없습니다.'}</p>
      </section>
    )
  }

  const applied = capture.events.slice(0, step)
  const current = capture.events[step - 1]
  const indexOf = (target: string) => Number(target.split(':')[1] ?? -1)

  const visited = new Set(applied.filter((e) => e.eventType === 'VISIT').map((e) => indexOf(e.target)))
  const matched = new Set(
    applied
      .filter((e) => e.eventType === 'MATCH')
      .flatMap((e) => [indexOf(e.target), Number(e.after)]),
  )
  const cursor = current ? indexOf(current.target) : -1

  return (
    <section className="panel">
      <h3>
        실행 리플레이 <span className="muted">{capture.caseId}</span>
      </h3>

      <div className="cells">
        {input.map((value, index) => {
          const state = matched.has(index)
            ? 'matched'
            : index === cursor
              ? 'cursor'
              : visited.has(index)
                ? 'visited'
                : ''
          return (
            <div key={index} className={`cell ${state}`}>
              <span className="cell-index">{index}</span>
              <span className="cell-value">{value}</span>
            </div>
          )
        })}
      </div>

      <div className="controls">
        <button onClick={() => setStep(0)} disabled={step === 0}>
          처음
        </button>
        <button onClick={() => setStep((s) => Math.max(0, s - 1))} disabled={step === 0}>
          이전
        </button>
        <input
          type="range"
          min={0}
          max={capture.events.length}
          value={step}
          onChange={(e) => setStep(Number(e.target.value))}
          aria-label="재생 위치"
        />
        <button
          onClick={() => setStep((s) => Math.min(capture.events.length, s + 1))}
          disabled={step === capture.events.length}
        >
          다음
        </button>
        <span className="muted">
          {step} / {capture.events.length}
        </span>
      </div>

      <p className="event-line">
        {current
          ? `#${current.seq} ${TYPE_LABEL[current.eventType]} — ${current.target}${
              current.after ? ` → ${current.after}` : ''
            }`
          : '재생 전'}
      </p>

      {capture.truncated && <p className="warn">이벤트 예산을 넘겨 이후가 잘렸습니다.</p>}
    </section>
  )
}
