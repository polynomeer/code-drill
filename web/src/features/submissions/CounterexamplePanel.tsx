import { useEffect, useRef, useState } from 'react'
import { getCounterexample, startCounterexample } from '../../api/client'
import type { Counterexample } from '../../shared/types'

/**
 * 최소 반례 (기술 설계서 §6.3, PRD §3.4 검증군 증거).
 *
 * **떨어진 입력 그대로는 배울 것이 없다.** 20만 원소에서 틀렸다는 사실은 원인을 가리키지
 * 않지만, 원소 하나로 줄인 입력은 대개 원인 그 자체다.
 *
 * 사용자가 눌러야 돈다. 판정마다 자동으로 돌리면 채점 한 번에 이 시스템에서 가장 비싼
 * 작업이 하나씩 붙고, 그중 대부분은 아무도 열어 보지 않는다.
 */
export function CounterexamplePanel({ submissionId }: { submissionId: string }) {
  const [result, setResult] = useState<Counterexample | null>(null)
  const [error, setError] = useState<string | null>(null)
  const timer = useRef<number | null>(null)

  useEffect(() => () => { if (timer.current) window.clearTimeout(timer.current) }, [])

  useEffect(() => {
    setResult(null)
    setError(null)
    // 이미 돌려 본 적이 있으면 그 결과를 보여준다. 다시 누르게 하면 사용자는 가장 비싼
    // 작업을 두 번 걸려 든다.
    getCounterexample(submissionId).then(setResult).catch(() => setResult(null))
  }, [submissionId])

  const poll = () => {
    timer.current = window.setTimeout(async () => {
      const next = await getCounterexample(submissionId).catch(() => null)
      setResult(next)
      if (!next || next.status === 'PENDING') poll()
    }, 3000)
  }

  const start = async () => {
    setError(null)
    try {
      setResult(await startCounterexample(submissionId))
      poll()
    } catch (e) {
      setError(e instanceof Error ? e.message : '축소를 걸지 못했습니다')
    }
  }

  if (!result) {
    return (
      <div className="counterexample">
        <button type="button" onClick={() => void start()}>
          가장 작은 반례 찾기
        </button>
        <p className="muted small">
          떨어진 입력을 더 이상 줄일 수 없을 때까지 줄여, 무엇이 원인인지 드러나게 합니다.
          몇십 초 걸립니다.
        </p>
        {error && <p className="warn small">{error}</p>}
      </div>
    )
  }

  if (result.status === 'PENDING') {
    return <p className="muted small">반례를 줄이는 중… (몇십 초 걸립니다)</p>
  }

  if (result.status === 'NOT_REPRODUCED') {
    // 실패가 아니다. 값이 아니라 시간·메모리로 떨어진 경우가 그렇다.
    return (
      <p className="muted small">
        작은 입력에서는 재현되지 않았습니다. 값이 아니라 시간이나 메모리로 떨어졌을 수
        있습니다.
      </p>
    )
  }

  if (result.status === 'FAILED') {
    return <p className="warn small">{result.message ?? '축소가 돌지 않았습니다'} — 제출 문제가 아닙니다.</p>
  }

  return (
    <div className="counterexample">
      <p className="small">
        원소 {result.originalSize}개에서 <strong>{result.minimalSize}개</strong>까지 줄였습니다.
        {result.rounds !== null && <span className="muted"> ({result.rounds}라운드)</span>}
      </p>
      <ul className="divergence-steps">
        <li>
          <span className="muted small">입력</span>
          <span className="mono small">{JSON.stringify(result.args)}</span>
        </li>
        <li>
          <span className="muted small">내 출력</span>
          <span className="mono small">{result.actual}</span>
        </li>
        <li>
          <span className="muted small">정답</span>
          <span className="mono small">{result.expected}</span>
        </li>
      </ul>
      <p className="muted small">
        이 입력을 테스트 패널에 넣고 고쳐 보세요.
      </p>
    </div>
  )
}
