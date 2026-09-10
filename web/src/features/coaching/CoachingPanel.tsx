import { useEffect, useState } from 'react'
import { openCoaching, revealHint } from '../../api/client'
import { COMPETENCY_LABEL } from '../../shared/types'
import { TransferTaskPanel } from './TransferTaskPanel'
import type { CoachingSession } from '../../shared/types'

/**
 * 코칭 (PRD FR-802, 기획서 §4.3).
 *
 * > 코칭 풀이는 한 세션에 약한 역량 1~2개만 선택적으로 개입합니다. 자유 풀이를 방해하지
 * > 않으며 도움 단계를 사용자가 확인할 수 있습니다.
 *
 * **관문이 아니다.** 세션은 사용자가 눌러야 열리고, 열어도 힌트는 다시 눌러야 나온다.
 * 문제를 열자마자 힌트가 보이면 그것은 "선택적 개입"이 아니라 그냥 힌트가 붙은 문제이고,
 * 그 뒤의 제출은 아무것도 재지 못한다.
 *
 * 받은 도움을 **감추지 않는다.** 몇 단계까지 봤는지가 항상 보이고, 그 값이 이 문제로
 * 쌓이는 증거를 얼마나 가볍게 하는지도 함께 적는다. 조용히 깎으면 사용자는 자기 지도가
 * 왜 안 오르는지 알 수 없다.
 */
export function CoachingPanel({
  problemId,
  onOpenProblem,
}: {
  problemId: string
  onOpenProblem: (problemId: string) => void
}) {
  const [session, setSession] = useState<CoachingSession | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  // 문제가 바뀌면 접는다. 남겨 두면 다른 문제의 힌트를 이 문제의 것으로 읽는다.
  useEffect(() => {
    setSession(null)
    setError(null)
  }, [problemId])

  const open = async () => {
    setBusy('open')
    setError(null)
    try {
      setSession(await openCoaching(problemId))
    } catch (e) {
      setError(e instanceof Error ? e.message : '세션을 열지 못했습니다')
    } finally {
      setBusy(null)
    }
  }

  const reveal = async (competency: string) => {
    if (!session) return
    setBusy(competency)
    setError(null)
    try {
      setSession(await revealHint(session.id, competency))
    } catch (e) {
      setError(e instanceof Error ? e.message : '단계를 펼치지 못했습니다')
    } finally {
      setBusy(null)
    }
  }

  if (!session) {
    return (
      <section className="panel coaching">
        <div className="problem-head">
          <h3>코칭</h3>
          <button type="button" onClick={() => void open()} disabled={busy === 'open'}>
            {busy === 'open' ? '여는 중…' : '도움받기'}
          </button>
        </div>
        <p className="muted small">
          막혔을 때만 누르세요. 지금까지의 기록에서 약한 역량 한두 개를 골라, 단계별로
          방향을 알려 줍니다. 코드는 알려 주지 않습니다.
        </p>
        {error && <p className="warn small">{error}</p>}
      </section>
    )
  }

  return (
    <section className="panel coaching">
      <div className="problem-head">
        <h3>코칭</h3>
        {/* 받은 도움을 감추지 않는다. 조용히 깎으면 지도가 왜 안 오르는지 알 수 없다. */}
        <span className="muted small">
          {session.helpLevel === 0
            ? '아직 도움 없음'
            : `${session.helpLevel}단계까지 봤습니다 · 이 문제의 증거가 그만큼 가벼워집니다`}
        </span>
      </div>

      {session.focus.length === 0 ? (
        <p className="muted small">
          이 문제에서 도울 것이 없습니다. 여기 걸린 역량은 이미 단단합니다 — 그냥 푸세요.
        </p>
      ) : (
        <ul className="coaching-focus">
          {session.focus.map((focus) => {
            const seen = session.revealed.filter((r) => r.competency === focus.competency)
            return (
              <li key={focus.competency}>
                <div className="coaching-head">
                  <span className="competency-name">
                    {COMPETENCY_LABEL[focus.competency] ?? focus.competency}
                  </span>
                  <span className="muted small mono">
                    {seen.length}/{seen.length + focus.remaining}
                  </span>
                  <button
                    type="button"
                    onClick={() => void reveal(focus.competency)}
                    disabled={busy === focus.competency || focus.remaining === 0}
                  >
                    {focus.remaining === 0 ? '더 없음' : seen.length === 0 ? '힌트' : '다음 단계'}
                  </button>
                </div>

                {/* 펼친 것은 계속 보인다. 다시 보려고 다음 단계를 펼치게 하면, 사용자는
                    필요하지도 않은 도움을 받고 그만큼 증거가 가벼워진다. */}
                {seen.map((hint) => (
                  <p key={hint.level} className="hint small">
                    <span className="muted mono">{hint.level}단계</span> {hint.text}
                  </p>
                ))}
              </li>
            )
          })}
        </ul>
      )}

      {/* 도움을 받은 뒤에만 나온다. 스스로 푼 사람에게 과제를 얹으면 안 받아도 될
          숙제가 된다 (FR-807). */}
      {session.helpLevel > 0 && (
        <TransferTaskPanel sessionId={session.id} onOpenProblem={onOpenProblem} />
      )}

      {error && <p className="warn small">{error}</p>}
    </section>
  )
}
