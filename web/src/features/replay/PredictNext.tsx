import { useEffect, useState } from 'react'
import { getPredictions, predictNext } from '../../api/client'
import type { StatePrediction } from '../../shared/types'
import type { TraceEvent } from './traceTypes'

/**
 * 다음 상태 예측 (PRD FR-805).
 *
 * > 리플레이 중 다음 상태 예측과 최초 분기 진단을 지원합니다. 선택·근거·결과가
 * > 코드·입력·이벤트 시점과 연결됩니다.
 *
 * **자기 코드가 다음에 무엇을 할지 모른 채로 재생을 보는 것은 관람이지 학습이 아니다.**
 * 멈춰 세우고 한 번 말하게 하면, 틀린 그 자리가 곧 자기가 몰랐던 자리다.
 *
 * 관문이 아니다. 답하지 않아도 다음으로 넘어갈 수 있고, 틀려도 아무것도 막지 않는다.
 * 한 자리는 한 번만 물어본다 — 답을 보고 다시 누르는 것은 예측이 아니라 받아쓰기다.
 */
export function PredictNext({
  submissionId,
  next,
  choices,
  labelOf,
}: {
  submissionId: string
  /** 아직 재생하지 않은 다음 이벤트. 없으면 물어볼 것이 없다. */
  next: TraceEvent | null
  /** 이 트레이스에 실제로 나오는 이벤트 종류. 보기가 여기서 나온다. */
  choices: string[]
  labelOf: (type: string) => string
}) {
  const [answered, setAnswered] = useState<StatePrediction[]>([])
  const [rationale, setRationale] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    setAnswered([])
    getPredictions(submissionId).then(setAnswered).catch(() => setAnswered([]))
  }, [submissionId])

  // 종류가 하나뿐이면 물어볼 것이 없다. 보기가 정답 하나인 질문은 질문이 아니다.
  if (!next || choices.length < 2) return null

  const already = answered.find((item) => item.step === next.seq)

  const answer = async (predicted: string) => {
    setBusy(true)
    try {
      const graded = await predictNext(submissionId, next.seq, predicted, rationale.trim() || null)
      setAnswered((prev) => [...prev.filter((item) => item.step !== graded.step), graded])
      setRationale('')
    } catch {
      // 이미 답한 자리이거나 서버가 흔들렸다. 어느 쪽이든 재생을 막지 않는다.
    } finally {
      setBusy(false)
    }
  }

  if (already) {
    return (
      <p className={already.correct ? 'ok small' : 'warn small'}>
        {/* 색만으로 알리지 않는다. 기호와 문장이 먼저 읽힌다. */}
        {already.correct ? '✓ 맞혔습니다' : `✗ ${labelOf(already.predicted)} 라고 봤지만 `}
        {!already.correct && <>실제로는 {labelOf(already.actual)}</>}
      </p>
    )
  }

  return (
    <div className="predict">
      <p className="small">다음 단계에서 무엇이 일어날까요?</p>
      <div className="filters">
        <fieldset>
          {choices.map((type) => (
            <button
              key={type}
              type="button"
              className="chip"
              disabled={busy}
              onClick={() => void answer(type)}
            >
              {labelOf(type)}
            </button>
          ))}
        </fieldset>
      </div>
      {/* 근거는 선택이다. 필수로 하면 예측 자체를 건너뛴다 (FR-803 과 같은 이유). */}
      <input
        className="rationale"
        placeholder="왜 그렇게 생각했나요? (선택)"
        value={rationale}
        onChange={(event) => setRationale(event.target.value)}
        aria-label="예측 근거"
      />
    </div>
  )
}
