import { useState } from 'react'
import { assignTransfer, explainTransfer } from '../../api/client'
import type { TransferTask } from '../../shared/types'

/**
 * 전이 확인 (PRD FR-807).
 *
 * > 코칭 후 설명 과제와 변형 문제로 전이를 확인합니다. 힌트·AI 없이 완료한 결과를 가장
 * > 높은 가중치의 증거로 반영합니다.
 *
 * **코칭받은 문제를 다시 푸는 것은 전이가 아니다.** 힌트를 보고 그 자리에서 고친 것은
 * 그 문제를 푼 것이고, 옮겨졌는지는 다른 문제에서만 드러난다.
 *
 * 설명이 먼저다. 자기 말로 정리하지 않고 넘어가면, 맞혔을 때 그것이 옮겨진 것인지 비슷한
 * 모양을 기억한 것인지 가릴 수 없다.
 */
export function TransferTaskPanel({
  sessionId,
  onOpenProblem,
}: {
  sessionId: string
  onOpenProblem: (problemId: string) => void
}) {
  const [task, setTask] = useState<TransferTask | null>(null)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const assign = async () => {
    setBusy(true)
    setError(null)
    try {
      setTask(await assignTransfer(sessionId))
    } catch (e) {
      setError(e instanceof Error ? e.message : '과제를 받지 못했습니다')
    } finally {
      setBusy(false)
    }
  }

  const explain = async () => {
    if (!task) return
    setBusy(true)
    setError(null)
    try {
      setTask(await explainTransfer(task.id, text))
    } catch (e) {
      setError(e instanceof Error ? e.message : '설명을 내지 못했습니다')
    } finally {
      setBusy(false)
    }
  }

  if (!task) {
    return (
      <div className="transfer">
        <button type="button" onClick={() => void assign()} disabled={busy}>
          {busy ? '고르는 중…' : '전이 확인 받기'}
        </button>
        <p className="muted small">
          방금 배운 것이 옮겨졌는지 봅니다. 자기 말로 한 번 정리한 뒤, 힌트 없이 다른
          문제를 풀면 가장 무거운 증거가 됩니다.
        </p>
        {error && <p className="warn small">{error}</p>}
      </div>
    )
  }

  return (
    <div className="transfer">
      {task.status === 'ASSIGNED' && (
        <>
          <p className="small">
            먼저, 방금 무엇을 알게 됐는지 자기 말로 적어 주세요. 코드가 아니라 생각을 적습니다.
          </p>
          <textarea
            className="rationale"
            rows={3}
            value={text}
            onChange={(event) => setText(event.target.value)}
            aria-label="설명 과제"
            placeholder="무엇을 저장해 두면 다시 훑지 않아도 되는지, 왜 그런지…"
          />
          <button type="button" onClick={() => void explain()} disabled={busy}>
            {busy ? '내는 중…' : '설명 내기'}
          </button>
        </>
      )}

      {task.status !== 'ASSIGNED' && (
        <p className="small">
          {task.status === 'VERIFIED' ? '✓ ' : ''}
          변형 문제:{' '}
          <button type="button" className="linklike mono" onClick={() => onOpenProblem(task.targetProblemId)}>
            {task.targetProblemId}
          </button>
          {task.status === 'EXPLAINED' && ' — 힌트 없이 풀면 전이로 인정됩니다'}
          {task.status === 'VERIFIED' && ' — 전이가 확인됐습니다'}
          {/* 실패가 아니라는 것을 말한다. 힌트를 보고 푼 것도 푼 것이다. */}
          {task.status === 'UNVERIFIED' && ' — 도움을 받아 풀어, 전이 증거는 되지 않았습니다'}
        </p>
      )}

      {error && <p className="warn small">{error}</p>}
    </div>
  )
}
