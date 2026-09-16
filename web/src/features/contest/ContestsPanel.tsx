import { useCallback, useEffect, useState } from 'react'
import { getContest, joinContest, joinDuel, listContests, openDuel, startVirtual } from '../../api/client'
import type { ContestSummary, ContestView } from '../../shared/types'

const KIND_LABEL: Record<ContestSummary['kind'], string> = { CONTEST: '대회', DUEL: '미니 대결', HACK: '반례 대전', VIRTUAL: '가상 참가' }

const STATUS_LABEL: Record<ContestSummary['status'], string> = {
  DRAFT: '준비 중',
  WAITING: '상대를 기다림',
  SCHEDULED: '예정',
  RUNNING: '진행 중',
  FINISHED: '끝남',
}

/**
 * 대회와 미니 대결 (기획서 §8.4 비레이팅·미니 대결).
 *
 * 문제 목록 위에 둔다 — 대회 중이면 무엇을 풀지는 대회가 정한다. 참가는 곧 이름 공개
 * 동의이고, 그 말을 버튼이 미리 한다. 이 제품에서 이름이 나가는 곳은 순위표뿐이다.
 *
 * 미니 대결은 코드로 붙는다: 여는 사람이 코드를 받아 상대에게 알리고, 상대가 붙는 순간
 * 시작한다. 짧은 문제를 동시에 풀고 끝난 뒤 순위표를 본다.
 */
