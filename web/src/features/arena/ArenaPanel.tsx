import { useEffect, useRef, useState } from 'react'
import { attemptArena, getArena, getArenaAttempt, reportArenaTarget } from '../../api/client'
import type { ArenaAttempt, ArenaBoard } from '../../shared/types'

/**
 * 반례 아레나 (기획서 §8.3, 검증 Gym G-01).
 *
 * > 익명화된 오답을 실패시키는 입력을 작성합니다. 성공한 반례를 자동 축소하고 깨뜨린
 * > 가정을 분류합니다. 작은 반례, 새로운 버그 유형, 많은 제출을 깨는 반례를 별도 평가합니다.
 *
 * 오답의 소스를 보여준다. 맞힌 사람에게만 열리는 화면이라 그래도 된다 — 오답은 정답에서
 * 한 곳만 다른 코드라, 이것을 보는 것은 정답을 보는 것과 거의 같다.
 *
 * 축소된 반례를 내 입력 옆에 놓는다. 내가 적은 것보다 작은 것이 있었다면 그 차이가 곧
 * "무엇이 정말 필요한 부분이었나"의 답이다.
 */
export function ArenaPanel({ problemId, problemSignature }: { problemId: string; problemSignature: string }) {
  const [board, setBoard] = useState<ArenaBoard | null>(null)
  const [text, setText] = useState('')
  const [attempt, setAttempt] = useState<ArenaAttempt | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState<string | null>(null)
  const [reported, setReported] = useState<Set<string>>(new Set())
  const timer = useRef<number | null>(null)

  useEffect(() => () => { if (timer.current) window.clearTimeout(timer.current) }, [])

  const reload = () => getArena(problemId).then(setBoard).catch(() => setBoard(null))
  useEffect(() => {
    setBoard(null)
    setAttempt(null)
    setText('')
    void reload()
  }, [problemId])

  if (!board || board.locked || board.targets.length === 0) return null

  const poll = (id: string) => {
    timer.current = window.setTimeout(async () => {
      const next = await getArenaAttempt(id).catch(() => null)
      if (next) setAttempt(next)
      if (!next || next.status === 'PENDING') poll(id)
      else void reload()
    }, 2000)
  }

  const submit = async () => {
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
      const started = await attemptArena(problemId, args)
      setAttempt(started)
      poll(started.id)
    } catch (e) {
      setError(e instanceof Error ? e.message : '시도하지 못했습니다')
    }
  }

  const recordOf = (name: string) => board.records.find((r) => r.mutantName === name)
  const resultOf = (name: string) => attempt?.results.find((r) => r.name === name)

  // 남의 오답만 신고할 수 있다. 저작자의 대표 오답은 검증 파이프라인이 이미 봤다.
  const report = async (name: string) => {
    const reason = window.prompt('무엇이 문제입니까? (정답과 같다, 남의 풀이다, 악의적이다 …)')
    if (!reason || reason.trim().length < 10) return
    try {
      await reportArenaTarget(problemId, name, reason)
      setReported((prev) => new Set(prev).add(name))
    } catch (e) {
      setError(e instanceof Error ? e.message : '신고하지 못했습니다')
    }
  }

  return (
    <div className="arena-gym">
      <h4>반례 아레나</h4>
      <p className="muted small">
        이 문제의 흔한 오답들입니다. 각각을 틀리게 만드는 입력을 적어 보세요 — {problemSignature}
      </p>

      <ul className="plain">
        {board.targets.map((target) => {
          const record = recordOf(target.name)
          const result = resultOf(target.name)
          return (
            <li key={target.name} className="arena-target">
              <div className="prescription-head">
                <span className="test-mark">{result ? (result.broken ? '✓' : '✗') : '·'}</span>
                <span className="reason">{target.kindLabel}</span>
                {target.community && <span className="muted small">누군가의 오답</span>}
                <span className="small">{target.note}</span>
                <button type="button" className="linklike small" onClick={() => setOpen(open === target.name ? null : target.name)}>
                  {open === target.name ? '코드 접기' : '코드 보기'}
                </button>
                {target.community && (
                  reported.has(target.name)
                    ? <span className="muted small">신고했습니다</span>
                    : <button type="button" className="linklike small" onClick={() => void report(target.name)}>신고</button>
                )}
                {/* 기록판. 이름은 없다 — 내 것인가 아닌가만 (§8.5 평판 정책 전). */}
                {record && (
                  <span className="muted small">
                    {record.breakers}명이 깨뜨림 · 최소 {record.smallestSize}
                    {record.smallestIsMine && ' (내 기록)'}
                    {record.firstIsMine && ' · 처음 깨뜨림'}
                  </span>
                )}
              </div>
              {open === target.name && <pre className="log mono small">{target.source}</pre>}
              {result?.broken && (
                <p className="ok small">
                  깨뜨렸습니다 — 이 오답은 {result.actual} 을 내놓았습니다.
                  {result.minimalArgs && result.minimalSize !== null && (
                    <> 줄이면 <code className="mono">{JSON.stringify(result.minimalArgs)}</code> ({result.minimalSize}) 로도 깨집니다.</>
                  )}
                </p>
              )}
            </li>
          )
        })}
      </ul>

      <input
        className="rationale mono"
        value={text}
        onChange={(e) => setText(e.target.value)}
        aria-label="반례 입력"
        placeholder="[인자들]"
      />
      <button type="button" onClick={() => void submit()} disabled={attempt?.status === 'PENDING' || !text.trim()}>
        {attempt?.status === 'PENDING' ? '돌리는 중…' : '깨뜨리기'}
      </button>
      {error && <p className="warn small">{error}</p>}
      {attempt?.status === 'INVALID_INPUT' && (
        // 전제 밖 입력은 시도가 아니다. 그것으로 "깨뜨렸다"고 하면 안 된다.
        <p className="warn small">{attempt.message}</p>
      )}
      {attempt?.status === 'COMPLETED' && attempt.results.every((r) => !r.broken) && (
        <p className="muted small">이 입력에서는 모든 오답이 정답과 같은 답을 냈습니다.</p>
      )}
    </div>
  )
}
