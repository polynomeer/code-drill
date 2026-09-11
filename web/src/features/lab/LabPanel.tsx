import { useEffect, useMemo, useRef, useState } from 'react'
import { getLab, startLab } from '../../api/client'
import type { ApproachResult, LabRun } from '../../shared/types'
import { applyAll } from '../replay/reducer'
import { renderersFor } from '../replay/renderers'
import type { TargetKind } from '../replay/traceTypes'

const MINE = '내 풀이'

/**
 * 비교 경기장과 실험실 (기획서 §6.5, §6.6).
 *
 * 한 입력에 여러 풀이를 돌려 나란히 놓는다. 비교 축(§6.5) 중 여기서 그리는 것은
 * **연산량**(이벤트 종류별 횟수), **메모리**, **상태 방문**(같은 시간축의 리플레이),
 * **최초 선택 차이**(이벤트 열이 처음 갈라지는 자리)다.
 *
 * 시간은 참고로만 적는다. 계측 실행의 시간에는 계측 비용이 섞여 있고, 작은 입력에서는
 * 그 비용이 알고리즘 차이보다 크다. 수십 ms 의 차이로 "이쪽이 빠르다"를 말하면 거짓말이다.
 *
 * 시간축은 하나다. 슬라이더 하나가 모든 풀이의 재생 위치를 함께 옮긴다 — "여러 알고리즘을
 * 동시에 실행하고 시간축을 맞춘다"(§6.6).
 */
export function LabPanel({ problemId, problemSignature, approaches, canUseMine, sampleArgs }: {
  problemId: string
  problemSignature: string
  approaches: string[]
  canUseMine: boolean
  sampleArgs: unknown[] | null
}) {
  const choices = useMemo(() => (canUseMine ? [...approaches, MINE] : approaches), [approaches, canUseMine])
  const [selected, setSelected] = useState<string[]>([])
  const [text, setText] = useState('')
  const [run, setRun] = useState<LabRun | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [step, setStep] = useState(0)
  const timer = useRef<number | null>(null)

  useEffect(() => () => { if (timer.current) window.clearTimeout(timer.current) }, [])

  useEffect(() => {
    setRun(null)
    setStep(0)
    setText(sampleArgs ? JSON.stringify(sampleArgs) : '')
    // 참조와 내 풀이를 기본으로 고른다. 그 둘이 §6.4 "공식 풀이와의 공통점·분기점"이다.
    setSelected(choices.filter((c) => c === choices[0] || c === MINE))
  }, [problemId, choices, sampleArgs])

  const toggle = (label: string) =>
    setSelected((prev) => (prev.includes(label) ? prev.filter((l) => l !== label) : [...prev, label]))

  const poll = (id: string) => {
    timer.current = window.setTimeout(async () => {
      const next = await getLab(id).catch(() => null)
      if (next) setRun(next)
      if (!next || next.status === 'PENDING') poll(id)
    }, 2000)
  }

  const start = async () => {
    setError(null)
    let args: unknown
    try {
      args = JSON.parse(text)
    } catch {
      setError('입력을 JSON 배열로 읽지 못했습니다')
      return
    }
    if (!Array.isArray(args)) {
      setError('입력은 인자 배열이어야 합니다')
      return
    }
    try {
      const started = await startLab(problemId, args, selected)
      setRun(started)
      setStep(0)
      poll(started.id)
    } catch (e) {
      setError(e instanceof Error ? e.message : '실행하지 못했습니다')
    }
  }

  const results = run?.status === 'COMPLETED' ? run.results : []
  const longest = Math.max(0, ...results.map((r) => r.events.length))
  const divergence = useMemo(() => firstDivergence(results), [results])

  return (
    <div className="lab">
      <h4>실험실</h4>
      <p className="muted small">
        입력을 바꿔 여러 풀이를 같은 시간축에서 나란히 봅니다 — {problemSignature}
      </p>

      <div className="filters">
        <fieldset>
          {choices.map((label) => (
            <button
              key={label}
              type="button"
              className={selected.includes(label) ? 'chip on' : 'chip'}
              aria-pressed={selected.includes(label)}
              onClick={() => toggle(label)}
            >
              {label}
            </button>
          ))}
        </fieldset>
      </div>

      <input
        className="rationale mono"
        value={text}
        onChange={(e) => setText(e.target.value)}
        aria-label="실험 입력"
        placeholder="[인자들]"
      />
      <button type="button" onClick={() => void start()} disabled={selected.length === 0 || run?.status === 'PENDING'}>
        {run?.status === 'PENDING' ? '돌리는 중…' : '나란히 돌리기'}
      </button>
      {error && <p className="warn small">{error}</p>}

      {results.length > 0 && (
        <>
          <table className="groups">
            <thead>
              <tr>
                <th>풀이</th>
                <th>출력</th>
                <th>연산량</th>
                <th>메모리</th>
                <th>시간 (참고)</th>
              </tr>
            </thead>
            <tbody>
              {results.map((r) => (
                <tr key={r.label}>
                  <td>{r.label}</td>
                  <td className="mono">{r.verdict === 'ACCEPTED' ? r.actual : r.verdict}</td>
                  <td className="mono small">{describeCounts(r)}</td>
                  <td className="mono small">{Math.round(r.measurements.peakMemoryBytes / 1024)}KB</td>
                  <td className="mono small muted">{r.measurements.wallTimeMillis}ms</td>
                </tr>
              ))}
            </tbody>
          </table>

          {divergence !== null && (
            <p className="small">
              {divergence === 0
                ? '첫 걸음부터 다른 길입니다.'
                : `${divergence}걸음까지 같은 일을 하다가 그 다음에서 갈립니다.`}
              {' '}
              <button type="button" className="linklike" onClick={() => setStep(divergence + 1)}>
                그 지점으로
              </button>
            </p>
          )}

          {longest > 0 && (
            <div className="controls">
              <button onClick={() => setStep(0)} disabled={step === 0}>처음</button>
              <button onClick={() => setStep(Math.max(0, step - 1))} disabled={step === 0}>이전</button>
              <input
                type="range" min={0} max={longest} value={step}
                onChange={(e) => setStep(Number(e.target.value))}
                aria-label="공통 재생 위치"
              />
              <button onClick={() => setStep(Math.min(longest, step + 1))} disabled={step >= longest}>다음</button>
              <span className="muted mono">{step} / {longest}</span>
            </div>
          )}

          <div className="arena">
            {results.map((r) => (
              <ApproachView key={r.label} result={r} step={step} input={firstArrayOf(run?.args)} />
            ))}
          </div>
        </>
      )}
    </div>
  )
}

