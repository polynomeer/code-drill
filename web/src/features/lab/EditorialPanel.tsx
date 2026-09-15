import { useEffect, useState } from 'react'
import { getEditorial, unlockEditorial } from '../../api/client'
import type { Editorial } from '../../shared/types'
import { ArenaPanel } from '../arena/ArenaPanel'
import { SolutionsPanel } from '../discussion/SolutionsPanel'
import { LabPanel } from './LabPanel'

/**
 * 해설 (PRD FR-214, 기획서 §6.4).
 *
 * > 정답 전에는 해설을 잠그되 사용자가 명시적으로 열 수 있습니다. 열람 이력이 학습 기록에
 * > 남으며 대회 정책이 우선합니다.
 *
 * 잠금을 감추지 않는다. 해설이 있다는 것과 지금은 볼 수 없다는 것을 함께 알아야 "열기"를
 * 고를 수 있다. 여는 것은 되돌릴 수 없고 그 뒤 제출의 증거가 가벼워지므로, 누르기 전에
 * 그 사실을 말한다 — 조용히 깎으면 사용자는 자기 지도가 왜 안 오르는지 알 수 없다.
 */
export function EditorialPanel({ problemId, problemSignature, sampleArgs }: {
  problemId: string
  problemSignature: string
  sampleArgs: unknown[] | null
}) {
  const [editorial, setEditorial] = useState<Editorial | null>(null)
  const [confirming, setConfirming] = useState(false)

  useEffect(() => {
    setEditorial(null)
    setConfirming(false)
    getEditorial(problemId).then(setEditorial).catch(() => setEditorial(null))
  }, [problemId])

  if (!editorial || !editorial.available) return null

  if (editorial.locked) {
    return (
      <section className="panel editorial">
        <div className="problem-head">
          <h3>해설</h3>
          <span className="muted small">맞히면 열립니다</span>
        </div>
        {!confirming ? (
          <button type="button" className="linklike" onClick={() => setConfirming(true)}>
            맞히기 전에 열기
          </button>
        ) : (
          <div className="settings-block">
            <p className="warn small">
              열면 되돌릴 수 없고, 이 문제로 쌓이는 증거가 힌트 3단계를 본 것만큼 가벼워집니다.
              방법을 본 뒤의 정답은 스스로 푼 정답과 같은 무게가 아닙니다.
            </p>
            <button type="button" onClick={() => void unlockEditorial(problemId).then(setEditorial)}>
              그래도 열기
            </button>{' '}
            <button type="button" className="linklike" onClick={() => setConfirming(false)}>
              닫기
            </button>
          </div>
        )}
      </section>
    )
  }

  return (
    <section className="panel editorial">
      <div className="problem-head">
        <h3>해설</h3>
        {!editorial.solved && <span className="warn small">맞히기 전에 열었습니다</span>}
      </div>
      <pre className="statement-body">{editorial.body}</pre>

      {/* 해설 아래에 실험실을 둔다. "입력을 바꾸면 해설과 시각화가 다시 실행된다"(§6.4)의
          그 실행이 이것이다. */}
      <LabPanel
        problemId={problemId}
        problemSignature={problemSignature}
        approaches={editorial.approaches}
        canUseMine={editorial.solved}
        sampleArgs={sampleArgs}
      />

      {/* 실험실 아래에 아레나. 둘 다 맞힌 뒤의 놀이이고, 실험실이 "왜 맞나"라면 아레나는
          "왜 틀리나"다 (§8.3). */}
      {editorial.solved && <ArenaPanel problemId={problemId} problemSignature={problemSignature} />}
      {/* 남의 접근은 저작자의 해설 다음이다. 맞힌 사람에게만 — 코드가 실린다 (§8.5). */}
      {editorial.solved && <SolutionsPanel problemId={problemId} />}
    </section>
  )
}
