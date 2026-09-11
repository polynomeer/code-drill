import { useEffect, useState } from 'react'
import { getPrescription, skipPrescribed } from '../../api/client'
import { COMPETENCY_LABEL, REASON_LABEL } from '../../shared/types'
import type { Prescription } from '../../shared/types'

/**
 * 오늘의 처방 (PRD FR-808, 기획서 §8.1).
 *
 * > 일일 처방과 주간 리포트는 약점·재발·성장을 구체적인 행동으로 설명합니다. 추천 이유와
 * > 다음 측정 시점을 표시하며 사용자가 조정할 수 있습니다.
 *
 * 이유가 문제보다 크게 보인다. "왜 이걸 풀어야 하나"에 답하지 못하는 추천은 건너뛰어지고,
 * 이 화면의 목적은 문제를 보여주는 것이 아니라 그 답을 보여주는 것이다.
 *
 * **밀어낼 수 있다.** 오늘은 안 하겠다는 것과 영영 안 하겠다는 것은 다르고, 밀어낸 자리는
 * 다음 후보로 채워진다.
 */
export function TodayPanel({
  onOpenProblem,
  refreshKey,
}: {
  onOpenProblem: (problemId: string) => void
  /** 제출이 끝날 때마다 바뀐다. 방금 푼 문제가 처방에 남아 있으면 안 된다. */
  refreshKey: number
}) {
  const [prescription, setPrescription] = useState<Prescription | null>(null)

  useEffect(() => {
    getPrescription().then(setPrescription).catch(() => setPrescription(null))
  }, [refreshKey])

  if (!prescription) return null

  const skip = async (problemId: string) => {
    setPrescription(await skipPrescribed(problemId).catch(() => prescription))
  }

  const { streak } = prescription

  return (
    <section className="panel today">
      <div className="problem-head">
        <h3>오늘</h3>
        {/* 스트릭은 숫자 하나로 충분하다. 다만 어제를 빠뜨렸으면 오늘이 위험하다고 미리
            말한다 — 끊긴 뒤에 알려 주는 것은 알려 주는 것이 아니다. */}
        <span className="muted small">
          {streak.days > 0 ? `${streak.days}일째` : '아직 시작 전'}
          {streak.atRisk && ' · 오늘 안 하면 끊깁니다'}
          {streak.activeToday && ' ✓'}
        </span>
      </div>

      {prescription.items.length === 0 ? (
        <p className="muted small">오늘 권할 것이 없습니다. 아무 문제나 골라 풀어도 됩니다.</p>
      ) : (
        <ul className="prescription">
          {prescription.items.map((item) => (
            <li key={item.problemId}>
              <div className="prescription-head">
                <span className="reason">{REASON_LABEL[item.reason]}</span>
                <button type="button" className="linklike mono" onClick={() => onOpenProblem(item.problemId)}>
                  {item.problemId}
                </button>
                {item.competency && (
                  <span className="muted small">{COMPETENCY_LABEL[item.competency] ?? item.competency}</span>
                )}
                <button
                  type="button"
                  className="linklike small"
                  onClick={() => void skip(item.problemId)}
                  aria-label={`${item.problemId} 오늘은 건너뛰기`}
                >
                  오늘은 넘기기
                </button>
              </div>
              <p className="small">{item.detail}</p>
              <p className="muted small">
                다음 측정: {new Date(item.nextMeasurement).toLocaleDateString()}
              </p>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
