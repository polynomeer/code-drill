import { useEffect, useRef, useState } from 'react'
import { getMutationCheck, startMutationCheck } from '../../api/client'
import type { KindSummary, MutationCheck, TrialCaseInput } from '../../shared/types'

/**
 * 내 테스트가 무엇을 잡고 무엇을 놓치는가 (PRD FR-804).
 *
 * > 사용자 테스트를 변이 구현에 실행해 mutation score와 누락 유형을 제공합니다.
 * > 숨은 정답 데이터는 노출하지 않고 탐지한 결함군을 설명합니다.
 *
 * 테스트 패널의 거울상이다. 저기서는 케이스를 잣대로 삼아 코드를 재고, 여기서는
 * 저작자의 오답을 잣대로 삼아 **케이스**를 잰다.
 *
 * **점수를 크게 그리지 않는다.** 사용자가 다음에 할 일을 정하는 것은 "62%"가 아니라
 * "경계 입력을 놓쳤다"이고, 숫자를 앞에 두면 그 문장을 읽지 않는다.
 */
export function MutationCheckPanel({
  problemId,
  cases,
}: {
  problemId: string
  cases: TrialCaseInput[] | null
}) {
  const [check, setCheck] = useState<MutationCheck | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [running, setRunning] = useState(false)

  const timer = useRef<number | null>(null)
  useEffect(() => () => { if (timer.current) window.clearTimeout(timer.current) }, [])

  // 문제가 바뀌면 지난 결과를 지운다. 남겨 두면 다른 문제의 점수를 이 문제의 것으로 읽는다.
  useEffect(() => {
    setCheck(null)
    setError(null)
  }, [problemId])

  const tests = cases?.filter((item) => item.expected !== undefined) ?? []

  const run = async () => {
    setError(null)
    setRunning(true)
    try {
      const started = await startMutationCheck(problemId, tests)
      setCheck(started)
      poll(started.id)
    } catch (e) {
      setRunning(false)
      setError(e instanceof Error ? e.message : '평가에 실패했습니다')
    }
  }

  const poll = (id: string) => {
    timer.current = window.setTimeout(async () => {
      try {
        const next = await getMutationCheck(id)
        setCheck(next)
        if (next.status === 'PENDING') poll(id)
        else setRunning(false)
      } catch {
        // 결과는 이미 큐에 있다. 한 번 실패했다고 포기하면 사용자는 영영 못 본다.
        poll(id)
      }
    }, 1000)
  }

  return (
    <div className="mutation-check">
      <div className="editor-header">
        <h4>테스트 점검</h4>
        <button onClick={() => void run()} disabled={running || tests.length === 0}>
          {running ? '재는 중…' : '재기'}
        </button>
      </div>

      {tests.length === 0 ? (
        <p className="muted small">
          기대 출력을 적은 케이스가 있어야 잽니다. 위 테스트 칸에{' '}
          <code className="mono">[인자들] =&gt; 기대값</code> 으로 적어 주세요.
        </p>
      ) : (
        <p className="muted small">
          기대 출력을 적은 {tests.length}건으로, 이 문제에서 흔한 오답들을 잡아 보게 합니다.
        </p>
      )}

      {error && <p className="warn">{error}</p>}

      {check?.status === 'NO_CASES' && (
        <p className="muted small">{check.message ?? '잴 것이 없었습니다.'}</p>
      )}
      {check?.status === 'FAILED' && (
        // 사용자 잘못이 아니라는 것을 말한다. 0% 로 보이면 자기 테스트를 의심한다.
        <p className="warn small">
          {check.message ?? '평가가 돌지 않았습니다'} — 테스트 문제가 아닙니다.
        </p>
      )}

      {check?.status === 'COMPLETED' && <Result check={check} />}
    </div>
  )
}

function Result({ check }: { check: MutationCheck }) {
  const scored = check.kinds.filter((kind) => kind.scored)
  const missed = scored.filter((kind) => kind.killed < kind.total)

  return (
    <>
      {check.mistakenCases.length > 0 && (
        // 정답 값은 알려 주지 않는다. "당신의 기대가 틀렸다"까지가 스스로 고칠 수 있는
        // 정보이고, 그 너머는 답을 주는 것이다.
        <p className="warn small">
          {check.mistakenCases.join(', ')}번 케이스의 기대 출력이 실제 정답과 다릅니다.
          점수에서 뺐습니다.
        </p>
      )}

      {/* 결론을 먼저 쓴다. 숫자는 그 뒤에 붙는다. */}
      <p className={missed.length === 0 ? 'ok' : 'warn'}>
        {missed.length === 0
          ? '흔한 오답을 전부 잡았습니다.'
          : `${missed.map((kind) => kind.label).join('·')} 결함을 놓쳤습니다.`}
        {check.score !== null && (
          <span className="muted small"> · {Math.round(check.score * 100)}%</span>
        )}
      </p>

      <ul className="mutation-kinds">
        {check.kinds.map((kind) => <KindRow key={kind.kind} kind={kind} />)}
      </ul>
    </>
  )
}

function KindRow({ kind }: { kind: KindSummary }) {
  const caught = kind.killed === kind.total
  return (
    <li className={kind.scored ? '' : 'unmeasured'}>
      {/* 색만으로 알리지 않는다. 기호와 문장이 먼저 읽힌다. */}
      <span className="test-mark">{!kind.scored ? '·' : caught ? '✓' : '✗'}</span>
      <span className="competency-name">{kind.label}</span>
      <span className="muted small mono">
        {kind.killed}/{kind.total}
      </span>
      <span className="muted small">
        {!kind.scored
          // 못 잡은 것을 감점하지 않는 이유를 말해 둔다. 말하지 않으면 사용자는 손으로
          // 큰 입력을 적으려 든다.
          ? '한도를 넘길 만큼 큰 입력이 있어야 잡힙니다 — 점수에서 뺐습니다'
          : caught
            ? `${kind.killedBy.join(', ')}번 케이스가 잡았습니다`
            : '이 결함을 잡는 케이스가 없습니다'}
      </span>
    </li>
  )
}
