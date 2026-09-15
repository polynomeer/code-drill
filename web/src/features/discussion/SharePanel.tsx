import { useState } from 'react'
import { shareSolution } from '../../api/client'
import type { Submission } from '../../shared/types'

/**
 * 맞힌 제출을 풀이로 올린다 (기획서 §8.5 "정적 풀이").
 *
 * 맞힌 제출의 판정 아래에 나온다. 코드만 올리는 것은 풀이가 아니다 — 접근을 적어야
 * 올라간다. 올린 뒤에는 맞힌 사람들에게 코드와 함께 보이고, 이름은 나가지 않는다.
 */
export function SharePanel({ submission }: { submission: Submission }) {
  const [open, setOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (submission.verdict !== 'ACCEPTED' || submission.mine === false) return null

  const share = async () => {
    setError(null)
    try {
      await shareSolution(submission.problemId, submission.id, title, body)
      setDone(true)
    } catch (e) {
      setError(e instanceof Error ? e.message : '올리지 못했습니다')
    }
  }

  return (
    <div className="arena-donate">
      <h4>이 풀이 공유하기</h4>
      {done ? (
        <p className="small">올렸습니다. 이 문제를 맞힌 사람들에게 보입니다 — 이름은 나가지 않습니다.</p>
      ) : !open ? (
        <button type="button" onClick={() => setOpen(true)}>
          풀이로 올리기
        </button>
      ) : (
        <div className="discussion-compose">
          <input
            className="rationale"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            aria-label="접근의 이름"
            placeholder="접근의 이름 (3자 이상) — 예: 해시맵 한 번 훑기"
          />
          <textarea
            className="rationale"
            rows={4}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            aria-label="접근 설명"
            placeholder="왜 이렇게 풀었는지, 어디가 핵심인지 (30자 이상). 코드만 올리는 것은 풀이가 아닙니다"
          />
          <div>
            <button type="button" onClick={() => void share()} disabled={title.trim().length < 3 || body.trim().length < 30}>
              올리기
            </button>{' '}
            <button type="button" className="linklike" onClick={() => setOpen(false)}>
              취소
            </button>
          </div>
          {error && <p className="warn small">{error}</p>}
        </div>
      )}
    </div>
  )
}