export function ContestsPanel({
  currentProblem,
  onOpenProblem,
  refreshKey,
}: {
  currentProblem: string | null
  onOpenProblem: (problemId: string) => void
  /** 판정이 끝날 때마다 순위표를 다시 읽는다. */
  refreshKey: number
}) {
  const [contests, setContests] = useState<ContestSummary[] | null>(null)
  const [open, setOpen] = useState<ContestView | null>(null)
  const [code, setCode] = useState('')
  const [minutes, setMinutes] = useState(30)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(() => {
    listContests()
      .then(setContests)
      .catch(() => setContests([]))
  }, [])

  useEffect(refresh, [refresh, refreshKey])

  const show = useCallback((id: string) => {
    setError(null)
    getContest(id)
      .then(setOpen)
      .catch((e: Error) => setError(e.message))
  }, [])

  // 열어 둔 대회는 판정이 끝날 때마다 다시 읽는다 — 순위표가 움직이는 곳이다.
  useEffect(() => {
    if (open) show(open.contest.id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshKey])

  const act = async (work: () => Promise<unknown>, then?: string) => {
    setError(null)
    try {
      const result = await work()
      refresh()
      const id = then ?? (result as { contest?: { id: string } } | undefined)?.contest?.id
      if (id) show(id)
    } catch (e) {
      setError(e instanceof Error ? e.message : '실패했습니다')
    }
  }

  return (
    <section className="panel contests">
      <h3>대회</h3>
      {error && <p className="warn small">{error}</p>}
      {open ? (
        <div>
          <button type="button" className="linklike" onClick={() => setOpen(null)}>
            ← 목록
          </button>
          <h4>{open.contest.title}</h4>
          <p className="muted small">
            {KIND_LABEL[open.contest.kind]} · {STATUS_LABEL[open.contest.status]}
            {open.contest.endsAt && open.contest.status === 'RUNNING' && <> · {new Date(open.contest.endsAt).toLocaleTimeString()} 까지</>}
            {' · '}참가 {open.contest.entrants}
          </p>
          {open.contest.kind === 'HACK' && (
            <p className="muted small">문제를 맞힌 뒤 아레나에서 깨뜨린 서로 다른 오답의 수가 점수입니다. 정답 자체는 점수가 아닙니다.</p>
          )}
          {open.joinCode && (
            <p className="small">
              상대에게 알릴 코드: <code className="mono">{open.joinCode}</code> — 상대가 붙는 순간 시작합니다.
            </p>
          )}
          {/* 끝난 대회는 같은 시간 조건으로 혼자 다시 돈다. 순위표는 그때 참가자들 사이의 내 자리다 (§8.4). */}
          {open.contest.kind === 'CONTEST' && open.contest.status === 'FINISHED' && (
            <p className="small">
              {open.virtual ? (
                <button type="button" className="linklike" onClick={() => show(open.virtual!)}>
                  돌고 있는 가상 참가 열기
                </button>
              ) : (
                <>
                  <button type="button" onClick={() => void act(() => startVirtual(open.contest.id))}>
                    가상 참가
                  </button>{' '}
                  <span className="muted">같은 문제, 같은 길이로 지금부터. 그때 참가했다면 몇 등이었을지 봅니다.</span>
                </>
              )}
            </p>
          )}
          {!open.contest.joined && open.contest.kind === 'CONTEST' && open.contest.status !== 'FINISHED' && (
            <p className="small">
              <button type="button" onClick={() => void act(() => joinContest(open.contest.id), open.contest.id)}>
                참가하기
              </button>{' '}
              <span className="muted">참가하면 표시 이름이 이 대회의 순위표에 오릅니다.</span>
            </p>
          )}
          <ul className="contest-problems">
            {open.problems.map((pid) => (
              <li key={pid}>
                <button type="button" className="linklike" onClick={() => onOpenProblem(pid)}>
                  {pid}
                </button>
                {open.standings.find((s) => s.mine)?.perProblem[pid] !== undefined && (
                  <span className="muted small"> · 내 점수 {open.standings.find((s) => s.mine)?.perProblem[pid]}</span>
                )}
              </li>
            ))}
          </ul>
          <table className="standings">
            <thead>
              <tr>
                <th>#</th>
                <th>이름</th>
                <th>{open.contest.kind === 'HACK' ? '깨뜨린 오답' : '총점'}</th>
                <th>{open.contest.kind === 'HACK' ? '문제' : '푼 문제'}</th>
              </tr>
            </thead>
            <tbody>
              {open.standings.map((s) => (
                <tr key={s.rank} className={s.mine ? 'mine' : undefined}>
                  <td>{s.rank}</td>
                  <td>
                    {s.displayName}
                    {s.mine && ' (나)'}
                    {s.virtual && <span className="muted"> · 가상</span>}
                  </td>
                  <td>{s.total}</td>
                  <td>{s.solved}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <>
          {contests === null && <p className="muted small">읽는 중…</p>}
          {contests && contests.length === 0 && <p className="muted small">열린 대회가 없습니다.</p>}
          {contests && contests.length > 0 && (
            <ul className="discussion-list">
              {contests.map((c) => (
                <li key={c.id}>
                  <button type="button" className="linklike" onClick={() => show(c.id)}>
                    {c.title}
                  </button>
                  <span className="muted small">
                    {' '}
                    {KIND_LABEL[c.kind]} · {STATUS_LABEL[c.status]} · 문제 {c.problemCount} · 참가 {c.entrants}
                    {c.joined && ' · 참가 중'}
                  </span>
                </li>
              ))}
            </ul>
          )}
          <div className="duel small">
            <p className="muted">미니 대결 — 짧은 문제를 동시에 풀고 끝난 뒤 순위표를 봅니다.</p>
            {currentProblem ? (
              <p>
                <button type="button" onClick={() => void act(() => openDuel(currentProblem, minutes))}>
                  {currentProblem} 으로 대결 열기
                </button>{' '}
                <input
                  type="number"
                  min={5}
                  max={120}
                  value={minutes}
                  onChange={(e) => setMinutes(Number(e.target.value))}
                  aria-label="대결 시간(분)"
                  className="discussion-line"
                />
                분
              </p>
            ) : (
              <p className="muted">문제를 고르면 그 문제로 대결을 열 수 있습니다.</p>
            )}
            <p>
              <input
                className="rationale discussion-code"
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                aria-label="대결 코드"
                placeholder="받은 코드"
                maxLength={6}
              />{' '}
              <button type="button" onClick={() => void act(() => joinDuel(code))} disabled={code.trim().length !== 6}>
                코드로 붙기
              </button>
            </p>
          </div>
        </>
      )}
    </section>
  )
}