function ApproachView({ result, step, input }: { result: ApproachResult; step: number; input: number[] }) {
  const state = useMemo(() => applyAll(result.events, Math.min(step, result.events.length)), [result.events, step])
  const kinds = useMemo(() => new Set<TargetKind>(result.events.map((e) => e.targetKind)), [result.events])
  const renderers = useMemo(() => renderersFor(kinds), [kinds])
  const current = step > 0 ? result.events[Math.min(step, result.events.length) - 1] : null
  return (
    <div className="arena-column">
      <h5>{result.label}</h5>
      {result.events.length === 0 ? (
        <p className="muted small">계측 호출이 없어 상태를 그릴 수 없습니다.</p>
      ) : (
        renderers.map((renderer) => (
          <div key={renderer.kind} className="renderer">{renderer.render(state, input)}</div>
        ))
      )}
      <p className="event-line small">
        {current ? `#${current.seq} ${current.eventType} — ${current.targetRef}${current.after ? ` → ${current.after}` : ''}` : '재생 전'}
        {step > result.events.length && result.events.length > 0 && <span className="muted"> (끝남)</span>}
      </p>
    </div>
  )
}

function describeCounts(r: ApproachResult): string {
  const entries = Object.entries(r.eventCounts)
  if (entries.length === 0) return '계측 없음'
  return entries.map(([type, n]) => `${type} ${n}`).join(' · ') + (r.truncated ? ' (잘림)' : '')
}

/**
 * 첫 두 풀이의 이벤트 열이 처음 갈라지는 자리 (§6.5 최초 선택 차이).
 *
 * 서버의 분기 진단과 같은 계약이다 — (종류, 대상, 결과) 셋. seq 는 비교하지 않는다.
 */
function firstDivergence(results: ApproachResult[]): number | null {
  const a = results[0]
  const b = results[1]
  if (!a || !b) return null
  const key = (e: ApproachResult['events'][number]) => `${e.eventType}|${e.targetRef}|${e.after ?? ''}`
  const n = Math.min(a.events.length, b.events.length)
  for (let i = 0; i < n; i++) {
    const x = a.events[i]
    const y = b.events[i]
    if (!x || !y || key(x) !== key(y)) return i
  }
  return a.events.length === b.events.length ? null : n
}

function firstArrayOf(args: unknown[] | undefined): number[] {
  const first = args?.find((a) => Array.isArray(a))
  return Array.isArray(first) ? first.filter((x): x is number => typeof x === 'number') : []
}
